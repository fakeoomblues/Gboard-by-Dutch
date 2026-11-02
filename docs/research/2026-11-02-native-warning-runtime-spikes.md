# Native warning audio feasibility spikes

Work package 1 of the [native warning design](../superpowers/specs/2026-11-02-flitsmeister-native-warnings-design.md). These experiments establish specific native lifecycle and pending-cancellation mechanisms. They do not implement the complete warning feature or satisfy its rollout gates.

## Setup and provenance

- Owned AVD: `native_warnings_api34`, Android 14/API 34, arm64 Google APIs, serial `emulator-5580`. Its configuration and userdata live under ignored `.analysis/native-warning-bindings/`; the pre-existing emulator and personal devices were not used.
- Original Maps and Vela input identities are pinned in [the binding report](2026-11-02-native-warning-bindings.md). The runner checks both original SHA-256 and the installed target/probe APK SHA-256. Same-test-signer instrumentation executes in the actual target application UID.
- Baseline application ZIP payloads are preserved byte-for-byte; only APK signing metadata changes, including removal of `stamp-cert-sha256`. Original downloads remain untouched. No accounts, destinations or public road reports are required.
- Google APIs includes system Maps. In this disposable AVD only, a writable-system overlay and a reversible rename of `/product/app/Maps/Maps.apk` to `Maps.apk.native-warning-backup` permit the test-signed standalone original to install. This is emulator setup, not an installation requirement for the shipped patch.
- Full instrumentation reports remain in `.analysis/native-warning-bindings/{maps-runtime-original,maps-manager-runtime-original,vela-runtime-original,vela-runtime-fenced}/`. [The compact evidence record](../../tests/fixtures/navigation-warnings/runtime-spikes.json) retains input/output identities, report digests, observations and explicit scope.

## Maps: original players and real application manager

The player probe constructs original `bmsk` with probe-owned MediaPlayers and local synthetic WAVs. Its original completion listener remains installed. Three cases exercise natural completion, cancellation during playback and cancellation before playback. Real `MediaPlayer.start()` return, `isPlaying()`, advancing playback position, stop/release and the native terminal callback were observed. The native start callback occurs before the real player starts, confirming that it cannot be used as proof of actual playback.

The manager probe obtains the original application dependency component through `rJ()` → `njj.hU` → provider `a()`. The provider creates/injects the real `bmrc`/focus dependencies when not already cached. It requires an idle manager before introducing two explicitly synthetic TEST_AUDIO jobs through native `bmrc.k`. It never replaces the manager or fabricates its dependencies.

| Case | Observed native result |
| --- | --- |
| Active synthetic job | Real playback and advancing position; exact native active-job identity |
| Pending second job | Exact native pending-job identity; no real player start |
| `l(pending)` | One `CANCELLED_NEVER_PLAYED / NON_AUDIO` terminal result; active job retained |
| `l(active)` | One `CANCELLED / LOCAL` result; active media stopped/released; native job cancellation flag true |
| Manager empty state | `B()`/`p`/`q` can become idle before active media teardown and terminal completion |

Pending cancellation does not release the supplied prepared media. The probe explicitly releases that remaining probe-owned player during cleanup and records this separately from native cancellation. Production coordination must account for this resource/lifecycle distinction.

These are real original manager operations with synthetic prompts. They do not prove current-route generation, ordinary GUIDANCE eligibility, a hold against arriving prompts, chained players, mute, focus denial or car output. A production hold must fence the final asynchronous dispatch and wait for actual leaf-player quiescence; clearing `p/q` alone is insufficient.

Static continuation anchors: `bmtf.b` selector 0 dispatches through `bmuc.d(job.w)` after asynchronous readiness/focus work without checking `job.s`; `bmsh`/`bmsg` handle chained media separately. Fresh guidance can originate through `bmia.u(current ybc)` → `bmvg.d(current blot,null)` → native PULL_GUIDANCE, but that path needs a real current route model. No saved turn text is a valid substitute.

## Vela: original race and selective private candidate

The original probe constructs a disposable native `wy4(Context)`, calls original system-engine initialization and original `j(text,false,false)`, and observes real system TTS callbacks. All phrases are clearly synthetic. The emulator default engine is Google TTS with usable English voice data; neural voices are not covered.

Android exposes no listener getter. This probe-only observer forwards callbacks to a newly constructed original `vy4(ownedGuide)`, preserving its behavior but not listener object identity. Production hooks must observe inside the existing listener rather than install this wrapper. TTS start/done/audio callbacks are engine evidence; acoustic output and car routing are not measured.

The baseline reproduces speech after original native stop and after native full mute when either is invoked inside the 350 ms dispatch lead. Both cases produced actual speech start and completion callbacks. The native stop clears its current queues/focus but leaves the delayed `pn1` runnable eligible.

The private candidate adds three guarded hooks:

1. `pn1` constructor return captures the specific closure only inside an explicit external-speech scope.
2. `pn1.invoke` checks the tagged closure before executing its original body; rejected work returns original `er4.a`.
3. `wy4.n` entry invalidates the native-stop generation before the original focus reset.

The builder verifies the original 37-method contract and then the exact inserted operations, register counts, flags, original instruction bodies and adjusted catch addresses. The nearly full first DEX cannot accept the added references directly. Only `wy4` and `pn1` move into a third DEX alongside the test helper; the existing second DEX, resources, manifest and non-signing assets remain unchanged. This relocation is a private builder choice, not a required production patch architecture.

| Private candidate case | Observed result |
| --- | --- |
| Tagged warmup and warm baseline | Original engine start then done retained |
| Tagged warning, then native stop during lead | Closure rejection; no speech callback; no extra focus decrement after native reset |
| Tagged warning, then full mute during lead | Same rejection; native mute remains true |
| Tagged warning, then source cancellation without native stop | Closure rejection; one native `f()` balances only its pending focus acquisition |
| Cancel tagged warning and submit untagged synthetic native prompt on the same guide | Tagged warning rejected; untagged prompt starts and completes; its focus count remains owned until completion |

The fenced suite records six captured closures, two allowed and four rejected, plus one allowed untagged prompt. A `NOT_STARTED` observation requires a captured token, a corresponding helper rejection and no speech callbacks during the bounded observation; silence alone does not qualify. Weak identity keys and retained one-shot tombstones prevent equality collisions or duplicate invocation from resurrecting cancelled work. Host tests cover duplicate calls, native-stop/new-work focus ownership, source cancellation, mute, untagged/UI paths, registry bounds, scope misuse and ABI failures.

This proves a pending **system-TTS** closure mechanism. It does not prove cancellation of already submitted/playing warning audio, unique playback IDs, neural player outcomes, focus grants, ownership epochs, deadlines, native turn regeneration or complete phone/car coordination. The test helper and its limits are not production state machines.

## Reproduction

Build the [native probe](../../scripts/navigation-alerts/build_native_probe.py) using a disposable test key, then use [the runner](../../scripts/navigation-alerts/run_native_probe.py) with the exact owned AVD. The runner refuses other serials/AVDs, archives previous evidence before execution and publishes successful reports atomically with a run ID. It rejects failed or inconclusive probe results; execution completion never means feature completion.

```bash
python3 scripts/navigation-alerts/build_native_probe.py --sdk "$ANDROID_HOME" --output .analysis/native-warning-bindings/vela-probe --target vela --key .analysis/native-warning-bindings/native-warning-test.p12
python3 scripts/navigation-alerts/run_native_probe.py --sdk "$ANDROID_HOME" --serial emulator-5580 --target vela --original ~/Downloads/vela-maps-v0.5.2621.apk --probe .analysis/native-warning-bindings/vela-probe/probe.apk --key .analysis/native-warning-bindings/native-warning-test.p12 --output .analysis/native-warning-bindings/vela-runtime-original --scenario all
python3 scripts/navigation-alerts/build_fenced_vela.py --sdk "$ANDROID_HOME" --original ~/Downloads/vela-maps-v0.5.2621.apk --output .analysis/native-warning-bindings/vela-private-fence --key .analysis/native-warning-bindings/native-warning-test.p12
python3 scripts/navigation-alerts/run_native_probe.py --sdk "$ANDROID_HOME" --serial emulator-5580 --target vela --original ~/Downloads/vela-maps-v0.5.2621.apk --candidate .analysis/native-warning-bindings/vela-private-fence/vela-private-fenced.apk --scenario fenced --probe .analysis/native-warning-bindings/vela-probe/probe.apk --key .analysis/native-warning-bindings/native-warning-test.p12 --output .analysis/native-warning-bindings/vela-runtime-fenced
```

Use `--target maps` with its pinned original and Maps probe for `--scenario baseline` and `--scenario manager`. None of these commands publishes an artifact or cuts a release.

## Remaining completion gates

| Design requirement | Evidence status |
| --- | --- |
| R1 native FM coverage/capture | Static inventory only; original native model/capture probes and semantic hooks pending |
| R2 immediate event delivery | Event transport/journal/scheduler not implemented |
| R3 recognizable coordinated warnings | Specific media/system-TTS lifecycle mechanisms demonstrated; complete semantic speech/guidance integration pending |
| R4 silence and one audio owner | Existing provider contract retained; new owner/grant/failover implementation and integration tests pending |
| R5 truthful playback outcomes | Real callbacks and a selective pending-rejection mechanism observed; full system/neural/tone outcome adapters pending |
| R6 bounded recovery | New replay/grant persistence and uncertainty behavior pending |
| R7 compatibility/preferences | No production integration changed; mixed-version/new-protocol tests pending |
| R8 source-to-output timing, phone and car | These isolated spikes are not that chain; full acceptance remains pending |
