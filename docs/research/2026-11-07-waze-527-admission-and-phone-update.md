# Waze 5.27 admission and phone update checkpoint

## Native adapter admission

The supplied original Waze 5.27.0.1 (1030748) base APK has SHA-256
`0a66289f3db3ad575ef76ea2bac88724ed49ec8afa6f2147955d93e992750397`.
The existing production `validateWazeWarningClassAbi` and
`validateWazeWarningMethods` validators accepted its complete required class,
method-body and selector contracts without changing any fingerprints or
skipping a guard. The temporary read-only original-artifact probe was removed
after retaining its result locally.

The runtime admission now accepts this exact version/code pair alongside
5.26.90.901 (1030746). Unknown or mismatched pairs remain rejected. Admission
still requires `bound()`: an unpatched extension cannot advertise installed
native hooks. Android compilation and unit checks passed in batch100, including
the exact-pair admission test. Python regression checks also passed; their
existing 45 skips do not establish Android runtime behavior.

## Physical phone update

The owner authorized in-place navigation app updates through the phone's
existing Morphe Manager and normal DHU/screenshots/logcat without Frida on
this unrooted phone. Emulator observation requirements remain unchanged.

Morphe installed Vela 0.5.2621 and Flitsmeister 15.0 using the signed local
engineering source 1.53.2. Both installed APK signing certificates match the
previous installations. Vela's 19 icon assets match the tracked assets, and the
installed FM DEX contains the current early-request and native maximum-speed
bindings. App data was not cleared and neither app was uninstalled.

Waze and Maps updates and the phone DHU acceptance remain open. Another
workflow changed the focused Morphe search to an unrelated GitHub patch screen;
navigation input stopped to avoid operating on that app. No Spotify or GitHub
patch/install was initiated by this navigation workflow.

The accepted Maps and Vela designs remain frozen. Compilation, bytecode guards
and successful installation do not establish driving alerts, speech, a full
average-speed section, or head-unit behavior. The remaining acceptance must use
genuine provider events with only GPS movement simulated.

## Local evidence

Compact receipts are retained under `.analysis/waze-527-guard-probe-99` and
`.analysis/phone-current-batch-100`. The completed batch's build outputs were
removed after archiving test reports and verifying that its process group had
stopped and no build files were open. Reusable input APKs and the signed review
bundle remain available for the pending Manager updates.
