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
 * Search sessions are created inside {@code SearchSession}'s constructor, which calls the
 * {@code search_ui} system service. That service requires MANAGE_SEARCH_UI, a
 * signature|role permission held only by shell (uid 2000), so a sideloaded launcher is
 * refused and all in-drawer search is dead.
 *
 * <p>The framework constructor does exactly this (verified with dexdump on the emulator's
 * framework.jar):
 *
 * <pre>
 * mCloseGuard = CloseGuard.get();
 * mIsClosed = new AtomicBoolean(false);
 * mToken = new Binder();
 * mRegisteredCallbacks = new ArrayMap&lt;&gt;();
 * mInterface = ISearchUiManager.Stub.asInterface(ServiceManager.getService("search_ui"));
 * mSessionId = new SearchSessionId(pkg + ":" + UUID.randomUUID(), context.getUserId());
 * searchContext.setPackageName(pkg);
 * mInterface.createSearchSession(searchContext, mSessionId, mToken);
 * </pre>
 *
 * <p>This class replays that sequence, but binds {@code mInterface} to a binder wrapped by
 * Shevery/Shizuku so the call executes with shell's identity. Without a running privileged
 * bridge the call degrades to {@code null}: the drawer keeps working and only search is
 * unavailable.
 */
public final class SearchUiBridge {

    private static final String TAG = "KitSearch";

    private SearchUiBridge() {
    }

    /** Replaces SearchUiManager.createSearchSession(SearchContext): SearchSession. */
    public static Object createSession(Object manager, Object searchContext) {
        try {
            Method direct = manager.getClass().getMethod("createSearchSession", searchContext.getClass());
            Object session = direct.invoke(manager, searchContext);
            Log.i(TAG, "createSearchSession succeeded directly");
            return session;
        } catch (Throwable direct) {
            Throwable cause = direct.getCause() != null ? direct.getCause() : direct;
            Log.w(TAG, "direct createSearchSession refused: " + cause);
            return viaShell(manager, searchContext);
        }
    }

    private static Object viaShell(Object manager, Object searchContext) {
        try {
            Class<?> shizuku = shizukuClass("Shizuku");
            // Shizuku only delivers a binder to apps that asked for permission, so ask once.
            // The prompt is shown by the Shizuku/Shevery app; the call then succeeds on the
            // next attempt (the bridge is reached again on the next launcher start).
            boolean granted = true;
            try {
                int value = (Integer) shizuku.getMethod("checkSelfPermission").invoke(null);
                Log.i(TAG, "checkSelfPermission returned " + value);
                granted = value == 0;
            } catch (Throwable noBinder) {
                // checkSelfPermission itself needs the binder, which only arrives after the
                // request below, so treat a failure as "not granted" and ask.
                Log.i(TAG, "no binder yet (" + noBinder.getMessage() + "), requesting permission");
                granted = false;
            }
            if (!granted) {
                try {
                    shizuku.getMethod("requestPermission", int.class).invoke(null, 0);
                    Log.i(TAG, "requested Shevery/Shizuku permission for search sessions");
                } catch (Throwable request) {
                    Log.w(TAG, "could not request permission: " + request);
                }
                return null;
            }
            Object ping = shizuku.getMethod("pingBinder").invoke(null);
            Log.i(TAG, "pingBinder=" + ping + " checkSelfPermission=" + granted);
            if (!(ping instanceof Boolean) || !((Boolean) ping)) {
                Log.w(TAG, "Shevery/Shizuku is not running; in-drawer search stays unavailable");
                return null;
            }
            Object selfUid = shizuku.getMethod("getUid").invoke(null);
            Log.i(TAG, "Shevery/Shizuku reachable, server uid=" + selfUid);

            Context context = field(manager, "mContext", Context.class);
            if (context == null) {
                Log.w(TAG, "SearchUiManager.mContext unavailable; cannot build a session");
                return null;
            }

            String serviceName = "search_ui";
            IBinder raw = (IBinder) Class.forName("android.os.ServiceManager")
                    .getMethod("getService", String.class).invoke(null, serviceName);
            if (raw == null) {
                Log.w(TAG, "search_ui service is not published");
                return null;
            }
            Class<?> wrapperClass = shizukuClass("ShizukuBinderWrapper");
            IBinder wrapped = (IBinder) wrapperClass.getConstructor(IBinder.class).newInstance(raw);

            Class<?> stubClass = Class.forName("android.app.search.ISearchUiManager$Stub");
            Object uiManager = stubClass.getMethod("asInterface", IBinder.class).invoke(null, wrapped);

            String ownPackage = context.getPackageName();
            String pkg = ownPackage;
            int userId = (Integer) Context.class.getMethod("getUserId").invoke(context);
            Class<?> idClass = Class.forName("android.app.search.SearchSessionId");
            Object sessionId = idClass.getDeclaredConstructor(String.class, int.class)
                    .newInstance(pkg + ":" + UUID.randomUUID(), userId);

            // setPackageName is a hidden framework method; find it by name and signature.
            java.lang.reflect.Method setPackageName = null;
            for (java.lang.reflect.Method candidate : searchContext.getClass().getDeclaredMethods()) {
                if (candidate.getName().equals("setPackageName") && candidate.getParameterCount() == 1) {
                    setPackageName = candidate;
                    break;
                }
            }
            if (setPackageName != null) {
                setPackageName.setAccessible(true);
                setPackageName.invoke(searchContext, pkg);
            } else {
                Log.w(TAG, "SearchContext.setPackageName not found; continuing without it");
            }

            Class<?> sessionClass = Class.forName("android.app.search.SearchSession");
            Object session = unsafeAllocate(sessionClass);
            setField(session, "mCloseGuard", Class.forName("dalvik.system.CloseGuard")
                    .getMethod("get").invoke(null));
            setField(session, "mIsClosed", new AtomicBoolean(false));
            IBinder token = (IBinder) Class.forName("android.os.Binder")
                    .getDeclaredConstructor().newInstance();
            setField(session, "mToken", token);
            setField(session, "mRegisteredCallbacks", createArrayMap());
            setField(session, "mInterface", uiManager);
            setField(session, "mSessionId", sessionId);

            Method create = Class.forName("android.app.search.ISearchUiManager")
                    .getMethod("createSearchSession",
                            Class.forName("android.app.search.SearchContext"), idClass, IBinder.class);

            // Package-scoped experiment: does the service care which package the SearchContext
            // names, given the caller is shell? Try the launcher's own package, then shell's.
            for (String candidate : new String[]{ownPackage, "com.android.shell"}) {
                try {
                    searchContext.getClass().getDeclaredMethod("setPackageName", String.class)
                            .invoke(searchContext, candidate);
                } catch (Throwable ignored) {
                    // the hidden setter may be absent; the id below still carries the package
                }
                Object candidateId = idClass.getDeclaredConstructor(String.class, int.class)
                        .newInstance(candidate + ":" + UUID.randomUUID(), userId);
                try {
                    create.invoke(uiManager, searchContext, candidateId, token);
                    Log.i(TAG, "createSearchSession accepted as shell with package=" + candidate);
                    setField(session, "mSessionId", candidateId);
                    Log.i(TAG, "created search session through shell uid");
                    return session;
                } catch (Throwable t) {
                    Log.w(TAG, "createSearchSession refused as shell with package=" + candidate
                            + " -> " + describe(t));
                }
            }
            return null;
        } catch (Throwable t) {
            Log.w(TAG, "shell bridge failed: " + describe(t));
            return null;
        }
    }

    /** Shizuku forks keep the API either in rikka.shizuku or in moe.shizuku.api. */
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

    private static String describe(Throwable t) {
        Throwable cause = t.getCause() != null ? t.getCause() : t;
        return cause.getClass().getName() + ": " + cause.getMessage();
    }

    private static Object createArrayMap() throws Exception {
        Class<?> arrayMap = Class.forName("android.util.ArrayMap");
        return arrayMap.getDeclaredConstructor().newInstance();
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
        throw new NoSuchFieldException(name);
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object target, String name, Class<T> type) {
        try {
            for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
                try {
                    Field field = c.getDeclaredField(name);
                    field.setAccessible(true);
                    return (T) field.get(target);
                } catch (NoSuchFieldException ignored) {
                    // keep walking up
                }
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return null;
    }
}
