# Final native-warning acceptance checkpoint

Status: **incomplete**. This checkpoint records one grouped functional session and one affected-group rerun on owned emulator 5590 with the corrected FM15 producer. It does not replace the [R1–R8 design](../superpowers/specs/2026-11-02-flitsmeister-native-warnings-design.md), earlier artifact-specific evidence or physical/car acceptance.

Maps and Vela retain trip routing and guidance. FM supplies alerts and speed/limit/zone data. The tested native zone path is an average-speed camera segment, not FM trip navigation.

## Executed group

The [functional receipt](../../tests/fixtures/navigation-warnings/flitsmeister15-final-functional-verification.json) names every tested authority, source acknowledgement, receiver attempt and artifact. Inputs were:

| Input | SHA-256 |
| --- | --- |
| Corrected FM15 producer APK | `19d40560cb885f868ffeeb936f12a0daa0e7fa666776acf1582e85e63b6c2ecd` |
| Maps APK | `3285d836bfb7caefd361fec42d7ed080017021f885eabf331b02b88287fb8839` |
| Vela APK | `958bd90f477c45d8210f797b34e8649162d4153238419ab2ab7af856cd9edf19` |

The session reused the frozen repeat10 producer probe, receiver-zone05 probes and existing gRPC GPS/runners. Their original source and compiled hashes were checked. No product implementation, test harness or app build changed during the session. The first cold-boot admission attempt refused a still-active Android audio player before candidate installation; the bounded settlement checkpoint admitted the subsequent functional group.

Both receivers completed the first native zone entry and exit. An offline join verifies four source ANNOUNCE authorities against their actual receiving speaker payloads, four STARTED callbacks, four COMPLETED callbacks and four source GRANTED → STARTED → COMPLETED sequences. Every authority has one attempt and one start. Text, language, category, priority, phase, warning identity and announcement index match; native source path entry, exit and candidate restoration were observed.

Both cases then failed the frozen probe's `Native source fix is not fresh` guard at `NativeFmCaptureProbe.java:528` / `Fm15PrivateZoneInput.freshFix`, while beginning the return trajectory. Neither produced a repeated native authority. The original job and both repeat cases remain failed/incomplete; the observed first-pass outputs are partial evidence only. This failure does not establish a product repeat defect or verify the expired-state identity seam.

## One affected-group correction and rerun

The collected traces showed an observation race at the GNSS handoff: the first return request repeats the last coordinate, while real native collector samples arrive approximately two seconds apart. The frozen loop aborted immediately on a stale observation instead of awaiting the original collector outside the main thread.

One private-probe correction classifies only valid-clock moving fixes that are stale or not yet consumed as pending observations. The loop waits outside the main thread for at most five seconds while normal reviewed GNSS requests continue. Every admitted sample still requires a positive, nonfuture timestamp, age at most 2.5 seconds, real motion/quality fields and the original consumed-fix identity. Original binding, job, candidate ownership and native policy checks still abort immediately on failure. The correction writes no native GPS, decision, policy or readiness field. Its sources, precise patch and compiled binding are retained; no product or receiver code changed. The source probe was compiled/signed once as `8536690218240131f3a1ed979573a9e47f990bcf32aef2ea5608af2221ad5ae7`.

In the sole rerun, the Maps wait resolved after 567 ms with an actual 46-ms-old consumed fix; Vela resolved after 562 ms with a 48-ms-old fix. Both then progressed onto the return trajectory. In both, the original FM producer replaced the private candidate set. The unchanged exact-ownership guard aborted with “Native producer replaced candidate set; restoration is refused.” The probe did not overwrite the replacement or manufacture another native announcement. Both repeated first-pass entry/exit outputs completed once again, but no repeat authority was generated or accepted.

This leaves natural-repeat acceptance open at a private-fixture ownership boundary. The polling correction is supported by the observed consumed fixes; neither run establishes a product defect or passing repeat. No further probe edits, rebuilds or reruns followed. The full outer VM rollback restored the task state after the inner candidate restore correctly refused lost ownership.

The local release watcher was active during this functional group. Its timestamps are retained, but this group is not used for timing-percentile acceptance. Native speaker payload/lifecycle joins do not establish acoustic audibility or car routing.

## Smallest remaining groups

| Required group | Current disposition |
| --- | --- |
| Same-ID original native repeat after producer-state expiry; changed unsaid revision | Expiry regression and component revision tests pass. Natural repeat remains unproved at the private candidate-ownership boundary above; changed-unsaid source-to-speaker acceptance remains open. Direct calls to native speech methods do not replace the original repeat decision. |
| Delayed point on an existing receiver route: bend, passed point, nearby/ambiguous legs | Existing straight passed/valid/expired replay, native route inventory, route-binding and scheduler tests remain valid in their recorded scopes. The coupled original-route delayed-warning cases remain open. No new route engine or fabricated route is required. |
| Receiver modes and failure/status | Retain Vela's system/neural/tone/mute-cap adapter evidence, Maps' supported tone/voice/mute/readiness evidence and current component tests. Targeted actual-source mode/failure and honest degraded-status seams remain open; no enum-by-mode Cartesian suite is required. |
| Source selection, screen-off/phone-absent/car-only lifetime and mixed versions | Retain passing BUSY/release/death/provider-restart results, existing car-host evidence and protocol/startup/Waze tests with their original identities. Current integrated source-switch/lifetime/compatibility seams remain open. A local car preview does not establish physical projection/audio routing. |
| Final artifact timing and output | Retain prior warm corpora and PCM/playback evidence under their original artifact identities. Final producer timing disposition and the remaining output-signal gate remain open. This functional group adds no percentile claim. |
| Authorized phone/car acceptance and delivery | Physical phone, Bluetooth/music ducking and supported actual car setups remain open. This unit has no authorization to mutate the phone or shared emulators. Final review/delivery and separately approved stable promotion remain separate. |

Earlier successful coverage, priority, native guidance, moving lead, zone and recovery results remain useful evidence of unchanged paths. Their older producer identities are unchanged in the records. The corrected producer's passing 172 FM component tests and nine FULL patch applications are not relabelled as passing runtime repeat acceptance.

## Cleanup

Both outer rollbacks verified the original three APKs, preference bytes/ownership/modes, permission grants/flags, window settings and firewall rules, and restored the original emulator launch arguments. Owned helpers and snapshots were removed; emulator 5590 was stopped. First-group candidate restores passed. Rerun candidate restores refused lost ownership as described above; the outer VM snapshots restored all task state. Receiver quiescence/preference restores passed throughout. Both outer receipts have `cleanup_required=false`, with no cleanup errors.

Only compact receipts and two local evidence archives remain for this unit: the original 202,145-byte archive (`15643bb563ab70dabefd2f7902bbc1a550585349e2dcb7881acfe0f3b667561c`) and the 234,721-byte rerun archive (`524e5d1294572b40747bff2a64948b2a8cf7167dbd739c97ae5c88d48cee3321`). Raw job folders, temporary runner/source copies and duplicate APKs were removed. Original inputs, active compiled probes and earlier evidence were preserved. The primary checkout was not modified.
