package nl.johninnl.vela;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.util.Set;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import nl.johninnl.navigation.alerts.PlaybackGrant;
import nl.johninnl.navigation.alerts.WarningAudioOwner;
import nl.johninnl.navigation.alerts.WarningEvent;

/** Uses the selected, initialized native system engine and its ORIGINAL listener/focus owner.
 * Neural speech remains unavailable until its output/drain hooks are implemented.
 * Untagged route guidance keeps its native listener path. No text or coordinates enter IDs/logs.
 */
public final class VelaWarningSpeaker {
    public interface Listener {
        void started(PlaybackGrant grant);
        void finished(PlaybackGrant grant, PlaybackGrant.Outcome outcome, boolean quiescent);
    }
    private static final Object LOCK = new Object();
    private static final Map<Object, WeakReference<VelaWarningSpeaker>> GUIDES = new WeakHashMap<>();
    private static final Map<Object, Attempt> CLOSURES = new WeakHashMap<>();
    private static final Map<String, Attempt> UTTERANCES = new HashMap<>();
    private static final ThreadLocal<Attempt> SCOPE = new ThreadLocal<>();
    private final Handler callbacks = new Handler(Looper.getMainLooper());
    private final ExecutorService tones=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"VelaWarningTone");t.setDaemon(true);return t;});
    private Object guide;
    private Attempt active;
    private final Map<String,Long> used = new HashMap<>();
    private long generation;
    private int focusResult;
    private boolean closed;

    public void bind(Object actualGuide) {
        synchronized (LOCK) {
            if (guide == actualGuide && !closed) return;
            close();
            guide = actualGuide; closed = false; generation++;
            if (guide != null) GUIDES.put(guide, new WeakReference<>(this));
        }
    }
    /** Never initialize an unrelated/default engine or alter the application's language. */
    public void prepare(String language) { /* Readiness is polled against the real selected engine. */ }
    public boolean isReady(String language) {
        synchronized (LOCK) {
            try {
                if (closed || guide == null || active != null || flag(guide, "m") || !flag(guide, "e")
                        || !flag(guide, "h") || flag(guide, "i") || integer(guide, "s") != 0
                        || !((Collection<?>) read(guide, "g")).isEmpty()) return false;
                TextToSpeech engine = (TextToSpeech) read(guide, "b");
                Locale locale = locale(language);
                return engine != null && locale != null && !engine.isSpeaking()
                        && engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE && installedVoice(engine,locale)!=null;
            } catch (ReflectiveOperationException | RuntimeException failure) { return false; }
        }
    }
    /** Tones do not initialize or depend on either speech engine. */
    public boolean isReady(String language, WarningAudioOwner.Mode mode) {
        if(mode==WarningAudioOwner.Mode.SILENT)return true;
        if(mode==WarningAudioOwner.Mode.VOICE)return isReady(language);
        synchronized(LOCK) {
            try { return !closed && guide!=null && active==null && integer(guide,"s")==0
                    && ((Collection<?>)read(guide,"g")).isEmpty() && read(guide,"p")!=null
                    && effectiveMode(mode)==mode; }
            catch(ReflectiveOperationException|RuntimeException failure){return false;}
        }
    }
    public WarningAudioOwner.Mode effectiveMode(WarningAudioOwner.Mode requested) {
        synchronized (LOCK) {
            try {
                if (guide == null || closed || requested == null) return WarningAudioOwner.Mode.SILENT;
                if (!flag(guide, "n")) return requested;
                return flag(guide, "o") && requested != WarningAudioOwner.Mode.SILENT
                        ? WarningAudioOwner.Mode.TONE : WarningAudioOwner.Mode.SILENT;
            } catch (ReflectiveOperationException | RuntimeException failure) { return WarningAudioOwner.Mode.SILENT; }
        }
    }
    public boolean speak(WarningEvent payload, PlaybackGrant grant, Listener listener) {
        return payload!=null && payload.operation==WarningEvent.Operation.ANNOUNCE
                && speak(payload,grant,payload.announceUntil,listener);
    }
    /** A revised UPSERT carries text, while the original ANNOUNCE retains playback authority/deadline. */
    public boolean speak(WarningEvent payload, PlaybackGrant grant, long originalAnnounceUntil, Listener listener) {
        synchronized (LOCK) {
            long now=SystemClock.elapsedRealtime();
            used.entrySet().removeIf(entry->entry.getValue()<=now);
            if (Looper.myLooper()!=Looper.getMainLooper() || payload == null || grant == null || listener == null || !grant.granted
                    || grant.started || grant.outcome!=PlaybackGrant.Outcome.GRANTED || payload.operation==WarningEvent.Operation.CLEAR || payload.message.isEmpty()
                    || !grant.providerSession.equals(payload.providerSession) || grant.revision!=payload.revision
                    || used.containsKey(grant.attemptId) || used.size()>=128
                    || grant.mode == WarningAudioOwner.Mode.SILENT
                    || effectiveMode(grant.mode) != grant.mode
                    || now>=originalAnnounceUntil || originalAnnounceUntil<=0 || !isReady(payload.languageTag,grant.mode)
                    || SCOPE.get() != null || UTTERANCES.size() >= 128) return false;
            Attempt attempt = new Attempt(this, payload, grant, listener, generation,originalAnnounceUntil);
            used.put(grant.attemptId,Math.max(originalAnnounceUntil,now+30000));
            active = attempt;
            if(grant.mode==WarningAudioOwner.Mode.TONE)return submitTone(attempt);
            UTTERANCES.put("vela-"+attempt.id,attempt); SCOPE.set(attempt);
            try {
                // Existing recorder/prompt callbacks, native language checks and focus acquisition survive.
                method(guide, "j", String.class, boolean.class, boolean.class).invoke(guide, payload.message, false, false);
                if (!attempt.captured) {
                    finish(attempt, PlaybackGrant.Outcome.NOT_STARTED, true);
                    return false;
                }
                return true; // Accepted is deliberately distinct from STARTED.
            } catch (ReflectiveOperationException | RuntimeException failure) {
                finish(attempt, attempt.submitted ? PlaybackGrant.Outcome.UNCERTAIN : PlaybackGrant.Outcome.FAILED,
                        !attempt.submitted);
                return attempt.submitted;
            } finally { SCOPE.remove(); }
        }
    }
    public void cancel(PlaybackGrant grant) {
        synchronized (LOCK) {
            Attempt attempt = active;
            if (attempt == null || !same(attempt.grant, grant)) return;
            attempt.cancelled = true;
            if(attempt.grant.mode==WarningAudioOwner.Mode.TONE) {
                boolean quiet=stopTone(attempt);releaseRejected(attempt);
                finish(attempt,quiet?PlaybackGrant.Outcome.CANCELLED:PlaybackGrant.Outcome.UNCERTAIN,quiet);return;
            }
            // Shared TextToSpeech.stop would destroy unrelated route prompts. Until native
            // regeneration is verified, retain the ownership fence through the actual terminal callback.
            finish(attempt, attempt.submitted ? PlaybackGrant.Outcome.UNCERTAIN : PlaybackGrant.Outcome.CANCELLED,
                    !attempt.submitted);
        }
    }
    public void close() {
        synchronized (LOCK) {
            if (active != null) cancel(active.grant);
            if (guide != null) GUIDES.remove(guide);
            closed = true; generation++;
            // Submitted attempts remain reachable from their closure/listener until terminal proof.
        }
    }

    /** Scope the native pre-dispatch language checks to this warning without changing app locale. */
    public static String language(Object nativeGuide,String nativeTag) {
        Attempt attempt=SCOPE.get();
        if(attempt==null || attempt.nativeGuide!=nativeGuide)return nativeTag;
        Locale locale=locale(attempt.event.languageTag);return locale==null?nativeTag:locale.getLanguage();
    }
    /** Constructor hook runs before the native delayed closure leaves the synchronous scope. */
    public static void capture(Object closure) {
        synchronized (LOCK) {
            Attempt attempt = SCOPE.get();
            if (attempt == null) return;
            try {
                if (((Number)read(closure,"f")).byteValue() == 0 || read(closure,"h") != attempt.owner.guide) return;
                if (CLOSURES.containsKey(closure)) return;
                if (CLOSURES.size() >= 4096) throw new IllegalStateException("Vela warning closure bound");
                attempt.engine = (TextToSpeech) read(closure,"i");
                attempt.captured = true;
                CLOSURES.put(closure,attempt);
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Vela warning closure ABI",failure); }
        }
    }
    /** True consumes ONLY a tagged branch. Untagged pn1/UI behavior executes verbatim. */
    public static boolean dispatch(Object closure) {
        synchronized (LOCK) {
            Attempt attempt = CLOSURES.get(closure);
            if (attempt == null) return false;
            if (attempt.dispatched) return true;
            attempt.dispatched = true;
            VelaWarningSpeaker owner = attempt.owner;
            PlaybackGrant.Outcome blocked = rejection(attempt);
            if (blocked != null) { releaseRejected(attempt); finish(attempt,blocked,true); return true; }
            try {
                TextToSpeech engine = attempt.engine;
                if (engine == null || engine != read(owner.guide,"b") || flag(owner.guide,"m")
                        || engine.setLanguage(locale(attempt.event.languageTag)) < TextToSpeech.LANG_AVAILABLE) {
                    releaseRejected(attempt); finish(attempt,PlaybackGrant.Outcome.NOT_STARTED,true); return true;
                }
                Voice voice=installedVoice(engine,locale(attempt.event.languageTag));
                if(voice==null || engine.setVoice(voice)==TextToSpeech.ERROR) {
                    releaseRejected(attempt); finish(attempt,PlaybackGrant.Outcome.NOT_STARTED,true); return true;
                }
                // Keep native last-language cache honest: the NEXT route prompt must restore its language.
                field(owner.guide,"j").set(owner.guide,attempt.event.languageTag);
                Bundle parameters = new Bundle();
                parameters.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,
                        Math.min(1f,((Number)read(owner.guide,"d")).floatValue()));
                int lead = engine.playSilentUtterance(300L,TextToSpeech.QUEUE_ADD,"vela-lead-"+attempt.id);
                if (lead == TextToSpeech.ERROR) {
                    releaseRejected(attempt); finish(attempt,PlaybackGrant.Outcome.FAILED,true); return true;
                }
                blocked = rejection(attempt);
                if (blocked != null) { releaseRejected(attempt); finish(attempt,blocked,true); return true; }
                // Preserve external native text verbatim; Vela's current NavLocale expansion is unrelated.
                attempt.submitted = true;
                if (engine.speak(attempt.event.message,TextToSpeech.QUEUE_ADD,parameters,"vela-"+attempt.id) == TextToSpeech.ERROR) {
                    attempt.submitted = false; releaseRejected(attempt); finish(attempt,PlaybackGrant.Outcome.FAILED,true);
                }
            } catch (ReflectiveOperationException | RuntimeException failure) {
                if (!attempt.submitted) releaseRejected(attempt);
                finish(attempt,attempt.submitted ? PlaybackGrant.Outcome.UNCERTAIN : PlaybackGrant.Outcome.FAILED,!attempt.submitted);
            }
            return true;
        }
    }
    public static void focusRequest(Object nativeGuide) {
        synchronized (LOCK) { VelaWarningSpeaker owner=owner(nativeGuide); if(owner!=null)owner.focusResult=0; }
    }
    /** Exact raw request result, before native code collapses DELAYED and GRANTED to boolean. */
    public static void focusResult(Object nativeGuide,int result) {
        synchronized (LOCK) {
            VelaWarningSpeaker owner = owner(nativeGuide);
            if (owner != null) owner.focusResult = result;
        }
    }
    /** Native stop ENTRY invalidates delayed tags before the host resets its focus count. */
    public static void nativeStop(Object nativeGuide) {
        synchronized (LOCK) {
            VelaWarningSpeaker owner = owner(nativeGuide);
            if (owner == null) return;
            owner.generation++; owner.focusResult = 0;
            if (owner.active != null) {
                Attempt attempt=owner.active; attempt.nativeStopped=true; attempt.cancelled=true;
                boolean quiet=attempt.grant.mode==WarningAudioOwner.Mode.TONE?stopTone(attempt):!attempt.submitted;
                finish(attempt,quiet?PlaybackGrant.Outcome.CANCELLED:PlaybackGrant.Outcome.UNCERTAIN,quiet);
            }
        }
    }
    /** Native route speech wins over an external tone; stop only the extension's track. */
    public static void nativeSpeech(Object nativeGuide) {
        synchronized(LOCK) {
            VelaWarningSpeaker owner=owner(nativeGuide);
            if(owner!=null && owner.active!=null && owner.active.grant.mode==WarningAudioOwner.Mode.TONE)
                owner.cancel(owner.active.grant);
        }
    }
    public static void focusChanged(Object nativeListener,int change) {
        if (change != AudioManager.AUDIOFOCUS_LOSS && change != AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) return;
        try { nativeStop(read(nativeListener,"a")); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Vela focus listener ABI",failure); }
    }
    public static void utteranceStart(Object listener,String id) { utterance(listener,id,0); }
    public static boolean utteranceDone(Object listener,String id) { return utterance(listener,id,1); }
    public static boolean utteranceError(Object listener,String id) { return utterance(listener,id,2); }
    public static boolean utteranceStopped(Object listener,String id) { return utterance(listener,id,3); }
    /** Observe inside the original listener. Tagged terminals balance its original f() once;
     * after native stop reset the count, a late tagged callback must not decrement a new route.
     * Untagged callbacks execute the complete original method.
     */
    public static boolean utterance(Object nativeListener,String id,int event) {
        if (id == null || !id.startsWith("vela-warning-")) return false;
        synchronized (LOCK) {
            Attempt attempt = UTTERANCES.get(id);
            // Native route IDs are vela-<integer>. Unknown/duplicate external IDs are inert.
            if (attempt == null) return true;
            try { if (read(nativeListener,"a") != attempt.nativeGuide) return true; }
            catch (ReflectiveOperationException failure) { return true; }
            if (event == 0) {
                if (!attempt.started && !attempt.quiescent) {
                    if (rejection(attempt) != null) {
                        attempt.cancelled=true; finish(attempt,PlaybackGrant.Outcome.UNCERTAIN,false);
                    } else {
                        attempt.started=true;
                        attempt.owner.callbacks.post(() -> attempt.listener.started(attempt.grant));
                    }
                }
            } else {
                releaseRejected(attempt);
                PlaybackGrant.Outcome outcome = attempt.cancelled ? PlaybackGrant.Outcome.CANCELLED
                        : event==1 ? PlaybackGrant.Outcome.COMPLETED
                        : event==3 ? PlaybackGrant.Outcome.INTERRUPTED : PlaybackGrant.Outcome.FAILED;
                finish(attempt,outcome,true);
            }
            return true;
        }
    }
    private static PlaybackGrant.Outcome rejection(Attempt a) {
        if (a.cancelled || a.owner.closed || a.generation != a.owner.generation) return PlaybackGrant.Outcome.CANCELLED;
        if (SystemClock.elapsedRealtime()>=a.announceUntil) return PlaybackGrant.Outcome.EXPIRED;
        if (a.owner.effectiveMode(a.grant.mode) != a.grant.mode) return PlaybackGrant.Outcome.USER_MUTED;
        if (a.owner.focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return PlaybackGrant.Outcome.NOT_STARTED;
        return null;
    }
    private static void releaseRejected(Attempt attempt) {
        if (attempt.released || attempt.nativeStopped || attempt.generation != attempt.owner.generation) return;
        attempt.released=true;
        try { method(attempt.nativeGuide,"f").invoke(attempt.nativeGuide); }
        catch (ReflectiveOperationException | RuntimeException failure) { attempt.releaseFailed=true; }
    }
    private static void finish(Attempt attempt,PlaybackGrant.Outcome outcome,boolean quiescent) {
        if (attempt.terminal && (!quiescent || attempt.quiescent)) return;
        attempt.terminal=true; attempt.quiescent=quiescent;
        if(quiescent && attempt.releaseFailed)outcome=PlaybackGrant.Outcome.FAILED;
        final PlaybackGrant.Outcome reported=outcome;
        if (quiescent) {
            UTTERANCES.remove("vela-"+attempt.id);
            if (attempt.owner.active==attempt) attempt.owner.active=null;
        }
        attempt.owner.callbacks.post(() -> attempt.listener.finished(attempt.grant,reported,quiescent));
    }
    private static VelaWarningSpeaker owner(Object guide) {
        WeakReference<VelaWarningSpeaker> ref=GUIDES.get(guide); return ref==null?null:ref.get();
    }
    private static Locale locale(String tag) {
        if (tag==null || tag.isEmpty()) return null;
        try { Locale locale=new Locale.Builder().setLanguageTag(tag).build(); return locale.getLanguage().isEmpty()?null:locale; }
        catch (RuntimeException failure) { return null; }
    }
    private boolean submitTone(Attempt attempt) {
        try {
            // Native acquisition/result hook owns focus; the adapter never creates a competing request.
            method(guide,"b").invoke(guide);attempt.captured=true;
            PlaybackGrant.Outcome blocked=rejection(attempt);
            if(blocked!=null){releaseRejected(attempt);finish(attempt,blocked,true);return false;}
            tones.execute(()->runTone(attempt));return true;
        } catch(ReflectiveOperationException|RuntimeException failure) {
            releaseRejected(attempt);finish(attempt,PlaybackGrant.Outcome.FAILED,true);return false;
        }
    }
    /** Same 22,050 Hz PCM envelope, 30 ms gap and notes used by the existing native alerts-only chime. */
    static short[] tonePcm() {
        int frames=22050*110/1000,gap=661;short[] pcm=new short[frames*2+gap];
        for(int note=0;note<2;note++)for(int i=0;i<frames;i++)
            pcm[note*(frames+gap)+i]=(short)(Math.sin(2*Math.PI*1046.5*i/22050d)*9500d
                    *Math.min(1d,Math.min(i/264.6d,(frames-1-i)/264.6d)));
        return pcm;
    }
    private static void runTone(Attempt attempt) {
        AudioTrack track=null;PlaybackGrant.Outcome outcome=PlaybackGrant.Outcome.FAILED;
        try {
            short[] pcm=tonePcm();
            track=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(new AudioFormat.Builder().setSampleRate(22050).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.length*2).build();
            synchronized(LOCK) {
                PlaybackGrant.Outcome blocked=rejection(attempt);
                if(blocked!=null){outcome=blocked;return;}
                attempt.toneTrack=track;
                if(track.getState()!=AudioTrack.STATE_INITIALIZED || track.write(pcm,0,pcm.length)!=pcm.length)return;
                blocked=rejection(attempt);if(blocked!=null){outcome=blocked;return;}
                attempt.submitted=true;track.play();
            }
            long timeout=SystemClock.elapsedRealtime()+1000,wallDeadline=System.nanoTime()+1000000000L;
            while(SystemClock.elapsedRealtime()<timeout && System.nanoTime()<wallDeadline) {
                synchronized(LOCK) {
                    PlaybackGrant.Outcome blocked=rejection(attempt);if(blocked!=null){outcome=blocked;return;}
                    long played=Integer.toUnsignedLong(track.getPlaybackHeadPosition());
                    if(!attempt.started && played>64 && track.getPlayState()==AudioTrack.PLAYSTATE_PLAYING) {
                        attempt.started=true;attempt.owner.callbacks.post(()->attempt.listener.started(attempt.grant));
                    }
                    if(played>=pcm.length){outcome=PlaybackGrant.Outcome.COMPLETED;return;}
                }
                Thread.sleep(10);
            }
        } catch(InterruptedException interrupted){Thread.currentThread().interrupt();outcome=PlaybackGrant.Outcome.INTERRUPTED;}
        catch(RuntimeException failure){outcome=PlaybackGrant.Outcome.FAILED;}
        finally {
            synchronized(LOCK) {
                boolean published=attempt.toneTrack==track;
                boolean quiet=stopTone(attempt);
                // A track constructed after a pending cancellation was never published or played.
                if(track!=null && !published && !attempt.toneReleased)try{track.release();attempt.toneReleased=true;}catch(RuntimeException ignored){}
                releaseRejected(attempt);
                finish(attempt,quiet?outcome:PlaybackGrant.Outcome.UNCERTAIN,quiet);
            }
        }
    }
    private static boolean stopTone(Attempt attempt) {
        AudioTrack track=attempt.toneTrack;
        if(track==null)return attempt.toneReleased || !attempt.submitted || attempt.quiescent;
        try {
            if(track.getPlayState()!=AudioTrack.PLAYSTATE_STOPPED)track.stop();
            track.release();attempt.toneReleased=true;attempt.toneTrack=null;return true;
        } catch(RuntimeException failure) { return false; }
    }
    private static Voice installedVoice(TextToSpeech engine,Locale locale) {
        if(locale==null)return null;
        Set<Voice> voices=engine.getVoices(); if(voices==null)return null;
        Voice current=engine.getVoice(),best=null;
        for(Voice voice:voices) {
            if(voice==null || voice.isNetworkConnectionRequired() || !voice.getLocale().getLanguage().equals(locale.getLanguage())
                    || voice.getFeatures()!=null && voice.getFeatures().contains("notInstalled"))continue;
            if(voice.equals(current) && (locale.getCountry().isEmpty() || voice.getLocale().getCountry().equals(locale.getCountry())))return voice;
            if(best==null || voice.getLocale().equals(locale) && !best.getLocale().equals(locale)
                    || voice.getLocale().equals(best.getLocale()) && voice.getQuality()>best.getQuality())best=voice;
        }
        return best;
    }
    private static boolean same(PlaybackGrant a,PlaybackGrant b) {
        return b!=null && a.attemptId.equals(b.attemptId) && a.attemptNonce.equals(b.attemptNonce)
                && a.ownerToken.equals(b.ownerToken) && a.ownerEpoch==b.ownerEpoch
                && a.consumerNonce.equals(b.consumerNonce) && a.providerSession.equals(b.providerSession)
                && a.bootIdentity.equals(b.bootIdentity) && a.ownerUid==b.ownerUid && a.eventKey.equals(b.eventKey)
                && a.revision==b.revision && a.mode==b.mode;
    }
    private static Field field(Object target,String name) throws NoSuchFieldException {
        Field field=target.getClass().getDeclaredField(name); field.setAccessible(true); return field;
    }
    private static Object read(Object target,String name) throws ReflectiveOperationException { return field(target,name).get(target); }
    private static boolean flag(Object target,String name) throws ReflectiveOperationException { return field(target,name).getBoolean(target); }
    private static int integer(Object target,String name) throws ReflectiveOperationException { return field(target,name).getInt(target); }
    private static Method method(Object target,String name,Class<?>... types) throws NoSuchMethodException {
        Method method=target.getClass().getDeclaredMethod(name,types); method.setAccessible(true); return method;
    }
    private static final class Attempt {
        final VelaWarningSpeaker owner;
        final Object nativeGuide;
        final WarningEvent event;
        final PlaybackGrant grant;
        final Listener listener;
        final long generation,announceUntil;
        final String id="warning-"+UUID.randomUUID();
        TextToSpeech engine;
        AudioTrack toneTrack;
        boolean captured,dispatched,submitted,started,cancelled,nativeStopped,released,terminal,quiescent,toneReleased,releaseFailed;
        Attempt(VelaWarningSpeaker owner,WarningEvent event,PlaybackGrant grant,Listener listener,long generation,long announceUntil) {
            this.owner=owner;this.nativeGuide=owner.guide;this.event=event;this.grant=grant;this.listener=listener;this.generation=generation;this.announceUntil=announceUntil;
        }
    }
}
