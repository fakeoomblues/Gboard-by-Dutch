# Maps Vela-style car layout implementation plan

> **For agentic workers:** Execute this connected batch inline using the executing-plans skill. Repository batching and frozen-tooling rules take precedence over per-step test or runtime cycles.

**Goal:** Make patched Maps navigation cards participate in the Android Auto host layout, using Vela's proven public-template ownership and the owner's approved speed/warning presentation.

**Architecture:** Keep Maps' native navigation engine, route/search logic and map renderer. Populate public `NavigationTemplate` routing/ETA models and `Trip` from the same actual Maps snapshot, as Vela's `ActiveNavCarScreen` does. Hide only the duplicate app-drawn guidance cards while the host owns them; adapt map/speed/warning overlays to actual surface/visible/stable callbacks.

**Tech stack:** Existing Java extension and guarded Morphe Kotlin patches; public CAL1.8.0-rc01 serialization already used in the adapter. No runtime dependency on Vela or Google-private host resources.

**References:** Owner-requested source `/Users/jneerdael/Scripts/Vela/app/src/main/java/app/vela/car/screen/ActiveNavCarScreen.kt`, `ManeuverMapper.kt`, `CarMapRenderer.kt`; Android Auto Architecture2026-11-04 blueprint sections2/5/6; `docs/contracts/navigation-indicators-v1.md`.

## Constraints and review focus

- Preserve the stock phone UI and Maps' native route/search/navigation authority.
- Preserve the owner-approved pill dimensions, typography, icon assets and right-column progress.
- Preserve real source clocks and data; GPS alone may be simulated for driving evidence.
- Unknown native maneuver/lane/junction/estimate facts remain unknown. Never use step estimates as destination ETA.
- Balance actual GUIDING navigationStarted/Ended; session replacement and late callbacks cannot revive old cards.
- Ordinary/map action strips are host-owned, bounded and icon-first. Overflow retains original callbacks, scrolling and Back.
- Visible/stable rectangles remain separate; empty/unknown rectangles cannot promise unobstructed overlay pixels.
- Surfaces resize in place across keyboard,1280x480,800x480 and1280x720; no duplicate cards or controls.
- Native map following remains a separate open defect: run146 resembles INSPECT_STEP; the selecting caller is unproved. Replacing cards alone is not evidence that it is fixed.

## Connected implementation

- [ ] Expand `NativeGuidance.Snapshot` with immutable current/next native step data and separate route/destination distance/time/zone. Decode exact26.44 path `blqy.b.a.e():blvb`: `d` current step, `i/j` current-step meters/seconds, `m` route distance, `c()` route seconds; current `ycx.S` next step. Preserve exact native maneuver/side, roundabout/exit, lane and junction data with patch-time ABI guards.
- [ ] Add the corresponding `CarProtocol` encoders for `RoutingInfo`, current/next `Step`, maneuver icon/metadata, lanes and actual destination `TravelEstimate`. Use the same step/estimate models in `Trip`; loading supplies no fabricated instruction. Validate through the real public CAL decoder/builders.
- [ ] Port Vela's browse/search/route-preview ownership as well as guidance: host-owned destination/result lists and preview content use the original Maps row/selection/Start callbacks. The owner's supplied screenshot demonstrates the action strip overlapping the app-drawn recent-destination panel; moving only the guidance card does not close this case. Preserve native Maps place/route data and refresh ordering.
- [ ] Store the latest authoritative snapshot in `NativeCarService.Session`, invalidate the host template on visible content changes, and rate-limit Trip updates to at most1Hz as Vela does. Preserve stop confirmation, exclusive ownership, reconnect and stale-session rejection.
- [ ] Update `ProjectionRenderer`/native controls to suppress only confirmed native guidance/ETA duplicates while host cards are active. Preserve original native state and restore on teardown/browsing. Expose required End/Sound/route/map actions through original callbacks and host overflow; preserve reachability.
- [ ] Feed real native GUIDING state into receiver route context independently of the phone camera hook. Adapt native map insets and the approved overlays to host visible/stable areas; retain actual safe-area/unknown handling.
- [ ] Add behavioral unit checks beside the implementation for typed current/next/lanes/ETA, absent/loading data, route-versus-step estimates,1Hz Trip pacing, duplicate suppression/restoration, stale callbacks and safe-area changes. No new runner, observer, fixture framework or maintained probe.

## Owner failure cases

- Supplied real car frame `Screenshot_2026-11-07-06-59-48-01_2de1447778c1596a724bafad8a56db3a (2).jpg`: Search/Settings/Sound/report action strip overlays the top of Maps' app-drawn recent-destination card. Treat this as a rejected layout, not an approved design input. Retain the private capture outside Git; committed documentation describes the mechanism without personal destinations.
- The directions pane must shrink/reflow automatically as the dock/music controls occupy more space, remain aligned with that dock with consistent margins, and expand again when space returns. Use Vela's actual host-template behavior for open/closed/expanded dock states; card, map framing and custom indicators update from the same surface/area state.
- Owner reports the small music overlay blocks the whole lower screen. Trace actual host overlay bounds, visible/stable rectangles and Maps drawing/insets. The full map surface continues behind a compact overlay; only affected controls/camera safe regions are reserved. Distinguish a host reservation defect from an app layout defect, and provide the requested host-project handover if needed.

## Grouped checkpoint and delivery

- [ ] Run the canonical Android checks once after the connected batch, plus relevant original-APK full selected-patch application and ABI/icon/signature verification. Reuse unchanged passing release/Python/docs evidence where valid; retain all canonical mandatory gates before release.
- [ ] Run one bounded DHU acceptance after the APK builds: compare host cards and native actions with Vela at1280x480, exercise cover/pan/keyboard/resize/Back and real Maps route progress, then the other required profiles. Existing Frida observation covers every exercised target; any required new read-only probe stays goal-scoped. Record the stop deadline first.
- [ ] Retain unchanged real screenshots, exact source/APK identities and limitations in the review folder. Stop owned runtimes, detach tracing, remove forwards and job scratch, preserve the initialized baseline. Reuse run146 genuine FM average/speed/announcement evidence unless relevant changes invalidate it.

The full emulation/provider goal stays active. UI ownership is the current priority; no additional pill gallery or repeated design approval is required.

## Current evidence (2026-11-08)

The standard-card implementation is committed at `f56b82ff6f78`; canonical checkpoint150 and original-APK application passed. Native DHU153 at1280x480 renders public host-owned current/next directions and route ETA, left-aligned at equal width, beside the approved clean native speed/limit pair. Search152 renders the host keyboard and genuine recent-destination row after its transition settles. A two-second blended search frame is not a persistent duplicate-panel defect.

DHU153 also exposes a real duplicate zoom widget. The shipped26.44 `rfi.a()` creates `zoom_widget_view` independently of the older exported `map_buttons_view_zoom_in/out` controls. Suppress that separate native widget only while both host zoom actions are usable; preserve native retirement and restore adapter-owned visibility at teardown. The connected follow-up includes behavioral tests and an original-APK guard for that separate slot. Its runtime fix is not yet accepted.

Native route-preview handoff, dock/player reflow, other sizes/input/reconnect and complete host ownership of the speed/warning widgets remain open. The DriveDeck widget extension is still a contract/handover, not implemented support. The original source-camera-following failure and the remaining provider functional gates are also open. Do not present the153 frame as full UI acceptance.
