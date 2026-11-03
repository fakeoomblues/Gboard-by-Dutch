"""Verify persisted uncertainty across actual provider-process death and parked recovery."""
import argparse
import io
import tarfile
import subprocess,threading,queue,time,json,hashlib,re,uuid,sys,shutil
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__)
for name in ['sdk','source-apk','maps-apk','vela-apk','source-probe','maps-probe','vela-probe','output']:parser.add_argument('--'+name,type=Path,required=True)
parser.add_argument('--receiver',choices=['maps','vela'],default='maps')
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
record={'scenario':'provider-restart','receiver':args.receiver,'serial':serial,'avd':'native_warnings_car_api34','output':str(output),'control_token':token,'installed_sha256':{}}
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
def snapshot_preferences(package,label):
 remote='/data/user/0/'+package+'/shared_prefs';state={'package':package,'remote':remote,'files':{}}
 if device('shell','test','-d',remote,check=False).returncode:
  state['directory_existed']=False;return state
 state['directory_existed']=True
 raw=subprocess.check_output([str(adb),'-s',serial,'exec-out','tar','-cf','-','-C',remote,'.']);archive=output/(label+'-preferences.tar');archive.write_bytes(raw);state['archive']=str(archive)
 with tarfile.open(fileobj=io.BytesIO(raw)) as tar:
  for item in tar.getmembers():
   name=item.name.removeprefix('./')
   if item.isdir() and name in ('','.'):continue
   assert item.isfile() and '/' not in name and re.fullmatch(r'[A-Za-z0-9_.+=-]+\.xml(?:\.bak)?',name),'Unexpected preference entry'
   data=tar.extractfile(item).read();state['files'][name]={'sha256':hashlib.sha256(data).hexdigest(),'uid':item.uid,'gid':item.gid,'mode':item.mode}
 return state
preferences_before={}
def assert_stopped(package):
 lines=device('shell','ps','-A','-o','UID,PID,NAME').stdout.splitlines()
 assert not any(line.split() and (line.split()[-1]==package or line.split()[-1].startswith(package+':')) for line in lines),package+' process is still alive'
for pkg in inputs:
 assert_stopped(pkg);preferences_before[pkg]=snapshot_preferences(pkg,pkg+'-before')
record['preferences_before']=preferences_before

def restore_preferences(package):
 assert_stopped(package);baseline=preferences_before[package];now=snapshot_preferences(package,package+'-pre-restore')
 remote=baseline['remote']
 for name in now['files'].keys()-baseline['files'].keys():device('shell','rm','-f',remote+'/'+name)
 if baseline.get('directory_existed'):
  device('shell','mkdir','-p',remote)
  with tarfile.open(baseline['archive']) as tar:
   for item in tar.getmembers():
    if not item.isfile():continue
    name=item.name.removeprefix('./');entry=baseline['files'][name];data=tar.extractfile(item).read();local=output/(package+'-restore-'+name);local.write_bytes(data)
    device('push',local,remote+'/'+name);device('shell','chown',str(entry['uid'])+':'+str(entry['gid']),remote+'/'+name);device('shell','chmod',format(entry['mode'],'o'),remote+'/'+name)
 elif device('shell','test','-d',remote,check=False).returncode==0:device('shell','rmdir',remote)
 after=snapshot_preferences(package,package+'-restored');assert after['files']==baseline['files'] and after['directory_existed']==baseline['directory_existed'],'Preference restore drift: '+package
 return {'restored_exactly':True,'files':after['files']}

receiver_package='com.google.android.apps.maps' if args.receiver=='maps' else 'app.vela'
base='probe-restart-'+token
source_details={}

def source(name,scenario,**expectations):
 command=[sys.executable,'-u','scripts/navigation-alerts/run_fm_capture_probe.py','--adb',adb,'--serial',serial,'--target-apk',inputs['nl.flitsmeister'],'--probe-apk',helpers['source'],'--output',output/name,'--scenarios',scenario,'--death-owner',receiver_package,'--native-id',base,'--matrix-timeout-seconds','240']
 for key,value in expectations.items():command.extend(['--'+key.replace('_','-'),value])
 launch(name,command)
def receiver(role):
 launch(role,[adb,'-s',serial,'shell','am','instrument','-w','-r','-e','reviewed_synthetic_choices','true','-e','control_token',token+'-'+role,'nl.johninnl.warningprobe.owner.'+args.receiver+'/nl.johninnl.warningprobe.ownership.OwnershipReceiverProbe'])
def signal(role):
 folder='/sdcard/Android/data/'+receiver_package+'/files/native-warning-ownership';device('shell','mkdir','-p',folder);device('shell','touch',folder+'/'+token+'-'+role+'-stop')
def receiver_report(role):
 text=(output/(role+'-output.txt')).read_text();m=re.search(r'^INSTRUMENTATION_RESULT: ownership_receiver_result=(.+)$',text,re.M);assert m,'Missing complete receiver result: '+role
 value=json.loads(m[1]);(output/(role+'-result.json')).write_text(json.dumps(value,indent=2)+'\n');assert value['status']=='observed' and value['output_quiescent'] and value['preferences_restored_exactly'];return value
def source_report(name):
 results=list((output/name).glob('*/result.json'));assert len(results)==1
 value=json.loads(results[0].read_text());assert value['network_and_lease_restored'] and not value['cleanup_errors'];return results[0],value

def stop_and_verify_outputs():
 for pkg in ['com.google.android.apps.maps','app.vela','com.google.android.tts']:
  device('shell','am','force-stop',pkg);assert_stopped(pkg)
 for pkg in ['com.google.android.apps.maps','app.vela']:record.setdefault('pre_recovery_preferences_cleanup',{})[pkg]=restore_preferences(pkg)
 audio=device('shell','dumpsys','audio').stdout;(output/'audio-before-parked-reset.txt').write_text(audio);players=audio.split('  players:',1)[1].split('  ducked players',1)[0]
 relevant={int(re.search(r'uid:(\d+)',device('shell','pm','list','packages','-U',pkg).stdout)[1]) for pkg in [*inputs,'com.google.android.tts']}
 for line in players.splitlines():
  uid=re.search(r'u/pid:(\d+)/',line);assert not(uid and int(uid[1]) in relevant and 'state:started' in line),'Relevant player remains active'
 record['outputs_verified_stopped']={'packages':['com.google.android.apps.maps','app.vela','com.google.android.tts'],'relevant_uids':sorted(relevant),'active_relevant_players':0}
 folder='/sdcard/Android/data/nl.flitsmeister/files/native-warning-ownership';device('shell','mkdir','-p',folder);device('shell','touch',folder+'/'+base+'-outputs-stopped')
try:
 for key,value in window_before.items():
  if value!='1':device('shell','settings','put','global',key,'1');window_changed.append(key)
 for permission,value in before.items():
  if not value['granted']:
   assert not any(flag in value['flags'] for flag in ['USER_FIXED','POLICY_FIXED','SYSTEM_FIXED']);device('shell','pm','grant','nl.flitsmeister',permission);changed.append(permission)
 source('seed-source','integrated_restart_seed');deadline=time.monotonic()+300
 while time.monotonic()<deadline:
  try:name,line=events.get(timeout=.2)
  except queue.Empty:
   if all(p.poll() is not None for p in processes.values()):break
   continue
  if name=='seed-source' and line is None and 'restarted-source' not in processes:raise RuntimeError('Seed source ended before the native playback boundary')
  if line:
   if name in ['seed-source','restarted-source'] and line.startswith('INSTRUMENTATION_STATUS: fm_capture_detail='):source_details[name]=json.loads(line.split('fm_capture_detail=',1)[1])
   if name=='seed-source' and 'fm_capture_stage=restart_seed_ready' in line:receiver('seed-receiver')
   if name=='seed-source' and 'fm_capture_stage=restart_seed_started' in line:
    detail=source_details[name];pending=detail['pending'];assert pending['stored_started'] and pending['stored_attempt']==pending['pending_attempt'];record['seed_started']=detail
    # Deliberately kill both real processes after the source durably acknowledged native STARTED.
    for package in [receiver_package,'nl.flitsmeister']:device('shell','am','force-stop',package);assert_stopped(package)
    processes['seed-source'].wait(timeout=20)
    for thread in threads:thread.join(timeout=.1)
    path,seed=source_report('seed-source');assert not seed['probe_completed'];record['expected_killed_source_result']=str(path)
    device('pull','/sdcard/Android/data/'+receiver_package+'/files/native-warning-ownership/'+token+'-seed-receiver-progress.json',output/'seed-receiver-progress.json')
    record['seed_receiver_preferences_restored']=restore_preferences(receiver_package)
    source('restarted-source','integrated_restart_recovery',expected_pending_attempt=pending['pending_attempt'],expected_old_session=pending['provider_session'])
   if name=='restarted-source' and 'fm_capture_stage=restart_uncertainty_loaded' in line:
    loaded=source_details[name];original=record['seed_started']['pending'];assert loaded['provider_session']!=original['provider_session'] and loaded['pending_provider_session']==original['provider_session'] and loaded['pending_attempt']==original['pending_attempt'] and loaded['code']=='AUDIO_UNCERTAIN';record['reloaded_pending']=loaded;receiver('fenced-receiver')
   if name=='restarted-source' and 'fm_capture_stage=restart_fenced_event_observed' in line:
    record['fenced_native_event']=source_details[name]
    device('pull','/sdcard/Android/data/'+receiver_package+'/files/native-warning-ownership/'+token+'-fenced-receiver-progress.json',output/'fenced-receiver-progress.json')
    progress=json.loads((output/'fenced-receiver-progress.json').read_text());assert any(row['audio']=='AUDIO_UNCERTAIN' for row in progress['states']);assert not any(row['stage']=='SCHEDULER_STARTED' for row in progress['diagnostics']);assert any(row['stage']=='RECEIVER_ACCEPTED' and row.get('sequence')==record['fenced_native_event']['authority']['sequence'] for row in progress['diagnostics'])
    picture=subprocess.check_output([str(adb),'-s',serial,'exec-out','screencap','-p']);(output/'actual-fenced-receiver.png').write_bytes(picture)
    (output/'actual-fenced-activities.txt').write_text(device('shell','dumpsys','activity','activities').stdout)
    signal('fenced-receiver');processes['fenced-receiver'].wait(timeout=20)
    for thread in threads:thread.join(timeout=.1)
    receiver_report('fenced-receiver');stop_and_verify_outputs()
   if name=='restarted-source' and 'fm_capture_stage=restart_recovery_completed' in line:receiver('recovered-receiver')
   if name=='restarted-source' and 'fm_capture_stage=restart_future_completed' in line:signal('recovered-receiver')
   if name=='restarted-source' and 'fm_capture_error=' in line:raise RuntimeError('Actual restarted provider probe failed; partial evidence retained')
  if all(p.poll() is not None for p in processes.values()):break
 assert 'restarted-source' in processes and all(p.poll() is not None for p in processes.values()),'Provider restart deadline'
 for thread in threads:thread.join(timeout=2)
 path,restarted=source_report('restarted-source');assert restarted['probe_completed'] and not restarted['failure'];record['restarted_source_result']=str(path);receiver_report('recovered-receiver')
 case=restarted['scenarios']['integrated_restart_recovery'];reset=case['uncertain_output_recovery']
 for field in ['actual_window_screenshot','confirmations_screenshot']:
  remote=reset[field];assert re.fullmatch(r'/storage/emulated/0/Android/data/nl\.flitsmeister/files/probe-restart-[a-f0-9]+-(?:recovered|uncertain-confirmations)\.png',remote);device('pull',remote,output/(field+'.png'))
except Exception as error:failure=repr(error)
finally:
 for role in ['seed-receiver','fenced-receiver','recovered-receiver']:
  if role in processes and processes[role].poll() is None:signal(role)
 for name,process in processes.items():
  try:process.wait(timeout=65)
  except subprocess.TimeoutExpired:failure=(failure or '')+'; '+name+' remains live; verified cleanup pending'
 for package in inputs:
  if package=='nl.flitsmeister' and any(name.endswith('source') and process.poll() is None for name,process in processes.items()):failure=(failure or '')+'; preserve source preferences while source runner is live';continue
  device('shell','am','force-stop',package)
  try:record.setdefault('final_preferences_cleanup',{})[package]=restore_preferences(package)
  except Exception as error:failure=(failure or '')+'; preference cleanup '+repr(error)
 for permission in reversed(changed):device('shell','pm','revoke','nl.flitsmeister',permission)
 record['source_permissions_after']=permissions();record['source_permissions_restored_exactly']=record['source_permissions_after']==before
 if not record['source_permissions_restored_exactly']:failure=(failure or '')+'; permission drift'
 for key in reversed(window_changed):
  current=device('shell','settings','get','global',key).stdout.strip()
  if current!='1':failure=(failure or '')+'; independent Android setting change, preserve for review';continue
  value=window_before[key];device('shell','settings','delete' if value=='null' else 'put','global',key,*([] if value=='null' else [value]))
 record['window_settings_after']={key:device('shell','settings','get','global',key).stdout.strip() for key in window_before};record['window_settings_restored_exactly']=record['window_settings_after']==window_before
 if not record['window_settings_restored_exactly']:failure=(failure or '')+'; window settings differ'
 record['failure']=failure;record['probe_completed']=failure is None;record['process_exit_codes']={name:process.poll() for name,process in processes.items()};(output/'result.json').write_text(json.dumps(record,indent=2)+'\n');print(json.dumps({'probe_completed':failure is None,'failure':failure,'result':str(output/'result.json')}),flush=True)
if failure:raise SystemExit(1)
