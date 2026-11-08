package org.ungoogled.patches.maps.car

import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import java.io.File
import org.junit.Assert.assertThrows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import org.junit.Assume.assumeNotNull
import org.junit.Test

class MapsGuidanceComponentTest {
    @Test fun privateTokenGuardExcludesBothUnsupportedClientsAndPreservesOtherConnectionPaths() {
        val path=System.getenv("MAPS_CAR_ORIGINAL_APK");assumeNotNull(path)
        val dex=DexFileFactory.loadDexContainer(File(path!!),Opcodes.forApi(29))
        val original=dex.dexEntryNames.flatMap { dex.getEntry(it)!!.dexFile.classes }
            .single { it.type=="Lbeop;" }.methods.single { it.name=="e"&&it.parameterTypes.isEmpty()&&it.returnType=="V" }
        val before=original.implementation!!.instructions.toList();val mutable=MutableMethod(original)
        guardMapsPrivateCarToken(mutable);val after=mutable.instructions.toList()
        assertEquals(before.size+6,after.size)
        assertEquals("Lbeop;->d:Lbeqa;",(after[0] as ReferenceInstruction).reference.toString())
        assertEquals("Lbeqt;",(after[1] as ReferenceInstruction).reference.toString())
        assertEquals("Lpxt;",(after[3] as ReferenceInstruction).reference.toString())
        assertEquals(Opcode.IF_NEZ,after[2].opcode);assertEquals(Opcode.IF_EQZ,after[4].opcode);assertEquals(Opcode.RETURN_VOID,after[5].opcode)
        val pc=mutableListOf(0);after.forEach { pc.add(pc.last()+it.codeUnits) }
        assertEquals(pc[5],pc[2]+(after[2] as OffsetInstruction).codeOffset)
        assertEquals(pc[6],pc[4]+(after[4] as OffsetInstruction).codeOffset)
        before.forEachIndexed { index,instruction ->
            assertEquals(instruction.opcode,after[index+6].opcode)
            assertEquals((instruction as? ReferenceInstruction)?.reference?.toString(),(after[index+6] as? ReferenceInstruction)?.reference?.toString())
        }
        assertTrue(after.drop(6).isNotEmpty())
    }
    @Test fun suppliedOriginalExposesTheConcreteCachedComponentAndCurrentListener() {
        val path = System.getenv("MAPS_CAR_ORIGINAL_APK"); assumeNotNull(path)
        val dex = DexFileFactory.loadDexContainer(File(path!!), Opcodes.forApi(29))
        val classes = dex.dexEntryNames.flatMap { dex.getEntry(it)!!.dexFile.classes }.associateBy { it.type }
        validateMapsGuidanceComponent { classes.getValue(it) }
        validateMapsNavigationProvider { classes.getValue(it) }
        validateMapsNativeContent { classes.getValue(it) }
        // An old interface can still exist while the actual application no longer
        // implements it. Reject that mismatch before emitting another broken APK.
        val app = classes.getValue("Lcom/google/android/apps/gmm/base/app/GoogleMapsApplication;")
        val disconnected = ImmutableClassDef(app.type, app.accessFlags, "Landroid/app/Application;",
            app.interfaces, app.sourceFile, app.annotations, app.fields, app.methods)
        assertThrows(IllegalStateException::class.java) {
            validateMapsGuidanceComponent { if (it == app.type) disconnected else classes.getValue(it) }
        }
        val binder = classes.getValue("Llvp;")
        val missingDispatch = ImmutableClassDef(binder.type, binder.accessFlags, binder.superclass,
            binder.interfaces, binder.sourceFile, binder.annotations, binder.fields, binder.methods.filter { it.name != "y" })
        assertThrows(IllegalStateException::class.java) {
            validateMapsNavigationProvider { if (it == binder.type) missingDispatch else classes.getValue(it) }
        }
        val action=classes.getValue("Lrvp;")
        val missingClick=ImmutableClassDef(action.type,action.accessFlags,action.superclass,action.interfaces,action.sourceFile,action.annotations,action.fields,action.methods.filter { it.name!="d" })
        assertThrows(IllegalStateException::class.java){validateMapsNativeContent { if(it==action.type)missingClick else classes.getValue(it) }}
    }
}
