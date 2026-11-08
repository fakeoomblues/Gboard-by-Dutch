# Maps voice-only guidance recovery implementation plan

> **For agentic workers:** Use superpowers:executing-plans inline. Complete the connected source/tests before the grouped checkpoint. Preserve the approved HUD design and frozen maintained validation tooling.

**Goal:** Resume native Maps guidance after an external warning without selecting or inspecting the upcoming map maneuver.

**Architecture:** Add a guarded native voice-recovery interface to the existing `bmon` owner. Its implementation reuses the voice-request branch of original `u(ycx)` while leaving camera mode, selected step and map state untouched. The Java coordinator requests that capability after its existing hold/focus/grant fence.

**Tech Stack:** Java/Robolectric, Morphe Kotlin/DEX bindings, unchanged native speech factory/queue and accessibility policy.

**Spec:** `docs/contracts/navigation-indicators-v1.md`; original Maps26.44 shipped DEX and AA17 blueprint.

## Evidence and constraints

- Original `bmon.u(ycx)` explicitly selects `bmml.d`, whose shipped enum name is `INSPECT_STEP`, sets the selected step and refreshes map presentation before requesting voice. It is not an audio-only replay API.
- Patched205 showed a white upcoming-maneuver arrow on the A10 while actual GPS and remaining-distance calculations were still on the A4. Its genuine FM average/speed panel rendered correctly.
- Original-stock206 followed the blue current-position arrow on the same first70A4GPS points. Stock used a phone navigation URI;205used car recents/Start. Keep this control limit explicit until matched recovery/trace evidence closes attribution.
- Preserve native current-model reads, accessibility/touch-exploration eligibility, native speech factory/queue/priority and native navigation-active checks. Never invent speaker readiness, grants, route text or observation freshness.
- No changes to maintained observers, JSON fixtures, runners or validation frameworks. Ordinary product unit regressions accompany this batch. A bounded private goal-required probe may observe recovery and native step inspection without changing their results.
- Reuse Vela/FM185 data proof, approved generic designs, and unchanged release/Medium checks. Current Vela density and other screen/input/lifecycle gates remain open.

## Connected implementation

- [x] Add `MapsGuidanceCoordinator.NativeReplay` with native eligibility and voice-only recovery methods; remove the recovery call to original `u`.
- [x] Add `MapsVoiceResume.kt`: validate the original complete method fingerprint, preserve exact native call opcodes/references, and install capability/methods only on the guarded native owner.
- [x] Preserve the user-facing original step-inspection method unchanged.
- [x] Add unit coverage for fresh recovery, native eligibility, ended navigation, unsupported owner, exactly-once release, and absence of camera writes in generated DEX. Keep the existing native-shape fixture classes unchanged.
- [x] Run canonical verification with the actual original Maps APK available to the native DEX regressions; retain all required gates and justified reuse.
- [ ] Build/sign one immutable review MPP, patch the exact original Maps APK with default guards and verify signer/code/icons.
- [ ] Run one bounded original-AA acceptance with the same car Start/FM/GPS scenario, observing the voice-only recovery and original inspection entry points. Prove current A4 following during genuine warning playback; stop GPS as the next tool action if any unexpected frame appears.
- [ ] Retain genuine images, source/tool identities and failure limits, stop owned runtime and clean terminal build/scratch storage. Do not declare full AA completion while other explicit gates remain open.
