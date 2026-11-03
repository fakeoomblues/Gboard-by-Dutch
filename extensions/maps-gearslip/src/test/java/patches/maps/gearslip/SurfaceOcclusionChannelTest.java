package patches.maps.gearslip;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBinder;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32)
public final class SurfaceOcclusionChannelTest {
    private boolean current = true, ready = true, available = true;
    private int owner = 10042, applications, releases;
    private long generation = 1;
    private List<Rect> received;
    private final SurfaceOcclusionChannel channel = new SurfaceOcclusionChannel(new Handler(Looper.getMainLooper()),
            new SurfaceOcclusionChannel.Target() {
                @Override public void checkOwner(int uid) { if (!current || uid != owner) throw new SecurityException(); }
                @Override public SurfaceOcclusionChannel.Info surface() { return new SurfaceOcclusionChannel.Info(generation, available ? 1280 : 0, available ? 480 : 0, available ? 160 : 0); }
                @Override public boolean apply(Rect player, List<Rect> covers) throws SurfaceOcclusionChannel.NotReady {
                    if (!ready) throw new SurfaceOcclusionChannel.NotReady();
                    applications++; received = covers; return true;
                }
                @Override public void release() { releases++; }
            });
    private static final class Result extends Binder {
        long id, generation;
        int status = -1, width;
        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            assertEquals(1, code); data.enforceInterface(SurfaceOcclusionChannel.RESULT);
            id = data.readLong(); status = data.readInt(); assertEquals(1, data.readInt());
            generation = data.readLong(); width = data.readInt(); data.readInt(); data.readInt();
            return true;
        }
    }
    private Result send(int code, int version, long surfaceGeneration, int width, int covers) throws Exception {
        ShadowBinder.setCallingUid(10042);
        Parcel data = Parcel.obtain(); Result result = new Result();
        try {
            data.writeInterfaceToken(SurfaceOcclusionChannel.DESCRIPTOR);
            data.writeInt(version); data.writeLong(42); data.writeStrongBinder(result);
            if (code == 2) {
                data.writeLong(surfaceGeneration); data.writeInt(width); data.writeInt(480); data.writeInt(160);
                data.writeInt(1); new Rect(920, 0, 1280, 180).writeToParcel(data, 0);
                data.writeInt(covers);
                if (covers == 1) new Rect(0, 396, 1280, 480).writeToParcel(data, 0);
            }
            assertTrue(channel.transact(code, data, null, IBinder.FLAG_ONEWAY));
            return result;
        } finally { data.recycle(); }
    }
    @After public void reset() { shadowOf(Looper.getMainLooper()).idle(); ShadowBinder.reset(); }
    @Test public void describeReturnsAdapterGenerationAndCanvasThroughTypedCallback() throws Exception {
        Result r = send(1, 1, 0, 0, 0); shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, r.status); assertEquals(42, r.id); assertEquals(1, r.generation); assertEquals(1280, r.width);
    }
    @Test public void measuredCoversAreDeliveredOnTheMainThread() throws Exception {
        Result r = send(2, 1, 1, 1280, 1);
        assertEquals(0, applications); shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, r.status); assertEquals(1, applications); assertEquals(new Rect(0, 396, 1280, 480), received.get(0));
    }
    @Test public void sameSizedSurfaceReplacementRejectsQueuedOldGeneration() throws Exception {
        Result r = send(2, 1, 1, 1280, 1); generation++;
        shadowOf(Looper.getMainLooper()).idle(); assertEquals(SurfaceOcclusionChannel.STALE, r.status); assertEquals(0, applications);
    }
    @Test public void sessionReplacementRevokesAlreadyQueuedUpdates() throws Exception {
        Result r = send(2, 1, 1, 1280, 1); current = false;
        shadowOf(Looper.getMainLooper()).idle(); assertEquals(SurfaceOcclusionChannel.DENIED, r.status); assertEquals(0, applications);
    }
    @Test public void anotherUidCannotFetchSurfaceMetadata() throws Exception {
        owner++; Result r = send(1, 1, 0, 0, 0); owner--;
        shadowOf(Looper.getMainLooper()).idle(); assertEquals(SurfaceOcclusionChannel.DENIED, r.status); assertEquals(0, r.width);
    }
    @Test public void versionDimensionsAndListBoundsAreValidatedBeforeApplying() throws Exception {
        Result version = send(2, 2, 1, 1280, 1), size = send(2, 1, 1, 720, 1), list = send(2, 1, 1, 1280, 33);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(SurfaceOcclusionChannel.UNSUPPORTED, version.status); assertEquals(SurfaceOcclusionChannel.INVALID, size.status);
        assertEquals(SurfaceOcclusionChannel.INVALID, list.status); assertEquals(0, applications);
    }
    @Test public void missingListCountCannotBecomeAnAccidentalClearRequest() throws Exception {
        ShadowBinder.setCallingUid(10042);
        Parcel data = Parcel.obtain(); Result result = new Result();
        try {
            data.writeInterfaceToken(SurfaceOcclusionChannel.DESCRIPTOR); data.writeInt(1);
            data.writeLong(42); data.writeStrongBinder(result);
            data.writeLong(1); data.writeInt(1280); data.writeInt(480); data.writeInt(160); data.writeInt(0);
            assertTrue(channel.transact(2, data, null, IBinder.FLAG_ONEWAY));
            shadowOf(Looper.getMainLooper()).idle();
            assertEquals(SurfaceOcclusionChannel.INVALID, result.status); assertEquals(0, applications);
        } finally { data.recycle(); }
    }
    @Test public void nativeTreeInitializationReturnsARetryableResultWithoutApplying() throws Exception {
        ready = false; Result result = send(2, 1, 1, 1280, 1); shadowOf(Looper.getMainLooper()).idle();
        assertEquals(SurfaceOcclusionChannel.NOT_READY, result.status); assertEquals(1, result.generation); assertEquals(0, applications);
    }
    @Test public void releaseDoesNotRequireAValidDrawingSurface() throws Exception {
        available = false; Result result = send(3, 1, 0, 0, 0); shadowOf(Looper.getMainLooper()).idle();
        assertEquals(SurfaceOcclusionChannel.ACCEPTED, result.status); assertEquals(1, releases); assertEquals(0, applications);
    }
    @Test public void releaseRechecksTheCapturedSessionAfterQueuing() throws Exception {
        Result result = send(3, 1, 0, 0, 0); current = false; shadowOf(Looper.getMainLooper()).idle();
        assertEquals(SurfaceOcclusionChannel.DENIED, result.status); assertEquals(0, releases);
    }
    @Test public void anotherUidCannotReleaseLayoutOwnership() throws Exception {
        owner++; Result result = send(3, 1, 0, 0, 0); owner--; shadowOf(Looper.getMainLooper()).idle();
        assertEquals(SurfaceOcclusionChannel.DENIED, result.status); assertEquals(0, releases);
    }
    @Test public void releaseRejectsUnexpectedPayloadBeforeClearingAnything() throws Exception {
        ShadowBinder.setCallingUid(10042); Parcel data = Parcel.obtain(); Result result = new Result();
        try {
            data.writeInterfaceToken(SurfaceOcclusionChannel.DESCRIPTOR); data.writeInt(1); data.writeLong(42); data.writeStrongBinder(result); data.writeInt(9);
            assertTrue(channel.transact(3, data, null, IBinder.FLAG_ONEWAY)); shadowOf(Looper.getMainLooper()).idle();
            assertEquals(SurfaceOcclusionChannel.INVALID, result.status); assertEquals(0, releases);
        } finally { data.recycle(); }
    }
}
