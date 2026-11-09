package patches.angrybirds2

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode

internal const val ADS_SDK = "Lcom/rovio/beacon/ads/AdsSdk;"
private const val ADS_SDK_BASE = "Lcom/rovio/beacon/ads/AdsSdkBase;"

/**
 * The Unity game drives every ad through the beacon AdsSdk JNI wrapper. Skipping load keeps the
 * C# side in its "not ready" state (no ad button, SDKs never initialize), and show() reporting
 * the canonical error code is the game's own offline/no-fill path, so reward and interstitial
 * flows close cleanly.
 */
internal val angryBirdsAdsGate = bytecodePatch {
    dependsOn(originalInput)
    execute {
        val owner = mutableClassDefBy(ADS_SDK)

        // Fire the game's own no-fill error instead of returning silently: the C# loader waits
        // for onAdReady or onAdError, and a silent skip leaves it spinning on the start screen.
        val errorCall = { label: String, code: String ->
            """
            const/4 v0, $code
            const-string v1, "$label"
            invoke-virtual { p0, v0, v1 }, $ADS_SDK->onAdError(ILjava/lang/String;)V
            return-void
            """
        }

        // The synthetic load runnable is the only caller of AdsSdkBase.load.
        val load = Fingerprint(
            definingClass = ADS_SDK,
            returnType = "V",
            filters = listOf(methodCall(definingClass = ADS_SDK_BASE, name = "load")),
        ).let { fingerprint ->
            fingerprint.method.also {
                check(it.definingClass == ADS_SDK) { "Angry Birds 2 ad load target changed" }
            }
        }
        check(load.name != "load") { "Angry Birds 2 ad load target is the public method" }
        load.addInstructions(0, errorCall("NoFill", "-0x3"))

        // The synthetic show runnable is the only caller of AdsSdkBase.show; patching it keeps
        // the UI-thread dispatch so the listener callback runs where the game expects it.
        val show = Fingerprint(
            definingClass = ADS_SDK,
            returnType = "V",
            filters = listOf(methodCall(definingClass = ADS_SDK_BASE, name = "show")),
        ).let { fingerprint ->
            fingerprint.method.also {
                check(it.definingClass == ADS_SDK) { "Angry Birds 2 ad show target changed" }
            }
        }
        check(show.name != "show") { "Angry Birds 2 ad show target is the public method" }
        // -9 needs const/16; const/4 only holds -8..7.
        show.addInstructions(0, errorCall("ShowFailed", "-0x9").replaceFirst("const/4", "const/16"))
    }
}

@Suppress("unused")
val angryBirds2HideAds = bytecodePatch(
    name = "Hide ads",
    description = "Stops the game's ad SDKs from ever starting: ad requests report the game's own " +
        "no-fill error, so interstitial and reward flows close cleanly and no ad network is contacted.",
    default = true,
) {
    compatibleWith(angryBirds2)
    dependsOn(angryBirdsAdsGate)
}
