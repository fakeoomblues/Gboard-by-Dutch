package patches.maps.gearslip;

import static org.junit.Assert.*;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32)
public final class NativeCarContentTest {
    private static final int HOME=0x7f990020,PRIMARY=0x7f990021;
    private final Object first=new Object(),second=new Object();
    private Context context;
    private FrameLayout root;
    private NativeControls controls;
    private NativeCarContent content,replacement;

    public static final class Place {
        final String title;int clicks;
        Place(String title){this.title=title;}
        public CharSequence f(){return title;}
        public CharSequence g(){return "5 min";}
        public CharSequence h(){return "2 km";}
        public void i(){clicks++;}
    }
    public static final class Suggestions { public List<Place> a(){return values;}List<Place> values=new ArrayList<>(); }
    public static final class Home { public Suggestions l=new Suggestions(); }
    public static final class Recent {
        int clicks;
        public String h(){return "Destination";}public String i(){return "5 min";}public String g(){return "2 km";}public void j(){clicks++;}
    }
    public static final class Recents {public List<Recent> d(){return Collections.singletonList(value);}Recent value=new Recent();}
    public static final class Preview {
        int starts;
        public CharSequence l(){return "Start";}
        public boolean K(){return true;}
        public void r(){starts++;}
    }

    @Before public void setup(){
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();Resources original=activity.getResources();
        Resources resources=new Resources(activity.getAssets(),original.getDisplayMetrics(),original.getConfiguration()){
            @Override public String getResourceEntryName(int id){if(id==HOME)return "main_suggested_destinations_card";if(id==PRIMARY)return "place_details_navigation_footer_action_button";return super.getResourceEntryName(id);}
        };
        context=new ContextWrapper(activity){@Override public Resources getResources(){return resources;}};
        root=new FrameLayout(context);FrameLayout card=new FrameLayout(context);card.setId(HOME);root.addView(card,new FrameLayout.LayoutParams(400,300));activity.setContentView(root);
        root.measure(View.MeasureSpec.makeMeasureSpec(1280,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY));root.layout(0,0,1280,480);
        controls=new NativeControls(root,()->{});
        LocalCarUiSession.attach(first,intent->{},(text,duration)->{});content=new NativeCarContent(first,context,()->{});
    }
    @After public void cleanup(){if(content!=null)content.close(first);if(replacement!=null)replacement.close(second);controls.close();LocalCarUiSession.detach(first);LocalCarUiSession.detach(second);}
    @Test public void nativeReaderExportsTheWholeLogicalListAndOriginalPrimaryCallback()throws Exception{
        Home home=new Home();for(int i=0;i<80;i++)home.l.values.add(new Place("Place "+i));
        List<NativeCarContent.Row> rows=NativeCarContent.readHome(home);assertEquals(80,rows.size());
        assertEquals("Place 79",rows.get(79).title);assertEquals(java.util.Arrays.asList("5 min","2 km"),rows.get(79).texts);
        rows.get(79).action.run();assertEquals(1,home.l.values.get(79).clicks);
        Recents recents=new Recents();NativeCarContent.readRecents(recents).get(0).action.run();assertEquals(1,recents.value.clicks);
    }
    @Test public void anOldNativeReadCannotPublishItsListIntoAReplacementHost()throws Exception{
        Object oldSource=new Object();List<Place> old=Collections.singletonList(new Place("Old"));
        NativeCarScopes.producer(oldSource,first);
        NativeCarContent.readingSuggestions(oldSource);
        LocalCarUiSession.attach(second,intent->{},(text,duration)->{});replacement=new NativeCarContent(second,context,()->{});
        NativeCarContent.suggestions(old);Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertNull(replacement.read(root,controls,false,null));
        Object currentSource=new Object();List<Place> current=Collections.singletonList(new Place("Current"));
        NativeCarScopes.producer(currentSource,second);
        NativeCarContent.readingSuggestions(currentSource);NativeCarContent.suggestions(current);
        NativeCarContent.Page page=replacement.read(root,controls,false,null);assertNotNull(page);assertEquals("Current",page.rows.get(0).title);
        NativeCarContent.readingSuggestions(oldSource);NativeCarContent.suggestions(old);
        assertEquals("Current",replacement.read(root,controls,false,null).rows.get(0).title);
    }
    @Test public void aRemovedNativeRowRevokesTheOldHostClick()throws Exception{
        Object source=new Object();Place firstPlace=new Place("First"),nextPlace=new Place("Next");
        NativeCarScopes.producer(source,first);
        List<Place> firstValues=Collections.singletonList(firstPlace);NativeCarContent.readingSuggestions(source);NativeCarContent.suggestions(firstValues);
        NativeCarContent.Action old=content.read(root,controls,false,null).rows.get(0).action;
        content.invoke(old,root,controls,false,null);assertEquals(1,firstPlace.clicks);
        List<Place> nextValues=Collections.singletonList(nextPlace);NativeCarContent.readingSuggestions(source);NativeCarContent.suggestions(nextValues);
        assertThrows(SecurityException.class,()->content.invoke(old,root,controls,false,null));assertEquals(1,firstPlace.clicks);
        content.invoke(content.read(root,controls,false,null).rows.get(0).action,root,controls,false,null);assertEquals(1,nextPlace.clicks);
    }
    @Test public void hostPreviewRetainsOneOriginalStartAndAllSecondaryCallbacks()throws Exception{
        FrameLayout panel=new FrameLayout(context);TextView destination=new TextView(context);destination.setText("Amsterdam");panel.addView(destination);
        TextView start=new TextView(context);start.setId(PRIMARY);start.setText("Start");start.setContentDescription("Start");start.setOnClickListener(v->fail("The duplicate view action must not replace native eligibility/Start"));panel.addView(start);
        int[] options={0};View routeOptions=new View(context);routeOptions.setContentDescription("Route options");routeOptions.setOnClickListener(v->options[0]++);panel.addView(routeOptions);
        root.addView(panel,new FrameLayout.LayoutParams(400,300));Preview source=new Preview();
        NativeCarContent.Page page=content.preview(source,panel,controls);
        assertEquals("Amsterdam",page.title);assertEquals(1,page.rows.size());assertEquals(2,page.actions.size());
        assertEquals("Start",page.actions.get(0).title);assertEquals("Route options",page.actions.get(1).title);
        page.actions.get(0).run();page.actions.get(1).run();assertEquals(1,source.starts);assertEquals(1,options[0]);
    }
}
