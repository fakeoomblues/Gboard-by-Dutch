package nl.johninnl.warningprobe.ownership;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.json.*;

/** Real receiver lifecycle/choices and read-only ownership observation. No constructed protocol objects. */
public final class OwnershipReceiverProbe extends Instrumentation {
    private Bundle args; private Class<?> telemetry,diagnostics; private boolean vela;
    @Override public void onCreate(Bundle value){super.onCreate(value);args=value;start();}
    @Override public void onStart(){
        JSONObject report=new JSONObject(); Bundle output=new Bundle(); Activity activity=null;
        SharedPreferences prefs=null; Map<String,Object> before=null;
        Map<String,Map<?,?>> archive=new LinkedHashMap<>(); List<JSONObject> states=new ArrayList<>();
        File off=null,stop=null,on=null; Object connection=null;
        try {
            check("true".equals(args.getString("reviewed_synthetic_choices")),"Reviewed choices required");
            Context context=getTargetContext();String pkg=context.getPackageName();vela=pkg.equals("app.vela");check(vela||pkg.equals("com.google.android.apps.maps")||pkg.equals("org.ungoogled.android.apps.maps"),"Unexpected target");
            String token=args.getString("control_token","");check(token.matches("[A-Za-z0-9_-]{1,80}"),"Fresh private control token required");
            File folder=new File(context.getExternalFilesDir(null),"native-warning-ownership");check(folder.exists()||folder.mkdirs(),"Control folder");
            off=new File(folder,token+"-off");on=new File(folder,token+"-on");stop=new File(folder,token+"-stop");check(!off.exists()&&!stop.exists()&&!on.exists(),"Reused controls");
            ClassLoader loader=context.getClassLoader();telemetry=loader.loadClass(vela?"nl.johninnl.vela.TelemetryRuntime":"org.ungoogled.ui.ExternalTelemetry");diagnostics=loader.loadClass("nl.johninnl.navigation.alerts.WarningDiagnostics");
            prefs=vela?context.getSharedPreferences("vela_settings",0):(SharedPreferences)method(telemetry,"prefs",Context.class).invoke(null,context);
            before=copy(prefs.getAll());if(vela)check(prefs.edit().putString("voice_engine","com.google.android.tts").putBoolean("car_speed_on",true).commit(),"Native voice choice");
            report.put("package",pkg).put("uid",android.os.Process.myUid()).put("off_control",off.getAbsolutePath()).put("stop_control",stop.getAbsolutePath());
            Intent launch=context.getPackageManager().getLaunchIntentForPackage(pkg);check(launch!=null,"Real launcher missing");launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);android.os.Bundle options=android.app.ActivityOptions.makeBasic().setLaunchBounds(new android.graphics.Rect(0,vela?1200:140,1080,vela?2260:1160)).toBundle();options.putInt("android.activity.windowingMode",5);activity=startActivitySync(launch,options);
            final Activity actualWindow=activity;SystemClock.sleep(250);check(actualWindow.isInMultiWindowMode(),"Real multi-window Activity required for simultaneous receivers");report.put("actual_multi_window",true);
            long until=SystemClock.elapsedRealtime()+10000;while(field(telemetry,"scheduler").get(null)==null&&SystemClock.elapsedRealtime()<until)SystemClock.sleep(20);check(field(telemetry,"scheduler").get(null)!=null,"Activity did not initialize graph");
            onMain(()->{Object settings=field(telemetry,"warningPreferences").get(null);Class<?> modes=loader.loadClass("nl.johninnl.navigation.alerts.WarningAudioOwner$Mode");Object voice=null;for(Object v:modes.getEnumConstants())if(v.toString().equals("VOICE"))voice=v;method(settings.getClass(),"mode",modes).invoke(settings,voice);method(settings.getClass(),"categories",int.class).invoke(settings,127);method(settings.getClass(),"confirm").invoke(settings);select("nl.flitsmeister");});
            boolean selected=true;String previous="";until=SystemClock.elapsedRealtime()+180000;
            while(SystemClock.elapsedRealtime()<until&&!stop.exists()){
                if(selected&&off.exists()){check(off.delete(),"Off control consumption");onMain(()->select(""));selected=false;status("RECEIVER_SOURCE_OFF",new JSONObject().put("package",pkg));}
                if(!selected&&on.exists()){check(on.delete(),"On control consumption");onMain(()->select("nl.flitsmeister"));selected=true;status("RECEIVER_SOURCE_ON",new JSONObject().put("package",pkg));}
                Object semantic=field(telemetry,"semantic").get(null),scheduler=field(telemetry,"scheduler").get(null);
                if(semantic!=null){Object current=method(semantic.getClass(),"connection").invoke(semantic);if(current!=null)connection=current;}
                String session=semantic==null?"off":String.valueOf(method(semantic.getClass(),"state").invoke(semantic));
                String audio=String.valueOf(method(scheduler.getClass(),"status").invoke(scheduler));
                String signature=session+":"+audio;
                if(!signature.equals(previous)){previous=signature;JSONObject row=new JSONObject().put("elapsed_ms",SystemClock.elapsedRealtime()).put("package",pkg).put("session",session).put("audio",audio);states.add(row);status("RECEIVER_STATE",row);}
                for(Object value:(List<?>)method(diagnostics,"snapshot").invoke(null)){Map<?,?> row=(Map<?,?>)value;archive.putIfAbsent(row.toString(),new HashMap<>(row));}check(archive.size()<=1024,"Archive bound");
                if(archive.size()>report.optInt("archived_rows",-1)){report.put("archived_rows",archive.size());JSONObject progress=new JSONObject().put("package",pkg).put("uid",android.os.Process.myUid()).put("states",new JSONArray(states)).put("diagnostics",new JSONArray(archive.values()));try(java.io.FileOutputStream stream=new java.io.FileOutputStream(new File(folder,token+"-progress.json"))){stream.write(progress.toString().getBytes("UTF-8"));}}
                SystemClock.sleep(20);
            }
            check(stop.exists(),"Ownership controller deadline");report.put("status","observed");
        }catch(Throwable error){put(report,"status","blocked");put(report,"error",android.util.Log.getStackTraceString(error));}
        finally{
            try{if(telemetry!=null){onMain(()->select(""));boolean quiet=false;long until=SystemClock.elapsedRealtime()+8000;while(SystemClock.elapsedRealtime()<until){Object scheduler=field(telemetry,"scheduler").get(null),speaker=field(telemetry,"nativeSpeaker").get(null);boolean idle=scheduler==null||field(scheduler.getClass(),"active").get(scheduler)==null&&field(scheduler.getClass(),"grantRequests").getInt(scheduler)==0;boolean pending=connection!=null&&(field(connection.getClass(),"outputOutstanding").getBoolean(connection)||field(connection.getClass(),"grantRequestPending").getBoolean(connection));if(idle&&!pending&&(speaker==null||field(speaker.getClass(),"active").get(speaker)==null)){quiet=true;break;}SystemClock.sleep(20);}put(report,"output_quiescent",quiet);check(quiet,"Receiver output not quiescent");}}catch(Throwable error){put(report,"cleanup_error",android.util.Log.getStackTraceString(error));put(report,"status","blocked");}
            try{if(activity!=null){Activity actual=activity;onMain(actual::finish);}if(prefs!=null&&before!=null){restore(prefs,before);boolean exact=copy(prefs.getAll()).equals(before);put(report,"preferences_restored_exactly",exact);check(exact,"Preference restore differs");}}catch(Throwable error){put(report,"restore_error",android.util.Log.getStackTraceString(error));put(report,"status","blocked");}
            if(on!=null&&on.exists())on.delete();if(off!=null&&off.exists())off.delete();if(stop!=null&&stop.exists())stop.delete();put(report,"states",new JSONArray(states));put(report,"diagnostics",new JSONArray(archive.values()));put(report,"probe_completed",true);
            output.putString("ownership_receiver_result",report.toString());finish("observed".equals(report.optString("status"))?Activity.RESULT_OK:Activity.RESULT_CANCELED,output);
        }
    }
    private void select(String source)throws Exception{if(vela){method(telemetry,"select",String.class).invoke(null,source);method(telemetry,"warningChoicesChanged").invoke(null);}else method(telemetry,"select",Context.class,String.class).invoke(null,getTargetContext(),source);}
    private interface Action{void run()throws Exception;}
    private void onMain(Action action)throws Exception{AtomicReference<Throwable> error=new AtomicReference<>();runOnMainSync(()->{try{action.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new IllegalStateException(error.get());}
    private void status(String stage,JSONObject detail){Bundle out=new Bundle();out.putString("stage",stage);out.putString("detail",detail.toString());sendStatus(0,out);}
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private static Field field(Class<?> owner,String name)throws Exception{for(Class<?> c=owner;c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException absent){}throw new NoSuchFieldException(name);}
    private static Method method(Class<?> owner,String name,Class<?>... types)throws Exception{for(Class<?> c=owner;c!=null;c=c.getSuperclass())try{Method m=c.getDeclaredMethod(name,types);m.setAccessible(true);return m;}catch(NoSuchMethodException absent){}throw new NoSuchMethodException(name);}
    private static Map<String,Object> copy(Map<String,?> source){Map<String,Object> out=new HashMap<>();for(Map.Entry<String,?> e:source.entrySet())out.put(e.getKey(),e.getValue() instanceof Set?new HashSet<>((Set<?>)e.getValue()):e.getValue());return out;}
    private static void restore(SharedPreferences prefs,Map<String,Object> before){SharedPreferences.Editor e=prefs.edit().clear();for(Map.Entry<String,Object> item:before.entrySet()){Object v=item.getValue();String k=item.getKey();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,(Set<String>)v);else throw new IllegalStateException("Preference type");}check(e.commit(),"Restore commit");}
    private static void put(JSONObject target,String key,Object value){try{target.put(key,value);}catch(Exception ignored){}}
}
