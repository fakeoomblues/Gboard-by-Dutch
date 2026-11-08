package org.ungoogled.patches.maps.car

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val CONTENT = "Lpatches/maps/gearslip/NativeCarContent;"

/** Guard complete native lists and genuine callbacks, independently of visible row count. */
internal fun validateMapsNativeContent(classBy: (String) -> ClassDef) {
    fun field(owner: String, name: String, type: String) {
        check(classBy(owner).fields.count { it.name == name && it.type == type } == 1) { "Maps native content field changed: $owner->$name" }
    }
    fun method(owner: String, name: String, parameters: List<String>, result: String) {
        check(classBy(owner).methods.count { it.name == name && it.parameterTypes.map(CharSequence::toString) == parameters && it.returnType == result } == 1) { "Maps native content method changed: $owner->$name" }
    }
    method("Lrmo;", "a", emptyList(), "Ljava/util/List;")
    field("Lrmo;", "a", "Lbhfu;")
    field("Lrmo;", "c", "Luvs;")
    field("Lqdf;", "b", "Lbhfu;")
    field("Lqdf;", "c", "Landroid/content/Context;")
    field("Lqbn;", "b", "Lbhfu;")
    field("Lqbn;", "p", "Landroid/content/Context;")
    field("Lsso;", "b", "Landroid/content/Context;")
    field("Lsso;", "c", "Lbhfu;")
    method("Lqcq;", "d", emptyList(), "Lcom/google/common/collect/ImmutableList;")
    method("Lqbh;", "c", emptyList(), "Lcom/google/common/collect/ImmutableList;")
    method("Lqbh;", "b", emptyList(), "Lbhhr;")
    method("Lrlr;", "b", emptyList(), "Lrls;")
    for (name in listOf("f", "g", "h")) method("Lrls;", name, emptyList(), "Ljava/lang/CharSequence;")
    method("Lrls;", "i", emptyList(), "V")
    for (name in listOf("g", "h", "i")) method("Lqcr;", name, emptyList(), "Ljava/lang/String;")
    method("Lqcr;", "j", emptyList(), "V")
    field("Lqbj;", "a", "Lqaq;")
    for (name in listOf("d", "e")) method("Lqaq;", name, emptyList(), "Ljava/lang/CharSequence;")
    field("Lqbo;", "a", "Ljava/lang/CharSequence;")
    method("Lpoe;", "<init>", listOf("I"), "V")
    method("Lpoe;", "accept", listOf("Ljava/lang/Object;"), "V")
    for (owner in listOf("Lqca;", "Lqbc;", "Lsnk;")) method(owner, "a", listOf("Lbhhr;"), "Ljava/lang/Object;")
    field("Lssf;", "aG", "Lssi;")
    method("Lssi;", "a", emptyList(), "Landroid/view/View;")
    method("Lssf;", "b", emptyList(), "Landroid/view/View;")
    method("Lssf;", "n", emptyList(), "Lstp;")
    method("Lstp;", "a", emptyList(), "Lsnt;")
    method("Lsnt;", "K", emptyList(), "Z")
    method("Lsnt;", "l", emptyList(), "Ljava/lang/CharSequence;")
    method("Lsnt;", "r", emptyList(), "V")
    method("Lsso;", "r", emptyList(), "V")
    field("Lruo;", "a", "Lrvr;")
    field("Lruo;", "p", "Landroid/view/View;")
    method("Lruo;", "b", emptyList(), "Landroid/view/View;")
    for (name in listOf("b", "c", "d", "e", "f", "g", "m")) field("Lrvr;", name, "Lrvo;")
    for (name in listOf("h", "i")) field("Lrvr;", name, "Lrvp;")
    field("Lrvr;", "l", "Z")
    field("Lrvr;", "n", "Lbhfu;")
    method("Lrvo;", "c", emptyList(), "Ljava/lang/String;")
    method("Lrvo;", "d", emptyList(), "V")
    method("Lrvp;", "d", emptyList(), "V")
    // These distinguish the real actions from the empty pre-click M()/f() hooks.
    val start = classBy("Lsso;").methods.single { it.name == "r" && it.parameterTypes.isEmpty() }
    check(start.implementation!!.instructions.any {
        val ref = (it as? ReferenceInstruction)?.reference as? MethodReference
        ref?.definingClass == "Lsqf;" && ref.name == "e" && ref.parameterTypes.map(CharSequence::toString) == listOf("Z")
    }) { "Maps native Start callback changed" }
    val action = classBy("Lrvp;").methods.single { it.name == "d" && it.parameterTypes.isEmpty() }
    check(action.implementation!!.instructions.any {
        val ref = (it as? ReferenceInstruction)?.reference as? MethodReference
        ref?.definingClass == "Ljava/lang/Runnable;" && ref.name == "run"
    }) { "Maps native footer callback changed" }
    val constructor = classBy("Lpux;").methods.single { it.name == "<init>" }
    val ids = constructor.implementation!!.instructions.mapNotNull { (it as? WideLiteralInstruction)?.wideLiteral }.toSet()
    check(ids.containsAll(setOf(0x7f0b069dL, 0x7f0b0adeL))) { "Maps native car card slots changed" }
    val zoom = classBy("Lrfi;").methods.single { it.name == "a" && it.parameterTypes.isEmpty() && it.returnType == "Lbhjq;" }
    check(zoom.implementation!!.instructions.any { (it as? WideLiteralInstruction)?.wideLiteral == 0x7f0b0d88L }) {
        "Maps separate native car zoom widget changed"
    }
}

internal fun bindMapsNativeContent(
    classBy: (String) -> ClassDef,
    mutableMethod: (String, String, List<String>, String) -> MutableMethod,
) {
    validateMapsNativeContent(classBy)
    for (owner in listOf("Lqca;", "Lqbc;", "Lsnk;")) {
        mutableMethod(owner, "a", listOf("Lbhhr;"), "Ljava/lang/Object;")
            .addInstructions(0, "invoke-static/range {p1 .. p1}, $CONTENT->capture(Ljava/lang/Object;)V")
    }
    for (owner in listOf("Lssf;", "Lruo;")) {
        mutableMethod(owner, "b", emptyList(), "Landroid/view/View;")
            .addInstructions(0, "invoke-static/range {p0 .. p0}, $CONTENT->panelSource(Ljava/lang/Object;)V")
    }
    val suggestions = mutableMethod("Lrmo;", "a", emptyList(), "Ljava/util/List;")
    suggestions.implementation!!.instructions.withIndex().filter { it.value.opcode == Opcode.RETURN_OBJECT }.reversed().forEach { (index, instruction) ->
        val register = (instruction as OneRegisterInstruction).registerA
        suggestions.replaceInstruction(index, "invoke-static/range {v$register .. v$register}, $CONTENT->suggestions(Ljava/util/List;)V")
        suggestions.addInstructions(index + 1, "return-object v$register")
    }
    suggestions.addInstructions(0, "invoke-static/range {p0 .. p0}, $CONTENT->readingSuggestions(Ljava/lang/Object;)V")
}
