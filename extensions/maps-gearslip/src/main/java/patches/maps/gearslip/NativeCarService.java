package patches.maps.gearslip;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;
import android.view.inputmethod.EditorInfo;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/** Exposes Maps' own car renderer through the Car App Library's stable Binder protocol. */
public final class NativeCarService extends Service {
    private static final String CAR = "androidx.car.app.ICarApp";
    private static final String APP = "androidx.car.app.IAppManager";
    private static final String SURFACE = "androidx.car.app.ISurfaceCallback";
    private static final String APP_HOST = "androidx.car.app.IAppHost";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Object lock = new Object();
    private final Map<Intent.FilterComparison, Binding> bindings = new HashMap<>();
    private final Map<Intent.FilterComparison, OcclusionBinding> occlusionBindings = new HashMap<>();
    private volatile Session active;

    private final class Binding {
        final CarBinder binder = new CarBinder(this);
        boolean bound = true;
        int generation = 1;
        Session session;
    }

    private static final class Session {
        final int uid;
        final Binding binding;
        final int generation;
        volatile boolean closed;
        ProjectionRenderer renderer;
        IBinder host, appHost;
        IBinder.DeathRecipient death;
        Throwable failure;
        volatile NativeKeyboard keyboard;
        OcclusionBinding occlusionOwner;
        Session(int uid, Binding binding) { this.uid = uid; this.binding = binding; this.generation = binding.generation; }
    }

    private final class OcclusionBinding {
        final Session session;
        final SurfaceOcclusionChannel binder;
        volatile boolean bound = true;
        OcclusionBinding(Session session) {
            this.session = session;
            binder = new SurfaceOcclusionChannel(main, new SurfaceOcclusionChannel.Target() {
                @Override public void checkOwner(int uid) {
                    if (!bound || session == null || owned(session.binding, uid) != session) throw new SecurityException("Current car session required");
                }
                @Override public SurfaceOcclusionChannel.Info surface() {
                    return session.renderer == null || session.failure != null ? null : session.renderer.surfaceInfo();
                }
                @Override public boolean apply(Rect player, java.util.List<Rect> covers) throws SurfaceOcclusionChannel.NotReady {
                    if (session.renderer != null && player != null && !player.isEmpty() && !session.renderer.occlusionReady())
                        throw new SurfaceOcclusionChannel.NotReady();
                    boolean accepted = session.renderer != null && session.renderer.applyOcclusions(player, covers);
                    if (accepted) session.occlusionOwner = OcclusionBinding.this;
                    return accepted;
                }
                @Override public void release() { releaseLayout(); }
            });
        }
        void releaseLayout() {
            if (session != null && active == session && !session.closed && session.occlusionOwner == this) {
                session.occlusionOwner = null;
                if (session.renderer != null) session.renderer.clearControls();
            }
        }
    }

    @Override public IBinder onBind(Intent intent) {
        if (isOcclusionBinding(intent)) {
            OcclusionBinding binding = new OcclusionBinding(active);
            occlusionBindings.put(bindingKey(intent), binding);
            return binding.binder;
        }
        // Android Auto can bind this component explicitly without an action. The
        // authenticated Binder handshake, rather than Intent metadata, owns access.
        return activateBinding(intent).binder;
    }

    @Override public boolean onUnbind(Intent intent) {
        if (isOcclusionBinding(intent)) {
            OcclusionBinding binding = occlusionBindings.remove(bindingKey(intent));
            if (binding != null) { binding.bound = false; main.post(binding::releaseLayout); }
            return false;
        }
        Session owner = null;
        synchronized (lock) {
            Binding binding = bindings.get(bindingKey(intent));
            if (binding != null) { binding.bound = false; owner = binding.session; }
        }
        release(owner);
        // Android retains the published Binder per Intent.FilterComparison. Reactivate its
        // binding record in onRebind, without allowing old queued cleanup to own a new session.
        return true;
    }

    @Override public void onRebind(Intent intent) {
        if (isOcclusionBinding(intent)) return;
        activateBinding(intent);
        super.onRebind(intent);
    }

    private static Intent.FilterComparison bindingKey(Intent intent) {
        return new Intent.FilterComparison(intent == null ? new Intent() : new Intent(intent));
    }
    private static boolean isOcclusionBinding(Intent intent) {
        return intent != null && SurfaceOcclusionChannel.ACTION.equals(intent.getAction());
    }

    private Binding activateBinding(Intent intent) {
        synchronized (lock) {
            Intent.FilterComparison key = bindingKey(intent);
            Binding binding = bindings.get(key);
            if (binding == null) { binding = new Binding(); bindings.put(key, binding); }
            else if (!binding.bound) { binding.bound = true; binding.generation++; }
            return binding;
        }
    }

    @Override public void onDestroy() {
        release(active);
        occlusionBindings.clear();
        synchronized (lock) { for (Binding binding : bindings.values()) binding.bound = false; bindings.clear(); }
        super.onDestroy();
    }

    private Session owned(Binding binding, int callerUid) {
        synchronized (lock) {
            Session session = binding.session;
            if (session == null || active != session || session.closed || !binding.bound ||
                    binding.generation != session.generation || callerUid != session.uid) throw new SecurityException("Host handshake required");
            return session;
        }
    }

    private Session authenticate(Binding binding, int uid, Bundle handshake) throws Exception {
        if (handshake == null || !"androidx.car.app.HandshakeInfo".equals(handshake.getString("tag_class_name"))) {
            throw new IllegalArgumentException("Missing car host handshake");
        }
        String host = CarProtocol.member(handshake, "mHostPackageName").getString("tag_value");
        int api = CarProtocol.member(handshake, "mHostCarAppApiLevel").getInt("tag_value");
        String[] current = null, history = null;
        if ("com.google.android.projection.gearhead".equals(host)) {
            PackageInfo info = getPackageManager().getPackageInfo(host, PackageManager.GET_SIGNING_CERTIFICATES);
            SigningInfo signing = info.signingInfo;
            if (signing != null) {
                current = digests(signing.getApkContentsSigners());
                if (!signing.hasMultipleSigners()) history = digests(signing.getSigningCertificateHistory());
            }
        }
        if (!HostPolicy.permits(host, uid, getPackageManager().getPackagesForUid(uid), current, history)) {
            throw new SecurityException("Unrecognized car host or caller package mismatch");
        }
        if (api < 5 || api > 7) throw new IllegalArgumentException("Unsupported car API level " + api);
        Session replacement, previous;
        synchronized (lock) {
            if (!binding.bound) throw new SecurityException("Car binding has been released");
            previous = active;
            if (previous != null && !previous.closed && previous.uid != uid) {
                throw new IllegalStateException("Another car host is already connected");
            }
            if (previous != null) {
                previous.closed = true;
                if (previous.binding.session == previous) previous.binding.session = null;
            }
            replacement = new Session(uid, binding);
            binding.session = replacement;
            active = replacement;
        }
        if (previous != null) main.post(() -> close(previous));
        return replacement;
    }

    private static String[] digests(Signature[] signatures) throws Exception {
        if (signatures == null) return null;
        String[] hashes = new String[signatures.length];
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int i = 0; i < signatures.length; i++) {
            StringBuilder text = new StringBuilder(64);
            for (byte value : digest.digest(signatures[i].toByteArray())) text.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
            hashes[i] = text.toString();
        }
        return hashes;
    }

    private void release(Session session) {
        if (session == null) return;
        synchronized (lock) {
            session.closed = true;
            if (session.binding.session == session) session.binding.session = null;
            if (active == session) active = null;
        }
        main.post(() -> close(session));
    }

    private void close(Session session) {
        session.occlusionOwner = null;
        clearKeyboard(session);
        if (session.host != null && session.death != null) session.host.unlinkToDeath(session.death, 0);
        session.host = null;
        session.death = null;
        if (session.renderer != null) session.renderer.close();
        session.renderer = null;
        session.appHost = null;
    }

    private void failed(Session session, Throwable error) {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post(() -> failed(session, error)); return; }
        if (active != session || session.closed || session.failure != null) return;
        session.failure = error;
        clearKeyboard(session);
        Log.e("MapsNativeCar", "Maps car renderer failed", error);
        if (session.renderer != null) session.renderer.close();
        invalidate(session);
    }

    private void clearKeyboard(Session session) {
        if (session.keyboard != null) session.keyboard.close();
        session.keyboard = null;
    }

    private void invalidate(Session session) {
        if (session.appHost == null || active != session || session.closed) return;
        try { CarProtocol.send(session.appHost, APP_HOST, 2, data -> {}); }
        catch (RemoteException | RuntimeException disconnected) { release(session); }
    }

    private void showKeyboard(Session session, IBinder input, EditorInfo editor) {
        if (active != session || session.closed || session.failure != null) return;
        try {
            clearKeyboard(session);
            session.keyboard = new NativeKeyboard(input, editor);
            invalidate(session);
        } catch (Exception error) { failed(session, error); }
    }

    private interface Action { void run() throws Exception; }

    private void dispatch(Session session, IBinder callback, boolean needsRenderer, Action action) {
        main.post(() -> {
            try {
                if (active != session || session.closed) throw new IllegalStateException("Car session closed");
                if (session.failure != null) throw new IllegalStateException("Maps car UI failed", session.failure);
                if (needsRenderer && session.renderer == null) throw new IllegalStateException("Car app has not been created");
                action.run();
                CarProtocol.complete(callback, null, session.failure);
            } catch (Exception error) {
                if (needsRenderer && session.renderer != null && !(error instanceof SecurityException)) failed(session, error);
                CarProtocol.complete(callback, null, error);
            }
        });
    }

    private void create(Session session, IBinder host, Intent intent, Configuration configuration, IBinder callback) {
        dispatch(session, callback, false, () -> {
            if (session.renderer != null) throw new IllegalStateException("Car app is already created");
            if (host == null) throw new IllegalArgumentException("Missing car host");
            session.host = host;
            session.death = () -> release(session);
            host.linkToDeath(session.death, 0);
            Parcel request = Parcel.obtain(), reply = Parcel.obtain();
            try {
                request.writeInterfaceToken("androidx.car.app.ICarHost"); request.writeString("app");
                if (!host.transact(3, request, reply, 0)) throw new RemoteException("Car host app service unavailable");
                reply.readException(); session.appHost = reply.readStrongBinder();
            } finally { reply.recycle(); request.recycle(); }
            if (session.appHost == null) throw new IllegalStateException("Car host has no app service");
            session.renderer = new ProjectionRenderer(this, error -> failed(session, error));
            session.renderer.setKeyboardListener(new ProjectionRenderer.KeyboardListener() {
                @Override public void show(IBinder input, EditorInfo editor) { showKeyboard(session, input, editor); }
                @Override public void hide() {
                    if (active != session || session.closed || session.keyboard == null) return;
                    clearKeyboard(session); invalidate(session);
                }
            });
            session.renderer.create(intent, configuration);
            if (session.failure == null) CarProtocol.send(session.appHost, APP_HOST, 4,
                    data -> data.writeStrongBinder(new SurfaceBinder(session)));
        });
    }

    private final class CarBinder extends Binder {
        private final Binding binding;
        CarBinder(Binding binding) { this.binding = binding; }
        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            if (CarProtocol.basic(CAR, code, data, reply)) return true;
            int uid = Binder.getCallingUid(); // Capture before posting any work to the main thread.
            IBinder callback = null;
            try {
                switch (code) {
                    case 10:
                        CarProtocol.complete(data.readStrongBinder(), CarProtocol.appInfo(), null); return true;
                    case 11:
                        Bundle handshake = CarProtocol.readBundleable(data); callback = data.readStrongBinder();
                        authenticate(binding, uid, handshake); CarProtocol.complete(callback, null, null); return true;
                    case 2: {
                        IBinder host = data.readStrongBinder();
                        Intent intent = CarProtocol.nullable(data, Intent.CREATOR);
                        Configuration configuration = CarProtocol.nullable(data, Configuration.CREATOR);
                        callback = data.readStrongBinder(); create(owned(binding, uid), host, intent, configuration, callback); return true;
                    }
                    case 3: case 4: case 5: case 6: {
                        callback = data.readStrongBinder(); Session session = owned(binding, uid);
                        dispatch(session, callback, true, () -> {
                            if (code == 3) session.renderer.start();
                            else if (code == 4) session.renderer.resume();
                            else if (code == 5) session.renderer.pause();
                            else session.renderer.stop();
                        }); return true;
                    }
                    case 7: {
                        Intent intent = CarProtocol.nullable(data, Intent.CREATOR); callback = data.readStrongBinder();
                        Session session = owned(binding, uid); dispatch(session, callback, true, () -> session.renderer.newIntent(intent)); return true;
                    }
                    case 8: {
                        Configuration config = CarProtocol.nullable(data, Configuration.CREATOR); callback = data.readStrongBinder();
                        Session session = owned(binding, uid); dispatch(session, callback, true, () -> session.renderer.configurationChanged(config)); return true;
                    }
                    case 9: {
                        String name = data.readString(); callback = data.readStrongBinder(); Session session = owned(binding, uid);
                        CarProtocol.complete(callback, "app".equals(name) ? CarProtocol.interfaceBinder("androidx.car.app.IAppManager$Stub", new AppBinder(session)) : null, null); return true;
                    }
                    case 12:
                        callback = data.readStrongBinder(); owned(binding, uid); CarProtocol.complete(callback, null, null); return true;
                    default: return false;
                }
            } catch (Exception error) { CarProtocol.complete(callback, null, error); return true; }
        }
    }

    private void requireSession(Session session) {
        if (owned(session.binding, Binder.getCallingUid()) != session) throw new SecurityException("Car session replaced");
    }

    private final class AppBinder extends Binder {
        private final Session session;
        AppBinder(Session session) { this.session = session; }
        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            if (CarProtocol.basic(APP, code, data, reply)) return true;
            IBinder callback = data.readStrongBinder();
            try {
                requireSession(session);
                if (code == 2) {
                    main.post(() -> {
                        if (active != session || session.closed) CarProtocol.complete(callback, null, new IllegalStateException("Car session closed"));
                        else if (session.renderer == null && session.failure == null) CarProtocol.complete(callback, null, new IllegalStateException("Car app has not been created"));
                        else {
                            try {
                                NativeKeyboard input = session.keyboard;
                                Bundle template = input == null || session.failure != null ? CarProtocol.template(session.failure)
                                        : CarProtocol.searchTemplate(input.text(), input.hint(), new SearchBinder(session, input));
                                CarProtocol.complete(callback, template, null);
                            } catch (Exception error) { failed(session, error); CarProtocol.complete(callback, null, error); }
                        }
                    });
                } else if (code == 3) dispatch(session, callback, true, () -> {
                    clearKeyboard(session); session.renderer.back(); invalidate(session);
                });
                else if (code == 4 || code == 5) CarProtocol.complete(callback, null, null); // Maps owns its location stream.
                else return false;
            } catch (Exception error) { CarProtocol.complete(callback, null, error); }
            return true;
        }
    }

    private final class SearchBinder extends Binder {
        private final Session session;
        private final NativeKeyboard input;
        SearchBinder(Session session, NativeKeyboard input) { this.session = session; this.input = input; }
        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            if (CarProtocol.basic("androidx.car.app.model.ISearchCallback", code, data, reply)) return true;
            if (code != 2 && code != 3) return false;
            String query = data.readString(); IBinder callback = data.readStrongBinder();
            try {
                requireSession(session);
                if (session.keyboard != input) throw new SecurityException("Maps input connection replaced");
                dispatch(session, callback, true, () -> {
                    if (session.keyboard != input) throw new SecurityException("Maps input connection replaced");
                    if (code == 2) input.replace(query);
                    else {
                        input.submit(query);
                        clearKeyboard(session); invalidate(session);
                    }
                });
            } catch (Exception error) { CarProtocol.complete(callback, null, error); }
            return true;
        }
    }

    private final class SurfaceBinder extends Binder {
        private final Session session;
        SurfaceBinder(Session session) { this.session = session; }
        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            if (CarProtocol.basic(SURFACE, code, data, reply)) return true;
            IBinder callback = null;
            try {
                int callerUid = Binder.getCallingUid();
                if (code != 5 && code != 2 && code != 3 && code != 4) requireSession(session);
                switch (code) {
                    case 5: {
                        Bundle container = CarProtocol.readBundleable(data); callback = data.readStrongBinder();
                        Surface surface = CarProtocol.surface(container);
                        if (surface != null) surface.release(); // The destruction parcel owns a separate reference.
                        if (callerUid != session.uid) throw new SecurityException("Car surface belongs to another host UID");
                        IBinder answer = callback;
                        main.post(() -> {
                            // A host sends this one-way immediately before unbinding. Android can
                            // close the session before this queued cleanup runs; acknowledge that
                            // owner's redundant cleanup without reviving any renderer/capability.
                            if (active != session || session.closed || session.failure != null || session.renderer == null) {
                                CarProtocol.complete(answer, null, null); return;
                            }
                            try {
                                session.renderer.surfaceDestroyed();
                                CarProtocol.complete(answer, null, session.failure);
                            } catch (Exception error) { failed(session, error); CarProtocol.complete(answer, null, error); }
                        }); return true;
                    }
                    case 2: {
                        Bundle container = CarProtocol.readBundleable(data); callback = data.readStrongBinder();
                        Surface surface = CarProtocol.surface(container);
                        try { requireSession(session); }
                        catch (RuntimeException error) { if (surface != null) surface.release(); throw error; }
                        if (surface == null) {
                            dispatch(session, callback, true, () -> session.renderer.surfaceDestroyed());
                            return true;
                        }
                        int width = CarProtocol.member(container, "mWidth").getInt("tag_value");
                        int height = CarProtocol.member(container, "mHeight").getInt("tag_value");
                        int dpi = CarProtocol.member(container, "mDpi").getInt("tag_value");
                        IBinder answer = callback;
                        main.post(() -> {
                            if (active != session || session.closed || session.failure != null || session.renderer == null) {
                                if (surface != null) surface.release();
                                CarProtocol.complete(answer, null, new IllegalStateException("Maps car surface is unavailable", session.failure)); return;
                            }
                            try {
                                session.renderer.surfaceAvailable(surface, width, height, dpi); // Renderer owns this Surface now.
                                CarProtocol.complete(answer, null, session.failure);
                            } catch (Exception error) {
                                if (surface != null) surface.release(); failed(session, error); CarProtocol.complete(answer, null, error);
                            }
                        }); return true;
                    }
                    case 3: case 4: {
                        Rect area = CarProtocol.nullable(data, Rect.CREATOR); callback = data.readStrongBinder();
                        requireSession(session);
                        dispatch(session, callback, true, () -> {
                            if (code == 3) session.renderer.visibleArea(area); else session.renderer.stableArea(area);
                        }); return true;
                    }
                    case 6: case 7: case 9: {
                        float x = data.readFloat(), y = data.readFloat();
                        dispatch(session, null, true, () -> {
                            if (code == 6) session.renderer.scroll(x, y);
                            else if (code == 7) session.renderer.fling(x, y);
                            else session.renderer.click(x, y);
                        }); return true;
                    }
                    case 8: {
                        float x = data.readFloat(), y = data.readFloat(), scale = data.readFloat();
                        dispatch(session, null, true, () -> session.renderer.scale(x, y, scale)); return true;
                    }
                    default: return false;
                }
            } catch (Exception error) { CarProtocol.complete(callback, null, error); return true; }
        }
    }
}
