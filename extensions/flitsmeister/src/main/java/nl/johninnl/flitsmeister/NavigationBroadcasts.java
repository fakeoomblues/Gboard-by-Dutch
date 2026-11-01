package nl.johninnl.flitsmeister;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import java.util.List;
import java.util.UUID;
import nl.johninnl.navigation.NavigationProtocol;
import nl.johninnl.navigation.NavigationState;

/** Hooks only shared background producers; no overlay, View or Activity references. */
public final class NavigationBroadcasts {
    private NavigationBroadcasts() {}
    public static final String ACTION = "nl.johninnl.flitsmeister.NAVIGATION_STATE";
    public static final String REQUEST = "nl.johninnl.flitsmeister.NAVIGATION_STATE_REQUEST";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final NavigationState STATE = new NavigationState();
    private static final String SESSION = UUID.randomUUID().toString();
    private static Context app;
    private static boolean active, dirty;
    private static volatile long generation;
    private static long sequence, lastSent = -5000, lastRequest = -1000;
    private static String lastStatus = "";
    private static BroadcastReceiver requests;
    private static Candidate candidate;
    private static long candidateAt=-1;

    public static String receiverPackage() { return ""; }
    private static void main(Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) action.run(); else MAIN.post(action);
    }
    public static void init(Context context) {
        Context application = context.getApplicationContext();
        main(() -> {
            if (app != null) return;
            app = application; active = true; STATE.reset(true);
            IntentFilter filter = new IntentFilter(REQUEST); filter.addAction(NavigationProtocol.REQUEST);
            requests = new BroadcastReceiver() {
                @Override public void onReceive(Context c, Intent intent) {
                    long now = SystemClock.elapsedRealtime();
                    if (!(REQUEST.equals(intent.getAction()) || NavigationProtocol.REQUEST.equals(intent.getAction())) || now-lastRequest<1000) return;
                    lastRequest = now; publish(true);
                }
            };
            if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(requests, filter, Context.RECEIVER_EXPORTED);
            else app.registerReceiver(requests, filter);
            publish(true); MAIN.postDelayed(TICK,1000);
        });
    }
    public static void started() {
        main(() -> { if (app == null || active) return; active = true; STATE.reset(true); dirty = true;
            publish(true); MAIN.removeCallbacks(TICK); MAIN.postDelayed(TICK,1000); });
    }
    public static void zoneStopped() {
        final long at=SystemClock.elapsedRealtime();
        main(() -> { if (at<candidateAt) return; candidateAt=at; candidate=null; STATE.clear("flitsmeister",at); dirty=true; publish(true); });
    }
    public static void stopped() {
        generation++;
        main(() -> { active = false; candidate = null; candidateAt=SystemClock.elapsedRealtime(); STATE.reset(true); STATE.clear("flitsmeister",SystemClock.elapsedRealtime());
            dirty = true; MAIN.removeCallbacks(TICK); publish(true); });
    }
    /** Capture before Flitsmeister substitutes the last speed into missing-speed fixes. */
    public static void location(Location original) {
        if (original == null) return;
        final Location fix = new Location(original);
        final long token = generation, received = SystemClock.elapsedRealtime();
        final long measured = fix.getElapsedRealtimeNanos()/1_000_000;
        final boolean timeValid = measured > 0 && measured <= received;
        final boolean valid = timeValid && fix.hasSpeed() && (!fix.hasAccuracy() || fix.getAccuracy() <= 50);
        final double kmh = valid ? fix.getSpeed()*3.6 : Double.NaN;
        main(() -> {
            if (!active || token != generation) return;
            STATE.current(kmh,timeValid ? measured : received); dirty = true;
            if (valid && received-measured <= NavigationState.SPEED_TTL_MS && candidate != null) observe(candidate,fix,measured);
            publish(false);
        });
    }
    public static void limit(int value, String units) {
        final long at=SystemClock.elapsedRealtime(), token=generation;
        main(() -> { if (!active || token != generation) return; STATE.speed(units,value,at); dirty=true; publish(false); });
    }
    /** Called after native RoadInfoService has selected the final road/vehicle/VMS limit. */
    public static void road() {
        try {
            Class<?> info=Class.forName("hmf");
            int limit=info.getField("f").getInt(null);
            int country=Class.forName("r65").getField("b").getInt(null);
            limit(limit,country==14 ? "imperial" : "metric");
        } catch (ReflectiveOperationException | ClassCastException e) { Log.w("NavigationFeed","Road source",e); }
    }
    /** Capture a candidate and geometry after native detection; do not use premium sample averages. */
    public static void zone(Object task, Location original) {
        if (original == null || (original.hasAccuracy() && original.getAccuracy()>50)) return;
        final Location fix=new Location(original);
        final long at=fix.getElapsedRealtimeNanos()/1_000_000, received=SystemClock.elapsedRealtime(), token=generation;
        try {
            Object report=task.getClass().getField("d").get(task);
            final Candidate selected=report==null ? null : readCandidate(report,fix);
            main(() -> {
                if (!active || token!=generation || at<=0 || at>received || received-at>NavigationState.SPEED_TTL_MS) return;
                if(at<candidateAt) return;
                candidateAt=at; candidate=selected;
                if (selected==null) STATE.clear("flitsmeister",at); else observe(selected,fix,at);
                dirty=true; publish(false);
            });
        } catch (ReflectiveOperationException | ClassCastException e) { Log.w("NavigationFeed","Zone source",e); }
    }
    private static void observe(Candidate c,Location fix,long at) {
        ZoneGeometry.Result p=ZoneGeometry.project(c.path,fix.getLatitude(),fix.getLongitude());
        if (!p.inside) STATE.clear("flitsmeister",at);
        else STATE.zone(c.id,p.remaining,p.percent,at);
    }
    private static Candidate readCandidate(Object report,Location fix) throws ReflectiveOperationException {
        String id=String.valueOf(call(report,"getId"));
        Object segments=call(report,"getSegments");
        if (segments instanceof List && !((List<?>)segments).isEmpty()) {
            for (Object segment:(List<?>)segments) {
                double[][] points=points(call(segment,"getGpsPath"));
                if (ZoneGeometry.project(points,fix.getLatitude(),fix.getLongitude()).inside)
                    return new Candidate(id+":"+call(segment,"getIndex"),points);
            }
            return null;
        }
        return new Candidate(id,points(call(report,"getGpsPath")));
    }
    private static Object call(Object o,String method) throws ReflectiveOperationException { return o.getClass().getMethod(method).invoke(o); }
    private static double[][] points(Object object) throws ReflectiveOperationException {
        if (!(object instanceof List)) return new double[0][0];
        List<?> list=(List<?>)object; double[][] result=new double[list.size()][2];
        for(int i=0;i<list.size();i++) {
            Object p=list.get(i); result[i][0]=p.getClass().getField("a").getDouble(p); result[i][1]=p.getClass().getField("b").getDouble(p);
        }
        return result;
    }
    private static final class Candidate {
        final String id; final double[][] path;
        Candidate(String id,double[][] path) { this.id=id; this.path=path; }
    }
    private static String status(NavigationState.Snapshot n) { return n.currentStatus+":"+n.roadStatus+":"+n.zoneStatus+":"+n.zoneSession+":"+n.limitKmh; }
    private static void publish(boolean force) {
        if(app==null)return;
        long now=SystemClock.elapsedRealtime(); NavigationState.Snapshot n=STATE.snapshot(now);
        String status=status(n);
        if(!force && status.equals(lastStatus) && now-lastSent<(dirty?1000:5000))return;
        Intent i=NavigationProtocol.snapshot(ACTION,app.getPackageName(),SESSION,++sequence,n,now);
        NavigationProtocol.send(app,i,receiverPackage());
        lastStatus=status; lastSent=now; dirty=false;
    }
    private static final Runnable TICK=new Runnable() {
        @Override public void run() {
            if(!active)return;
            NavigationState.Snapshot n=STATE.snapshot(SystemClock.elapsedRealtime());
            if(dirty || n.currentStatus.equals("valid") || n.roadStatus.equals("valid") || n.zoneStatus.equals("active") || !status(n).equals(lastStatus)) publish(false);
            MAIN.postDelayed(this,1000);
        }
    };
    static void resetForTest() {
        MAIN.removeCallbacksAndMessages(null);
        // Robolectric resets receiver registrations between tests.
        app=null; requests=null; active=false; candidate=null; candidateAt=-1; generation++; lastSent=-5000; lastRequest=-1000; lastStatus="";
    }
}
