package patches.deezertv

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private const val POLICY = "Lcom/deezer/core/data/model/policy/IIlIIlIIIIIlII;"
private const val FEATURE = "Lcom/deezer/core/data/model/policy/IIlIIlIIIIIlII\$IIlIIlIIIIIlII;"
private const val AD_KIND = "Lcom/deezer/core/data/model/policy/IIlIIlIIIIIlII\$IlIllllIlIIlllll;"
private const val TRACK = "LIlIIllIIIIlIIII;"
private const val RIGHTS_HELPER = "LllIIlIIIIIlllII;"

/**
 * The TV build carries the same licence object as the phone app: feature bits for
 * playback rights (preview, device limit, offline, radio) and a separate audio/display
 * advertising entitlement. Keep the transport and the gateway-provided licence, and
 * only remove the client-side verdicts that turn the same account-backed stream into a
 * preview, a device-limit refusal or a time-limited session.
 */
@Suppress("unused")
val deezerTvEnableFullStreaming = bytecodePatch(
    name = "Enable full streaming (TV)",
    description = "Keeps full-track playback enabled for the signed-in TV account: removes the preview " +
        "and device-limit blocks, keeps offline/radio rights, disables the streaming time limit and " +
        "leaves unlicensed catalogue entries playable through Deezer's own media service. Account, " +
        "catalogue and playback transport are unchanged.",
    default = true,
) {
    compatibleWith(deezerTv)
    dependsOn(originalInput)

    execute {
        // Only PREVIEW (0x20) and DEVICE_LIMITATION (0x40) stay denied. MOD, OFFLINE,
        // CHROMECAST, RADIO and QUEUELIST_EDITION remain available. p0/p1 are free to
        // reuse because the method returns immediately.
        exact(POLICY, "llIlllIlllIIllI", "Z", FEATURE).method.addInstructions(
            0,
            """
                iget p1, p1, $FEATURE->IIllIIlIllIlIIl:I
                const/16 p0, 0x20
                if-eq p1, p0, :morphe_tv_deny
                const/16 p0, 0x40
                if-eq p1, p0, :morphe_tv_deny
                const/4 p1, 0x1
                return p1
                :morphe_tv_deny
                const/4 p1, 0x0
                return p1
            """,
        )

        // Audio and display advertising are not an entitlement requirement here.
        exact(POLICY, "IlIIIlIIlIIIIll", "Z", AD_KIND).method.addInstructions(
            0,
            "const/4 p0, 0x0\nreturn p0",
        )

        // A zero streaming limit is the "no limit" value the licence exposes.
        exact(POLICY, "lIIllIlllllllI", "I").method.addInstructions(0, "const/4 p0, 0x0\nreturn p0")

        // The licence stays active; the app rechecks it through the gateway itself.
        exact(POLICY, "IllIIlllIIllIIII", "Z").method.addInstructions(0, "const/4 p0, 0x1\nreturn p0")

        // Per-track rights: the native player asks this helper before starting a track.
        // A signed-in free account still receives a token and a full media stream, so
        // the client-side verdict is what has to stop rejecting the track.
        exact(
            RIGHTS_HELPER,
            "IIIIlIlIIlllIll",
            "Z",
            TRACK,
            POLICY,
            "J",
        ).method.addInstructions(0, "const/4 p0, 0x1\nreturn p0")
    }
}
