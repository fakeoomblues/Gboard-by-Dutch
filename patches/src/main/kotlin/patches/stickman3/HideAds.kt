package patches.stickman3

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod

private const val IMPLEMENTATION = "Lcom/unity3d/services/ads/UnityAdsImplementation;"
private const val LOAD_LISTENER = "Lcom/unity3d/ads/IUnityAdsLoadListener;"
private const val SHOW_LISTENER = "Lcom/unity3d/ads/IUnityAdsShowListener;"
private const val INIT_LISTENER = "Lcom/unity3d/ads/IUnityAdsInitializationListener;"
private const val LOAD_ERROR = "Lcom/unity3d/ads/UnityAds\$UnityAdsLoadError;"
private const val SHOW_ERROR = "Lcom/unity3d/ads/UnityAds\$UnityAdsShowError;"

/**
 * The game is an IL2CPP build whose only ad path is the Unity Ads Android SDK. Every placement goes
 * through `UnityAdsImplementation`, so this gate sits on the entries the SDK's public `UnityAds`
 * wrapper funnels into: initialization completes without ever reaching `UnityServices` (no
 * configuration, gateway or metrics request), every load reports the SDK's own NO_FILL error and
 * every show reports NOT_READY. The C# side therefore stays in the state it already handles for
 * "no ad available", and the AppLovin, IronSource and Audience Network mediation adapters are never
 * reached because nothing is ever loaded.
 */
internal val stickmanAdsGate = bytecodePatch {
    dependsOn(originalInput)
    execute {
        val owner = mutableClassDefBy(classDefBy(IMPLEMENTATION))

        fun entry(name: String, parameters: List<String>): MutableMethod =
            owner.methods.singleOrNull {
                it.name == name && it.returnType == "V" &&
                    it.parameterTypes.map(CharSequence::toString) == parameters
            } ?: error("Draw a Stickman ad $name entry changed")

        fun MutableMethod.requireBody(marker: Opcode, locals: Int) {
            val body = implementation ?: error("Draw a Stickman ad $name entry has no body")
            check(body.instructions.first().opcode != marker) {
                "Draw a Stickman ad $name entry is already patched"
            }
            check(body.registerCount - (parameterTypes.size + 1) >= locals) {
                "Draw a Stickman ad $name entry has fewer registers than expected"
            }
        }

        // initialize(Context, String, boolean, listener): report success, never start the SDK.
        val initialize = entry("initialize", listOf("Landroid/content/Context;", "Ljava/lang/String;", "Z", INIT_LISTENER))
        initialize.requireBody(Opcode.IF_EQZ, 0)
        initialize.addInstructionsWithLabels(
            0,
            """
            if-eqz p4, :noListener
            invoke-interface { p4 }, $INIT_LISTENER->onInitializationComplete()V
            return-void
            """.trimIndent(),
            ExternalLabel("noListener", initialize.getInstruction(0)),
        )

        // The C# wrapper reads these before it requests a placement; keep them consistent with the
        // initialization callback above, so no retry loop or "unsupported device" path is taken.
        for (name in listOf("isInitialized", "isSupported")) {
            val state = owner.methods.singleOrNull {
                it.name == name && it.parameterTypes.isEmpty() && it.returnType == "Z"
            } ?: error("Draw a Stickman ads state entry changed: $name")
            state.requireBody(Opcode.CONST_4, 1)
            state.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
        }

        val load = entry("load", listOf("Ljava/lang/String;", "Lcom/unity3d/ads/UnityAdsLoadOptions;", LOAD_LISTENER))
        load.requireBody(Opcode.IF_EQZ, 2)
        load.addInstructionsWithLabels(
            0,
            """
            if-eqz p3, :noListener
            sget-object v0, $LOAD_ERROR->NO_FILL:$LOAD_ERROR
            const-string v1, "ads disabled"
            invoke-interface { p3, p1, v0, v1 }, $LOAD_LISTENER->onUnityAdsFailedToLoad(Ljava/lang/String;${LOAD_ERROR}Ljava/lang/String;)V
            return-void
            """.trimIndent(),
            ExternalLabel("noListener", load.getInstruction(0)),
        )

        val show = entry(
            "show",
            listOf("Landroid/app/Activity;", "Ljava/lang/String;", "Lcom/unity3d/ads/UnityAdsShowOptions;", SHOW_LISTENER),
        )
        show.requireBody(Opcode.IF_EQZ, 2)
        show.addInstructionsWithLabels(
            0,
            """
            if-eqz p4, :noListener
            sget-object v0, $SHOW_ERROR->NOT_READY:$SHOW_ERROR
            const-string v1, "ads disabled"
            invoke-interface { p4, p2, v0, v1 }, $SHOW_LISTENER->onUnityAdsShowFailure(Ljava/lang/String;${SHOW_ERROR}Ljava/lang/String;)V
            return-void
            """.trimIndent(),
            ExternalLabel("noListener", show.getInstruction(0)),
        )
    }
}

@Suppress("unused")
val stickman3HideAds = bytecodePatch(
    name = "Hide ads",
    description = "Never starts the Unity Ads SDK: initialization completes locally, every load " +
        "reports the SDK's own no-fill error and every show reports not-ready, so no ad network, " +
        "mediation adapter or consent screen is ever contacted.",
    default = true,
) {
    compatibleWith(stickman3)
    dependsOn(stickmanAdsGate)
}
