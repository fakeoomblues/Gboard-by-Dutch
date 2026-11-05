import Java from 'frida-java-bridge';
// Observation only: each wrapper calls the original once and returns/throws unchanged.
let count=0,exhausted=false;const limit=2500;const installed=[];const patched=new Set();
function emit(event,fields={}){if(count++<limit)send({event,pid:Process.id,host_clock_ms:Date.now(),...fields});else if(!exhausted){exhausted=true;send({event:'observer-budget-exhausted',pid:Process.id});}}
function field(o,name){if(o===null||o===undefined)return null;let c=o.getClass();while(c!==null){try{const f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(_){c=c.getSuperclass();}}return null;}
function scalar(o){if(o===null||o===undefined)return null;try{const n=o.getClass().getName().toString();if(n==='java.lang.Boolean')return o.toString().toString()==='true';if(['java.lang.Long','java.lang.Integer'].includes(n))return Number(o.toString());if(n==='java.lang.String')return o.toString().slice(0,100);if(o.name)return o.name().toString();}catch(_){}return null;}
function get(o,n){return scalar(field(o,n));}
function klass(o){try{return o===null?null:o.getClass().getName().toString();}catch(_){return null;}}
function state(o){const a=field(o,'active'),p=a===null?null:field(a,'pending'),g=a===null?null:field(a,'grant'),e=p===null?null:field(p,'authority'),lease=field(o,'lease');return {status:get(o,'status'),generation:get(o,'generation'),lease_until:lease===null?null:get(lease,'until'),lease_epoch:lease===null?null:get(lease,'epoch'),active:a!==null,started:a===null?null:get(a,'started'),cancelled:a===null?null:get(a,'cancelled'),attempt:g===null?null:get(g,'attemptId'),event_key:g===null?null:get(g,'eventKey'),authority_sequence:e===null?null:get(e,'sequence'),deadline:e===null?null:get(e,'announceUntil')};}
function safe(event,build){try{emit(event,{elapsed_ms:Number(Java.use('android.os.SystemClock').elapsedRealtime()),...build()});}catch(e){emit('observer-error',{where:event,error_type:e.name});}}
function hook(name,method,signature,before,after){const key=name+'!'+method+'('+signature.join(',')+')';if(patched.has(key))return;const C=Java.use(name),m=C[method].overload(...signature);m.implementation=function(...args){if(before)safe(key+':enter',()=>before(this,args));let result;try{result=m.call(this,...args);}catch(e){emit('original-exception',{method:key,error_type:e.$className||e.name});throw e;}if(after)safe(key+':return',()=>after(this,args,result));return result;};patched.add(key);installed.push(key);}
function stack(){return Java.backtrace({limit:10}).frames.map(f=>f.signature);}
function focusListener(request){const listener=field(request,'mOnAudioFocusChangeListener'),name=klass(listener);if(name)hook(name,'onAudioFocusChange',['int'],(_,a)=>({change:Number(a[0]),stack:stack()}));}
function policy(o){const p=field(o,'policy'),name=klass(p);if(!name)return;hook(name,'active',[],null,(_,a,r)=>({policy_active:!!r}));}
function progress(o){const p=field(o,'progress'),name=klass(p);if(!name)return;for(const method of ['onStart','onDone','onError']){try{hook(name,method,['java.lang.String'],(_,a)=>({utterance_id:String(a[0])}));}catch(_){}}try{hook(name,'onStop',['java.lang.String','boolean'],(_,a)=>({utterance_id:String(a[0]),interrupted:!!a[1]}));}catch(_){}}
Java.performNow(()=>{try{hook('java.lang.Thread','dispatchUncaughtException',['java.lang.Throwable'],(_,a)=>({exception_class:klass(a[0])}));}catch(e){emit('hook-optional-error',{where:'uncaught-exception',error_type:e.name});}});
const attachDeadline=Date.now()+20000;
function installApplicationHooks(){Java.performNow(()=>{try{
 const AT=Java.use('android.app.ActivityThread'),thread=AT.currentActivityThread(),bound=thread===null?null:thread.mBoundApplication.value;
 if(bound===null){if(Date.now()<attachDeadline){setTimeout(installApplicationHooks,25);return;}throw new Error('APPLICATION_BINDING_UNAVAILABLE');}
 // Instrumentation targets do not follow the ordinary Application.onCreate barrier.
 Java.classFactory.loader=bound.info.value.getClassLoader();
 const pkg=AT.currentProcessName().toString();
 for(const method of ['callActivityOnPause','callActivityOnResume','callActivityOnStop','callActivityOnDestroy'])hook('android.app.Instrumentation',method,['android.app.Activity'],(_,a)=>({activity:klass(a[0])}));
 hook('android.media.AudioManager','requestAudioFocus',['android.media.AudioFocusRequest'],(_,a)=>{focusListener(a[0]);return {};},(_,a,r)=>({result:Number(r)}));
 hook('android.speech.tts.TextToSpeech','speak',['java.lang.CharSequence','int','android.os.Bundle','java.lang.String'],(_,a)=>({utterance_id:String(a[3]),queue_mode:Number(a[1]),text_length:a[0]===null?0:Number(a[0].length())}),(_,a,r)=>({utterance_id:String(a[3]),result:Number(r)}));
 for(const method of ['stop','shutdown'])hook('android.speech.tts.TextToSpeech',method,[],()=>({stack:stack()}),(_,a,r)=>({result:r===undefined?null:Number(r)}));
 hook('android.app.Instrumentation','finish',['int','android.os.Bundle'],(_,a)=>({result_code:a[0],stimuli_finished:true}));
 if(pkg==='nl.flitsmeister'){
  hook('nl.johninnl.navigation.alerts.WarningEndpoint','handle',['int','int','java.lang.String','android.os.Bundle'],null,(_,a,r)=>({code:a[0],caller_uid:a[1],status:r===null?null:r.getString('status'),audio_status:r===null?null:r.getString('audio_status')}));
  hook('nl.johninnl.flitsmeister.NavigationControlService','observeControlClient',['int','boolean'],(_,a)=>({caller_uid:a[0],warning_aware:!!a[1]}));
  hook('nl.johninnl.flitsmeister.NavigationBroadcasts','engineRunning',[],null,(_,a,r)=>({engine_running:!!r}));
 }else if(pkg==='org.ungoogled.android.apps.maps'||pkg==='com.google.android.apps.maps'){
  const S='nl.johninnl.navigation.alerts.WarningScheduler',A=S+'$Active',G='nl.johninnl.navigation.alerts.PlaybackGrant';
  hook(S,'pumpOne',[],o=>{policy(o);return {};});
  hook(S,'cancel',[A,'boolean'],(o,a)=>({...state(o),retry:!!a[1],stack:stack()}));
  hook(S,'finish',[A,'nl.johninnl.navigation.alerts.PlaybackGrant$Outcome','boolean'],(o,a)=>({...state(o),outcome:a[1].name().toString(),quiescent:!!a[2]}));
  hook(S,'disconnected',[],o=>state(o));hook(S,'revoked',['long'],(o,a)=>({...state(o),revoked_epoch:Number(a[0])}));
  const W='org.ungoogled.ui.MapsWarningSpeaker',WA=W+'$Attempt';
  hook(W,'begin',[WA],(o,a)=>{progress(o);return {initialized:get(o,'initialized'),prepared_language:get(o,'preparedLanguage'),cancelled:get(a[0],'cancelled'),until:get(a[0],'until')};});
  hook(W,'cancel',[G],(o,a)=>({attempt:get(a[0],'attemptId'),stack:stack()}));
  hook(W,'selectVoice',['java.lang.String'],(_,a)=>({language:String(a[0])}),(o,a,r)=>({language:String(a[0]),selected:!!r,prepared_language:get(o,'preparedLanguage')}));
  hook(W,'isReady',['java.lang.String','nl.johninnl.navigation.alerts.WarningAudioOwner$Mode'],null,(o,a,r)=>({language:String(a[0]),mode:a[1].name().toString(),ready:!!r,closed:get(o,'closed'),initialized:get(o,'initialized'),prepared_language:get(o,'preparedLanguage')}));
  hook(W,'finish',[WA,'nl.johninnl.navigation.alerts.PlaybackGrant$Outcome','boolean'],(o,a)=>({attempt:get(field(a[0],'grant'),'attemptId'),cancelled:get(a[0],'cancelled'),started:get(a[0],'started'),outcome:a[1].name().toString(),quiescent:!!a[2]}));
  const C='org.ungoogled.ui.MapsGuidanceCoordinator';
  hook(C,'abandon',['long',C+'$Ready','java.lang.String'],(o,a)=>({reason:String(a[2]),holding:get(o,'holding'),fault:get(o,'fault')}));
  hook(C,'acquireMain',['java.lang.String','nl.johninnl.navigation.alerts.WarningEvent$Priority','long',C+'$Ready'],o=>{const m=field(o,'manager');return {bound:get(o,'bound'),fault:get(o,'fault'),holding:get(o,'holding'),native_active_class:m===null?null:klass(field(m,'p')),native_pending_class:m===null?null:klass(field(m,'q'))};});
  hook(C,'canRegenerate',[],null,(_,a,r)=>({can_regenerate:!!r}));
 }
 emit('hooks-ready',{package:pkg,installed});
}catch(e){emit('hooks-failed',{error_type:e.$className||e.name,error:String(e).slice(0,240),installed});}});}
setImmediate(installApplicationHooks);
