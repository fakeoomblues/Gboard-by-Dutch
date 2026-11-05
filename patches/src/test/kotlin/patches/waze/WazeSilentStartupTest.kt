package patches.waze

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test
import java.io.File

class WazeSilentStartupTest {
 @Test fun providerOwnershipObserverRunsAfterTheRealCarSessionHasInitialized() {
  val path=System.getenv("WAZE_ORIGINAL_APK");assumeNotNull(path)
  val dex=DexFileFactory.loadDexContainer(File(path!!),Opcodes.getDefault())
  val original=dex.dexEntryNames.flatMap { dex.getEntry(it)!!.dexFile.classes }
   .single { it.type=="Landroidx/car/app/Session;" }.methods.single { it.name=="<init>" && it.parameterTypes.isEmpty() }
  val method=MutableMethod(original);val before=method.implementation!!.instructions.toList()
  patches.shared.observeNavigationProviderSession(method)
  val after=method.implementation!!.instructions.toList()
  assertEquals(before.size+1,after.size);assertEquals(before.dropLast(1),after.dropLast(2))
  val hook=after[after.size-2]
  assertEquals(Opcode.INVOKE_STATIC_RANGE,hook.opcode)
  assertEquals("Lnl/johninnl/navigation/startup/ProviderLifecycle;->observeCarSession(Ljava/lang/Object;)V",(hook as ReferenceInstruction).reference.toString())
  assertEquals(method.implementation!!.registerCount-1,(hook as RegisterRangeInstruction).startRegister)
  assertEquals(1,hook.registerCount);assertEquals(Opcode.RETURN_VOID,after.last().opcode)
  assertThrows(IllegalStateException::class.java) { patches.shared.observeNavigationProviderSession(method) }
 }

 private fun originalCarKeepAlive(): MutableMethod {
  val path=System.getenv("WAZE_ORIGINAL_APK");assumeNotNull(path)
  val dex=DexFileFactory.loadDexContainer(File(path!!),Opcodes.getDefault())
  val original=dex.dexEntryNames.flatMap { dex.getEntry(it)!!.dexFile.classes }
   .single { it.type=="Lcom/waze/car_lib/platform/af;" }.methods.single { it.name=="b" && it.parameterTypes.isEmpty() }
  return MutableMethod(original)
 }

 @Test fun carKeepAlivePreservesTheNativeDecisionBeforeApplyingTheBroadcastSwitch() {
  val method=originalCarKeepAlive();val before=method.implementation!!.instructions.toList()
  val register=(before.last() as OneRegisterInstruction).registerA
  preserveBroadcastsAfterCarSession(method)
  val after=method.implementation!!.instructions.toList()
  assertEquals(before.size+2,after.size)
  assertEquals(before.dropLast(1),after.dropLast(3))
  val hook=after[after.size-3]
  assertEquals(Opcode.INVOKE_STATIC_RANGE,hook.opcode)
  assertEquals("Lorg/ungoogled/waze/NavigationBroadcasts;->keepAliveAfterCarSession(Z)Z",(hook as ReferenceInstruction).reference.toString())
  assertEquals(register,(hook as RegisterRangeInstruction).startRegister)
  assertEquals(1,hook.registerCount)
  assertEquals(Opcode.MOVE_RESULT,after[after.size-2].opcode)
  assertEquals(register,(after[after.size-2] as OneRegisterInstruction).registerA)
  assertEquals(Opcode.RETURN,after.last().opcode)
  assertEquals(register,(after.last() as OneRegisterInstruction).registerA)
 }

 @Test fun changedCarKeepAliveReturnIsRejectedBeforeEditing() {
  val method=originalCarKeepAlive()
  method.replaceInstruction(method.implementation!!.instructions.size-1,"return-void")
  val before=method.implementation!!.instructions.toList()
  assertThrows(IllegalStateException::class.java) { preserveBroadcastsAfterCarSession(method) }
  assertEquals(before,method.implementation!!.instructions.toList())
 }

 @Test fun coldApplicationNeverForcesAnActivityRelaunchFromStaleTaskHistory() {
  val path=System.getenv("WAZE_STARTUP_APK");assumeNotNull(path)
  val dex=DexFileFactory.loadDexContainer(File(path!!),Opcodes.getDefault())
  val original=dex.dexEntryNames.flatMap { dex.getEntry(it)!!.dexFile.classes }.single { it.type=="Lcom/waze/ls;" }.methods.single { it.name=="onCreate" }
  val method=MutableMethod(original)
  val before=method.implementation!!.instructions.toList()
  val branch=before[49] as BuilderOffsetInstruction
  val target=branch.target
  val helper=Class.forName("patches.waze.WazeNavigationBroadcastPatchKt").getDeclaredMethod("suppressTaskRelaunch",MutableMethod::class.java)
  helper.isAccessible=true;helper.invoke(null,method)
  val after=method.implementation!!.instructions.toList()
  assertEquals(before.size,after.size);assertEquals(Opcode.GOTO_32,after[49].opcode)
  assertSame(target,(after[49] as BuilderOffsetInstruction).target)
 }
}
