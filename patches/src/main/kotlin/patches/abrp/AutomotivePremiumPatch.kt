package patches.abrp

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val CONFIG_MODULE = "Lcom/lugg/RNCConfig/RNCConfigModule;"
private const val CONFIG_MAP = "Lcom/facebook/react/bridge/WritableMap;"
private const val HASH_MAP_PUT = "Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"

/**
 * `RNCConfigModule.getConfig()` copies every `BuildConfig` field into a map through reflection
 * and hands that map to JavaScript as the `Config` object. The app then derives
 *
 *     isAndroidAutomotive         = Config.ANDROID_AUTOMOTIVE === "true"
 *     isAndroidAutomotiveTemplate = isAndroidAutomotive && Config.USE_AUTOMOTIVE_TEMPLATES === "true"
 *
 * and `isPremiumUserSelector` returns true for the template build before it reads the account
 * roles.
 *
 * The regular build carries no `USE_AUTOMOTIVE_TEMPLATES` field, so editing existing
 * constants cannot turn the template flag on. Both entries are written into the same map
 * immediately before it is wrapped as `{ config: ... }`.
 */
@Suppress("unused")
val automotivePremium = bytecodePatch(
    name = "Unlock Premium",
    description = "Makes the app treat itself as the Android Automotive template build, which " +
        "unlocks the in-app Premium features without a subscription. That decision does not read " +
        "the account roles. Anything the ABRP server authorizes per account stays as it is.",
) {
    compatibleWith(abrp)
    dependsOn(originalInput)

    execute {
        val owner = mutableClassDefBy(CONFIG_MODULE)
        val method = owner.methods.singleOrNull { candidate ->
            candidate.name == "getConfig" && candidate.returnType == CONFIG_MAP
        } ?: throw IllegalStateException("ABRP config bridge changed")

        val implementation = method.implementation
            ?: throw IllegalStateException("ABRP config bridge has no code")

        // The reflected map is filled by a loop and then stored as the "config" entry of a
        // second map. Inserting in front of that store keeps the reflection copy intact and
        // adds the two keys this build does not carry.
        val store = (0 until implementation.instructions.size - 1).singleOrNull { index ->
            val key = method.getInstruction(index)
            val put = method.getInstruction(index + 1)
            key.opcode == Opcode.CONST_STRING &&
                (key as? ReferenceInstruction)?.reference?.let { it as? StringReference }?.string == "config" &&
                put.opcode == Opcode.INVOKE_VIRTUAL &&
                (put as? ReferenceInstruction)?.reference?.toString() == HASH_MAP_PUT
        } ?: throw IllegalStateException("ABRP config map assembly changed")

        // put(outer, key, reflected) - the reflected map is the value register.
        val reflected = "v${(method.getInstruction(store + 1) as ThreeRegisterInstruction).registerC}"

        method.addInstructions(
            store,
            """
                const-string v0, "ANDROID_AUTOMOTIVE"
                const-string v1, "true"
                invoke-virtual {$reflected, v0, v1}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                const-string v0, "USE_AUTOMOTIVE_TEMPLATES"
                invoke-virtual {$reflected, v0, v1}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
            """.trimIndent(),
        )
    }
}
