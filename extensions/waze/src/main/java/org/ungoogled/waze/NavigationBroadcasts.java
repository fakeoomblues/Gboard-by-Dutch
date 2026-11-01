package org.ungoogled.waze;

import nl.johninnl.navigation.NavigationState;
import nl.johninnl.navigation.NavigationProtocol;

import android.app.Application;
import android.app.BroadcastOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import java.util.List;
import java.util.UUID;
import java.util.Locale;

/** Versioned local data feed. All state mutation/publication is serialized on the main looper. */
public final class NavigationBroadcasts {
    private NavigationBroadcasts() {}
    public static final String ACTION = "nl.johninnl.waze.NAVIGATION_STATE";
    public static final String REQUEST = "nl.johninnl.waze.NAVIGATION_STATE_REQUEST";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final NavigationState STATE = new NavigationState();
    private static final String SESSION = UUID.randomUUID().toString();
    private static volatile boolean installed;
    private static Application app;
    private static long sequence, lastSent = -5000, lastRequest = -1000;
    private static boolean dirty;

    /** Bound by the patch; blank means implicit local broadcasts for any compatible receiver. */
    public static String receiverPackage() { return ""; }

    static boolean isEnabled() { return installed && Customization.on("navigationBroadcasts"); }

    private static void main(Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) action.run(); else MAIN.post(action);
    }

    public static void init(Application application) {
        main(() -> {
            if (installed) return;
            app = application; installed = true;
            IntentFilter filter = new IntentFilter(REQUEST);
            filter.addAction(NavigationProtocol.REQUEST);
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override public void onReceive(Context c, Intent in) {
                    long now = SystemClock.elapsedRealtime();
                    if (!(REQUEST.equals(in.getAction()) || NavigationProtocol.REQUEST.equals(in.getAction())) || !isEnabled() || now - lastRequest < 1000) return;
                    lastRequest = now; publish(true);
                }
            };
            if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            else app.registerReceiver(receiver, filter);
            switched(isEnabled());
            try {
                // Apply keep-alive when native initialization finishes, even without a server sync.
                Class.forName("com.waze.NativeManager").getMethod("runOnNativeManagerReady", Runnable.class)
                        .invoke(null, (Runnable) () -> { if (isEnabled()) Customization.onConfigSynced(); });
            } catch (ReflectiveOperationException e) { Log.w("UA", "Navigation background initialization", e); }
        });
    }

    static void switched(boolean on) {
        main(() -> {
            if (!installed) return;
            MAIN.removeCallbacks(TICK);
            STATE.reset(on); dirty = true; publish(true); // clearing snapshot is sent even on disable
            if (on) MAIN.postDelayed(TICK, 1000);
        });
    }

    /** Called by LocationSensorListener.updateSpeedometer, not a visible speedometer View. */
    public static void speed(String units, int limit, double currentKmh) {
        final long at = SystemClock.elapsedRealtime();
        main(() -> { if (isEnabled()) { STATE.current(currentKmh, at); dirty = true; } });
        speed(units, limit);
    }

    public static void speed(String units, int limit) {
        final long at = SystemClock.elapsedRealtime();
        if (!isEnabled()) return;
        String value = units == null ? "" : units.toLowerCase(Locale.ROOT).replace(" ", "");
        if (!(value.equals("mph") || value.equals("mi/h") || value.equals("km/h") ||
                value.equals("kmh") || value.equals("kph"))) value = configuredUnits();
        final String normalized = value;
        main(() -> {
            if (!isEnabled()) return;
            NavigationState.Snapshot before = STATE.snapshot(at);
            STATE.speed(normalized, limit, at); dirty = true;
            NavigationState.Snapshot after = STATE.snapshot(at);
            publish(!before.roadStatus.equals(after.roadStatus) || Double.compare(before.limitKmh, after.limitKmh) != 0);
        });
    }

    public static void legacy(Object update) {
        final long at = SystemClock.elapsedRealtime();
        main(() -> {
            if (!isEnabled() || update == null) return;
            try {
                long previousSession = STATE.snapshot(at).zoneSession;
                STATE.legacy((Integer) call(update, "getPercent"), (Integer) call(update, "getLeftDistance"), at);
                dirty = true; publish(previousSession != STATE.snapshot(at).zoneSession);
            } catch (ReflectiveOperationException | ClassCastException e) { Log.w("UA", "Navigation legacy data", e); }
        });
    }

    public static void legacyClear() {
        final long at = SystemClock.elapsedRealtime();
        main(() -> { if (isEnabled()) { STATE.clear("legacy", at); dirty = true; publish(true); } });
    }

    public static void insights(Object update) {
        final long at = SystemClock.elapsedRealtime();
        main(() -> {
            if (!isEnabled() || update == null) return;
            try {
                String id = null; int remaining = -1;
                long previousSession = STATE.snapshot(at).zoneSession;
                Object list = call(update, "getInfoList");
                if (!(list instanceof List)) return;
                for (Object info : (List<?>) list) {
                    Object identity = call(info, "getId");
                    if ((Integer) call(identity, "getSegmentTypeValue") != 7 ||
                            (Integer) call(info, "getPositionRelativeToSegmentValue") != 1) continue;
                    id = (String) call(identity, "getSegmentId");
                    remaining = (Integer) call(info, "getRemainingDistanceMeters"); break;
                }
                if (id == null) STATE.clear("insights", at); else STATE.insights(id, remaining, at);
                dirty = true; publish(previousSession != STATE.snapshot(at).zoneSession);
            } catch (ReflectiveOperationException | ClassCastException e) { Log.w("UA", "Navigation insights data", e); }
        });
    }

    private static Object call(Object value, String method) throws ReflectiveOperationException {
        return value.getClass().getMethod(method).invoke(value);
    }

    /** Waze's own nonlocalized unit setting, including its automatic/default selection. */
    private static String configuredUnits() {
        try {
            Class<?> values = Class.forName("com.waze.config.ConfigValues");
            String unit = (String) call(values.getField("CONFIG_VALUE_GENERAL_UNITS").get(null), "a");
            if ("default".equals(unit)) unit = (String) call(values.getField("CONFIG_VALUE_GENERAL_DEFAULT_UNITS").get(null), "a");
            return "metric".equals(unit) || "imperial".equals(unit) ? unit : "";
        } catch (ReflectiveOperationException | ClassCastException e) { return ""; }
    }

    private static final Runnable TICK = new Runnable() {
        @Override public void run() {
            if (!isEnabled()) { switched(false); return; }
            NavigationState.Snapshot n = STATE.snapshot(SystemClock.elapsedRealtime());
            if (dirty || n.currentStatus.equals("valid") || n.roadStatus.equals("valid") || n.zoneStatus.equals("active") ||
                    !n.roadStatus.equals(lastRoadStatus) || !n.zoneStatus.equals(lastZoneStatus) ||
                    !n.currentStatus.equals(lastCurrentStatus)) publish(false);
            MAIN.postDelayed(this, 1000);
        }
    };

    private static String lastRoadStatus = "", lastZoneStatus = "", lastCurrentStatus = "";
    private static void publish(boolean force) {
        if (app == null) return;
        long now = SystemClock.elapsedRealtime();
        NavigationState.Snapshot n = STATE.snapshot(now);
        boolean transition = !n.roadStatus.equals(lastRoadStatus) || !n.zoneStatus.equals(lastZoneStatus) ||
                    !n.currentStatus.equals(lastCurrentStatus);
        if (!force && !transition && now - lastSent < (dirty ? 1000 : 5000)) return;
        NavigationProtocol.send(app, snapshot(n, now), receiverPackage());
        lastSent = now; lastRoadStatus = n.roadStatus; lastZoneStatus = n.zoneStatus; lastCurrentStatus = n.currentStatus; dirty = false;
    }

    static Intent snapshot(NavigationState.Snapshot n, long now) {
        return NavigationProtocol.snapshot(ACTION, app == null ? "com.waze" : app.getPackageName(), SESSION, ++sequence, n, now);
    }
}
