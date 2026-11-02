package patches.flitsmeister

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val WARNINGS="Lnl/johninnl/flitsmeister/NativeWarnings;"
private const val REPORT="Lnl/flitsmeister/fmcore/data/model/reports/BaseReport;"
private const val EARLY_TYPE="Lnl/flitsmeister/earlywarning/model/EarlyWarningTypeEnum;"
private const val EARLY_REPORT="Lnl/flitsmeister/fmcore/models/data/reports/EarlyWarningReport;"
private const val EARLY_NOTIFICATION="Lnl/flitsmeister/earlywarning/model/EarlyWarningNotification;"
private const val EARLY_VIEW="Lnl/flitsmeister/fmcore/models/data/earlywarnings/EarlyWarning;"

/** Resolve every native authority before edits; parameters and native completion stay untouched. */
context(context: BytecodePatchContext)
internal fun bindFlitsmeisterNativeWarnings() {
    fun shape(method:MutableMethod,registers:Int,count:Int):MutableMethod {
        check(method.implementation?.registerCount==registers&&method.instructions.size==count) { "Changed native warning authority: ${method.definingClass}->${method.name}" }
        return method
    }
    val regular=shape(exact("Ls6i;","l","V",REPORT,"Z","Z","Ljava/lang/String;","Lz39;").method,16,136)
    val early=shape(exact("Ls6i;","b","V",EARLY_TYPE,EARLY_REPORT,EARLY_NOTIFICATION,"Ljava/lang/String;","Z","Lz39;").method,18,40)
    val voiceOnly=shape(exact("Lxl\$c;","invokeSuspend","Ljava/lang/Object;","Ljava/lang/Object;").method,14,119)
    val cits=shape(exact("Lfc0;","x","V",REPORT).method,27,610)
    val presentation=shape(exact("Lxl;","k","V","Landroid/content/Context;",EARLY_VIEW,"Z").method,11,69)
    val converted=shape(exact("Lvz2;","J","Z","Lbemobile/cits/sdk/core/model/response/generalObjects/BaseEvent;").method,31,279)
    fun anchor(method:MutableMethod,reference:String,opcode:Opcode?=null):Int {
        val positions=method.instructions.withIndex().filter { (it.value as? ReferenceInstruction)?.reference?.toString()==reference && (opcode==null||it.value.opcode==opcode) }
        check(positions.size==1) { "Changed warning anchor $reference in ${method.definingClass}->${method.name}" }
        return positions.single().index
    }
    // This getter exists only after the resumed suppression and VOICE_ONLY tests.
    val voiceReference="Lnl/flitsmeister/fmcore/data/model/reports/Incident;->getVoiceOnlyText()Ljava/lang/String;"
    val voiceAt=anchor(voiceOnly,voiceReference,Opcode.INVOKE_VIRTUAL)
    val voiceRegister=(voiceOnly.instructions[voiceAt] as FiveRegisterInstruction).registerC
    val citsType="Lnl/flitsmeister/fmcore/models/data/cits/CitsBaseReport;"
    val citsAt=anchor(cits,citsType,Opcode.CHECK_CAST)
    val citsRegister=(cits.instructions[citsAt] as OneRegisterInstruction).registerA
    val busAt=anchor(converted,"Lnl/flitsmeister/fmcore/models/eventbuses/EarlyWarningsBus;->getBusAlert()Lfs7;")
    val publishes=converted.instructions.withIndex().filter { it.index in busAt+1..busAt+5 && (it.value as? ReferenceInstruction)?.reference?.toString()=="Lfs7;->a(Ljava/lang/Object;)V" }
    check(publishes.size==1) { "Changed CITS early-warning projection publication" }
    val publishAt=publishes.single().index
    val publish=publishes.single().value as FiveRegisterInstruction
    check(publish.registerCount==2) { "Changed CITS projection registers" }
    val projectionRegister=publish.registerD
    // Entry capture precedes g(), BLE routing and every local audio preference.
    regular.addInstruction(0,"invoke-static/range {p0 .. p5}, $WARNINGS->regular(Ljava/lang/Object;Ljava/lang/Object;ZZLjava/lang/String;Ljava/lang/Object;)V")
    early.addInstruction(0,"invoke-static/range {p0 .. p6}, $WARNINGS->early(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;ZLjava/lang/Object;)V")
    presentation.addInstruction(0,"invoke-static/range {p1 .. p3}, $WARNINGS->presentation(Landroid/content/Context;Ljava/lang/Object;Z)V")
    // Replace anchors so native incoming labels also execute the observer.
    voiceOnly.replaceInstruction(voiceAt,"invoke-static/range {v$voiceRegister .. v$voiceRegister}, $WARNINGS->voiceOnly(Ljava/lang/Object;)V")
    voiceOnly.addInstruction(voiceAt+1,"invoke-virtual {v$voiceRegister}, $voiceReference")
    cits.replaceInstruction(citsAt,"invoke-static/range {v$citsRegister .. v$citsRegister}, $WARNINGS->cits(Ljava/lang/Object;)V")
    cits.addInstruction(citsAt+1,"check-cast v$citsRegister, $citsType")
    converted.replaceInstruction(publishAt,"invoke-static/range {v$projectionRegister .. v$projectionRegister}, $WARNINGS->suppressProjection(Ljava/lang/Object;)V")
    converted.addInstruction(publishAt+1,"invoke-virtual {v${publish.registerC}, v$projectionRegister}, Lfs7;->a(Ljava/lang/Object;)V")
}
