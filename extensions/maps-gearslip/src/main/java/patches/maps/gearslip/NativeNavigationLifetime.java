package patches.maps.gearslip;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Keeps Maps' original navigation owner independent of a disposable car presentation. */
final class NativeNavigationLifetime {
    static final String PROVIDER = "com.google.android.apps.gmm.car.projected.service.CarNavigationProviderService";
    private static NativeNavigationLifetime current;

    interface ObserverFactory {
        NativeGuidance create(Consumer<NativeGuidance.Snapshot> update, Consumer<Throwable> failure) throws Exception;
    }

    static Lease connect(Context context, Handler main, Consumer<NativeGuidance.Snapshot> update,
            Consumer<Throwable> failure) throws Exception {
        requireMain();
        if (current == null) {
            Context app = context.getApplicationContext();
            current = new NativeNavigationLifetime(app, main,
                    (next, error) -> new NativeGuidance(app, main, next, error));
        }
        return current.attach(update, failure);
    }

    final class Lease implements AutoCloseable {
        final Consumer<NativeGuidance.Snapshot> update;
        final Consumer<Throwable> failure;
        boolean released;
        Lease(Consumer<NativeGuidance.Snapshot> update, Consumer<Throwable> failure) {
            this.update = update; this.failure = failure;
        }
        @Override public void close() {
            requireMain();
            if (released) return;
            released = true;
            clients.remove(this);
            releaseIfUnused();
        }
    }

    private final Context app;
    private final Handler main;
    private final ObserverFactory factory;
    private final List<Lease> clients = new ArrayList<>();
    private NativeGuidance observer;
    private NativeGuidance.Snapshot latest;
    private boolean bound, started, closed;
    private final Runnable timeout = () -> fail(new IllegalStateException("Maps' native navigation provider did not connect"));
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            if (closed) return;
            if (binder == null) { fail(new IllegalStateException("Maps' native navigation provider returned no Binder")); return; }
            main.removeCallbacks(timeout);
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            fail(new IllegalStateException("Maps' native navigation provider disconnected"));
        }
        @Override public void onBindingDied(ComponentName name) {
            fail(new IllegalStateException("Maps' native navigation provider binding died"));
        }
        @Override public void onNullBinding(ComponentName name) {
            fail(new IllegalStateException("Maps refused its native navigation provider binding"));
        }
    };

    NativeNavigationLifetime(Context app, Handler main, ObserverFactory factory) {
        this.app = app; this.main = main; this.factory = factory;
    }

    Lease attach(Consumer<NativeGuidance.Snapshot> update, Consumer<Throwable> failure) throws Exception {
        requireMain();
        if (closed) throw new IllegalStateException("Maps navigation owner is closed");
        Lease lease = new Lease(update, failure);
        clients.add(lease);
        if (!started) {
            started = true;
            try {
                // This same-UID service registers its own native navigation-focus client.
                // Its registration must outlive the last car activity while guiding.
                Intent intent = new Intent("com.google.android.apps.gmm.INTERNAL")
                        .setClassName(app.getPackageName(), PROVIDER);
                bound = app.bindService(intent, connection, Context.BIND_AUTO_CREATE);
                if (!bound) throw new IllegalStateException("Cannot bind Maps' native navigation provider");
                main.postDelayed(timeout, 15000);
                observer = factory.create(this::snapshot, this::fail);
                if (closed) { observer.close(); observer = null; }
            } catch (Exception error) { fail(error); throw error; }
        } else if (latest != null) {
            update.accept(latest);
        }
        return lease;
    }

    private void snapshot(NativeGuidance.Snapshot value) {
        requireMain();
        if (closed) return;
        latest = value;
        for (Lease lease : new ArrayList<>(clients)) if (!lease.released) lease.update.accept(value);
        releaseIfUnused();
    }

    private void releaseIfUnused() {
        if (clients.isEmpty() && (latest == null || !latest.guiding)) dispose();
    }

    private void fail(Throwable error) {
        requireMain();
        if (closed) return;
        List<Lease> affected = new ArrayList<>(clients);
        for (Lease lease : affected) lease.released = true;
        clients.clear();
        dispose();
        for (Lease lease : affected) lease.failure.accept(error);
        if (affected.isEmpty()) Log.w("MapsNativeCar", "Detached native navigation owner failed", error);
    }

    private void dispose() {
        if (closed) return;
        closed = true;
        if (current == this) current = null;
        main.removeCallbacks(timeout);
        if (observer != null) {
            try { observer.close(); } catch (Exception error) { Log.w("MapsNativeCar", "Native navigation listener cleanup failed", error); }
            observer = null;
        }
        if (bound) {
            try { app.unbindService(connection); } catch (IllegalArgumentException error) { Log.w("MapsNativeCar", "Native navigation binding cleanup failed", error); }
            bound = false;
        }
    }

    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Maps navigation ownership requires the main thread");
    }
}
