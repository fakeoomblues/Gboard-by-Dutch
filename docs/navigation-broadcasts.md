# Navigation broadcasts, schema 1

The default-selected **Broadcast navigation data** patches for Waze 5.26.90.901 and Flitsmeister 14.17 publish the same primitive-only Android Intent schema. A consumer can register once for `nl.johninnl.navigation.NAVIGATION_STATE` and use the same keys for either provider. No proprietary Parcelable, overlay, visible Activity or account identifier is required by the transport.

## Transport and provider selection

- Common state: `nl.johninnl.navigation.NAVIGATION_STATE`.
- Common snapshot request: `nl.johninnl.navigation.NAVIGATION_STATE_REQUEST`.
- Waze aliases retained: `nl.johninnl.waze.NAVIGATION_STATE` and `_REQUEST`.
- Flitsmeister aliases: `nl.johninnl.flitsmeister.NAVIGATION_STATE` and `_REQUEST`.

Choose the common state action for new receivers. Both aliases and the common action carry identical extras/session/sequence for the same event; subscribing to both requires deduplication. Existing Waze receivers continue to work. `source_package` identifies `com.waze` or `nl.flitsmeister`; maintain state separately for each provider and choose the preferred provider in the consuming app. Never combine the speed of one app with the zone of the other.

Register an exported dynamic receiver in the consumer's running service. A manifest receiver can receive explicitly targeted messages when the patch option `receiverPackage` names that package. Blank means implicit local broadcasts to running receivers. Android background/stopped-package rules still apply.

Request a snapshot with `new Intent("nl.johninnl.navigation.NAVIGATION_STATE_REQUEST").setPackage(providerPackage)`. Requests are throttled to one per second per provider. They do not start a stopped app or refresh observation timestamps. Normal updates are capped at one per second, status/limit/segment changes may publish immediately, and fresh data receives a five-second heartbeat. Stale values are omitted.

Android 14+ uses the framework share-identity broadcast option. Validate `BroadcastReceiver.getSentFromPackage()` and `getSentFromUid()` against the installed provider; the source_package extra alone is not authentication. Earlier Android versions do not provide this authentication through this open protocol. Only scalar values, short status strings and per-process/session identifiers are sent; no coordinates are broadcast.

## Fields common to both providers

| Extra | Android type | Meaning |
| --- | --- | --- |
| schema_version | int | 1; additions are optional fields |
| source_package | String | Provider application package |
| publisher_session_id | String | Random per-process UUID |
| sequence | long | Increasing event sequence in that process |
| sent_elapsed_ms / sent_unix_ms | long | Publication times; use monotonic elapsed time for freshness |
| current_speed_status | String | unknown, valid, stale or disabled |
| current_speed_updated_elapsed_ms | long | Speed observation time, or -1 |
| current_speed_max_age_ms | long | 5000 |
| current_speed_kmh | double, optional | Fresh normalized current speed; stationary zero is valid |
| road_speed_limit_status | String | unknown, valid, stale or disabled |
| road_updated_elapsed_ms | long | Road observation time, or -1 |
| road_max_age_ms | long | 30000 |
| road_speed_limit_kmh | double, optional | Fresh normalized effective limit |
| road_speed_limit_display | int, optional | Original numeric limit |
| road_speed_limit_display_unit | String, optional | Original recognized units or metric/imperial |
| zone_status | String | unknown, active, inactive, stale or disabled |
| zone_updated_elapsed_ms | long | Last accepted zone observation/clear time, or -1 |
| zone_max_age_ms | long | 90000 |
| zone_source | String, active only | legacy / insights for Waze, flitsmeister for Flitsmeister |
| zone_segment_id | String, active only | Opaque native identity, including Flitsmeister segment index; can be empty for Waze legacy |
| zone_session | long, active only | Observation session, scoped to publisher process |
| zone_remaining_m | int, active only | Remaining distance in metres |
| zone_progress_percent | double, active only | Progress percentage; preserve estimated flag |
| zone_progress_estimated | boolean, active only | Whether progress is an estimate |
| zone_observation_start_elapsed_ms | long, active only | Start of observed portion |
| zone_observed_distance_m | int, active only | Decrease from initial remaining distance, without double-crediting backward jitter |
| zone_average_partial | boolean, active only | Always true; earlier unobserved parts are unavailable |
| zone_average_method | String, active only | observed_distance_over_elapsed_time |
| zone_average_speed_kmh | double, optional | Observed distance * 3600 / elapsed milliseconds, after 30m and 3s |

Check status and optional-field presence. Missing values are unknown, not zero. Check `0 <= nowElapsed - observationElapsed <= maxAge` locally, including after the provider dies; Android may omit a final shutdown message. A request or heartbeat timestamp cannot make an old measurement current. Segment changes, reacquisition after expiry and substantial distance/progress reversals reset the observation average.

## App-specific production and lifetime

Waze reads its native LocationSensorListener speedometer callback, including normalizedSpeedKph, plus native legacy/insights zone callbacks. Its speedometer View and zone bar can remain disabled. The patch includes background keep-alive (default 180 minutes); Waze's engine and permitted background location must remain active. Legacy percent is Waze-provided; insights progress estimates the observed remainder. See [Waze bindings](waze/navigation-broadcasts.md).

Flitsmeister captures original AppLocationService fixes before missing GPS speeds are substituted. GPS speed is normalized from m/s; location accuracy and timestamps govern validity. RoadInfoService supplies the final effective limit after variable signs, roadworks and vehicle restrictions. Native active-zone candidates/segments supply their directed polylines; the exporter projects onto that path within 30m and calculates estimated remaining/progress. It updates this geometry with fresh GPS fixes and resets on segment changes. Its distance/time observed average uses the same calculation as Waze and does not rely on the eligibility-gated native sample-average display. Flitsmeister's driving/location foreground service must remain running; closing it clears the feed. Background location and Android power policies still govern delivery. It does not require overlay permission or a route on screen.

The module tests exercise the shared schema, speed freshness/zero/missing values, units, averages, projection, out-of-order detection, identity resets and lifecycle clearing. Test-only native probes and an independently signed common-action receiver are in `scripts/navigation-probe/`. Emulator callback/IPC evidence is distinct from a physical drive or certified enforcement-zone accuracy.

[Recorded candidate validation](navigation-validation.json).
