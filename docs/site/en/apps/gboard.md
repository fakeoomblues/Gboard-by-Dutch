# Gboard

**App version:** 18.5.11.993608717-release-arm64-v8a · **Build:** 176050094 · **Original file:** APK

[Use your original APK](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Select Google's arm64-v8a APK for this exact build (Android 8.0+).

## Patches

| Patch | Default | What it does |
| --- | --- | --- |
| **Parakeet voice typing** | On | Adds **Parakeet voice typing** to Gboard settings → Voice typing. Once you download a model there and switch it on, the microphone key transcribes on the phone with NVIDIA Parakeet instead of Google's speech recognition. Each sentence is recognised in the language you speak it in, so Dutch and English work in the same dictation without switching. Until a model is ready, Gboard keeps using Google's voice typing. The patched Gboard gets the Parakeet launcher icon. |
| **Change package name** | On | Installs the patched Gboard under its own package name (app.morphe.android.inputmethod.latin by default), so it sits beside the stock Gboard that most phones have built in. Your stock Gboard, its settings and its dictionary are not touched; choose the patched keyboard in Android's keyboard settings. |
| **Custom branding** | Off | Changes the app name shown in the launcher, Android's keyboard list and app information, and the launcher icon (Parakeet by default, Gboard's original or five other designs), so the patched keyboard is easy to tell apart from the stock one. |

## Choosing a model

| Model | Download | Best for |
| --- | --- | --- |
| Parakeet Ultra (recommended) | about 630 MB | Moondream's further-trained Parakeet TDT v3. More accurate, especially with background noise. |
| Parakeet TDT 0.6B v3 | about 670 MB | NVIDIA's original model, as published by the sherpa-onnx project. |

Both models understand 25 European languages, including Dutch, English, German, French and Spanish, and pick the language per sentence by themselves. Punctuation and capitals are added automatically. The first download also fetches the speech runtime and speech detection (about 40 MB). Every file is checked against a fixed checksum before it is used. After that, voice typing works offline and your audio never leaves the phone.

## Dictating

Tap the microphone key and speak. Text appears while you talk and is settled when you pause. Dictation stops after five seconds of silence or when you tap the microphone key again. Gboard asks for microphone access the first time, as usual.

## Before using

- A 64-bit ARM phone is required. Keep about 1.4 GB free while downloading; the installed model takes about 650 MB.
- The model uses roughly 700 MB of memory while you dictate and is unloaded after five idle minutes, so the first sentence after a break takes a moment longer.
- With Change package name on, the patched Gboard is a separate app with its own settings: enable it under Android settings → Keyboard, then pick it. Without that patch it cannot be installed on phones that have Gboard built in. Features that depend on Google's own app signature may not work in the patched copy; Parakeet does not need them.
- Checked on an Android 14 emulator: the settings entry, the download, and Dutch, English and mixed Dutch-English dictation into a text field, plus silence and background noise without stray text. Dictation on a phone has not been confirmed yet.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md)
