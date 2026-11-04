package org.ungoogled.waze;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import nl.johninnl.navigation.alerts.WarningCapabilities;
import nl.johninnl.navigation.alerts.WarningEndpoint;
import nl.johninnl.navigation.alerts.WazeWarningTypes;
import nl.johninnl.navigation.startup.ProviderAudio;
import nl.johninnl.navigation.startup.StartupProtocol;

/** Native Waze decisions into the SAME protocol/journal/owner used by FM. No audio/UI actions. */
public final class NativeWarnings {
    private NativeWarnings(){}
    private static final int MAX_WARNINGS=128;
    private static final Map<String,Record> records=new LinkedHashMap<>();
    // Current native repository models outlive the bounded display-state cache. A later
    // genuine sound decision can therefore keep its identity without replaying stale speech.
    private static final Map<String,Object> nativeModels=new LinkedHashMap<>();
    private static final Map<String,String> nativeLinks=new LinkedHashMap<>();
    private static final Set<String> segments=new HashSet<>(),notifications=new HashSet<>();
    private static final Map<Object,ZoneDecision> zones=new WeakHashMap<>();
    private static final ThreadLocal<Object> presentation=new ThreadLocal<>();
    private static Context app;
    private static WarningEndpoint endpoint;
    private static Consumer<Bundle> testSink;
    private static long announcementCounter;
    private static boolean repositoryObserved,controllersResolved;
    private static String controllerFailure="waiting_for_engine";
    private static final class Record {Bundle fields;long revision=1,index;}
    private static final class ZoneDecision {final String id,phase;ZoneDecision(String id,String phase){this.id=id;this.phase=phase;}}

    /** Changed to true only after the patch validates/binds the complete original method set. */
    public static boolean bound(){return false;}
    static boolean supportsEventProtocol(Context context){
        if(!bound())return false;
        try{android.content.pm.PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),0);return "5.26.90.901".equals(info.versionName)&&info.getLongVersionCode()==1030746;}
        catch(android.content.pm.PackageManager.NameNotFoundException unavailable){return false;}
    }
    public static synchronized WarningEndpoint init(Context context){
        app=context.getApplicationContext();
        if(endpoint==null)endpoint=new WarningEndpoint(app,uid->{
            String[] packages=app.getPackageManager().getPackagesForUid(uid);String configured=NavigationBroadcasts.receiverPackage();
            if(!StartupProtocol.allowed(packages,configured))return null;
            for(String name:packages)if(StartupProtocol.allowed(new String[]{name},configured))return name;return null;
        },new WarningCapabilities(WazeWarningTypes.MANIFEST_DIGEST,WazeWarningTypes.required(),false));
        // The structural inventory/guards are not a headless capture/playback audit.
        // Keep coverage incomplete until the required Waze capture gates are evidenced.
        return endpoint;
    }
    public static synchronized WarningEndpoint endpoint(){return endpoint;}
    static synchronized String controllerState(){return controllersResolved?(repositoryObserved?"native_updates_observed":"waiting_for_native_updates"):controllerFailure;}
    static void prepareControllers(){
        Context context; synchronized(NativeWarnings.class){context=app;if(controllersResolved||context==null||!bound()||!NavigationBroadcasts.isEnabled())return;}
        try{
            if(!NavigationStartup.running())return;
            ClassLoader loader=NativeWarnings.class.getClassLoader();Class<?> lookup=Class.forName("kotlinx.coroutines.a.ab",true,loader);
            Object provider=lookup.getMethod("l",Class.class).invoke(null,Class.forName("com.waze.alerters.b.a",true,loader));
            Object selector=WazeWarningModels.call(provider,"a");
            // These are Waze-owned singleton flows/scopes. Never cancel them or create an Activity.
            if(selector==null||WazeWarningModels.call(selector,"a")==null)throw new IllegalStateException("MISSING_NATIVE_SELECTOR");
            Object zone=lookup.getMethod("l",Class.class).invoke(null,Class.forName("com.waze.navigate.cd",true,loader));
            if(zone==null)throw new IllegalStateException("MISSING_NATIVE_ZONE_CONTROLLER");
            synchronized(NativeWarnings.class){controllersResolved=true;controllerFailure="";}
        }catch(ReflectiveOperationException|RuntimeException unavailable){synchronized(NativeWarnings.class){controllerFailure="binding_unproven";}Log.w("UA","Native warning controllers unavailable");}
    }
    private static boolean enabled(){return testSink!=null||app!=null&&bound()&&NavigationBroadcasts.isEnabled();}
    private static boolean managed(){return testSink!=null||ProviderAudio.muted();}
    private static synchronized void capture(Runnable action){
        if(!enabled())return;
        long now=SystemClock.elapsedRealtime();records.entrySet().removeIf(e->now>=e.getValue().fields.getLong("state_until_elapsed_ms"));
        try{action.run();}catch(RuntimeException unavailable){if(endpoint!=null)endpoint.captureOverflow();Log.w("UA","Native warning capture unavailable: "+unavailable.getClass().getSimpleName());}
    }
    public static void repository(Object update){capture(()->{
        Object values=WazeWarningModels.call(update,"getDescriptorsList");if(!(values instanceof Collection)||((Collection<?>)values).size()>MAX_WARNINGS)throw new IllegalArgumentException("NATIVE_REPOSITORY_BOUND");
        Map<String,Bundle> parsed=new LinkedHashMap<>();Map<String,Object> models=new LinkedHashMap<>();
        for(Object model:(Collection<?>)values){Bundle fields=WazeWarningModels.descriptor(app,model);String id=fields.getString("warning_id");if(parsed.put(id,fields)!=null)throw new IllegalArgumentException("DUPLICATE_NATIVE_ID");models.put(id,model);}
        for(String id:new ArrayList<>(nativeModels.keySet()))if(!models.containsKey(id))clear(id);
        nativeModels.clear();nativeModels.putAll(models);nativeLinks.clear();repositoryObserved=true;
        for(Bundle fields:parsed.values()){String key=fields.getString("native_link_key"),id=fields.getString("warning_id");String old=nativeLinks.get(key);nativeLinks.put(key,old==null||old.equals(id)?id:"");}
        long now=SystemClock.elapsedRealtime();for(Bundle fields:parsed.values()){
            if(!fields.getBoolean("native_valid")||fields.containsKey("native_silent_activation")){
                clear(fields.getString("warning_id"));fields.putString("native_category",fields.getString("category"));fields.putString("category","display_only");
            }
            put(fields,false,now,"native_repository");
        }
    });}
    public static void sound(Object identity){capture(()->{
        String id=WazeWarningModels.identity(identity);Object shown=presentation.get();presentation.remove();
        if(shown!=null&&WazeWarningModels.identity(WazeWarningModels.call(shown,"a")).equals(id)){
            // Either selector or real UI sound may arrive first. Both observations refer to
            // this controller's one accepted entry, rather than independent announcements.
            zoneDecision(shown,"enter","native_zone_sound");return;
        }
        Object model=nativeModels.get(id);if(model==null)throw new IllegalStateException("UNCORRELATED_NATIVE_SOUND");
        Bundle fields=WazeWarningModels.descriptor(app,model);
        if(!fields.getBoolean("native_valid")||fields.containsKey("native_silent_activation")||"display_only".equals(fields.getString("category")))return;
        if(!managed())return; // Do not replay independent Waze playback into a later receiving session.
        fields.putString("announcement_origin","native_sound_intent");put(fields,true,SystemClock.elapsedRealtime(),"native_sound");
    });}
    private static String zonePhase(Object controller){
        try{Field state=controller.getClass().getDeclaredField("b");state.setAccessible(true);Object value=state.get(controller);int ordinal=((Enum<?>)value).ordinal();return ordinal==1?"enter":ordinal==2?"exit":"approach";}
        catch(ReflectiveOperationException|ClassCastException incompatible){throw new IllegalStateException("WAZE_ZONE_STATE_ABI",incompatible);}
    }
    /** Native selector acceptance; no native shown/start/timeout acknowledgement is forged. */
    public static void zoneSelected(Object controller){capture(()->zoneDecision(controller,zonePhase(controller),"native_zone_selection"));}
    public static void zonePresented(Object controller){capture(()->{
        presentation.remove();if(zonePhase(controller).equals("enter"))presentation.set(controller);
    });}
    /** Original LeftZone arm before the original native exit-sound callback. */
    public static void zoneExit(Object controller){capture(()->zoneDecision(controller,"exit","native_zone_exit_sound"));}
    private static void zoneDecision(Object controller,String phase,String origin){
        if(!managed()||phase.equals("approach"))return;
        String id=WazeWarningModels.identity(WazeWarningModels.call(controller,"a"));ZoneDecision old=zones.get(controller);
        if(old!=null&&old.id.equals(id)&&old.phase.equals(phase))return;
        Object model=nativeModels.get(id);if(model==null)throw new IllegalStateException("UNCORRELATED_NATIVE_ZONE");
        Bundle fields=WazeWarningModels.descriptor(app,model);String expected=phase.equals("enter")?"InZone":"LeftZone";
        if(!expected.equals(fields.getString("native_zone_state"))||fields.containsKey("native_silent_activation")||!fields.getBoolean("native_valid"))return;
        if(zones.size()>=MAX_WARNINGS)throw new IllegalArgumentException("NATIVE_ZONE_BOUND");
        fields.putString("phase",phase);fields.putString("relevance",phase.equals("exit")?"exit":"zone");fields.putString("announcement_origin",origin);
        if(phase.equals("exit")){
            boolean nl=WazeWarningModels.language(app).startsWith("nl"),danger=fields.getString("native_type","").equals("DANGER_ZONE");
            fields.putString("message",danger?(nl?"Einde van de gevarenzone.":"End of danger zone."):(nl?"Einde van de controlezone.":"End of control zone."));
            fields.putString("message_origin","template");fields.putString("template_id","waze_native_zone_exit_v1");fields.putString("language_tag",nl?"nl":"en");
        }
        put(fields,true,SystemClock.elapsedRealtime(),"native_zone");zones.put(controller,new ZoneDecision(id,phase));
    }
    public static void segments(Object update){capture(()->{
        Object values=WazeWarningModels.call(update,"getInfoList");if(!(values instanceof Collection)||((Collection<?>)values).size()>MAX_WARNINGS)throw new IllegalArgumentException("SEGMENT_BOUND");
        Map<String,Bundle> parsed=new LinkedHashMap<>();long observed=SystemClock.elapsedRealtime();
        for(Object info:(Collection<?>)values){
            Bundle fields=WazeWarningModels.segment(app,info);String linked=null;boolean ambiguous=false;
            for(int i=0;i<fields.getInt("source_alert_count");i++){
                String candidate=nativeLinks.get(fields.getString("source_alert_"+i+"_link_key"));Record record=candidate==null?null:records.get(candidate);
                if(candidate!=null&&!candidate.isEmpty()&&record!=null&&fields.getString("source_alert_"+i+"_type").equals(record.fields.getString("native_type"))){if(linked!=null&&!linked.equals(candidate))ambiguous=true;else linked=candidate;}
            }
            if(linked!=null&&!ambiguous){
                Bundle nativeFields=new Bundle(records.get(linked).fields);
                nativeFields.putString("native_segment_type",fields.getString("native_type"));nativeFields.putString("native_segment_phase",fields.getString("native_segment_phase"));
                nativeFields.putString("native_segment_key",fields.getString("native_segment_key"));nativeFields.putBoolean("native_segment_correlated",true);
                if(fields.containsKey("distance_m")){nativeFields.putInt("native_segment_distance_m",fields.getInt("distance_m"));nativeFields.putLong("native_segment_observed_ms",observed);}
                put(nativeFields,false,nativeFields.getLong("observed_elapsed_ms"),"native_segment_correlated_state");
            }else{String id=fields.getString("warning_id");if(parsed.put(id,fields)!=null)throw new IllegalArgumentException("DUPLICATE_SEGMENT");}
        }
        for(String id:new ArrayList<>(segments))if(!parsed.containsKey(id))clear(id);
        segments.clear();segments.addAll(parsed.keySet());long now=SystemClock.elapsedRealtime();for(Bundle fields:parsed.values())put(fields,false,now,"native_segment_state");
    });}
    public static void notificationAdded(Object notification){capture(()->{
        Bundle fields=WazeWarningModels.notification(app,notification);String id=fields.getString("warning_id");
        if(notifications.size()>=MAX_WARNINGS&&!notifications.contains(id))throw new IllegalArgumentException("NOTIFICATION_BOUND");
        notifications.add(id);put(fields,false,SystemClock.elapsedRealtime(),"native_notification_state");
    });}
    public static void notificationRemoved(Object identity){capture(()->{String id=WazeWarningModels.notificationIdentity(identity);notifications.remove(id);clear(id);});}
    static boolean osm(String feature,long id,int subtype,String label,double latitude,double longitude,double distance,double trigger,long observed,boolean announce){boolean[] accepted={false};capture(()->{
        Bundle fields=WazeWarningModels.base("org.ungoogled.waze.OsmCameras",feature.equals("flockCameras")?"FLOCK_CAMERA":"CAMERA",label,"en");
        fields.putString("warning_id","waze/osm/"+WazeWarningModels.hash(feature+":"+id+":"+subtype));fields.putString("native_osm_feature",feature);fields.putString("native_osm_legacy_id","osm:"+feature+":"+id);
        fields.putString("native_subtype",feature.equals("flockCameras")?"PLATE_READER":subtype==1?"CAMERA_TYPE_REDLIGHT":subtype==2?"OSM_AVERAGE_SPEED_CAMERA":subtype==3?"CAMERA_TYPE_ROAD_SAFETY_STOP_SIGN":subtype==0?"CAMERA_TYPE_SPEED":"UNKNOWN_"+subtype);fields.putString("native_subtype_namespace","org.ungoogled.waze.OsmCameras.Kind");
        fields.putString("message_origin","template");fields.putString("template_id","waze_patch_owned_osm_v1");fields.putString("announcement_origin","patch_owned_osm");fields.putString("category","speed_control");fields.putDouble("latitude",latitude);fields.putDouble("longitude",longitude);
        fields.putString("relevance","point_ahead");fields.putInt("distance_m",(int)Math.round(distance));fields.putInt("trigger_m",(int)Math.round(trigger));fields.putBoolean("distance_estimated",true);fields.putBoolean("trigger_estimated",true);fields.putBoolean("position_estimated",false);
        // The existing layer proves its ahead-on-current-motion decision. Its input does not
        // prove equivalence to a native directed-road camera, so never merge merely by radius.
        fields.putString("dedup_status","unresolved_without_directed_road_equivalence");if(managed()){put(fields,announce,observed,"patch_owned_osm");accepted[0]=true;}
    });return accepted[0];}
    static void osmSnapshot(String feature,java.util.List<nl.johninnl.navigation.NavigationAlerts.Alert> values){capture(()->{
        Set<String> present=new HashSet<>();for(nl.johninnl.navigation.NavigationAlerts.Alert value:values)present.add(value.id);
        for(Map.Entry<String,Record> value:new ArrayList<>(records.entrySet()))if(feature.equals(value.getValue().fields.getString("native_osm_feature"))&&!present.contains(value.getValue().fields.getString("native_osm_legacy_id")))clear(value.getKey());
    });}
    static void overflow(){capture(()->{throw new IllegalStateException("SOURCE_CAPTURE_BOUND");});}
    static synchronized void stopped(){
        for(String id:new ArrayList<>(records.keySet()))clear(id);nativeModels.clear();nativeLinks.clear();segments.clear();notifications.clear();zones.clear();presentation.remove();repositoryObserved=false;controllersResolved=false;controllerFailure="waiting_for_engine";
    }
    private static void put(Bundle source,boolean announce,long observed,String path){
        String id=source.getString("warning_id");long now=SystemClock.elapsedRealtime();if(observed<0||observed>now||now-observed>=30000)return;
        Record record=records.get(id);if(record==null){if(records.size()>=MAX_WARNINGS)throw new IllegalArgumentException("WARNING_BOUND");record=new Record();records.put(id,record);}
        Bundle fields=new Bundle(source);
        if(record.fields!=null&&!sameContent(record.fields,fields))record.revision++;
        if(announce){if(fields.getString("message","").trim().isEmpty())throw new IllegalArgumentException("MISSING_NATIVE_TEXT");record.index=++announcementCounter;}
        fields.putLong("revision",record.revision);fields.putLong("announcement_index",record.index);fields.putString("operation",announce?"ANNOUNCE":"UPSERT");
        fields.putLong("observed_elapsed_ms",observed);long until=observed+30000;
        if(fields.containsKey("native_timeout_ms")&&fields.getLong("native_timeout_ms")>0)until=observed+Math.min(30000,fields.getLong("native_timeout_ms"));
        fields.putLong("state_until_elapsed_ms",until);fields.putString("capture_path",path);
        if(fields.containsKey("distance_m"))fields.putLong("distance_observed_elapsed_ms",observed);
        if(fields.containsKey("trigger_m"))fields.putLong("trigger_observed_elapsed_ms",observed);
        if(fields.containsKey("latitude")){fields.putLong("position_observed_elapsed_ms",observed);fields.putString("position_clock_origin","native_decision");}
        record.fields=new Bundle(fields);emit(fields);
    }
    private static boolean sameContent(Bundle a,Bundle b){
        for(String key:new String[]{"native_type","native_subtype","native_trait","category","priority","phase","message","language_tag","native_valid","native_silent_activation","native_car_title","native_car_body"})if(!java.util.Objects.equals(a.get(key),b.get(key)))return false;return true;
    }
    private static void clear(String id){Record record=records.get(id);if(record==null||"CLEAR".equals(record.fields.getString("operation")))return;Bundle b=new Bundle();b.putString("warning_id",id);b.putString("operation","CLEAR");b.putLong("revision",record.revision);b.putLong("announcement_index",record.index);long now=SystemClock.elapsedRealtime();b.putLong("observed_elapsed_ms",now);b.putLong("state_until_elapsed_ms",now+30000);record.fields=b;emit(b);}
    private static void emit(Bundle fields){if(testSink!=null)testSink.accept(new Bundle(fields));else if(endpoint!=null)endpoint.emit(fields);}
    static synchronized void resetForTest(Context context,Consumer<Bundle> sink){app=context;testSink=sink;records.clear();nativeModels.clear();nativeLinks.clear();segments.clear();notifications.clear();zones.clear();presentation.remove();announcementCounter=0;repositoryObserved=false;controllersResolved=false;controllerFailure="waiting_for_engine";}
}
