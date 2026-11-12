# Gboard

**App version:** 18.6.3.1002251336-release-arm64-v8a · **Build:** 176071766 · **Original file:** APK

[Use your original APK](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Select Google's arm64-v8a APK for this exact build (Android 12L+).

## Patches

| Patch | Default | What it does |
| --- | --- | --- |
| **Parakeet voice typing** | On | Adds **Parakeet voice typing** to Gboard settings → Voice typing. Once you download a model there and switch it on, the microphone key transcribes on the phone with NVIDIA Parakeet instead of Google's speech recognition. Each sentence is recognised in the language you speak it in, so Dutch and English work in the same dictation without switching. Until a model is ready, Gboard keeps using Google's voice typing. The patched Gboard gets the Parakeet launcher icon. |
| **Rambler voice typing** | On | Adds Google's **Rambler** to Gboard settings → Voice typing, as a choice next to Parakeet (Gboard's Standard voice typing). With Rambler picked, the microphone key sends your speech to Google's servers, which return finished text and carry out spoken edits such as "delete the last sentence". Parakeet stays the default. |
| **Rambler personal dictionary** | On | Adds Rambler's **Voice correction list** to Gboard settings → Voice typing → Rambler: names and words you add, import or export, which Rambler receives with every dictation so it spells them right. Requires Rambler voice typing. |
| **Rambler contact names** | On | Fills the voice correction list with your contacts' names. Names you delete are not added again; new contacts are added the next time Rambler starts. Requires Rambler personal dictionary and contacts access. |
| **Change package name** | On | Installs the patched Gboard under its own package name (app.morphe.android.inputmethod.latin by default), so it sits beside the stock Gboard that most phones have built in. Your stock Gboard, its settings and its dictionary are not touched; choose the patched keyboard in Android's keyboard settings. |
| **Custom branding** | Off | Changes the app name shown in the launcher, Android's keyboard list and app information, and the launcher icon (Parakeet by default, Gboard's original or five other designs), so the patched keyboard is easy to tell apart from the stock one. |
| **Clipboard Enhancements** | On | Controls how long copied snippets stay available, how many are kept, how many preview lines a card shows, and whether cards show a countdown, creation time, order number and column count. |
| **Web Clipboard** | On | Hosts a small Web Clipboard portal on the phone so a desktop browser on the same local network can exchange clipboard text with Gboard. The portal is protected by a pairing code and can be reached from a Quick Settings tile. |
| **Toolbar Editing Buttons** | On | Adds Select All, Copy, Cut and Paste to Gboard's menu so they can be dragged onto the top toolbar and used across supported keyboard languages and editors. |
| **Cursor trackpad mode** | On | Long-press the space bar to move the cursor more precisely with a trackpad and use the lock mode. |
| **Incognito Mode Toggle** | On | Adds an Incognito toggle to the Access Point toolbar and lets you choose whether the clipboard and voice typing stay available while Incognito is active. This controls Gboard's own incognito state; it is not a network-wide tracking blocker. |
| **Simple Calculator** | On | Type an arithmetic expression in a text field and see a scrollable equation with its answer in Gboard's suggestion row, ready to insert. |

Clipboard Enhancements, Web Clipboard, Toolbar Editing Buttons, Cursor trackpad mode, Incognito Mode Toggle and Simple Calculator are ported from the GPLv3 [Gboard Patches](https://github.com/jasonwu1994/Gboard-patches) project and updated for Gboard 18.6.3.

## Choosing a model

| Model | Download | Best for |
| --- | --- | --- |
| Parakeet Ultra (recommended) | about 630 MB | Moondream's further-trained Parakeet TDT v3. More accurate, especially with background noise. |
| Parakeet TDT 0.6B v3 | about 670 MB | NVIDIA's original model, as published by the sherpa-onnx project. |

Both models understand 25 European languages, including Dutch, English, German, French and Spanish, and pick the language per sentence by themselves. Punctuation and capitals are added automatically. The first download also fetches the speech runtime and speech detection (about 40 MB). Every file is checked against a fixed checksum before it is used. After that, voice typing works offline and your audio never leaves the phone.

## Dictating

Tap the microphone key and speak. Text appears while you talk and is settled when you pause. Dictation stops after five seconds of silence or when you tap the microphone key again. Gboard asks for microphone access the first time, as usual.

## Rambler voice typing

Rambler is Google's own voice typing with Gemini: instead of writing down every word, it turns what you say into finished text and follows spoken corrections. Google only switches it on for some Pixel phones; this patch offers it on other phones too.

After applying the patch, open Gboard settings → Voice typing and choose **Rambler** or **Parakeet**. Parakeet, Gboard's Standard voice typing renamed by the Parakeet patch, is selected until you change it. Without a Parakeet model it uses Google's standard recognizer.

- Rambler needs an internet connection and sends your speech to Google. Google limits how much you can dictate per day.
- Parakeet only handles the Parakeet choice. When Rambler cannot reach Google, Gboard falls back to its own standard Google recognizer, not to Parakeet.
- Whether Google's servers accept Rambler from a phone outside its rollout is decided by Google. On a OnePlus phone with these patches it works, including Dutch and spoken corrections.

### Rambler's personal dictionary

With **Rambler personal dictionary** applied, Gboard settings → Voice typing → Rambler shows Gboard's own **Voice correction list**. Rambler receives these words with every dictation and uses them to spell names and unusual words correctly.

- Open the list's menu and choose **Import** to add words: type or paste names, separated by commas or new lines. **Export** shares the list as text.
- Select words to delete them, or use **Clear all corrected words** to empty the list.
- **Contact names** asks Gboard for access to your contacts. With **Rambler contact names** applied, your contacts' names are then added to the list the next time Rambler starts, and new contacts are added later. Names you delete stay deleted. Contacts without a name, e-mail-only entries and phone numbers are skipped.
- Gboard's own personal dictionary under Settings → Dictionary stays for typing only, as Gboard says on that screen.

## Before using

- A 64-bit ARM phone is required. Keep about 1.4 GB free while downloading; the installed model takes about 650 MB.
- The model uses roughly 700 MB of memory while you dictate and is unloaded after five idle minutes, so the first sentence after a break takes a moment longer.
- With Change package name on, the patched Gboard is a separate app with its own settings: enable it under Android settings → Keyboard, then pick it. Without that patch it cannot be installed on phones that have Gboard built in. Features that depend on Google's own app signature may not work in the patched copy; Parakeet does not need them.
- The twelve Gboard patches were verified to build and apply against this 18.6.3 build. Patch application is not the same as a full device test of every feature.
- Checked on an Android 14 emulator: the settings entry, the download, and Dutch, English and mixed Dutch-English dictation into a text field, plus silence and background noise without stray text. Dictation on a phone has not been confirmed yet.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md)
