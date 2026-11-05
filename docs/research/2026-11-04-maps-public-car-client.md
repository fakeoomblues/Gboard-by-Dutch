# Maps public car client and Gearslip handover

## Scope and evidence

This development change retains Maps 26.43.05.1002020710's own car activity, map,
location, search and route engine. It replaces the external AppDecor boundary;
it does not copy Android Auto's host layout engine into the app. The native map
continues to draw into the public CAL Surface. Host template controls remain
host-owned. Routing stays in Maps/Vela, with Waze/Flitsmeister supplying alerts.

The input architecture is the owner's `Android Auto Architecture 2026-11-04`
package, notably `START_HERE.md`, the rendering blueprint, public-contract report,
CAL pipeline report, navigation layout blueprint and adaptation/resize reports.
It describes Android Auto **17.9.664044**, base SHA-256
`a024a3c478dd01a1aecdd5cfe9b038d86a60e8b9b930e52762a781cf6b300b05`.
The supplied Downloads APK matches this digest; use its three matching splits.
Do not mix its contracts with the separate AAOS 2024 reference or treat static
layout matrices as executed acceptance tests. The large input archive was not
duplicated or added to Git.

Maps original input SHA-256:
`81506a2c54fc9a8faca34c18af7619d9c9d26cbfbe799b66884a59ac17791004`.
All native ABI references are guarded by the patch and require a separate port
if the input changes. Decompiled sources and APKs remain private input evidence.

## Implemented client boundary

| Requirement | Implementation | Verification required |
|---|---|---|
| Own car UI code/resources | Scoped `nnd(Context)` retains the Maps context; `lui` loads `LocalCarUiEntry`. No foreign package context, AppDecor service binding or foreign theme/resource ID. | Actual candidate starts under Gearslip without full AA installed. |
| Preserve native engine | Original `GmmCarProjectionService`, activity and content attachment stay in place. | Native map, search, preview, route and stop on real inputs. |
| Truthful navigation ownership | `NativeGuidance` subscribes to `njj.bR`/`bllk`, seeds its volatile snapshot and unregisters the same listener. Only native GUIDING starts CAL navigation; CRUISING does not. | Host starts/ends ownership and reconnects while already guiding. |
| Host stop | `INavigationManager.onStopNavigation` sends the original `NavigationService` `abortcurrentsession` command; completion waits for a non-GUIDING snapshot, with a failure timeout. | A second navigation app/host stop actually ends the Maps route. |
| Public Trip | Paired native cue/next-step progress and estimate; loading has no invented steps. Device timezone is used for the calculated next-step arrival. Destination identity, destination timezone, maneuver and lanes are omitted until mapped. | Real host deserialization and cluster/Trip consumption. |
| Real editor | Native input connection remains owned by Maps; host SearchTemplate transports edits/submission. | Search input/results and keyboard resize. |
| Screen stack | Top-first TemplateInfo includes class wire values and actual input/controls parents; Back pops the same local stack. | Back, overflow, keyboard and repeated refresh under host quotas. |
| Resize/DPI | Resize the existing VirtualDisplay, update DrawingSpec/configuration and keep the activity/editor. | 1280×480, 1280×720, 800×480; DPI change while typing. |
| Safe areas | Map rendering survives unknown areas. Warning HUD requires a nonempty known clipped region; disjoint regions suppress it. | Callback order, minimized/IME, host controls and warning visibility. |
| Controls | CAL strips remain bounded; extra native actions use a real ListTemplate and original callbacks. | Scroll to every row; touch/rotary/Back and no duplicated map controls. |
| Toast | Internal native toast requests route through authenticated CAL IAppHost.showToast. | Toast is visible on the car host. |

The local layout saves native child hierarchy with the app classloader and
focusability. Controller Binders accept only the app UID. The genuine optimized
Maps path has no legacy SDK drawer/menu/search UI; their initialization and
visibility contracts are retained without inventing an active drawer. The
only advertised SDK capability is app-bar inset behavior.

Google's private demand-space microphone is **not implemented** by this port.
Its native permission/version predicate is disabled so the app does not offer a
button which cannot run. Typed native search remains available. A real public
microphone/recognition integration is a separate remaining capability; never
acknowledge the private voice transaction as though speech recognition happened.
Global host-overlay closure is not advertised as available.

## Gearslip engineer handover

The primary Gearslip checkout contains other work and was not edited. Its current
`gearslip/src/main/java/app/seb3thehacker/gearslip/host/CarAppConnection.kt`
already exposes INavigationHost, obtains INavigationManager, stores Trip and
calls onStopNavigation. Reuse those paths. It currently returns null from
`IAppHost.openMicrophone`; that cannot support an app-owned speech pipeline.

Host work must use the public client contract, not special-case the Maps package:

1. Enforce exclusive navigation ownership: stop the previous manager and observe
   completion/state before transferring ownership. A Trip update alone is not a
   route start. Clear stale Trip after stop/death and reject old session callbacks.
2. Render app Surface and host controls from one measured geometry. Emit visible
   and stable rectangles independently in app Surface coordinates. Empty means
   unknown. Do not accumulate historical stable intersections or subtract shell
   insets twice. Resize available/area callback ordering must follow the supplied
   CAL blueprint, including shrink versus growth.
3. Keep display/window px, usable bounds, dp and projection transform distinct.
   Invert the final transform for input. Driver side, dashboard side and RTL are
   independent. At 1280×480/160dpi use a wide, compact-height policy; do not
   select a wide/tall layout just because width is large.
4. Honor native CAL action, row, search, Back and list delegates once. Do not
   silently drop overflow rows. Verify last-row reachability and focus return.
5. For full stock-like host chrome, implement NavigationTemplate and
   MapWithContent as separate models. Apply the reference navigation blueprint's
   measured card overflow policy, safe areas, minimized/pan states and focus graph.
   Do not overlay a duplicate turn card on a client which already draws one.
6. If implementing microphone support, implement actual IAppHost.openMicrophone
   transport/lifetime and a real recognizer, with permission, cancellation and
   host-disconnect behavior. Returning a non-null placeholder is not support.

Protocol anchors in the supplied AA17.9 source: `INavigationHost` transactions
2=start, 3=end, 4=typed Bundleable Trip; `INavigationManager` transaction 2 is
oneway stop with an IOnDoneCallback. TemplateInfo Class values use Bundler tag 8.
These are independently decoded by the real public CAL library in unit tests.

## Acceptance ledger

The earlier signed v1.38 baseline rendered a genuine Maps car map in DriveDeck
at 1280×480. The new checkpoint06 candidate additionally rendered its native map
and an active 11-minute/3.7 km route at 1280×480/160dpi with **only the AA stub**
installed. DriveDeck received native-driven `navigationStarted`. Six GPS updates
were delivered and Maps' real `ExternalTelemetry.fix` consumer observed the new
coordinates. All four exercised processes (Maps, Waze, TTS and DriveDeck) retained
Frida coverage. The 60-second route/warning phase completed; original hashes,
volume and GPS were restored, emulator stopped, and `cleanupRequired=false`.

The review folder in Downloads contains `maps-public-client-acceptance.json` and
`screenshots/maps-public-client-1280x480-{map,navigation}.png`. Full compact logs
and trace remain in the private acceptance01 archive. This session did **not**
accept warning speech: the supplied warnings were off-route and correctly
rejected. It also exposed the pre-existing warning guidance coordinator's wrong
direct application `rJ` lookup; the shipped application exposes `rI` then its
manager's `rJ`. The connected repair and on-route warning acceptance follow.

Checkpoint09's connected repair corrects the application component accessor,
refuses an empty/unprepared voice language, and recognizes the shipped non-audio
sentinel and vibration/wearable player variants without changing media-release
fences. It also uses sparse car-display configuration and prevents inherited
phone window bounds from constraining the Surface.

Acceptance04 passed the on-route police, camera and hazard cases: **three TTS
requests, three onStart, three onDone and three COMPLETED/quiescent outcomes**,
with no extra utterance for the duplicate police event. Native route relevance
was RELEVANT, warning text was visible, and GPS-driven speed was 14 km/h. The map
filled all 1280 pixels after GPS movement; the former 1080-pixel map/200-pixel
strip was absent. Frida coverage stayed intact and cleanup restored/stopped the
owned emulator. Stream volume was zero: this is synthesis-path acceptance, not
a claim of heard physical car audio. The Downloads receipt now identifies
checkpoint09 and acceptance04.

Host stop, resize/scroll, other aspect ratios and real AA17.9 rendering still
need their planned acceptance cases. Earlier garbled DHU images are not rendering
acceptance. No physical phone installation is part of this change.

At the batch checkpoint run the canonical local verifier, patch the exact APK
through Morphe with the expected applied/failed patch list, and verify signing
and alignment. Then perform one bounded emulator acceptance session with Frida
coverage on exercised apps/hosts. Keep screenshots, source/artifact digests,
actual surface/usable geometry, callback outcomes and cleanup receipt together.
Do not describe this port as complete until these gates and capability limits
are explicitly resolved. Stop owned runtimes and remove owned build/scratch
storage after retaining compact evidence; preserve original inputs and FM's
protected phone-test candidate.

## Genuine AA17 admission findings, 2026-11-05

GitLab `main` was fetched again and remains
`483eb84b594af17c4ee441ddecf81616ac34b9fe` (v1.38.0); it is an ancestor of
the current feature head. The primary local checkout's older `main` is not
the release baseline for this feature.

The review index in `Downloads/Android-Auto-Waze-validation/README.md` now
separates DriveDeck acceptance, partial genuine-AA screenshots and unverified
cases. Earlier garbled head-unit frames and the stock Maps home screenshot
are excluded from patched-app rendering acceptance.

In the observed `aa17-waze-10` run, normal Waze phone setup completed. A settled
tap on genuine AA's Continue reached the native terms acknowledgement at
00:05:32 and advanced the preflight requirement from 1 to 3. The Welcome UI
remained visible. Requirement 3 is notification-listener access: in the exact
AA17 source, `mqh.a()` returns 3, `mqh.e()` checks `mln.a().d()`, and its
fragment is `mov`. Android's normal notification-access Settings listed Android
Auto under Not allowed. The bounded consent window ended before the access
switch was enabled. This run therefore does not accept car navigation.

The early requirement-transition lines were observed live but rolled out of
the guest log before terminal archiving. The qualified receipt records that
retention limit; the retained filtered log contains the later repeated terms
acknowledgements. Coverage remained intact and baseline restoration/cleanup
passed. Complete this normal Settings review before starting the next DHU
connection; do not force a native completion callback or change policy.

`aa17-waze-11` stopped before stimuli when Frida timed out attaching to an
already-running AA `:car` process. Its trace records the failed PID and timeout.
The next fixture cold-starts AA under the unchanged spawn observer. An attach
failure remains a failed admission gate; no app acceptance is inferred from it.

`aa17-waze-12` completed normal notification-access review before connection.
Genuine AA then logged `onAllRequirementsComplete`, `projectionAuthorized: true`
and `PREFLIGHT->CONNECTED`. Those transition lines are retained separately.
The normal 800×480/160dpi launcher rendered and its down control exposed the
separate Ungoogled Maps and Vela tiles. Screenshots in the review folder prove
that bounded launcher/scrolling observation, not navigation acceptance.

The run stopped after Waze PID 5204 terminated. The retained native log at
00:17:37.750 says `ShutdownManager` satisfied its shutdown constraints and
started shutdown. Android exit info reports SIGNALED/status 9, with no crash
report. The shipped Java chain is `NativeManager.onNativeLayerShutDown(boolean)`
→ `com.waze.ifs.ui.aa.d()` → stop service/remove tasks/kill own process. This
establishes Waze's explicit termination path; its upstream native trigger is
still unproven. Do not suppress the final kill after the native engine has
already shut down.

REA inspection of the exact tested Waze APK (SHA-256
`81c817f7f4ec1c0274fdb4ee8d424df7c986cd3403e06c86864489667fb91613`)
confirms `ConfigManager.uaOverrides()` contains the background timeout
`10800000` ms. Evidence ID:
`ev_7458b6de35f9728139e3ca2da251a46ff2100f58bb5579dca1406cef3ef442d1`.
This rules out an omitted keep-alive dependency; it does not prove the native
value was applied or which shutdown condition fired. Existing endpoint traces
showed unowned audio and SPEAKER_UNAVAILABLE claim replies during fixture
startup. Establish whether a managed receiving session owns the source when
tracing the upstream shutdown request. The entire app acceptance run remains
failed; restoration, stopped runtimes and cleanup were verified.

The connected product correction wraps the original
`com.waze.car_lib.platform.af.b()` car-service keep-alive result with
`NavigationBroadcasts.keepAliveAfterCarSession(boolean)`. An initialized,
enabled navigation feed retains the native engine when the host releases the
car service. Disabling broadcasting preserves the original native decision.
The original body and its boolean result remain in place. Manual native shutdown
and the phone idle timeout are unchanged; this does not suppress the final
process kill after native shutdown.

`waze-car-session-checkpoint-01` passed canonical verification, with 760 Android
test cases, zero failures/errors and 33 skips. Both new bytecode tests ran
against the supplied original APK. Runtime-toggle tests cover broadcasting on,
off, re-enabled and uninitialized while retaining a native true result. Morphe
applied all 21 selected patches to the original APKM, followed by signature and
alignment verification. The temporary checkout was removed. Bundle SHA-256:
`6822821ddae0545f3a8adafc1184a8d08ebc30e8fa716cca7c8c0f5827fe5374`;
Waze APK SHA-256:
`44a1141a98e03ee446ff74314a53c932087bf756e551d24224eb91927faeb1a3`.
The corrected candidate still requires genuine-DHU runtime acceptance.


## Requested owned-provider shutdown, 2026-11-05

The user requested a 10-second Maps/Vela health check and native shutdown after
15 seconds without it, exclusively for engines started by the API. The existing
client already polls at 10 seconds. The new batch changes the lease from
45 seconds to 15 and adds process-local start ownership plus provider-native
teardown. A private initialization provider registers phone lifecycle observation
before Activities start. The public CAL Session lifecycle protects user takeover
on the car without treating a metadata-only service bind as manual use. A native
service already starting is preserved even before its readiness flag becomes true.
Ownership is not persisted across process death or reboot. Deferred native
shutdown rechecks the latest heartbeat and takeover before acting.

The Waze backend uses NativeManager's normal shutdown; FM stops its existing base
service and its reconstructed normal cleanup. Shared status reports ownership and
both intervals. Tests cover expiration, renewal, manual starts, takeover, wrapper
destruction and late callbacks. Canonical verification and actual four-app patch
application remain required for this new batch.

Run 13 with the preceding car keep-alive candidate still ended in Waze's own
ShutdownManager path. The guard did not resolve that termination. Patched Maps
rendered the Amsterdam map in genuine AA at 800x480, but Search did not open its
editor through the exercised DHU taps. Neither routing nor speech nor the
remaining profiles were accepted in that run. The retained qualified receipt is
native-aa17-runtime-13.json in the Downloads review folder; cleanup passed.

Run 15 proved the API-only Waze PID terminated about 821 ms after the recorded
lease deadline. A manually used Waze also survived an expired lease. The run
then exposed an unhealthy restart: Android resurrected the private wrapper
without a receiver or fresh heartbeat because it returned START_STICKY. The
connected correction returns START_NOT_STICKY and removes/stops the wrapper
before invoking native shutdown. A pending native initializer retains foreground
eligibility until its final ownership/health check. Regression assertions cover
that ordering and deferred readiness. Native expiry remains unaccepted until
the no-restart and FM cases pass. Run 15 restored and stopped the emulator; its
trace coverage and cleanup passed.

The corrected checkpoint02 passed canonical verification and all four actual
APK patch/sign/alignment gates. Native API-only expiry then passed all four
receiver/provider pairs: Maps/Waze, Maps/FM, Vela/Waze and Vela/FM. Waze quit
without wrapper resurrection; FM stopped its native base service and wrapper.
Both bounded sessions retained Frida coverage, restored baseline state and
stopped the owned emulator. The Downloads review folder contains the signed
engineering MPP and owned-provider-shutdown-validation.json. FM native manual
takeover remains unaccepted because first-run onboarding independently stopped
its driving service; a fully initialized manual session must exercise that gate.
The original AA navigation, speech, input and profile acceptance gates stay open.

## Search and public pan correction

The current original APK still contains the configured search/zoom IDs. Static
DEX inspection locates main search in rhm.e, destination input in qax.a and zoom
in rcq.d/ref.a. Native click owners contain other interactive descendants. The
adapter previously descended into those children before considering a clickable
named container; it now preserves that owner and ignores invisible descendant
candidates. The destination_input_search_box resource is also recognized.
Search continues through the original native handler and input connection.

The NavigationTemplate also now carries the public PanModeDelegateImpl /
IPanModeListener callback. Leaving host pan mode finishes a pending native drag.
The capability is bound to the authenticated UID and session, and completes
rejected stale/foreign callbacks. The supplied AA17 source confirms transaction
2, boolean then IOnDoneCallback, and the mStub serialized field. Tests decode the
real library delegate, exercise both transitions, and cover capability rejection
and nested native search actions. Runtime search/keyboard/input acceptance is
still required for this batch. No observer or validation framework was changed.

Run 18 accepted native Search action discovery, the public AA keyboard, typed
query dam, native Dam/Amsterdam preview, route start and admitted moving GPS
on genuine AA17 at active head-unit geometry 1280x480/160dpi. The app Surface
is 1200x480 beside the 80px dock. Encoded screenshots remain 1280x720 with
120px top/bottom margins; console input uses active viewport coordinates.
The review folder links each state and records exact candidate hashes.

The planned 800x480 reconnect showed an idle car UI, while NavigationService
remained foreground. Route cancellation is therefore not established; native
guidance UI restoration needs diagnosis. Optional external speed/HUD and native
speed/limit views also duplicate/overlap at lower right. Silent-mode self-test
produced a native warning toast; semantic card and speech were not accepted.
Frida coverage remained intact; the original baseline was restored, the emulator
stopped and cleanupRequired=false. Preserve the passing input/route evidence.

## Native UI saved-state correction

The original bemm.m(int, boolean) STOP transition calls J.e(Bundle), stores
the result in n, and sends projection callback 12. The adapter previously
acknowledged that callback without retaining its payload and passed an empty
Bundle to bemm.o on each fresh presentation. The connected correction retains
native saved-instance state per authenticated host UID with a bounded six-host
in-memory store and presentation epochs. Retired callbacks cannot overwrite
the replacement; bundles are copied and never written to disk. The native
activity receives its own saved state on the next creation.

Close now performs native STOP, captures n synchronously before native DESTROY
can clear it, then completes original teardown. Callback 12 also accepts normal
and one-way parcels. No destination is replayed and no route or voice policy
is fabricated. The exact input ABI includes n:Bundle. Regression tests cover
UID isolation, bounded retention, late callbacks, copying and native teardown
ordering.

Genuine-AA runtime19 rejected this candidate as a guidance-restoration fix:
the pre-disconnect guidance screen became idle after the 800x480 reconnect,
including after a fresh GPS update. Native NavigationService remained foreground.
The existing Frida observer retained coverage; the terminal run restored the
baseline, stopped its emulator and reported cleanupRequired=false. Preserve
this failed result and its screenshots in the Downloads review index. A
foreground service alone does not establish a GUIDING native snapshot; trace
native screen/intent selection before another implementation batch.

Focused original-APK inspection then established a separate initialization
contract: `toi.c(Bundle)` passes the state to `qtd.c`, implemented by
`qsx.c(Intent, Uri, Bundle)`. Only a null Bundle processes the initial intent;
every non-null Bundle restores postponed intents and skips that initial routing.
The adapter had manufactured an empty Bundle even for a fresh session. It now
passes null when no genuine saved state exists and copies only actual restored
state, with Maps' class loader. The regression covers fresh versus restored
state and copy isolation. This is a native contract correction; genuine-AA
guidance restoration still requires acceptance and is not claimed fixed.

Genuine-AA run20 repeated the planned reconnect with checkpoint12. Guidance
visibly started at 1280x480, and the built-in native car-service dump confirmed
`navigationMode: GUIDED_NAV`. After reconnecting at 800x480 with current GPS,
the settled UI was idle and the same native dump reported `FREE_NAV`. This
narrows the problem to native car mode/lifecycle state rather than only the
visibility of a guidance card; it does not independently prove navcore stopped.
Original activity identity changed, both intent routers had zero postponed
intents and no outstanding intent blocks, and Frida retained coverage. The run
restored its baseline and stopped its emulator with cleanupRequired=false.
Preserve these passing route and failed reconnect receipts; do not repeat
checkpoint12 without a relevant product change.

The run20 disconnected-service dump contains only Room invalidation service;
native NavigationService and car services are absent. On reconnect the native
car service is recreated in FREE_NAV. Trace native navigation teardown and
handover rather than assuming that a retained foreground service proves
route continuity. No additional runtime session is justified for the unchanged
checkpoint12 candidate.

## Reuse Maps' native speed widget

The owner rejected a second speed display. Original APK inspection identifies
`azqb.G(float)` as the shared native speed setter and `azqb.H(int, ciqt)`
as its speed-limit setter. Both update `ahvw`; the genuine car UI constructs
that model through `rec`/`ahwb` inside `map_buttons_view_speed_limit`.
`G` stores raw metres/second in `ahvw.n` before converting to the displayed
integer `m`. `H` owns units, limit `l`, repaint and speed-dependent styling.

The connected change substitutes fresh selected-provider values in those
existing setters. Speed substitution happens after native conversion and
before its equality check, preserving raw `n`, native units and integer
conversion. Limits use the incoming/native units. Original inputs are retained
per weakly referenced widget; provider updates, expiry, disable and source
changes replay those native inputs through the original setters. A repaint
guard prevents these refreshes from renewing native observation timestamps or
recapturing substituted values as native data. The original attach listener
clears `ahvy.e` on detach; a guarded binding excludes retired native widgets.

The supplementary view no longer draws a current-speed/limit pair or hides
Maps' own widget. It contains only warnings and observed average-zone progress
and has no visible content without those extras. Car extras reserve the native
speed/watermark region; phone extras sit above the existing native speed slot.
Regression checks cover units, measured zero, per-field expiry, source changes,
disable/fallback, repaint freshness and the absence of a second speed pair.
Genuine AA pixel and native-value acceptance for this batch remains required.
The independently observed guidance-reconnect failure remains open.

Checkpoint13 passed the canonical build/regression checks and patched/signed
the actual Maps APK with its expected patch selection. Genuine-AA run21 at
1280x480 showed one native speed/limit control and an unobscured watermark.
Advancing GPS coordinates at 4 m/s produced 14 km/h in that control. The native
car dump contained no supplementary CompactSpeedView. The actual native canvas
was 1190x460 at 160dpi; DHU encoded the 1280x480 active head unit inside a
1280x720 PNG with 120px vertical margins.

The planned session ended before a small-profile transition. This is a wide
rendering/moving-GPS pass, not full native-provider substitution acceptance:
independently differing provider/native values and runtime source-switch/expiry
combinations remain open. Search opened recent destinations but its repeated
action did not open the keyboard in this run, so route/input acceptance was
not obtained. Native attach and Frida coverage remained intact, the baseline
was restored and the owned emulator stopped; cleanupRequired=false. Keep the
existing reconnect and remaining app/profile/voice gates open.

## Native Search owner and editor-focus lifetime

The input batch now visits native children from front to back when matching
controls, so a destination panel's Search owner replaces the older map Search
when both remain in the view tree. Old callback identities remain fenced.
Editor discovery uses logical native visibility, accounting only for visibility
hidden by the adapter; original hidden ancestors and disabled/detached editors
are still respected. Adapter-owned hiding is restored while a native editor
is active, including during the originating click, so re-hiding its ancestor
cannot immediately clear focus or invalidate asynchronous IME work. Once the
editor disappears, normal duplicate-control hiding resumes.

Checkpoint16 passed the canonical Android component and real Maps patch/sign
checks. The focus-lifetime refinement in checkpoint17 reran the changed
maps-gearslip component and actual APK patch/sign checks, reusing unchanged
Android module evidence from checkpoint16. Regression checks cover frontmost
Search selection, retained native editor focus and original hidden ancestors.
The final full verification remains incomplete at checkpoint15's environmental
Node/lsof cleanup failure. Genuine-AA Search/keyboard/route acceptance of this
new input batch remains open; no successful runtime claim is inferred from
these build/regression results.


### Genuine-AA input acceptance of the editor batch

Run28 exercised checkpoint17's signed Maps APK (SHA-256
`c6998d826d01cfc30e899f61d07d6d6e6ebeb963a41ed5f6d35a086e885f0ae4`)
on genuine AA17/DHU at 1280x480/160dpi. The native Search action opened the
AA keyboard, accepted `dam`, produced native results and a route preview, and
Start reached native `GUIDED_NAV`. Advancing GPS coordinates rendered moving
guidance with one native speed/limit widget. This accepts the editor/input
correction; it does not complete every navigation/profile/alerting gate.

The normal AA welcome task had previously been placed behind notification
settings. Focusing that already-launched task and clicking its real Continue
button completed native consent without changing permissions or preflight
state. Run26/27 setup failures remain retained separately.

Standalone Waze still displayed its native one-time phone-setup gate. The
subsequent reconnect attempt hit a DHU quit timeout, so the enclosing run remains
failed for Waze/reconnect acceptance. No Frida coverage failure or Waze shutdown
hook event was recorded. Baseline restoration, DHU/emulator stop and local
cleanup passed. No physical phone was touched. Compact screenshots, exact
artifacts, native dumps and limitations are in the Downloads review folder's
`maps-aa17-native-editor-run28.json` and README. The full local verifier remains
incomplete at the previously retained Node/lsof environment failure.
