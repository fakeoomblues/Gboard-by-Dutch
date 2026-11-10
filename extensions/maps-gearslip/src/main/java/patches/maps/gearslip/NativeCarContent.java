package patches.maps.gearslip;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Native Maps content and callbacks, presented through the same host templates as Vela. */
public final class NativeCarContent {
    enum Kind { HOME, RECENTS, SEARCH, PREVIEW, DETAILS, SETTINGS }
    static final class Action {
        final Object model;
        final String title,method;
        final int consumer;
        final boolean enabled;
        Action(Object model,String title,String method,int consumer,boolean enabled){this.model=model;this.title=title;this.method=method;this.consumer=consumer;this.enabled=enabled;}
        boolean same(Action other){return other!=null&&model==other.model&&method.equals(other.method)&&consumer==other.consumer&&enabled==other.enabled&&title.equals(other.title);}
        void run()throws Exception{
            if(!enabled)throw new SecurityException("Native action is unavailable");
            if(model instanceof View){
                View view=(View)model;if(!view.isEnabled())throw new SecurityException("Native action was disabled");
                if(!view.performClick())NativeControls.touch(view);
            }else if(consumer>=0){
                Class<?> type=model.getClass().getClassLoader().loadClass("poe");
                Constructor<?> constructor=type.getDeclaredConstructor(int.class);constructor.setAccessible(true);
                Object handler=constructor.newInstance(consumer);call(handler,"accept",new Class<?>[]{Object.class},model);
            }else call(model,method);
        }
    }
    static final class Row {
        final String title;
        final List<String> texts;
        final Action action;
        /** Native switch state for a toggle row; null for an ordinary row. */
        final Boolean checked;
        Row(String title,List<String> texts,Action action){this(title,texts,action,null);}
        Row(String title,List<String> texts,Action action,Boolean checked){this.title=title;this.texts=Collections.unmodifiableList(new ArrayList<>(texts));this.action=action;this.checked=checked;}
    }
    /** Host-only commands; they resolve the live native owner when run, never a cached view. */
    static final String SEARCH_COMMAND="morphe:search",PAGE_DOWN="morphe:pageDown",PAGE_UP="morphe:pageUp";
    private static final Object SEARCH_MODEL=new Object();
    static final class Page {
        final Kind kind;
        final String title;
        final List<Row> rows;
        final List<Action> actions;
        final Action primaryAction;
        final View panel;
        Page(Kind kind,String title,List<Row> rows,List<Action> actions,View panel){
            this(kind,title,rows,actions,panel,actions.isEmpty()?null:actions.get(0));
        }
        Page(Kind kind,String title,List<Row> rows,List<Action> actions,View panel,Action primaryAction){
            this.kind=kind;this.title=title;this.rows=Collections.unmodifiableList(new ArrayList<>(rows));this.actions=Collections.unmodifiableList(new ArrayList<>(actions));this.panel=panel;
            this.primaryAction=primaryAction;
        }
        boolean contains(Action candidate){
            for(Action action:actions)if(action.same(candidate))return true;
            for(Row row:rows)if(row.action!=null&&row.action.same(candidate))return true;
            return false;
        }
        String signature(){
            StringBuilder out=new StringBuilder(kind.name()).append('|').append(title);
            for(Row row:rows){out.append('|').append(row.title).append(row.texts).append(row.checked);if(row.action!=null)out.append(System.identityHashCode(row.action.model)).append(row.action.enabled);}
            for(Action action:actions)out.append('|').append(System.identityHashCode(action.model)).append(action.title).append(action.enabled);
            return out.toString();
        }
    }

    private static final Map<Object,WeakReference<NativeCarContent>> OWNERS=new WeakHashMap<>();
    private static final Map<Object,WeakReference<Object>> SOURCES=new WeakHashMap<>();
    private static final ThreadLocal<WeakReference<Object>> READING_SUGGESTIONS=new ThreadLocal<>();
    private final Context context;
    private final WeakReference<Object> windowOwner;
    private final Runnable changed;
    private final Handler main=new Handler(Looper.getMainLooper());
    private WeakReference<Object> home=new WeakReference<>(null),recents=new WeakReference<>(null),search=new WeakReference<>(null);
    private final Map<View,WeakReference<Object>> previewPanels=new WeakHashMap<>(),footerPanels=new WeakHashMap<>();
    private static final Handler PANELS=new Handler(Looper.getMainLooper());
    private WeakReference<List<?>> suggestionValues=new WeakReference<>(null);
    private boolean closed,details;
    private Page displayed;
    private String lastSignature="";
    private final Map<String,String> capturedSignatures=new java.util.HashMap<>();
    private long lastPollAt=-1;
    private boolean notificationQueued;
    private final Runnable notifyChange;
    private void notifyChanged(){if(!notificationQueued){notificationQueued=true;main.post(notifyChange);}}

    NativeCarContent(Object owner,Context context,Runnable changed){this.context=context;this.windowOwner=new WeakReference<>(owner);this.changed=changed;notifyChange=()->{notificationQueued=false;if(!closed)this.changed.run();};synchronized(OWNERS){OWNERS.put(owner,new WeakReference<>(this));}}
    private static NativeCarContent current(){synchronized(OWNERS){WeakReference<NativeCarContent> reference=OWNERS.get(LocalCarUiSession.owner());return reference==null?null:reference.get();}}
    private static NativeCarContent forOwner(Object owner){if(owner==null||LocalCarUiSession.owner()!=owner)return null;synchronized(OWNERS){WeakReference<NativeCarContent> value=OWNERS.get(owner);return value==null?null:value.get();}}
    private static Object ownerFor(Object source){
        synchronized(SOURCES){
            WeakReference<Object> known=SOURCES.get(source);
            if(known!=null)return known.get();
            Object owner=NativeCarScopes.producerOwner(source);
            if(owner==null)try{
                String type=source.getClass().getName();
                if("rmo".equals(type))owner=NativeCarScopes.sceneOwner(field(source,"c"),field(source,"a"));
                else if("rmg".equals(type))owner=NativeCarScopes.owner((Context)field(source,"a"),field(source,"b"));
                else if("qdf".equals(type))owner=NativeCarScopes.owner((Context)field(source,"c"),field(source,"b"));
                else if("qbn".equals(type))owner=NativeCarScopes.owner((Context)field(source,"p"),field(source,"b"));
                else if("sso".equals(type))owner=NativeCarScopes.owner((Context)field(source,"b"),field(source,"c"));
            }catch(Exception unavailable){return null;}
            if(owner!=null)SOURCES.put(source,new WeakReference<>(owner));return owner;
        }
    }

    /** Guarded product hooks preserve the original native method results and policy. */
    public static void capture(Object model){
        if(model==null)return;NativeCarContent current=forOwner(ownerFor(model));if(current==null||current.closed)return;
        try{
            String key;List<Row> rows;
            if("rmg".equals(model.getClass().getName())){current.home=new WeakReference<>(model);key="home";rows=readHome(model);}
            else if(instance(model,"qcq")){current.recents=new WeakReference<>(model);key="recents";rows=readRecents(model);}
            else if(instance(model,"qbh")){current.search=new WeakReference<>(model);key="search";rows=current.readSearch(model);}
            else if(instance(model,"snt")){key="preview";rows=Collections.singletonList(new Row(text(call(model,"l")),Collections.singletonList(String.valueOf(call(model,"K"))),null));}
            else return;
            current.captured(key,model,rows);
        }catch(Exception error){Log.w("MapsNativeCar","Native card model unavailable",error);}
    }
    public static void readingSuggestions(Object source){READING_SUGGESTIONS.set(new WeakReference<>(ownerFor(source)));}
    public static void suggestions(List<?> values){
        WeakReference<Object> owner=READING_SUGGESTIONS.get();READING_SUGGESTIONS.remove();
        NativeCarContent current=owner==null?null:forOwner(owner.get());if(current==null||values==null||current.closed)return;
        current.suggestionValues=new WeakReference<>(values);
        try{current.captured("suggestions",values,readHomeRows(values));}
        catch(Exception error){Log.w("MapsNativeCar","Native suggestions unavailable",error);}
    }
    private void captured(String key,Object source,List<Row> rows){
        String signature=System.identityHashCode(source)+new Page(Kind.HOME,"",rows,Collections.emptyList(),null).signature();
        if(!signature.equals(capturedSignatures.put(key,signature)))notifyChanged();
    }
    public static void panel(Object source,View view){
        if(source==null||view==null)return;Object owner=NativeCarScopes.contextOwner(view.getContext());NativeCarContent current=forOwner(owner);if(current==null||current.closed)return;
        try{
            if("ssf".equals(source.getClass().getName())){
                Object model=call(call(source,"n"),"a");NativeCarScopes.producer(field(model,"c"),owner);
                WeakReference<Object> old=current.previewPanels.put(view,new WeakReference<>(source));if(old!=null&&old.get()==source)return;
            }else if("ruo".equals(source.getClass().getName())){
                NativeCarScopes.producer(field(field(source,"a"),"n"),owner);
                WeakReference<Object> old=current.footerPanels.put(view,new WeakReference<>(source));if(old!=null&&old.get()==source)return;
            }else return;
            current.notifyChanged();
        }catch(Exception error){Log.w("MapsNativeCar","Native panel content unavailable",error);}
    }
    public static void panelSource(Object source){
        if(source==null)return;
        String type=source.getClass().getName();if(!"ssf".equals(type)&&!"ruo".equals(type))return;
        PANELS.post(()->{
            try{
                // Read the cached root after the original d() SHOWN lifecycle
                // callback returns; R8 can bypass the final b() view getter.
                Object root="ssf".equals(type)?call(field(source,"aG"),"a"):field(source,"p");
                if(root instanceof View)panel(source,(View)root);
            }catch(Exception error){Log.w("MapsNativeCar","Native panel root unavailable",error);}
        });
    }

    Page read(ViewGroup root,NativeControls controls,boolean guiding,String query)throws Exception{
        if(closed)return null;
        if(query!=null){
            details=false;
            boolean results=!query.trim().isEmpty();Object source=(results?search:recents).get();
            if(source==null)return new Page(results?Kind.SEARCH:Kind.RECENTS,query,Collections.emptyList(),Collections.emptyList(),null);
            List<Row> rows=results?readSearch(source):readRecents(source);
            return new Page(results?Kind.SEARCH:Kind.RECENTS,query,rows,Collections.emptyList(),null);
        }
        // Maps draws its Settings/Sound panel on the map surface, where a host's own left
        // panel can cover it. Present the same native rows as a host list instead.
        View settings=find(root,"settings_dialog_setting_list");
        if(ownedWindow(root)&&visible(settings,root,controls)){details=false;return settings(settings,root,controls);}
        View panel=findPanel(root,controls,previewPanels);
        if(panel!=null){Object overlay=previewPanels.get(panel).get();return preview(call(call(overlay,"n"),"a"),panel,controls);}
        // The active optimized family can render this same native panel without
        // using ssf/ruo. Its actual attached window proves ownership; arrival
        // time, the process Context and a global-current fallback never do.
        View actualPreview=find(root,"place_details_card_scrollable");
        if(ownedWindow(root)&&visible(actualPreview,root,controls))return previewView(actualPreview,controls);
        details=false;
        if(guiding)return null;
        View homePanel=find(root,"main_suggested_destinations_card");Object source=home.get();
        List<?> suggestions=suggestionValues.get();
        if((source!=null||suggestions!=null)&&visible(homePanel,root,controls))
            return new Page(Kind.HOME,context.getApplicationInfo().loadLabel(context.getPackageManager()).toString(),source!=null?readHome(source):readHomeRows(suggestions),Collections.emptyList(),homePanel);
        // Compact car layouts have only a search box and no suggestion card. Still give the
        // host Vela's idle list: Search first, then places Maps already handed to the adapter.
        if(!ownedWindow(root)||!controls.available(NativeControls.Role.SEARCH))return null;
        List<Row> rows=new ArrayList<>();String search=controls.title(NativeControls.Role.SEARCH);
        rows.add(new Row(search,Collections.emptyList(),new Action(SEARCH_MODEL,search,SEARCH_COMMAND,-1,true)));
        Object recentSource=recents.get();
        try{if(source!=null)rows.addAll(readHome(source));else if(suggestions!=null)rows.addAll(readHomeRows(suggestions));else if(recentSource!=null)rows.addAll(readRecents(recentSource));}
        catch(Exception unavailable){Log.w("MapsNativeCar","Native idle places unavailable",unavailable);}
        return new Page(Kind.HOME,context.getApplicationInfo().loadLabel(context.getPackageManager()).toString(),rows,Collections.emptyList(),null);
    }
    Page show(ViewGroup root,NativeControls controls,boolean guiding,String query)throws Exception{
        Page value=read(root,controls,guiding,query);displayed=value;
        lastSignature=signature(value,root,controls,guiding);return value;
    }
    void layoutChanged(ViewGroup root,NativeControls controls,boolean guiding,String query){
        long now=android.os.SystemClock.uptimeMillis();if(lastPollAt>=0&&now-lastPollAt<250)return;lastPollAt=now;
        try{Page value=read(root,controls,guiding,query);String signature=signature(value,root,controls,guiding);if(!signature.equals(lastSignature)){lastSignature=signature;changed.run();}}
        catch(Exception error){Log.w("MapsNativeCar","Native content update unavailable",error);}
    }
    private String signature(Page value,ViewGroup root,NativeControls controls,boolean guiding)throws Exception{
        StringBuilder result=new StringBuilder(value==null?"":value.signature());
        if(guiding)for(Action action:footer(root,controls))result.append('|').append(System.identityHashCode(action.model)).append(action.title).append(action.enabled);
        return result.toString();
    }
    void invoke(Action action,ViewGroup root,NativeControls controls,boolean guiding,String query)throws Exception{
        Page current=read(root,controls,guiding,query);
        boolean allowed=current!=null&&current.contains(action);
        if(guiding&&!allowed)for(Action footerAction:footer(root,controls))if(footerAction.same(action)){allowed=true;break;}
        if(!allowed)throw new SecurityException("Native card action was replaced");
        if(SEARCH_COMMAND.equals(action.method)){controls.invoke(NativeControls.Role.SEARCH);notifyChanged();return;}
        if(PAGE_DOWN.equals(action.method)||PAGE_UP.equals(action.method)){
            View list=(View)action.model;int step=Math.max(1,list.getHeight()*4/5);
            list.scrollBy(0,PAGE_DOWN.equals(action.method)?step:-step);notifyChanged();return;
        }
        controls.invoke(action);notifyChanged();
    }
    boolean back(){if(!details)return false;details=false;changed.run();return true;}
    void details(){details=true;changed.run();}
    boolean detailsOpen(){return details;}
    String detailsTitle(){return label("CAR_ROUTE_DETAILS","Route details");}

    List<Action> footer(ViewGroup root,NativeControls controls)throws Exception{
        View panel=findPanel(root,controls,footerPanels);if(panel==null)return footerView(root,controls);
        Object overlay=footerPanels.get(panel).get(),source=field(overlay,"a");
        List<Action> actions=new ArrayList<>();Set<Object> used=Collections.newSetFromMap(new IdentityHashMap<>());
        // Exact original ruq/rty order; m aliases Options, so retain it once.
        for(String name:new String[]{"b","c","d","e","f","g","m",(Boolean)field(source,"l")?"i":"h"}){
            Object model=field(source,name);if(model==null||!used.add(model))continue;
            String title=text(call(model,"c"));if(!title.isEmpty())actions.add(new Action(model,title,"d",-1,true));
        }
        return actions;
    }
    private boolean ownedWindow(ViewGroup root){
        Object owner=windowOwner.get();
        return owner!=null&&owner==LocalCarUiSession.owner()&&root!=null&&NativeCarScopes.contextOwner(root.getContext())==owner;
    }
    private List<Action> footerView(ViewGroup root,NativeControls controls){
        View panel=find(root,"status_card_view");
        if(!ownedWindow(root)||!visible(panel,root,controls))return Collections.emptyList();
        View stop=find(panel,"status_panel_close_button");
        if(stop==null||!visible(stop,panel,controls))return Collections.emptyList();
        View target=controls.clickable(stop);String title=actionTitle(stop,controls);
        if(target==null||title.isEmpty())return Collections.emptyList();
        List<Action> actions=new ArrayList<>(),remaining=new ArrayList<>();
        actions.add(new Action(target,title,"view",-1,enabled(target)));collectActions(panel,controls,remaining);
        for(Action action:remaining){
            View view=(View)action.model;boolean exit=false;
            for(View node=view;node!=null;node=node.getParent() instanceof View?(View)node.getParent():null)if(node==stop){exit=true;break;}
            if(!exit)actions.add(action);
        }
        return actions;
    }

    static List<Row> readHome(Object source)throws Exception{
        return readHomeRows(list(call(field(source,"l"),"a")));
    }
    private static List<Row> readHomeRows(List<?> values)throws Exception{
        List<Row> rows=new ArrayList<>();
        for(Object value:values){
            Object model=instance(value,"rlr")?call(value,"b"):value;
            String title=text(call(model,"f"));if(title.isEmpty())continue;
            rows.add(new Row(title,texts(call(model,"g"),call(model,"h")),new Action(model,title,"i",-1,true)));
        }
        return rows;
    }
    static List<Row> readRecents(Object source)throws Exception{
        List<Row> rows=new ArrayList<>();
        for(Object model:list(call(source,"d"))){
            String title=text(call(model,"h"));if(title.isEmpty())continue;
            rows.add(new Row(title,texts(call(model,"i"),call(model,"g")),new Action(model,title,"j",-1,true)));
        }
        return rows;
    }
    List<Row> readSearch(Object source)throws Exception{
        List<Row> rows=new ArrayList<>();
        for(Object model:list(call(source,"c"))){
            Object place=field(model,"a");String title=text(call(place,"e"));if(title.isEmpty())continue;
            rows.add(new Row(title,texts(call(place,"d")),new Action(model,title,"accept",18,true)));
        }
        Object footer=call(source,"b");
        if(footer!=null&&"qbo".equals(footer.getClass().getName())){
            String title=text(field(footer,"a"));if(!title.isEmpty())rows.add(new Row(title,Collections.emptyList(),null));
        }else if(footer!=null&&"qbp".equals(footer.getClass().getName())){
            String title=label("OFFLINE_TOUCH_TO_RETRY","Offline. Tap to retry");
            rows.add(new Row(title,Collections.emptyList(),new Action(footer,title,"accept",19,true)));
        }
        return rows;
    }
    Page preview(Object source,View panel,NativeControls controls)throws Exception{
        List<String> labels=new ArrayList<>();collectText(panel,controls,labels);
        List<Row> rows=new ArrayList<>();for(String label:labels)rows.add(new Row(label,Collections.emptyList(),null));
        String title=rows.isEmpty()?context.getApplicationInfo().loadLabel(context.getPackageManager()).toString():rows.get(0).title;
        List<Action> actions=new ArrayList<>();String start=text(call(source,"l"));
        Action primary=start.isEmpty()?null:new Action(source,start,"r",-1,(Boolean)call(source,"K"));
        if(primary!=null)actions.add(primary);
        collectActions(panel,controls,actions);
        return new Page(details?Kind.DETAILS:Kind.PREVIEW,title,rows,actions,panel,primary);
    }
    private Page settings(View list,ViewGroup root,NativeControls controls){
        List<String> heading=new ArrayList<>();View bar=find(root,"title_bar_title");if(bar!=null)collectText(bar,controls,heading,false);
        String title=heading.isEmpty()?label("SETTINGS","Settings"):heading.get(0);
        View recycler=find(list,"recycler_view");List<Row> rows=new ArrayList<>();
        if(recycler instanceof ViewGroup){
            ViewGroup items=(ViewGroup)recycler;boolean dutch=dutch();
            if(items.canScrollVertically(-1)){String previous=dutch?"Vorige":"Previous";rows.add(new Row(previous,Collections.emptyList(),new Action(items,previous,PAGE_UP,-1,true)));}
            for(int i=0;i<items.getChildCount();i++)settingsRows(items.getChildAt(i),controls,rows,dutch);
            if(items.canScrollVertically(1)){String more=dutch?"Meer instellingen":"More settings";rows.add(new Row(more,Collections.emptyList(),new Action(items,more,PAGE_DOWN,-1,true)));}
        }
        return new Page(Kind.SETTINGS,title,rows,Collections.emptyList(),list);
    }
    /** One native settings item: a switch row, an icon-button group (guidance audio) or a plain row. */
    static void settingsRows(View item,NativeControls controls,List<Row> rows,boolean dutch){
        if(controls.nativeVisibility(item)!=View.VISIBLE||item.getWidth()<=0||item.getHeight()<=0)return;
        List<String> labels=new ArrayList<>();collectText(item,controls,labels,false);
        android.widget.CompoundButton toggle=toggle(item,controls);
        if(toggle!=null&&!labels.isEmpty()){
            View target=clickableOwner(toggle,item);String title=labels.get(0);
            rows.add(new Row(title,labels.subList(1,labels.size()),new Action(target,title,"view",-1,enabled(target)),toggle.isChecked()));return;
        }
        List<View> buttons=new ArrayList<>();iconButtons(item,controls,buttons);
        if(buttons.size()>=2){
            String title=labels.isEmpty()?"":labels.get(0);
            if(!title.isEmpty())rows.add(new Row(title,labels.subList(1,labels.size()),null));
            String[] fallback=buttons.size()!=3?null:dutch?new String[]{"Gedempt","Alleen meldingen","Niet gedempt"}:new String[]{"Muted","Alerts only","Unmuted"};
            for(int b=0;b<buttons.size();b++){
                View button=buttons.get(b);String name=text(button.getContentDescription());
                if(name.isEmpty())name=fallback!=null?fallback[b]:title+" "+(b+1);
                // Maps marks the active mode only in its status line ("Muted", "Unmuted").
                boolean active=selected(button)||labels.size()>1&&labels.get(1).equalsIgnoreCase(name);
                List<String> state=active?Collections.singletonList(dutch?"Gekozen":"Selected"):Collections.emptyList();
                rows.add(new Row(name,state,new Action(button,name,"view",-1,enabled(button))));
            }
            return;
        }
        if(labels.isEmpty())return;
        View target=controls.clickable(item);String title=labels.get(0);
        rows.add(new Row(title,labels.subList(1,labels.size()),target==null?null:new Action(target,title,"view",-1,enabled(target))));
    }
    private static android.widget.CompoundButton toggle(View view,NativeControls controls){
        if(controls.nativeVisibility(view)!=View.VISIBLE)return null;
        if(view instanceof android.widget.CompoundButton)return (android.widget.CompoundButton)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){android.widget.CompoundButton found=toggle(((ViewGroup)view).getChildAt(i),controls);if(found!=null)return found;}
        return null;
    }
    /** The clickable row that owns a switch toggles it natively; fall back to the switch itself. */
    private static View clickableOwner(View view,View item){
        for(View node=view;node!=null;node=node.getParent() instanceof View?(View)node.getParent():null){
            if(node.hasOnClickListeners()||node.isClickable())return node;if(node==item)break;
        }
        return view;
    }
    private static void iconButtons(View view,NativeControls controls,List<View> buttons){
        if(controls.nativeVisibility(view)!=View.VISIBLE||view.getWidth()<=0||view.getHeight()<=0)return;
        if((view.hasOnClickListeners()||view.isClickable())&&!(view instanceof TextView)&&!hasText(view)&&hasImage(view)){buttons.add(view);return;}
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)iconButtons(((ViewGroup)view).getChildAt(i),controls,buttons);
    }
    private static boolean hasText(View view){
        if(view instanceof TextView)return !text(((TextView)view).getText()).isEmpty();
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(hasText(((ViewGroup)view).getChildAt(i)))return true;
        return false;
    }
    private static boolean hasImage(View view){
        if(view instanceof android.widget.ImageView)return true;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(hasImage(((ViewGroup)view).getChildAt(i)))return true;
        return false;
    }
    private static boolean selected(View view){
        if(view.isSelected()||view.isActivated())return true;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(selected(((ViewGroup)view).getChildAt(i)))return true;
        return false;
    }
    private boolean dutch(){return "nl".equals(context.getResources().getConfiguration().getLocales().get(0).getLanguage());}
    private Page previewView(View panel,NativeControls controls){
        List<String> labels=new ArrayList<>();collectText(panel,controls,labels);
        List<Row> rows=new ArrayList<>();for(String label:labels)rows.add(new Row(label,Collections.emptyList(),null));
        String title=rows.isEmpty()?context.getApplicationInfo().loadLabel(context.getPackageManager()).toString():rows.get(0).title;
        View start=find(panel,"place_details_navigation_footer_action_button");
        if(start==null)start=find(panel,"start_navigation_action_button");
        Action primary=null;List<Action> actions=new ArrayList<>();
        if(start!=null&&visible(start,panel,controls)){
            String label=actionTitle(start,controls);
            View target=controls.clickable(start);
            if(!label.isEmpty()&&target!=null){
                primary=new Action(target,label,"view",-1,enabled(target));actions.add(primary);
            }
        }
        collectActions(panel,controls,actions);
        return new Page(details?Kind.DETAILS:Kind.PREVIEW,title,rows,actions,panel,primary);
    }
    private static void collectText(View view,NativeControls controls,List<String> labels){
        collectText(view,controls,labels,true);
    }
    private static void collectText(View view,NativeControls controls,List<String> labels,boolean skipPrimary){
        if(controls.nativeVisibility(view)!=View.VISIBLE||view.getAlpha()<=0||view.getWidth()<=0||view.getHeight()<=0||skipPrimary&&primaryStart(view))return;
        if(view instanceof TextView){String value=text(((TextView)view).getText());if(!value.isEmpty()&&!value.matches("[\\s·•|]+")&&!labels.contains(value))labels.add(value);}
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)collectText(((ViewGroup)view).getChildAt(i),controls,labels,skipPrimary);
    }
    private static void collectActions(View view,NativeControls controls,List<Action> actions){
        if(controls.nativeVisibility(view)!=View.VISIBLE||view.getAlpha()<=0||primaryStart(view))return;
        int before=actions.size();
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)collectActions(((ViewGroup)view).getChildAt(i),controls,actions);
        if(actions.size()!=before)return;
        if(view.hasOnClickListeners()||view.isClickable()){
            String title=actionTitle(view,controls);
            if(!title.isEmpty()){actions.add(new Action(view,title,"view",-1,enabled(view)));return;}
        }
    }
    private static String actionTitle(View view,NativeControls controls){
        String title=text(view.getContentDescription());
        if(title.isEmpty()){List<String> labels=new ArrayList<>();collectText(view,controls,labels,false);if(!labels.isEmpty())title=labels.get(0);}
        return title;
    }
    private static boolean primaryStart(View view){
        String id=name(view);
        return "start_navigation_action_button".equals(id)||"place_details_navigation_footer_action_button".equals(id);
    }
    private String label(String name,String fallback){
        for(String pkg:new String[]{context.getPackageName(),"com.google.android.apps.maps"}){
            int id=context.getResources().getIdentifier(name,"string",pkg);if(id!=0)return context.getString(id);
        }
        return fallback;
    }
    private static boolean enabled(View view){for(View node=view;node!=null;node=node.getParent() instanceof View?(View)node.getParent():null)if(!node.isEnabled())return false;return true;}
    private static boolean visible(View view,View root,NativeControls controls){
        if(view==null||root==null||controls==null||!view.isAttachedToWindow())return false;
        for(View node=view;node!=null;node=node.getParent() instanceof View?(View)node.getParent():null){if(controls.nativeVisibility(node)!=View.VISIBLE||node.getAlpha()==0)return false;if(node==root)return true;}
        return false;
    }
    private static View find(View view,String id){
        if(view==null)return null;if(id.equals(name(view)))return view;
        if(view instanceof ViewGroup)for(int i=((ViewGroup)view).getChildCount()-1;i>=0;i--){View found=find(((ViewGroup)view).getChildAt(i),id);if(found!=null)return found;}
        return null;
    }
    private static View findPanel(View view,NativeControls controls,Map<View,WeakReference<Object>> panels){
        if(view==null||controls==null||!view.isAttachedToWindow()||controls.nativeVisibility(view)!=View.VISIBLE||view.getAlpha()==0)return null;
        if(view instanceof ViewGroup)for(int i=((ViewGroup)view).getChildCount()-1;i>=0;i--){View found=findPanel(((ViewGroup)view).getChildAt(i),controls,panels);if(found!=null)return found;}
        WeakReference<Object> model=panels.get(view);return model!=null&&model.get()!=null?view:null;
    }
    private static String name(View view){try{return view.getResources().getResourceEntryName(view.getId());}catch(android.content.res.Resources.NotFoundException missing){return null;}}
    private static String text(Object value){return value instanceof CharSequence?value.toString().trim():"";}
    private static List<String> texts(Object... values){List<String> result=new ArrayList<>();for(Object value:values){String text=text(value);if(!text.isEmpty()&&!result.contains(text))result.add(text);}return result;}
    private static List<?> list(Object value){if(!(value instanceof List))throw new IllegalStateException("Native card list changed");return (List<?>)value;}
    private static boolean instance(Object value,String type){try{return value.getClass().getClassLoader().loadClass(type).isInstance(value);}catch(ClassNotFoundException absent){return false;}}
    private static Object field(Object value,String name)throws Exception{
        for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{Field field=type.getDeclaredField(name);field.setAccessible(true);return field.get(value);}catch(NoSuchFieldException absent){}
        throw new NoSuchFieldException(name);
    }
    private static Object call(Object value,String name)throws Exception{return call(value,name,new Class<?>[0]);}
    private static Object call(Object value,String name,Class<?>[] types,Object...args)throws Exception{
        Method method=null;try{method=value.getClass().getMethod(name,types);}catch(NoSuchMethodException absent){
            for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{method=type.getDeclaredMethod(name,types);break;}catch(NoSuchMethodException inherited){}
        }
        if(method==null)throw new NoSuchMethodException(name);method.setAccessible(true);return method.invoke(value,args);
    }
    void close(Object owner){closed=true;synchronized(OWNERS){OWNERS.remove(owner);}main.removeCallbacksAndMessages(null);previewPanels.clear();footerPanels.clear();displayed=null;}
}
