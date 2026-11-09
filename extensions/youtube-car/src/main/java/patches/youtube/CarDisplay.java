package patches.youtube;

import android.app.Activity;
import android.companion.virtual.VirtualDevice;
import android.companion.virtual.VirtualDeviceManager;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

/**
 * Finds whether YouTube is shown on a car display: a parked app on Android Auto, DriveDeck or another car host.
 * <p>
 * A parked app is an ordinary activity that the car host launches onto a display of its own, so the
 * activity itself shows where it is. Nothing here relies on a list of allowed packages or hosts:
 * Android Auto reports its projection through the public car connection provider, DriveDeck names its
 * guest display {@code drivedeck-guest-<package>} and its virtual device {@code DriveDeck}, and a host
 * can give the display the car UI mode. The "any external display" detection needs none of these.
 */
public final class CarDisplay {
    static final String TAG = "NlYouTubeCar";

    /** androidx.car.app.connection.CarConnection, as Android Auto implements it. */
    private static final String CAR_CONNECTION_URI = "content://androidx.car.app.connection";
    private static final String CAR_CONNECTION_STATE = "CarConnectionState";
    private static final int CONNECTION_TYPE_PROJECTION = 2;

    /** Parts of the names that car hosts give their displays and virtual devices. */
    private static final String[] CAR_HOST_NAMES = {
            "drivedeck", "gearslip", "gearhead", "android auto", "androidauto", "automotive",
    };

    private static volatile boolean onCarDisplay;
    private static WeakReference<Activity> activityRef = new WeakReference<>(null);
    private static int activityDisplayId = Display.INVALID_DISPLAY;

    private CarDisplay() {}

    /** Replaced by the patch option: {@code car} for car displays, {@code any} for any external display. */
    public static String getConfiguredDetection() {
        return "car";
    }

    /** Replaced with true by the automotive layout patch. */
    public static boolean isLayoutPatchIncluded() {
        return false;
    }

    /** Replaced with true by the navigation bar patch. */
    public static boolean isNavigationBarPatchIncluded() {
        return false;
    }

    public static boolean isOnCarDisplay() {
        return onCarDisplay;
    }

    /**
     * Injection point, at the start of the onCreate of the main activity, before the activity
     * requests anything, so the first requests already use the layout of the display.
     */
    public static void onMainActivityCreate(Activity activity) {
        try {
            activityRef = new WeakReference<>(activity);
            activityDisplayId = getDisplayId(activity);
            onCarDisplay = isCarDisplay(activity);
            Log.i(TAG, "Main activity on display " + activityDisplayId + ", car display: " + onCarDisplay);
        } catch (RuntimeException ex) {
            Log.e(TAG, "onMainActivityCreate failure", ex);
        }
    }

    /**
     * Injection point.
     *
     * @param bottomBarContainer The container of the navigation bar.
     */
    public static void onBottomBarContainerCreated(View bottomBarContainer) {
        try {
            // Moving the activity to another display changes its size, so the container is laid out again.
            bottomBarContainer.addOnLayoutChangeListener((view, left, top, right, bottom,
                                                          oldLeft, oldTop, oldRight, oldBottom) -> {
                if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                    checkDisplayChanged();
                }
            });

            if (isNavigationBarPatchIncluded()) {
                CarNavigationBar.install(bottomBarContainer);
            }
        } catch (RuntimeException ex) {
            Log.e(TAG, "onBottomBarContainerCreated failure", ex);
        }
    }

    /**
     * The car host can move an open activity between the phone and the car display. The layout comes
     * from the requests the activity made, so with the layout patch the activity is recreated.
     */
    private static void checkDisplayChanged() {
        Activity activity = activityRef.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        final int displayId = getDisplayId(activity);
        if (displayId == activityDisplayId) return;
        activityDisplayId = displayId;

        final boolean wasOnCarDisplay = onCarDisplay;
        onCarDisplay = isCarDisplay(activity);
        Log.i(TAG, "Main activity moved to display " + displayId + ", car display: " + onCarDisplay);

        if (wasOnCarDisplay != onCarDisplay && isLayoutPatchIncluded()) {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    activity.recreate();
                }
            });
        }
    }

    @SuppressWarnings("deprecation")
    private static Display getDisplay(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return activity.getDisplay();
        }
        WindowManager windowManager = activity.getWindowManager();
        return windowManager == null ? null : windowManager.getDefaultDisplay();
    }

    private static int getDisplayId(Activity activity) {
        try {
            Display display = getDisplay(activity);
            return display == null ? Display.INVALID_DISPLAY : display.getDisplayId();
        } catch (RuntimeException ex) {
            Log.e(TAG, "getDisplayId failure", ex);
            return Display.INVALID_DISPLAY;
        }
    }

    private static boolean isCarDisplay(Activity activity) {
        try {
            Display display = getDisplay(activity);
            if (display == null || display.getDisplayId() == Display.DEFAULT_DISPLAY) return false;
            if ("any".equals(getConfiguredDetection())) return true;

            if (isCarUiMode(activity)) {
                Log.i(TAG, "Car display found by the car UI mode");
                return true;
            }
            String displayName = display.getName();
            if (isCarHostName(displayName)) {
                Log.i(TAG, "Car display found by the display name: " + displayName);
                return true;
            }
            String deviceName = getVirtualDeviceName(activity);
            if (isCarHostName(deviceName)) {
                Log.i(TAG, "Car display found by the virtual device name: " + deviceName);
                return true;
            }
            if (isAndroidAutoProjecting(activity)) {
                Log.i(TAG, "Car display found by the Android Auto projection, display: " + displayName);
                return true;
            }
            Log.i(TAG, "Not a car display: " + displayName + ", virtual device: " + deviceName);
        } catch (RuntimeException ex) {
            Log.e(TAG, "isCarDisplay failure", ex);
        }
        return false;
    }

    private static boolean isCarUiMode(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        return (configuration.uiMode & Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_CAR;
    }

    /** Whether a display or virtual device name is one a car host gives. */
    static boolean isCarHostName(String name) {
        if (name == null || name.isEmpty()) return false;
        String lowerCaseName = name.toLowerCase(Locale.ROOT);
        for (String carHostName : CAR_HOST_NAMES) {
            if (lowerCaseName.contains(carHostName)) return true;
        }
        return false;
    }

    /** The name of the virtual device the activity is shown on, or null for a display of the phone. */
    private static String getVirtualDeviceName(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null;
        try {
            final int deviceId = context.getDeviceId();
            if (deviceId == Context.DEVICE_ID_DEFAULT || deviceId == Context.DEVICE_ID_INVALID) return null;

            VirtualDeviceManager manager = context.getSystemService(VirtualDeviceManager.class);
            if (manager == null) return null;
            List<VirtualDevice> devices = manager.getVirtualDevices();
            for (VirtualDevice device : devices) {
                if (device.getDeviceId() != deviceId) continue;
                String name = device.getName();
                if (name != null) return name;
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
                        ? device.getPersistentDeviceId() : null;
            }
        } catch (RuntimeException ex) {
            Log.e(TAG, "getVirtualDeviceName failure", ex);
        }
        return null;
    }

    /**
     * Android Auto answers the car connection provider for every app, unless an Android Auto flag
     * limits it. The query is only made for an activity on a display other than the phone display.
     */
    private static boolean isAndroidAutoProjecting(Context context) {
        final Uri uri = Uri.parse(CAR_CONNECTION_URI);
        try (ContentProviderClient client = context.getContentResolver().acquireUnstableContentProviderClient(uri)) {
            if (client == null) return false;
            try (Cursor cursor = client.query(uri,
                    new String[]{CAR_CONNECTION_STATE}, null, null, null)) {
                if (cursor == null || !cursor.moveToFirst()) return false;
                final int column = cursor.getColumnIndex(CAR_CONNECTION_STATE);
                return column >= 0 && cursor.getInt(column) == CONNECTION_TYPE_PROJECTION;
            }
        } catch (Exception ex) {
            // A car host that is not installed, or does not answer this app.
            Log.i(TAG, "Car connection is not available: " + ex);
            return false;
        }
    }
}
