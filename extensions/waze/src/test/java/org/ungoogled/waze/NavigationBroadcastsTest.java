package org.ungoogled.waze;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32, manifest = Config.NONE)
public class NavigationBroadcastsTest {
    private Application app;
    private void field(Class<?> c, String name, Object value) throws Exception {
        Field f = c.getDeclaredField(name); f.setAccessible(true); f.set(null, value);
    }
    @Before public void setup() throws Exception {
        app = RuntimeEnvironment.getApplication();
        app.getSharedPreferences("ungoogled_waze", 0).edit().clear().putBoolean("zoneBar", false)
                .putBoolean("stayAwake", false).commit();
        field(Customization.class, "app", app);
        Field f = NavigationBroadcasts.class.getDeclaredField("MAIN"); f.setAccessible(true);
        ((Handler) f.get(null)).removeCallbacksAndMessages(null);
        field(NavigationBroadcasts.class, "installed", false);
        field(NavigationBroadcasts.class, "lastRequest", -1000L);
        field(NavigationBroadcasts.class, "lastSent", -5000L);
        NavigationBroadcasts.init(app);
    }
    private List<Intent> events() {
        return shadowOf(app).getBroadcastIntents().stream().filter(i -> NavigationBroadcasts.ACTION.equals(i.getAction()))
                .collect(Collectors.toList());
    }
    private Intent last() { List<Intent> list = events(); return list.get(list.size() - 1); }

    @Test public void roadDataDoesNotNeedAnActivityOrZoneOverlay() {
        NavigationBroadcasts.speed("mph", 65);
        assertEquals(104.60736, last().getDoubleExtra("road_speed_limit_kmh", -1), 0.00001);
        assertEquals(1, last().getIntExtra("schema_version", 0));
        assertFalse(last().hasExtra("zone_average_speed_kmh"));
        assertTrue(Customization.on("stayAwake"));
    }
    @Test public void requestsAndHeartbeatCannotMakeOldObservationsFresh() {
        NavigationBroadcasts.speed("km/h", 100);
        long observed = last().getLongExtra("road_updated_elapsed_ms", -1);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(31));
        app.sendBroadcast(new Intent(NavigationBroadcasts.REQUEST));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("stale", last().getStringExtra("road_speed_limit_status"));
        assertEquals(observed, last().getLongExtra("road_updated_elapsed_ms", -1));
        assertFalse(last().hasExtra("road_speed_limit_kmh"));
    }
    @Test public void disablingClearsAndStopsEvenWhenNativeCallbacksContinue() {
        NavigationBroadcasts.speed("km/h", 100);
        Customization.setForTest("navigationBroadcasts", false);
        assertEquals("disabled", last().getStringExtra("road_speed_limit_status"));
        assertFalse(last().hasExtra("road_speed_limit_kmh"));
        int count = events().size();
        NavigationBroadcasts.speed("km/h", 120);
        app.sendBroadcast(new Intent(NavigationBroadcasts.REQUEST));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6));
        assertEquals(count, events().size());
        assertFalse(Customization.on("stayAwake"));
    }
    @Test public void unknownValuesAreOmittedRatherThanSentAsZero() {
        NavigationBroadcasts.speed("unknown-units", 50);
        assertEquals("unknown", last().getStringExtra("road_speed_limit_status"));
        assertFalse(last().hasExtra("road_speed_limit_kmh"));
    }
    @Test public void commonContractIncludesCurrentSpeedAndStationaryZero() {
        NavigationBroadcasts.speed("mph", 65, 0);
        Intent common = shadowOf(app).getBroadcastIntents().stream()
            .filter(i -> "nl.johninnl.navigation.NAVIGATION_STATE".equals(i.getAction()))
            .reduce((a,b) -> b).get();
        assertEquals(last().getLongExtra("sequence",-1),common.getLongExtra("sequence",-2));
        assertEquals(app.getPackageName(),common.getStringExtra("source_package"));
        assertEquals("valid",common.getStringExtra("current_speed_status"));
        assertEquals(0,common.getDoubleExtra("current_speed_kmh",-1),0);
        assertEquals(104.60736,common.getDoubleExtra("road_speed_limit_kmh",-1),0.00001);
        long at=last().getLongExtra("current_speed_updated_elapsed_ms",-1);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6));
        app.sendBroadcast(new Intent("nl.johninnl.navigation.NAVIGATION_STATE_REQUEST"));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("stale",last().getStringExtra("current_speed_status"));
        assertEquals(at,last().getLongExtra("current_speed_updated_elapsed_ms",-2));
        assertFalse(last().hasExtra("current_speed_kmh"));
    }

}
