package org.ungoogled.patches.maps.ui.speed

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test

class MapsWarningVoiceTest {
    @Test fun pinnedOriginalHasOneFinalDispatchFenceAndKeepsNativeTerminalHandling() {
        val path=System.getenv("MAPS_ORIGINAL_APK");assumeNotNull(path)
        val dex=DexFileFactory.loadDexContainer(File(path!!),Opcodes.forApi(29))
        val classes=dex.dexEntryNames.flatMap { dex.getEntry(it)!!.dexFile.classes }
        val original=validateMapsWarningVoice(classes)
        assertEquals(33,original.size)
        val methods=original.mapValues { MutableMethod(it.value) }
        bindMapsNativeVoice(methods)
        val dispatch=methods.getValue("Lbmtf;->b(Ljava/lang/Object;)V")
        val refs=dispatch.implementation!!.instructions.mapNotNull { (it as? ReferenceInstruction)?.reference?.toString() }
        assertEquals(0,refs.count { it=="Lbmuc;->d(Lbmua;)V" })
        assertEquals(1,refs.count { it=="Lorg/ungoogled/ui/MapsGuidanceCoordinator;->dispatch(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V" })
        assertEquals(16,dispatch.implementation!!.registerCount)
        val terminal=methods.getValue("Lbmth;->a(Lbmuc;)V")
        assertEquals(original.getValue(terminal.toString()).implementation!!.instructions.count()+1,terminal.implementation!!.instructions.count())
        val terminalRefs=terminal.implementation!!.instructions.mapNotNull { (it as? ReferenceInstruction)?.reference?.toString() }
        assertEquals("Lorg/ungoogled/ui/MapsGuidanceCoordinator;->terminal(Ljava/lang/Object;Ljava/lang/Object;)V",terminalRefs.first())
        assertEquals(1,terminalRefs.count { it=="Lbmug;->a(Lbmuf;Lbmub;)V" })
        assertThrows(IllegalStateException::class.java) {
            validateMapsWarningVoice(classes.map { cls ->
                if(cls.type!="Lbmtf;")cls else com.android.tools.smali.dexlib2.immutable.ImmutableClassDef(
                    cls.type,cls.accessFlags,cls.superclass,cls.interfaces,cls.sourceFile,cls.annotations,
                    cls.fields,cls.methods.map { methods[it.toString()]?:it })
            })
        }
    }
}
