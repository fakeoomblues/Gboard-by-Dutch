package app.morphe.extension.nexuslauncher;

import android.util.Log;

/**
 * Framework compatibility shim shipped by the addon.
 *
 * The Pixel launcher is compiled against Google's framework build, which carries APIs
 * that plain AOSP (and most OEM builds) do not have. Each such call site is redirected
 * by the launcher patch to one of these methods.
 *
 * Rule: use the real API through reflection when the device has it (full feature
 * parity), otherwise fall back to the value AOSP would produce.
 */
public final class FrameworkShim {

    private static final String TAG = "KitShim";
    private static boolean bypassed;

    private FrameworkShim() {
    }

    /**
     * Hidden framework APIs (ThreadedRenderer.setContextPriority, DesktopModeFlags, …) are
     * blocked for normal apps by the hidden-API policy, even when the device has them —
     * a plain getMethod() throws NoSuchMethodException. LSPosed's HiddenApiBypass lifts
     * that restriction without root, which is what makes the shim a real passthrough.
     */
    private static synchronized void bypass() {
        if (bypassed) return;
        bypassed = true;
        try {
            org.lsposed.hiddenapibypass.HiddenApiBypass.addHiddenApiExemptions("");
        } catch (Throwable t) {
            Log.w(TAG, "HiddenApiBypass unavailable: " + t);
        }
    }

    // ---------------------------------------------------------------- desktop mode

    /** android.window.DesktopModeFlags.isDesktopModeForcedEnabled() */
    public static boolean isDesktopModeForcedEnabled() {
        return bool("android.window.DesktopModeFlags", "isDesktopModeForcedEnabled", false);
    }

    /** android.window.DesktopExperienceFlags.&lt;flag&gt;.isTrue() */
    public static boolean desktopFlag(String flagName) {
        bypass();
        try {
            Class<?> c = Class.forName("android.window.DesktopExperienceFlags");
            Object flag = c.getField(flagName).get(null);
            Object v = flag.getClass().getMethod("isTrue").invoke(flag);
            return v instanceof Boolean && (Boolean) v;
        } catch (Throwable t) {
            return false;
        }
    }

    // ------------------------------------------------------------------ rendering

    /** android.view.ThreadedRenderer.setContextPriority(int) — performance hint only. */
    public static void setContextPriority(int priority) {
        bypass();
        try {
            Class<?> c = Class.forName("android.view.ThreadedRenderer");
            c.getMethod("setContextPriority", int.class).invoke(null, priority);
        } catch (Throwable ignored) {
            // absent on this framework: no-op
        }
    }

    /** android.view.SurfaceControl.setDebugUsageAfterRelease(boolean) — debug only. */
    public static void setDebugUsageAfterRelease(boolean enabled) {
        bypass();
        try {
            Class<?> c = Class.forName("android.view.SurfaceControl");
            c.getMethod("setDebugUsageAfterRelease", boolean.class).invoke(null, enabled);
        } catch (Throwable ignored) {
        }
    }

    /** android.view.ViewRootImpl.setDebugWrongThreadInit(boolean) — debug only. */
    public static boolean setDebugWrongThreadInit(boolean enabled) {
        bypass();
        try {
            Class<?> c = Class.forName("android.view.ViewRootImpl");
            Object r = c.getMethod("setDebugWrongThreadInit", boolean.class).invoke(null, enabled);
            return r instanceof Boolean && (Boolean) r;
        } catch (Throwable ignored) {
            return true;
        }
    }

    // -------------------------------------------------------------------- protolog

    /** com.android.internal.protolog.ProtoLog.init(IProtoLogGroup[]) — logging only. */
    public static void initProtoLog() {
        bypass();
        try {
            Class<?> protoLog = Class.forName("com.android.internal.protolog.ProtoLog");
            Class<?> group = Class.forName("com.android.internal.protolog.common.IProtoLogGroup");
            Object arr = java.lang.reflect.Array.newInstance(group, 0);
            protoLog.getMethod("init", arr.getClass()).invoke(null, arr);
        } catch (Throwable ignored) {
            // signature differs or class absent: skip
        }
    }


    // ---------------------------------------------------------------------- display

    /** android.view.Display.getMinSizeDimensionDp() — Pixel display API. */
    public static float getMinSizeDimensionDp(android.view.Display display) {
        if (display == null) return 0f;
        try {
            Object v = display.getClass().getMethod("getMinSizeDimensionDp").invoke(display);
            return v instanceof Float ? (Float) v : 0f;
        } catch (Throwable ignored) {
            // absent: AOSP has no minimum, so 0 dp is the equivalent behaviour
            return 0f;
        }
    }


    // ------------------------------------------------------------------ window metrics

    /**
     * android.view.WindowManager.getPossibleMaximumWindowMetrics(int) — public since API 30,
     * but absent from this image's boot image. Equivalent behaviour: the maximum metrics of
     * the display (single-element set), which is what the API returns on a normal device.
     */
    public static java.util.Set<android.view.WindowMetrics> getPossibleMaximumWindowMetrics(
            android.view.WindowManager wm, int displayId) {
        java.util.LinkedHashSet<android.view.WindowMetrics> out = new java.util.LinkedHashSet<>();
        if (wm == null) return out;
        try {
            Object r = wm.getClass().getMethod("getPossibleMaximumWindowMetrics", int.class)
                    .invoke(wm, displayId);
            if (r instanceof java.util.Set) {
                //noinspection unchecked
                return (java.util.Set<android.view.WindowMetrics>) r;
            }
        } catch (Throwable ignored) {
        }
        try {
            out.add(wm.getMaximumWindowMetrics());
        } catch (Throwable ignored) {
        }
        return out;
    }


    // ------------------------------------------------------------------ cutout rotation

    /**
     * android.view.DisplayCutout.getRotated(int,int,int,int) — hidden framework API.
     * Equivalent behaviour when absent: the cutout is unchanged (no rotation applied),
     * which is what a device without the API would compute.
     */
    public static android.view.DisplayCutout getRotated(android.view.DisplayCutout cutout,
                                                        int msw, int msh, int rot, int physRot) {
        if (cutout == null) return null;
        try {
            Object r = cutout.getClass()
                    .getMethod("getRotated", int.class, int.class, int.class, int.class)
                    .invoke(cutout, msw, msh, rot, physRot);
            if (r instanceof android.view.DisplayCutout) return (android.view.DisplayCutout) r;
        } catch (Throwable ignored) {
        }
        return cutout;
    }


    // ---------------------------------------------------------------------- report

    /**
     * Proves the shim policy: every API is called for real when the framework has it,
     * and only falls back when it does not. Used by the console's self-check.
     */
    public static String report() {
        StringBuilder sb = new StringBuilder("framework shim report\n");
        sb.append(row("ThreadedRenderer.setContextPriority", has("android.view.ThreadedRenderer", "setContextPriority", int.class)));
        sb.append(row("SurfaceControl.setDebugUsageAfterRelease", has("android.view.SurfaceControl", "setDebugUsageAfterRelease", boolean.class)));
        sb.append(row("ViewRootImpl.setDebugWrongThreadInit", has("android.view.ViewRootImpl", "setDebugWrongThreadInit", boolean.class)));
        sb.append(row("ProtoLog.init(IProtoLogGroup[])", hasProtoLogInit()));
        sb.append(row("DesktopModeFlags.isDesktopModeForcedEnabled", has("android.window.DesktopModeFlags", "isDesktopModeForcedEnabled")));
        sb.append(row("DesktopExperienceFlags.isTrue", hasFlag()));
        sb.append(row("Display.getMinSizeDimensionDp", has("android.view.Display", "getMinSizeDimensionDp")));
        sb.append(row("WindowManager.getPossibleMaximumWindowMetrics", has("android.view.WindowManager", "getPossibleMaximumWindowMetrics", int.class)));
        sb.append(row("DisplayCutout.getRotated", has("android.view.DisplayCutout", "getRotated", int.class, int.class, int.class, int.class)));
        return sb.toString();
    }

    private static String row(String api, boolean present) {
        return "  " + (present ? "PASS-THROUGH  " : "fallback      ") + api + "\n";
    }

    private static boolean has(String cls, String method, Class<?>... args) {
        bypass();
        try {
            Class.forName(cls).getMethod(method, args);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean hasProtoLogInit() {
        try {
            Class<?> group = Class.forName("com.android.internal.protolog.common.IProtoLogGroup");
            Class.forName("com.android.internal.protolog.ProtoLog")
                    .getMethod("init", java.lang.reflect.Array.newInstance(group, 0).getClass());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean hasFlag() {
        try {
            Object f = Class.forName("android.window.DesktopExperienceFlags")
                    .getField("ENABLE_DESKTOP_FIRST_BASED_DEFAULT_TO_DESKTOP_BUGFIX").get(null);
            f.getClass().getMethod("isTrue");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    // ---------------------------------------------------------------------- utils

    private static boolean bool(String className, String method, boolean fallback) {
        bypass();
        try {
            Class<?> c = Class.forName(className);
            Object v = c.getMethod(method).invoke(null);
            return v instanceof Boolean ? (Boolean) v : fallback;
        } catch (Throwable t) {
            Log.d(TAG, className + "." + method + " unavailable, using " + fallback);
            return fallback;
        }
    }
}
