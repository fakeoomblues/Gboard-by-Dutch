package patches.waze

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest

class WazeNativeWarningHooksTest {
    private fun original(): List<ClassDef> {
        val path = System.getenv("WAZE_ORIGINAL_APK")
        assumeNotNull(path)
        val file = File(path!!)
        val hash = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        assertEquals("Use the supplied original base APK", "b1504af4bcbe50325195774a891f50f7189cdef7ad38ee67a2f5b7af00f29e4d", hash)
        val dex = DexFileFactory.loadDexContainer(file, Opcodes.forApi(29))
        val classes = dex.dexEntryNames.flatMap { dex.getEntry(it)!!.dexFile.classes }
        validateWazeWarningClassAbi(classes.filter { it.type in wazeWarningClassAbi }.associateBy { it.type })
        return classes
    }

    private fun roots(classes: Iterable<ClassDef>): Map<String, Method> = classes.flatMap { it.methods }
        .filter { it.toString() in wazeWarningContracts }.associateBy { it.toString() }

    private fun nativeReferences(method: Method) = method.implementation!!.instructions.mapNotNull {
        (it as? ReferenceInstruction)?.reference?.toString()
    }.filterNot { it.startsWith("$WAZE_WARNINGS->") }

    @Test fun nativeDecisionsRetainEveryOriginalCallAndRoundTripThroughDexAssembly() {
        val classes = original()
        val originals = roots(classes)
        assertEquals(wazeWarningContracts.keys, originals.keys)
        val changed = originals.mapValues { MutableMethod(it.value) }
        bindWazeNativeWarnings(changed)
        val expected = mapOf(WAZE_SOUND_ROOT to "sound", WAZE_ZONE_EXIT_ROOT to "zoneExit", WAZE_ZONE_SELECTED_ROOT to "zoneSelected",
            WAZE_NOTIFICATION_ADD_ROOT to "notificationAdded", WAZE_NOTIFICATION_REMOVE_ROOT to "notificationRemoved")
        expected.forEach { (signature, name) ->
            val method = changed.getValue(signature)
            val hooks = method.implementation!!.instructions.withIndex().filter {
                (it.value as? ReferenceInstruction)?.reference?.toString() == "$WAZE_WARNINGS->$name(Ljava/lang/Object;)V"
            }
            assertEquals(1, hooks.size)
            val hook = hooks.single()
            assertEquals(Opcode.INVOKE_STATIC_RANGE, hook.value.opcode)
            val invocation = hook.value as RegisterRangeInstruction
            assertEquals(1, invocation.registerCount)
            if (signature == WAZE_ZONE_EXIT_ROOT) {
                assertEquals("Only the verified LeftZone branch captures exit", 7, hook.index)
                assertEquals(2, invocation.startRegister)
                val native = method.implementation!!.instructions.toList()[8]
                assertEquals(Opcode.IGET_OBJECT, native.opcode)
                assertEquals(2, (native as TwoRegisterInstruction).registerA)
                assertEquals(2, native.registerB)
            } else assertEquals(0, hook.index)
        }
        val zoneBody = changed.getValue(WAZE_ZONE_EXIT_ROOT).implementation!!.instructions.toList()
        val presented = zoneBody.withIndex().single {
            (it.value as? ReferenceInstruction)?.reference?.toString() == "$WAZE_WARNINGS->zonePresented(Ljava/lang/Object;)V"
        }
        assertEquals("InZone marker precedes p0 register reuse", 13, presented.index)
        assertEquals(2, (presented.value as RegisterRangeInstruction).startRegister)
        val enterRead = zoneBody[presented.index + 1] as TwoRegisterInstruction
        assertEquals(0, enterRead.registerA)
        assertEquals(2, enterRead.registerB)
        originals.forEach { (signature, method) ->
            assertEquals(nativeReferences(method), nativeReferences(changed.getValue(signature)))
            if (signature !in expected) assertEquals(wazeWarningFingerprint(method), wazeWarningFingerprint(changed.getValue(signature)))
        }
        val pool = DexPool(Opcodes.forApi(29))
        val owners = changed.values.map { it.definingClass }.toSet()
        classes.filter { it.type in owners }.forEach { cls ->
            pool.internClass(ImmutableClassDef(cls.type, cls.accessFlags, cls.superclass, cls.interfaces,
                cls.sourceFile, cls.annotations, cls.fields, cls.methods.map { changed[it.toString()] ?: it }))
        }
        val store = MemoryDataStore()
        val bytes = try { pool.writeTo(store); store.data } finally { store.close() }
        val assembled = roots(DexBackedDexFile.fromInputStream(Opcodes.forApi(29), ByteArrayInputStream(bytes)).classes)
        assertEquals(originals.keys, assembled.keys)
        originals.forEach { (signature, method) -> assertEquals(nativeReferences(method), nativeReferences(assembled.getValue(signature))) }
    }

    @Test fun changedAbiOrRepeatedBindingFailsBeforeAnyOtherMethodChanges() {
        val originals = roots(original())
        val changed = originals.mapValues { MutableMethod(it.value) }
        changed.getValue("Lcom/waze/j;->c()Ljava/lang/String;").replaceInstruction(0, "nop")
        val before = changed.mapValues { wazeWarningFingerprint(it.value) }
        assertThrows(IllegalStateException::class.java) { bindWazeNativeWarnings(changed) }
        assertEquals(before, changed.mapValues { wazeWarningFingerprint(it.value) })
        val valid = originals.mapValues { MutableMethod(it.value) }
        bindWazeNativeWarnings(valid)
        val after = valid.mapValues { wazeWarningFingerprint(it.value) }
        assertThrows(IllegalStateException::class.java) { bindWazeNativeWarnings(valid) }
        assertEquals(after, valid.mapValues { wazeWarningFingerprint(it.value) })
    }

    @Test fun exactExistingZoneBarObserverIsAllowedWithoutAcceptingOtherEdits() {
        val originals = roots(original())
        val selected = originals.mapValues { MutableMethod(it.value) }
        selected.getValue(WAZE_SEGMENT_ROOT).addInstruction(0,
            "invoke-static/range {p1 .. p1}, Lorg/ungoogled/waze/ZoneBar;->insights(Ljava/lang/Object;)V")
        assertEquals(WAZE_SEGMENT_ZONE_BAR_HASH, wazeWarningFingerprint(selected.getValue(WAZE_SEGMENT_ROOT)))
        bindWazeNativeWarnings(selected)
        val altered = originals.mapValues { MutableMethod(it.value) }
        altered.getValue(WAZE_SEGMENT_ROOT).addInstruction(0,
            "invoke-static/range {p0 .. p0}, Lorg/ungoogled/waze/ZoneBar;->insights(Ljava/lang/Object;)V")
        val before = altered.mapValues { wazeWarningFingerprint(it.value) }
        assertThrows(IllegalStateException::class.java) { bindWazeNativeWarnings(altered) }
        assertEquals(before, altered.mapValues { wazeWarningFingerprint(it.value) })
    }
}
