package patches.marktplaats

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import com.android.tools.smali.dexlib2.Opcode
import patches.shared.uniqueMatch

internal val marktplaats = Compatibility(
    name = "Marktplaats", packageName = "nl.marktplaats.android", apkFileType = ApkFileType.APKM,
    signatures = setOf("742dc275b25928621a8fbdb4362876825c8aec6a4afdbd04d0b8d72b9ca22052"),
    targets = listOf(
        AppTarget(version = "16.19.0", versionCode = 633464, minSdk = 26),
    ),
)
internal val originalInput = patches.shared.originalInput(marktplaats,
    additionalMinSdksByVersion = mapOf("16.19.0" to setOf(28, 29)))
private val payloads = setOf(Opcode.PACKED_SWITCH_PAYLOAD, Opcode.SPARSE_SWITCH_PAYLOAD, Opcode.ARRAY_PAYLOAD)

context(context: app.morphe.patcher.patch.BytecodePatchContext)
internal fun target(owner: String, signature: String) = run {
    val binding = marktplaats1619Bindings["$owner->$signature"]
    val actualOwner = binding?.owner ?: owner
    val actualSignature = binding?.signature ?: signature
    Fingerprint(
        definingClass = actualOwner, name = actualSignature.substringBefore('('), returnType = actualSignature.substringAfter(')'),
        parameters = Regex("\\[*L[^;]+;|\\[*[ZBCSIJFD]")
            .findAll(actualSignature.substringAfter('(').substringBefore(')')).map { it.value }.toList(),
    ).uniqueMatch()
}

context(context: app.morphe.patcher.patch.BytecodePatchContext)
@Suppress("UNUSED_PARAMETER") // Keep canonical call sites; the audited binding owns the current shape.
internal fun exact(owner: String, signature: String, flags: Int, registers: Int, count: Int) = target(owner, signature).method.also { method ->
    val binding = checkNotNull(marktplaats1619Bindings["$owner->$signature"]) {
        "Missing audited Marktplaats 16.19.0 binding: $owner->$signature"
    }
    check(method.accessFlags == binding.flags && method.implementation!!.registerCount == binding.registers &&
        method.instructions.count { it.opcode !in payloads } == binding.count) {
        "Marktplaats original method changed: $owner->$signature"
    }
}
