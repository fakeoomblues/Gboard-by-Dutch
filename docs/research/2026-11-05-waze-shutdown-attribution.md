# Waze shutdown attribution

## Observed failure

Genuine-AA setup run22 started Vela with the API-owned Waze provider before native first-run setup was completed. Waze PID 4870 reached foreground MainActivity, then Android recorded SIGNALED/status 9. The Android log explicitly records ShutdownManager aborting the VM and Waze sending SIGKILL to its own PID. The observer declared coverage failed; no DHU app acceptance followed.

Original APK static evidence maps NativeManager.onNativeLayerShutDown(boolean) to com.waze.ifs.ui.aa.d(). The native-layer callback first marks its engine shut down; aa.d() then stops WazeAppService, removes app tasks and kills the process. Suppressing only the final kill would not restore the stopped engine. The initiating request is still unproven. Both API expiry and original Java/native shutdown paths must remain distinguishable.

## Approved observation scope

The owner explicitly approved two read-only hooks in the existing native-warning-observer.js: NativeManager.shutDown() and onNativeLayerShutDown(boolean). They record the existing bounded Java stack plus API ownership, remaining lease, wrapper state and activity/car-session counts. The shared hook helper still calls the original exactly once and preserves its result/exception. No runner, unrelated hook or application eligibility/result was changed.

Syntax and Frida compilation passed. During run24 both methods appeared in hooks-ready, but the prior shutdown did not recur in the bounded setup observation (about 150 seconds from receiver launch). No caller/ownership payload was emitted because neither shutdown method ran. This is not evidence of a fix and does not close the Waze API-background or car acceptance gates. The run was deliberately ended before native setup/AA admission, restored its baseline and stopped its emulator with cleanupRequired=false.

Run23 used FM as Vela's provider and started Waze normally, but operator setup sequencing attempted Vela onboarding inside the Waze handoff window. It timed out before DHU acceptance. Preserve that as a setup failure, not an application pass or new crash.

Exact retained records are indexed in the private Downloads/Android-Auto-Waze-validation review folder. No physical phone was operated.
