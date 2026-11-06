package dev.jason.gboardpatches.patches.gboard.features.editingaccesspoints

import app.morphe.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import dev.jason.gboardpatches.patches.gboard.shared.GboardFieldTarget
import dev.jason.gboardpatches.patches.gboard.shared.GboardMethodTarget
import dev.jason.gboardpatches.patches.gboard.shared.mutableClass

/** Reviewed target shape. No editor policy belongs in this version adapter. */
internal object GboardEditingAccessPointsTargets {
    private const val OWNER = "Lpsx;"
    val startView = lifecycle("onStartInputView", "Landroid/view/inputmethod/EditorInfo;", "Z")
    val resetMethods = listOf(
        startView,
        lifecycle("onStartInput", "Landroid/view/inputmethod/EditorInfo;", "Z"),
        lifecycle("onFinishInputView", "Z"),
        lifecycle("onFinishInput"),
        lifecycle("onWindowHidden"),
        lifecycle("onDestroy"),
    )
    private fun lifecycle(name: String, vararg parameters: String) =
        GboardMethodTarget(OWNER, name, parameters.toList(), "V")

    private val descriptorMethods = listOf(
        GboardMethodTarget("Lnex;", "c", emptyList(), "Lnes;") to true,
        GboardMethodTarget("Lnes;", "l", listOf("Ljava/lang/String;"), "V") to false,
        GboardMethodTarget("Lnes;", "i", listOf("I"), "V") to false,
        GboardMethodTarget("Lnes;", "q", listOf("Ljava/lang/Runnable;"), "V") to false,
        GboardMethodTarget("Lnes;", "a", emptyList(), "Lnex;") to false,
        GboardMethodTarget("Lnib;", "g", listOf("Lnex;", "Z"), "V") to false,
        GboardMethodTarget("Lxae;", "n", listOf("Ljava/util/Collection;"), "Lxae;") to true,
    )

    fun validate(context: BytecodePatchContext) {
        var owner: String? = OWNER
        val seen = mutableSetOf<String>()
        while (owner != null && owner != "Landroid/inputmethodservice/InputMethodService;" &&
            seen.add(owner)) {
            owner = context.mutableClass(owner).superclass
        }
        check(owner == "Landroid/inputmethodservice/InputMethodService;") {
            "Editing buttons lifecycle owner must extend InputMethodService"
        }
        (descriptorMethods + resetMethods.map { it to false }).forEach { (target, isStatic) ->
            val method = target.resolve(context)
            check(AccessFlags.STATIC.isSet(method.accessFlags) == isStatic) {
                "Unexpected static shape: ${target.reference}"
            }
            check(method.implementation != null) { "Missing body: ${target.reference}" }
        }
        listOf("d").forEach { name ->
            val field = GboardFieldTarget("Lnes;", name, "Ljava/lang/String;").resolve(context)
            check(!AccessFlags.STATIC.isSet(field.accessFlags)) { "Unexpected builder field: $name" }
        }
    }
}
