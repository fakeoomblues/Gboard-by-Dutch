package app.morphe.extension.nexuslauncher;

import android.content.Context;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The prediction session is created by {@code AppPredictor}'s constructor, which calls the
 * {@code app_prediction} system service. That service requires PACKAGE_USAGE_STATS, a
 * privileged permission a sideloaded launcher cannot hold (the appop alone is refused), so the
 * call throws on the model-loader thread and the All Apps list is starved.
 *
 * <p>The framework constructor does (verified with dexdump on framework.jar):
 *
 * <pre>
 * mCloseGuard = CloseGuard.get();  mIsClosed = new AtomicBoolean(false);
 * mRegisteredCallbacks = new ArrayMap&lt;&gt;();
 * mPredictionManager = IPredictionManager.Stub.asInterface(
 *         ServiceManager.getService("app_prediction"));
 * mSessionId = new AppPredictionSessionId(pkg + ":" + UUID.randomUUID(), context.getUserId());
 * token = AppPredictor.getToken();
 * mPredictionManager.createPredictionSession(appPredictionContext, mSessionId, token);
 * </pre>
 *
 * <p>This class replays that with the manager bound to a Shevery/Shizuku-wrapped binder, so the
 * call runs as shell (uid 2000), which does hold PACKAGE_USAGE_STATS. The registration then
 * continues exactly as PredictorState does it: the launcher's own callback is instantiated,
 * {@code registerPredictionUpdates} and {@code requestPredictionUpdate} are called on the
 * session, and the session is installed on the state object.
 */
public final class PredictorBridge {

    private static final String TAG = "KitPredict";

    private PredictorBridge() {
    }

    /** Replaces PredictorState.registerPredictor(...) at its call sites. */
    public static void registerAsync(final Object state, final Object context,
                                     final Object predictionContext, final Object model,
                                     final Object factory) {
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    register(state, context, predictionContext, model, factory);
                } catch (Throwable t) {
                    Log.w(TAG, "prediction registration failed: " + describe(t));
                }
            }
        }, "kit-predictor");
        worker.setDaemon(true);
        worker.start();
    }

    private static void register(Object state, Object context, Object predictionContext,
                                 Object model, Object factory) throws Exception {
        Object existing = field(state, "predictor");
        if (existing != null) {
            try {
                existing.getClass().getMethod("destroy").invoke(existing);
            } catch (Throwable ignored) {
                // best effort
            }
        }
        setField(state, "predictor", null);

        Object session = createSession((Context) context, predictionContext);
        if (session == null) {
            Log.w(TAG, "no prediction session could be created");
            return;
        }

        setField(state, "lastTargets", emptyList());

        Object callback = newLauncherCallback(state, model, factory);
        if (callback != null) {
            Object executor = modelExecutor();
            Method register = findMethod(session.getClass(), "registerPredictionUpdates", 2);
            register.setAccessible(true);
            register.invoke(session, executor, callback);
            Method request = findMethod(session.getClass(), "requestPredictionUpdate", 0);
            request.setAccessible(true);
            request.invoke(session);
        }

        setField(state, "predictor", session);
        Log.i(TAG, "prediction session installed on the launcher state");
        seedLocalTargets(callback, context);

    }

    /** Builds an AppPredictor bound to the shell-wrapped prediction manager. */
    private static Object createSession(Context context, Object predictionContext) {
        try {
            Class<?> shizuku = shizukuClass("Shizuku");
            boolean ping = (Boolean) shizuku.getMethod("pingBinder").invoke(null);
            if (!ping) {
                Log.w(TAG, "Shevery/Shizuku not running; prediction stays unavailable");
                return null;
            }
            Class<?> wrapperClass = shizukuClass("ShizukuBinderWrapper");

            IBinder raw = (IBinder) Class.forName("android.os.ServiceManager")
                    .getMethod("getService", String.class).invoke(null, "app_prediction");
            if (raw == null) {
                Log.w(TAG, "app_prediction service not published");
                return null;
            }
            IBinder wrapped = (IBinder) wrapperClass.getConstructor(IBinder.class).newInstance(raw);

            Class<?> stub = Class.forName("android.app.prediction.IPredictionManager$Stub");
            Object manager = stub.getMethod("asInterface", IBinder.class).invoke(null, wrapped);

            Class<?> predictorClass = Class.forName("android.app.prediction.AppPredictor");
            Object session = unsafeAllocate(predictorClass);
            setField(session, "mCloseGuard", Class.forName("dalvik.system.CloseGuard")
                    .getMethod("get").invoke(null));
            setField(session, "mIsClosed", new AtomicBoolean(false));
            setField(session, "mRegisteredCallbacks", Class.forName("android.util.ArrayMap")
                    .getDeclaredConstructor().newInstance());
            setField(session, "mPredictionManager", manager);

            Class<?> idClass = Class.forName("android.app.prediction.AppPredictionSessionId");
            int userId = (Integer) Context.class.getMethod("getUserId").invoke(context);
            Object sessionId = idClass.getDeclaredConstructor(String.class, int.class)
                    .newInstance(context.getPackageName() + ":" + UUID.randomUUID(), userId);
            setField(session, "mSessionId", sessionId);

            Method tokenGetter = predictorClass.getDeclaredMethod("getToken");
            tokenGetter.setAccessible(true);
            IBinder token = (IBinder) tokenGetter.invoke(null);

            Method create = Class.forName("android.app.prediction.IPredictionManager")
                    .getMethod("createPredictionSession",
                            Class.forName("android.app.prediction.AppPredictionContext"),
                            idClass, IBinder.class);
            create.invoke(manager, predictionContext, sessionId, token);
            Log.i(TAG, "prediction session created through shell uid");
            return session;
        } catch (Throwable t) {
            Log.w(TAG, "shell prediction session failed: " + describe(t));
            return null;
        }
    }

    /** Feeds the launcher's callback a local target set when the provider is silent. */
    private static void seedLocalTargets(final Object callback, final Object context) {
        if (callback == null || context == null) return;
        try {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Wait for the workspace bind to commit: a model update fired while
                        // ModelCallbacks is binding cancels the active bind task, which drops
                        // every inflated workspace/hotseat item.
                        Thread.sleep(10000);
                        java.util.List<Object> targets = buildLocalTargets(context);
                        if (targets.isEmpty()) {
                            Log.w(TAG, "no local targets to seed");
                            return;
                        }
                        Method onTargets = null;
                        for (Method m : callback.getClass().getDeclaredMethods()) {
                            if (m.getName().equals("onTargetsAvailable") && m.getParameterCount() == 1) {
                                onTargets = m;
                                break;
                            }
                        }
                        if (onTargets == null) {
                            Log.w(TAG, "callback has no onTargetsAvailable");
                            return;
                        }
                        onTargets.setAccessible(true);
                        onTargets.invoke(callback, targets);
                        Log.i(TAG, "seeded " + targets.size() + " local prediction targets");
                    } catch (Throwable t) {
                        Log.w(TAG, "seeding failed: " + describe(t));
                    }
                }
            }, "kit-predict-seed");
            t.setDaemon(true);
            t.start();
        } catch (Throwable t) {
            Log.w(TAG, "could not start seeding thread: " + describe(t));
        }
    }

    /**
     * Builds AppTargets exactly the way the helper's KitPredictionService does (AppTargetId +
     * ShortcutInfo + setClassName). Any extra setter call on this build clears the target and
     * build() then throws "No target is set".
     */
    private static java.util.List<Object> buildLocalTargets(Object context) throws Exception {
        Class<?> idClass = Class.forName("android.app.prediction.AppTargetId");
        Class<?> builderClass = Class.forName("android.app.prediction.AppTarget$Builder");
        Class<?> appTargetClass = Class.forName("android.app.prediction.AppTarget");
        Class<?> shortcutClass = Class.forName("android.content.pm.ShortcutInfo");
        Context ctx = (Context) context;
        java.util.List<Object> targets = new java.util.ArrayList<>();
        android.content.pm.PackageManager pm = ((Context) context).getPackageManager();
        android.content.Intent launcher = new android.content.Intent(android.content.Intent.ACTION_MAIN)
                .addCategory(android.content.Intent.CATEGORY_LAUNCHER);
        java.util.List<android.content.pm.ResolveInfo> resolved = pm.queryIntentActivities(launcher, 0);
        String self = ((Context) context).getPackageName();
        int added = 0;
        for (android.content.pm.ResolveInfo info : resolved) {
            if (added >= 4) break;
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(self)) continue;
            String cls = info.activityInfo.name;
            android.content.pm.ShortcutInfo shortcut =
                    new android.content.pm.ShortcutInfo.Builder(ctx, "kit-predict-" + pkg + "-" + added)
                            .setActivity(new android.content.ComponentName(pkg, cls))
                            .setShortLabel(info.activityInfo.loadLabel(pm))
                            .setLongLabel(info.activityInfo.loadLabel(pm))
                            .build();
            Object id = idClass.getConstructor(String.class).newInstance(pkg + "/" + cls);
            Object builder = builderClass.getConstructor(idClass, shortcutClass)
                    .newInstance(id, shortcut);
            callSetter(builder, "setClassName", cls);
            Object target = builderClass.getMethod("build").invoke(builder);
            if (appTargetClass.isInstance(target)) {
                targets.add(target);
                added++;
            }
        }
        return targets;
    }

    /** Calls the first single-argument method with this name, if the build has one. */
    private static void callSetter(Object builder, String name, Object value) {
        for (Method m : builder.getClass().getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 1
                    && m.getParameterTypes()[0].isInstance(value)) {
                try {
                    m.setAccessible(true);
                    m.invoke(builder, value);
                } catch (Throwable ignored) {
                    // setter is optional for this build
                }
                return;
            }
        }
    }

    /** PredictorState$registerPredictor$1$1 — the launcher's own prediction callback. */
    private static Object newLauncherCallback(Object state, Object model, Object factory) {
        try {
            Class<?> callbackClass = Class.forName(
                    "com.android.launcher3.model.PredictorState$registerPredictor$1$1");
            // The synthetic class carries no constructor in this build; allocate it directly.
            Object callback = unsafeAllocate(callbackClass);
            setField(callback, "this$0", state);
            setField(callback, "$model", model);
            setField(callback, "$taskFactory", factory);
            return callback;
        } catch (Throwable t) {
            Log.w(TAG, "launcher prediction callback unavailable: " + describe(t));
            return null;
        }
    }

    private static Object modelExecutor() throws Exception {
        Class<?> executors = Class.forName("com.android.launcher3.util.Executors");
        Field field = executors.getField("MODEL_EXECUTOR");
        return field.get(null);
    }

    /** lastTargets must never be null: the loader calls size() on it. */
    private static Object emptyList() {
        return java.util.Collections.emptyList();
    }

    private static Class<?> shizukuClass(String simpleName) throws ClassNotFoundException {
        for (String pkg : new String[]{"rikka.shizuku.", "moe.shizuku.api."}) {
            try {
                return Class.forName(pkg + simpleName);
            } catch (ClassNotFoundException ignored) {
                // try the next package
            }
        }
        throw new ClassNotFoundException("rikka.shizuku." + simpleName);
    }

    private static Method findMethod(Class<?> type, String name, int params) throws Exception {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == params) return m;
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name + "/" + params);
    }

    private static Object unsafeAllocate(Class<?> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Object unsafe = theUnsafe.get(null);
        return unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe, type);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                // keep walking up
            }
        }
    }

    private static Object field(Object target, String name) {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (Throwable ignored) {
                // keep walking up
            }
        }
        return null;
    }

    private static String describe(Throwable t) {
        Throwable cause = t.getCause() != null ? t.getCause() : t;
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
