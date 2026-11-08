# Host-owned navigation widgets v1

Status: implementation contract and DriveDeck handover. The Maps standard-template port is in progress. This document does not claim that the widget extension is implemented or accepted.

## Ownership and platform boundary

Directions, ETA, destination/search/preview panels and sidebar buttons use the public Car App Library models. Android Auto or DriveDeck measures, positions and adapts those controls. Maps supplies genuine data and original callbacks.

The pinned CAL1.8.0-rc01 / Car App API8 model has no speedometer or arbitrary custom-warning-pill field. Vela's `CarMapRenderer.drawSpeed` paints its badge on the app surface using host-reported safe bounds. That is adaptive client drawing, not a host-owned template widget.

The owner now requires the speedometer and long warning/average bubble to be owned by the host too, retaining the approved design. DriveDeck therefore needs this additional widget contract. Its current support for standard navigation cards alone does not fulfill that requirement. An unmodified Android Auto host cannot implement this extension merely because the client sends extra fields. Keep that platform limit explicit; do not silently label a client-painted fallback fully templated.

## Generic capability negotiation

Expose a per-CarAppConnection host service through `ICarHost.getHost`:

- Service key: `nl.johninnl.navigation.widgets.v1`.
- Binder descriptor: `nl.johninnl.navigation.widgets.INavigationWidgetsHost`.
- Transaction1: capabilities, returning protocol1, supported widget types and the current surface epoch.
- Transaction2: submit a primitive `Bundle` widget snapshot; reply with accepted types and layout/ownership token.
- Transaction3: clear this app session's widgets, idempotently.

Supported types are `SPEED`, `WARNING` and `AVERAGE_ZONE`. Advertise only types that the host can actually render. The interface is generic; never special-case the Maps package. A future Vela client can use the same contract.

Bind the service to the authenticated app UID and CarAppConnection generation. Derive caller identity from Binder and the existing handshake, not bundle-supplied package names. Reject replaced connections, wrong UIDs, stale epochs and nonmonotonic sequences. An old app cannot clear the replacement's widgets. Clear on connection death/session teardown, including a missing client clear command.

No explicit capability/ownership acknowledgement means no host-widget ownership. The client must not hide its only speed/alert display in that state. On acknowledgement, suppress the matching app-drawn widget only; never leave permanent duplicates. A host failure releases ownership and makes the client restore its responsive supported fallback.

## Snapshot fields

Top level:

| Field | Meaning |
| --- | --- |
| `protocol_version` | Integer1. |
| `app_session` | Nonempty client UI-session identity, independent of provider session. |
| `sequence` | Increasing within that UI session. |
| `surface_epoch` | Echo of the host's current surface generation. |
| `style_version` | `navigation-indicators-v1`; use the approved assets/typography. |
| `speed`, `warning`, `average_zone` | Optional primitive child bundles. Absence clears that widget for this snapshot. |

Speed bundle:

- Separate current/limit known flags and canonical km/h values; missing values never become0 or130.
- Original observation clocks for current speed and limit. A redraw is not an observation.
- Native speed-display unit independently from the native distance-display unit.
- Retain actual freshness: stale current speed becomes unknown; stale limits are withheld.

Warning bundle:

- Original provider session, warning identity, revision, native type/subtype, category, priority, phase, indicator kind, source text/language, observation clock and state expiry.
- Fresh qualified distance/range/trigger values and each field's own clock/estimated/origin metadata, following `navigation-indicators-v1.md`.
- Original icon bytes or a standard CarIcon carrying the unchanged approved asset. No host tint/recolor/background edit.
- Qualified responder relationship only; no inferred direction or arrow. Behind/Van achteren requires fresh source evidence.
- This is display data. It never grants speech, creates announcements, changes category settings or extends source deadlines.

Average-zone bundle:

- Native zone identity/session, phase, original observation start/update/expiry clocks.
- Qualified numeric average plus its partial/estimated flag, known limit, progress and remaining distance. Unknown values remain unknown.
- Approach distance and within-zone remaining distance are distinct. Never synthesize progress from an approach threshold or use entry-point0m as remaining distance.
- Native exit/removal/session stop clears the panel. Preserve the source's active/left state during layout changes.

This version assumes local Android Binder: producer, receiver and host share the device elapsed-realtime clock. Remote-device transports need a separate clock contract; never assume their elapsed clocks agree.

## Host layout requirements

- Place cards, speed/alert widgets, dock/player and action buttons in one measured layout, using actual bounds and display density.
- The directions pane shrinks/reflows when the dock occupies space and expands when it closes, keeping Vela's consistent margins and alignment.
- Follow the supplied AA blueprint's compact-height overflow policy: reclaim junction/next-step/lane/cue space as needed while preserving the primary maneuver and reachable controls. A fixed-height card plus clipping is insufficient.
- Keep the map surface full size behind a compact player. Reserve its real occupied bounds for layout/input; do not turn a small overlay into a blanket lower-screen blocker.
- Use a consistent speed/limit pair and fixed outer warning-pill size across alert types. Keep the icon large and the approach bar in the right column under the distance, aligned with the speed widget.
- Keep remaining zone distance visible. Average, speed and distance units are separate data choices, not a shared imperial boolean.
- Sidebar controls remain standard ActionStrip/MapActionStrip items with native callbacks. Overflow is a real scrollable ListTemplate with Back and focus return.
- Report visible/stable areas separately in surface coordinates after layout. Do not promise covered pixels by resetting an overconstrained visible rectangle to the full surface; use the documented unknown state.
- Widget ownership belongs to the active CarAppStage/split-map pane. Weather or another full-screen app must not inherit stale Maps widgets.

## DriveDeck integration points

The existing Gearslip-folder checkout is currently read-only for this task.

1. `gearslip/src/main/java/app/seb3thehacker/gearslip/host/CarAppConnection.kt`: register the optional host service; validate UID/generation/epoch/sequence; own immutable widget state and death/clear handling.
2. `gearslip/src/main/java/app/seb3thehacker/gearslip/car/CarAppStage.kt`: render supported widgets in the same host composition as cards, buttons and the player; pass actual measured cover bounds.
3. `gearslip/src/main/java/app/seb3thehacker/gearslip/car/TemplateChrome.kt` or a focused `NavigationWidgetChrome.kt`: reuse Vela/approved widget appearance, with host-owned measurement, responsive placement and expiry-to-unknown/clear behavior.
4. `gearslip/src/main/java/app/seb3thehacker/gearslip/car/CarAppOverlay.kt`: preserve compact versus expanded player reservations and control capacity.
5. `ChromeCover.visibleWithin`: review the existing less-than-quarter-size fallback, which restores full width/height even when those pixels are occupied. A guaranteed-safe rectangle must not include known covers.

Maps client integration follows acknowledgement: publish qualified receiver snapshots, then suppress only the accepted native speed card/custom bubble inside the owned car decor. Restore on disconnect. The stock phone UI is unchanged. Do not remove the native watermark or implement another always-visible speed pair.

## Required acceptance evidence

- Original owner case: recent-destination card stays clear of Search/Settings/Sound/report controls.
- At1280x480, open/close/expand the music dock during guidance. Pane, widgets and controls reflow together; the player stays compact and every button remains usable.
- Repeat at800x480 and1280x720 with Back, scrolling, rotary/touch and keyboard/surface replacement.
- Show the unchanged warning icon and right-column progress, speed/limit and average/remaining distance using genuine FM/Waze data with GPS-only simulated movement.
- Disconnect/replace the client while an update is queued: the replacement receives no old widget/model/action state.
- Retain genuine frames/receipts with exact source/APK/host identities. Client fallback and acknowledged host-widget rendering are different outcomes; label them accurately.

The full emulation goal remains open until the actual implemented paths and their limits are verified. A contract, build or screenshot from the old renderer does not prove this handoff.
