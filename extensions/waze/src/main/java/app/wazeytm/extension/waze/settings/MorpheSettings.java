package app.wazeytm.extension.waze.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

/** Retain the existing audio preference while shipping only the YT Music integration. */
public final class MorpheSettings {
    public enum Feature { PATCHED_YOUTUBE_MUSIC }
    private MorpheSettings() { }

    public static boolean isEnabled(Feature feature) {
        Context context = getContext();
        return context == null || context.getSharedPreferences("morphe_patch_settings", Context.MODE_PRIVATE)
                .getBoolean("patched_youtube_music_support", true);
    }

    @SuppressLint("PrivateApi")
    public static Context getContext() {
        try {
            return (Context) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
        } catch (Exception exception) {
            Log.e("WazeAudioContext", "Application context unavailable", exception);
            return null;
        }
    }
}
