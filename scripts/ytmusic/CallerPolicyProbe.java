package nl.johninnl.ytmusic.probe;

import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

/** Run against an explicitly installed separate test copy signed with the probe's key. */
public final class CallerPolicyProbe extends Instrumentation {
    private String host;
    private int checks;

    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        host = args == null ? null : args.getString("host");
        if (host == null) host = "app.seb3thehacker.gearslip";
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Context context = getTargetContext();
            ClassLoader loader = context.getClassLoader();
            String version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            boolean old = "9.30.53".equals(version);
            check(old || "9.44.52".equals(version), "Unsupported target version " + version);
            String[] types = old ? new String[]{"kyh", "awld", "awjm", "u"}
                    : new String[]{"hdv", "zph", "znr", "t"};
            Class<?> manager = loader.loadClass(types[0]);
            Class<?> caller = loader.loadClass(types[1]);
            Class<?> client = loader.loadClass(types[2]);
            Object policy = allocate(manager);
            Field ctx = manager.getDeclaredField("q");
            ctx.setAccessible(true);
            ctx.set(policy, context);
            Field browseSet = manager.getDeclaredField(types[3]);
            browseSet.setAccessible(true);
            browseSet.set(policy, Collections.emptySet());
            Object hostCaller = caller.getConstructor(String.class).newInstance(host);
            Object hostClient = client.getConstructor(String.class).newInstance(host);
            int hostUid = context.getPackageManager().getApplicationInfo(host, 0).uid;
            check(hostUid != context.getApplicationInfo().uid, "Probe needs a separately installed host");
            Method trust = manager.getMethod("g", caller, client, int.class);
            Method eligible = manager.getMethod("i", client);
            Method browsable = manager.getMethod("h", client);

            // The flag provider and certificate helpers are intentionally unset. Successful
            // execution proves the actual emitted DEX takes its host path before those gates.
            check(Boolean.TRUE.equals(trust.invoke(policy, hostCaller, hostClient, hostUid)),
                    "Installed host ownership did not bypass server-flag and AA-dependent rejection");
            check(Boolean.FALSE.equals(trust.invoke(policy, hostCaller, hostClient, context.getApplicationInfo().uid)),
                    "Spoofed host package accepted under YT Music's UID");
            check(Boolean.FALSE.equals(trust.invoke(policy, hostCaller, hostClient, -1)), "Invalid UID accepted");
            check(Boolean.TRUE.equals(eligible.invoke(policy, hostClient)), "Configured host browser eligibility missing");
            check(Boolean.TRUE.equals(browsable.invoke(policy, hostClient)), "Configured host returned an empty browser root gate");

            Object otherCaller = caller.getConstructor(String.class).newInstance("unrelated.client");
            Object otherClient = client.getConstructor(String.class).newInstance("unrelated.client");
            check(Boolean.FALSE.equals(eligible.invoke(policy, otherClient)), "Unrelated client gained eligibility");
            boolean reachedOriginal = false;
            try {
                trust.invoke(policy, otherCaller, otherClient, hostUid);
            } catch (InvocationTargetException expected) {
                reachedOriginal = expected.getCause() instanceof NullPointerException;
            }
            check(reachedOriginal, "Unrelated caller did not retain original flag-dependent policy");

            result.putString("result", "PASS " + checks + " actual-DEX caller policy checks; host=" + host + "; UID=" + hostUid);
            result.putString("scope", "Reflective policy execution with real PackageManager ownership. Live Binder browser sessions, content and playback require separate validation.");
            finish(-1, result);
        } catch (Throwable failure) {
            result.putString("result", "FAIL after " + checks + " checks: " + android.util.Log.getStackTraceString(failure));
            finish(1, result);
        }
    }

    private static Object allocate(Class<?> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type);
    }

    private void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }
}
