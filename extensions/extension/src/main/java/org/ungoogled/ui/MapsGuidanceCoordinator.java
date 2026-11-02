package org.ungoogled.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import nl.johninnl.navigation.alerts.WarningAudioOwner;
import nl.johninnl.navigation.alerts.WarningEvent;

/** Native Maps queue hold. Never writes mute preferences or stream volume. */
public final class MapsGuidanceCoordinator {
    public interface Ready { void complete(boolean quiet,String reason); }
    private static final Object LOCK=new Object();
    private static final Map<Object,Object> PLAYERS=new IdentityHashMap<>();
    private static WeakReference<Object> route=new WeakReference<>(null);
    private static MapsGuidanceCoordinator current;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Context app;
    private Object manager;
    private volatile boolean bound,fault,holding;
    private long generation;
    private volatile boolean regenerate;
    private String holdToken;

    public MapsGuidanceCoordinator(Context context) {
        app=context.getApplicationContext();synchronized(LOCK){current=this;}
        main.post(this::bind);
    }
    /** Patched true only after every native speech guard has passed. */
    private static boolean hooksInstalled(){return false;}
    private void bind() {
        try {
            Object component=method(app.getClass(),"rJ").invoke(app);
            Object provider=field(component.getClass(),"hU").get(component);
            Object actual=method(provider.getClass(),"a").invoke(provider);
            if(!actual.getClass().getName().equals("bmrc"))throw new IllegalStateException("NATIVE_MANAGER");
            manager=actual;bound=true;
        }catch(ReflectiveOperationException | RuntimeException error){fault=true;}
    }
    public boolean isReady(){return hooksInstalled()&&bound&&!fault;}
    public WarningAudioOwner.Mode effectiveMode(WarningAudioOwner.Mode requested) {
        if(requested==WarningAudioOwner.Mode.SILENT)return requested;
        if(!isReady())return WarningAudioOwner.Mode.SILENT;
        try {
            Object mute=field(manager.getClass(),"f").get(manager);
            Object value=method(mute.getClass(),"c").invoke(mute);
            String nativeMode=((Enum<?>)value).name();
            if(nativeMode.equals("MUTED"))return WarningAudioOwner.Mode.SILENT;
            // MINIMAL tone routing has not been proven; do not promote it to semantic Voice.
            if(nativeMode.equals("MINIMAL"))return requested==WarningAudioOwner.Mode.VOICE?WarningAudioOwner.Mode.TONE:requested;
            return requested;
        }catch(ReflectiveOperationException | RuntimeException error){fault=true;return WarningAudioOwner.Mode.SILENT;}
    }
    public void acquire(String token,WarningEvent.Priority priority,long deadline,Ready callback) {
        main.post(()->acquireMain(token,priority,deadline,callback));
    }
    private void acquireMain(String token,WarningEvent.Priority priority,long deadline,Ready callback) {
        if(!isReady() || holding || SystemClock.elapsedRealtime()>=deadline){callback.complete(false,"GUIDANCE_UNAVAILABLE");return;}
        holding=true;holdToken=token;regenerate=false;long version=++generation;
        try {
            Executor serial=(Executor)field(manager.getClass(),"j").get(manager);
            serial.execute(()-> {
                try {
                    Object active=field(manager.getClass(),"p").get(manager),pending=field(manager.getClass(),"q").get(manager);
                    main.post(()-> {
                        if(version!=generation || !holding)return;
                        if(active!=null || pending!=null) {
                            if(priority!=WarningEvent.Priority.P0 || !canRegenerate()) { abandon(version,callback,"GUIDANCE_BUSY");return; }
                            regenerate=true;
                            try {
                                if(active!=null)cancel(active);
                                if(pending!=null)cancel(pending);
                            }catch(ReflectiveOperationException | RuntimeException error){fault=true;abandon(version,callback,"GUIDANCE_CANCEL_FAILED");return;}
                        }
                        awaitQuiet(version,deadline,callback);
                    });
                }catch(ReflectiveOperationException | RuntimeException error){main.post(()->{fault=true;abandon(version,callback,"GUIDANCE_STATE_FAILED");});}
            });
        }catch(ReflectiveOperationException | RuntimeException error){fault=true;abandon(version,callback,"GUIDANCE_EXECUTOR_FAILED");}
    }
    private void cancel(Object job) throws ReflectiveOperationException {
        Class<?> jobInterface=Class.forName("bmud",false,app.getClassLoader());
        method(manager.getClass(),"l",jobInterface).invoke(manager,job);
    }
    private void awaitQuiet(long version,long deadline,Ready callback) {
        if(version!=generation || !holding)return;
        boolean quiet=true;
        synchronized(LOCK) {
            for(Object player:PLAYERS.values())try { if(!released(player,0))quiet=false; }
            catch(ReflectiveOperationException | RuntimeException error){fault=true;abandon(version,callback,"GUIDANCE_PLAYER_UNSUPPORTED");return;}
        }
        if(quiet){callback.complete(true,"QUIESCENT");return;}
        if(SystemClock.elapsedRealtime()>=deadline){abandon(version,callback,"GUIDANCE_DEADLINE");return;}
        main.postDelayed(()->awaitQuiet(version,deadline,callback),10);
    }
    private void abandon(long version,Ready callback,String reason) {
        if(version!=generation)return;releaseMain(holdToken);callback.complete(false,reason);
    }
    public void release(String token){main.post(()->releaseMain(token));}
    private void releaseMain(String token) {
        if(!holding || token==null || !token.equals(holdToken))return;
        holding=false;holdToken=null;generation++;
        boolean restore=regenerate;regenerate=false;
        if(restore)regenerateCurrent();
    }
    private boolean canRegenerate(){synchronized(LOCK){Object owner=route.get();if(owner==null)return false;try{return field(owner.getClass(),"o").get(owner)!=null&&field(owner.getClass(),"e").get(owner)!=null;}catch(ReflectiveOperationException error){return false;}}}
    /** Read current route and step at release. No captured turn text or stale ybc is replayed. */
    private void regenerateCurrent() {
        Object owner;synchronized(LOCK){owner=route.get();}if(owner==null)return;
        try {
            Object step=field(owner.getClass(),"o").get(owner),model=field(owner.getClass(),"e").get(owner);
            if(step!=null && model!=null)method(owner.getClass(),"u",Class.forName("ybc",false,app.getClassLoader())).invoke(owner,step);
        }catch(ReflectiveOperationException | RuntimeException error){fault=true;}
    }
    public void close(){main.post(()->{releaseMain(holdToken);synchronized(LOCK){if(current==this)current=null;}});}
    public static void captureRoute(Object owner){synchronized(LOCK){route=new WeakReference<>(owner);}}
    public static void routeEnded(Object owner){synchronized(LOCK){if(route.get()==owner)route.clear();}}

    /** Exact final bmuc.d boundary after asynchronous focus/readiness. Native unheld behavior stays intact. */
    public static void dispatch(Object job,Object player,Object callback) {
        MapsGuidanceCoordinator coordinator;
        synchronized(LOCK) {
            coordinator=current;
            if(PLAYERS.size()>=128 && !PLAYERS.containsKey(job)) { if(coordinator!=null)coordinator.fault=true; }
            else PLAYERS.put(job,player);
        }
        try {
            if(coordinator!=null && coordinator.holding) {
                coordinator.regenerate=true;
                ((AtomicBoolean)field(job.getClass(),"s").get(job)).set(true);
                // A not-yet-dispatched prepared player has no native completion listener installed.
                // Release its resources, then use the original never-played queue completion path.
                method(player.getClass(),"l").invoke(player);
                method(job.getClass(),"e",Class.forName("bmuc",false,job.getClass().getClassLoader())).invoke(job,player);
                synchronized(LOCK) {
                    try { if(released(player,0))PLAYERS.remove(job); }
                    catch(ReflectiveOperationException | RuntimeException error){coordinator.fault=true;}
                }
                return;
            }
            method(player.getClass(),"d",Class.forName("bmua",false,player.getClass().getClassLoader())).invoke(player,callback);
        }catch(InvocationTargetException error) {
            Throwable cause=error.getCause();if(cause instanceof RuntimeException)throw (RuntimeException)cause;if(cause instanceof Error)throw (Error)cause;throw new IllegalStateException(cause);
        }catch(ReflectiveOperationException error){if(coordinator!=null)coordinator.fault=true;throw new IllegalStateException("Maps native dispatch ABI",error);}
    }
    /** Original native completion remains installed; observe before it advances the queue. */
    public static void terminal(Object nativeListener,Object player) {
        try {
            Object job=field(nativeListener.getClass(),"a").get(nativeListener);
            MapsGuidanceCoordinator coordinator;
            synchronized(LOCK) {
                coordinator=current;
                if(released(player,0))PLAYERS.remove(job);
                else if(coordinator!=null)coordinator.main.post(()->pruneReleased(job,player));
            }
        }
        catch(ReflectiveOperationException error){synchronized(LOCK){if(current!=null)current.fault=true;}}
        catch(RuntimeException error){synchronized(LOCK){if(current!=null)current.fault=true;}}
    }
    private static void pruneReleased(Object job,Object player) {
        synchronized(LOCK) {
            try { if(PLAYERS.get(job)==player && released(player,0))PLAYERS.remove(job); }
            catch(ReflectiveOperationException | RuntimeException error){if(current!=null)current.fault=true;}
        }
    }
    private static boolean released(Object player,int depth) throws ReflectiveOperationException {
        if(depth>8)throw new IllegalArgumentException("PLAYER_DEPTH");
        switch(player.getClass().getName()) {
            case "bmsk":return field(player.getClass(),"a").get(player)==null;
            case "bmtu":Object leaf=field(player.getClass(),"a").get(player);return leaf==null || released(leaf,depth+1);
            case "bmsh":for(Object leafPlayer:(Object[])field(player.getClass(),"a").get(player))if(!released(leafPlayer,depth+1))return false;return true;
            case "bmsi":return true; // Native non-audio silence timer.
            default:throw new IllegalArgumentException("PLAYER_UNSUPPORTED");
        }
    }
    static Field field(Class<?> type,String name) throws NoSuchFieldException {
        for(Class<?> owner=type;owner!=null;owner=owner.getSuperclass())try { Field f=owner.getDeclaredField(name);f.setAccessible(true);return f; }catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(type.getName()+"."+name);
    }
    static Method method(Class<?> type,String name,Class<?>...parameters) throws NoSuchMethodException {
        for(Class<?> owner=type;owner!=null;owner=owner.getSuperclass())try { Method m=owner.getDeclaredMethod(name,parameters);m.setAccessible(true);return m; }catch(NoSuchMethodException ignored){}
        throw new NoSuchMethodException(type.getName()+"."+name);
    }
}
