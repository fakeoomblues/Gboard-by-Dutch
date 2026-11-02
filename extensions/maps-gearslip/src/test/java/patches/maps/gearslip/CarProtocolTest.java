package patches.maps.gearslip;

import static org.junit.Assert.*;
import android.os.Binder;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.TemplateWrapper;
import androidx.car.app.serialization.Bundleable;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32)
public final class CarProtocolTest {
    @Test public void navigationTemplateLeavesAllMapControlsToTheOriginalRenderer() {
        Bundle template = CarProtocol.member(CarProtocol.template(null), "mTemplate");
        assertEquals("androidx.car.app.navigation.model.NavigationTemplate", template.getString("tag_class_name"));
        Bundle strip = CarProtocol.member(template, "mActionStrip");
        assertTrue(CarProtocol.member(strip, "mActions").getParcelableArrayList("tag_value").isEmpty());
        assertFalse(template.containsKey("androidx.car.app.navigation.model.NavigationTemplatemNavigationInfo"));
    }

    @Test public void originalRendererFailureProducesAnErrorScreenWithTheActualCause() {
        Bundle template = CarProtocol.member(CarProtocol.template(new InvocationTargetException(new IllegalStateException("native map unavailable"))), "mTemplate");
        assertEquals("androidx.car.app.model.MessageTemplate", template.getString("tag_class_name"));
        Bundle text = CarProtocol.member(template, "mMessage");
        assertTrue(CarProtocol.member(text, "mText").getString("tag_value").contains("native map unavailable"));
        assertFalse(template.containsKey("androidx.car.app.model.MessageTemplatemIsLoading"));
    }

    @Test public void appInfoAndSerializedFieldKeysMatchThePublishedWireSchema() {
        Bundle info = CarProtocol.appInfo();
        assertEquals(5, info.getBundle("androidx.car.app.AppInfomMinCarAppApiLevel").getInt("tag_value"));
        assertEquals(7, info.getBundle("androidx.car.app.AppInfomLatestCarAppApiLevel").getInt("tag_value"));
        assertEquals(5, info.getInt("tag_class_type"));
    }

    @Test public void callbacksUseSynchronousTransactionsAndReadTheReceiversReply() {
        RecordingCallback callback = new RecordingCallback(true);
        CarProtocol.complete(callback, CarProtocol.appInfo(), null);
        assertEquals(0, callback.flags);
        assertEquals(2, callback.code);
        assertEquals("androidx.car.app.AppInfo", callback.response.getString("tag_class_name"));
        CarProtocol.complete(callback, null, new SecurityException("spoofed caller"));
        assertEquals(3, callback.code);
        assertEquals(4, CarProtocol.member(callback.response, "mErrorType").getInt("tag_value"));
    }

    @Test public void olderGearslipCallbacksWithNoReplyBodyRemainCompatible() {
        RecordingCallback callback = new RecordingCallback(false);
        CarProtocol.complete(callback, null, null);
        assertEquals(2, callback.code);
        assertNull(callback.response);
    }

    @Test public void nullableBundleableRoundTripsTheActualParcelRepresentation() {
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeInt(1); parcel.writeBundle(CarProtocol.appInfo()); parcel.setDataPosition(0);
            assertEquals("androidx.car.app.AppInfo", CarProtocol.readBundleable(parcel).getString("tag_class_name"));
        } finally { parcel.recycle(); }
    }

    @Test public void searchTemplateDecodesItsRealHostKeyboardAndTypedSearchCallback() throws Exception {
        String[] query = new String[1];
        Binder nativeCallback = new Binder() {
            @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
                data.enforceInterface("androidx.car.app.model.ISearchCallback");
                if (code == 16777215) { reply.writeNoException(); reply.writeInt(1); return true; }
                query[0] = data.readString();
                CarProtocol.complete(data.readStrongBinder(), null, null); return true;
            }
        };
        Parcel wire = Parcel.obtain();
        try {
            wire.writeBundle(CarProtocol.searchTemplate("Amst", "Find a place", nativeCallback)); wire.setDataPosition(0);
            TemplateWrapper wrapper = (TemplateWrapper) Bundleable.CREATOR.createFromParcel(wire).get();
            SearchTemplate search = (SearchTemplate) wrapper.getTemplate();
            assertEquals("Amst", search.getInitialSearchText());
            assertEquals("Find a place", search.getSearchHint());
            assertTrue(search.isShowKeyboardByDefault());
            assertEquals(androidx.car.app.model.Action.TYPE_BACK, search.getHeaderAction().getType());
            search.getSearchCallbackDelegate().sendSearchTextChanged("Amsterdam", new OnDoneCallback() {
                @Override public void onSuccess(Bundleable response) {}
                @Override public void onFailure(Bundleable error) { throw new AssertionError("Search callback failed"); }
            });
            assertEquals("Amsterdam", query[0]);
        } finally { wire.recycle(); }
    }

    private static final class RecordingCallback extends Binder {
        final boolean replyBody;
        int code, flags;
        Bundle response;
        RecordingCallback(boolean replyBody) { this.replyBody = replyBody; }
        @Override protected boolean onTransact(int transaction, Parcel data, Parcel reply, int transactionFlags) throws RemoteException {
            data.enforceInterface("androidx.car.app.IOnDoneCallback");
            code = transaction; flags = transactionFlags;
            response = CarProtocol.readBundleable(data);
            if (replyBody) reply.writeNoException();
            return true;
        }
    }
}
