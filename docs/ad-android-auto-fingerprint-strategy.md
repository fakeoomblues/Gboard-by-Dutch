# AD Android Auto nieuws — obfuscation-resistant fingerprint strategy

Status: design, not implemented. Evidence base: AD 9.22.0 (build 9220000,
`be.persgroep.android.news.mobilead`, `kubusapp` application skeleton, the two
known DPG publisher certificates), decompiled with jadx on 2026-11-05 and
compared against the Volkskrant 7.53.1 Android Auto implementation
(`patches/volkskrant/CarNieuwsPatch.kt`, `extensions/volkskrant`).

## Why AD needs a different strategy

Parool and Trouw 7.53.1 ship the Amalia car SDK unminified: their
`classes6.dex` car stack is byte-identical to Volkskrant's apart from R8 build
map IDs, so the Volkskrant fingerprints port verbatim. AD 9.22.0 is a rebuilt
app generation: R8 renames the browse layer to short `defpackage` names while
serialization keep-rules preserve the data models. Verified renaming:

| Volkskrant/Parool/Trouw anchor | AD 9.22.0 state |
| --- | --- |
| `AmaliaAudioPlayerService` (+ manifest `MediaSessionService`/`MediaBrowserService`) | **kept** |
| `CarTabUi`, `CarTabSdUiResponse`, `CarAudioQueue`, `CarItemTrackingArgs`, radio models | **kept** (kotlinx.serialization) |
| `AudioItemContentResponse` DTO family, `ImageDto`, `AmaliaTtsDisclaimerResponse` | **kept** (26 string references) |
| `CarAudioQueue$Companion.fromJsonString` | **kept** |
| `CarApi$getTabStart$…` coroutine | renamed (`vo2`/`to2` shape) |
| `RootController.get` | renamed (`jq2` browse controller) |
| `CarMediaItemMapper`/`TabMapper`/`QueueMapper` | renamed (`nq2`, `mp2`) |
| `AmaliaResult$Success` | renamed (`m10`/`l10`/`k10` hierarchy) |
| `CarMediaSessionCallbackDelegate.readLaneQueue` | renamed (uses the kept `LANE_QUEUE_ITEMS` extra) |

Short names can change on every AD release, so name-based fingerprints are
unusable for the renamed half. The strategy below never relies on a minified
name surviving.

## Pillars

### 1. Hook stable-named classes directly

The service init hook is unchanged: `AmaliaAudioPlayerService.onCreate` keeps
its name and its manifest registration on every R8 pass, because Android
manifest references cannot be renamed. `init(Context)` from the extension
attaches there exactly as in the Volkskrant patch.

### 2. Resolve renamed hop points by surviving constants and type references

Every obfuscated hop point is found at patch time through an anchor R8 must
keep, then verified by instruction shape before any injection:

- **Start-tab wrap point** (Volkskrant step 2): find the suspend-lambda
  `invokeSuspend` whose body deserializes `CarTabSdUiResponse` and constructs
  the single success wrapper around it. Anchor: the method's references to
  `CarTabSdUiResponse` plus exactly one construction whose type is the local
  success wrapper; the wrapper class is discovered from that construction, not
  from a name.
- **Tab title and route** (step 3–4): the browse controller builds the root
  `MediaItem` list with the constants `"/start"`, `"/podcast"`, `"/radio"` and
  the authority `"amalia.android.auto"` (present verbatim in AD 9.22.0). Anchor
  on the `UriMatcher` construction or the `const-string "/start"` site within a
  method returning `List<MediaItem>`; the title rewrite targets the resolved
  string call feeding the start tab's builder.
- **Queue delivery** (steps 5–6): the bundle extra key `LANE_QUEUE_ITEMS` and
  the decoder `CarAudioQueue$Companion.fromJsonString` both survive. Anchor the
  read path on the `getString("LANE_QUEUE_ITEMS")` call site; the browse-row
  slimming hook anchors on the queue mapper call whose result feeds
  `CarLaneItemQueue`-shaped objects (discovered by field shape, not name).
- **Row synthesis** (extension runtime): the DTO constructor for rows is
  reached reflectively via the kept `AudioItemContentResponse` class name and
  the surviving `"spoken_article"` / `"Playlist"` / `"Nieuws"` constants, as
  the Volkskrant extension already does.
- **Queue commit / player publication** (steps 8–10): anchor on
  `androidx.media3.common` types (`Player$Commands`, `Timeline`,
  `MediaItem`) and the notification command provider discovered as the only
  method returning `Player$Commands` from the media service package.

### 3. Fail closed per build

Each structural fingerprint carries the same contract assertions as the
Volkskrant patch (`"… contract changed"` checks): expected instruction shape,
single match, register/opcode expectations. A renamed or regressed anchor
aborts the patch with a named error instead of mis-patching. AD targets stay a
single `AppTarget` entry; no multi-version tolerance, so every new AD release
re-runs the assertions during patching.

### 4. Re-derive the feed anchor per release

The `kubusapp` skeleton does not share the `nl.persgroep.edition`
`OverviewPageRepositoryImpl` feed path the Volkskrant runtime uses
reflectively. A future AD patch must first locate AD's own overview repository
and text-to-speech DTOs in a bounded decompile pass (the same
download → jadx → anchor-verify loop used for this design) and wire the
extension's feed walk to those names. Until that pass lands, no AD Android
Auto patch is published.

## Maintenance cadence

The three 7.53-line apps keep name-based fingerprints and only need the
regular latest-release check. AD needs the bounded decompile pass above per
release. The constants in this document (`amalia.android.auto`,
`LANE_QUEUE_ITEMS`, `spoken_article`, `/start`) are the tripwires: if a future
AD build drops one, the strategy must be re-evaluated before patching.
