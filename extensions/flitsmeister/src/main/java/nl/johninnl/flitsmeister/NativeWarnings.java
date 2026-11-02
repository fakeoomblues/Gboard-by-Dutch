package nl.johninnl.flitsmeister;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import nl.johninnl.navigation.alerts.FlitsmeisterWarningTypes;
import nl.johninnl.navigation.alerts.WarningCapabilities;
import nl.johninnl.navigation.alerts.WarningEndpoint;
import nl.johninnl.navigation.startup.StartupProtocol;

/** Observes accepted native decisions. Never invokes sound, BLE or eligibility code. */
public final class NativeWarnings {
    private NativeWarnings() {}
    private static final int MAX_WARNINGS=128;
    private static final Map<String,Record> records=new LinkedHashMap<>();
    private static final IdentityHashMap<Object,String> anonymous=new IdentityHashMap<>();
    private static final IdentityHashMap<Object,Long> suppressedProjections=new IdentityHashMap<>();
    private static final ThreadLocal<Long> citsProjection=new ThreadLocal<>();
    private static Context app;
    private static WarningEndpoint endpoint;
    private static Consumer<Bundle> testSink;
    private static String presentationId;
    private static boolean captureEnabled;
    private static final class Record {
        Bundle fields; long revision=1,index; boolean listed;
    }
    public static synchronized WarningEndpoint init(Context context) {
        if(app==null){app=context.getApplicationContext();captureEnabled=true;}
        if(endpoint==null)endpoint=new WarningEndpoint(app,uid->{
            String[] packages=app.getPackageManager().getPackagesForUid(uid);
            String configured=NavigationBroadcasts.receiverPackage();
            if(!StartupProtocol.allowed(packages,configured))return null;
            for(String name:packages)if(StartupProtocol.allowed(new String[]{name},configured))return name;
            return null;
        },new WarningCapabilities(FlitsmeisterWarningTypes.MANIFEST_DIGEST,FlitsmeisterWarningTypes.required(),false));
        return endpoint;
    }
    public static synchronized WarningEndpoint endpoint(){return endpoint;}
    public static synchronized void started(){captureEnabled=true;}
    public static void regular(Object sound,Object report,boolean entering,boolean localMuteBypass,String text,Object completion) {
        capture(()->announce(report,null,text,entering?"enter":"exit",localMuteBypass,"regular"));
    }
    public static void early(Object sound,Object type,Object report,Object notification,String text,boolean localMuteBypass,Object completion) {
        capture(()->{
            if(!captureEnabled)return;
            Object model=notification!=null?notification:report;
            if(model==null)return;
            String effective=notification!=null?value(notification,"getType"):value(report,"getEarlyWarningTypeEnum");
            Bundle fields=describe(model);
            if(!effective.isEmpty())fields.putString("native_subtype",effective);
            fields.putString("native_early_type_argument",string(type));
            if(!effective.isEmpty()&&!string(type).equals(effective))fields.putString("capture_diagnostic","EARLY_TYPE_MISMATCH");
            announceFields(model,fields,text,"notice",localMuteBypass,"early");
        });
    }
    /** Called only inside the resumed, accepted VOICE_ONLY arm, before local speech filtering. */
    public static void voiceOnly(Object report){capture(()->announce(report,null,value(report,"getVoiceOnlyText"),"notice",false,"voice_only"));}
    /** Called only in fc0.x's accepted CitsBaseReport arm. */
    public static void cits(Object report){capture(()->announce(report,null,null,"notice",false,"cits"));}
    /** The immediately following synchronous CITS UI projection is the same native authority. */
    public static void suppressNextProjection(){citsProjection.set(SystemClock.elapsedRealtime()+1000);}
    /** Pin the actual converted object before CITS publishes it to its UI bus. */
    public static void suppressProjection(Object warning){capture(()->{
        long now=SystemClock.elapsedRealtime();suppressedProjections.entrySet().removeIf(e->e.getValue()<now);
        if(suppressedProjections.size()<MAX_WARNINGS)suppressedProjections.put(warning,now+1000);
    });}
    public static void presentation(Context context,Object warning,boolean announce) {
        capture(()->{
            if(!captureEnabled)return;
            if(app==null&&context!=null)app=context.getApplicationContext();
            if(warning==null){if(presentationId!=null)clear(presentationId);presentationId=null;return;}
            Long until=citsProjection.get();citsProjection.remove();
            if(announce&&until!=null&&SystemClock.elapsedRealtime()<=until)return;
            Long projectedUntil=suppressedProjections.get(warning);
            if(announce&&projectedUntil!=null&&SystemClock.elapsedRealtime()<=projectedUntil)return;
            Bundle fields=describe(warning);String text=join(value(warning,"getTitle"),value(warning,"getSubTitle"));
            String id=!announce&&presentationId!=null?presentationId:identity(warning,fields);
            if(presentationId!=null&&!presentationId.equals(id))clear(presentationId);
            presentationId=id;
            update(id,fields,text,"native_resource","notice",announce,false,"early_presentation",false);
        });
    }
    /** Snapshot updates never create announcement authority. */
    public static void reports(Object values) {
        if(!(values instanceof Collection))return;
        capture(()->{
            if(!captureEnabled)return;
            Set<String> seen=new HashSet<>();int count=0;
            for(Object entry:(Collection<?>)values){
                if(++count>MAX_WARNINGS)break;
                if(!Boolean.TRUE.equals(optional(entry,"isActive")))continue;
                Object report=optional(entry,"getReport");if(report==null)continue;
                Bundle fields=describe(report);String id=identity(report,fields);seen.add(id);
                Record old=records.get(id);
                boolean current=old!=null&&!"CLEAR".equals(old.fields.getString("operation"));
                String text=!current?nativeText(report):old.fields.getString("message","");
                String origin=!current?"native_resource":old.fields.getString("message_origin","native_resource");
                String phase=!current?"active":old.fields.getString("phase","active");
                update(id,fields,text,origin,phase,false,false,"report_state",true);
            }
            for(String id:new ArrayList<>(records.keySet())){
                Record record=records.get(id);
                if(record.listed&&!seen.contains(id)&&!"exit".equals(record.fields.getString("phase")))clear(id);
            }
        });
    }
    public static void stopped(){capture(()->{for(String id:new ArrayList<>(records.keySet()))clear(id);captureEnabled=false;anonymous.clear();suppressedProjections.clear();presentationId=null;citsProjection.remove();});}
    private static synchronized void capture(Runnable action){
        try{
            long now=SystemClock.elapsedRealtime();
            records.entrySet().removeIf(e->now>=e.getValue().fields.getLong("state_until_elapsed_ms"));
            anonymous.entrySet().removeIf(e->!records.containsKey(e.getValue()));
            suppressedProjections.entrySet().removeIf(e->now>e.getValue());
            action.run();
        }catch(RuntimeException error){Log.w("NativeWarnings","Capture unavailable: "+error.getClass().getSimpleName());}
    }
    private static void announce(Object report,Bundle fields,String supplied,String phase,boolean bypass,String path) {
        if(!captureEnabled||report==null)return;announceFields(report,fields==null?describe(report):fields,supplied,phase,bypass,path);
    }
    private static void announceFields(Object report,Bundle fields,String supplied,String phase,boolean bypass,String path) {
        String text=supplied==null||supplied.trim().isEmpty()?nativeText(report):supplied;
        String origin=supplied==null||supplied.trim().isEmpty()?"native_resource":"native_text";
        update(identity(report,fields),fields,text,origin,phase,!text.trim().isEmpty(),bypass,path,false);
    }
    private static void update(String id,Bundle fields,String text,String origin,String phase,boolean announce,boolean bypass,String path,boolean listed) {
        Record record=records.get(id);
        if(record==null){if(records.size()>=MAX_WARNINGS){overflow();return;}record=new Record();records.put(id,record);}
        fields.putString("message",text);fields.putString("message_origin",origin);fields.putString("language_tag",language());fields.putString("phase",phase);
        if(record.fields!=null&&semanticChange(record.fields,fields))record.revision++;
        if(announce)record.index++;
        long now=SystemClock.elapsedRealtime();
        fields.putString("warning_id",id);fields.putLong("revision",record.revision);fields.putLong("announcement_index",record.index);
        fields.putString("phase",phase);fields.putString("operation",announce?"ANNOUNCE":"UPSERT");
        fields.putString("message",text);fields.putString("message_origin",origin);fields.putString("language_tag",language());
        fields.putLong("observed_elapsed_ms",now);fields.putLong("state_until_elapsed_ms",now+30000);
        fields.putString("relationship","unknown");fields.putString("capture_path",path);fields.putBoolean("native_local_mute_bypass",bypass);
        fields.putString("relevance",phase.equals("exit")?"exit":"notice");
        if(text.trim().isEmpty())fields.putString("capture_diagnostic","MISSING_NATIVE_TEXT");
        record.fields=new Bundle(fields);record.listed|=listed;emit(fields);
    }
    private static void clear(String id){
        Record record=records.get(id);if(record==null||"CLEAR".equals(record.fields.getString("operation")))return;
        Bundle fields=new Bundle();fields.putString("operation","CLEAR");fields.putString("warning_id",id);
        fields.putLong("revision",record.revision);fields.putLong("announcement_index",record.index);long now=SystemClock.elapsedRealtime();
        fields.putLong("observed_elapsed_ms",now);fields.putLong("state_until_elapsed_ms",now+30000);
        record.fields=fields;record.listed=false;emit(fields);
    }
    private static void emit(Bundle fields){if(testSink!=null)testSink.accept(new Bundle(fields));else if(endpoint!=null)endpoint.emit(fields);}
    private static Bundle describe(Object report){
        Bundle b=new Bundle();String family=report.getClass().getSimpleName();
        b.putString("native_family",family);String type=value(report,"getReportType"),sub="";
        if(isFamily(report,"EarlyWarningReport"))sub=value(report,"getEarlyWarningTypeEnum");
        else if(isFamily(report,"EarlyWarningNotification")){type="EARLY_WARNING";sub=value(report,"getType");}
        else if(isFamily(report,"Incident"))sub=value(report,"getIncidentType");
        else if(isFamily(report,"SpeedCam"))sub=value(report,"getSpeedCamType");
        else if(isFamily(report,"ServiceVehicle"))sub=value(report,"getType");
        if(type.isEmpty())type=family;
        b.putString("native_type",type);if(!sub.isEmpty())b.putString("native_subtype",sub);
        b.putString("category",category(type,sub));b.putInt("priority",priority(type,sub));
        Object original=optional(report,"getLocation");
        if(original instanceof Location){Location l=(Location)original;long at=l.getElapsedRealtimeNanos()/1000000;
            if(at>0&&at<=SystemClock.elapsedRealtime()){b.putDouble("latitude",l.getLatitude());b.putDouble("longitude",l.getLongitude());b.putLong("position_observed_elapsed_ms",at);b.putBoolean("position_estimated",false);}}
        return b;
    }
    private static String identity(Object model,Bundle fields){
        String id=value(model,"getId");if(id.isEmpty())id=value(model,"getCode");
        if(id.isEmpty()){id=anonymous.get(model);if(id==null){if(anonymous.size()>=MAX_WARNINGS){overflow();return fields.getString("native_family","")+":overflow";}id=fields.getString("native_family","")+":"+UUID.randomUUID().toString();anonymous.put(model,id);}return id;}
        return fields.getString("native_family","")+":"+id;
    }
    private static boolean semanticChange(Bundle old,Bundle next){
        for(String key:new String[]{"native_type","native_subtype","native_family","category","priority","phase","message","message_origin","language_tag","latitude","longitude","position_observed_elapsed_ms","position_estimated"})
            if(!java.util.Objects.equals(old.get(key),next.get(key)))return true;
        return false;
    }
    private static void overflow(){if(endpoint!=null)endpoint.captureOverflow();}
    private static String category(String type,String subtype){
        if(type.equals("SPEED_CAM")||type.equals("SPEED_TRAP")||type.equals("AVG_SPEED_CHECK"))return "speed_control";
        if(subtype.equals("AMBULANCE")||subtype.equals("FIRE_DEPARTMENT")||subtype.equals("POLICE"))return "emergency_vehicle";
        if(type.equals("TRAFFIC_JAM"))return "traffic";return "road_danger";
    }
    private static int priority(String type,String subtype){
        if(subtype.equals("WRONG_WAY_DRIVER")||subtype.equals("PERSON_ON_ROAD")||subtype.equals("AMBULANCE")||subtype.equals("FIRE_DEPARTMENT")||subtype.equals("POLICE"))return 0;
        if(type.equals("SPEED_CAM")||type.equals("SPEED_TRAP")||type.equals("AVG_SPEED_CHECK")||subtype.equals("ACCIDENT")||subtype.equals("OBSTACLE"))return 1;
        return 2;
    }
    private static String nativeText(Object model){
        for(String method:new String[]{"getShortDescription","reportTitleText"}){
            if(app==null)break;try{Object result=model.getClass().getMethod(method,Context.class).invoke(model,app);String text=string(result);if(!text.trim().isEmpty())return text;}catch(ReflectiveOperationException ignored){}
        }
        for(String method:new String[]{"getFreeText","getFreetext","getDescription","getTitle"}){String text=value(model,method);if(!text.trim().isEmpty())return text;}
        return "";
    }
    private static boolean isFamily(Object object,String name){for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass())if(c.getSimpleName().equals(name))return true;return false;}
    private static Object optional(Object object,String method){if(object==null)return null;try{return object.getClass().getMethod(method).invoke(object);}catch(ReflectiveOperationException ignored){return null;}}
    private static String value(Object object,String method){return string(optional(object,method));}
    private static String string(Object value){return value==null?"":value instanceof Enum?((Enum<?>)value).name():String.valueOf(value);}
    private static String join(String a,String b){return a.isEmpty()?b:b.isEmpty()?a:a+". "+b;}
    private static String language(){if(app!=null){android.os.LocaleList locales=app.getResources().getConfiguration().getLocales();if(!locales.isEmpty())return locales.get(0).toLanguageTag();}return "und";}
    static synchronized void resetForTest(Context context,Consumer<Bundle> sink){app=context;testSink=sink;captureEnabled=true;records.clear();anonymous.clear();suppressedProjections.clear();presentationId=null;citsProjection.remove();}
    static synchronized void setEndpointForTest(WarningEndpoint value){
        if(endpoint!=null&&endpoint!=value)endpoint.close();endpoint=value;app=null;testSink=null;captureEnabled=false;
        records.clear();anonymous.clear();suppressedProjections.clear();presentationId=null;citsProjection.remove();
    }
}
