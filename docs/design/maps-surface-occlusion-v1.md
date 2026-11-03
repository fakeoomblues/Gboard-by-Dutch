# Maps surface occlusion extension, version 1

Status: development preview. Emitted-APK checks cover compact reflow, Search paint restoration, editor/surface replacement and real host-to-Maps companion IPC with synthetic card fixtures on both sides at 1280×480/160 DPI. Explicit overflow invokes native Settings on both player sides and at 200 DPI; the renamed microG-RE profile also passes the compact/Search and overflow probes. Native limited-home layouts at 800×480/160 DPI and 1280×480/240 DPI still hide the normal control strip after safe-area updates. Composed-player, live-route, complete gestures and physical-head-unit acceptance remain open.

## Ownership and transport

Keep `SurfaceContainer` and `ISurfaceCallback` unchanged. Use an explicit companion bind to `patches.maps.gearslip.NativeCarService`, with action `patches.maps.gearslip.action.SURFACE_OCCLUSION` and a fresh per-connection Intent data URI. Return an interface with descriptor `patches.maps.gearslip.ISurfaceOcclusion`. Bind only after the normal CAL handshake and surface creation have completed. The fresh data URI prevents Android's filter-keyed Binder cache from returning a capability from an earlier connection; it is not an authorization credential.

Capture the authenticated CAL Session when issuing this capability. Check the Binder caller UID and current Session before parsing state-changing requests, then check Session and surface generation again when queued work reaches the renderer's main thread. Unbinding the companion interface must not close the CAL Session. A handshake replacement, CAL unbind or process death revokes the companion capability. Do not accept a package name, surface dimensions or a generation counter as a substitute for ownership.

Maintain an adapter-owned monotonically increasing surface generation within the captured Session. Advance it for every accepted surface availability/replacement and destruction, including same-size replacements. Reset measured covers on a new generation. A host must fetch the new generation after its surface callback completes; delayed updates for older generations are rejected even if dimensions happen to match. CAL fallback rectangles retain their public semantics and remain available to hosts without this extension.

## Versioned messages

Use explicit primitive Parcel fields and bounded typed Rect lists; do not serialize private Maps classes or add members to public CAL objects. Reject unknown versions and malformed messages without mutating layout. Never block the main thread waiting for a Binder result.

| Operation | Request | Result |
| --- | --- | --- |
| Describe surface | Interface token, protocol version 1, result callback | Version, adapter surface generation, width, height, DPI, whether a valid surface exists |
| Apply covers | Interface token, version 1, generation, width, height, DPI, optional player Rect, list of other measured cover Rects, result callback | Applied, stale generation, unavailable surface, unsupported version, or invalid geometry |
| Clear covers | Apply covers with null player and empty list, current generation and matching dimensions | Restore native normal placement while continuing to honor CAL fallback areas |
| Release ownership | Header-only transaction 3 from the current companion binding | Restore that binding's layout if it remains the last accepted owner, even after surface destruction |

The implemented Parcel order is:

- Transaction 1 (describe): interface token, version int, request ID long, result callback Binder.
- Transaction 2 (apply): the same header, then generation long, width/height/DPI ints, player-present int (0 or 1), player Rect payload when present, cover count int, then that many Rect payloads. Reject trailing payload.
- Transaction 3 (release): the same header with no payload. Recheck caller and Session on the main thread. Restore only this binding's last accepted layout; an older binding cannot clear a newer binding's layout. The host sends this best-effort operation before unbinding; service unbind also releases its own layout. Neither operation closes navigation or the editor.
- Result callback transaction 1: result interface token, request ID long, status int, protocol version int, generation long, width/height/DPI ints. Zero dimensions mean no valid surface metadata; denied requests receive no surface metadata.

Status values are accepted 0, denied 1, unsupported 2, no surface 3, stale 4, invalid 5, application failure 6, and native tree not ready 7. The host retries temporary 3/4/7 results within a fixed limit, re-describing the generation before each attempt. Unsupported layouts wait for changed geometry instead of polling. Accepted describe means the surface metadata was retrieved. Accepted apply means the current generation's layout update was processed, not proof of rendering or user-visible acceptance. All layout operations and their final ownership/generation checks run on the main thread; callbacks are one-way and never block that thread waiting for the host.

Define the result callback as a separate one-way interface `patches.maps.gearslip.ISurfaceOcclusionResult`; return typed status and surface metadata. Associate every result with the initiating request ID so an old response cannot overwrite a newer host state. Bound lists to 32 covers, surface sides to 32768 pixels, and require dimensions/DPI to match the actual supplied surface. A null or empty player means no floating player. Reject inverted rectangles; clip valid partial intersections to the canvas. Covers fully outside the canvas have no effect. An empty public CAL rectangle continues to mean unknown, not a fully covered map.

## Host measurements

Derive bounds from the existing measured `ChromeCover` pieces and the actual surface bounds in window pixels. Translate and clip them into surface pixels with the same density used by `SurfaceContainer`; do not add encoder letterbox margins or scale a second time. Send the actual player bounds including reserved spacing, the bottom bar, and every other cover that can obscure controls. Send a null player immediately when it closes. Player side and compact/expanded labels are optional hints; measured rectangles determine placement.

Retain the latest host measurement while a surface is being replaced, then reproject it against the replacement's actual bounds and resend with its new generation. Never reuse a generation from a previous surface. Treat unsupported extension binding as a normal fallback to existing CAL areas.

## Placement and native application

`ControlPlacement` currently computes surface-pixel target slots from measured covers. It prefers at most two columns below the actual card when all requested targets fit. Otherwise it chooses an uncovered region without changing target size. When too few slots remain, reserve the final slot for an explicit overflow action and expose all remaining native actions there. A fully covered surface reports unavailable space rather than creating clipped targets. Its test fixtures describe synthetic measured cards; they are not inferred head-unit dimensions.

`NativeControlLayout` now reflows the two intact native groups inside `map_buttons_view_container` into columns when the planner can fit them below a measured player. It preserves every native parent and the zoom visual/TouchDelegate relationships. It temporarily disables ancestor clipping along that branch so controls can draw outside the old rectangular chrome bounds. `ProjectionRenderer.click` routes a hit at the moved group into its actual native touch hierarchy, avoiding the old ancestor hit bounds. Original orientation, parameters, translations and clipping flags are restored on clear, close or surface change. No hidden native action is made visible. Empty placeholders for native-hidden actions are excluded from the fit calculation; a clipped visible action still prevents reflow.

An explicit More target is used when the intact groups cannot fit. Its scrollable panel lists currently available native actions, uses native accessibility labels or localized known-role labels, and invokes their existing handlers. The original groups retain their parents; only their wrapper visibility is temporarily owned while collapsed. Back or an outside tap dismisses the panel. Changed native trees, Search transitions and new geometry revoke old rows. Unknown unlabeled actions and unrecognized native trees restore normal layout and report unsupported. More and Search restoration have emitted-APK evidence for the cases listed above. The limited native layout remains unresolved; accepting a pending geometry update while its normal control root is hidden does not establish visible control reachability.

Keep route/ETA, speed overlays, search and Android Auto's decor microphone separately readable; the button grid alone does not satisfy this requirement. Keep the full native Surface and map canvas, navigation activity, editor and route alive across every cover update. Native view replacement and keyboard transitions must invalidate layout references without reviving an older Session.

## Required evidence before acceptance

Run typed transport ownership/version/generation tests, placement tests, emitted-DEX inspection and actual native-renderer tests. Exercise right/left/closed/expanded players, bottom-bar changes, 800×480 and 1280×480/720, all requested UI sizes, route metadata, search/voice/editor, real moved-button effects, drag/pinch/recenter, same-size and resized surface replacement, reconnect and queued stale geometry. Verify map pixels behind covers in captured frames. Confirm the intended short physical head unit; emulator receipts establish only their tested behavior.
