# Waze native-warning implementation checkpoint

This task extends the existing **Broadcast navigation data** patch into the [reviewed four-app design](../superpowers/specs/2026-11-02-navigation-provider-warnings-design.md), reusing the implemented FM transport, ownership, scheduler and Maps/Vela speakers. It does not replace Maps/Vela navigation with a Waze route. The user's clarified goal is integration parity: recover warnings dropped by the current Waze bridge, without assuming Waze offers warnings that FM lacks. Preserve Waze's native distinctions in the shared protocol; route, ETA and lane-guidance enrichment are outside this implementation batch.

## Recovered design and current base

The expanded design, [Waze research report](2026-11-02-waze-first-class-provider.md) and [model inventory](../waze/native-provider-inventory.json) were recovered from local commit `65a4d8c`. Those three files retain their original contents. Their historical statement that implementation was pending must be read against the current FM implementation.

Implementation starts from `304bbcf66468e517a3a8f48422fe959c154fd21f` on `feat/waze-native-warnings`, in the existing isolated `navigation-startup` worktree. This includes the FM review corrections at `c521111`, whose signed candidate was transferred for user testing. The primary dirty checkout and the frozen, uncommitted Frida work are preserved. No phone or emulator operation has been performed for this Waze task.

## Supplied input: same DEX, different APK envelope

| Input | SHA-256 |
| --- | --- |
| Supplied complete Waze APKM | `62418bf0898ea1cc6c6926a136bb08bedcfcbff31de3bbc357afc6ef579db1d3` |
| Its original base APK | `b1504af4bcbe50325195774a891f50f7189cdef7ad38ee67a2f5b7af00f29e4d` |
| Retained research base APK | `5ab9853c76a3f83a493e1872f232bbbde96ec61245cbca117aa39e176cdfe9c4` |

All ten `classes*.dex` entries match byte-for-byte between the supplied and retained bases. The existing decompiled corpus and method discoveries can therefore be reused. The supplied base declares `com.waze`, version `5.26.90.901`, version code `1030746`, min SDK 29 and target SDK 36. SDK/manifest/resource handling remains variant-specific; equal DEX does not make every APK entry identical.

SDK `apksigner verify` succeeds and the publisher certificate SHA-256 is `03637f6c5d8f604e6fdb79a6ffbfa578de4e318f8da22fc6106665247f8807d7`, matching the recovered research. The complete 28-split archive is preserved for eventual Manager-equivalent merging and patching. The extracted base and retained decompiled corpus are reusable inputs for this active task, not per-run build scratch. Exact DEX hashes, archive entries, input and REA receipts are retained under ignored `.analysis/waze-native-warnings/`.

## Existing implementation versus required changes

| Area | Existing behavior | Required extension |
| --- | --- | --- |
| Waze report export | `NavigationBroadcasts.alerts` reduces descriptor types to broad schema-1 kinds, drops native current validation distance, priority/silence and subtype/trait details | A separate native event adapter preserves meaning, presence-qualified fields and source policy without turning repository snapshots into speech |
| Segment insights | The exporter filters to segment type 7 for average-speed zones | Cover every required segment family and correlate linked alerters without duplicate speech |
| Notifications | No event-channel notification capture | Add/remove, native text/car variants and expiry; ADD remains state unless native audible intent or explicitly negotiated advisory policy authorizes speech |
| Waze control service | Startup/health only; no warning endpoint | Reuse the authenticated warning endpoint and durable audio-owner rules, with Waze's own profile and coverage |
| Receivers | FM-specific event negotiation, settings/status attribution and event-audio suppression | Generalize provider profiles/settings and retain source attribution, existing FM preferences and legacy Waze compatibility |
| Speaker/transport core | Implemented and reviewed for FM | Reuse it; do not create another TTS engine, scheduler or startup mechanism |

## Native boundaries re-established

Fresh REA inspection on the supplied base confirms `AlerterNativeManagerBase.h(AlerterId)` dispatches case 9 through the native looper. The matching DEX corpus shows that case invoking `AlerterNativeManager.playSoundNTV(byte[])`. The selector update coroutine calls the selected holder's `i()`, then `alerters.a.m.f(id)` reaches this dispatch. This is a correlated sound-intent candidate; source eligibility, silence and other-family coverage must still be respected. Repository publication and UI `onAlertStartNTV` are different boundaries.

Fresh REA inspection also confirms `alerters.b.g.a()` resolves the lazy selector. Its native DI factory registers `alerters.b.a`, the combined collection, state-holder factory and native manager. The selector constructor subscribes to the native collection with a native coroutine scope; the collection subscribes to native repository updates. Existing code uses `kotlinx.coroutines.a.ab.l(Class)` to resolve native DI singletons. These provide a service-safe path to investigate without constructing a Waze Activity. Singleton resolution alone will not be reported as runtime controller readiness.

The native converter reads `AlerterInfo.getSubType()` directly into the UI model after converting its wire type, while separate subtype enum namespaces differ numerically. Preserve raw namespace/value and verify actual interpretation rather than assuming protobuf hazard or blocked-lane enum values encode that untyped field. Known priority, validity, familiar/grouped silence and native source settings must survive managed local mute.

## Batch sequence and retained gates

1. Implement Waze's semantic capture and native controller/lifecycle bindings, with exact original-method guards and complete required-family accounting.
2. Connect its endpoint and generalize both receivers' profiles, settings, attribution and legacy-audio suppression while preserving FM behavior.
3. Complete related product tests alongside each implementation batch, then run grouped static/build/unit checkpoints using the existing verification workflow.
4. Rebuild/sign intended combinations, then perform bounded all-four-pairing acceptance with the required Frida coverage, provider silence and exact cleanup. Current capture, UI-free production, route relevance, speech/output, phone/car and full-scope acceptance remain unproved for this extension.

The existing validation-tooling freeze remains in force. New probes, runners, observers, fixtures or framework changes require explicit named approval. No such tooling was created or changed for this checkpoint, and no runtime claim follows from the static findings above.

## Implemented checkpoint and actual-input patching

`94d6d1382fbbd433e53807c500df958a3c8a1178` connects Waze native descriptors, sound decisions, zone selection/exit, segment state and notification lifecycle to the existing FM warning endpoint. Maps and Vela now select the provider's profile and preferences while retaining their original speakers and navigation. A repository snapshot or notification ADD remains state rather than an announcement. Zone selection and the original UI sound callback correlate to one entry decision in either order.

The next connected batch applies FM's disjoint schema-1 views to Waze: confirmed event clients retain the full display feed; legacy recipients receive speed/limit/active-zone fields with report/approach announcements disabled during exclusive or uncertain output. The independently optional Share alerts bridge clears cached pins and heartbeats while suspended, without changing its saved setting. Endpoint startup begins fenced until the durable output ledger is loaded, preventing a legacy delivery window before uncertainty is known.

The original complete APKM was patched with Morphe Desktop 1.19.1 using FULL DEX compilation and fingerprint validation. All 21 selected patches applied with no failures, including Broadcast navigation data, Share alerts with Maps, Average speed zone bar, Native camera and hazard controls, Speed limit controls, and Speed camera sound at any speed. This is a real split-input patch/rebuild check, not just a patch-library build. The unchanged public Morphe signing identity verifies the bundle; SDK apksigner and zipalign verify the test-signed APK. Nothing was installed on a phone or emulator.

| Retained acceptance input | SHA-256 |
| --- | --- |
| Signed MPP, compatibility/startup-fence batch | `2c13af05a8e04d2be0de6e044139927a5ed68adae96c68199e15337656b12677` |
| Test-signed patched Waze APK | `9860136bfc79e1ab39f58bd4022f0e8e8ded9c3cfcea50c98a24338193ab95f6` |

The local `checkpoint-20261104-c` and `checkpoint-20261104-d` receipts retain exact base/source-file digests, per-module outcomes and quiescent process-group cleanup. Unaffected passing checks from the preceding complete `verify.cjs` checkpoint are reused; the affected FM and Waze tests were rerun after the test-handshake correction. Both build checkouts and all patching scratch are removed. Only compact evidence, the signed bundle and patched APK needed for the pending acceptance session remain.

## Remaining gates

- **Coverage is deliberately incomplete:** Waze's profile still advertises `complete=false`. Structural bindings and the successful APK rebuild do not authorize an exclusive first-class voice claim.
- **Native semantics:** retain a per-family availability account and prove the bound native decisions at runtime. The artifact-backed roadside/safety/crash-history classification and conservative urgency mapping are described below. Optional route/ETA/lane-guidance features remain outside this batch under the user's clarified scope.
- **Receiving relevance:** the scheduler now checks fresh point warnings against the receiver's active directed route, including ambiguous/missing context and a changed context at grant reply. Runtime acceptance of those checks remains open.
- **Overlap:** native/OSM events retain separate provenance and explicitly unresolved equivalence when directed-road evidence is absent. No radius-only equivalence or successful duplicate suppression is claimed.
- **Runtime:** establish actual UI-free controller production, warning/message correlation, silent source output, receiver speech outcomes, source switches and phone/car acceptance for the required pairings. The existing observer hardcodes Flitsmeister and cannot cover Waze; explicit approval for the named observer adaptation is pending under the tooling freeze. No observer or probe was changed in these implementation batches.
- **Delivery:** finish review and acceptance before marking coverage complete or promoting a stable release. The retained signed candidate is an engineering acceptance input, not a published release.

## Native family policy and receiving route checks

REA resource decoding on the supplied original base (`ev_ae7a32e7d3ffe7dbc30f55f960da5f719a6942182a228c5a86fd58fa5aed005e`) resolves the formerly unproven family names. `ALERT_TITLE_PERSONAL_SAFETY` is a safety-incident warning, the five SOS titles describe roadside assistance, and `ALERT_TITLE_GUARDIAN_PS_PS` describes crash history. The complete retained resource evidence includes the target digest and decoding limits: smali was skipped, one markup entry was omitted, and the default string table is limited to the first 2,000 of 4,364 entries. These specific resource entries were present; absence elsewhere in that bounded table is not evidence of absence in the APK.

| Pinned native types | Adapter policy |
| --- | --- |
| POLICE, CAMERA | Native enforcement subtype/text, P1 |
| ACCIDENT, HAZARD, CONSTRUCTION, CLOSURE, BLOCKED_LANE, BAD_WEATHER, ANIMAL, SUDDEN_SLOWDOWN | Native road-warning subtype/text, P1 |
| TRAFFIC_JAM, TRAFFIC_INFO | Native traffic subtype/text, P2 |
| PERMANENT_HAZARD, SPEED_LIMIT_DECREASE, GUARDIAN | Native road-feature or crash-history text, P2 |
| PERSONAL_SAFETY | Native reported safety-incident text, P1; no route action exported |
| SOS | Native roadside-assistance subtype/text, P2 |
| DANGER_ZONE and descriptors with ZoneAlertData | Native selected zone phase and message; exit template retains explicit template provenance |
| OTHER, DYNAMIC, UNRECOGNIZED | Native-text fallback with original type/raw subtype, P2; no inferred urgency |
| CHITCHAT, PARKING, PARKED, ECO, REROUTE | Social, personal parking or separate source-route content excluded from road-warning speech |

All audible rows still require native validity, absence of silent activation, current identity correlation and an actual native sound/selection decision. The native priority/preemptive values remain metadata. Waze's card-preemption flag alone does not authorize P0 speech or interruption of receiving navigation guidance. Unknown types without usable native content remain unsupported rather than receiving invented text.

The shared scheduler applies receiver relevance to fresh as well as replayed `point_ahead` events. Both receivers use their original directed route geometry and a genuinely fresh, accurate location fix. Clearly passed/off-route points are rejected; parallel/ambiguous legs, unavailable geometry or stale/missing fixes yield CONTEXT_UNVERIFIED and wait only within the original native announcement window. Nonspatial native warnings and explicit speech previews are exempt from point geometry. A held point cannot block a nonspatial emergency behind it in the queue. Context is checked again after the asynchronous playback grant and before speaker submission; a route change also cancels accepted but unstarted output. These checks neither start Waze navigation nor create later announcement authority.

The resulting four-app engineering candidate uses signed MPP SHA-256 `fa431fd1d01352ebfaeb4b5510250c736f893c940bea74bf0fad9c8057afe4fd`. Actual-input patching applied 21 Waze, nine FM, four Maps and one Vela patch with zero failures. Every APK passed SDK signature and alignment checks. Exact input/output hashes, patch reports and cleanup receipts are retained under `.analysis/waze-native-warnings/checkpoint-20261104-g/`; this is the pending acceptance input, not a published release. Passing evidence from the grouped checkpoint is retained, with only affected failed tests rerun after correcting their test data. Runtime acceptance and the incomplete Waze coverage flag remain unchanged.

The cached native startup trace also shows `com.waze.ls` launching `com.waze.ci` case 15 in its Application-owned coroutine scope; that case resolves `alerters.b.o` and calls its suspending `f()` collector. The collector filters zone descriptors using native validity before producing entry/exit holders. This supports reusing the original headless initialization path rather than creating another zone subscription. It is static evidence, and does not prove that the collector is running in an exercised process.
