# Vela native Android Auto acceptance

## Current boundary

The unchanged patched Vela 0.5.2621 APK now renders its native map and completes
Search, AA keyboard/results, route preview, Go, native turn-card guidance and
admitted moving GPS at 1280x480. A real native guidance content utterance reached
TTS request success, start and completion while emulator volume was zero and
DHU used SDL dummy audio. These are retained, fully observed cases before a
later global coverage failure. The complete run and the full all-app objective
remain incomplete.

Reconnecting at 800x480 returned the car UI to browsing, without its route/turn
card. That frame also predates the later coverage loss. Compare the original
app's route/disconnect behavior before changing Vela: its native behavior has
not yet been established for this transition. Tall-profile, 240dpi, complete
controls/scrolling/rotary/Back and external-warning audio modes remain open.
No source, observer, runner or application policy was changed in these checks.

## Exact inputs and original control

The original APK SHA-256 is
`f168bc730568b98ee8132131658d9220ef5be3557af41fd373bdaf104c31b20f`.
Its verified publisher certificate SHA-256 is
`29938b4858063e42e677ff95c901cd48248a7f032a3ae85f9b9e5617568b0d36`.
The patched checkpoint02 APK SHA-256 remains
`c9c1954ba23ac20124cef0f80a34c67d526ad739d80f0a0fd9daa03921a99f49`.
Its target extension payload is unchanged through canonical checkpoint28;
reuse the complete passing verification and real-input patch/sign/alignment
receipts instead of rebuilding unchanged source.

All eight ARM64 native libraries match byte-for-byte between original and
patched APKs, including MapLibre SHA-256
`d3a1ab9b9d056d84b93f2aa5f676d20d2d76723952dfcd8bd8d203d6eca6980d`.
This establishes native-library identity, not Java lifecycle equivalence.

Original-signed control53 rendered the actual AA map after the normal precise
foreground-location prompt. Its map frame was captured 54 seconds after car
selection, inside the planned 60-second window. Terminal observation handling
ended 46 seconds late; do not label the whole control a bounded acceptance pass.
The existing Frida CLI retained original Vela native crash/exit attachment;
patch-only Vela Java hooks do not exist in the original. The unchanged
coordinator observed the exercised AA/TTS/other patched processes. There was
no original crash before terminal stop. No original route/GPS/voice acceptance
or single causal patch attribution follows from this short map control.

## Historical encoder crash and software projection

Run31's MapLibre Snapshotter SIGSEGV maps to the original emulator GLES encoder
at relative PC 0x4215c. REA/Ghidra loaded this ELF at 0x100000; the mapped
instruction at 0x14215c is `ldr x8, [x0, #0x30]`. The recorded x0 is zero,
immediately after `GLSharedGroup::getBufferData(unsigned int)`. This identifies
the null buffer-data dereference, not why its record is absent. The encoder
SHA-256 is `cc70972b6f00814a24032bf630f65c52ad34a9ae7229fdf2d4f29efd3168d9b5`;
its retained BuildId matches the crash. Static evidence and the closed Ghidra
snapshot are retained privately. No driver or application binary was edited.

Run52 selected the documented SwiftShader software backend. Actual renderer
identity was ANGLE with SwiftShader/Vulkan. Genuine AA17/DHU connected using
initialized consent but projected only black frames, including after the known
focus refresh. Vela was not selected. Retained EGL_BAD_ATTRIBUTE messages are
diagnostic context, not an isolated cause. This failed projection prerequisite
does not establish a Vela fix or app acceptance. Use the working host backend.
The historical native crash did not recur in the later patched host window;
no causal fix or general non-recurrence is claimed.

## Run54 observation and delivery

The full unchanged observer installed 37 hooks in Vela, including its required
scheduler/speaker callbacks. Vela, TTS and AA hooks were ready before the native
content request. Content ID `vela--1194923203` had a successful 44-character
TTS request, native start at elapsed260783ms and completion at264941ms. Its
separate lead-in is not counted as another content utterance. This proves the
original synthesis/callback path, not heard physical-car audio or external
warning playback.

The final coordinator result records background Waze native shutdown and an
unexpected detach. Its callback reported api_started=false, lease0, wrapper
stopped, activity0 and car-session0. Do not attribute that unmanaged shutdown
to the API-owned expiry contract. All retained Vela map/search/route/GPS/speech
and failed reconnect frames precede the loss; the 800x480 frame precedes it
by about39seconds. Preserve the incomplete overall result. No further stimuli
followed the observed reconnect failure.

Runs52,53,54 restored and verified baseline APKs, audio, debug property,
automotive role and GPS, detached scripts, stopped exact owned servers and
runtimes, removed forwards and owned snapshots, and archived/removed scratch.
CleanupRequired=false. The initialized baseline and original inputs remain.
No physical phone was touched. The private review index contains exact
receipts, native methods/encoder evidence, screenshots and speech correlation.
