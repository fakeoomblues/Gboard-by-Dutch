package patches.waze

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test
import java.io.File

class WazeSilentStartupTest {
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
