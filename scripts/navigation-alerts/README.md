# Native warning artifact contracts

Inspect the exact original APKs before adding semantic warning or speech hooks. This tool reads DEX with dexlib2; it never starts an app or modifies an APK. The `selection.json` lists raw DEX owners and method names. Every overload of a selected name is included. Missing methods/enums fail inspection.

Requirements: JDK 17+, the project's Gradle-resolved dexlib2/Guava/Gson jars. Use `--classpath` to select explicit jar paths; otherwise the tool selects matching jars from the local Gradle cache. No tools or dependencies are installed automatically.

From the repository root:

```bash
python3 scripts/navigation-alerts/inspect_bindings.py --artifact /absolute/path/to/original.apk --selection tests/fixtures/navigation-warnings/maps-selection.json --expect tests/fixtures/navigation-warnings/maps-contract.json
```

Use `flitsmeister` or `vela` selection/contract pairs for those artifacts. Extract FM's `base.apk` from the supplied APKM first. Input identities are recorded in [the binding report](../../docs/research/2026-11-02-native-warning-bindings.md).

FM additionally has an exhaustive coverage inventory:

```bash
python3 scripts/navigation-alerts/inspect_bindings.py --artifact /absolute/path/to/flitsmeister-base.apk --selection tests/fixtures/navigation-warnings/flitsmeister-selection.json --expect tests/fixtures/navigation-warnings/flitsmeister-contract.json --coverage tests/fixtures/navigation-warnings/flitsmeister-coverage.json
```

Add `--require-complete` to reject unsupported categories or entry points. This currently fails intentionally: capture and playback integration are pending. Required native entry points are frozen separately in the contract; deleting a manifest row fails validation. An exported row must reference evidence; an excluded/unsupported row must state its reason. The checker validates disposition shape, and cannot establish runtime truth from an evidence string.

`--output` writes a fresh inventory for review. Do not automatically overwrite the frozen oracle to accommodate an incompatible target. The method hash includes opcodes, widths, register operands, literals, references, branch/switch/array payloads and typed catch ranges, excluding debug metadata. Enum inventories contain raw ACC_ENUM field names; they may differ from runtime `Enum.name()` or JADX aliases. Initializer contracts retain those semantic mappings for Maps.

For private control-flow inspection, add `"include_instructions": true` to a copy of the selection. Keep resulting instruction dumps in ignored `.analysis/`, and use the ordinary selection when comparing frozen contracts. Offsets are obtained by summing instruction code-unit widths; catch rows are not instructions.

Tests:

```bash
python3 -m unittest discover -s tests -p test_navigation_warning_bindings.py
```

Exact matching does not prove insertion safety, warning eligibility, playback, routing, timing or compatibility after patching. Those gates remain in the reviewed design.

## Private native audio experiments

`build_native_probe.py` builds test-only same-signer Maps/Vela instrumentation. `run_native_probe.py` checks the original artifact digest, its non-signing ZIP payloads, the exact owned emulator/AVD, installed APK hashes and actual native results. It archives previous reports before a rerun and atomically publishes a new result only after successful collection. Raw results stay under ignored `.analysis/`.

`build_fenced_vela.py` produces a private candidate for the specific delayed system-TTS closure race, with exact three-hook verification. `probe/TaggedSpeechFence.java` is a test prototype; it is not part of any extension or release. The native probe's `scenario=fenced` compares tagged cancellation with retained untagged synthetic speech.

See [runtime evidence and reproduction](../../docs/research/2026-11-02-native-warning-runtime-spikes.md). Native model capture, real route regeneration, neural outcomes, audio ownership/replay, full phone/car coordination and complete-warning acceptance remain separate requirements.

`scenario=codec` compiles the production [shared protocol core](../../support/navigation-alerts/README.md) into instrumentation and verifies immutable round trips, native Parcel sizing, UTF-8 rejection and bounded batch splitting without speaking. It does not fabricate native FM capture or prove Binder authentication. The component verification snapshot is in `tests/fixtures/navigation-warnings/warning-core-verification.json` from the repository root.

`scenario=ledger_io` checks the real app-private writer lock, file permissions, 100 fsync/rename commits and descriptor counts. Run `ledger_write` then `ledger_recover` in separate instrumentation processes to check reload/failover fencing, new-consumer rejection and revoked STARTED handling. These fixtures never invoke an audio adapter. Recovery performs explicit provider-internal test cleanup because no real player was created; it does not prove quiescence of a production receiver. The ownership component snapshot is `tests/fixtures/navigation-warnings/ownership-verification.json`.

## Separate-UID Binder fixture

Build three disposable APKs without contacting a device:

```bash
python3 scripts/navigation-alerts/build_binder_probe.py --sdk "$ANDROID_HOME" --output .analysis/native-warning-bindings/binder-probe --key /path/to/private-test.p12
```

The source service permits the actual PackageManager UID of the allowed client; the stranger APK is denied. The client instrumentation checks the remote Binder descriptor, source callback UID, callback challenge, initial snapshot/live ordering, authenticated health PING and grant bookkeeping. Readiness is explicitly simulated and no speaker is created. The synthetic event's NOT_STARTED/quiescent report is justified by that absence of output.

Run only on the designated disposable `native_warnings_api34` AVD, verifying both its name and `ro.kernel.qemu=1` before installing. Each client APK supplies `nl.johninnl.warningprobe.BinderClientProbe` instrumentation targeting itself. Pass `-e unauthorized true` for the stranger. Retain source/build hashes, independently verified installed APK hashes and both instrumentation results. The observed component record is `tests/fixtures/navigation-warnings/binder-verification.json`; it does not establish FM capture, native speech or phone/car acceptance.
