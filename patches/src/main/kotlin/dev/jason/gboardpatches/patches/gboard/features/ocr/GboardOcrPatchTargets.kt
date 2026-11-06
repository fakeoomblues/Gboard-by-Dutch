package dev.jason.gboardpatches.patches.gboard.features.ocr

import dev.jason.gboardpatches.patches.gboard.shared.GboardMethodTarget
import dev.jason.gboardpatches.patches.gboard.shared.GboardFieldTarget
import dev.jason.gboardpatches.patches.gboard.shared.mutableClass
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags

internal object GboardOcrPatchTargets {
    val taskCompletionListener = GboardMethodTarget("Lmpr;", "l", listOf("Lxgp;"), "V")

    internal fun requireTaskCompletionRegistration(owner: MutableClass): MutableMethod {
        val method = taskCompletionListener.resolve(owner)
        check(owner.accessFlags == (AccessFlags.PUBLIC.value or AccessFlags.ABSTRACT.value))
        check(method.accessFlags == AccessFlags.PUBLIC.value) { "Expected public instance task registration" }
        return method
    }

    fun preflight(context: BytecodePatchContext) {
        val methods = listOf(configLabel, languageHint, loggingLibrary, optionalModuleName, moduleId,
            moduleAvailability, optionalModuleBuilder, moduleManagerRequest, thinRecognizerInit) + listOf(
            GboardMethodTarget("Llrf;", "<init>", listOf("Ljava/lang/String;", "I", "J", "Z"), "V"),
            GboardMethodTarget("Lzwz;", "b", emptyList(), "Lzwz;"),
            GboardMethodTarget("Lzwz;", "a", emptyList(), "Landroid/content/Context;"),
            GboardMethodTarget("Lmbc;", "a", listOf("Landroid/content/Context;", "Ljava/lang/String;"), "I"),
            GboardMethodTarget("Lmbc;", "d", listOf("Landroid/content/Context;", "Lmbb;", "Ljava/lang/String;"), "Lmbc;"),
            GboardMethodTarget("Lmbc;", "c", listOf("Ljava/lang/String;"), "Landroid/os/IBinder;"),
            GboardMethodTarget("Lmap;", "<init>", listOf("Ljava/lang/Object;"), "V"),
            GboardMethodTarget("Laatg;", "<init>", listOf("Landroid/os/IBinder;"), "V"),
            GboardMethodTarget("Laatf;", "<init>", listOf("Landroid/os/IBinder;"), "V"),
            GboardMethodTarget("Lemt;", "a", emptyList(), "Landroid/os/Parcel;"),
            GboardMethodTarget("Lemt;", "z", listOf("I", "Landroid/os/Parcel;"), "Landroid/os/Parcel;"),
            GboardMethodTarget("Lemt;", "C", listOf("I", "Landroid/os/Parcel;"), "V"),
            GboardMethodTarget("Lemv;", "d", listOf("Landroid/os/Parcel;", "Landroid/os/IInterface;"), "V"),
            GboardMethodTarget("Lemv;", "c", listOf("Landroid/os/Parcel;", "Landroid/os/Parcelable;"), "V"),
            GboardMethodTarget("Laatl;", "<init>", listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Z", "I", "Ljava/lang/String;", "Z"), "V"),
            GboardMethodTarget("Laatq;", "b", emptyList(), "V"),
            GboardMethodTarget("Lzxe;", "<init>", listOf("[Llrf;"), "V"),
            GboardMethodTarget("Llyu;", "<init>", listOf("Landroid/content/Context;"), "V"),
            GboardMethodTarget("Llyu;", "a", listOf("[Lvvp;"), "Lmpr;"),
            GboardMethodTarget("Llyu;", "b", listOf("Lackq;"), "V"),
            GboardMethodTarget("Llyu;", "c", listOf("Lsrk;"), "Lmpr;"),
            GboardMethodTarget("Lsrk;", "<init>", listOf("Ljava/util/List;", "Lackq;"), "V"),
            GboardMethodTarget("Lackq;", "a", listOf("Llyi;"), "V"),
            GboardMethodTarget("Lxgp;", "a", listOf("Lmpr;"), "V"),
            GboardMethodTarget("Lmpr;", "g", emptyList(), "Z"),
            GboardMethodTarget("Lmpr;", "e", emptyList(), "Ljava/lang/Object;"),
            GboardMethodTarget("Lmpr;", "d", emptyList(), "Ljava/lang/Exception;"),
        )
        methods.forEach { it.resolve(context) }
        requireTaskCompletionRegistration(context.mutableClass("Lmpr;"))
        listOf(
            GboardFieldTarget("Laatq;", "a", "Landroid/content/Context;"),
            GboardFieldTarget("Laatq;", "b", "Laatd;"),
            GboardFieldTarget("Laatq;", "f", "Laatf;"),
            GboardFieldTarget("Lmbc;", "b", "Lmbb;"),
            GboardFieldTarget("Llrf;", "a", "Ljava/lang/String;"),
            GboardFieldTarget("Llyi;", "b", "I"), GboardFieldTarget("Llyi;", "e", "I"),
            GboardFieldTarget("Llyh;", "a", "I"), GboardFieldTarget("Llyh;", "b", "Z"),
        ).forEach { it.resolve(context) }
        check(context.mutableClass("Laatg;").superclass == "Lemt;")
        check(context.mutableClass("Laatf;").superclass == "Lemt;")
        check("Laatd;" in context.mutableClass("Laauc;").interfaces)
        check("Lvvp;" in context.mutableClass("Lzxe;").interfaces)
    }
    val configLabel = method("b", "Ljava/lang/String;")
    val languageHint = method("c", "Ljava/lang/String;")
    val loggingLibrary = method("d", "Ljava/lang/String;")
    val optionalModuleName = method("e", "Ljava/lang/String;")
    val moduleId = method("f", "Ljava/lang/String;")
    val moduleAvailability = method("g", "Z")

    val optionalModuleBuilder = GboardMethodTarget(
        classType = "Lzws;",
        name = "a",
        parameterTypes = listOf("Laatd;"),
        returnType = "[Llrf;",
    )
    val moduleManagerRequest = GboardMethodTarget(
        classType = "Laatx;",
        name = "a",
        parameterTypes = emptyList(),
        returnType = "[Llrf;",
    )
    val thinRecognizerInit = GboardMethodTarget(
        classType = "Laatq;",
        name = "a",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    private fun method(name: String, returnType: String) = GboardMethodTarget(
        classType = "Laauc;",
        name = name,
        parameterTypes = emptyList(),
        returnType = returnType,
    )
}
