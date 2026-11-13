package patches.deezertv

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private const val ADS_CONFIG = "Lcom/deezer/feature/ads/config/model/AdsConfigDataModel;"
private const val EMPTY_MAP =
    "new-instance p0, Ljava/util/HashMap;\n" +
        "invoke-direct {p0}, Ljava/util/HashMap;-><init>()V\n" +
        "return-object p0"

/**
 * Every TV placement reads the gateway-provided AdsConfigDataModel: the Google ad
 * brick, the SmartAdServer preroll and the Triton audio-ad controller. Returning an
 * empty configuration turns each of them off at its own decision point, and the
 * remaining launch paths are closed so no ad activity can render.
 */
@Suppress("unused")
val deezerTvRemoveAdvertising = bytecodePatch(
    name = "Remove advertising (TV)",
    description = "Disables the TV build's banner, native, preroll and audio advertising: no ad " +
        "configuration is exposed to the placement logic, the audio-ad launcher cannot attach its " +
        "payload, and the video-ad activity closes instead of loading content. Playback, catalogue " +
        "and account services are unchanged.",
    default = true,
) {
    compatibleWith(deezerTv)
    dependsOn(originalInput)

    execute {
        exact(ADS_CONFIG, "getConfig", "Ljava/util/HashMap;").method.addInstructions(0, EMPTY_MAP)
        exact(ADS_CONFIG, "getSections", "Ljava/util/HashMap;").method.addInstructions(0, EMPTY_MAP)

        // Google banner placement: no "banner" config section means no request.
        exact("LIIlIIlIlllIlllI;", "IIIIlIlIIlllIll", "Z")
            .method.addInstructions(0, "const/4 p0, 0x0\nreturn p0")

        // The audio-ad launcher normally copies its ad into the intent. Without the
        // extra, AudioAdsActivity takes its existing no-ad path (resume + finish).
        exact("LIIlIIllIIIlIlII;", "IIIllllIIlllll", "V", "Landroid/content/Intent;")
            .method.addInstructions(0, "return-void")

        // If a video-ad activity is reached through another path, close it instead of
        // loading and playing the ad.
        closeAfterSuperOnCreate("Lcom/deezer/feature/ads/audio/model/video/VideoAdsActivity;")
    }
}
