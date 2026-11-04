# Native warning implementation review

Review checkpoint: `d0bc0d1`, after the FM15 port, background-start correction, recovery work and local-CI sync. This is a code review and delivery inventory. It does not mark the design's remaining runtime or physical acceptance complete.

## Product scope

Maps and Vela own trip routing, route selection and turn-by-turn guidance. FM supplies native alerts and existing speed/limit/zone data while its UI remains backgrounded. Receiving-route inspection only checks stale delayed point alerts against an existing native route. The FM average-speed model's `gpsPath` is the camera segment, not a trip route.

## Requirement review

| Requirement | Implemented path reviewed | Remaining boundary |
| --- | --- | --- |
| R1: native capture and meaning | Separate original-DEX contracts and hooks for FM14/FM15; versioned coverage manifest; model/text/language adapters in `NativeWarnings` and `NativeWarningText`. Original native decisions remain upstream of capture. | The same-ID native-repeat and changed-unsaid cases remain final-pass checks. Enum accounting and original ABI guards already have evidence. |
| R2: immediate delivery and priorities | `WarningEndpoint` journals and delivers independently of schema1 heartbeat. `WarningScheduler` orders native authorities and allows P0 preemption. Its passage filter applies to replay or an authority already over one second old; a fresh native announcement does not wait for route geometry. | Final timing/outcome figures must name the final producer artifact. Existing priority and delivery runs remain retained. |
| R3: visible warnings and native speech | Maps uses `MapsWarningSpeaker` and the original guidance coordinator. Vela uses its original guide and supported system/neural paths. Readiness and effective Voice/Tone/Silent modes are distinct; native mute remains authoritative. | Targeted final mode/engine/failure checks and supported car routing remain acceptance items. No new speech/routing engine is required. |
| R4: provider silence and one owner | Existing managed provider lease, original playback suppression and completion handling; authenticated `WarningAudioOwner` leases, nonce/epoch checks and a durable pending grant. Unknown output blocks later grants. | Retain passing BUSY, release, death and restart proofs. Check the remaining source-switch/lifecycle seam in the final pass. |
| R5: honest lifecycle/status | Speaker acceptance is separate from actual start/completion. Failure without positive quiescence remains uncertain. Both consumers expose degraded/owner/mute status; Vela's actual waiting status is captured. | Final-pass output signal and remaining degraded UI cases; physical audibility is not inferred from callbacks. |
| R6: replay, cancellation and expiry | `WarningJournal`, `WarningConnection`, `WarningSession` and `WarningScheduler` preserve sequence/session identity, original authority/deadline, bounded replay and stale-work fencing. Actual started output is not retried speculatively. | Delayed-alert relevance around bends/nearby legs and changed-unsaid payload cancellation remain explicit checks. The expired-state announcement-ID defect is fixed below. |
| R7: existing feed and compatibility | Schema1 remains separate. Legacy control sessions block full ownership, unsupported event capability stays display/legacy compatible, and Waze selection remains independent. Existing route and car-session lifetime signals feed the startup client. | Targeted mixed-version/package and current car-only/session checks; do not claim an undetectable historical broadcast-only client can be revoked. |
| R8: acceptance and delivery | Versioned fixtures, native callback/source-ACK joins, original APK guards, Android builds and the maintained local verification workflow are present. | This requirement is still open. Remaining integrated, timing, authorized phone/car checks, final review and separately approved stable promotion are not replaced by a green build. |

No further missing core implementation block was identified in the reviewed paths. That is not evidence that every unexecuted scenario will pass. A confirmed final-pass failure may require a product correction.

## Confirmed correction

The producer's bounded warning-state cache previously owned its announcement counter. After state expiry, a later accepted native announcement for the same warning/phase could reuse the earlier index and therefore its authority key.

`d0bc0d1` allocates accepted announcement indices independently of expired model state. The counter advances only for a native ANNOUNCE; state updates do not create speech authority. This changes identity bookkeeping, not native relevance, mute decisions or navigation.

The expiry regression failed before the correction. All 172 FM component tests pass afterward. The replacement producer also applies all nine intended FM15 patches through FULL validation and independent emitted-APK checks.

## Receiver reconnect correction

The subsequent source review found that `WarningSession` fenced callbacks only by the startup ticket. Replacing an event endpoint within the same ticket could let the retired connection's later close, owner-revocation, event or snapshot callback affect the replacement scheduler/view. Incompatible event declarations also left the previous connection attached until the startup client completed its asynchronous disconnect.

The session now binds callbacks to the specific connection and serializes the validity check with delivery. Detachment invalidates that identity before retiring transport and cancels old output once. An incompatible event declaration detaches immediately. This preserves the retired transport's terminal/release IPC for outstanding output; it does not erase grants or claim that uncertain audio stopped.

Two regressions in the existing `WarningConnectionTest` exercise real endpoint handshakes, same-ticket replacement and incompatible negotiation, then deliver callbacks retained from the old connection. All 100 shared warning unit tests pass, and both Maps and Vela release Java compilation pass in the same offline checkpoint. No runtime session or validation-tooling change was performed. This correction is not included in the older signed candidate below; new receiver artifacts and remaining acceptance are still required.

## Delivery artifacts and evidence reuse

These are development validation artifacts built from the working tree containing the identity correction. Their receipt records the build base and dirty fix explicitly. Stable release automation will build from the approved committed head; these files are not a published release.

| Artifact | SHA-256 |
| --- | --- |
| Signed bundle after identity correction | `66abe63b4e27e2bc06b9719f5be65efa96d0fd181b124e6cad53990e1f4b46d1` |
| Replacement FM15 test APK | `19d40560cb885f868ffeeb936f12a0daa0e7fa666776acf1582e85e63b6c2ecd` |
| Existing Maps test APK | `3285d836bfb7caefd361fec42d7ed080017021f885eabf331b02b88287fb8839` |
| Existing Vela test APK | `958bd90f477c45d8210f797b34e8649162d4153238419ab2ab7af856cd9edf19` |

The earlier FM15 runtime corpus names producer `13dc…` and bundle `86728…`. Keep those identities in its records. It remains evidence of its recorded behavior and unchanged component paths; it is not silently relabelled as final-artifact acceptance. The final integrated pass must include the replacement producer's repeat identity seam.

The Maps speaker last changed in `7550659`; the Vela speaker and shared scheduler last changed in `64c000c`. Their existing component and adapter evidence can be reused where the exact source/binding is unchanged. The FM15 producer profile and the startup capability change have their own current evidence. Rebuilding every unchanged adapter for every warning enum is not a design requirement.

## Finish sequence

1. Preserve the verified implementation and recorded zone/recovery results; freeze further harness expansion during implementation review.
2. Use the existing tools for one grouped final integrated pass over the remaining named criteria. Retain passing checks. Investigate a confirmed behavior failure with a focused reproduction before changing code or tooling.
3. Prepare the reviewed source, signed bundle, artifact matrix and bounded limitations for delivery. Complete authorized device/car acceptance and required promotion approval before stable publication.

The existing [acceptance record](2026-11-03-native-warning-acceptance.md) and [design](../superpowers/specs/2026-11-02-flitsmeister-native-warnings-design.md) retain the full criteria. This review narrows neither the requested outcome nor the completion decision.
