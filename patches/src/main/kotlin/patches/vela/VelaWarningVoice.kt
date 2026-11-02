package patches.vela

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val SPEAKER = "Lnl/johninnl/vela/VelaWarningSpeaker;"
/** Independent original-artifact guards, checked before any telemetry or voice mutation. */
internal val velaWarningVoiceContracts = mapOf(
    "Lwy4;->b()Z" to Contract(4, 21, 17, "f8de40ec32e92d21714dbff7f1cdd281ee5f309bfe636c5f22362c5b35fddf2e"),
    "Lwy4;->l(Ljava/lang/String;Z)V" to Contract(16, 203, 17, "b3f4b99200101e866aa4a336fab764f25e88e6eb7791903be2a46fd9922a38d5"),
    "Lpn1;-><init>(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ZLjava/lang/Object;B)V" to Contract(7, 8, 69633, "27eef516d92545c66d29cf6cdfcdebf28c1cce6bb90f28e3e623b485a06d58ff"),
    "Lpn1;->invoke()Ljava/lang/Object;" to Contract(10, 68, 17, "87034ad4e7fb508ab9e50109efbb3d64f603a5bb96a75a30c2c600d54a95315e"),
    "Lty4;->onAudioFocusChange(I)V" to Contract(3, 25, 17, "802d2c2c8bb117f59849f58ca595919ed573b77224b9ec005b38dcdef8cb5803"),
    "Lvy4;->onDone(Ljava/lang/String;)V" to Contract(4, 11, 17, "c1a2809e2be620c33a9c45e64088e83aea4da1c5443b7c8da80886f95484d5e4"),
    "Lvy4;->onError(Ljava/lang/String;)V" to Contract(4, 11, 17, "c1a2809e2be620c33a9c45e64088e83aea4da1c5443b7c8da80886f95484d5e4"),
    "Lvy4;->onStart(Ljava/lang/String;)V" to Contract(2, 1, 17, "4e0cd0d219f0f23e01f5a9d23ac0586a022973bad3939eb21158d37da6746735"),
    "Lvy4;->onStop(Ljava/lang/String;Z)V" to Contract(4, 11, 17, "2e2ad611cf7bb32fac9e068938e55783af961d9b5f9e86e16c66c8cf57ec6064"),
    "Lwy4;->d(Ljava/lang/String;)V" to Contract(7, 84, 17, "a9a87055e86c376ce5abe69102f17c3b04eff5d07d2f51eae038c1be5b843600"),
    "Lwy4;->g()V" to Contract(6, 34, 17, "78f9ea92475c9f214ef770dff57bb98fa41e81701b48cfef0f6fd2e06bd24bcf"),
    "Lwy4;->n()V" to Contract(3, 24, 17, "cbc73562d6c102e4fbd952193d5fc6bad845504054b48f11101b74acec237eac"),
)

internal fun validateVelaWarningVoice(classes: Iterable<ClassDef>): Map<String,Method> {
    val found=mutableMapOf<String,Method>()
    val types=classes.associateBy { it.type }
    classes.forEach { cls -> cls.methods.forEach { method ->
        velaWarningVoiceContracts[method.toString()]?.let { c ->
            check(cls.sourceFile==VELA_MAP_ID && method.accessFlags==c.flags &&
                method.implementation?.registerCount==c.registers &&
                method.implementation!!.instructions.count()==c.count && velaFingerprint(method)==c.hash) {
                "Changed or already patched Vela warning voice ${method}"
            }
            check(found.put(method.toString(),method)==null) { "Duplicate Vela warning voice $method" }
        }
    } }
    check(found.keys==velaWarningVoiceContracts.keys) { "Missing Vela warning voice contracts ${velaWarningVoiceContracts.keys-found.keys}" }
    fun field(owner:String,name:String,type:String) {
        check(types.getValue(owner).fields.count { it.name==name && it.type==type }==1) { "Changed Vela warning field $owner->$name:$type" }
    }
    mapOf("b" to "Landroid/speech/tts/TextToSpeech;", "d" to "F", "e" to "Z", "g" to "Ljj;",
        "h" to "Z", "i" to "Z", "j" to "Ljava/lang/String;", "m" to "Z", "n" to "Z", "o" to "Z", "s" to "I", "p" to "Landroid/media/AudioManager;").forEach { (name,type)->field("Lwy4;",name,type) }
    field("Lpn1;","f","B");field("Lpn1;","h","Ljava/lang/Object;");field("Lpn1;","i","Ljava/lang/Object;")
    field("Lvy4;","a","Lwy4;");field("Lty4;","a","Lwy4;")
    check(types.getValue("Lwy4;").methods.count { it.toString()=="Lwy4;->f()V" }==1)
    return found
}

context(context: BytecodePatchContext)
internal fun validateVelaWarningExtension() {
    listOf("language(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;", "capture(Ljava/lang/Object;)V", "dispatch(Ljava/lang/Object;)Z", "focusRequest(Ljava/lang/Object;)V",
        "focusResult(Ljava/lang/Object;I)V", "nativeStop(Ljava/lang/Object;)V", "nativeSpeech(Ljava/lang/Object;)V", "focusChanged(Ljava/lang/Object;I)V",
        "utteranceStart(Ljava/lang/Object;Ljava/lang/String;)V", "utteranceDone(Ljava/lang/Object;Ljava/lang/String;)Z",
        "utteranceError(Ljava/lang/Object;Ljava/lang/String;)Z", "utteranceStopped(Ljava/lang/Object;Ljava/lang/String;)Z").forEach { signature ->
        check(context.classDefBy(SPEAKER).methods.count { it.toString()=="$SPEAKER->$signature" }==1) { "Missing Vela warning extension $signature" }
    }
}

internal fun bindVelaWarningVoice(methods:Map<String,MutableMethod>) {
    fun pick(owner:String,name:String)=methods.values.single { it.definingClass==owner && it.name==name }
    val constructor=pick("Lpn1;","<init>")
    constructor.instructions.withIndex().filter { it.value.opcode==Opcode.RETURN_VOID }.map { it.index }.reversed().forEach {
        constructor.addInstructions(it,"invoke-static/range {p0 .. p0}, $SPEAKER->capture(Ljava/lang/Object;)V")
    }
    val dispatch=pick("Lpn1;","invoke")
    dispatch.addInstructionsWithLabels(0,"""
        invoke-static/range {p0 .. p0}, $SPEAKER->dispatch(Ljava/lang/Object;)Z
        move-result v0
        if-eqz v0, :vela_native_warning_closure
        sget-object v0, Ler4;->a:Ler4;
        return-object v0
    """,ExternalLabel("vela_native_warning_closure",dispatch.getInstruction(0)))
    val speech=pick("Lwy4;","l")
    val language=speech.instructions.withIndex().single { (it.value as? ReferenceInstruction)?.reference?.toString()=="Lwy4;->o()Ljava/lang/String;" }.index
    check(speech.getInstruction(language+1).opcode==Opcode.MOVE_RESULT_OBJECT)
    val tag=speech.getInstruction<OneRegisterInstruction>(language+1).registerA
    speech.addInstructions(language+2,"""
        invoke-static {p0, v$tag}, $SPEAKER->language(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;
        move-result-object v$tag
    """)
    speech.addInstructions(0,"invoke-static/range {p0 .. p0}, $SPEAKER->nativeSpeech(Ljava/lang/Object;)V")
    val focus=pick("Lwy4;","g")
    val request=focus.instructions.withIndex().single {
        (it.value as? ReferenceInstruction)?.reference?.toString()=="Landroid/media/AudioManager;->requestAudioFocus(Landroid/media/AudioFocusRequest;)I"
    }.index
    check(focus.getInstruction(request+1).opcode==Opcode.MOVE_RESULT)
    val result=focus.getInstruction<OneRegisterInstruction>(request+1).registerA
    focus.addInstructions(request+2,"invoke-static {p0, v$result}, $SPEAKER->focusResult(Ljava/lang/Object;I)V")
    focus.addInstructions(0,"invoke-static/range {p0 .. p0}, $SPEAKER->focusRequest(Ljava/lang/Object;)V")
    listOf("n","d").forEach { pick("Lwy4;",it).addInstructions(0,"invoke-static/range {p0 .. p0}, $SPEAKER->nativeStop(Ljava/lang/Object;)V") }
    pick("Lty4;","onAudioFocusChange").addInstructions(0,"invoke-static/range {p0 .. p1}, $SPEAKER->focusChanged(Ljava/lang/Object;I)V")
    pick("Lvy4;","onStart").addInstructions(0,"invoke-static/range {p0 .. p1}, $SPEAKER->utteranceStart(Ljava/lang/Object;Ljava/lang/String;)V")
    mapOf("onDone" to "utteranceDone", "onError" to "utteranceError", "onStop" to "utteranceStopped").forEach { (name,hook) ->
        val m=pick("Lvy4;",name)
        m.addInstructionsWithLabels(0,"""
            invoke-static/range {p0 .. p1}, $SPEAKER->$hook(Ljava/lang/Object;Ljava/lang/String;)Z
            move-result v0
            if-eqz v0, :vela_native_utterance
            return-void
        """,ExternalLabel("vela_native_utterance",m.getInstruction(0)))
    }
}
