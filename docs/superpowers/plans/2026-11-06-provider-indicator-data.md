# FM/Waze indicator data audit — 6 November 2026

The revised Maps camera pill is approved; styling is fixed. The owner stopped all emulator testing. Owned emulator/DHU/tracing processes are stopped. No phone is involved in this batch.

The normative mapping, field, unit, freshness, expiry and clearing rules for all19 visual families are now in [Navigation indicator contract v1](../../contracts/navigation-indicators-v1.md). That table separates explicit FM/Waze bindings from unsupported equivalents. It does not claim19 natural-feed or Vela acceptance results.

## Connected product batch48

- Normalize all19 visual families while retaining native identity, categories and announcement authority. Maps and Vela share the same contract and unchanged owner assets.
- Correlate FM15 early-response geometry to the actual network-request fix and exact parser JSON/model. Preserve its measurement clock. Distinguish a native UI maximum-distance span from an audio trigger. Derive fresh Behind only from qualified relative coordinates, never heading alone.
- Capture actual road and average-zone overspeed decisions separately, excluding settings previews. A native average alarm supplies no invented numeric average.
- Supply data-only final metric road-limit transitions and GPS-only country transitions, excluding initial/default/manual country changes. These UPSERTs do not authorize speech.
- Distinguish Waze distance to a zone from distance remaining within it. Do not use an entry trigger as total zone length. Retain the genuine zone-percent observation clock.
- Require the native isTrafficLight flag before selecting the traffic-light glyph. Advice is not signal phase/time-to-green.

## Exact limitations

The130 asset may represent an actual known130 road limit, while the original native type remains intact. FM's separate Hop-hop notice is not equivalent; its driving-event binding is still unproven. The static native sound helper also has settings-preview callers, so it is not hooked globally.

No matching Waze native source has been proven for historical speed-trap hotspots, wrong-way drivers, opening-bridge state, traffic-observation cameras or border-crossing notices. The existence of an asset is not evidence of provider support. Other unmatched native warnings keep useful source text and a generic glyph.

Waze moving-emergency AA acceptance is excluded by owner instruction. Its static emergency hazard does not become a moving responder. FM emergency relative position does not establish closing speed, lane or predicted responder route.

## Verification and remaining gates

The current source is rebased onto GitLab main76a386c. Complete canonical verification50 and the affected Android checkpoints pass. Reviewed batch54 fixes partial FM parser coordinates and keeps independently clocked Waze segment facts through descriptor refreshes. All four complete APK/APKM inputs patch, sign and align; Maps/Vela include all19 exact owner asset hashes. Receipts are in `.analysis/android-auto-acceptance/native-indicator-reviewed-54/receipt.json`. Google Maps uses the retained original code/resources re-signed for engineering setup; the three Desktop publisher originals are used for Vela/FM/Waze. These are package/static results, not natural-feed acceptance.

Existing geometry47 build/patch evidence remains retained for its exact candidate. The approved design is runtime80/camera-fresh.png. The runtime79 missing-alert frame was captured66ms after receiver UPSERT and less than0.6s before CLEAR; it was not valid evidence of provider delivery failure.

The owner reduced the design gate to one generic Vela frame. Run85 rendered the pill but exposed zoom-minus overlap. Vela now selects its current visible callback rectangle instead of the preferred stable rectangle; its updated single-frame placement approval, real FM/Waze delivery/lifecycle acceptance and car-size gates remain open. Runtime testing stays stopped until the owner explicitly resumes it. No publication or main promotion is part of this batch.
