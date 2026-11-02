# Flitsmeister 14.17 native warning bindings

Static discovery for design sections 5 and 14/WP1. Semantic export, native hook probes and physical-device acceptance remain unimplemented/unverified. No product code or device state was changed by this investigation.

## Artifact and evidence

- Original supplied APKM base: `.analysis/native-warning-bindings/flitsmeister-original.apk`, SHA-256 `17f622c8072c9fd8bd121b872a3733a9a22aaff041d57560f6f3dd59a33ce1b7`.
- Prior analysis APK: `<primary-checkout>/.analysis/flitsmeister-broadcasts/base.apk`, SHA-256 `c1f923ce12054b1dde88b7a0cec50e60075ce6a5e578c3bdba0dcdc8bac5a7ac`. It is **not** the original package. All seven DEX members were compared and are byte-identical; manifest/resources differ. `.analysis/native-warning-bindings/fm-dex-comparison.json` retains that comparison. Reusing its JADX code is justified by DEX equality, not version-name similarity. Do not reuse its decoded resources/SDK facts as original evidence.
- Source paths below are relative to `<primary-checkout>/.analysis/flitsmeister-broadcasts/jadx/sources/`. They identify decompiler views; exact DEX identities, method flags/registers/instruction counts/body hashes are in `tests/fixtures/navigation-warnings/flitsmeister-contract.json`, generated against the original artifact. Decompiled `loaded from` comments can misidentify DEX placement; use the collector's actual member.
- REA historical-code evidence: `s6i.l` `ev_e5e89f52d0ce62922770055bad579b31787e08495991e262ff48d73aa2074e19`; `s6i.b` `ev_8a84db6c20686cbd4c5d79870284f860e8f0b765e59f96fc995cbdbeb8048a07`; `dz2.invokeSuspend` smali fallback `ev_a4d40e3d3a12b279678636a0f31036480d7939b27adf8ec0d23c9968504394a1`; `ez2.invokeSuspend` `ev_5f5b21dc085e7eacbb8187686c891bdf3e4f26df010c80dca29d57165f7e73d2`. These REA records bind the prior APK hash, with code reuse justified only by the explicit DEX comparison. REA display signatures do not establish exact DEX descriptors.

## Finding: the initial `s6i.l` boolean is sound selection

`defpackage/s6i.java:350-510` does **not** reject a semantic warning when `zBooleanValue` is false. Its false branch still calls sound-pack selection and then `t50.a("", originalOrNullText, ...)`. The true branch adds BLE alarm/tone handling before the same speech pipeline. Therefore suppressing public ANNOUNCE when this boolean is false would silently discard otherwise native eligible warnings. The service/slow helper Kotlin metadata explicitly names this logic `SoundManagerFM$isReportSoundActive`.

Capture original arguments before `g()` nulls speech text. Preserve native feature/relevance acceptance upstream. Do not reinterpret local tone, speech, BLE or sound-pack preferences as complete semantic eligibility.

| Parameter | Observed native meaning | Evidence |
| --- | --- | --- |
| `s6i.l` report | Concrete accepted report context | `fc0.J`, `fc0.x`, `fc0.y` |
| First boolean `z` | Start/enter versus stop/leave for trajectories, traffic and path incidents; true on point-warning calls | `fc0.java:988,1046,1138,1860`; `s6i.java:362-366,394-397,435-437` |
| Second boolean `z2` | Local audio mute bypass; reaches sixth `t50.a` argument (`z3`) | `s6i.java:492,510`; `t50.java:348-356`: when `!z3`, OFF plus `!z2` rejects local playback |
| `s6i.b` boolean | Same local audio mute-bypass argument, **not** early-warning enter/exit | `s6i.java:261-284`; regular `fc0.J` calls false; CITS `vz2` call passes true |
| String | Fully formatted native text before local spoken-warning setting | `fc0.y` wraps `IncidentType.getTTSString`, handles native free text; `fc0.J` early-warning descriptions/free text |
| Callback | Native completion behavior | Preserve invocation and lifetime; never call sound methods twice to query decisions |

`s6i.l` initial sound predicates map as follows. `wie` is implemented by `gm8`; most methods return a stored setting only when `sle.b(context)` permits customization, otherwise a native default. Do not bypass this effective-value wrapper by reading preferences directly.

| Branch/predicate | Exact native key | Classification in this method |
| --- | --- | --- |
| Traffic-camera `b38.Z1` | `pref_traffic_cam_warning_sound` (false default) | Local sound |
| Other SpeedCam `wie.c` → `kph.n` | `pref_signal_hotspot_signal_enabled` | Local sound; surprising literal mapping verified, do not rename it by assumption |
| Zone start `wie.g` / end `wie.t` | `pref_signal_avg_speed_trap_beginning` / `pref_signal_avg_speed_trap_end` | Local sound |
| Traffic start `wie.y` / end `wie.F` | `pref_signal_traffic_jam_beginning` / `pref_signal_traffic_jam_end` | Local sound |
| Roadworks start `wie.v` / end `wie.j` | `pref_signal_road_works_beginning` / `pref_signal_road_works_end` | Local sound |
| Service `t6i.invokeSuspend` | DataStore `pref_service_vehicle_warning_sound_enabled` (true) | Local sound; surrounding constructed feature keys are unused here |
| Slow vehicle `u6i.invokeSuspend` | DataStore `pref_slow_moving_vehicle_warning_sound_enabled` (true) | Local sound |
| Other models | true | No extra initial sound predicate |

The callback body is compiled as `q6i.invoke`, although JADX inlines it under `s6i.l`. Its complete branch decisions are:

- BLE success plus `pref_fm_device_use_as_audio_source` suppresses phone sound; connected FM Dot also invokes completion and returns. This is output routing, not warning removal.
- Zone chooses start/stop trajectory recording; traffic chooses safety/completed; traffic camera chooses information recording.
- SpeedTrap/SpeedCam choose DEFAULT/LOUD/SILENT through `wie.Q/K` → `pref_signal_flitser_harder_new` / `pref_signal_flitspaal_harder_new`. SILENT produces empty recording but does not erase native speech text.
- PoliceHotspot tone uses `kph.o` → `pref_enable_hotspot_signal`.
- Incident/CITS tone umbrella `wie.q` → `pref_enable_incident_signal`; CitsLaneClosed additionally `wie.R` → `pref_cits_lane_closed_enabled`; CitsBridgeClosed uses `s6i.c(BRIDGE_CLOSED)`.
- ROADWORKS/TRAFFIC choose delay/completed when a GPS path is present; ROAD_CLOSURE additionally `wie.S` → `pref_signal_incident_type_road_closures`; WRONG_WAY_DRIVER has its dedicated recording; OBSTACLE uses `wie.e` → `pref_signal_incident_type_obstacle`; ACCIDENT uses `wie.n` → `pref_signal_incident_type_accident`; BRIDGE_OPEN uses `s6i.c`; CONTROL uses `kph.l` → `pref_enable_signal_control`; ANIMAL_ON_ROAD uses `wie.H` → `pref_enable_incident_animal_signal`; SLIPPERY_ROAD uses `kph.w` → `pref_weather_conditions_signal`; CLOSURE_WITH_REROUTE has no generic recording. Remaining incident types use the safety recording.
- CitsBaseReport without those special cases has no recording in this callback. Other models use safety recording.
- `s6i.g` → `b38.g` → `pref_enable_custom_spoken_warnings` (false) nulls text; `w7i.d` chooses sound packs; `t50.a` applies playback state. These are local output decisions.

Several keys also serve native feature gating upstream (for example obstacle/accident/lane closure). Their use **inside a tone-selection branch** is not sufficient to classify the warning as ineligible. Let the native accepted path decide, rather than globally replaying these predicates in a new exporter.

## Native feature/relevance authorities

| Authority | Observed decisions |
| --- | --- |
| `fc0.J`, `defpackage/fc0.java:570-649` | On entry: enabled flags A1/z1/B1/C1 for camera/traffic/zone/hotspot; camera ID exclusion and `SpeedCamType.isSpeedCamTypeMutedFromCategory`; incident ID exclusion; existing-alert suppression; cameras may repeat after native three-minute interval. Lane closure checks entitlement and category and suppresses nearby matching repeats before `s6i.l`. |
| `fc0.P`, `fc0.java:1331-1356` | Refreshes flags via `wie.r/L/O/s` → `pref_filemeldingen_enabled`, `pref_signal_paal`, `pref_signal_traject`, `pref_fm_pro_hotspots_enabled`. |
| `SpeedCamType.isSpeedCamTypeMutedFromCategory`, model source:400-421 | Uses native `pref_muted_speed_cam_categories`; category grouping must stay native. |
| `f1a.i`, `defpackage/f1a.java:377-379` | Requires `Incident.getShouldSignal`; excludes HEAVY_TRAFFIC and CLOSURE_WITH_REROUTE from this ordinary incident-task signaling path. This does not prove every alternate path excludes them. |
| `xl.l`, `defpackage/xl.java:469-514` | Suppresses when both DataStore `pref_user_has_dash` and `pref_dash_mode_all_warnings_disabled` are true. Applies before `xl$c` regular/VOICE_ONLY path. |
| `nh7$c.onEarlyWarningNotificationUpdate`, `defpackage/nh7.java:177-300` | Provider allowed/active state, country/feature conditions, `qh7.a` category decisions; emits removal/null when no accepted warning remains. Retains previous state and uses equality to select new vs update. |
| `qh7.a`, `defpackage/qh7.java:85-121` | Emergency FIRE/AMBULANCE/POLICE/ROAD_SERVICE/ACTION_CAR/SERVICE_CAR → `wie.U` (`pref_safety_vehicles`); four railway variants → `wie.d` (`pref_railway_crossings_enabled`); bridge → `wie.b` (`pref_bridge_openings_enabled`); ROAD_WORKER/TEXT_CAR/TRAFFIC_CONTROLLER → `wie.i` (`pref_signal_incident_type_road_works`); SLOWING_DOWN_TRAFFIC/ADVICE/UNKNOWN true. Null preference interface returns true. |
| `vz2.U`, `defpackage/vz2.java:483+` | CITS source/country/event filtering; one branch delegates to `m71.a`. Keep this native decision, including its alternate country/event allowlists. |
| `m71.a`, `defpackage/m71.java` | Accident `wie.n`, emergency U, traffic r, broken-down/stationary p, closure S, slippery k, obstacles e, person G, animals a, bridge b, roadworks i; service/slow use separate helpers below. These are actual event signaling gates. |

True service-vehicle feature eligibility is `m71.a` → `dz2.invokeSuspend`, **not** `t6i`. REA smali recovers the otherwise failed Java decompilation: read master `kpg.a`, reject if false, map the native report, then read its corresponding subtype key. `kpg` declares true defaults for `pref_service_vehicle_warnings_enabled`, `pref_common_service_vehicle_warnings_enabled`, `pref_roadside_assistance_warnings_enabled`, `pref_tow_service_warnings_enabled`, `pref_road_inspector_warnings_enabled`, `pref_gritter_service_warnings_enabled`, `pref_impact_attenuator_vehicle_warnings_enabled`. An Incident produced by the maintenance mapping with alert code `804` additionally checks `pref_mower_warnings_enabled`; other mapped Incidents pass the master. Unknown/nonreport mapping rejects.

Slow-vehicle feature eligibility is `m71.a` → `ez2.invokeSuspend`: DataStore `pref_slow_moving_vehicle_warnings_enabled` has false default. Independent original DEX decoding resolves the decompiler's uninitialized-local artifact: the native `y1i.c.a` flag is read into `v6`; `if-nez v6` at CU `0x004b` jumps to `0x0061`, setting the return boolean false. When the flag is false, the coroutine reads the DataStore preference and returns that boolean. Thus this path requires both the native flag to be false and the preference to be true. The flag's product meaning remains unknown. The contract freezes this body; `.analysis/native-warning-bindings/slow-instructions.json` retains the private instruction inspection. Existing accepted calls avoid duplicating this coroutine.

## Early warning and verified bypasses

`s6i.b` resolves an effective type from notification, else report, else argument for BLE. Its callback `p6i.invoke` still selects the sound using the original type argument. Capture all three fields and preserve a mismatch diagnostic; do not silently assume they always agree.

`s6i.c` maps local signal settings: FIRE/AMBULANCE/POLICE → `wie.E` / `pref_enable_safety_vehicles_signal`; BRIDGE_CLOSED → `wie.w` / `pref_enable_bridge_openings_signal`; all four railway variants → `wie.u` / `pref_enable_railway_crossings_signal`; ADVICE → `kph.v` / `pref_weaving_section_sliedrecht_signal`; all other enum values true. False removes the initial recording, **not** formatted text or sound-pack processing. `p6i` then handles BLE/FM Dot routing and sound packs like the regular path.

There are three distinct buses, not two interchangeable announcement feeds:

1. `AppNotificationBus.getBusEarlyWarningNotification()` carries `EarlyWarningNotification` and null removal (`nh7$c`); native equality suppresses unchanged publication.
2. `EarlyWarningsBus.INSTANCE.getBusAlert()` carries `EarlyWarning` state (title, subtitle, heading, location, icons, closable/showNow), including impact attenuators (`cz2.a/b`) and converted CITS early warnings (`vz2.J/K` (native added/deleted interception)). The model does not carry EarlyWarningTypeEnum or a stable report ID; retain source context before conversion when needed.
3. `AppNotificationBus.getBusEarlyWarningNotificationEventAA()` is an Android Auto presentation projection from `xl.k/m`; it is not a separate danger.

Verified announcement bypasses requiring context-aware branch hooks:

| Bypass | Native behavior and proposed capture boundary |
| --- | --- |
| `xl$c.invokeSuspend` VOICE_ONLY | After coroutine suppression returns false, branches on IncidentType.VOICE_ONLY, takes `getVoiceOnlyText`, calls `g()` and `t50.a` directly, then `fc0.N`; never enters `s6i.l`. Hook only the accepted VOICE_ONLY branch before `g()`; coroutine entry executes before suspension and is unsafe as ANNOUNCE authority. |
| `fc0.x` CitsBaseReport | Calls `t50.a` directly with SAFETY_NOTIFICATION and empty-or-null text; bypasses `s6i.l/b`. Capture accepted report context before this local sound branch and resolve meaningful model text separately. Decompiled duplicated CITS blocks require DEX branch inspection; do not infer duplicate native announcements from duplicated Java. |
| `xl.k(Context,EarlyWarning,boolean)` | Non-null plus `z=true` produces direct safety recording/vibration and UI/AA handling; `z=false` refreshes presentation without that sound; null removes notification. Impact attenuator path is `cz2.a` → EarlyWarningsBus → `xl.k(true)`; it never enters `s6i.b`. |
| Overlapping CITS early warning | `vz2.J` (native added interception) can call `s6i.b(...,true)` then EarlyWarningsBus, `xl.k(true)`, and `xl.m(true)` for one converted report. Correlate one native warning instance; exporting every tone/bus observation creates duplicates. |

`xl.m` constructs Android Auto/UI content for a subset of notification types, without its own native speech call. Do not announce its formatted AA projection again. `cz2.a` speed advice publishes `VRISpeedAdviceBus` state when native conditions permit; a native announcement path for every speed-advice variant is not established. Keep display/state distinct from ANNOUNCE.

## Coverage and hook safety

The artifact inventory contains 19 BaseReportType, 35 IncidentType, 82 SpeedCamType, 17 EarlyWarningTypeEnum, and six ServiceVehicle.Type values (159 total). Preserve the literal misspelling `CITS_BRIDE_CLOSED` in raw identity. `tests/fixtures/navigation-warnings/flitsmeister-coverage.json` accounts for each member explicitly with candidate path and category-specific blockers; every status remains `unsupported` because semantic export/native probes are pending. No commercial/personal enum is excluded merely by its name. BASEREPORT/LBA/CITS_PARKING/FM_OFFICE/PARKED_LOCATION/TOLLING/ENVIRONMENTAL_ZONE need their actual producer/consumer audit before an exclusion is justified. `EarlyWarningReport` returns INCIDENT but is not ordinary Incident; model-family dispatch must precede subtype access. CitsRoadWorks also returns INCIDENT.

The collector fixture gives exact descriptors, including:

```
Ls6i;->l(Lnl/flitsmeister/fmcore/data/model/reports/BaseReport;ZZLjava/lang/String;Lz39;)V
Ls6i;->b(Lnl/flitsmeister/earlywarning/model/EarlyWarningTypeEnum;Lnl/flitsmeister/fmcore/models/data/reports/EarlyWarningReport;Lnl/flitsmeister/earlywarning/model/EarlyWarningNotification;Ljava/lang/String;ZLz39;)V
Lxl$c;->invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
Lfc0;->x(Lnl/flitsmeister/fmcore/data/model/reports/BaseReport;)V
Lxl;->k(Landroid/content/Context;Lnl/flitsmeister/fmcore/models/data/earlywarnings/EarlyWarning;Z)V
```

Proposed insertion safeguards, not demonstrated patched behavior:

- Resolve and verify every target before mutation. Validate all exact descriptors, flags, normalized body/catch hashes and expected invoke references against the supplied APK.
- Preserve p-registers and every native boolean/callback. `s6i.l/b` have more than five argument words including receiver; use valid range/bridge invocation or explicitly reserved contiguous locals rather than an oversized 35c call. Never assume low parameter registers or spare locals.
- Keep capture small, nonblocking and exception-contained. Do not read DataStore by starting another native coroutine from a hook; observe native acceptance or already-computed decisions. Do not call `s6i`, BLE, sound-pack or playback methods a second time.
- For coroutine bypasses, bind the resumed accepted branch and report field, not every `invokeSuspend` entry. Preserve state labels, exception handlers and native completion exactly.
- Match callback classes `p6i/q6i` separately because JADX inline presentation hides the actual DEX boundaries. Added monitor callbacks cannot prove scheduling/audio outcomes.
- Distinguish `fc0.J/O` state from ANNOUNCE. Handle native disappearance/null removal and deduplicate overlapping EarlyWarningReport/notification/bus/AA observations. A genuine repeat decision needs its own announcement index.

Outstanding blockers: no instrumented acceptance/capture traces, no original-resource language/text extraction for all 159 variants, unresolved nonroad-family producer audit, the slow-helper native flag's product meaning, complete CITS source/path coverage, no proof of immutable event identity/deduplication under concurrent callbacks, and no receiver delivery/audio timing proof. Static DEX equivalence and valid fingerprints establish discovery reproducibility, not semantic or runtime completeness.
