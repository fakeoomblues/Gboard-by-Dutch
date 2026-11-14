package patches.messages

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction20t
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val GMS_CLIENT = "Lcom/google/android/gms/common/internal/BaseGmsClient;"
private const val GET_SERVICE =
    "Lcom/google/android/gms/common/internal/IGmsServiceBroker;->getService(" +
        "Lcom/google/android/gms/common/internal/IGmsCallbacks;Lcom/google/android/gms/common/internal/GetServiceRequest;)V"

/**
 * Google Play services refuses several of its APIs to apps that Google did not sign, answering the
 * connection request with a SecurityException ("PackageVerificationRslt: not allowed"). Google's
 * client library re-throws exactly that exception on its own worker thread, so a re-signed
 * Messages crashed as soon as it started.
 *
 * The client already treats every other failed connection request (a RemoteException or any
 * other RuntimeException) by logging it and reporting a failed connection, which Messages
 * handles. The SecurityException handler now jumps into that same path instead of re-throwing.
 * Google-signed apps never reach it, and nothing else in the connection logic changes.
 */
internal val playServicesRefusalPatch = bytecodePatch {
    dependsOn(originalInput)

    execute {
        val client = mutableClassDefBy(GMS_CLIENT)
        val connect = client.methods.singleOrNull { method ->
            method.implementation?.instructions?.any {
                (it as? ReferenceInstruction)?.reference?.toString() == GET_SERVICE
            } == true
        } ?: throw PatchException("Play services client connection request is missing")
        val implementation = connect.implementation!!
        val instructions = implementation.instructions
        fun indexAt(address: Int): Int {
            var current = 0
            instructions.forEachIndexed { index, instruction ->
                if (current == address) return index
                current += instruction.codeUnits
            }
            throw PatchException("Play services client handler address $address is not an instruction")
        }

        val handlers = implementation.tryBlocks.flatMap { it.exceptionHandlers }
        fun handler(type: String) = handlers.filter { it.exceptionType == type }.map { it.handlerCodeAddress }.distinct()
            .singleOrNull() ?: throw PatchException("Play services client has no single $type handler")
        val refused = indexAt(handler("Ljava/lang/SecurityException;"))
        val failed = indexAt(handler("Landroid/os/RemoteException;"))

        if (instructions[refused].opcode != Opcode.MOVE_EXCEPTION || instructions[refused + 1].opcode != Opcode.THROW) {
            throw PatchException("Play services client no longer re-throws refused connections, or patch already applied")
        }
        if (instructions[failed].opcode != Opcode.MOVE_EXCEPTION ||
            (instructions[failed] as OneRegisterInstruction).registerA != (instructions[refused] as OneRegisterInstruction).registerA
        ) throw PatchException("Play services client failure path changed shape")

        // Continue right after the failure handler's move-exception: the same register already holds the exception.
        val label = implementation.newLabelForIndex(failed + 1)
        connect.replaceInstruction(refused + 1, BuilderInstruction20t(Opcode.GOTO_16, label))
    }
}
