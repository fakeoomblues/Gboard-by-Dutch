# Gboard

**App version:** 18.6.3.1002251336-release-arm64-v8a · **Build:** 176071766 · **Original file:** APK

[Use your original APK](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Select Google's arm64-v8a APK for this exact build (Android 12L+).

## Patches

| Patch | Default | What it does |
| --- | --- | --- |
| **Parakeet voice typing** | On | Adds **Parakeet voice typing** to Gboard settings → Voice typing. Once you download a model there and switch it on, the microphone key transcribes on the phone with NVIDIA Parakeet instead of Google's speech recognition. Each sentence is recognised in the language you speak it in, so Dutch and English work in the same dictation without switching. Until a model is ready, Gboard keeps using Google's voice typing. The patched Gboard gets the Parakeet launcher icon. |
| **Rambler voice typing** | On | Adds Google's **Rambler** to Gboard settings → Voice typing, as a choice next to Parakeet (Gboard's Standard voice typing). With Rambler picked, the microphone key sends your speech to Google's servers, which return finished text and carry out spoken edits such as "delete the last sentence". Parakeet stays the default. |
| **Rambler personal dictionary** | On | Adds **Voice dictionary** to Gboard settings → Voice typing, with Gboard's own **Voice correction list**: names and words you add, import or export, which Rambler receives with every dictation so it spells them right. Requires Rambler voice typing. |
| **Rambler contact names** | On | Fills the voice correction list with your contacts' names once you switch on **Contact names** under Voice dictionary. Names you delete are not added again; new contacts are added the next time Rambler starts. Requires Rambler personal dictionary and contacts access. |
| **Rambler custom instructions** | On | Adds **Custom instructions** to Gboard settings → Voice typing → Rambler. Your text is added to Rambler's own instructions, not a replacement for them: Rambler receives it with every dictation and follows it for style only, such as spelling, punctuation, vocabulary and tone. Requires Rambler voice typing. |
| **Rambler cursor-aware editing** | On | Rambler also sees the text after the cursor, so spoken edits reach the whole text and dictating in the middle of a text fits in. With text selected, only the selection is sent, so a spoken edit such as "make this more formal" applies to it, and the result is selected again for the next edit. Requires Rambler voice typing. |
| **Rambler app context** | On | Rambler also receives the name of the app you dictate in and the text field's hint, so it can match the tone of a chat, an e-mail or a search, and custom instructions can depend on the app. This tells Google which app you are typing in. Requires Rambler voice typing. |
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

### Voice dictionary

With **Rambler personal dictionary** applied, Gboard settings → Voice typing shows **Voice dictionary**, above the choice between Rambler and Parakeet. It holds Gboard's own **Voice correction list**: Rambler receives these words with every dictation and uses them to spell names and unusual words correctly.

- Open **Voice correction list**, then its menu, and choose **Import** to add words: type or paste names, separated by commas or new lines. **Export** shares the list as text.
- Select words to delete them, or use **Clear all corrected words** on the Voice dictionary screen to empty the list. Gboard asks for confirmation first.
- **Contact names** asks Gboard for access to your contacts. With **Rambler contact names** applied, your contacts' names are then added to the list the next time Rambler starts, and new contacts are added later. Names you delete stay deleted. Contacts without a name, e-mail-only entries and phone numbers are skipped.
- Gboard's own personal dictionary under Settings → Dictionary stays for typing only, as Gboard says on that screen.
- For now only Rambler uses the list; Parakeet does not read it yet.

### Custom instructions

With **Rambler custom instructions** applied, Gboard settings → Voice typing → Rambler shows **Custom instructions**. Tap it, type your rules and tap OK. Rambler then receives that text with every dictation.

- Your text is **added** to Rambler's own instructions; it does not replace them. Google's rules still come first: Gboard wraps your text in Google's own guard text, which limits it to how dictated text is written (spelling, grammar, language conventions, vocabulary, formatting, punctuation, capitals and tone) and to the text you just dictated or edited.
- Write style rules, not tasks. "Write every number in digits." works; "Answer in English" or "Summarise what I say" are ignored by design.
- A rule can depend on a word or a situation, such as "When I say 'new paragraph', start a new paragraph." Rambler applies it only when that condition is met.
- There is one text field: saving new text replaces your previous instructions. Leave it empty to send nothing.
- A quick check: save "Write every number in digits.", pick Rambler and dictate "ik heb drie appels". With the instruction it shows "ik heb 3 appels".

### Cursor-aware editing

Stock Rambler only sees the text **before** the cursor. With **Rambler cursor-aware editing** applied it also sees the text after it, and a selection becomes the focus of a spoken edit.

- **Edit anywhere in the text:** put the cursor anywhere, even at the very start, and say what to change: "change Tuesday to Wednesday", "delete the last sentence", "make the second sentence shorter". Rambler finds the words in the whole field, not only before the cursor.
- **Dictate in the middle:** put the cursor between two sentences and dictate. Rambler sees what comes after, so capitals, punctuation and spacing fit around the inserted text.
- **Edit only a selection:** select a sentence or paragraph and say an edit, such as "make this more formal" or "make this shorter". Only the selected text is sent and changed; the rest of the field stays as it is.
- **Keep editing:** after a spoken edit the changed text stays selected, so you can say the next edit ("shorter", "add an exclamation mark") without selecting it again. Tap elsewhere to deselect.
- Rambler receives more of the field this way: the text after the cursor (up to about 2,000 characters on each side of it), or only your selection when you have one.

### App context

With **Rambler app context** applied, each dictation also tells Rambler which app you are in: the app's name and package, the kind of field (such as e-mail address or search) and the field's hint text (at most 100 characters). Rambler uses it to match the style of the app, and custom instructions can refer to it.

- Rules per app work: for example "Only in WhatsApp: end every message with 👍." or "In Gmail, write complete sentences with a greeting."
- Google learns which app you are typing in. Rambler already receives your speech and the text around the cursor; switch this patch off if you do not want to share the app name as well.

## Before using

- A 64-bit ARM phone is required. Keep about 1.4 GB free while downloading; the installed model takes about 650 MB.
- The model uses roughly 700 MB of memory while you dictate and is unloaded after five idle minutes, so the first sentence after a break takes a moment longer.
- With Change package name on, the patched Gboard is a separate app with its own settings: enable it under Android settings → Keyboard, then pick it. Without that patch it cannot be installed on phones that have Gboard built in. Features that depend on Google's own app signature may not work in the patched copy; Parakeet does not need them.
- The fifteen Gboard patches were verified to build and apply against this 18.6.3 build. Patch application is not the same as a full device test of every feature.
- Checked on an Android 14 emulator: the settings entry, the download, and Dutch, English and mixed Dutch-English dictation into a text field, plus silence and background noise without stray text. Dictation on a phone has not been confirmed yet.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md)
