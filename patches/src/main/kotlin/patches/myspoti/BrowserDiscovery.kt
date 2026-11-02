package patches.myspoti

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

/** Locate the shared permission check without relying on R8 method names. */
internal object BrowserDiscovery {
    private const val STRING = "Ljava/lang/String;"
    private const val UID_LOOKUP = "Landroid/content/pm/PackageManager;->getPackagesForUid(I)[Ljava/lang/String;"
    private fun Method.calls() = implementation?.instructions?.toList().orEmpty().mapNotNull {
        ((it as? ReferenceInstruction)?.reference as? MethodReference)?.toString()
    }
    private fun <T> List<T>.one(label: String): T = singleOrNull()
        ?: error("Unsupported Myspoti browser: expected one $label, found $size")

    @JvmStatic
    @JvmOverloads
    fun discover(service: ClassDef, dispatchMethods: List<Method> = emptyList()): Method {
        val methods = service.methods.filter { it.implementation != null && !AccessFlags.STATIC.isSet(it.accessFlags) }
        val root = methods.filter { method ->
            method.implementation!!.instructions.any {
                ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == "media browser: empty root for "
            } && UID_LOOKUP in method.calls() && method.returnType.startsWith("L")
        }.one("UID-checked browser root")
        val predicate = methods.filter { method ->
            method.returnType == "Z" && method.parameterTypes.map(CharSequence::toString) == listOf(STRING) &&
                method.toString() in root.calls() && method.calls().any {
                    it == "Landroid/content/pm/PackageManager;->getApplicationInfo(Ljava/lang/String;I)Landroid/content/pm/ApplicationInfo;"
                } && method.calls().any { it == "Ljava/util/Set;->contains(Ljava/lang/Object;)Z" }
        }.one("shared browse permission predicate")
        val caller = methods.filter { method ->
            method.returnType == "Z" && method.parameterTypes.isEmpty() &&
                UID_LOOKUP in method.calls() && predicate.toString() in method.calls()
        }.one("UID-checked children/search caller")
        val callbacks = (methods + dispatchMethods).filter { it.returnType == "V" && caller.toString() in it.calls() }
        check(callbacks.any { it.parameterTypes.size == 2 && it.parameterTypes[0] == STRING } &&
            callbacks.any { method ->
                (method.parameterTypes.size == 3 && method.parameterTypes[0] == STRING &&
                    method.parameterTypes[1] == "Landroid/os/Bundle;") ||
                    method.implementation!!.instructions.any {
                        ((it as? ReferenceInstruction)?.reference as? StringReference)?.string?.startsWith(
                            "onSearch must call detach() or sendResult() before returning for query=") == true
                    }
            }) {
            "Unsupported Myspoti browser: children and search no longer share the UID-checked caller"
        }
        check(predicate.implementation!!.instructions.none { instruction ->
            instruction.opcode == Opcode.INVOKE_STATIC &&
                ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.definingClass == "Lpatches/myspoti/GearslipSupport;"
        }) { "Myspoti Gearslip browser support is already applied" }
        return predicate
    }
}
