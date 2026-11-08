package org.ungoogled.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import nl.johninnl.navigation.alerts.WarningScheduler;
import nl.johninnl.navigation.alerts.WarningPreferences;
import nl.johninnl.navigation.alerts.WarningPassage;
import nl.johninnl.navigation.alerts.WarningEvent;
import nl.johninnl.navigation.alerts.PlaybackGrant;
import nl.johninnl.navigation.alerts.WarningAudioOwner;
import nl.johninnl.navigation.alerts.WarningStatus;
import nl.johninnl.navigation.alerts.WarningProviders;

/** Local navigation observations shared with De-Waze and Flitsmeister, serialized on the UI thread. */
public final class ExternalTelemetry {
    private static nl.johninnl.navigation.startup.NavigationClient startup;
    private static nl.johninnl.navigation.alerts.WarningSession semantic;
    private static MapsWarningSpeaker nativeSpeaker;
    private static WarningScheduler scheduler;
    private static WarningPreferences warningPreferences;
    private static boolean eventAudioManaged;
    private static final nl.johninnl.navigation.alerts.WarningView nativeWarnings=new nl.johninnl.navigation.alerts.WarningView();
    private static final Runnable SEMANTIC_RENDER=ExternalTelemetry::update;
    private static void semanticRender(){MAIN.removeCallbacks(SEMANTIC_RENDER);MAIN.post(SEMANTIC_RENDER);}
    private ExternalTelemetry() {}
    static final String ACTION="nl.johninnl.navigation.NAVIGATION_STATE";
    static final String REQUEST="nl.johninnl.navigation.NAVIGATION_STATE_REQUEST";
    static final String KEY_SOURCE="external_navigation_source", KEY_WIDGET="external_navigation_widget";
    static final String LEGACY="waze_bridge";
    static final String[] SOURCES={"",LEGACY,TelemetryStore.WAZE,TelemetryStore.FLITSMEISTER};
    static final String[] LABELS={"Off · Google Maps", "Waze · Share alerts with Maps", "De-Waze · navigation data", "Flitsmeister · navigation data"};
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static TelemetryStore store=new TelemetryStore();
    private static final RoadWarningEngine warnings=new RoadWarningEngine();
    private static Context app;
    private static String source="",publisherSession="",warning="",legacyWarning="";
    private static boolean registered,navigating,phoneNavigating;
    private static Location fix;
    private static long fixAt=-1,nativeSpeedAt=-1,nativeLimitAt=-1;
    private static Double nativeSpeed,nativeLimit;
    private static WeakReference<Activity> foreground=new WeakReference<>(null);
    private static CompactSpeedView widget;
    private static final java.util.Set<Object> carSessions=java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    private static final java.util.Set<Object> carNavigation=java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    private static final Map<Object,WeakReference<ViewGroup>> carRoots=new java.util.WeakHashMap<>();
    private static final Map<Object,int[]> carViewportSizes=new java.util.WeakHashMap<>();
    private static final Map<Object,CompactSpeedView> carWidgets=new java.util.WeakHashMap<>();
    private static boolean carActive(){return !carSessions.isEmpty();}
    /** Called only by an actual resumed native projection session; independent of its surface. */
    public static void carSession(Context context,Object owner,boolean visible){
        if(context==null||owner==null)return;
        Runnable change=()->{if(visible)carSessions.add(owner);else{carSessions.remove(owner);removeCarWidget(owner);carRoots.remove(owner);carViewportSizes.remove(owner);}refresh(context);};
        if(Looper.myLooper()==Looper.getMainLooper())change.run();else MAIN.post(change);
    }
    /** Native GUIDING is independent of phone camera updates and transient surface loss. */
    public static void carNavigation(Context context,Object owner,boolean active){
        if(context==null||owner==null)return;
        Runnable change=()->{
            if(active)carNavigation.add(owner);else carNavigation.remove(owner);
            navigating=phoneNavigating||!carNavigation.isEmpty();
            WazeAlerts.carNavigation(!carNavigation.isEmpty());
            refresh(context);
        };
        if(Looper.myLooper()==Looper.getMainLooper())change.run();else MAIN.post(change);
    }
    /** The original Maps Presentation supplies this real host-bound decor, never a phone mirror. */
    public static void carSurface(Object owner,ViewGroup root){carSurface(owner,root,root==null?0:root.getWidth(),root==null?0:root.getHeight());}
    public static void carSurface(Object owner,ViewGroup root,int width,int height){carSurface(owner,root,new android.graphics.Rect(0,0,width,height));}
    public static void carSurface(Object owner,ViewGroup root,android.graphics.Rect area){
        final android.graphics.Rect viewport=area==null?new android.graphics.Rect():new android.graphics.Rect(area);
        if(owner==null)return;Runnable change=()->{
            WeakReference<ViewGroup> old=carRoots.get(owner);
            if(old==null||old.get()!=root)removeCarWidget(owner);
            if(root==null){carRoots.remove(owner);carViewportSizes.remove(owner);}else{Shapes.hostedCarSurface(root);carRoots.put(owner,new WeakReference<>(root));carViewportSizes.put(owner,new int[]{viewport.left,viewport.top,viewport.right,viewport.bottom});}renderCar();
        };if(Looper.myLooper()==Looper.getMainLooper())change.run();else MAIN.post(change);
    }
    private static void removeCarWidget(Object owner){CompactSpeedView value=carWidgets.remove(owner);if(value!=null&&value.getParent() instanceof ViewGroup)((ViewGroup)value.getParent()).removeView(value);}
    private static void renderCar(){
        for(Object owner:new ArrayList<>(carRoots.keySet())){
            ViewGroup root=carRoots.get(owner).get();
            if(root==null||!carSessions.contains(owner)||!modern()||app==null||!widgetEnabled(app)){removeCarWidget(owner);continue;}
            int[] bounds=carViewportSizes.get(owner);
            if(bounds!=null&&(bounds[2]<=bounds[0]||bounds[3]<=bounds[1])){removeCarWidget(owner);continue;}
            TelemetryStore.View info=view();
            long now=SystemClock.elapsedRealtime();
            List<WarningEvent> nativeEvents=nativeWarnings.carEvents(now,event->{
                if(warningPreferences!=null&&!warningPreferences.enabled(event))return false;
                if(!nl.johninnl.navigation.alerts.WarningGeometry.pointDisplay(event))return true;
                WarningPassage.Relevance relevance=MapsGuidanceCoordinator.relevance(event,fix,now,navigating);
                return relevance!=WarningPassage.Relevance.PASSED&&relevance!=WarningPassage.Relevance.OFF_ROUTE;
            });
            List<WarningEvent> pills=new ArrayList<>();for(WarningEvent event:nativeEvents)if(!info.zoneActive||!nl.johninnl.navigation.alerts.CarWarningPills.zoneEvent(event))pills.add(event);
            String message=nativeEvents.isEmpty()?legacyWarning:"";
            if(!info.zoneActive&&message.isEmpty()&&pills.isEmpty()){removeCarWidget(owner);continue;}
            CompactSpeedView value=carWidgets.get(owner);if(value==null){value=new CompactSpeedView(root.getContext());carWidgets.put(owner,value);root.addView(value);}
            float density=root.getResources().getDisplayMetrics().density;
            boolean dark=(root.getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;
            android.widget.TextView number=largestNativeText(carSpeedBadge(root));
            value.carTypeface(number==null?null:number.getTypeface());
            java.util.Map<String,nl.johninnl.navigation.alerts.WarningGeometry> geometry=new java.util.HashMap<>();
            for(WarningEvent event:pills)geometry.put(event.warningId,MapsGuidanceCoordinator.displayGeometry(event,fix,now,navigating));
            value.carGeometry(geometry);
            value.bindCar(info,limitKmh(),PowerSaving.miles(),dark,message,pills);
            int left=bounds==null?0:bounds[0],top=bounds==null?0:bounds[1];
            int right=bounds!=null&&bounds[2]>left?bounds[2]:root.getWidth(),bottom=bounds!=null&&bounds[3]>top?bounds[3]:root.getHeight();
            android.graphics.Rect area=carInfoArea(root,new android.graphics.Rect(left,top,right,bottom));
            left=area.left;top=area.top;right=area.right;bottom=area.bottom;
            int width=Math.max(0,right-left),height=Math.max(0,bottom-top);
            int margin=Math.round(16*density);value.measure(View.MeasureSpec.makeMeasureSpec(Math.max(0,width-2*margin),View.MeasureSpec.AT_MOST),View.MeasureSpec.makeMeasureSpec(Math.max(0,height-2*margin),View.MeasureSpec.AT_MOST));
            if(value.getMeasuredWidth()<=0||value.getMeasuredHeight()<=0){removeCarWidget(owner);continue;}
            FrameLayout.LayoutParams layout=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.TOP|Gravity.START);
            layout.leftMargin=Math.max(left+margin,right-value.getMeasuredWidth()-margin);layout.topMargin=Math.max(top+margin,bottom-value.getMeasuredHeight()-margin);value.setLayoutParams(layout);value.bringToFront();
        }
    }

    static android.graphics.Rect carInfoArea(ViewGroup root,android.graphics.Rect viewport){
        return carInfoArea(viewport,occupiedCarView(root,carSpeedBadge(root)),
            occupiedCarView(root,"map_buttons_view_branding_watermark_container"),Math.round(16*root.getResources().getDisplayMetrics().density));
    }
    private static View carSpeedBadge(ViewGroup root){
        View wrapper=namedCarView(root,"map_buttons_view_speed_limit");
        if(wrapper==null)return null;
        return carSpeedCard(wrapper,namedCarView(wrapper,"speedometer"),namedCarView(wrapper,"speed_limit"));
    }
    static View carSpeedCard(View wrapper,View speed,View limit){
        // Stop at their shared card, before Maps' larger watermark container.
        for(View candidate=speed;candidate!=null&&candidate!=wrapper;candidate=parentView(candidate)){
            for(View child=limit;child!=null;child=parentView(child)){
                if(candidate==child)return candidate;
                if(child==wrapper)break;
            }
        }
        return wrapper;
    }
    private static View parentView(View view){return view.getParent() instanceof View?(View)view.getParent():null;}
    static android.graphics.Rect carInfoArea(android.graphics.Rect viewport,android.graphics.Rect speed,android.graphics.Rect watermark,int margin){
        android.graphics.Rect result=new android.graphics.Rect(viewport);
        if(speed!=null&&android.graphics.Rect.intersects(speed,viewport)){
            result.bottom=Math.min(result.bottom,speed.top);
            // renderCar subtracts its margin: the resulting pill ends exactly at the native badge's right edge.
            result.right=Math.min(result.right,speed.right+margin);
        }
        if(watermark!=null&&android.graphics.Rect.intersects(watermark,viewport))result.bottom=Math.min(result.bottom,watermark.top);
        return result;
    }
    private static android.graphics.Rect occupiedCarView(ViewGroup root,String name){
        return occupiedCarView(root,namedCarView(root,name));
    }
    static android.graphics.Rect occupiedCarView(ViewGroup root,View view){
        if(view==null||view.getVisibility()!=View.VISIBLE||view.getWidth()<=0||view.getHeight()<=0)return null;
        android.graphics.Rect bounds=new android.graphics.Rect();if(!view.getGlobalVisibleRect(bounds))return null;
        int[] origin=new int[2];root.getLocationOnScreen(origin);bounds.offset(-origin[0],-origin[1]);return bounds;
    }
    private static android.widget.TextView largestNativeText(View view){
        android.widget.TextView largest=view instanceof android.widget.TextView?(android.widget.TextView)view:null;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){
            android.widget.TextView candidate=largestNativeText(group.getChildAt(i));
            if(candidate!=null&&(largest==null||candidate.getTextSize()>largest.getTextSize()))largest=candidate;
        }}
        return largest;
    }
    private static View namedCarView(View view,String name){
        if(view.getId()!=View.NO_ID){try{if(name.equals(view.getResources().getResourceEntryName(view.getId())))return view;}catch(android.content.res.Resources.NotFoundException ignored){}}
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=namedCarView(group.getChildAt(i),name);if(found!=null)return found;}}
        return null;
    }
    private static String pinSignature="";

    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(Shapes.PREFS,Context.MODE_PRIVATE);}
    static String source(Context c){return prefs(c).getString(KEY_SOURCE,prefs(c).getBoolean(WazeAlerts.KEY_ON,false)?LEGACY:"");}
    static int sourceIndex(Context c){String value=source(c);for(int i=0;i<SOURCES.length;i++)if(SOURCES[i].equals(value))return i;return 0;}
    static boolean suppressNativeFlock(){return modern()&&store.suppressNativeFlock(SystemClock.elapsedRealtime());}
    static boolean legacy(){return LEGACY.equals(source);}
    static boolean modern(){return TelemetryStore.supported(source);}
    static boolean widgetEnabled(Context c){return prefs(c).getBoolean(KEY_WIDGET,true);}
    static boolean nativeMetricsEnabled(){return modern()&&app!=null&&widgetEnabled(app);}
    static void setWidget(Context c,boolean enabled){prefs(c).edit().putBoolean(KEY_WIDGET,enabled).apply();refresh(c);}
    static void select(Context c,String value){
        boolean allowed=false;for(String candidate:SOURCES)if(candidate.equals(value))allowed=true;
        if(!allowed)value="";
        prefs(c).edit().putString(KEY_SOURCE,value).putBoolean(WazeAlerts.KEY_ON,!value.isEmpty()).commit();
        refresh(c);WazeAlerts.refresh(c);SavedOnMap.refresh();
    }
    static void refresh(Context context){
        if(app!=context.getApplicationContext()&&startup!=null){startup.close();startup=null;if(semantic!=null){semantic.close();semantic=null;}if(nativeSpeaker!=null){nativeSpeaker.close();nativeSpeaker=null;}}
        app=context.getApplicationContext();
        String next=source(context);
        boolean changed=!next.equals(source);
        if(changed){source=next;warningPreferences=new WarningPreferences(prefs(app),true,WarningProviders.supported(source)?source:WarningProviders.FLITSMEISTER);store=new TelemetryStore();store.select(source);publisherSession="";warnings.reset();nativeWarnings.clear();eventAudioManaged=false;if(semantic!=null)semantic.resetEventNegotiation();warning="";legacyWarning="";WazeAlerts.resetSource();}
        if(startup==null){
            nativeSpeaker=new MapsWarningSpeaker(app);
            warningPreferences=new WarningPreferences(prefs(app),true,WarningProviders.supported(source)?source:WarningProviders.FLITSMEISTER);
            final MapsWarningSpeaker speaker=nativeSpeaker;
            scheduler=new WarningScheduler(new WarningScheduler.Policy(){
                public boolean active(){return warningPreferences.reviewed()&&WarningProviders.supported(source)&&(navigating||carActive()||foreground.get()!=null||startup!=null&&startup.activeSession());}
                public WarningAudioOwner.Mode mode(){return warningPreferences.mode();}
                public boolean enabled(WarningEvent event){return warningPreferences.enabled(event);}
                public boolean passed(WarningEvent event,long now){return MapsGuidanceCoordinator.passed(event,fix,now);}
                public nl.johninnl.navigation.alerts.WarningPassage.Relevance relevance(WarningEvent event,long now){return MapsGuidanceCoordinator.relevance(event,fix,now,navigating);}
            },new WarningScheduler.Speaker(){
                public void prepare(String language){speaker.prepare(language);}
                public void prepare(String language,WarningAudioOwner.Mode mode){speaker.prepare(language,mode);}
                public boolean ready(String language,WarningAudioOwner.Mode mode){return speaker.isReady(language,mode);}
                public WarningAudioOwner.Mode effective(WarningAudioOwner.Mode mode){return speaker.effectiveMode(mode);}
                public boolean speak(WarningEvent event,PlaybackGrant grant,long deadline,WarningScheduler.Callback callback){return speaker.speak(event,grant,deadline,new MapsWarningSpeaker.Listener(){public void started(PlaybackGrant value){callback.started(value);}public void finished(PlaybackGrant value,PlaybackGrant.Outcome outcome,boolean quiet){callback.finished(value,outcome,quiet);}});}
                public void cancel(PlaybackGrant grant){speaker.cancel(grant);}
            },new WarningScheduler.Observer(){
                public void changed(String value){if(value.equals("READY")||value.equals("USER_MUTED")||value.equals("STARTED"))eventAudioManaged=true;semanticRender();}
                public void outcome(WarningEvent event,PlaybackGrant.Outcome value){}
            });
            semantic=new nl.johninnl.navigation.alerts.WarningSession(app,new nl.johninnl.navigation.alerts.WarningConnection.Listener(){
                public void snapshot(nl.johninnl.navigation.alerts.WarningConnection.Snapshot value){nl.johninnl.navigation.alerts.WarningSession current=semantic;if(current!=null&&current.consume(value.providerSession,()->{nativeWarnings.snapshot(value);scheduler.connected(current.connection(),value);}))semanticRender();}
                public void events(java.util.List<nl.johninnl.navigation.alerts.WarningEvent> values){nl.johninnl.navigation.alerts.WarningSession current=semantic;if(!values.isEmpty()&&current!=null&&current.consume(values.get(0).providerSession,()->{nativeWarnings.events(values);scheduler.events(current.connection(),values);}))semanticRender();}
                public void ownerRevoked(long epoch){scheduler.revoked(epoch);}
                public void disconnected(String reason){nativeWarnings.clear();scheduler.disconnected();semanticRender();}
            });
            startup=new nl.johninnl.navigation.startup.NavigationClient(app,semantic);
        }
        startup.select(source,navigating||carActive());
        if(modern()&&!registered){
            IntentFilter filter=new IntentFilter(ACTION);
            if(Build.VERSION.SDK_INT>=33)app.registerReceiver(RECEIVER,filter,Context.RECEIVER_EXPORTED);else app.registerReceiver(RECEIVER,filter);
            registered=true;request();MAIN.removeCallbacks(TICK);MAIN.post(TICK);
        }else if(!modern()&&registered){app.unregisterReceiver(RECEIVER);registered=false;MAIN.removeCallbacks(TICK);removeWidget();}
        if(changed&&registered)request();
        update();
    }
    private static final BroadcastReceiver RECEIVER=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent intent){
            if(!ACTION.equals(intent.getAction()))return;
            try{
                Bundle extras=intent.getExtras();if(extras==null)return;
                String sender=Build.VERSION.SDK_INT>=34?getSentFromPackage():intent.getStringExtra("source_package");
                if(Build.VERSION.SDK_INT>=34&&(sender==null||!TelemetryStore.supported(sender)||getSentFromUid()!=c.getPackageManager().getApplicationInfo(sender,0).uid))return;
                Map<String,Object> values=new HashMap<>();for(String key:extras.keySet())values.put(key,extras.get(key));
                if(store.accept(values,sender,SystemClock.elapsedRealtime())){
                    if(source.equals(sender)){
                        String session=intent.getStringExtra("publisher_session_id");
                        if(!session.equals(publisherSession)){publisherSession=session;warnings.reset();}
                    }
                    update();
                }
            }catch(Exception ignored){ /* Reject malformed extras and unknown framework senders. */ }
        }
    };
    static void request(){
        if(app==null||!modern())return;
        app.sendBroadcast(new Intent(REQUEST).setPackage(source).putExtra("from",app.getPackageName()).addFlags(Intent.FLAG_RECEIVER_FOREGROUND));
    }
    static TelemetryStore.View view(){return store.view(SystemClock.elapsedRealtime());}
    static Float speedMps(){Double external=view().currentKmh;if(external!=null)return (float)(external/3.6);if(freshNative(nativeSpeedAt,5000)&&nativeSpeed!=null)return nativeSpeed.floatValue()/3.6f;return null;}
    static Double limitKmh(){Double external=view().limitKmh;return external!=null?external:freshNative(nativeLimitAt,30000)?nativeLimit:null;}
    private static boolean freshNative(long at,long age){long now=SystemClock.elapsedRealtime();return at>=0&&now>=at&&now-at<=age;}
    static void nativeSpeedMps(float speed) {
        nativeSpeedAt=SystemClock.elapsedRealtime();
        nativeSpeed=Float.isFinite(speed)&&speed>=0&&speed<=500/3.6?speed*3.6:null;
    }
    static void nativeSpeed(int speed,boolean kmh){nativeSpeedAt=SystemClock.elapsedRealtime();nativeSpeed=speed>=0&&speed*(kmh?1:1.609344)<=500?speed*(kmh?1.0:1.609344):null;}
    static void nativeLimit(int limit,boolean kmh){nativeLimitAt=SystemClock.elapsedRealtime();nativeLimit=limit>0&&limit*(kmh?1:1.609344)<=500?limit*(kmh?1.0:1.609344):null;}
    static void nativeSpeedometer(int speed,int limit,boolean kmh){nativeSpeed(speed,kmh);nativeLimit(limit,kmh);}

    static void fix(Location value){
        if(value==null)return;long now=SystemClock.elapsedRealtime(),at=value.getElapsedRealtimeNanos()/1000000;
        if(at<0||at<fixAt||at>now||now-at>10000||!Double.isFinite(value.getLatitude())||Math.abs(value.getLatitude())>90||!Double.isFinite(value.getLongitude())||Math.abs(value.getLongitude())>180||(value.hasAccuracy()&&value.getAccuracy()>50))return;
        fix=new Location(value);fixAt=at;
        // A native GPS speed is a fallback only; never infer stationary zero from a missing speed.
        if(value.hasSpeed()&&Float.isFinite(value.getSpeed())&&value.getSpeed()>=0&&value.getSpeed()*3.6<=500){nativeSpeed=value.getSpeed()*3.6;nativeSpeedAt=at;}
        update();
    }
    static void navigating(boolean active){phoneNavigating=active;navigating=active||!carNavigation.isEmpty();if(startup!=null)startup.select(source,navigating||carActive());update();}
    static String provider(){return WarningProviders.supported(source)?WarningProviders.displayName(source):LEGACY.equals(source)?"Waze":"Google Maps";}
    static String status(){
        if(!modern())return LEGACY.equals(source)?WazeAlerts.legacyStatus():"Off · Google Maps";
        TelemetryStore.View v=view();
        boolean dutch=app!=null&&app.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("nl");
        String out=nl.johninnl.navigation.startup.NavigationHealth.warnings(provider(),
            semantic==null?"off":semantic.state(),semantic!=null&&semantic.advertisesEvents(source),
            semantic!=null&&semantic.captureComplete(),startup==null?"off":startup.state(),dutch)
            +"\n"+nl.johninnl.navigation.startup.NavigationHealth.service(provider(),startup==null?"off":startup.state(),dutch)
            +"\n"+nl.johninnl.navigation.startup.NavigationHealth.data(provider(),"Google Maps",v.external,v.currentKmh==null||v.limitKmh==null,dutch);
        if(WarningProviders.supported(source)&&warningPreferences!=null)out+="\n"+warningAudioStatus();
        if(v.zoneActive)out+="\n"+(v.averageKmh==null?"Observed partial average pending":String.format(Locale.getDefault(),"Observed partial average %.0f km/h",v.averageKmh))+String.format(Locale.getDefault()," · %s%.0f%% · %s remaining",v.progressEstimated?"≈":"",v.progressPercent,distance(v.remainingMeters,PowerSaving.miles()));
        else if(v.approachDistanceMeters!=null)out+="\nAverage-speed zone "+(v.approachEstimated?"≈":"")+distance(v.approachDistanceMeters,PowerSaving.miles())+" ahead";
        return out;
    }
    static String distance(double meters,boolean imperial){
        if(imperial){double miles=meters/1609.344;return miles>=.1?String.format(Locale.getDefault(),"%.1f mi",miles):Math.round(meters*3.28084)+" ft";}
        return meters>=1000?String.format(Locale.getDefault(),"%.1f km",meters/1000):Math.round(meters)+" m";
    }
    static String compactWarning(){return warning;}
    static String compactCarWarning(){
        String messages=nativeWarnings.carMessages(SystemClock.elapsedRealtime(),event->warningPreferences==null||warningPreferences.enabled(event));
        return messages.isEmpty()?legacyWarning:provider()+"\n"+messages;
    }
    static int kind(String kind){
        switch(kind){case "police":return WazeAlerts.POLICE;case "crash":return WazeAlerts.CRASH;case "hazard":return WazeAlerts.HAZARD;case "jam":return WazeAlerts.JAM;case "closure":return WazeAlerts.CLOSURE;case "camera":case "mobile_camera":return WazeAlerts.CAMERA;case "average_speed_zone":return WazeAlerts.ZONE;case "flock":return WazeAlerts.FLOCK;default:return WazeAlerts.OTHER;}
    }
    static List<WazeAlerts.Alert> mapAlerts(){
        List<WazeAlerts.Alert> out=new ArrayList<>();
        for(TelemetryStore.RoadAlert a:view().alerts)if(Double.isFinite(a.latitude)&&Double.isFinite(a.longitude))out.add(new WazeAlerts.Alert(source+":"+publisherSession+":"+a.kind+":"+a.id,kind(a.kind),a.latitude,a.longitude,a.triggerMeters,a.title,"From "+provider(),true));
        return out;
    }
    private static void update(){
        SpeedAlerts.refreshNativeWidgets();
        if(!modern()){render();return;}
        if(startup!=null)startup.select(source,navigating||carActive());
        TelemetryStore.View v=view();RoadWarningEngine.Fix position=fix==null?null:new RoadWarningEngine.Fix(fix.getLatitude(),fix.getLongitude(),fix.hasBearing()?fix.getBearing():Double.NaN,fix.hasSpeed()?fix.getSpeed():Double.NaN,fixAt);
        // A kind disabled for map pins is disabled for warnings too.
        RoadWarningEngine.Warning ahead=warnings.update(filteredView(v),position,SystemClock.elapsedRealtime());
        warning=ahead==null?"":(ahead.estimated?"≈":"")+distance(ahead.distanceMeters,PowerSaving.miles())+" · "+warningLabel(ahead.kind);
        legacyWarning=warning;
        nl.johninnl.navigation.alerts.WarningEvent nativeWarning=nativeWarnings.current(SystemClock.elapsedRealtime(),event->warningPreferences==null||warningPreferences.enabled(event));
        if(nativeWarning!=null)warning=provider()+"\n"+nativeWarnings.messages(SystemClock.elapsedRealtime(),event->warningPreferences==null||warningPreferences.enabled(event));
        if(semantic!=null&&semantic.advertisesEvents(source))eventAudioManaged=true;
        boolean legacyAudio=!eventAudioManaged;
        if(ahead!=null&&legacyAudio&&WazeAlerts.CHIME&&(navigating||carActive()||foreground.get()!=null)&&warnings.shouldSpeak(ahead,SystemClock.elapsedRealtime())){
            Chime.alert();if(app!=null)Toast.makeText(app,warningLabel(ahead.kind)+" "+(ahead.estimated?"≈":"")+distance(ahead.distanceMeters,PowerSaving.miles())+" ahead",Toast.LENGTH_LONG).show();
        }
        if(navigating||carActive()||foreground.get()!=null)SpeedAlerts.onTelemetry();
        StringBuilder signature=new StringBuilder(source).append(':').append(publisherSession).append(':').append(suppressNativeFlock());
        for(TelemetryStore.RoadAlert a:v.alerts)signature.append('|').append(a.id).append(':').append(a.kind).append(':').append(a.latitude).append(':').append(a.longitude).append(':').append(a.title);
        String pins=signature.toString();if(!pins.equals(pinSignature)){pinSignature=pins;SavedOnMap.refresh();}
        render();
    }
    private static TelemetryStore.View filteredView(TelemetryStore.View original){
        return original.withAlerts(alert->(WazeAlerts.KINDS&(1<<kind(alert.kind)))!=0,(WazeAlerts.KINDS&(1<<WazeAlerts.ZONE))!=0);
    }
    static WarningPreferences warningPreferences(Context context){String selected=source(context);return warningPreferences(context,WarningProviders.supported(selected)?selected:WarningProviders.FLITSMEISTER);}
    static WarningPreferences warningPreferences(Context context,String provider){return new WarningPreferences(prefs(context),true,provider);}
    static void warningChoicesChanged(){if(scheduler!=null)scheduler.refresh();update();}
    static void previewWarning(Activity activity){
        boolean dutch=activity.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("nl");
        boolean parked=!navigating&&!(freshNative(nativeSpeedAt,5000)&&nativeSpeed!=null&&nativeSpeed>1);
        nl.johninnl.navigation.alerts.WarningPreviewDialog.show(activity,parked,dutch,()->{
            if(scheduler==null||semantic==null||semantic.connection()==null||!semantic.connection().supportsPreview())return java.util.concurrent.CompletableFuture.completedFuture("INCOMPATIBLE");
            return scheduler.preview(dutch?"nl":"en");
        });
    }
    static void recoverWarningAudio(Activity activity){
        android.app.PendingIntent action=semantic==null?null:semantic.recoveryAction();
        if(action==null){Toast.makeText(activity,"Connect an updated "+provider()+" patch first",Toast.LENGTH_LONG).show();return;}
        try{action.send();}catch(android.app.PendingIntent.CanceledException error){Toast.makeText(activity,"Recovery is unavailable; reconnect "+provider(),Toast.LENGTH_LONG).show();}
    }
    static String warningAudioStatus(){if(warningPreferences==null||!warningPreferences.reviewed())return "Review warning categories and output";WarningAudioOwner.Mode requested=warningPreferences.mode();return WarningStatus.output(requested,nativeSpeaker==null?requested:nativeSpeaker.effectiveMode(requested),"Google Maps",false)+" · "+WarningStatus.label(scheduler==null?"OFF":scheduler.status(),false);}
    private static String warningLabel(String kind){switch(kind){case "average_speed_zone":return "Average-speed zone";case "mobile_camera":return "Mobile camera";case "camera":return "Speed camera";case "police":return "Police";case "closure":return "Road closure";case "jam":return "Traffic jam";case "flock":return "Flock camera";case "crash":return "Crash";default:return "Hazard";}}
    private static final Runnable TICK=new Runnable(){public void run(){if(!registered)return;update();MAIN.postDelayed(this,1000);}};
    static void resume(Activity activity){
        if(startup!=null)startup.onActivityResumed(activity);
        if(activity.getClass().getName().equals("com.google.android.maps.MapsActivity")){foreground=new WeakReference<>(activity);request();render();}
    }
    static void pause(Activity activity){if(startup!=null)startup.onActivityPaused(activity);if(foreground.get()==activity){foreground.clear();removeWidget();}}
    private static void removeWidget(){
        if(widget!=null&&widget.getParent() instanceof ViewGroup)((ViewGroup)widget.getParent()).removeView(widget);
        widget=null;
    }
    private static void render(){
        renderCar();
        Activity activity=foreground.get();if(!modern()||activity==null||!widgetEnabled(activity)){removeWidget();return;}
        if(!view().zoneActive&&compactWarning().isEmpty()){removeWidget();return;}
        ViewGroup content=activity.findViewById(android.R.id.content);if(!(content instanceof FrameLayout))return;
        if(widget==null){widget=new CompactSpeedView(activity);widget.setOnClickListener(v->activity.startActivity(Screens.intent(activity,CustomizationActivity.class).putExtra(CustomizationActivity.EXTRA_PAGE,CustomizationActivity.PAGE_WAZE)));content.addView(widget);}
        float density=activity.getResources().getDisplayMetrics().density;
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.BOTTOM|Gravity.START);
        lp.leftMargin=Math.round(16*density);lp.bottomMargin=Math.round(112*density);
        View badge=Shapes.findSpeedBadge(content);
        if(badge!=null){View card=Shapes.badgeCard(badge,content);int[] location=new int[2],origin=new int[2];card.getLocationOnScreen(location);content.getLocationOnScreen(origin);lp.leftMargin=Math.max(0,location[0]-origin[0]);lp.bottomMargin=Math.max(Math.round(24*density),content.getHeight()-(location[1]-origin[1])+Math.round(8*density));}
        widget.setLayoutParams(lp);widget.refresh(Shapes.navDarkScheme());widget.bringToFront();
    }
}
