"""Run the real watcher against fixture Frida/ADB events; never contact a device."""
import json
from pathlib import Path
import queue
import runpy
import shutil
import subprocess
import sys
import tempfile
import types
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
WATCHER = ROOT / 'scripts/navigation-alerts/frida/observe_owned_processes.py'


class SelectedReceiverTest(unittest.TestCase):
    def run_watcher(self, receiver='app.vela', fault=None, contract='ownership', provider='nl.flitsmeister', additional_provider=None, additional_host=None):
        with tempfile.TemporaryDirectory(prefix='frida-fixture-') as directory:
            root = Path(directory)
            agent = root / 'agent.js'
            agent.write_text('fixture-only')
            output, stop = root / 'trace', root / 'stop'
            sessions, calls, attached, resumed = {}, [], [], []
            original_queue = queue.Queue
            names = {101: provider, 102: receiver, 103: 'com.google.android.tts'}
            if additional_provider: names[105] = additional_provider
            if additional_host: names[106] = additional_host
            other = 'org.ungoogled.android.apps.maps' if receiver == 'app.vela' else 'app.vela'

            class Events(original_queue):
                terminal = False

                def get(self, *args, **kwargs):
                    if self.empty():
                        if self.terminal:
                            stop.write_text('fixture complete')
                            raise queue.Empty
                        self.terminal = True
                        if fault in ['detach', 'crash', 'requested-stop', 'requested-stop-crash', 'host-detach', 'host-stop', 'host-crash']:
                            crash = types.SimpleNamespace(report='fixture-native-crash') if fault in ['crash', 'requested-stop-crash', 'host-crash'] else None
                            sessions[106 if fault.startswith('host-') else 102].callback('process-terminated', crash)
                        else:
                            for pid in [101, 102]:
                                sessions[pid].script.callback({'type': 'send', 'payload': {'event': 'android.app.Instrumentation!finish(int,android.os.Bundle):enter'}}, None)
                                sessions[pid].callback('process-terminated', None)
                        self.put(('spawn', 999, 'unrelated.fixture'))
                    return super().get(block=False)

            events = Events()

            class Script:
                def __init__(self, pid):
                    self.pid = pid

                def on(self, event, callback):
                    self.callback = callback

                def load(self):
                    symbols = ['WarningEndpoint!handle('] if self.pid == 101 or self.pid == 105 and fault != 'additional-missing' else []
                    if self.pid == 105 and additional_provider == 'com.waze':
                        symbols += ['NativeWarnings!prepareControllers(', 'NativeWarnings!repository(', 'NativeWarnings!sound(']
                    if self.pid == 101 and provider == 'com.waze' and fault != 'missing-source':
                        symbols += ['NativeWarnings!prepareControllers(', 'NativeWarnings!repository(', 'NativeWarnings!sound(']
                    if self.pid == 102:
                        symbols = ['WarningScheduler!cancel(']
                        if receiver == 'app.vela' and fault != 'missing':
                            symbols += ['VelaWarningSpeaker!dispatch(', 'VelaWarningSpeaker!beforeSpeech(', 'VelaWarningSpeaker!finish(', 'VelaWarningSpeaker!utteranceStopped(']
                    if self.pid == 106 and fault != 'host-missing':
                        symbols = ['java.lang.Thread!dispatchUncaughtException(', 'android.app.Instrumentation!callActivityOnResume(']
                    package = other if fault == 'package' and self.pid == 102 else names[self.pid]
                    self.callback({'type': 'send', 'payload': {'event': 'hooks-ready', 'package': package, 'installed': symbols}}, None)

            class Session:
                def __init__(self, pid):
                    self.pid = pid

                def on(self, event, callback):
                    self.callback = callback

                def create_script(self, *args, **kwargs):
                    self.script = Script(self.pid)
                    return self.script

                def detach(self):
                    pass

            class Device:
                def on(self, *args):
                    pass

                def enable_spawn_gating(self):
                    pass

                def disable_spawn_gating(self):
                    pass

                def enumerate_processes(self):
                    return [types.SimpleNamespace(pid=pid, name=name) for pid, name in names.items()] + [types.SimpleNamespace(pid=104, name=other)]

                def attach(self, pid):
                    attached.append(pid)
                    sessions[pid] = Session(pid)
                    return sessions[pid]

                def resume(self, pid):
                    resumed.append(pid)

            device = Device()
            manager = types.SimpleNamespace(add_remote_device=lambda _: device, remove_remote_device=lambda _: None)
            fake_frida = types.SimpleNamespace(__version__='fixture-only', get_device_manager=lambda: manager, InvalidOperationError=RuntimeError)

            def adb(command, **kwargs):
                args = command[3:]
                calls.append(args)
                result = ''
                code = 0
                if args == ['emu', 'avd', 'name']:
                    result = 'native_warnings_ui_api34\nOK'
                elif args[:2] == ['shell', 'getprop']:
                    result = '34'
                elif args == ['shell', 'id', '-u']:
                    result = '0'
                elif args == ['forward', '--list']:
                    result = 'emulator-5590 tcp:28101 tcp:27283'
                elif args[:3] == ['shell', 'test', '-f']:
                    code = 0 if fault == 'crash' or ('/trace-stop-' in args[-1] and fault in ['requested-stop', 'requested-stop-crash', 'host-stop', 'host-crash']) else 1
                return subprocess.CompletedProcess(command, code, result, '')

            argv = [str(WATCHER), '--adb', '/fixture/adb', '--agent', str(agent), '--token', 'a' * 32, '--output', str(output), '--stop-file', str(stop), '--provider', provider, '--receiver', receiver, '--receiver-cancel-contract', contract]
            if additional_provider: argv += ['--additional-provider', additional_provider]
            if additional_host: argv += ['--additional-host', additional_host]
            with patch.dict(sys.modules, {'frida': fake_frida}), patch.object(sys, 'argv', argv), patch.object(subprocess, 'run', side_effect=adb), patch.object(queue, 'Queue', return_value=events), patch('signal.signal'):
                runpy.run_path(str(WATCHER), run_name='__main__')
            record = json.loads((output / 'result.json').read_text())
            log = [json.loads(line) for line in (output / 'events.jsonl').read_text().splitlines()]
            self.assertEqual([101, 102, 103] + ([105] if additional_provider else []) + ([106] if additional_host else []), attached, 'Only selected sources, receiver and host may be attached')
            if not record['coverage_lost']:
                self.assertIn(999, resumed, 'Unrelated gated spawns must always resume')
            self.assertFalse(record['cleanup_required'])
            touches = [call[-1] for call in calls if call[:2] == ['shell', 'touch']]
            return record, log, touches

    def test_vela_requires_and_releases_actual_selected_hooks(self):
        record, log, _ = self.run_watcher()
        self.assertTrue(record['barrier_released'])
        self.assertEqual({'nl.flitsmeister', 'app.vela'}, set(record['required_pids']))
        self.assertFalse(record['coverage_lost'])

    def test_waze_requires_native_capture_and_marks_only_selected_source(self):
        record, _, touches = self.run_watcher(provider='com.waze')
        self.assertTrue(record['barrier_released'])
        self.assertEqual({'com.waze', 'app.vela'}, set(record['required_pids']))
        self.assertFalse(record['coverage_lost'])
        self.assertFalse(any('/nl.flitsmeister/' in path for path in touches))
        record, _, touches = self.run_watcher(provider='com.waze', fault='missing-source')
        self.assertTrue(record['coverage_lost'])
        self.assertNotIn('barrier_released', record)
        self.assertTrue(any('/com.waze/' in path and 'trace-lost-' in path for path in touches))

    def test_both_native_producers_are_observed_for_source_switches(self):
        record, _, touches = self.run_watcher(provider='com.waze', additional_provider='nl.flitsmeister')
        self.assertFalse(record['coverage_lost'])
        self.assertIn('com.waze', record['ready_pids'])
        self.assertIn('nl.flitsmeister', record['ready_pids'])
        self.assertTrue(any('/nl.flitsmeister/' in path and 'trace-ready-' in path for path in touches))
        record, _, _ = self.run_watcher(provider='com.waze', additional_provider='nl.flitsmeister', fault='additional-missing')
        self.assertTrue(record['coverage_lost'])

    def test_maps_selected_behavior_is_preserved(self):
        record, _, _ = self.run_watcher('org.ungoogled.android.apps.maps', contract='integrated')
        self.assertTrue(record['barrier_released'])
        self.assertFalse(record['coverage_lost'])

    def test_real_car_hosts_require_exception_and_activity_observation(self):
        for host in ['app.johninnl.drivedeck.dev', 'com.google.android.projection.gearhead']:
            with self.subTest(host=host):
                record, _, _ = self.run_watcher(additional_host=host)
                self.assertFalse(record['coverage_lost'])
                self.assertEqual(106, record['ready_pids'][host])
                record, _, _ = self.run_watcher(additional_host=host, fault='host-missing')
                self.assertTrue(record['coverage_lost'])

    def test_requested_stop_is_distinct_from_success_and_cannot_hide_a_crash(self):
        record, log, _ = self.run_watcher(fault='requested-stop')
        self.assertFalse(record['coverage_lost'])
        stopped = next(row for row in log if row.get('event') == 'detached')
        self.assertTrue(stopped['expected_after_requested_stop'])
        self.assertFalse(stopped['expected_after_successful_receiver_capture'])
        record, _, _ = self.run_watcher(fault='requested-stop-crash')
        self.assertTrue(record['coverage_lost'])
        self.assertEqual(102, record['crashes'][0]['pid'])

    def test_car_host_exit_and_crash_remain_fail_closed_except_requested_stop(self):
        host = 'com.google.android.projection.gearhead'
        for fault in ['host-detach', 'host-crash', 'host-stop']:
            with self.subTest(fault=fault):
                record, _, _ = self.run_watcher(additional_host=host, fault=fault)
                self.assertEqual(fault != 'host-stop', record['coverage_lost'])

    def test_incomplete_vela_or_wrong_package_never_releases_stimuli(self):
        for fault in ['missing', 'package']:
            with self.subTest(fault=fault):
                record, _, touches = self.run_watcher(fault=fault)
                self.assertTrue(record['coverage_lost'])
                self.assertNotIn('barrier_released', record)
                self.assertTrue(any('/app.vela/files/native-warning-ownership/' in path for path in touches))
                self.assertFalse(any('/org.ungoogled.android.apps.maps/' in path for path in touches))

    def test_unexpected_exit_cancels_only_selected_receiver(self):
        record, _, touches = self.run_watcher(fault='detach')
        self.assertTrue(record['coverage_lost'])
        self.assertIn('Unexpected process detach', record['coverage_failure'])
        self.assertTrue(any(path.endswith('-seed-receiver-stop') for path in touches))

    def test_crash_cannot_be_hidden_by_terminal_marker(self):
        record, log, _ = self.run_watcher(fault='crash')
        self.assertTrue(record['coverage_lost'])
        self.assertEqual(102, record['crashes'][0]['pid'])
        self.assertTrue(next(row for row in log if row.get('event') == 'detached')['crash_present'])

    @unittest.skipUnless(shutil.which('node'), 'Node required for Java binding readiness check')
    def test_partial_application_binding_waits_for_loaded_apk(self):
        source = ROOT / 'scripts/navigation-alerts/frida/native-warning-observer.js'
        script = r"""
const fs=require('fs'),vm=require('vm'),assert=require('assert');
const source=fs.readFileSync(process.argv[1],'utf8').replace("import Java from 'frida-java-bridge';",'');
const events=[],timers=[],method={call(){}};
const context={Process:{id:1},send:e=>events.push(e),setImmediate:f=>f(),setTimeout:(f,delay)=>timers.push({f,delay}),
 Java:{performNow:f=>f(),classFactory:{},use:name=>name==='java.lang.Thread'?{dispatchUncaughtException:{overload:()=>method}}:{currentActivityThread:()=>({mBoundApplication:{value:{info:{value:null}}}})}}};
vm.createContext(context);vm.runInContext(source,context);
assert.strictEqual(timers.length,1);assert.strictEqual(timers[0].delay,25);
assert(!events.some(e=>e.event==='hooks-failed'||e.event==='hooks-ready'));
process.stdout.write('partial binding deferred');
"""
        result = subprocess.run(['node', '-e', script, str(source)], capture_output=True, text=True)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn('partial binding deferred', result.stdout)

    @unittest.skipUnless(shutil.which('node'), 'Node required for actual JavaScript wrapper check')
    def test_actual_javascript_wrappers_preserve_result_exception_and_single_original_call(self):
        source = ROOT / 'scripts/navigation-alerts/frida/native-warning-observer.js'
        script = r'''
const fs=require('fs'),vm=require('vm'),assert=require('assert');
const source=fs.readFileSync(process.argv[1],'utf8').replace("import Java from 'frida-java-bridge';",'').split('Java.performNow')[0];
const methods={},sentinel={},failure=new Error('original fixture failure'),events=[];
const context={Process:{id:1},send:e=>events.push(e),Java:{use:name=>name==='android.os.SystemClock'?{elapsedRealtime:()=>1}:methods}};
vm.createContext(context);vm.runInContext(source,context);
for(const [name,result,throws] of [['false',false,false],['null',null,false],['object',sentinel,false],['throw',failure,true],['observation','unchanged',false]]){
 let calls=0;const original={call(){calls++;if(throws)throw result;return result;}};
 methods[name]={overload:()=>original};
 context.hook('Fixture',name,[],()=>{if(name==='observation')throw new Error('observer fixture');return {};},()=>({}));
 if(throws){try{original.implementation.call({});assert.fail('original exception swallowed');}catch(error){assert.strictEqual(error,failure);}}
 else assert.strictEqual(original.implementation.call({}),result);
 assert.strictEqual(calls,1);
}
assert(events.some(e=>e.event==='original-exception'));assert(events.some(e=>e.event==='observer-error'));
process.stdout.write('five call-through contracts passed');
'''
        result = subprocess.run(['node', '-e', script, str(source)], capture_output=True, text=True)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn('five call-through contracts passed', result.stdout)


if __name__ == '__main__':
    unittest.main()
