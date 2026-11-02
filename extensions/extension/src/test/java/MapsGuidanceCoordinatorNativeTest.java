import static org.junit.Assert.*;
import android.content.Context;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.ungoogled.ui.MapsGuidanceCoordinator;

/** Native-shape fixtures verify fencing side effects; these do not establish original-app audio. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=32,manifest=Config.NONE) @LooperMode(LooperMode.Mode.PAUSED)
public class MapsGuidanceCoordinatorNativeTest {
    MapsGuidanceCoordinator coordinator;
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    @Before public void setup()throws Exception{coordinator=new MapsGuidanceCoordinator(RuntimeEnvironment.getApplication());players().clear();}
    @After public void cleanup()throws Exception{players().clear();field(MapsGuidanceCoordinator.class,"current").set(null,null);}
    @SuppressWarnings("unchecked") Map<Object,Object> players()throws Exception{return (Map<Object,Object>)field(MapsGuidanceCoordinator.class,"PLAYERS").get(null);}
    @Test public void ordinaryNativeDispatchKeepsOriginalCallbackAndDoesNotInventStart()throws Exception{
        bmtl job=new bmtl();bmsk player=new bmsk();bmua callback=new bmua(){};
        MapsGuidanceCoordinator.dispatch(job,player,callback);
        assertEquals(1,player.plays);assertSame(callback,player.callback);assertEquals(0,player.stops);assertFalse(job.s.get());assertEquals(0,job.neverPlayed);
        assertSame(player,players().get(job));
    }
    @Test public void holdRejectsLateAsyncDispatchAndReleasesPreparedMediaBeforeNativeCompletion()throws Exception{
        field(MapsGuidanceCoordinator.class,"holding").setBoolean(coordinator,true);
        bmtl job=new bmtl();bmsk player=new bmsk();job.player=player;
        MapsGuidanceCoordinator.dispatch(job,player,new bmua(){});
        assertTrue(job.s.get());assertEquals(0,player.plays);assertEquals(1,player.stops);assertNull(player.a);assertEquals(1,job.neverPlayed);assertTrue(job.releasedWhenCompleted);assertFalse(players().containsKey(job));
    }
    @Test public void nativeDispatchExceptionIsPreservedWithoutFakingCompletion()throws Exception{
        bmtl job=new bmtl();bmsk player=new bmsk();player.fail=true;
        try{MapsGuidanceCoordinator.dispatch(job,player,new bmua(){});fail("native play failure");}catch(IllegalStateException expected){assertEquals("native failure",expected.getMessage());}
        assertEquals(0,job.neverPlayed);assertEquals(1,player.plays);assertSame(player,players().get(job));
    }
    @Test public void nativeTerminalObserverRemovesOnlyTheCompletedJob()throws Exception{
        bmtl first=new bmtl(),second=new bmtl();bmsk a=new bmsk(),b=new bmsk();
        MapsGuidanceCoordinator.dispatch(first,a,new bmua(){});MapsGuidanceCoordinator.dispatch(second,b,new bmua(){});
        MapsGuidanceCoordinator.terminal(new bmth(first),a);assertSame(a,players().get(first));
        a.l();MapsGuidanceCoordinator.terminal(new bmth(first),a);assertFalse(players().containsKey(first));assertSame(b,players().get(second));assertEquals(0,first.neverPlayed);assertEquals(0,second.neverPlayed);
    }
}
interface bmua{}
interface bmuc{void d(bmua callback);void l();}
final class bmsk implements bmuc {
    Object a=new Object();int plays,stops;boolean fail;bmua callback;
    public void d(bmua callback){plays++;this.callback=callback;if(fail)throw new IllegalStateException("native failure");}
    public void l(){stops++;a=null;}
}
final class bmtl {
    final AtomicBoolean s=new AtomicBoolean();int neverPlayed;boolean releasedWhenCompleted;bmsk player;
    public void e(bmuc ignored){neverPlayed++;releasedWhenCompleted=player==null||player.a==null;}
}
final class bmth {final bmtl a;bmth(bmtl job){a=job;}}
