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
