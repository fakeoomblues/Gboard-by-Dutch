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

        val load = owner.methods.singleOrNull {
            it.name == "load" && it.returnType == "V" &&
                it.parameterTypes.toList() == listOf("Ljava/lang/String;")
        } ?: error("Angry Birds 2 ad load entry changed")
        check(load.implementation?.instructions?.firstOrNull()?.opcode != Opcode.RETURN_VOID) {
            "Angry Birds 2 ad load already patched"
        }
        load.addInstructions(0, "return-void")

        // The synthetic runnable dispatched by show() is the only caller of AdsSdkBase.show().
        // Patching it keeps the UI-thread dispatch: the listener callback runs exactly where
        // the game expects its JNI callbacks.
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
        show.addInstructions(
            0,
            """
            const/4 v0, 0x1
            const-string v1, "ads disabled"
            invoke-virtual { p0, v0, v1 }, $ADS_SDK->onAdError(ILjava/lang/String;)V
            return-void
            """,
        )
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
