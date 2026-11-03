package nl.johninnl.warningprobe;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

/** Private actual-native-entry capture probe. Detectors and public report submission stay off. */
public final class NativeFmCaptureProbe extends Instrumentation {
    private static final String REPORTS="nl.flitsmeister.fmcore.data.model.reports.";
    private static final String EARLY="nl.flitsmeister.earlywarning.model.";
    private Context context;
    private Bundle arguments;
    private Class<?> capture,audio;
    private final List<Bundle> events=Collections.synchronizedList(new ArrayList<>());
    private final List<String> roots=new ArrayList<>();
    private ScheduledExecutorService renewer;
    private SharedPreferences preferences,lease;
    private Map<String,?> oldPreferences,oldLease;
    private AudioManager manager;
    private int initialVolume;
    private final AtomicInteger maxActive=new AtomicInteger();
    private String integratedEventKey,observedConsumerNonce;
    private int observedConsumerUid=-1;
    private boolean terminalObserved,ownerPresent,pendingPresent;
    private boolean startedObserved;
    private boolean startedTerminalObserved;
    private String lastStartedTerminalState;
    private String lastTerminalState;
    private final AudioManager.AudioPlaybackCallback playback=new AudioManager.AudioPlaybackCallback(){
        @Override public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs){observePlayback(configs);}
    };
    @Override public void onCreate(Bundle args){super.onCreate(args);arguments=args==null?new Bundle():args;start();}
    @Override public void onStart(){
        Bundle output=new Bundle();JSONObject report=new JSONObject();
        try{
            String scenario=arguments.getString("scenario","sound");boolean integrated=scenario.equals("integrated_trigger")||scenario.equals("integrated_matrix")||scenario.equals("integrated_handover");
            context=getTargetContext();check(context.getPackageName().equals("nl.flitsmeister"),"Unexpected target");
            check(context.getApplicationInfo().uid==Process.myUid(),"Target UID mismatch");bootstrap();
            preferences=(SharedPreferences)type("androidx.preference.PreferenceManager").getMethod("getDefaultSharedPreferences",Context.class).invoke(null,context);
            lease=context.getSharedPreferences("navigation_startup_lease",0);oldPreferences=new HashMap<>(preferences.getAll());oldLease=new HashMap<>(lease.getAll());
            manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);initialVolume=manager.getStreamVolume(AudioManager.STREAM_MUSIC);
            manager.registerAudioPlaybackCallback(playback,new Handler(Looper.getMainLooper()));observePlayback(manager.getActivePlaybackConfigurations());if(!integrated)check(maxActive.get()==0,"Baseline active playback is not quiet");
            audio=type("nl.johninnl.navigation.startup.ProviderAudio");renew();
            renewer=Executors.newSingleThreadScheduledExecutor();renewer.scheduleAtFixedRate(()->{try{renew();}catch(Exception e){throw new IllegalStateException(e);}},1,1,TimeUnit.SECONDS);
            capture=type("nl.johninnl.flitsmeister.NativeWarnings");
            if(!integrated){Method reset=capture.getDeclaredMethod("resetForTest",Context.class,Consumer.class);reset.setAccessible(true);reset.invoke(null,context,(Consumer<Bundle>)value->events.add(new Bundle(value)));}
            if(scenario.equals("sound"))sound(report);
            else if(scenario.equals("point"))point(report);
            else if(scenario.equals("voice"))voice(report);
            else if(scenario.equals("cits_direct"))directCits(report);
            else if(scenario.equals("early_bus"))earlyBus(report);
            else if(scenario.equals("impact"))impact(report);
            else if(scenario.equals("vri"))vri(report);
            else if(scenario.equals("cits"))cits(report);
            else if(scenario.equals("integrated_trigger"))realEndpointTrigger(report);
            else if(scenario.equals("integrated_matrix"))realEndpointMatrix(report);
            else if(scenario.equals("integrated_handover"))realEndpointHandover(report);
            else throw new IllegalArgumentException("Unknown native capture scenario");
            waitForIdleSync();SystemClock.sleep(300);assertMuted();observePlayback(manager.getActivePlaybackConfigurations());
            if(!integrated)check(maxActive.get()==0,"Active playback occurred during managed native capture");check(initialVolume==manager.getStreamVolume(AudioManager.STREAM_MUSIC),"Device stream volume changed");
            report.put("scenario",scenario);report.put("process_uid",Process.myUid());report.put("process_pid",Process.myPid());report.put("android_sdk",android.os.Build.VERSION.SDK_INT);
            report.put("actual_native_roots",new JSONArray(roots));report.put("events",eventJson());report.put("max_active_playbacks",maxActive.get());report.put("managed_mute_verified",true);report.put("device_stream_unchanged",true);
            report.put("detectors_started_by_probe",false);report.put("public_report_submission_called",false);report.put("probe_completed",true);
            if(!integrated)report.put("detectors_started",false);else report.put("real_consumer_may_start_provider_engine",true);
            report.put("acceptance_scope","Actual patched native entry/callback capture with genuine initialization, controlled synthetic inputs and managed mute; not natural detector or receiver/car acceptance");
            output.putString("fm_capture_result",report.toString());
        }catch(Throwable error){output.putString("fm_capture_error",android.util.Log.getStackTraceString(error));}
        finally{
            // Keep the verified mute lease until the runner force-stops this process. Restoring
            // it earlier could unmute a late native callback; the runner restores exact lease XML.
            try{if(renewer!=null){renewer.shutdownNow();check(renewer.awaitTermination(2,TimeUnit.SECONDS),"Lease renewal did not stop");}if(oldPreferences!=null)restore(preferences,oldPreferences);if(manager!=null)manager.unregisterAudioPlaybackCallback(playback);}
            catch(Throwable error){output.putString("fm_capture_cleanup_error",android.util.Log.getStackTraceString(error));}
        }
        finish(output.containsKey("fm_capture_result")&&!output.containsKey("fm_capture_cleanup_error")?Activity.RESULT_OK:Activity.RESULT_CANCELED,output);
    }
    private void bootstrap()throws Exception{
        Class<?> globals=type("t25");if(globals.getField("a").get(null)!=null)return;
        Object factory=type("z80").getField("e").get(null),value=call(factory,"getInstance",context);
        globals.getMethod("i",Context.class,type("ye4")).invoke(null,context,value);
    }
    private void renew()throws Exception{type("nl.johninnl.navigation.startup.ProviderEngineService").getMethod("renewLease",Context.class).invoke(null,context);assertMuted();}
    private void assertMuted()throws Exception{check(Boolean.TRUE.equals(audio.getMethod("muted").invoke(null)),"Managed ProviderAudio lease is not active");}
    // The public unprivileged active-playback view must stay empty. Do not rely on
    // hidden player-state/UID accessors or change platform hidden-API policy.
    private void observePlayback(List<AudioPlaybackConfiguration> configs){maxActive.accumulateAndGet(configs.size(),Math::max);}
    private void sound(JSONObject report)throws Exception{
        preferences.edit().putBoolean("pref_enable_custom_spoken_warnings",false).putBoolean("pref_traffic_cam_warning_sound",false).commit();
        Object sound=singleton("s6i","f");check(Boolean.FALSE.equals(call(sound,"g")),"Native local speech preference not disabled");
        Object camera=camera("TRAFFIC_CAMERA","probe-native-sound");Completion first=new Completion();int before=announces();
        onMain(()->call(sound,"l",camera,true,false,"TEST ONLY native camera instruction",first.proxy));roots.add("Ls6i;->l");first.await();
        check(announces()==before+1,"Actual s6i.l did not emit exactly one ANNOUNCE");Bundle one=lastAnnounce();check("TEST ONLY native camera instruction".equals(one.getString("message")),"Native text was lost before local g() suppression");
        Completion repeat=new Completion();onMain(()->call(sound,"l",camera,true,true,"TEST ONLY native camera instruction",repeat.proxy));repeat.await();
        Bundle two=lastAnnounce();check(one.getString("warning_id").equals(two.getString("warning_id"))&&two.getLong("announcement_index")==one.getLong("announcement_index")+1,"Native repeated intent lost distinct identity");
        Object notification=notification("AMBULANCE","probe-native-early"),type=call(notification,"getType");Completion early=new Completion();
        onMain(()->call(sound,"b",type,null,notification,"TEST ONLY ambulance behind you",true,early.proxy));roots.add("Ls6i;->b");early.await();
        check("TEST ONLY ambulance behind you".equals(lastAnnounce().getString("message")),"Actual early native entry lost text");
        Object queue=call(sound,"d");check(((java.util.Queue<?>)queue.getClass().getField("f").get(queue)).isEmpty(),"Muted native sound reached playback queue");
        report.put("native_completions",first.count.get()+repeat.count.get()+early.count.get());report.put("local_speech_disabled",true);report.put("local_traffic_sound_disabled",true);report.put("native_queue_empty",true);
    }
    private void attachProductionReports()throws Exception{
        Class<?> feed=type("nl.johninnl.flitsmeister.NavigationBroadcasts");Field app=feed.getDeclaredField("app");app.setAccessible(true);app.set(null,context);
        Field active=feed.getDeclaredField("active");active.setAccessible(true);active.setBoolean(null,false);
        Method subscribe=feed.getDeclaredMethod("subscribeReports");subscribe.setAccessible(true);onMain(()->subscribe.invoke(null));
        // Uses the unchanged production subscription/proxy; inactive legacy publication prevents
        // consumer auto-start. No detector/service or fake native implementation is created.
    }
    private void controlledFix()throws Exception{
        Location fix=new Location("synthetic-native-probe");fix.setLatitude(52);fix.setLongitude(5);fix.setAccuracy(3);fix.setSpeed(20);fix.setBearing(0);fix.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        Class<?> source=type("bza");source.getField("b").set(null,fix);source.getField("h").set(null,null);
    }
    private void point(JSONObject report)throws Exception{
        attachProductionReports();controlledFix();Object service=singleton("fc0","S1");call(service,"P");Object camera=camera("SPEED_50","probe-native-point");
        int before=announces();onMain(()->call(service,"J",camera,true));roots.add("Lfc0;->J");waitFor(()->announces()==before+1,"Native accepted point did not announce");
        String id=lastAnnounce().getString("warning_id");int updates=count("UPSERT",id);onMain(()->call(service,"O",camera));roots.add("Lfc0;->O");waitFor(()->count("UPSERT",id)>updates,"Native state update did not reach production report subscription");
        check(announces()==before+1,"State update manufactured an ANNOUNCE");onMain(()->call(service,"J",camera,true));waitForIdleSync();check(announces()==before+1,"Native immediate repeat guard was bypassed");
        onMain(()->call(service,"J",camera,false));waitFor(()->has("CLEAR",id),"Native removal did not clear source state");
        Class<?> typeClass=type(REPORTS+"SpeedCamType");Method mute=typeClass.getMethod("muteCategory",Context.class,int.class,boolean.class);Object type=enumValue(REPORTS+"SpeedCamType","RED_LIGHT");int category=-1;
        for(int i=0;i<3;i++)mute.invoke(null,context,i,false);
        for(int i=0;i<3;i++){mute.invoke(null,context,i,true);if(Boolean.TRUE.equals(call(type,"isSpeedCamTypeMutedFromCategory",context))){category=i;break;}mute.invoke(null,context,i,false);}
        check(category>=0,"Could not select the native red-light category filter");Object disabled=camera("RED_LIGHT","probe-native-disabled-category");int accepted=announces();
        onMain(()->call(service,"J",disabled,true));waitForIdleSync();SystemClock.sleep(200);check(announces()==accepted,"Native disabled category still exported an announcement");
        report.put("state_updates_never_announce",true);report.put("native_removal_cleared",true);report.put("native_immediate_repeat_guard_preserved",true);report.put("native_category_filter_preserved",true);report.put("native_muted_category_index",category);
    }
    private void voice(JSONObject report)throws Exception{
        controlledFix();preferences.edit().putBoolean("pref_enable_custom_spoken_warnings",false).commit();Object incident=type(REPORTS+"Incident").getConstructor().newInstance();
        call(incident,"setId","probe-native-voice-only");call(incident,"setIncidentType",enumValue(REPORTS+"IncidentType","VOICE_ONLY"));call(incident,"setVoiceOnlyText","TEST ONLY native voice-only warning");
        Object manager=type("xl").getField("a").get(null);int before=announces();onMain(()->call(manager,"a",context,incident));
        waitFor(()->announces()==before+1,"Accepted resumed native VOICE_ONLY arm did not announce");roots.add("Lxl$c;->invokeSuspend");check("TEST ONLY native voice-only warning".equals(lastAnnounce().getString("message")),"VOICE_ONLY text lost before g()");
        report.put("accepted_resumed_voice_only",true);
    }
    private void directCits(JSONObject report)throws Exception{
        controlledFix();Class<?> eventClass=type("bemobile.cits.sdk.core.model.response.RoadWorks");Object event=eventClass.getConstructor().newInstance();
        event.getClass().getField("info").set(event,type("bemobile.cits.sdk.core.model.response.generalObjects.Info").getConstructor(JSONObject.class).newInstance(new JSONObject().put("eventID","probe-native-roadworks").put("version",1)));
        JSONObject geo=new JSONObject("{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[5,52.001]},\"properties\":{}}]}");
        event.getClass().getField("geoJSON").set(event,type("ma9").getMethod("a",JSONObject.class).invoke(null,geo));
        Object model=type("nl.flitsmeister.fmcore.models.data.cits.CitsRoadWorks").getConstructor(eventClass).newInstance(event),service=singleton("fc0","S1");int before=announces();
        onMain(()->call(service,"x",model));roots.add("Lfc0;->x");waitFor(()->announces()==before+1,"Actual accepted CITS branch did not announce");
        check("CitsRoadWorks".equals(lastAnnounce().getString("native_family")),"CITS family lost");report.put("actual_cits_branch",true);
    }
    private Object genuinePreferences()throws Exception{
        Class<?> api=type("xie"),entry=type("zp0");Object component=entry.getMethod("b",Context.class,Class.class).invoke(null,context,api);
        return api.getMethod("C").invoke(component);
    }
    private void earlyBus(JSONObject report)throws Exception{
        attachProductionReports();controlledFix();type("gqg").getField("b").setBoolean(null,true);
        Object service=singleton("nh7","Z");service.getClass().getField("s").set(service,genuinePreferences());Object listener=service.getClass().getField("I").get(service);
        Object notification=notification("AMBULANCE","probe-native-early-bus");call(notification,"setLatitude",52.001d);call(notification,"setLongitude",5d);
        int before=announces();onMain(()->call(listener,"onEarlyWarningNotificationUpdate",type("bza").getField("b").get(null),notification));roots.add("Lnh7$c;->onEarlyWarningNotificationUpdate");
        waitFor(()->announces()==before+1,"Accepted native early listener did not announce");String id=lastAnnounce().getString("warning_id");
        Object update=notification("AMBULANCE","probe-native-early-bus");call(update,"setLatitude",52.001d);call(update,"setLongitude",5d);call(update,"setFreetext","TEST ONLY updated early instruction");
        onMain(()->call(listener,"onEarlyWarningNotificationUpdate",type("bza").getField("b").get(null),update));waitFor(()->hasMessage("UPSERT",id,"TEST ONLY updated early instruction"),"Native early update did not produce revised state text");
        check(announces()==before+1,"Native early update manufactured ANNOUNCE");onMain(()->call(listener,"onEarlyWarningNotificationUpdate",type("bza").getField("b").get(null),null));
        waitFor(()->has("CLEAR",id),"Native early null removal did not clear");report.put("native_early_state_and_null_removal",true);report.put("native_service_onStart_called",false);
    }
    private Object fallbackCitsManager()throws Exception{
        preferences.edit().putString("pref_cits_base_url","ACCEPTANCE").commit();
        return type("cz2").getConstructor(Context.class,type("wie")).newInstance(context,genuinePreferences());
    }
    private Object baseEvent(String kind,String id)throws Exception{
        Object event=type("bemobile.cits.sdk.core.model.response."+kind).getConstructor().newInstance();
        JSONObject info=new JSONObject().put("eventID",id).put("version",1).put("countries",new JSONArray().put("nl"));
        event.getClass().getField("info").set(event,type("bemobile.cits.sdk.core.model.response.generalObjects.Info").getConstructor(JSONObject.class).newInstance(info));
        JSONObject geo=new JSONObject("{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[5,52.001]},\"properties\":{}}]}");
        event.getClass().getField("geoJSON").set(event,type("ma9").getMethod("a",JSONObject.class).invoke(null,geo));
        JSONObject alert=new JSONObject().put("codes",new JSONArray().put(new JSONObject().put("code",201)));
        event.getClass().getField("alertC").set(event,type("bemobile.cits.sdk.core.model.response.generalObjects.AlertC").getConstructor(JSONObject.class).newInstance(alert));
        event.getClass().getField("heading").setDouble(event,0);return event;
    }
    private void impact(JSONObject report)throws Exception{
        controlledFix();Object manager=fallbackCitsManager(),event=baseEvent("ImpactAttenuator","probe-native-impact");int before=announces();
        onMain(()->call(manager,"a",event));roots.add("Lcz2;->a");roots.add("Lxl;->k");waitFor(()->announces()==before+1,"Native Impact producer did not announce once");
        String id=lastAnnounce().getString("warning_id");onMain(()->call(manager,"e",event));roots.add("Lcz2;->e");waitFor(()->has("UPSERT",id),"Native Impact update missing");
        check(announces()==before+1,"Native Impact projection/update duplicated ANNOUNCE");onMain(()->call(manager,"b",event));roots.add("Lcz2;->b");waitFor(()->has("CLEAR",id),"Native Impact deletion did not clear");
        report.put("native_impact_single_authority",true);report.put("native_impact_update_and_clear",true);
    }
    private void vri(JSONObject report)throws Exception{
        controlledFix();Object manager=fallbackCitsManager(),event=baseEvent("SpeedAdvice","probe-native-vri");event.getClass().getField("adviceKph").setInt(event,40);event.getClass().getField("isTrafficLight").setBoolean(event,true);
        int before=announces();onMain(()->call(manager,"a",event));roots.add("Lcz2;->a");waitFor(()->hasAnyType("UPSERT","CITS_SPEED_ADVICE"),"Native VRI state missing");String id=lastType("CITS_SPEED_ADVICE").getString("warning_id");
        event.getClass().getField("adviceKph").setInt(event,50);onMain(()->call(manager,"e",event));roots.add("Lcz2;->e");waitFor(()->lastType("CITS_SPEED_ADVICE").getInt("native_advice_kmh")==50,"Native VRI update lost speed");
        onMain(()->call(manager,"b",event));roots.add("Lcz2;->b");waitFor(()->has("CLEAR",id),"Native VRI removal missing");check(announces()==before,"Native VRI state manufactured speech authority");report.put("native_vri_state_only",true);
    }
    private Object emergencyEvent(String kind,String id)throws Exception{
        Object event=baseEvent(kind,id);String prefix="bemobile.cits.sdk.core.model.response.generalObjects.";Class<?> vehicle=type(prefix+"SourceVehicle");
        event.getClass().getField("sourceVehicleWhat").set(event,vehicle.getConstructor(type(prefix+"SourceVehicleType"),type(prefix+"SourceVehicleSubType")).newInstance(enumValue(prefix+"SourceVehicleType","SPECIAL"),enumValue(prefix+"SourceVehicleSubType","AMBULANCE")));
        Object language=type("t25").getMethod("c").invoke(null);String tag=((java.util.Locale)call(language,"a")).toLanguageTag();
        JSONObject free=new JSONObject().put("language",tag).put("freeText","TEST ONLY ambulance behind you");Object value=type(prefix+"FreeTextWhere").getConstructor(JSONObject.class).newInstance(free);
        event.getClass().getField("freeTextsWhere").set(event,Collections.singletonList(value));return event;
    }
    private void cits(JSONObject report)throws Exception{
        controlledFix();((Location)type("bza").getField("b").get(null)).setSpeed(0);type("bza").getField("h").set(null,null);
        preferences.edit().putString("pref_cits_base_url","ACCEPTANCE").commit();Object service=singleton("vz2","x1"),first=emergencyEvent("IncomingEmergencyVehicle","probe-native-cits-a"),second=emergencyEvent("IncomingEmergencyVehicle","probe-native-cits-b");int before=announces();
        onMain(()->check(Boolean.TRUE.equals(call(service,"J",first)),"Native CITS first eligibility rejected"));roots.add("Lvz2;->J");waitFor(()->announces()==before+1,"Native CITS first announce missing");String firstId=lastAnnounce().getString("warning_id");
        check("TEST ONLY ambulance behind you".equals(lastAnnounce().getString("native_instruction")),"Actual CITS instruction was discarded");
        check(lastAnnounce().getString("message").contains("TEST ONLY ambulance behind you"),"Native CITS playback payload lost the available instruction");check(hasText(lastAnnounce().getString("native_announced_text")),"Original native announced text missing");
        onMain(()->check(Boolean.TRUE.equals(call(service,"J",second)),"Native CITS second eligibility rejected"));waitFor(()->announces()==before+2,"Native CITS second announce missing");String secondId=lastAnnounce().getString("warning_id");check(!firstId.equals(secondId),"Same-labelled native CITS vehicles merged");
        Object updatedWhere=((List<?>)second.getClass().getField("freeTextsWhere").get(second)).get(0);updatedWhere.getClass().getField("freeText").set(updatedWhere,"TEST ONLY updated ambulance instruction");
        onMain(()->check(Boolean.TRUE.equals(call(service,"L",second)),"Native CITS update rejected"));roots.add("Lvz2;->L");waitFor(()->hasMessage("UPSERT",secondId,"TEST ONLY updated ambulance instruction"),"Native CITS update lost revised instruction");check(announces()==before+2,"Native CITS update duplicated ANNOUNCE");
        onMain(()->check(Boolean.TRUE.equals(call(service,"K",first)),"Native CITS deletion rejected"));roots.add("Lvz2;->K");waitFor(()->has("CLEAR",firstId),"Resumed native CITS deletion did not clear first event");roots.add("Lvz2$c;->invokeSuspend");
        check(!has("CLEAR",secondId),"Deleting first native CITS event cleared a different vehicle");report.put("native_cits_distinct_vehicle_ids",true);report.put("stationary_driver_did_not_veto_emergency",true);report.put("native_cits_update_and_individual_clear",true);report.put("native_cits_instruction_preserved",true);
    }
    /** Actual producer singleton/journal/owner only: no test sink, packet emit or coverage override. */
    private void realEndpointTrigger(JSONObject report)throws Exception{
        Field sink=capture.getDeclaredField("testSink");sink.setAccessible(true);check(sink.get(null)==null,"A test sink would bypass the production Endpoint");
        Object endpoint=capture.getMethod("init",Context.class).invoke(null,context);Object profile=read(endpoint,"profile");check(Boolean.TRUE.equals(profile.getClass().getField("complete").get(profile)),"Production producer coverage is not complete");
        Class<?> feed=type("nl.johninnl.flitsmeister.NavigationBroadcasts");onMain(()->feed.getMethod("init",Context.class).invoke(null,context));
        String receiver=arguments.getString("receiver_package","com.google.android.apps.maps");String session=String.valueOf(call(endpoint,"sourceSession"));
        String nativeId=arguments.getString("native_id","probe-integrated-"+java.util.UUID.randomUUID());
        status("production_endpoint_ready",new JSONObject().put("provider_session",session).put("receiver_package",receiver).put("producer_complete",true).put("test_sink_present",false).put("target_classloader_used",capture==context.getClassLoader().loadClass(capture.getName())).put("expected_warning_id","SpeedCam:"+nativeId).put("expected_type","SPEED_CAM").put("expected_subtype","SPEED_50"));
        int confirmWait=Integer.parseInt(arguments.getString("receiver_wait_seconds",arguments.getString("confirm_wait_seconds","120")));check(confirmWait>=30&&confirmWait<=120,"Invalid bounded source confirmation wait");long waitUntil=SystemClock.elapsedRealtime()+confirmWait*1000L;
        while(SystemClock.elapsedRealtime()<waitUntil&&!productionReceiverSynced(endpoint,receiver)){assertMuted();SystemClock.sleep(25);}
        check(productionReceiverSynced(endpoint,receiver),"Real receiver never confirmed/synced with production Endpoint");
        controlledFix();Object nativeService=singleton("fc0","S1");call(nativeService,"P");Object camera=camera("SPEED_50",nativeId);
        onMain(()->call(nativeService,"J",camera,true));roots.add("Lfc0;->J");
        Object event=null;long deadline=SystemClock.elapsedRealtime()+3000;
        while(event==null&&SystemClock.elapsedRealtime()<deadline){event=productionEvent(endpoint,session,nativeId);if(event==null)SystemClock.sleep(10);}
        check(event!=null,"Actual accepted native point did not reach the production journal");JSONObject record=new JSONObject((Map<?,?>)call(event,"fields"));report.put("production_native_event",record);
        Method eventKey=type("nl.johninnl.navigation.alerts.PlaybackGrant").getDeclaredMethod("key",event.getClass());eventKey.setAccessible(true);integratedEventKey=String.valueOf(eventKey.invoke(null,event));
        status("native_event_in_production_journal",record);check(sink.get(null)==null,"Production test sink unexpectedly changed");
        int hold=Integer.parseInt(arguments.getString("hold_seconds","20"));hold=Math.min(30,Math.max(10,hold));long minimum=SystemClock.elapsedRealtime()+hold*1000L,until=minimum+30000;
        Set<String> seen=new HashSet<>();JSONArray outcomes=new JSONArray();
        boolean quiescent=false;
        while(SystemClock.elapsedRealtime()<until){assertMuted();JSONArray fresh=productionOutcomes(endpoint,seen);for(int i=0;i<fresh.length();i++)outcomes.put(fresh.get(i));
            if(SystemClock.elapsedRealtime()>=minimum&&terminalObserved&&!ownerPresent&&!pendingPresent&&!((Set<?>)call(endpoint,"capablePackages")).contains(receiver)){quiescent=true;break;}SystemClock.sleep(25);}
        report.put("real_terminal_ack_observed",terminalObserved);report.put("receiver_source_off_and_quiescent",quiescent);check(quiescent,"Real receiver did not terminally ACK and release source ownership before cleanup");
        onMain(()->call(nativeService,"J",camera,false));report.put("production_endpoint_used",true);report.put("test_sink_used",false);report.put("raw_packet_emitted",false);report.put("coverage_override_used",false);report.put("source_dispositions",outcomes);report.put("source_process_hold_seconds",hold);report.put("provider_native_clear_called",true);
        Object sound=singleton("s6i","f"),queue=call(sound,"d");check(((java.util.Queue<?>)queue.getClass().getField("f").get(queue)).isEmpty(),"Provider native queue is not muted/empty");report.put("provider_native_queue_empty",true);
        status("native_trigger_complete",new JSONObject().put("source_dispositions",outcomes).put("provider_muted",true));
    }
    /** Observe two real consumers and trigger original native models; never create a lease or grant. */
    private void realEndpointHandover(JSONObject report)throws Exception{
        Field sink=capture.getDeclaredField("testSink");sink.setAccessible(true);check(sink.get(null)==null,"Production handover forbids a test sink");
        Object endpoint=capture.getMethod("init",Context.class).invoke(null,context),profile=read(endpoint,"profile");check(Boolean.TRUE.equals(read(profile,"complete")),"Incomplete native profile");
        type("nl.johninnl.flitsmeister.NavigationBroadcasts").getMethod("init",Context.class).invoke(null,context);
        String first="com.google.android.apps.maps",second="app.vela",session=String.valueOf(call(endpoint,"sourceSession")),base=arguments.getString("native_id");
        status("production_endpoint_ready",new JSONObject().put("provider_session",session).put("receiver_package",first));
        long end=SystemClock.elapsedRealtime()+120000;while(SystemClock.elapsedRealtime()<end&&(!productionReceiverSynced(endpoint,first)||!first.equals(productionOwnerPackage(endpoint)))){assertMuted();SystemClock.sleep(20);}
        check(first.equals(productionOwnerPackage(endpoint)),"First actual receiver never owned audio");status("handover_first_owner_ready",new JSONObject().put("owner",first));
        end=SystemClock.elapsedRealtime()+60000;while(SystemClock.elapsedRealtime()<end&&!productionReceiverSynced(endpoint,second)){assertMuted();SystemClock.sleep(20);}check(productionReceiverSynced(endpoint,second),"Second receiver never synced");
        SystemClock.sleep(1500);check(first.equals(productionOwnerPackage(endpoint)),"Second receiver stole first ready owner");
        report.put("both_confirmed_before_first_warning",productionReceiverSynced(endpoint,first)&&productionReceiverSynced(endpoint,second));
        Object service=singleton("fc0","S1");call(service,"P");JSONArray rows=new JSONArray();
        for(int index=1;index<=2;index++){
            String expected=index==1?first:second;
            if(index==2){end=SystemClock.elapsedRealtime()+20000;while(SystemClock.elapsedRealtime()<end&&(productionReceiverSynced(endpoint,first)||!second.equals(productionOwnerPackage(endpoint)))){assertMuted();SystemClock.sleep(20);}check(!productionReceiverSynced(endpoint,first)&&second.equals(productionOwnerPackage(endpoint)),"Actual release did not hand ownership to Vela");}
            String id=base+"-"+index;Object model=camera("SPEED_50",id);controlledFix();onMain(()->call(service,"J",model,true));roots.add("Lfc0;->J");Object event=awaitProductionEvent(endpoint,session,id,0);
            activateEvent(event);Set<String> seen=new HashSet<>();JSONArray outcomes=new JSONArray();status("native_event_in_production_journal",new JSONObject((Map<?,?>)call(event,"fields")));awaitTerminal(endpoint,seen,outcomes,SystemClock.elapsedRealtime()+15000,false);
            check("COMPLETED".equals(lastTerminalState),"Handover native speech not completed");check(expected.equals(productionOwnerPackage(endpoint)),"Wrong receiver owned terminal output");
            if(index==1){java.io.File barrier=new java.io.File(context.getExternalFilesDir(null),"native-warning-ownership/"+base+"-contender-busy");end=SystemClock.elapsedRealtime()+15000;while(!barrier.exists()&&SystemClock.elapsedRealtime()<end){assertMuted();check(first.equals(productionOwnerPackage(endpoint)),"First owner changed before release");SystemClock.sleep(20);}check(barrier.exists(),"Second genuine speaker never reported BUSY");check(barrier.delete(),"Private BUSY barrier cleanup");report.put("contender_busy_observed_before_release",true);}
            onMain(()->call(service,"J",model,false));JSONObject row=new JSONObject().put("index",index).put("owner",expected).put("authority",new JSONObject((Map<?,?>)call(event,"fields"))).put("source_dispositions",outcomes);rows.put(row);report.put("handover_records",rows);status(index==1?"handover_first_complete":"handover_second_complete",row);
        }
        end=SystemClock.elapsedRealtime()+20000;while(SystemClock.elapsedRealtime()<end){productionOutcomes(endpoint,new HashSet<>());if(!ownerPresent&&!pendingPresent&&((Set<?>)call(endpoint,"capablePackages")).isEmpty())break;assertMuted();SystemClock.sleep(20);}
        check(!ownerPresent&&!pendingPresent&&((Set<?>)call(endpoint,"capablePackages")).isEmpty(),"Actual receivers did not release and quiesce");
        Object queue=call(singleton("s6i","f"),"d");check(((java.util.Queue<?>)queue.getClass().getField("f").get(queue)).isEmpty(),"Source queue not muted");
        report.put("production_endpoint_used",true).put("test_sink_used",false).put("raw_packet_emitted",false).put("coverage_override_used",false).put("provider_native_queue_empty",true).put("receiver_source_off_and_quiescent",true).put("real_terminal_ack_observed",true).put("handover_records",rows);
    }
    private String productionOwnerPackage(Object endpoint)throws Exception{
        java.util.concurrent.ExecutorService actor=(java.util.concurrent.ExecutorService)read(endpoint,"actor");return actor.submit(()->{Object owner=read(endpoint,"audio");check(owner!=null,"Actual owner storage missing");Object snapshot=call(owner,"snapshot",SystemClock.elapsedRealtime()),lease=read(snapshot,"owner");return lease!=null&&Boolean.TRUE.equals(read(lease,"ready"))?String.valueOf(read(lease,"packageName")):"";}).get(3,TimeUnit.SECONDS);
    }
    private Object productionEvent(Object endpoint,String session,String id)throws Exception{
        return productionEventAfter(endpoint,session,id,0);
    }
    private Object productionEventAfter(Object endpoint,String session,String id,long after)throws Exception{
        java.util.concurrent.ExecutorService actor=(java.util.concurrent.ExecutorService)read(endpoint,"actor");
        return actor.submit(()->{Object journal=read(endpoint,"journal"),view=call(journal,"resume",session,0L,SystemClock.elapsedRealtime());
            for(Object item:(List<?>)view.getClass().getField("events").get(view))if(String.valueOf(item.getClass().getField("warningId").get(item)).endsWith(id)&&String.valueOf(item.getClass().getField("operation").get(item)).equals("ANNOUNCE")&&item.getClass().getField("sequence").getLong(item)>after)return item;
            return null;
        }).get(3,TimeUnit.SECONDS);
    }
    private boolean productionReceiverSynced(Object endpoint,String receiver)throws Exception{
        java.util.concurrent.ExecutorService actor=(java.util.concurrent.ExecutorService)read(endpoint,"actor");
        return actor.submit(()->{Map<?,?> registrations=(Map<?,?>)read(endpoint,"registrations");for(Object registration:registrations.values()){
            Object client=read(registration,"client");if(receiver.equals(client.getClass().getField("packageName").get(client))&&Boolean.TRUE.equals(read(registration,"confirmed"))&&Boolean.TRUE.equals(read(registration,"synced")))return true;
        }return false;}).get(3,TimeUnit.SECONDS);
    }
    private JSONArray productionOutcomes(Object endpoint,Set<String> seen)throws Exception{
        java.util.concurrent.ExecutorService actor=(java.util.concurrent.ExecutorService)read(endpoint,"actor");
        return actor.submit(()->{Object owner=read(endpoint,"audio");check(owner!=null,"Production owner storage is unavailable");Object snapshot=call(owner,"snapshot",SystemClock.elapsedRealtime()),lease=snapshot.getClass().getField("owner").get(snapshot);JSONArray out=new JSONArray();ownerPresent=lease!=null;pendingPresent=snapshot.getClass().getField("pending").get(snapshot)!=null;
            if(lease!=null){Object client=read(lease,"client");observedConsumerUid=client.getClass().getField("uid").getInt(client);observedConsumerNonce=String.valueOf(client.getClass().getField("consumerNonce").get(client));}
            if(observedConsumerUid>0){for(Object grant:(List<?>)call(owner,"dispositions",observedConsumerUid,observedConsumerNonce,SystemClock.elapsedRealtime())){
                    if(!integratedEventKey.equals(grant.getClass().getField("eventKey").get(grant)))continue;
                    Object outcome=grant.getClass().getField("outcome").get(grant);String state=String.valueOf(outcome),attempt=String.valueOf(grant.getClass().getField("attemptId").get(grant));
                    if(grant.getClass().getField("started").getBoolean(grant))startedObserved=true;
                    Method terminal=grant.getClass().getDeclaredMethod("terminal",outcome.getClass());terminal.setAccessible(true);if(Boolean.TRUE.equals(terminal.invoke(null,outcome))){terminalObserved=true;lastTerminalState=state;if(grant.getClass().getField("started").getBoolean(grant)){startedTerminalObserved=true;lastStartedTerminalState=state;}}
                    if(seen.add(attempt+":"+state))out.put(new JSONObject().put("receiver_uid",observedConsumerUid).put("outcome",state).put("started",grant.getClass().getField("started").getBoolean(grant)).put("mode",String.valueOf(grant.getClass().getField("mode").get(grant))));}
            }
            return out;
        }).get(3,TimeUnit.SECONDS);
    }
    private boolean productionOwnerReady(Object endpoint)throws Exception{
        java.util.concurrent.ExecutorService actor=(java.util.concurrent.ExecutorService)read(endpoint,"actor");return actor.submit(()->{Object owner=read(endpoint,"audio");if(owner==null)return false;Object snapshot=call(owner,"snapshot",SystemClock.elapsedRealtime()),lease=snapshot.getClass().getField("owner").get(snapshot);return lease!=null&&lease.getClass().getField("ready").getBoolean(lease);}).get(3,TimeUnit.SECONDS);
    }
    private void realEndpointMatrix(JSONObject report)throws Exception{
        Field sink=capture.getDeclaredField("testSink");sink.setAccessible(true);check(sink.get(null)==null,"Matrix must not install a test sink");Object endpoint=capture.getMethod("init",Context.class).invoke(null,context);Object profile=read(endpoint,"profile");check(Boolean.TRUE.equals(profile.getClass().getField("complete").get(profile)),"Production producer coverage incomplete");
        type("nl.johninnl.flitsmeister.NavigationBroadcasts").getMethod("init",Context.class).invoke(null,context);
        String receiver=arguments.getString("receiver_package","com.google.android.apps.maps"),session=String.valueOf(call(endpoint,"sourceSession")),base=arguments.getString("native_id","probe-matrix-"+java.util.UUID.randomUUID()),family=arguments.getString("trigger_family","ordinary"),pattern=arguments.getString("trigger_pattern","single");
        int count=Integer.parseInt(arguments.getString("event_count","1")),limit=Integer.parseInt(arguments.getString("matrix_timeout_seconds","600"));check(count>=1&&count<=100&&limit>=60&&limit<=900,"Invalid bounded native matrix");check(family.equals("ordinary")||family.equals("p0"),"Unknown native family");check(java.util.Arrays.asList("single","update","clear","repeat").contains(pattern),"Unknown native matrix pattern");
        status("production_endpoint_ready",new JSONObject().put("provider_session",session).put("receiver_package",receiver).put("producer_complete",true).put("test_sink_present",false).put("target_classloader_used",true).put("native_id_base",base).put("trigger_family",family).put("trigger_pattern",pattern).put("event_count",count).put("native_id_rule",count==1?"base":"base-001 through base-NNN"));
        int startup=Integer.parseInt(arguments.getString("receiver_wait_seconds","120"));check(startup>=30&&startup<=120,"Invalid matrix receiver wait");long startupEnd=SystemClock.elapsedRealtime()+startup*1000L;while(!productionReceiverSynced(endpoint,receiver)&&SystemClock.elapsedRealtime()<startupEnd){assertMuted();SystemClock.sleep(25);}check(productionReceiverSynced(endpoint,receiver),"Matrix real receiver never confirmed/synced");
        Object service=singleton("fc0","S1");call(service,"P");JSONArray rows=new JSONArray();long matrixEnd=SystemClock.elapsedRealtime()+limit*1000L;
        // A real native warning prepares the actual receiver language. Keep its authority
        // separate so cold initialization cannot enter the warm measured distribution.
        String warmupId=base+"-warmup";Object warmupModel=camera("SPEED_50",warmupId);
        onMain(()->{controlledFix();call(service,"J",warmupModel,true);});Object warmupEvent=awaitProductionEvent(endpoint,session,warmupId,0);JSONObject warmup=matrixRow(warmupEvent,0,"ordinary","warmup",false);activateEvent(warmupEvent);JSONArray warmupOutcomes=new JSONArray();Set<String> warmupSeen=new HashSet<>();status("native_event_in_production_journal",warmup.getJSONObject("authority"));warmup.put("source_dispositions",warmupOutcomes);report.put("warmup_record",warmup);awaitTerminal(endpoint,warmupSeen,warmupOutcomes,Math.min(matrixEnd,SystemClock.elapsedRealtime()+20000),false);check("COMPLETED".equals(lastTerminalState),"Genuine native warmup did not complete");warmup.put("source_dispositions",warmupOutcomes).put("source_terminal",lastTerminalState).put("excluded_from_warm_measurements",true);onMain(()->call(service,"J",warmupModel,false));
        long readyEnd=Math.min(matrixEnd,SystemClock.elapsedRealtime()+5000);while(!productionOwnerReady(endpoint)&&SystemClock.elapsedRealtime()<readyEnd){assertMuted();SystemClock.sleep(10);}check(productionOwnerReady(endpoint),"Actual receiver owner not ready after genuine warmup");status("native_matrix_warmup_terminal",warmup);report.put("warmup_record",warmup);
        int noGpsDelay=Integer.parseInt(arguments.getString("post_warmup_no_gps_delay_ms","0"));check(noGpsDelay>=0&&noGpsDelay<=12000,"Invalid bounded post-warmup no-GPS delay");
        if(noGpsDelay>0){
            onMain(()->matrixInput("p0"));long waitStarted=SystemClock.elapsedRealtime();
            status("native_matrix_no_gps_wait_started",new JSONObject().put("elapsed_ms",waitStarted).put("delay_ms",noGpsDelay).put("native_gps_present",type("bza").getField("b").get(null)!=null).put("native_speed",type("bza").getField("h").get(null)));
            while(SystemClock.elapsedRealtime()-waitStarted<noGpsDelay){assertMuted();SystemClock.sleep(20);}
            JSONObject expired=new JSONObject().put("started_elapsed_ms",waitStarted).put("ended_elapsed_ms",SystemClock.elapsedRealtime()).put("requested_delay_ms",noGpsDelay).put("native_gps_present",type("bza").getField("b").get(null)!=null).put("native_speed",type("bza").getField("h").get(null));
            report.put("post_warmup_no_gps_wait",expired);status("native_matrix_no_gps_wait_complete",expired);
        }
        int spacing=Integer.parseInt(arguments.getString("native_spacing_ms","1000"));check(spacing>=1000&&spacing<=5000,"Invalid controlled native input spacing");long previousNative=-1;
        for(int index=1;index<=count;index++){
            while(previousNative>=0&&SystemClock.elapsedRealtime()-previousNative<spacing){assertMuted();SystemClock.sleep(10);}
            check(SystemClock.elapsedRealtime()<matrixEnd,"Native matrix overall deadline");String id=count==1?base:base+"-"+String.format(java.util.Locale.ROOT,"%03d",index);Object model=matrixModel(family,id);boolean ready=productionOwnerReady(endpoint);
            previousNative=SystemClock.elapsedRealtime();onMain(()->{matrixInput(family);call(service,"J",model,true);});Object nativeEvent=awaitProductionEvent(endpoint,session,id,0);JSONObject row=matrixRow(nativeEvent,index,family,pattern,ready);JSONArray outcomes=new JSONArray();activateEvent(nativeEvent);Set<String> seen=new HashSet<>();
            status("native_event_in_production_journal",row.getJSONObject("authority"));
            if(pattern.equals("update")){long beforeUpdate=productionHead(endpoint,session);onMain(()->{Location updated=new Location("synthetic-updated-target");updated.setLatitude(52.0012);updated.setLongitude(5);call(model,"setLocation",updated);call(service,"O",model);});Object updated=awaitNativeOperation(endpoint,session,String.valueOf(nativeEvent.getClass().getField("warningId").get(nativeEvent)),"UPSERT",beforeUpdate);check(updated.getClass().getField("revision").getLong(updated)>nativeEvent.getClass().getField("revision").getLong(nativeEvent),"Native update did not advance revision");row.put("native_update_called",true).put("native_update",new JSONObject((Map<?,?>)call(updated,"fields")));}
            if(pattern.equals("clear")){
                long until=Math.min(matrixEnd,SystemClock.elapsedRealtime()+15000);while(!startedObserved&&SystemClock.elapsedRealtime()<until){append(outcomes,productionOutcomes(endpoint,seen));assertMuted();SystemClock.sleep(10);}check(startedObserved&&!startedTerminalObserved,"Could not clear a genuinely active started warning");onMain(()->call(service,"J",model,false));row.put("native_clear_after_started",true);
            }
            row.put("source_dispositions",outcomes);rows.put(row);report.put("matrix_records",rows);awaitTerminal(endpoint,seen,outcomes,Math.min(matrixEnd,SystemClock.elapsedRealtime()+20000),pattern.equals("clear"));check(pattern.equals("clear")?("INTERRUPTED".equals(lastTerminalState)||"CANCELLED".equals(lastTerminalState)):"COMPLETED".equals(lastTerminalState),"Native matrix terminal did not match requested behavior: "+lastTerminalState);row.put("source_terminal",lastTerminalState);status("native_matrix_event_terminal",new JSONObject().put("index",index).put("authority",row.getJSONObject("authority")).put("terminal",lastTerminalState));
            if(pattern.equals("repeat")){
                long sequence=nativeEvent.getClass().getField("sequence").getLong(nativeEvent);Object sound=singleton("s6i","f");boolean repeatReady=productionOwnerReady(endpoint);
                if(family.equals("ordinary")){String text=String.valueOf(call(call(model,"getSpeedCamType"),"getLocalizedTTS",context));onMain(()->call(sound,"l",model,true,false,text,null));}
                else{Object type=call(model,"getEarlyWarningTypeEnum");String text=String.valueOf(call(model,"reportTitleText",context));onMain(()->call(sound,"b",type,model,null,text,false,null));}
                Object repeat=awaitProductionEvent(endpoint,session,id,sequence);check(repeat.getClass().getField("announcementIndex").getLong(repeat)>nativeEvent.getClass().getField("announcementIndex").getLong(nativeEvent),"Native repeat reused announcement identity");JSONObject repeatRow=matrixRow(repeat,index,family,"repeat_intent",repeatReady);activateEvent(repeat);JSONArray repeatOutcomes=new JSONArray();Set<String> repeatSeen=new HashSet<>();status("native_event_in_production_journal",repeatRow.getJSONObject("authority"));repeatRow.put("source_dispositions",repeatOutcomes);rows.put(repeatRow);awaitTerminal(endpoint,repeatSeen,repeatOutcomes,Math.min(matrixEnd,SystemClock.elapsedRealtime()+20000),false);check("COMPLETED".equals(lastTerminalState),"Native repeat did not complete");repeatRow.put("source_terminal",lastTerminalState);status("native_matrix_repeat_terminal",repeatRow);
            }
            if(!pattern.equals("clear"))onMain(()->call(service,"J",model,false));check(sink.get(null)==null,"Matrix test sink changed");
        }
        status("native_matrix_complete",new JSONObject().put("provider_session",session).put("distinct_native_models",count).put("native_announcements",rows.length()+1).put("measured_announcements",rows.length()).put("warmup_warning_id","SpeedCam:"+warmupId));
        boolean off=false;Set<String> endSeen=new HashSet<>();long cleanupEnd=SystemClock.elapsedRealtime()+30000;while(SystemClock.elapsedRealtime()<cleanupEnd){productionOutcomes(endpoint,endSeen);if(!ownerPresent&&!pendingPresent&&!((Set<?>)call(endpoint,"capablePackages")).contains(receiver)){off=true;break;}assertMuted();SystemClock.sleep(25);}check(off,"Matrix receiver did not release source ownership/quiesce");
        Object queue=call(singleton("s6i","f"),"d");check(((java.util.Queue<?>)queue.getClass().getField("f").get(queue)).isEmpty(),"Matrix provider queue not empty");report.put("production_endpoint_used",true).put("test_sink_used",false).put("raw_packet_emitted",false).put("coverage_override_used",false).put("provider_native_queue_empty",true).put("receiver_source_off_and_quiescent",off).put("real_terminal_ack_observed",true).put("source_session",session).put("matrix_records",rows).put("distinct_native_models",count).put("native_announcements",rows.length()+1).put("measured_announcements",rows.length()).put("warmup_warning_id","SpeedCam:"+warmupId).put("trigger_family",family).put("trigger_pattern",pattern).put("native_input_spacing_ms",spacing);
    }
    private Object matrixModel(String family,String id)throws Exception{
        if(family.equals("ordinary"))return camera("SPEED_50",id);Object notification=notification("AMBULANCE",id);call(notification,"setFreetext","");int resource=context.getResources().getIdentifier("aa_ew_ambulance","string",context.getPackageName());check(resource!=0,"Native emergency resource missing");call(notification,"setTitle",context.getString(resource));call(notification,"setDescription","[]");return type("nl.flitsmeister.fmcore.models.data.reports.EarlyWarningReport").getConstructor(type(EARLY+"EarlyWarningNotification")).newInstance(notification);
    }
    private void matrixInput(String family)throws Exception{if(family.equals("ordinary"))controlledFix();else{type("bza").getField("b").set(null,null);type("bza").getField("h").set(null,Float.valueOf(0));}}
    private Object awaitProductionEvent(Object endpoint,String session,String id,long after)throws Exception{Object event=null;long until=SystemClock.elapsedRealtime()+3000;while(event==null&&SystemClock.elapsedRealtime()<until){event=productionEventAfter(endpoint,session,id,after);if(event==null)SystemClock.sleep(10);}check(event!=null,"Actual native matrix intent absent from real journal");return event;}
    private long productionHead(Object endpoint,String session)throws Exception{java.util.concurrent.ExecutorService actor=(java.util.concurrent.ExecutorService)read(endpoint,"actor");return actor.submit(()->{Object view=call(read(endpoint,"journal"),"resume",session,0L,SystemClock.elapsedRealtime());return view.getClass().getField("headSequence").getLong(view);}).get(3,TimeUnit.SECONDS);}
    private Object awaitNativeOperation(Object endpoint,String session,String id,String operation,long after)throws Exception{
        java.util.concurrent.ExecutorService actor=(java.util.concurrent.ExecutorService)read(endpoint,"actor");Object found=null;long until=SystemClock.elapsedRealtime()+3000;
        while(found==null&&SystemClock.elapsedRealtime()<until){found=actor.submit(()->{Object view=call(read(endpoint,"journal"),"resume",session,after,SystemClock.elapsedRealtime());for(Object event:(List<?>)view.getClass().getField("events").get(view))if(id.equals(event.getClass().getField("warningId").get(event))&&operation.equals(String.valueOf(event.getClass().getField("operation").get(event))))return event;return null;}).get(3,TimeUnit.SECONDS);if(found==null)SystemClock.sleep(10);}
        check(found!=null,"Native "+operation+" absent from real journal");return found;
    }
    private JSONObject matrixRow(Object event,int index,String family,String pattern,boolean ready)throws Exception{return new JSONObject().put("index",index).put("family",family).put("pattern",pattern).put("receiver_owner_ready_before_native",ready).put("authority",new JSONObject((Map<?,?>)call(event,"fields")));}
    private void activateEvent(Object event)throws Exception{Method key=type("nl.johninnl.navigation.alerts.PlaybackGrant").getDeclaredMethod("key",event.getClass());key.setAccessible(true);integratedEventKey=String.valueOf(key.invoke(null,event));terminalObserved=false;startedObserved=false;lastTerminalState=null;startedTerminalObserved=false;lastStartedTerminalState=null;}
    private void awaitTerminal(Object endpoint,Set<String> seen,JSONArray outcomes,long deadline,boolean clear)throws Exception{boolean desired=false;while(!desired&&SystemClock.elapsedRealtime()<deadline){append(outcomes,productionOutcomes(endpoint,seen));desired=clear?startedTerminalObserved&&("CANCELLED".equals(lastStartedTerminalState)||"INTERRUPTED".equals(lastStartedTerminalState)):"COMPLETED".equals(lastTerminalState);assertMuted();if(!desired)SystemClock.sleep(10);}check(desired,"Real receiver did not ACK desired native matrix outcome; observed "+lastTerminalState);if(clear)lastTerminalState=lastStartedTerminalState;}
    private static void append(JSONArray to,JSONArray from)throws Exception{for(int i=0;i<from.length();i++)to.put(from.get(i));}
    private void status(String stage,JSONObject value){Bundle status=new Bundle();status.putString("fm_capture_stage",stage);status.putString("fm_capture_detail",value.toString());sendStatus(1,status);}
    private Class<?> type(String name)throws ClassNotFoundException{return context.getClassLoader().loadClass(name);}
    private static Object read(Object object,String field)throws Exception{Field value=object.getClass().getDeclaredField(field);value.setAccessible(true);return value.get(object);}
    private Object camera(String type,String id)throws Exception{Object value=type(REPORTS+"SpeedCam").getConstructor().newInstance();call(value,"setSpeedCamType",enumValue(REPORTS+"SpeedCamType",type));call(value,"setId",id);Location target=new Location("synthetic-target");target.setLatitude(52.001);target.setLongitude(5);call(value,"setLocation",target);return value;}
    private Object notification(String type,String id)throws Exception{Object value=type(EARLY+"EarlyWarningNotification").getConstructor().newInstance();call(value,"setType",enumValue(EARLY+"EarlyWarningTypeEnum",type));call(value,"setName",id);call(value,"setCode",id);call(value,"setDescription","TEST ONLY native description");call(value,"setTitle","TEST ONLY native title");call(value,"setFreetext","TEST ONLY native early instruction");return value;}
    private Object singleton(String type,String field)throws Exception{
        AtomicReference<Object> value=new AtomicReference<>();onMain(()->value.set(call(type(type).getField(field).get(null),"getInstance",context)));return value.get();
    }
    private Object enumValue(String cls,String value)throws Exception{for(Object member:type(cls).getEnumConstants())if(((Enum<?>)member).name().equals(value))return member;throw new IllegalArgumentException(value);}
    private int announces(){synchronized(events){int count=0;for(Bundle event:events)if("ANNOUNCE".equals(event.getString("operation")))count++;return count;}}
    private Bundle lastAnnounce(){synchronized(events){for(int i=events.size()-1;i>=0;i--)if("ANNOUNCE".equals(events.get(i).getString("operation")))return events.get(i);}throw new IllegalStateException("Missing ANNOUNCE");}
    private boolean has(String operation,String id){synchronized(events){for(Bundle event:events)if(operation.equals(event.getString("operation"))&&id.equals(event.getString("warning_id")))return true;}return false;}
    private int count(String operation,String id){synchronized(events){int count=0;for(Bundle event:events)if(operation.equals(event.getString("operation"))&&id.equals(event.getString("warning_id")))count++;return count;}}
    private boolean hasMessage(String operation,String id,String text){synchronized(events){for(Bundle event:events)if(operation.equals(event.getString("operation"))&&id.equals(event.getString("warning_id"))&&event.getString("message","").contains(text))return true;}return false;}
    private boolean hasAnyType(String operation,String type){synchronized(events){for(Bundle event:events)if(operation.equals(event.getString("operation"))&&type.equals(event.getString("native_type")))return true;}return false;}
    private Bundle lastType(String type){synchronized(events){for(int i=events.size()-1;i>=0;i--)if(type.equals(events.get(i).getString("native_type")))return events.get(i);}throw new IllegalStateException("Missing type "+type);}
    private JSONArray eventJson()throws Exception{JSONArray result=new JSONArray();synchronized(events){for(Bundle event:events){JSONObject row=new JSONObject();for(String key:event.keySet())row.put(key,event.get(key));result.put(row);}}return result;}
    private interface Action{void run()throws Exception;}
    private void onMain(Action action)throws Exception{AtomicReference<Throwable> error=new AtomicReference<>();runOnMainSync(()->{try{assertMuted();action.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new IllegalStateException("Actual native entry failed",error.get());}
    private interface Condition{boolean ready();}
    private void waitFor(Condition condition,String message)throws Exception{long until=SystemClock.elapsedRealtime()+10000;while(!condition.ready()&&SystemClock.elapsedRealtime()<until){assertMuted();SystemClock.sleep(20);}check(condition.ready(),message);waitForIdleSync();}
    private final class Completion{
        final AtomicInteger count=new AtomicInteger();final CountDownLatch done=new CountDownLatch(1);final Object proxy;
        Completion()throws Exception{Class<?> api=type("z39");proxy=Proxy.newProxyInstance(api.getClassLoader(),new Class<?>[]{api},(p,m,a)->{
            if(m.getName().equals("invoke")){count.incrementAndGet();done.countDown();return type("tvj").getField("a").get(null);}if(m.getName().equals("hashCode"))return System.identityHashCode(p);if(m.getName().equals("equals"))return p==a[0];return "NativeCaptureCompletion";});}
        void await()throws Exception{check(done.await(10,TimeUnit.SECONDS),"Original native completion did not finish under mute");check(count.get()==1,"Native completion was not exactly once");}
    }
    private static Object call(Object object,String name,Object... args)throws Exception{return invoke(object.getClass(),object,name,args);}
    private static Object invoke(Class<?> cls,Object object,String name,Object... args)throws Exception{
        for(Method method:cls.getMethods())if(method.getName().equals(name)&&method.getParameterTypes().length==args.length){Class<?>[] types=method.getParameterTypes();boolean fits=true;for(int i=0;i<types.length;i++)if(args[i]==null?types[i].isPrimitive():!box(types[i]).isInstance(args[i])){fits=false;break;}if(fits){method.setAccessible(true);return method.invoke(object,args);}}
        throw new NoSuchMethodException(cls.getName()+"."+name);
    }
    private static Class<?> box(Class<?> value){if(!value.isPrimitive())return value;if(value==boolean.class)return Boolean.class;if(value==int.class)return Integer.class;if(value==float.class)return Float.class;if(value==long.class)return Long.class;if(value==double.class)return Double.class;return value;}
    @SuppressWarnings("unchecked") private static void restore(SharedPreferences prefs,Map<String,?> values){SharedPreferences.Editor edit=prefs.edit().clear();for(Map.Entry<String,?> entry:values.entrySet()){String key=entry.getKey();Object value=entry.getValue();if(value instanceof String)edit.putString(key,(String)value);else if(value instanceof Boolean)edit.putBoolean(key,(Boolean)value);else if(value instanceof Integer)edit.putInt(key,(Integer)value);else if(value instanceof Long)edit.putLong(key,(Long)value);else if(value instanceof Float)edit.putFloat(key,(Float)value);else if(value instanceof Set)edit.putStringSet(key,new HashSet<>((Set<String>)value));else throw new IllegalStateException("Unknown preference type");}check(edit.commit(),"Could not restore native preference fixture");}
    private static void check(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private static boolean hasText(String value){return value!=null&&!value.trim().isEmpty();}
}
