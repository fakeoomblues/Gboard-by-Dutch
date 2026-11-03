"""Run real Maps/Vela BUSY and release handover on the task-owned API34 AVD."""
import argparse
import subprocess,threading,queue,time,json,hashlib,re,uuid,sys,shutil
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__)
for name in ['sdk','source-apk','maps-apk','vela-apk','source-probe','maps-probe','vela-probe','output']:parser.add_argument('--'+name,type=Path,required=True)
parser.add_argument('--reviewed-synthetic-choices',action='store_true',required=True)
args=parser.parse_args();sdk=args.sdk;adb=sdk/'platform-tools/adb';serial='emulator-5584';output=args.output;output.mkdir(parents=True,exist_ok=False);token=uuid.uuid4().hex

def device(*args,check=True):return subprocess.run([str(adb),'-s',serial,*map(str,args)],capture_output=True,text=True,check=check,timeout=60)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
assert device('emu','avd','name').stdout.splitlines()[0]=='native_warnings_car_api34'
assert device('shell','getprop','ro.kernel.qemu').stdout.strip()=='1'
assert device('shell','getprop','ro.build.version.sdk').stdout.strip()=='34'
assert device('shell','getprop','sys.boot_completed').stdout.strip()=='1'
assert device('shell','id','-u').stdout.strip()=='0'
inputs={'nl.flitsmeister':args.source_apk,'com.google.android.apps.maps':args.maps_apk,'app.vela':args.vela_apk}
record={'serial':serial,'avd':'native_warnings_car_api34','output':str(output),'control_token':token,'installed_sha256':{}}
for pkg,apk in inputs.items():
 installed=device('shell','pm','path',pkg).stdout.strip().removeprefix('package:');actual=device('shell','sha256sum',installed).stdout.split()[0];assert actual==sha(apk),(pkg,actual);record['installed_sha256'][pkg]=actual
certs=[]
helpers={}
for kind in ['maps','vela','source']:
 apk=getattr(args,kind+'_probe');frozen=output/(kind+'-helper.apk');shutil.copyfile(apk,frozen);helpers[kind]=frozen
 build=apk.parent/'build-report.json'
 if build.exists():shutil.copyfile(build,output/(kind+'-build-report.json'))
for apk in [*inputs.values(),*helpers.values()]:
 certs.append(re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]+)',subprocess.check_output([str(sdk/'build-tools/36.0.0/apksigner'),'verify','--print-certs',str(apk)],text=True)).group(1))
assert len(set(certs))==1;record['signer_sha256']=certs[0];record['helper_sha256']={k:sha(v) for k,v in helpers.items()}
for kind in ['maps','vela']:device('install','--no-incremental','-r',helpers[kind])
def permissions():
 text=device('shell','dumpsys','package','nl.flitsmeister').stdout;out={}
 for name in ['ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION','ACCESS_BACKGROUND_LOCATION']:
  key='android.permission.'+name;m=re.search(re.escape(key)+r': granted=(true|false), flags=\[([^\]]*)\]',text)
  if m:out[key]={'granted':m[1]=='true','flags':m[2].strip()}
 return out
before=permissions();record['source_permissions_before']=before;changed=[];events=queue.Queue();processes={};threads=[];failure=None
window_before={key:device('shell','settings','get','global',key).stdout.strip() for key in ['enable_freeform_support','force_resizable_activities']};window_changed=[];record['window_settings_before']=window_before

def launch(name,args):
 process=subprocess.Popen(list(map(str,args)),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1);processes[name]=process
 def drain():
  with (output/(name+'-output.txt')).open('w') as f:
   for line in process.stdout:
    f.write(line);f.flush();events.put((name,line))
    if '_stage=' in line or 'STATUS: stage=' in line:print(name+': '+line.strip(),flush=True)
  events.put((name,None))
 t=threading.Thread(target=drain,daemon=True);t.start();threads.append(t)
def receiver(kind):
 launch(kind,[adb,'-s',serial,'shell','am','instrument','-w','-r','-e','reviewed_synthetic_choices','true','-e','control_token',token,'nl.johninnl.warningprobe.owner.'+kind+'/nl.johninnl.warningprobe.ownership.OwnershipReceiverProbe'])
def signal(kind,command):
 pkg='com.google.android.apps.maps' if kind=='maps' else 'app.vela';folder='/sdcard/Android/data/'+pkg+'/files/native-warning-ownership';device('shell','mkdir','-p',folder);device('shell','touch',folder+'/'+token+'-'+command)
try:
 for key,value in window_before.items():
  if value!='1':device('shell','settings','put','global',key,'1');window_changed.append(key)
 for perm,value in before.items():
  if not value['granted']:
   assert not any(x in value['flags'] for x in ['USER_FIXED','POLICY_FIXED','SYSTEM_FIXED']);device('shell','pm','grant','nl.flitsmeister',perm);changed.append(perm)
 launch('source',[sys.executable,'-u','scripts/navigation-alerts/run_fm_capture_probe.py','--adb',adb,'--serial',serial,'--target-apk',inputs['nl.flitsmeister'],'--probe-apk',helpers['source'],'--output',output/'source','--scenarios','integrated_handover','--native-id','probe-handover-'+token,'--matrix-timeout-seconds','180'])
 deadline=time.monotonic()+220
 while time.monotonic()<deadline:
  try:name,line=events.get(timeout=.2)
  except queue.Empty:
   if all(p.poll() is not None for p in processes.values()):break
   continue
  if line:
   if name=='source' and 'fm_capture_stage=production_endpoint_ready' in line and 'maps' not in processes:receiver('maps')
   if name=='source' and 'fm_capture_stage=handover_first_owner_ready' in line and 'vela' not in processes:receiver('vela')
   if name=='source' and 'fm_capture_stage=handover_first_complete' in line:signal('maps','off')
   if name=='source' and 'fm_capture_stage=handover_second_complete' in line:
    signal('maps','stop');signal('vela','stop')
   if name=='source' and 'fm_capture_stage=native_event_in_production_journal' in line:(output/('concurrent-activities-'+str(time.monotonic_ns())+'.txt')).write_text(device('shell','dumpsys','activity','activities').stdout)
   if name=='vela' and line.startswith('INSTRUMENTATION_STATUS: detail='):
    state=json.loads(line.split('detail=',1)[1])
    if state.get('audio','').startswith('BUSY:'):
     folder='/sdcard/Android/data/nl.flitsmeister/files/native-warning-ownership';device('shell','mkdir','-p',folder);device('shell','touch',folder+'/probe-handover-'+token+'-contender-busy');record['contender_busy_state']=state
   if 'fm_capture_error=' in line:raise RuntimeError('Source native handover failed; raw log preserved')
   if 'ownership_receiver_result=' in line:
    r=json.loads(line.split('ownership_receiver_result=',1)[1]);(output/(name+'-result.json')).write_text(json.dumps(r,indent=2));assert r.get('status')=='observed',r
  if all(p.poll() is not None for p in processes.values()):break
 assert all(p.poll() is not None for p in processes.values()),'Bounded handover did not end'
 for t in threads:t.join(3)
 source=list((output/'source').glob('*/result.json'));assert len(source)==1
 r=json.loads(source[0].read_text());record['source_result']=str(source[0]);assert r['probe_completed'] and r['network_and_lease_restored'] and not r.get('failure'),r
 for kind in ['maps','vela']:
  r=json.loads((output/(kind+'-result.json')).read_text());assert r['status']=='observed' and r['output_quiescent'] and r['preferences_restored_exactly']
 # Save genuine platform windowing/lifecycle evidence; no lifecycle or policy flag is fabricated.
 (output/'actual-activities.txt').write_text(device('shell','dumpsys','activity','activities').stdout)
except Exception as e:failure=repr(e)
finally:
 for kind in ['maps','vela']:
  if kind in processes and processes[kind].poll() is None:signal(kind,'stop')
 for name,p in processes.items():
  try:p.wait(timeout=65)
  except subprocess.TimeoutExpired:failure=(failure or '')+'; '+name+' remains live, verified cleanup required'
 for perm in reversed(changed):device('shell','pm','revoke','nl.flitsmeister',perm)
 record['source_permissions_after']=permissions();record['source_permissions_restored_exactly']=record['source_permissions_after']==before
 if not record['source_permissions_restored_exactly']:failure=(failure or '')+'; permission drift'
 for key in reversed(window_changed):
  current=device('shell','settings','get','global',key).stdout.strip()
  if current!='1':failure=(failure or '')+'; independent window-setting change, preserve for manual review';continue
  value=window_before[key];device('shell','settings','delete' if value=='null' else 'put','global',key,*([] if value=='null' else [value]))
 record['window_settings_after']={key:device('shell','settings','get','global',key).stdout.strip() for key in window_before};record['window_settings_restored_exactly']=record['window_settings_after']==window_before
 if not record['window_settings_restored_exactly']:failure=(failure or '')+'; window-setting drift'
 record['failure']=failure;record['probe_completed']=failure is None;record['process_exit_codes']={k:p.poll() for k,p in processes.items()};(output/'result.json').write_text(json.dumps(record,indent=2)+'\n');print(json.dumps(record),flush=True)
if failure:sys.exit(1)
