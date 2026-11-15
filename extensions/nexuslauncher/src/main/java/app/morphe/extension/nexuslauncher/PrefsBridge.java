package app.morphe.extension.nexuslauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.UserManager;

/**
 * LauncherPrefs keeps a credential-encrypted context for most preference items. A launcher
 * that is the HOME app is started before user 0 is unlocked on a cold boot, and any
 * credential-protected read throws until then, which kills the process on every boot. Serve
 * the device-protected context for that window; the values are re-read after unlock.
 */
public final class PrefsBridge {

    private PrefsBridge() {
    }

    /** Replaces the direct `encryptedContext` field read in LauncherPrefs.getSharedPrefs. */
    public static Context context(Object launcherPrefs) {
        Context context = null;
        try {
            java.lang.reflect.Field field =
                    launcherPrefs.getClass().getDeclaredField("encryptedContext");
            field.setAccessible(true);
            context = (Context) field.get(launcherPrefs);
        } catch (Throwable ignored) {
            // fall through to the locked-state decision below
        }
        if (context == null) {
            return null;
        }
        try {
            UserManager users = context.getSystemService(UserManager.class);
            if (users != null && !users.isUserUnlocked()) {
                return context.createDeviceProtectedStorageContext();
            }
        } catch (Throwable ignored) {
            // an unknown lock state is treated as unlocked, which is the pre-existing behaviour
        }
        return context;
    }

    /** Kept for symmetry with the prototype; unused by the shipped patches. */
    public static SharedPreferences prefs(Context context, String name) {
        return context.getSharedPreferences(name, 0);
    }
}
