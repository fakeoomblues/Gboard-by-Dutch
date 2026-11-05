package patches.maps.gearslip;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32)
public final class NativeNavigationLifetimeTest {
    private final NativeGuidanceTest.Source source = new NativeGuidanceTest.Source();
    private final List<NativeGuidance.Snapshot> updates = new ArrayList<>();
    private final List<Throwable> failures = new ArrayList<>();
    private NativeNavigationLifetime lifetime;
    private NativeNavigationLifetime.Lease lease;
    private ServiceConnection connection;
    private Intent binding;
    private int binds, unbinds;
    private boolean acceptsBinding = true;

    @Before public void create() {
        Context app = new ContextWrapper(RuntimeEnvironment.getApplication()) {
            @Override public boolean bindService(Intent intent, ServiceConnection callback, int flags) {
                binds++; binding = intent; connection = callback;
                assertEquals(Context.BIND_AUTO_CREATE, flags);
                return acceptsBinding;
            }
            @Override public void unbindService(ServiceConnection callback) {
                assertSame(connection, callback); unbinds++;
            }
        };
        lifetime = new NativeNavigationLifetime(app, new Handler(Looper.getMainLooper()),
                (update, failure) -> new NativeGuidance(source, NativeGuidanceTest.Listener.class,
                        NativeGuidanceTest.Marker.class, Runnable::run, update, failure));
    }

    @After public void stop() {
        if (source.listener != null) source.emit(NativeGuidanceTest.Mode.OFF);
        if (lease != null) lease.close();
        shadowOf(Looper.getMainLooper()).idle();
    }

    private void attach() throws Exception {
        lease = lifetime.attach(updates::add, failures::add);
        connection.onServiceConnected(binding.getComponent(), new Binder());
    }

    @Test public void guidanceKeepsTheOriginalProviderBoundAcrossUiDetachAndRebind() throws Exception {
        attach();
        assertEquals("com.google.android.apps.gmm.INTERNAL", binding.getAction());
        assertEquals(NativeNavigationLifetime.PROVIDER, binding.getComponent().getClassName());
        assertEquals(RuntimeEnvironment.getApplication().getPackageName(), binding.getComponent().getPackageName());
        NativeNavigationLifetime.Lease old = lease;
        old.close();
        assertEquals(0, unbinds); assertEquals(0, source.removals);
        attach(); old.close();
        assertEquals(1, binds); assertEquals(0, unbinds);
        assertEquals(2, updates.size()); assertTrue(updates.get(1).guiding);
        source.emit(NativeGuidanceTest.Mode.OFF);
        assertEquals(0, unbinds); // A connected car may start another native route.
        lease.close();
        assertEquals(1, unbinds); assertEquals(1, source.removals);
        assertTrue(failures.isEmpty());
    }

    @Test public void routeCompletionReleasesDetachedOwnershipAndIgnoresLateUpdates() throws Exception {
        attach(); lease.close();
        source.emit(NativeGuidanceTest.Mode.CRUISING);
        assertEquals(1, unbinds); assertEquals(1, source.removals);
        source.emit(NativeGuidanceTest.Mode.GUIDING);
        connection.onServiceConnected(binding.getComponent(), new Binder());
        assertEquals(1, binds); assertEquals(1, unbinds); assertEquals(1, updates.size());
    }

    @Test public void anIdleCarDoesNotLeaveANativeProviderRunningAfterDetach() throws Exception {
        source.b = new NativeGuidanceTest.State(NativeGuidanceTest.Mode.OFF);
        attach(); lease.close(); lease.close();
        assertEquals(1, unbinds); assertEquals(1, source.removals);
    }

    @Test public void bindingFailureDoesNotAdmitOwnershipOrInstallAnObserver() throws Exception {
        acceptsBinding = false;
        try { lifetime.attach(updates::add, failures::add); fail("Missing native provider must fail creation"); }
        catch (IllegalStateException expected) { assertEquals(1, failures.size()); }
        assertNull(source.listener); assertEquals(0, unbinds);
    }

    @Test public void missingProviderConnectionTimesOutAndClosesTheNativeObserver() throws Exception {
        lease = lifetime.attach(updates::add, failures::add);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(15));
        assertEquals(1, failures.size()); assertEquals(1, unbinds); assertEquals(1, source.removals);
        lease.close(); assertEquals(1, unbinds);
    }

    @Test public void nativeBindingDeathReleasesDetachedGuidanceOwnership() throws Exception {
        attach(); lease.close();
        connection.onBindingDied(new ComponentName(binding.getComponent().getPackageName(), NativeNavigationLifetime.PROVIDER));
        assertEquals(1, unbinds); assertEquals(1, source.removals);
        assertTrue(failures.isEmpty()); // The closed UI lease cannot receive callbacks.
    }
}
