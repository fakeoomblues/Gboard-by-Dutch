# Capture-fault snapshot acknowledgement loop

## Observed product failure

Run56 used the unchanged signed Waze27/Vela02 cohort and existing observer.
Native Vela car settings and its provider selector were reachable through real
AA17 UI. Selecting De-Waze started a new observed Waze process; the UI reported
warning API connected, engine running/provider muted and fresh data, but
incomplete warning capture and unreviewed output. No warning playback was
accepted from that state.

The final failure was observer-budget-exhausted, not process death. Waze PID10322
made 5,183 RESUME and 5,115 ACK calls in roughly61seconds, reaching the existing
12,000-event observation cap. The late tall-profile keyboard capture crossed
that coverage boundary and is excluded. The earlier settings/selector captures
remain qualified evidence. Actual APK inspection confirms NativeWarnings.bound()
returns true; this was not an unbound-marker workaround.

The provider's deliver() invalidated any synchronized registration whenever its
sticky captureFault was true. A receiver fetched and acknowledged a snapshot
showing that fault, then ACK immediately called deliver(), which sent the same
invalidation again. Receiver resynchronization repeated the cycle without new
source data. This is a product control-plane feedback loop, not justification
to increase the frozen observer budget.

## Correction and safety boundary

Each frozen view now records the capture-fault state it contains. ACK retains
that state per registration. A new fault still causes invalidation, but an
acknowledged fault alone does not invalidate the same state again. Genuine
journal gaps continue to require resynchronization, and valid later records
can still be delivered. A healthy view frozen before a fault cannot acknowledge
the newer fault. The wire protocol is unchanged.

The source capture fault remains sticky. Capture completeness stays false and
new audio claims/attempts remain blocked by the existing overflow fence. The
change does not manufacture capture completeness, discard uncertainty, reset
the durable output ledger or change native results/policy. Its initial cause
remains to be identified during native acceptance.

Normal application error logging now records only the first bounded fault
reason: native-capture overflow, ingress/callback queue overflow, journal
overflow or an invalid-event code/field name. It excludes native packet dumps,
messages and coordinates. No observer, probe, fixture, runner or validation
framework was changed.

## Grouped verification

The two transport regressions first ran against the previous endpoint in the
isolated checkpoint30 workspace. Both failed at repeated-invalidation
assertions, with no test setup errors. The fixed endpoint then passed the
complete canonical verify.cjs set, including all Android modules, Python,
release/identity/history, Medium and strict bilingual documentation checks.
The regressions also require the audio claim to stay OVERFLOW and later valid
records to arrive, and cover a fault that appears after a healthy view freezes.

The same built/signed MPP passed actual Waze21-selection and FM15 nine-selection
patching in FULL/validate mode, test signing, certificate verification and
alignment. Input APKMs were staged inside owned job storage before the existing
Morphe CLI performed its normal split merge. Temporary inputs, patching scratch,
unsigned APKs and the isolated build checkout were removed after owned-process
and storage quiescence proof. CleanupRequired=false. Exact input, bundle and
APK hashes, expected before-fix failures and final test records are retained in
the private checkpoint30 receipt. Signed engineering MPP SHA-256:
`d4e9fdcafb30bb2b8beefec2eeb47c5f8d062ff4b99c95369581c71e0b7c2e91`.

Native acceptance of the correction remains pending. Preserve already accepted
receiver route/rendering/speech and API-owned expiry evidence where unaffected;
do not claim the remaining profile/DPI/controls or external-warning modes are
closed. Run56's baseline APKs/audio/debug/role/GPS were verified restored, its
owned runtime/server/forwards stopped, and scratch archived/removed. No physical
phone was touched and no stable release or main promotion occurred.
