package patches.waze

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.string
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import patches.waze.returnString
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

internal data class AudioSdkMethods(val items: MutableMethod, val disconnect: MutableMethod)

private fun MethodReference.parameters() = parameterTypes.map { it.toString() }
private fun MutableMethod.calls() = implementation!!.instructions.mapNotNull {
    (it as? ReferenceInstruction)?.reference as? MethodReference
}
private fun String.javaName() = removePrefix("L").removeSuffix(";").replace('/', '.')
private fun <T> List<T>.unique(label: String): T = singleOrNull()
    ?: throw PatchException("Unsupported Waze audio SDK: expected one $label, found $size")

context(context: BytecodePatchContext)
internal fun discoverAudioSdk(): AudioSdkMethods {
    val callback = Fingerprint(name = "onConnected", returnType = "V", parameters = emptyList(),
        filters = listOf(string("WazeSdk: Connected to media browser service: ")))
        .matchAll().toList().unique("media-browser callback")
    val adapter = callback.originalClassDef.fields.filter {
        !AccessFlags.STATIC.isSet(it.accessFlags) && it.type.startsWith("Lcom/waze/sdk/")
    }.map { context.mutableClassDefBy(it.type) }.filter { candidate ->
        candidate.methods.any { it.name == "<init>" && it.parameters() == listOf(
            "Landroid/content/Context;", "Ljava/lang/String;", "Landroid/content/ComponentName;") }
    }.unique("media-browser adapter")
    adapter.fields.filter { !AccessFlags.STATIC.isSet(it.accessFlags) && it.type == "Ljava/lang/String;" }
        .unique("adapter package field")
    val items = adapter.methods.filter { !AccessFlags.STATIC.isSet(it.accessFlags)
            && it.parameters().isEmpty() && it.returnType == "Ljava/util/List;" }.unique("audio list getter")
    val shelfType = items.implementation!!.instructions.filter { it.opcode == Opcode.CHECK_CAST }
        .mapNotNull { ((it as? ReferenceInstruction)?.reference as? TypeReference)?.type }
        .filter { it.startsWith("Lcom/waze/sdk/") }.distinct().unique("shelf item type")
    val shelf = context.mutableClassDefBy(context.mutableClassDefBy(shelfType).superclass
        ?: throw PatchException("Unsupported Waze audio SDK: missing shelf superclass"))
    shelf.fields.filter { !AccessFlags.STATIC.isSet(it.accessFlags) && it.type == "Ljava/util/List;" }
        .unique("shelf children field")
    val mediaType = shelf.methods.filter { it.name == "<init>" && it.parameterTypes.size == 1 }
        .unique("shelf constructor").parameterTypes.single().toString()

    val notify = adapter.methods.filter { AccessFlags.STATIC.isSet(it.accessFlags)
            && it.parameters() == listOf(adapter.type) && it.returnType == "V"
            && it.calls().any { call -> call.definingClass == adapter.type && call.name == items.name
                && call.returnType == "Ljava/util/List;" && call.parameters().isEmpty() } }
        .unique("shelf notification bridge")
    val publish = notify.calls().filter { it.definingClass.startsWith("Lcom/waze/sdk/")
            && it.returnType == "V" && it.parameters() == listOf("Ljava/lang/String;", "Ljava/util/List;") }
        .unique("shelf publication method")
    val singleton = notify.calls().filter { it.definingClass == publish.definingClass
            && it.returnType == publish.definingClass && it.parameters().isEmpty() }
        .unique("audio manager singleton")
    val disconnect = adapter.methods.filter { !AccessFlags.STATIC.isSet(it.accessFlags)
            && it.parameters().isEmpty() && it.returnType == "V"
            && it.calls().any { call ->
                call.definingClass.startsWith("Lcom/waze/sdk/") && call.returnType == "V"
                    && call.parameters() == listOf("Ljava/lang/String;")
                    && context.mutableClassDefBy(call.definingClass).methods.any { helper ->
                        helper.name == call.name && helper.parameters() == call.parameters()
                            && helper.returnType == "V" && helper.calls().any { nested ->
                                nested.definingClass == "Ljava/util/Map;" && nested.name == "remove"
                                    && nested.parameters() == listOf("Ljava/lang/Object;")
                            }
                    }
            } }
        .unique("browser disconnect method")
    val bindings = "Lapp/wazeytm/extension/waze/WazeAudioBindings;"
    for ((name, value) in listOf(
        "adapterClass" to adapter.type.javaName(), "groupClass" to shelf.type.javaName(),
        "mediaItemClass" to mediaType.javaName(), "managerClass" to publish.definingClass.javaName(),
        "managerInstanceMethod" to singleton.name, "publishMethod" to publish.name
    )) returnString(bindings, name, value)
    return AudioSdkMethods(items, disconnect)
}
