# Native warning protocol core

This source set supplies the first shared implementation pieces for the [native warning design](../../docs/superpowers/specs/2026-11-02-flitsmeister-native-warnings-design.md): immutable event values, the primitive Bundle codec, bounded batches and the process-local journal. FM, Maps and Vela compile these sources. Provider capture, Binder transport, durable playback grants, receiving scheduling/UI and production speech adapters remain pending; no warning endpoint or voice-readiness claim is enabled by these classes alone.

## Codec

`WarningCodec.decode(Bundle, now)` validates and copies the event into `WarningEvent`. `encode(event)` returns a new Bundle. Only String, Integer, Long, Double and Boolean fields are accepted; unknown scalar fields survive minor-compatible round trips. Nested/custom Parcelable event values, arrays, wrong scalar types, malformed Unicode and future observation/emission clocks are rejected. Version 1 remains independent of navigation schema 1 and STARTUP_V1.

Messages are limited to 4 KiB UTF-8 without truncation. Events are limited to 24 KiB measured through actual `Parcel.writeBundle`, including unknown fields. `encodeBatch`/`decodeBatch` enforce at most eight events and 128 KiB. `splitBatches` partitions by whichever limit is reached first, preserving every event. Batches require increasing sequence numbers from one provider session; detecting missing contiguous sequences belongs to the journal/client cursor policy, not successful codec parsing.

Keep native family/subtype, message/language/origin, phase, category/priority and relationship distinct. Native emergency-behind events require neither speed nor coordinates. Geometry is optional; when present, distance/trigger/position include their own observation timestamp and explicit estimated flag. Zero distance remains valid. No receiver camera-distance or cooldown heuristic is part of this contract.

Fallback announcement deadlines are at most five seconds after emission for P0/moving-emergency events and ten seconds otherwise. An audited native deadline can be marked `deadline_native=true`; its provenance remains the provider adapter's responsibility. State validity is at most thirty seconds from the actual observation. Announcement and state deadlines are independent, and decoding/replay never changes either clock. CLEAR needs identity and observation/emission fields but invents no spoken payload.

## Journal and replay

`WarningJournal` belongs to one provider-process UUID. Append requires the next sequence and nondecreasing emission time; native observation times may be older. Exact retained transport duplicates are idempotent, conflicting payloads/semantic ANNOUNCE identities are rejected, and new native repeats require distinct announcement indexes.

The journal retains at most 128 original transport records for at most thirty seconds, while their state or announcement remains valid. CLEAR tombstones remain for thirty seconds. Another bounded map holds at most 128 latest warning/revision/cancellation records; each retained ANNOUNCE may reference one accepted same-phase/index payload revision. These references are bounded by the 128 retained authorities and disappear on cancellation/authority expiry. No journal data is persisted.

Revision and cancellation metadata outlive an expired/replaced display payload while older records remain. A later UPSERT or native repeat cannot resurrect an authority cancelled by CLEAR. Likewise, a newer unsaid instruction does not revert to its original text when display state expires or a later native repeat arrives. The replay candidate keeps its original ANNOUNCE identity and deadline separately from the revised payload.

Superseded UPSERTs are coalesced before exhausting history capacity. Valid ANNOUNCE/CLEAR records are not silently evicted. Overflow advances the transport head, explicitly reports loss and latches degraded status. Even an unretained CLEAR updates the cancellation floor, so the source lookup cannot grant playback of an older danger.

`resume(session, afterSequence, now)` returns original retained records, data-only current state, eligible native announcement candidates, head sequence, overflow and explicit resynchronization status. Any missing sequence—including coalescing/expiry/overflow—is a declared resynchronization boundary. A current-state snapshot is authoritative at that head: older replay records must not overwrite it as newer display state. A different session invalidates the old cursor; an impossible future cursor in the same session is rejected.

Replay candidates are **not playback grants**. Before output, the future authenticated BEGIN_ATTEMPT implementation must call `announcement(warningId, phase, index, now)` against current source state and apply ownership/capability/durable-grant checks. This rejects CLEAR/expiry even when a consumer missed a cancellation. Receiving acceptance cursors, scheduling, passage checks and persisted uncertainty dispositions remain separate work.

## Verification

Shared codec/journal regression tests live in the FM extension's `nl.johninnl.navigation.alerts` test package. Run the FM, Maps-extension and Vela unit tasks plus Waze compatibility checks. The full patch build verifies source-set integration; it does not prove the unimplemented event endpoint or complete spoken warnings.

The test-only native probe's `scenario=codec` compiles these production sources into instrumentation and exercises the codec against real Android Parcel. It preserves eight large synthetic events by splitting them into 121,940-byte and 40,708-byte batches on the controlled API-34 emulator. This is codec evidence, not proof of native FM capture, cross-UID event authentication, audio or car routing. See the active [transport implementation plan](../../docs/superpowers/plans/2026-11-02-warning-contract-transport.md) for remaining work.
