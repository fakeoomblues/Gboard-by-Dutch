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

## Original signed control 45: incomplete before car selection

Control 45 installed the original signed base, ARM64, English and density
splits without modifying or re-signing them. Its normal phone setup reached
the map and displayed “Using offline maps”. Precise foreground location and
notification permissions were enabled through ordinary Android UI; background
location remained ungranted, matching the earlier patched run.

Before selecting stock Waze in the car launcher, the attached original
process 5371 terminated while backgrounded during AA setup. Android exit info
records SIGNALED/status 9 at 2026-11-05 15:15:55.834. The caller was not traced;
this is not proof of intentional shutdown or a patch-independent explanation
for the earlier car-login error. A later process 7477 was discovered during
setup. Further input stopped when the missing original-process attachment
was detected. No stock car-login, route or speech pass is claimed.

The original process used existing Frida CLI 17.9.11 against server 17.23.3,
without injected-patch hooks. The unchanged observer retained AA/TTS coverage
and reported no coverage loss; the stock application comparison remains
incomplete because its CLI session ended. An initial TTS attach timeout was
preserved separately and resolved with the existing stop-TTS startup step.
No observer, fixture or runner source was changed.

Retained network configuration reports Android's active network validated,
no HTTP proxy and no Waze hosts-file override. This does not establish that
Waze's own backend login works. The original offline-map banner remains an
unattributed observation.

The emulator's package hashes, audio, debug property, automotive role
configuration and GPS were restored to the saved snapshot; all owned
runtimes/server/forwards stopped, the temporary snapshot was removed, and
202,222,776 bytes of extracted input/system-backup scratch were deleted after
compact evidence was archived. CleanupRequired=false. Review
`waze-original-control45.json` and its screenshots/exit-info for the exact
boundary. A matched car-login control is still required: complete AA setup
first, then initialize original Waze immediately before car selection to
avoid consuming its background lifetime on host setup. The broader goal and
all previously accepted provider/Maps evidence remain unchanged.
