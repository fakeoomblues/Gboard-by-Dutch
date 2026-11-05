# Waze shutdown attribution

## Observed failure

Genuine-AA setup run22 started Vela with the API-owned Waze provider before native first-run setup was completed. Waze PID 4870 reached foreground MainActivity, then Android recorded SIGNALED/status 9. The Android log explicitly records ShutdownManager aborting the VM and Waze sending SIGKILL to its own PID. The observer declared coverage failed; no DHU app acceptance followed.

Original APK static evidence maps NativeManager.onNativeLayerShutDown(boolean) to com.waze.ifs.ui.aa.d(). The native-layer callback first marks its engine shut down; aa.d() then stops WazeAppService, removes app tasks and kills the process. Suppressing only the final kill would not restore the stopped engine. The initiating request is still unproven. Both API expiry and original Java/native shutdown paths must remain distinguishable.

## Approved observation scope

The owner explicitly approved two read-only hooks in the existing native-warning-observer.js: NativeManager.shutDown() and onNativeLayerShutDown(boolean). They record the existing bounded Java stack plus API ownership, remaining lease, wrapper state and activity/car-session counts. The shared hook helper still calls the original exactly once and preserves its result/exception. No runner, unrelated hook or application eligibility/result was changed.

Syntax and Frida compilation passed. During run24 both methods appeared in hooks-ready, but the prior shutdown did not recur in the bounded setup observation (about 150 seconds from receiver launch). No caller/ownership payload was emitted because neither shutdown method ran. This is not evidence of a fix and does not close the Waze API-background or car acceptance gates. The run was deliberately ended before native setup/AA admission, restored its baseline and stopped its emulator with cleanupRequired=false.

Run23 used FM as Vela's provider and started Waze normally, but operator setup sequencing attempted Vela onboarding inside the Waze handoff window. It timed out before DHU acceptance. Preserve that as a setup failure, not an application pass or new crash.

Exact retained records are indexed in the private Downloads/Android-Auto-Waze-validation review folder. No physical phone was operated.

## Separate native car setup/login boundary

Run44 retained the normally initialized Waze phone process into genuine AA17,
instead of force-stopping it at the operator handoff. The previous native
“One-time setup” prompt was absent. The car client then displayed “Sign in to
Waze” and “Can't connect to Waze. Check your settings and try again.” No native
route, GPS, speech or profile pass is inferred. Further input stopped; the
session restored its state, detached observation and stopped the owned emulator
with cleanupRequired=false. Frida recorded no coverage loss. No Waze product
code, native credentials or login result was changed.

The original APK's `LoginController$LoginState.getAreCredentialsMissing()`
checks its native login-state variant. `login.s.h()` subscribes to native user
login and install/setup state; missing credentials cause the native install
request in `login.s.g()`. These static contracts explain separate setup/login
states but do not attribute the observed connection error to a patch.

An unmodified runtime control remains required before changing Waze for this
failure. The original 5.26.90.901 base APK verifies under its original signer
SHA-256 `03637f6c5d8f604e6fdb79a6ffbfa578de4e318f8da22fc6106665247f8807d7`.
The base contains no native libraries: use the original ARM64 split together
with its matching base, language and density splits, without patching or
re-signing. Exact member hashes, native-screen proof and bounded run outcome
are in the review folder's `waze-stock-control-inputs.json` and
`waze-native-car-login-run44.json`. Keep this login boundary separate from the
earlier native self-shutdown and the already validated provider integration.
