#!/usr/bin/env python3
"""Bounded call-through Frida observation for the explicitly owned emulator only."""
import argparse,json,queue,re,signal,subprocess,time,hashlib
from pathlib import Path
import frida

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--adb',type=Path,required=True);p.add_argument('--agent',type=Path,required=True);p.add_argument('--token',required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--stop-file',type=Path,required=True)
a=p.parse_args()
if not re.fullmatch('[a-f0-9]{32}',a.token):raise SystemExit('Invalid owned tracing token')
def adb(*args):return subprocess.run([str(a.adb),'-s','emulator-5590',*map(str,args)],check=True,capture_output=True,text=True,timeout=10).stdout.strip()
if adb('emu','avd','name').splitlines()[0]!='native_warnings_ui_api34' or adb('shell','getprop','ro.build.version.sdk')!='34' or adb('shell','id','-u')!='0':raise SystemExit('Wrong owned tracing target')
forwards=adb('forward','--list').splitlines()
if not any(row.split()==['emulator-5590','tcp:28101','tcp:27283'] for row in forwards):raise SystemExit('Expected owned loopback forward is absent')
a.output.mkdir(parents=True,exist_ok=True);events=queue.Queue();sessions={};ready={};finished=set();gated=set();stopping=False;lost=False;barrier=False
source=a.agent.read_text();metadata={'frida_version':frida.__version__,'agent_sha256':hashlib.sha256(source.encode()).hexdigest(),'targets':['nl.flitsmeister','org.ungoogled.android.apps.maps','com.google.android.tts'],'attachments':[],'cleanup_errors':[]}
manager=frida.get_device_manager();device=manager.add_remote_device('127.0.0.1:28101')
log=(a.output/'events.jsonl').open('w')
def write(event):
    event['observed_host_monotonic_ns']=time.monotonic_ns()
    event['observed_host_unix_ms']=time.time_ns()//1000000
    log.write(json.dumps(event,default=str)+'\n');log.flush()
def stop(*args):
    global stopping
    stopping=True
signal.signal(signal.SIGTERM,stop);signal.signal(signal.SIGINT,stop)
def target(name):return next((x for x in metadata['targets'] if name and (name==x or name.startswith(x+':'))),None)
def marker(package,name):
    folder='/sdcard/Android/data/'+package+'/files/native-warning-integrated';adb('shell','mkdir','-p',folder);adb('shell','touch',folder+'/'+name+'-'+a.token+'.request')
def fail(reason):
    global lost
    if lost:return
    lost=True;metadata['coverage_failure']=reason;write({'event':'coverage-failed','reason':reason})
    marker('nl.flitsmeister','trace-lost');marker('org.ungoogled.android.apps.maps','cancel')
def attach(pid,name):
    if pid in sessions:return
    session=device.attach(pid);sessions[pid]=session
    def detached(reason,crash):events.put(('detach',pid,name,reason,crash))
    session.on('detached',detached)
    script=session.create_script(source,name='native-warning-observer')
    def message_received(message,data):events.put(('message',pid,name,message))
    script.on('message',message_received)
    script.load();metadata['attachments'].append({'pid':pid,'process':name,'script_loaded':True});write({'event':'attached','pid':pid,'process':name})
try:
    device.on('spawn-added',lambda spawn:events.put(('spawn',spawn.pid,spawn.identifier)))
    device.enable_spawn_gating()
    for process in device.enumerate_processes():
        if target(process.name):attach(process.pid,process.name)
    (a.output/'observer-ready').write_text('spawn observation active\n')
    deadline=time.monotonic()+240
    while not stopping and not a.stop_file.exists() and time.monotonic()<deadline:
        try:event=events.get(timeout=.1)
        except queue.Empty:continue
        kind,pid,name,*body=event
        if kind=='spawn':
            gated.add(pid)
            try:
                if target(name):attach(pid,name)
            finally:
                device.resume(pid);gated.discard(pid)
        elif kind=='message':
            message=body[0];write({'pid':pid,'process':name,'message':message})
            if message.get('type')=='error':fail('Agent script error in '+name);continue
            payload=message.get('payload',{});code=payload.get('event')
            if code=='hooks-ready':
                installed=payload.get('installed',[])
                if name=='nl.flitsmeister' and not any('WarningEndpoint!handle' in x for x in installed):fail('Required source hooks absent')
                elif name=='org.ungoogled.android.apps.maps' and not any('WarningScheduler!cancel' in x for x in installed):fail('Required receiver hooks absent')
                else:ready[name]=pid
            elif code in ['hooks-failed','observer-error','observer-budget-exhausted']:fail('Observation coverage failed in '+name+': '+str(code))
            elif code and 'Instrumentation!finish' in code:finished.add(pid)
            if not barrier and not lost and all(x in ready for x in ['nl.flitsmeister','org.ungoogled.android.apps.maps']):
                marker('nl.flitsmeister','trace-ready');barrier=True;metadata['barrier_released']=True;metadata['required_pids']=dict(ready);write({'event':'both-required-processes-hooked-before-stimuli','pids':ready})
        elif kind=='detach':
            reason,crash=body
            terminal=False
            if name=='nl.flitsmeister' and crash is None and reason=='process-terminated':
                path='/sdcard/Android/data/nl.flitsmeister/files/native-warning-integrated/trace-terminal-'+a.token+'.request'
                check=subprocess.run([str(a.adb),'-s','emulator-5590','shell','test','-f',path],capture_output=True,timeout=10)
                if check.returncode not in [0,1]:raise RuntimeError('Cannot verify source terminal marker')
                terminal=check.returncode==0
            write({'event':'detached','pid':pid,'process':name,'reason':reason,'expected_after_instrumentation_finish':pid in finished,'expected_after_successful_source_capture':terminal,'crash_present':crash is not None})
            if crash is not None:
                report=str(getattr(crash,'report',crash));(a.output/('crash-'+str(pid)+'.txt')).write_text(report[:524288]);metadata.setdefault('crashes',[]).append({'pid':pid,'truncated':len(report)>524288});fail('Observed target crash: '+name)
            if not terminal and pid not in finished and not (metadata.get('required_pids') and all(v in finished for v in metadata['required_pids'].values())) and not a.stop_file.exists():fail('Unexpected process detach: '+name+' '+str(reason))
            ready.pop(name,None)
    if time.monotonic()>=deadline and not a.stop_file.exists():fail('Bounded observer window ended')
except BaseException as error:
    metadata['error']=repr(error)
    try:fail('Observer failed')
    except Exception as nested:metadata['cleanup_errors'].append('Coverage marker: '+repr(nested))
finally:
    try:device.disable_spawn_gating()
    except Exception as error:metadata['cleanup_errors'].append('Disable spawn gating: '+repr(error))
    for pid in list(gated):
        try:device.resume(pid)
        except Exception as error:metadata['cleanup_errors'].append('Resume paused process: '+repr(error))
    for pid,session in sessions.items():
        try:session.detach()
        except frida.InvalidOperationError:pass
        except Exception as error:metadata['cleanup_errors'].append('Detach: '+repr(error))
    for package in ['nl.flitsmeister','org.ungoogled.android.apps.maps']:
        folder='/sdcard/Android/data/'+package+'/files/native-warning-integrated/'
        for prefix in ['trace-ready','trace-lost','trace-terminal','cancel']:
            try:adb('shell','rm','-f',folder+prefix+'-'+a.token+'.request')
            except Exception as error:metadata['cleanup_errors'].append('Marker removal: '+repr(error))
    metadata['coverage_lost']=lost;metadata['cleanup_required']=bool(metadata['cleanup_errors']);log.close()
    (a.output/'result.json').write_text(json.dumps(metadata,indent=2)+'\n');manager.remove_remote_device('127.0.0.1:28101')
