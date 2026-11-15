package patches.nexuslauncher

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val BRIDGE = "Lapp/morphe/extension/nexuslauncher/SearchUiBridge;"
private const val MANAGER = "Landroid/app/search/SearchUiManager;"

/**
 * SearchUiManager.createSearchSession builds a SearchSession, whose constructor calls the
 * search_ui service. That service requires MANAGE_SEARCH_UI, a signature|role permission held
 * only by shell, so a sideloaded launcher gets "Permission Denial: createSearchSession" and
 * every in-drawer search is dead. The call site is redirected to SearchUiBridge, which replays
 * the framework constructor with the binder wrapped by Shevery/Shizuku, so the service call
 * runs with shell's identity.
 */
@Suppress("unused")
val searchSessionRedirect = bytecodePatch(
    name = "Experimental: create search sessions through the privileged bridge",
    description = "Redirects SearchUiManager.createSearchSession to SearchUiBridge, which " +
        "rebuilds the session around a Shevery/Shizuku-wrapped search_ui binder so the call " +
        "runs as shell uid 2000 (the only principal holding MANAGE_SEARCH_UI). Without a " +
        "running bridge the call degrades to no search instead of a SecurityException." +
        " Requires the pixel-launcher-dhizuku addon (gitlab.com/johninnld/pixel-launcher-dhizuku) to be installed.",
    default = true,
) {
    compatibleWith(pixelLauncher)
    dependsOn(originalInput, searchUiProvider)
    extendWith("extensions/nexuslauncher.mpe")

    execute {
        // The all-apps session holder creates the session and immediately registers a callback
        // on it. When the redirect returns null (the service refuses even shell uid), that call
        // NPEs on a worker thread. Skip the session setup entirely: the drawer and its local
        // filtering still work, only the privileged SearchSession is absent.
        var skippedSessionHolder = 0
        classDefForEach { classDef ->
            if (classDef.type != "Lcom/google/android/apps/nexuslauncher/allapps/m4;") {
                return@classDefForEach
            }
            val mutable = mutableClassDefBy(classDef)
            for (method in mutable.methods) {
                if (method.name == "a" && method.parameterTypes.isEmpty() && method.returnType == "V") {
                    method.addInstructions(0, "return-void")
                    skippedSessionHolder++
                }
            }
        }
        // The holder is an obfuscated, build-specific class: it exists in the 17/907 release the
        // patches target, but not in every launcher build (an OEM or CANARY build renames it).
        // Missing it only means the null-session guard is not installed, so warn and continue
        // instead of aborting the whole patch run for an unrelated build.
        if (skippedSessionHolder != 1) {
            System.err.println(
                "nexuslauncher: all-apps search session holder not found " +
                    "(found $skippedSessionHolder); skipping the null-session guard",
            )
        }

        var rewritten = 0
        classDefForEach { classDef ->
            val mutable = mutableClassDefBy(classDef)
            for (method in mutable.methods) {
                val body = method.implementation ?: continue
                var index = 0
                while (index < body.instructions.size) {
                    val instruction = method.getInstruction(index)
                    if (instruction.opcode != Opcode.INVOKE_VIRTUAL) {
                        index++
                        continue
                    }
                    val reference = (instruction as ReferenceInstruction).reference as? MethodReference
                    if (reference == null ||
                        reference.definingClass != MANAGER ||
                        reference.name != "createSearchSession"
                    ) {
                        index++
                        continue
                    }
                    // invoke-virtual {manager, context}, LSearchUiManager;->createSearchSession(SearchContext)SearchSession
                    val registers = (instruction as com.android.tools.smali.dexlib2.iface
                        .instruction.FiveRegisterInstruction)
                    val managerRegister = registers.registerC
                    val contextRegister = registers.registerD
                    method.replaceInstruction(
                        index,
                        "invoke-static {v$managerRegister, v$contextRegister}, " +
                            "$BRIDGE->createSession(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                    )
                    val next = method.getInstruction(index + 1)
                    if (next.opcode == Opcode.MOVE_RESULT_OBJECT) {
                        val target = (next as OneRegisterInstruction).registerA
                        method.addInstructions(
                            index + 2,
                            "check-cast v$target, Landroid/app/search/SearchSession;",
                        )
                    }
                    rewritten++
                    index += 1
                }
            }
        }
        check(rewritten > 0) { "No SearchUiManager.createSearchSession call site found" }
        System.err.println("nexuslauncher: redirected $rewritten search-session call sites")
    }
}
