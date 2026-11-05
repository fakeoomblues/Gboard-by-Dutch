package nl.johninnl.gboard;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.k2fsa.sherpa.onnx.FeatureConfig;
import com.k2fsa.sherpa.onnx.LibraryLoader;
import com.k2fsa.sherpa.onnx.OfflineModelConfig;
import com.k2fsa.sherpa.onnx.OfflineRecognizer;
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig;
import com.k2fsa.sherpa.onnx.OfflineStream;
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig;
import com.k2fsa.sherpa.onnx.SileroVadModelConfig;
import com.k2fsa.sherpa.onnx.Vad;
import com.k2fsa.sherpa.onnx.VadModelConfig;

import java.io.File;

/**
 * Holds one loaded Parakeet recognizer. Loading costs about a second and several hundred
 * MB, so it is kept between dictations and released after a few idle minutes.
 */
final class ParakeetEngine {
    private static final String TAG = "MorpheParakeet";
    static final int SAMPLE_RATE = 16_000;
    private static final long IDLE_RELEASE_MS = 5 * 60_000L;

    private static final Object LOCK = new Object();
    private static OfflineRecognizer recognizer;
    private static String loadedModel;
    private static volatile boolean libraryLoaded;
    private static Handler releaser;
    private static final Runnable RELEASE = ParakeetEngine::release;

    private ParakeetEngine() {}

    /** Decodes one utterance of 16 kHz mono samples; loads the selected model when needed. */
    static String transcribe(Context context, float[] samples, int count) {
        synchronized (LOCK) {
            cancelRelease();
            try {
                OfflineRecognizer active = load(context);
                OfflineStream stream = active.createStream();
                try {
                    float[] audio = count == samples.length ? samples : java.util.Arrays.copyOf(samples, count);
                    stream.acceptWaveform(audio, SAMPLE_RATE);
                    active.decode(stream);
                    String text = active.getResult(stream).getText();
                    return text == null ? "" : text.trim();
                } finally {
                    stream.release();
                }
            } finally {
                scheduleRelease();
            }
        }
    }

    /** Loads the model ahead of the first utterance so speaking does not wait for it. */
    static void warm(Context context) {
        synchronized (LOCK) {
            cancelRelease();
            try {
                load(context);
            } catch (RuntimeException | UnsatisfiedLinkError e) {
                Log.w(TAG, "Preloading Parakeet failed", e);
            } finally {
                scheduleRelease();
            }
        }
    }

    private static OfflineRecognizer load(Context context) {
        ParakeetCatalog.Model model = ParakeetStore.selectedModel(context);
        if (recognizer != null && model.id.equals(loadedModel)) return recognizer;
        releaseLocked();
        ensureLibrary(context);
        File dir = ParakeetStore.modelDir(context, model);
        OfflineTransducerModelConfig transducer = OfflineTransducerModelConfig.builder()
                .setEncoder(new File(dir, ParakeetCatalog.ENCODER).getPath())
                .setDecoder(new File(dir, ParakeetCatalog.DECODER).getPath())
                .setJoiner(new File(dir, ParakeetCatalog.JOINER).getPath())
                .build();
        OfflineModelConfig modelConfig = OfflineModelConfig.builder()
                .setTransducer(transducer)
                .setTokens(new File(dir, ParakeetCatalog.TOKENS).getPath())
                .setModelType("nemo_transducer")
                .setNumThreads(threads())
                .setDebug(false)
                .build();
        OfflineRecognizerConfig config = OfflineRecognizerConfig.builder()
                .setFeatureConfig(FeatureConfig.builder().setSampleRate(SAMPLE_RATE).setFeatureDim(80).build())
                .setOfflineModelConfig(modelConfig)
                .setDecodingMethod("greedy_search")
                .build();
        long start = System.nanoTime();
        recognizer = new OfflineRecognizer(config);
        loadedModel = model.id;
        Log.i(TAG, "Loaded " + model.id + " in " + (System.nanoTime() - start) / 1_000_000 + " ms");
        return recognizer;
    }

    /** Loads the verified runtime once, instead of sherpa-onnx's own library search. */
    static void ensureLibrary(Context context) {
        synchronized (ParakeetEngine.class) {
            if (libraryLoaded) return;
            LibraryLoader.setAutoLoadEnabled(false);
            System.load(ParakeetStore.runtimeLibrary(context).getPath());
            libraryLoaded = true;
        }
    }

    /** A Silero VAD for one voice session, scoring 512-sample windows at 16 kHz. */
    static Vad createVad(Context context) {
        ensureLibrary(context);
        SileroVadModelConfig silero = SileroVadModelConfig.builder()
                .setModel(ParakeetStore.vadModel(context).getPath())
                .setWindowSize(Endpointer.WINDOW_SAMPLES)
                .build();
        return new Vad(VadModelConfig.builder()
                .setSileroVadModelConfig(silero)
                .setSampleRate(SAMPLE_RATE)
                .setNumThreads(1)
                .setDebug(false)
                .build());
    }

    /** Big-core count on typical phones; never more than four, never fewer than two. */
    static int threads() {
        int cores = Runtime.getRuntime().availableProcessors();
        return Math.max(2, Math.min(4, cores / 2));
    }

    static void release() {
        synchronized (LOCK) {
            releaseLocked();
        }
    }

    private static void releaseLocked() {
        if (recognizer != null) {
            recognizer.release();
            recognizer = null;
            loadedModel = null;
        }
    }

    private static Handler releaser() {
        if (releaser == null) {
            HandlerThread thread = new HandlerThread("MorpheParakeetIdle");
            thread.start();
            releaser = new Handler(thread.getLooper());
        }
        return releaser;
    }

    private static void cancelRelease() {
        releaser().removeCallbacks(RELEASE);
    }

    private static void scheduleRelease() {
        releaser().postDelayed(RELEASE, IDLE_RELEASE_MS);
    }
}
