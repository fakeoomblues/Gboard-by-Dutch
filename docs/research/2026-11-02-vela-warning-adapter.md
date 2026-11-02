# Vela warning adapter implementation

Implementation follow-up to the [binding inventory](2026-11-02-vela-warning-bindings.md) and [private runtime spikes](2026-11-02-native-warning-runtime-spikes.md). Target: original Vela 0.5.2621, SHA-256 `f168bc730568b98ee8132131658d9220ef5be3557af41fd373bdaf104c31b20f`.

The production adapter lives in `VelaWarningSpeaker.java`; guarded DEX edits live in `VelaWarningVoice.kt`, invoked by **External speed controls**. The patch verifies original bodies, operands, registers, catch ranges, fields and the navigation-generator caller inventory before mutation. The previously recorded private probe demonstrated pending system-closure fencing. Acceptance of this complete adapter still requires a rebuilt patched artifact and the runtime cases below.

## Output identity and readiness

Each attempt carries its exact provider grant, revision, generation and original ANNOUNCE deadline. A revised UPSERT supplies current text while retaining that original authority. Per-attempt utterance IDs contain random identifiers. Output decisions remain independent from receiver display state.

The adapter retains the application-scoped `wy4`, its mode, selected engine and native focus acquisition/release. Voice requires an initialized system engine with a matching installed offline voice, or a loaded selected neural model with the message's language. Tone requires native audio availability. Missing models, initialization flags alone and queued synthesis do not establish readiness. Native full mute caps all external sound; alerts-only mode caps Voice to Tone.

Ordinary alerts wait for a native guidance gap. P0 admission can interrupt native output when the real navigation session is bound. It holds incoming native dispatch, fences pending `pn1` and `dn2` closures, stops the native engines and waits for their native acquisition count and real output state to become idle before submitting the warning. An expired attempt terminates without receiving a fresh deadline.

## System speech and cancellation

A scoped `wy4.j(text,false,false)` preserves the application's recorder/prompt callbacks and native mute gate. Its language checks use the warning language for that call. The final tagged `pn1` dispatch preserves the semantic text, selects an installed voice on the existing engine and uses unique IDs. The original `vy4` object observes start/done/error/stop; untagged native IDs retain their original path.

While the external utterance owns the system queue, incoming native guidance is held for fresh regeneration. An interrupting native prompt cancels the warning and proceeds on the same engine. Source/off cancellation stops the external queue. The adapter reports uncertainty until the actual tagged terminal callback; a successful `stop()` request alone does not prove completion. Late and duplicate tagged callbacks cannot decrement a subsequent native route's focus acquisition.

## Fresh native route restoration

Original `xo2.h(LatLng,boolean,Double,Double,Double)` is the sole guarded caller of `rj2.j0(Route,yo2,LatLng,boolean,Double,double,double,double,Double,y74)`. The native location loop uses that generator's result to publish progress and speak its generated events.

A scoped interruption requests regeneration on the **next real native location update**. The hook verifies the current native session still owns that route and is navigating, unpaused and unarrived. It supplies an immutable `yo2` copy retaining current progress, geometry state, stop cues, chaining and all other fields while clearing only current-step spoken slots. The original native engine reprojects the current fix and produces currently applicable prompt bands. Held text is discarded. Paused, muted, arrived or off-route states wait for an applicable native update. No synthetic location is submitted and no captured turn text is replayed.

## Selected neural engine

The scoped external branch preserves the selected `y43` instance and submits an identity-bearing job to its original serialized synthesis executor. It verifies the loaded model identity, language and native cancellation generation before calling original `y43.a/g/e`: selected model, speaker and synthesis. It applies the native speed/volume settings and the shared native RMS/limiter policy. The normal `wy4.j` callbacks still run; untagged native neural jobs and their callbacks remain intact.

The external job owns its output AudioTrack with native navigation/speech attributes. It accounts for actual nonblocking writes, checks cancellation before each content write and observes playback-head advancement beyond the silent lead and first content sample before STARTED. COMPLETED requires all written frames to drain. Missing models, synthesis failure, partial-write failures, silence, generation changes and drain timeouts retain distinct failure boundaries. Synthesis completion alone never means spoken.

Cancellation pauses, flushes, stops and releases only that attempt's retained track. Synthesis already running on the native worker may finish its CPU work; its cancelled identity prevents later audio submission. Output uncertainty remains fenced if positive track release cannot be established. Uncertain output suppresses automatic replay.

## Tone

Tone retains the existing external alerts-only PCM recipe: two 1046.5 Hz, 110 ms notes at 22,050 Hz, the native envelope and a 661-frame gap. It uses the application's focus owner and navigation/sonification attributes. The adapter retains the track, observes actual playback-head start/drain and cancels only its output. A thread starting or a successful write is insufficient for COMPLETED.

## Verification and remaining acceptance

Host tests cover original-artifact guards, preserved untagged listener bodies, reapplication rejection, unique attempts, native language selection, original deadlines, focus denial, pending and active cancellation, fresh native-state copying, native pending-closure preemption, selected neural readiness and model/generation changes across the real worker boundary. Test doubles exercise controlled output conditions; they do not establish installed-model or car routing behavior.

The required patched-artifact runtime cases remain:

- System and selected installed neural voice: actual start, completion, failure and focus denial.
- Active source/off cancellation, ownership change and late callbacks, with native guidance preserved.
- P0 interruption followed by a fresh route update, including passage to another turn and a route replacement.
- Actual PCM partial-write/drain behavior and cancellation while synthesis or playback is active.
- Cold phone/background and supported car sessions, native mute caps and routed output.

These are completion gates for R3, R5 and R8 in the [design](../superpowers/specs/2026-11-02-flitsmeister-native-warnings-design.md).

## Private runtime probe source

`scripts/navigation-alerts/probe/VelaAdapterProbe.java` is a same-signer instrumentation source for an actual patched target. Its scenarios are `adapter_system`, `adapter_neural` and `adapter_all`; the probe builder/dispatcher/runner must explicitly route those scenarios before execution. It resolves production classes through the target class loader and compiles without embedding duplicate production adapter classes.

System cases require real engine callbacks, pending/active cancellation and actual original `xo2`/`rj2` route regeneration with synthetic geometry. The probe-only observer wraps and forwards to the same installed `vy4` object on a newly owned guide. Its report identifies that observer change. Neural cases require the real selected installed model, original synthesis worker and actual AudioTrack lifecycle; a missing model returns **inconclusive**, which must fail an acceptance run. The probe uses isolated in-memory grants and creates no public road reports or system location updates. It refuses an already active native route and restores the application's previous voice/session binding during cleanup.

The source passes compilation against Android 36. This is preparation for runtime validation, not an executed result.
