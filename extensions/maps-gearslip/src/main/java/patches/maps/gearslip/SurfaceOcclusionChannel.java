package patches.maps.gearslip;

import android.graphics.Rect;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Versioned companion protocol; public CAL parcels remain unchanged. */
final class SurfaceOcclusionChannel extends Binder {
    static final String ACTION = "patches.maps.gearslip.action.SURFACE_OCCLUSION";
    static final String DESCRIPTOR = "patches.maps.gearslip.ISurfaceOcclusion";
    static final String RESULT = "patches.maps.gearslip.ISurfaceOcclusionResult";
    static final int VERSION = 1;
    static final int ACCEPTED = 0, DENIED = 1, UNSUPPORTED = 2, NO_SURFACE = 3, STALE = 4, INVALID = 5, FAILED = 6, NOT_READY = 7;
    static final class NotReady extends Exception {
        NotReady() { super(null, null, false, false); }
    }
    private final Handler main;
    private final Target target;
    static final class Info {
        final long generation;
        final int width, height, dpi;
        Info(long generation, int width, int height, int dpi) {
            this.generation = generation; this.width = width; this.height = height; this.dpi = dpi;
        }
    }
    interface Target {
        void checkOwner(int uid);
        Info surface();
        boolean apply(Rect player, List<Rect> covers) throws Exception;
        default void release() throws Exception {}
    }
    SurfaceOcclusionChannel(Handler main, Target target) {
        this.main = main; this.target = target; attachInterface(null, DESCRIPTOR);
    }
    @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
        if (code == INTERFACE_TRANSACTION) { if (reply != null) reply.writeString(DESCRIPTOR); return true; }
        if (code != 1 && code != 2 && code != 3) return false;
        data.enforceInterface(DESCRIPTOR);
        int version = data.readInt(); long id = data.readLong(); IBinder callback = data.readStrongBinder();
        if (callback == null) return true;
        int uid = Binder.getCallingUid();
        try {
            target.checkOwner(uid);
            if (version != VERSION) { complete(callback, id, UNSUPPORTED, null); return true; }
            long generation;
            int width, height, dpi;
            Rect player;
            List<Rect> covers = new ArrayList<>();
            if (code == 2) {
                if (data.dataAvail() < 28) throw new IllegalArgumentException("Missing geometry fields");
                generation = data.readLong(); width = data.readInt(); height = data.readInt(); dpi = data.readInt();
                if (width <= 0 || height <= 0 || width > 32768 || height > 32768 || dpi <= 0 || dpi > 1280)
                    throw new IllegalArgumentException("Invalid canvas");
                int present = data.readInt();
                if (present != 0 && present != 1) throw new IllegalArgumentException("Invalid player marker");
                player = present == 0 ? null : rectangle(data);
                if (data.dataAvail() < 4) throw new IllegalArgumentException("Missing cover count");
                int count = data.readInt();
                if (count < 0 || count > 32) throw new IllegalArgumentException("Too many covers");
                for (int i = 0; i < count; i++) covers.add(rectangle(data));
            } else { generation = 0; width = height = dpi = 0; player = null; }
            if (data.dataAvail() != 0) throw new IllegalArgumentException("Unexpected payload");
            List<Rect> copied = Collections.unmodifiableList(covers);
            main.post(() -> {
                Info info = null;
                try {
                    // Capture UID before posting: Binder.getCallingUid() on main would be ours.
                    target.checkOwner(uid);
                    info = target.surface();
                    int status;
                    if (code == 3) { target.release(); status = ACCEPTED; }
                    else if (info == null || info.width <= 0 || info.height <= 0) status = NO_SURFACE;
                    else if (code == 1) status = ACCEPTED;
                    else if (generation != info.generation) status = STALE;
                    else if (width != info.width || height != info.height || dpi != info.dpi) status = INVALID;
                    else status = target.apply(player, copied) ? ACCEPTED : UNSUPPORTED;
                    complete(callback, id, status, info);
                } catch (SecurityException denied) { complete(callback, id, DENIED, null); }
                catch (NotReady pending) { complete(callback, id, NOT_READY, info); }
                catch (Exception failure) { complete(callback, id, FAILED, info); }
            });
        } catch (SecurityException denied) { complete(callback, id, DENIED, null); }
        catch (RuntimeException malformed) { complete(callback, id, INVALID, null); }
        return true;
    }
    private static Rect rectangle(Parcel data) {
        if (data.dataAvail() < 16) throw new IllegalArgumentException("Missing rectangle");
        Rect value = Rect.CREATOR.createFromParcel(data);
        if (value.left > value.right || value.top > value.bottom ||
                value.left < -65536 || value.top < -65536 || value.right > 65536 || value.bottom > 65536)
            throw new IllegalArgumentException("Invalid cover");
        return value;
    }
    private static void complete(IBinder callback, long id, int status, Info info) {
        Parcel result = Parcel.obtain();
        try {
            result.writeInterfaceToken(RESULT); result.writeLong(id); result.writeInt(status); result.writeInt(VERSION);
            result.writeLong(info == null ? 0 : info.generation);
            result.writeInt(info == null ? 0 : info.width); result.writeInt(info == null ? 0 : info.height);
            result.writeInt(info == null ? 0 : info.dpi);
            callback.transact(1, result, null, IBinder.FLAG_ONEWAY);
        } catch (RemoteException | RuntimeException disconnected) { /* A dead result observer cannot own navigation. */ }
        finally { result.recycle(); }
    }
}
