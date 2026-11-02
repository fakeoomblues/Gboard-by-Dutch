package nl.johninnl.warningprobe;

import android.app.Instrumentation;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;

/** Test-only original Maps media lifecycle exercise. No app-wide manager is changed. */
public final class MapsVoiceProbe {
    private static final String ARTIFACT =
            "81506a2c54fc9a8faca34c18af7619d9c9d26cbfbe799b66884a59ac17791004";

    public static JSONObject run(Instrumentation probe, Bundle args) throws Exception {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Run this probe on the instrumentation worker thread");
        }
        if (args != null && "manager".equals(args.getString("scenario"))) {
            return runManager(probe);
        }
        JSONObject result = new JSONObject();
        result.put("probe", "MapsVoiceProbe");
        result.put("expected_original_artifact_sha256", ARTIFACT);
        result.put("target_package", probe.getTargetContext().getPackageName());
        result.put("scope", "Original bmsk methods with probe-owned MediaPlayer and synthetic local PCM WAV");
        JSONArray cases = new JSONArray();
        for (String name : new String[]{"natural_completion", "cancel_active", "cancel_before_play"}) {
            cases.put(runCase(probe, name));
        }
        result.put("cases", cases);
        result.put("limitations", new JSONArray(new String[]{
                "Instrumentation uses a re-signed copy; artifact equality must be checked by the runner.",
                "MediaPlayer start success, isPlaying and advancing position are API evidence, not audible output measurement.",
                "The native bmua callback is observed through a proxy; bmth manager queue/focus handling is not exercised.",
                "No live bmrc/bmtl manager is obtained: their app dependencies and active navigation lifecycle are not constructed or replaced.",
                "No proof of pending-job cancellation, dispatch fencing, guidance hold, chained players, mute modes, car routing, focus denial or current-maneuver restoration.",
                "Native constructor may enable LoudnessEnhancer for this player; no stream volume or persisted app preference is changed."
        }));
        return result;
    }

    /** Exercise the real application's manager with synthetic, explicitly non-route test audio. */
    private static JSONObject runManager(Instrumentation probe) throws Exception {
        JSONObject result = new JSONObject().put("probe", "MapsVoiceProbe").put("scenario", "manager")
                .put("expected_original_artifact_sha256", ARTIFACT)
                .put("scope", "Real app bmrc/bmtl cancellation with synthetic TEST_AUDIO jobs; no route simulation");
        Trace trace = new Trace();
        Object[] manager = new Object[1];
        List<ManagerItem> items = new ArrayList<>();
        ClassLoader loader = probe.getTargetContext().getClassLoader();
        Method[] cancel = new Method[1];
        try {
            onMain(probe, () -> {
                Object application = probe.getTargetContext().getApplicationContext();
                Method componentMethod = inheritedMethod(application.getClass(), "rJ");
                Object component = componentMethod.invoke(application);
                result.put("component_class", component.getClass().getName());
                Object provider = field(component.getClass(), "hU").get(component);
                Object cached = field(provider.getClass(), "c").get(provider);
                boolean existed = cached != null && "bmrc".equals(cached.getClass().getName());
                result.put("manager_previously_cached", existed);
                trace.add("manager_provider_resolve", existed ? "cached" : "lazy_initialization_requested");
                manager[0] = inheritedMethod(provider.getClass(), "a").invoke(provider);
                if (!"bmrc".equals(manager[0].getClass().getName())) {
                    throw new IllegalStateException("Expected original bmrc, got " + manager[0].getClass().getName());
                }
                result.put("manager_created_by_provider", !existed);
                result.put("manager_identity", identity(manager[0]));
            });
            JSONObject initial = managerSnapshot(manager[0], trace, "initial");
            result.put("initial_manager_state", initial);
            if (!initial.getBoolean("idle")) throw new IllegalStateException("Native manager not idle; no test jobs submitted");
            Class<?> jobClass = Class.forName("bmud", true, loader);
            cancel[0] = method(manager[0].getClass(), "l", jobClass);
            ManagerItem active = createManagerItem(probe, loader, "active", 10000);
            items.add(active);
            submitManagerItem(probe, manager[0], active, loader, trace);
            if (!active.started.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Active native job never reached real MediaPlayer.start; terminal=" + active.lastTerminal);
            }
            SystemClock.sleep(150);
            onMain(probe, () -> {
                result.put("active_playing_before_cancel", active.player.isPlaying());
                result.put("active_position_ms_before_cancel", active.player.getCurrentPosition());
            });
            JSONObject playing = managerSnapshot(manager[0], trace, "active_playing");
            result.put("playing_manager_state", playing);
            if (!identity(active.job).equals(playing.getString("active"))) {
                throw new IllegalStateException("Submitted active job is not native manager p");
            }
            ManagerItem pending = createManagerItem(probe, loader, "pending", 10000);
            items.add(pending);
            submitManagerItem(probe, manager[0], pending, loader, trace);
            JSONObject queued = managerSnapshot(manager[0], trace, "pending_queued");
            result.put("queued_manager_state", queued);
            if (!identity(active.job).equals(queued.getString("active"))
                    || !identity(pending.job).equals(queued.getString("pending"))) {
                throw new IllegalStateException("Expected original native p/q identity pair not observed");
            }
            onMain(probe, () -> {
                trace.add("cancel_pending_call", identity(pending.job));
                cancel[0].invoke(manager[0], pending.job);
                trace.add("cancel_pending_return");
            });
            result.put("after_pending_cancel_serialized", managerSnapshot(manager[0], trace, "after_pending_cancel"));
            if (!pending.terminal.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Pending native terminal timeout");
            onMain(probe, () -> {
                trace.add("cancel_active_call", identity(active.job));
                cancel[0].invoke(manager[0], active.job);
                trace.add("cancel_active_return");
            });
            result.put("after_active_cancel_serialized", managerSnapshot(manager[0], trace, "after_active_cancel"));
            if (!active.terminal.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Active native terminal timeout");
            // Native bmth posts further manager work; sample independently after its terminal acknowledgement.
            JSONObject terminalState = managerSnapshot(manager[0], trace, "after_terminal_callbacks");
            result.put("terminal_manager_state", terminalState);
            onMain(probe, () -> {
                result.put("active_media", active.snapshot());
                result.put("pending_media", pending.snapshot());
            });
            boolean passed = "CANCELLED".equals(active.lastTerminal)
                    && "CANCELLED_NEVER_PLAYED".equals(pending.lastTerminal)
                    && active.terminalCount == 1 && pending.terminalCount == 1
                    && active.player.starts == 1 && active.player.stops == 1 && active.player.releases == 1
                    && pending.player.starts == 0 && terminalState.optBoolean("idle")
                    && result.getJSONObject("active_media").optBoolean("native_player_cleared")
                    && result.getJSONObject("active_media").optBoolean("native_job_cancel_flag")
                    && result.optBoolean("active_playing_before_cancel")
                    && result.optInt("active_position_ms_before_cancel") > 0
                    && active.trace.before("native_manager_start_callback", "media_start_enter")
                    && active.trace.before("media_release_return", "native_manager_terminal_callback");
            result.put("status", passed ? "observed" : "inconclusive");
        } catch (Throwable failure) {
            result.put("status", "blocked").put("exception", describe(failure));
            trace.add("manager_probe_exception", describe(failure));
        } finally {
            // Cancel only retained probe identities. Never call native cancel-all on the live manager.
            for (ManagerItem item : items) {
                if (manager[0] != null && cancel[0] != null && item.job != null && item.terminalCount == 0) {
                    try {
                        onMain(probe, () -> cancel[0].invoke(manager[0], item.job));
                        item.terminal.await(2, TimeUnit.SECONDS);
                    } catch (Throwable failure) { trace.add("cleanup_cancel_exception", describe(failure)); }
                }
                try {
                    onMain(probe, () -> {
                        if (item.player != null && item.player.releases == 0) {
                            item.trace.add("probe_cleanup_release", "Pending cancellation does not release supplied media in bmrc.z");
                            item.player.release();
                        }
                    });
                } catch (Throwable failure) { trace.add("cleanup_release_exception", describe(failure)); }
                item.deleted = item.wav.delete();
            }
            JSONArray observations = new JSONArray();
            for (ManagerItem item : items) observations.put(item.snapshot());
            result.put("jobs", observations).put("events", trace.json());
            result.put("limitations", new JSONArray(new String[]{
                    "Original app DI, manager, jobs, focus path and native callbacks are used; prompts are synthetic TEST_AUDIO, not navigation guidance.",
                    "Provider lazy creation may register original native listeners and a locale receiver. No preference or stream volume is changed.",
                    "TEST_AUDIO has its own native eligibility/focus policy; success does not prove GUIDANCE eligibility or route lifecycle behavior.",
                    "B and p/q snapshots are serialized queue observations, not proof that every asynchronous native task is quiescent.",
                    "No generation hold, final dispatch fence, automatic chained-player path, car routing or current-route regeneration is tested.",
                    "Pending native cancellation reports its terminal result without releasing supplied prepared media; remaining probe-owned media is explicitly released during cleanup.",
                    "A re-signed app and instrumentation are used; real start/position/release are API evidence rather than audible output measurement."
            }));
        }
        return result;
    }

    private static ManagerItem createManagerItem(Instrumentation probe, ClassLoader loader,
            String name, int durationMs) throws Exception {
        ManagerItem item = new ManagerItem(name, File.createTempFile("maps-manager-probe-", ".wav",
                probe.getTargetContext().getCacheDir()));
        writeWav(item.wav, durationMs);
        try {
            onMain(probe, () -> {
                item.player = new ObservedPlayer(item.trace, item.started);
                item.player.setDataSource(item.wav.getAbsolutePath());
                Class<?> alertClass = Class.forName("bmsk", true, loader);
                Class<?> loudnessClass = Class.forName("bmur", true, loader);
                Class<?> typeClass = Class.forName("bmub", true, loader);
                Constructor<?> constructor = alertClass.getDeclaredConstructor(MediaPlayer.class,
                        loudnessClass, Executor.class, typeClass);
                constructor.setAccessible(true);
                Handler handler = new Handler(Looper.getMainLooper());
                Executor callbackExecutor = command -> {
                    if (!handler.post(command)) throw new IllegalStateException("Native terminal callback rejected");
                };
                item.alert = constructor.newInstance(item.player, enumNamed(loudnessClass, "NORMAL"),
                        callbackExecutor, enumNamed(typeClass, "LOCAL"));
                item.trace.add("native_prepare_call");
                if (!(Boolean) method(alertClass, "f").invoke(item.alert)) {
                    throw new IllegalStateException("Native prepare failed for " + name);
                }
                item.trace.add("native_prepare_return");
            });
        } catch (Exception failure) {
            onMain(probe, () -> { if (item.player != null) item.player.release(); });
            item.wav.delete();
            throw failure;
        }
        return item;
    }

    private static void submitManagerItem(Instrumentation probe, Object manager, ManagerItem item,
            ClassLoader loader, Trace trace) throws Exception {
        Class<?> eventClass = Class.forName("bmvk", true, loader);
        Class<?> categoryClass = Class.forName("bmuk", true, loader);
        Class<?> mediaClass = Class.forName("bmuc", true, loader);
        Class<?> listenerClass = Class.forName("bmug", true, loader);
        Class<?> eventType = Class.forName("bmvj", true, loader);
        Constructor<?> eventConstructor = eventClass.getDeclaredConstructor(eventType,
                Class.forName("ybf", true, loader), String.class, String.class,
                Class.forName("xzs", true, loader), Class.forName("cmrs", true, loader), int.class,
                Class.forName("cbgf", true, loader), Class.forName("ciux", true, loader),
                Class.forName("bjil", true, loader), boolean.class);
        eventConstructor.setAccessible(true);
        Object event = eventConstructor.newInstance(enumNamed(eventType, "TEST_NAVIGATION_VOICE"),
                null, "Maps native manager probe " + item.name, null, null, null, -1,
                null, null, null, false);
        Object callback = Proxy.newProxyInstance(loader, new Class<?>[]{listenerClass}, (proxy, called, values) -> {
            if (called.getDeclaringClass() == Object.class) {
                if (called.getName().equals("toString")) return "MapsManagerProbeCallback_" + item.name;
                if (called.getName().equals("hashCode")) return System.identityHashCode(proxy);
                if (called.getName().equals("equals")) return proxy == values[0];
            }
            if (called.getName().equals("b")) {
                item.trace.add("native_manager_start_callback", "durationMs=" + values[0]
                        + ",type=" + ((Enum<?>) values[1]).name());
            } else if (called.getName().equals("a")) {
                item.lastTerminal = ((Enum<?>) values[0]).name();
                item.terminalType = ((Enum<?>) values[1]).name();
                item.terminalCount++;
                item.trace.add("native_manager_terminal_callback", item.lastTerminal + ",type=" + item.terminalType);
                item.terminal.countDown();
            } else throw new UnsupportedOperationException(called.toString());
            return null;
        });
        Object[] future = new Object[1];
        onMain(probe, () -> {
            trace.add("native_manager_submit", item.name);
            future[0] = method(manager.getClass(), "k", eventClass, categoryClass, mediaClass, listenerClass)
                    .invoke(manager, event, enumNamed(categoryClass, "TEST_AUDIO"), item.alert, callback);
        });
        item.job = ((Future<?>) future[0]).get(5, TimeUnit.SECONDS);
        trace.add("native_manager_submit_resolved", item.name + ":" + identity(item.job));
        if (item.job == null || !"bmtl".equals(item.job.getClass().getName())) {
            throw new IllegalStateException("Native submission did not return bmtl: " + identity(item.job)
                    + "; native terminal=" + item.lastTerminal);
        }
    }

    private static JSONObject managerSnapshot(Object manager, Trace trace, String phase) throws Exception {
        AtomicReference<JSONObject> state = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch finished = new CountDownLatch(1);
        Executor executor = (Executor) field(manager.getClass(), "j").get(manager);
        executor.execute(() -> {
            try {
                JSONObject snapshot = new JSONObject().put("phase", phase)
                        .put("idle", method(manager.getClass(), "B").invoke(manager))
                        .put("active", identity(field(manager.getClass(), "p").get(manager)))
                        .put("pending", identity(field(manager.getClass(), "q").get(manager)));
                state.set(snapshot);
                trace.add("manager_state", snapshot.toString());
            } catch (Throwable caught) { failure.set(caught); }
            finally { finished.countDown(); }
        });
        if (!finished.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Manager serialized snapshot timeout: " + phase);
        if (failure.get() != null) throw new Exception("Manager snapshot failed", failure.get());
        return state.get();
    }

    private static final class ManagerItem {
        final String name;
        final File wav;
        final Trace trace = new Trace();
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch terminal = new CountDownLatch(1);
        volatile int terminalCount;
        volatile String lastTerminal = "";
        volatile String terminalType = "";
        ObservedPlayer player;
        Object alert;
        Object job;
        boolean deleted;
        ManagerItem(String name, File wav) { this.name = name; this.wav = wav; }
        JSONObject snapshot() throws Exception {
            JSONObject snapshot = new JSONObject().put("name", name).put("job_identity", identity(job))
                    .put("terminal_count", terminalCount).put("terminal_result", lastTerminal)
                    .put("terminal_type", terminalType).put("starts", player == null ? 0 : player.starts)
                    .put("stops", player == null ? 0 : player.stops)
                    .put("releases", player == null ? 0 : player.releases)
                    .put("wav_deleted", deleted).put("events", trace.json());
            if (alert != null) snapshot.put("native_player_cleared", field(alert.getClass(), "a").get(alert) == null);
            if (job != null && "bmtl".equals(job.getClass().getName())) {
                snapshot.put("native_job_cancel_flag",
                        ((java.util.concurrent.atomic.AtomicBoolean) field(job.getClass(), "s").get(job)).get());
            }
            return snapshot;
        }
    }

    private static String identity(Object value) {
        return value == null ? "null" : value.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(value));
    }

    private static Field field(Class<?> type, String name) throws Exception {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field found = current.getDeclaredField(name);
                found.setAccessible(true);
                return found;
            } catch (NoSuchFieldException missing) { /* Continue through original app inheritance. */ }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    private static Method inheritedMethod(Class<?> type, String name) throws Exception {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try { return method(current, name); }
            catch (NoSuchMethodException missing) { /* Continue through original app inheritance. */ }
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }

    private static JSONObject runCase(Instrumentation probe, String name) throws Exception {
        Trace trace = new Trace();
        JSONObject result = new JSONObject().put("case", name);
        File wav = File.createTempFile("maps-warning-probe-", ".wav", probe.getTargetContext().getCacheDir());
        ObservedPlayer[] player = new ObservedPlayer[1];
        Object[] alert = new Object[1];
        ClassLoader loader = probe.getTargetContext().getClassLoader();
        AtomicReference<Throwable> asynchronousFailure = new AtomicReference<>();
        CountDownLatch terminal = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        try {
            writeWav(wav, name.equals("natural_completion") ? 700 : 4000);
            Class<?> alertClass = Class.forName("bmsk", true, loader);
            Class<?> loudnessClass = Class.forName("bmur", true, loader);
            Class<?> typeClass = Class.forName("bmub", true, loader);
            Class<?> callbackClass = Class.forName("bmua", true, loader);
            Object normal = enumNamed(loudnessClass, "NORMAL");
            Object local = enumNamed(typeClass, "LOCAL");
            result.put("loudness_enum", normal.toString()).put("media_type_enum", local.toString());
            Method prepare = method(alertClass, "f");
            Method play = method(alertClass, "d", callbackClass);
            Method stop = method(alertClass, "l");
            Method register = method(alertClass, "b", callbackClass);
            Field nativePlayer = alertClass.getDeclaredField("a");
            nativePlayer.setAccessible(true);
            Handler main = new Handler(Looper.getMainLooper());
            Executor executor = command -> {
                trace.add("native_executor_enqueue");
                if (!main.post(() -> {
                    try {
                        trace.add("native_executor_run");
                        command.run();
                    } catch (Throwable failure) {
                        asynchronousFailure.compareAndSet(null, failure);
                        trace.add("native_executor_exception", failure.toString());
                        terminal.countDown();
                    }
                })) throw new IllegalStateException("Main looper rejected native callback");
            };
            Object callback = Proxy.newProxyInstance(loader, new Class<?>[]{callbackClass}, (proxy, called, values) -> {
                if (called.getDeclaringClass() == Object.class) {
                    if (called.getName().equals("toString")) return "MapsVoiceProbeCallback";
                    if (called.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (called.getName().equals("equals")) return proxy == values[0];
                }
                if (called.getName().equals("b")) {
                    trace.add("native_start_callback", "isPlaying=" + player[0].isPlaying());
                } else if (called.getName().equals("a")) {
                    trace.add("native_terminal_callback", "nativePlayerNull=" + (nativePlayer.get(alert[0]) == null));
                    terminal.countDown();
                } else {
                    throw new UnsupportedOperationException(called.toString());
                }
                return null;
            });
            Constructor<?> constructor = alertClass.getDeclaredConstructor(MediaPlayer.class,
                    loudnessClass, Executor.class, typeClass);
            constructor.setAccessible(true);
            onMain(probe, () -> {
                player[0] = new ObservedPlayer(trace, started);
                player[0].setDataSource(wav.getAbsolutePath());
                alert[0] = constructor.newInstance(player[0], normal, executor, local);
                trace.add("native_constructed");
                boolean prepared = (Boolean) prepare.invoke(alert[0]);
                trace.add("native_prepare_return", Boolean.toString(prepared));
                if (!prepared) throw new IllegalStateException("Native prepare returned false");
                if (name.equals("cancel_before_play")) {
                    register.invoke(alert[0], callback);
                    trace.add("native_cancel_call");
                    stop.invoke(alert[0]);
                    trace.add("native_cancel_return");
                } else {
                    trace.add("native_play_call");
                    play.invoke(alert[0], callback);
                    trace.add("native_play_return");
                }
            });
            if (!name.equals("cancel_before_play")) {
                if (!started.await(2, TimeUnit.SECONDS)) throw new IllegalStateException("Real MediaPlayer.start did not return");
                SystemClock.sleep(150);
                onMain(probe, () -> {
                    trace.add("playback_sample", "isPlaying=" + player[0].isPlaying()
                            + ",positionMs=" + player[0].getCurrentPosition());
                    result.put("playing_sample", player[0].isPlaying());
                    result.put("position_ms", player[0].getCurrentPosition());
                    if (name.equals("cancel_active")) {
                        trace.add("native_cancel_call");
                        stop.invoke(alert[0]);
                        trace.add("native_cancel_return");
                    }
                });
            }
            if (!terminal.await(6, TimeUnit.SECONDS)) throw new IllegalStateException("Native terminal callback timeout");
            onMain(probe, () -> {
                if (asynchronousFailure.get() != null) throw new Exception(asynchronousFailure.get());
                result.put("native_player_cleared", nativePlayer.get(alert[0]) == null);
                result.put("start_calls", player[0].starts);
                result.put("stop_calls", player[0].stops);
                result.put("release_calls", player[0].releases);
            });
            boolean expectsPlay = !name.equals("cancel_before_play");
            boolean ordering = expectsPlay
                    ? trace.before("native_start_callback", "media_start_enter")
                            && trace.before("media_start_return", "media_stop_return")
                            && trace.before("media_release_return", "native_terminal_callback")
                    : !trace.has("media_start_enter") && trace.before("media_release_return", "native_terminal_callback");
            boolean active = !expectsPlay || (result.optBoolean("playing_sample") && result.optInt("position_ms") > 0);
            boolean passed = ordering && active && result.optBoolean("native_player_cleared")
                    && player[0].stops == 1 && player[0].releases == 1
                    && player[0].starts == (expectsPlay ? 1 : 0)
                    && trace.count("native_terminal_callback") == 1;
            result.put("callback_order_observed", ordering);
            result.put("status", passed ? "observed" : "inconclusive");
        } catch (Throwable failure) {
            result.put("status", "blocked");
            result.put("exception", describe(failure));
            trace.add("case_exception", describe(failure));
        } finally {
            try {
                onMain(probe, () -> {
                    if (player[0] != null && player[0].releases == 0) {
                        trace.add("probe_cleanup_release");
                        player[0].release();
                    }
                });
            } catch (Throwable cleanupFailure) {
                result.put("cleanup_exception", describe(cleanupFailure));
            }
            result.put("wav_deleted", wav.delete());
            result.put("events", trace.json());
        }
        return result;
    }

    private static Object enumNamed(Class<?> type, String name) throws Exception {
        for (Object value : (Object[]) method(type, "values").invoke(null)) {
            if (((Enum<?>) value).name().equals(name)) return value;
        }
        throw new NoSuchFieldException(type.getName() + " enum name " + name);
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters) throws Exception {
        Method method = type.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private interface MainOperation { void run() throws Exception; }

    private static void onMain(Instrumentation probe, MainOperation operation) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        probe.runOnMainSync(() -> {
            try { operation.run(); } catch (Throwable caught) { failure.set(caught); }
        });
        if (failure.get() != null) throw new Exception("Main-thread operation failed", failure.get());
    }

    private static String describe(Throwable failure) {
        StringBuilder message = new StringBuilder();
        for (int depth = 0; failure != null && depth < 8; depth++, failure = failure.getCause()) {
            if (depth > 0) message.append(" <- ");
            message.append(failure.getClass().getName()).append(": ").append(failure.getMessage());
        }
        return message.toString();
    }

    private static final class ObservedPlayer extends MediaPlayer {
        final Trace trace;
        final CountDownLatch started;
        int starts;
        int stops;
        int releases;
        ObservedPlayer(Trace trace, CountDownLatch started) {
            this.trace = trace;
            this.started = started;
        }
        @Override public void start() {
            trace.add("media_start_enter");
            super.start();
            starts++;
            trace.add("media_start_return", "isPlaying=" + isPlaying());
            started.countDown();
        }
        @Override public void stop() {
            trace.add("media_stop_enter");
            super.stop();
            stops++;
            trace.add("media_stop_return");
        }
        @Override public void release() {
            trace.add("media_release_enter");
            super.release();
            releases++;
            trace.add("media_release_return");
        }
    }

    private static final class Trace {
        final long origin = SystemClock.elapsedRealtimeNanos();
        final List<JSONObject> events = new ArrayList<>();
        synchronized void add(String event) { add(event, ""); }
        synchronized void add(String event, String detail) {
            try {
                long at = SystemClock.elapsedRealtimeNanos();
                events.add(new JSONObject().put("event", event).put("detail", detail)
                        .put("elapsed_ns", at - origin).put("elapsed_realtime_ns", at)
                        .put("thread", Thread.currentThread().getName()));
            } catch (Exception impossible) { throw new IllegalStateException(impossible); }
        }
        synchronized int count(String name) {
            int count = 0;
            for (JSONObject event : events) if (name.equals(event.optString("event"))) count++;
            return count;
        }
        synchronized boolean has(String name) { return count(name) > 0; }
        synchronized boolean before(String first, String second) {
            int left = -1;
            for (int i = 0; i < events.size(); i++) {
                String name = events.get(i).optString("event");
                if (left < 0 && first.equals(name)) left = i;
                if (second.equals(name)) return left >= 0 && left < i;
            }
            return false;
        }
        synchronized JSONArray json() { return new JSONArray(events); }
    }

    private static void writeWav(File file, int durationMs) throws Exception {
        int rate = 16000;
        int samples = rate * durationMs / 1000;
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(new byte[]{'R', 'I', 'F', 'F'});
            little(out, 36 + samples * 2, 4);
            out.write(new byte[]{'W', 'A', 'V', 'E', 'f', 'm', 't', ' '});
            little(out, 16, 4); little(out, 1, 2); little(out, 1, 2);
            little(out, rate, 4); little(out, rate * 2, 4);
            little(out, 2, 2); little(out, 16, 2);
            out.write(new byte[]{'d', 'a', 't', 'a'});
            little(out, samples * 2, 4);
            for (int i = 0; i < samples; i++) {
                little(out, (int) (Math.sin(2 * Math.PI * 440 * i / rate) * 1400), 2);
            }
        }
    }

    private static void little(FileOutputStream out, int value, int bytes) throws Exception {
        for (int i = 0; i < bytes; i++) out.write((value >>> (8 * i)) & 255);
    }
}
