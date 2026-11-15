package nl.johninnl.reolink.calls

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import java.security.MessageDigest

internal data class DoorbellEvent(
    val key: String,
    val address: String,
    val displayName: String,
    val device: Any,
    val channel: Any,
    val context: Context,
    val remoteMessage: Any,
    val notificationId: Int,
)

/** Process-local handoff; no app credentials or push payloads are persisted or exported. */
object DoorbellCalls {
    private var pending: DoorbellEvent? = null
    private val seen = LinkedHashMap<String, Long>()

    @JvmStatic
    fun onMessage(context: Context, remoteMessage: Any): Boolean {
        try {
            val extras = (NativeApi.call(remoteMessage, "toIntent") as Intent).extras ?: return false
            val push = NativeApi.call(NativeApi.singleton("com.reolink.push.PushMessageFactory"),
                "createPushMessage", extras) ?: return false
            if (NativeApi.call(push, "getDoorPhone") != true || NativeApi.call(push, "getDup") == true) return false
            // Match the native onMessage account policy before taking over presentation.
            if (NativeApi.call(push, "isCloudAccountPush") == true) {
                val account = NativeApi.instance("com.android.bc.account.AccountManager")
                if (NativeApi.call(account, "isLogin") != true ||
                    NativeApi.call(push, "getReceiverUserId") != NativeApi.call(account, "getID")) return false
            }
            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                return false // Keep the native permission/notification route until the owner grants access.
            val uid = NativeApi.call(push, "getUid") as? String ?: return false
            val channelIds = NativeApi.call(push, "getChannel") as? String ?: return false
            val messageId = NativeApi.call(push, "getMessageId")?.toString()
                ?: NativeApi.call(remoteMessage, "getMessageId") as? String ?: return false
            val device = NativeApi.call(NativeApi.instance("com.android.bc.devicemanager.DeviceManager"),
                "getDeviceByUID", uid) ?: return false
            val channel = (NativeApi.call(device, "getPushChannels", channelIds) as? List<*>)?.firstOrNull()
                ?: return false
            fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            val digest = hash("$uid/$channelIds/$messageId")
            val address = hash("$uid/$channelIds") // Stable doorbell identity; the event ID remains per ring.
            val name = (NativeApi.call(push, "getDeviceName") as? String)?.takeIf { it.isNotBlank() }
                ?: (NativeApi.call(push, "getTitle") as? String)?.takeIf { it.isNotBlank() } ?: "Reolink doorbell"
            val event = DoorbellEvent(digest, address, name, device, channel, context, remoteMessage, remoteMessage.hashCode())
            synchronized(this) {
                val now = SystemClock.elapsedRealtime()
                seen.entries.removeAll { now - it.value > 300_000 }
                if (digest in seen) return true
                if (pending != null) return false // Native SDK owns one talk session; don't steal another call.
                pending = event
                seen[digest] = now
                while (seen.size > 64) seen.remove(seen.keys.first())
                try {
                    context.startForegroundService(Intent(context, ReolinkCallService::class.java)
                        .putExtra(ReolinkCallService.KEY, digest))
                } catch (error: RuntimeException) {
                    pending = null
                    seen.remove(digest)
                    throw error
                }
            }
            return true
        } catch (error: Throwable) {
            Log.w("ReolinkCalls", "Native call handoff unavailable: ${error.javaClass.simpleName}")
            return false
        }
    }

    @Synchronized internal fun event(key: String?): DoorbellEvent? = pending?.takeIf { it.key == key }
    @Synchronized internal fun finished(key: String) { if (pending?.key == key) pending = null }

    internal fun fallback(event: DoorbellEvent) {
        try { NativeApi.call(event.context, "showCallNotification", event.remoteMessage) }
        catch (error: Throwable) { Log.w("ReolinkCalls", "Native notification fallback failed: ${error.javaClass.simpleName}") }
    }
}
