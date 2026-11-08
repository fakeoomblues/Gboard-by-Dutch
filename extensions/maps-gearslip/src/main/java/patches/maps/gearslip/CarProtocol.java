package patches.maps.gearslip;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The stable Car App Library wire schema; no duplicate AndroidX classes enter Maps. */
final class CarProtocol {
    static final int VERSION_TRANSACTION = 16777215;
    private CarProtocol() {}

    static Bundle object(String type) {
        Bundle bundle = new Bundle();
        bundle.putInt("tag_class_type", 5);
        bundle.putString("tag_class_name", type);
        return bundle;
    }

    static Bundle primitive(Object value) {
        Bundle bundle = new Bundle();
        bundle.putInt("tag_class_type", 0);
        if (value instanceof Integer) bundle.putInt("tag_value", (Integer) value);
        else if (value instanceof String) bundle.putString("tag_value", (String) value);
        else if (value instanceof Boolean) bundle.putBoolean("tag_value", (Boolean) value);
        else if (value instanceof Long) bundle.putLong("tag_value", (Long)value);
        else if (value instanceof Double) bundle.putDouble("tag_value", (Double)value);
        else throw new IllegalArgumentException("Unsupported protocol primitive");
        return bundle;
    }

    static void field(Bundle bundle, String name, Bundle value) {
        bundle.putBundle(bundle.getString("tag_class_name") + name, value);
    }

    static Bundle member(Bundle bundle, String name) {
        if (bundle == null) throw new IllegalArgumentException("Missing protocol object");
        Bundle value = bundle.getBundle(bundle.getString("tag_class_name") + name);
        if (value == null) throw new IllegalArgumentException("Missing protocol field " + name);
        return value;
    }

    static Surface surface(Bundle container) {
        if (container == null) throw new IllegalArgumentException("Missing surface container");
        // Bundleable omits null object fields. Android Auto legitimately sends a
        // SurfaceContainer whose Surface is absent while changing host layouts.
        Bundle value = container.getBundle(container.getString("tag_class_name") + "mSurface");
        return value == null ? null : value.getParcelable("tag_value");
    }

    static Bundle emptyList() {
        return list(Collections.emptyList());
    }

    static Bundle list(List<Bundle> values) {
        Bundle bundle = new Bundle();
        bundle.putInt("tag_class_type", 4);
        bundle.putParcelableArrayList("tag_value", new ArrayList<>(values));
        return bundle;
    }

    static Bundle text(String value) {
        Bundle text = object("androidx.car.app.model.CarText");
        field(text, "mText", primitive(value));
        field(text, "mSpans", emptyList());
        field(text, "mTextVariants", emptyList());
        field(text, "mSpansForVariants", emptyList());
        return text;
    }

    private static Bundle click(IBinder callback) {
        Bundle delegate = object("androidx.car.app.model.OnClickDelegateImpl");
        field(delegate, "mIsParkedOnly", primitive(false));
        field(delegate, "mListener", interfaceBinder("androidx.car.app.model.IOnClickListener$Stub", callback));
        return delegate;
    }

    static Bundle icon(Bitmap bitmap) {
        return icon(bitmap,true);
    }
    static Bundle icon(Bitmap bitmap,boolean tinted){
        Bundle compat = new Bundle();
        compat.putInt("type", 1); // IconCompat.TYPE_BITMAP, serialized by the public Bundler IMAGE type.
        compat.putParcelable("obj", bitmap);
        compat.putInt("int1", 0); compat.putInt("int2", 0);
        Bundle image = new Bundle(); image.putInt("tag_class_type", 6); image.putBundle("tag_value", compat);
        Bundle icon = object("androidx.car.app.model.CarIcon");
        field(icon, "mType", primitive(1));
        field(icon, "mIcon", image);
        if(tinted){
            Bundle tint = object("androidx.car.app.model.CarColor");
            field(tint, "mType", primitive(1)); // Monochrome controls follow host contrast.
            field(icon, "mTint", tint);
        }
        return icon;
    }

    static Bundle action(String title, Bitmap bitmap, IBinder callback) {
        Bundle action = object("androidx.car.app.model.Action");
        field(action, "mType", primitive(1));
        field(action, "mIsEnabled", primitive(true));
        if (title != null) field(action, "mTitle", text(title));
        field(action, "mIcon", icon(bitmap));
        field(action, "mOnClickDelegate", click(callback));
        return action;
    }

    static Bundle panAction() {
        Bundle action = object("androidx.car.app.model.Action");
        field(action, "mType", primitive(65540)); // Action.PAN: standard host pan mode, not a private command.
        field(action, "mIsEnabled", primitive(true));
        Bundle icon = object("androidx.car.app.model.CarIcon");
        field(icon, "mType", primitive(7));
        field(action, "mIcon", icon);
        return action;
    }

    static Bundle controlRow(String title, IBinder callback) {
        Bundle row = object("androidx.car.app.model.Row");
        field(row, "mTitle", text(title)); field(row, "mTexts", emptyList());
        field(row, "mIsEnabled", primitive(true)); field(row, "mNumericDecoration", primitive(-1));
        field(row, "mMetadata", object("androidx.car.app.model.Metadata"));
        field(row, "mOnClickDelegate", click(callback));
        return row;
    }
    static Bundle contentRow(String title,List<String> texts,IBinder callback,boolean enabled){
        Bundle row=object("androidx.car.app.model.Row");
        field(row,"mTitle",text(title));List<Bundle> lines=new ArrayList<>();
        for(String line:texts)if(lines.size()<2)lines.add(text(line));
        field(row,"mTexts",list(lines));field(row,"mIsEnabled",primitive(enabled));field(row,"mIsBrowsable",primitive(callback!=null));
        field(row,"mNumericDecoration",primitive(-1));field(row,"mMetadata",object("androidx.car.app.model.Metadata"));
        if(callback!=null)field(row,"mOnClickDelegate",click(callback));return row;
    }
    static Bundle titledAction(String title,IBinder callback,boolean enabled){
        Bundle action=object("androidx.car.app.model.Action");field(action,"mType",primitive(1));field(action,"mTitle",text(title));
        field(action,"mIsEnabled",primitive(enabled));field(action,"mOnClickDelegate",click(callback));return action;
    }
    private static Bundle headerAction(boolean home){
        Bundle action=object("androidx.car.app.model.Action");field(action,"mType",primitive(home?65538:65539));
        if(home){Bundle icon=object("androidx.car.app.model.CarIcon");field(icon,"mType",primitive(5));field(action,"mIcon",icon);}
        return action;
    }
    static Bundle pageTemplate(String title,boolean home,boolean preview,List<Bundle> rows,List<Bundle> paneActions,
            List<Bundle> actions,List<Bundle> mapActions,IBinder panCallback,int api){
        Bundle content;
        if(preview){
            Bundle pane=object("androidx.car.app.model.Pane");field(pane,"mRows",list(rows));field(pane,"mActionList",list(paneActions));field(pane,"mIsLoading",primitive(rows.isEmpty()));
            content=object("androidx.car.app.model.PaneTemplate");field(content,"mPane",pane);
        }else{
            Bundle items=object("androidx.car.app.model.ItemList");field(items,"mItems",list(rows));
            content=object("androidx.car.app.model.ListTemplate");field(content,"mSingleList",items);field(content,"mSectionedLists",emptyList());field(content,"mActions",emptyList());field(content,"mIsLoading",primitive(false));
        }
        field(content,"mTitle",text(title));field(content,"mHeaderAction",headerAction(home));
        Bundle strip=object("androidx.car.app.model.ActionStrip");field(strip,"mActions",list(actions));
        Bundle mapStrip=object("androidx.car.app.model.ActionStrip");field(mapStrip,"mActions",list(mapActions));
        Bundle controller=object("androidx.car.app.navigation.model.MapController");field(controller,"mMapActionStrip",mapStrip);
        if(panCallback!=null){Bundle pan=object("androidx.car.app.navigation.model.PanModeDelegateImpl");field(pan,"mStub",interfaceBinder("androidx.car.app.navigation.model.IPanModeListener$Stub",panCallback));field(controller,"mPanModeDelegate",pan);}
        Bundle result;
        if(api>=7){
            result=object("androidx.car.app.navigation.model.MapWithContentTemplate");field(result,"mContentTemplate",content);
        }else{
            result=object("androidx.car.app.navigation.model.MapTemplate");
            field(result,preview?"mPane":"mItemList",member(content,preview?"mPane":"mSingleList"));
            Bundle header=object("androidx.car.app.model.Header");field(header,"mTitle",text(title));field(header,"mStartHeaderAction",headerAction(home));field(header,"mEndHeaderActions",emptyList());field(result,"mHeader",header);
            // Retain the pre-Header field names for older host-side model classes.
            field(result,"mTitle",text(title));field(result,"mHeaderAction",headerAction(home));field(result,"mMapActionStrip",mapStrip);
        }
        field(result,"mMapController",controller);field(result,"mActionStrip",strip);
        return wrap(result,"morphe-maps-content");
    }

    static Bundle binder(IBinder value) {
        Bundle bundle = new Bundle();
        bundle.putInt("tag_class_type", 9);
        bundle.putBinder("tag_value", value);
        return bundle;
    }

    static Bundle interfaceBinder(String factoryClass, IBinder value) {
        Bundle bundle = new Bundle();
        bundle.putInt("tag_class_type", 1);
        bundle.putString("tag_class_name", factoryClass);
        bundle.putBinder("tag_value", value);
        return bundle;
    }

    static Bundle appInfo() {
        Bundle info = object("androidx.car.app.AppInfo");
        field(info, "mMinCarAppApiLevel", primitive(5));
        field(info, "mLatestCarAppApiLevel", primitive(7));
        field(info, "mLibraryVersion", primitive("Morphe Maps native car adapter"));
        return info;
    }

    static Bundle trip(NativeGuidance.Snapshot value,long now){
        return CarNavigationModels.trip(value,now,false);
    }

    static Bundle navigationTemplate(NativeGuidance.Snapshot value,List<Bundle> actions,List<Bundle> mapActions,IBinder panCallback,long now,boolean imperial){
        Bundle result=template(null,actions,mapActions,panCallback);
        CarNavigationModels.apply(member(result,"mTemplate"),value,now,imperial);
        return result;
    }

    static Bundle template(Throwable failure) {
        return template(failure, Collections.emptyList(), Collections.emptyList());
    }

    static Bundle template(Throwable failure, List<Bundle> actions, List<Bundle> mapActions) {
        return template(failure, actions, mapActions, null);
    }

    static Bundle template(Throwable failure, List<Bundle> actions, List<Bundle> mapActions, IBinder panCallback) {
        Bundle template;
        if (failure == null) {
            Bundle strip = object("androidx.car.app.model.ActionStrip");
            field(strip, "mActions", list(actions));
            template = object("androidx.car.app.navigation.model.NavigationTemplate");
            field(template, "mActionStrip", strip);
            if (!mapActions.isEmpty()) {
                Bundle mapStrip = object("androidx.car.app.model.ActionStrip");
                field(mapStrip, "mActions", list(mapActions));
                field(template, "mMapActionStrip", mapStrip);
            }
            if (panCallback != null) {
                Bundle pan = object("androidx.car.app.navigation.model.PanModeDelegateImpl");
                field(pan, "mStub", interfaceBinder("androidx.car.app.navigation.model.IPanModeListener$Stub", panCallback));
                field(template, "mPanModeDelegate", pan);
            }
        } else {
            Throwable cause = failure;
            for (int i = 0; i < 8 && cause.getCause() != null && cause.getCause() != cause; i++) cause = cause.getCause();
            String detail = cause.getMessage();
            if (detail == null || detail.trim().isEmpty()) detail = cause.getClass().getSimpleName();
            Bundle text = object("androidx.car.app.model.CarText");
            field(text, "mText", primitive("Google Maps car UI could not start: " + detail));
            field(text, "mSpans", emptyList());
            field(text, "mTextVariants", emptyList());
            field(text, "mSpansForVariants", emptyList());
            template = object("androidx.car.app.model.MessageTemplate");
            field(template, "mMessage", text);
            field(template, "mActionList", emptyList());
        }
        return wrap(template, failure == null ? "morphe-maps-native" : "morphe-maps-error");
    }

    static Bundle controlsTemplate(String title, List<Bundle> rows) {
        Bundle items = object("androidx.car.app.model.ItemList");
        field(items, "mItems", list(rows));
        Bundle back = object("androidx.car.app.model.Action"); field(back, "mType", primitive(65539));
        Bundle template = object("androidx.car.app.model.ListTemplate");
        field(template, "mTitle", text(title)); field(template, "mHeaderAction", back);
        field(template, "mSingleList", items); field(template, "mSectionedLists", emptyList());
        return wrap(template, "morphe-maps-controls");
    }

    static Bundle searchTemplate(String query, String hint, IBinder callback) {
        return searchTemplate(query,hint,callback,Collections.emptyList());
    }
    static Bundle searchTemplate(String query,String hint,IBinder callback,List<Bundle> rows){
        Bundle delegate = object("androidx.car.app.model.SearchCallbackDelegateImpl");
        field(delegate, "mStubCallback", interfaceBinder("androidx.car.app.model.ISearchCallback$Stub", callback));
        Bundle back = object("androidx.car.app.model.Action");
        field(back, "mType", primitive(65539));
        Bundle items = object("androidx.car.app.model.ItemList");
        field(items, "mItems", list(rows));
        Bundle search = object("androidx.car.app.model.SearchTemplate");
        field(search, "mInitialSearchText", primitive(query));
        field(search, "mSearchHint", primitive(hint));
        field(search, "mSearchCallbackDelegate", delegate);
        field(search, "mShowKeyboardByDefault", primitive(true));
        field(search, "mHeaderAction", back);
        field(search, "mItemList", items);
        return wrap(search, "morphe-maps-input");
    }

    private static Bundle wrap(Bundle template, String id) {
        Bundle wrapper = object("androidx.car.app.model.TemplateWrapper");
        field(wrapper, "mTemplate", template);
        field(wrapper, "mId", primitive(id));
        field(wrapper, "mCurrentTaskStep", primitive(1));
        screenStack(wrapper, false);
        return wrapper;
    }

    /** Public CAL screen metadata is top first, matching the local Back path. */
    static void screenStack(Bundle wrapper, boolean controlsParent) {
        String id=member(wrapper,"mId").getString("tag_value");
        String type=member(wrapper,"mTemplate").getString("tag_class_name");
        List<Bundle> stack=new ArrayList<>();
        stack.add(templateInfo(type,id));
        if("morphe-maps-input".equals(id)&&controlsParent)
            stack.add(templateInfo("androidx.car.app.model.ListTemplate","morphe-maps-controls"));
        if("morphe-maps-input".equals(id)||"morphe-maps-controls".equals(id))
            stack.add(templateInfo("androidx.car.app.navigation.model.NavigationTemplate","morphe-maps-native"));
        field(wrapper,"mTemplateInfoForScreenStack",list(stack));
        field(wrapper,"mCurrentTaskStep",primitive(stack.size()));
    }

    static Bundle templateInfo(String type,String id){
        Bundle info=object("androidx.car.app.model.TemplateInfo");
        Bundle clazz=new Bundle();clazz.putInt("tag_class_type",8);clazz.putString("tag_value",type);
        field(info,"mTemplateClass",clazz);field(info,"mTemplateId",primitive(id));
        return info;
    }

    static Bundle failure(Throwable failure) {
        Bundle response = object("androidx.car.app.FailureResponse");
        int kind = failure instanceof SecurityException ? 4 : failure instanceof IllegalArgumentException ? 3 : 5;
        field(response, "mErrorType", primitive(kind));
        field(response, "mStackTrace", primitive(Log.getStackTraceString(failure)));
        return response;
    }

    static Bundle readBundleable(Parcel data) {
        return data.readInt() == 0 ? null : data.readBundle(CarProtocol.class.getClassLoader());
    }

    static <T> T nullable(Parcel data, Parcelable.Creator<T> creator) {
        return data.readInt() == 0 ? null : creator.createFromParcel(data);
    }

    static boolean basic(String descriptor, int code, Parcel data, Parcel reply) {
        if (code == IBinder.INTERFACE_TRANSACTION) {
            if (reply != null) reply.writeString(descriptor);
            return true;
        }
        data.enforceInterface(descriptor);
        if (code == VERSION_TRANSACTION) {
            if (reply != null) { reply.writeNoException(); reply.writeInt(1); }
            return true;
        }
        return false;
    }

    interface Writer { void write(Parcel parcel); }

    /** Newer hosts use synchronous replies; older Gearslip stubs leave the reply empty. */
    static void send(IBinder target, String descriptor, int transaction, Writer writer) throws RemoteException {
        Parcel data = Parcel.obtain(), reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(descriptor);
            writer.write(data);
            if (!target.transact(transaction, data, reply, 0)) throw new RemoteException("Unsupported " + descriptor + " transaction " + transaction);
            if (reply.dataSize() > 0) reply.readException();
        } finally { reply.recycle(); data.recycle(); }
    }

    static void complete(IBinder callback, Bundle value, Throwable error) {
        if (callback == null) return;
        Bundle response = error == null ? value : failure(error);
        try {
            send(callback, "androidx.car.app.IOnDoneCallback", error == null ? 2 : 3, data -> {
                data.writeInt(response == null ? 0 : 1);
                if (response != null) data.writeBundle(response);
            });
        } catch (RemoteException | RuntimeException failure) {
            Log.w("MapsNativeCar", "Car host callback disconnected", failure);
        }
    }
}
