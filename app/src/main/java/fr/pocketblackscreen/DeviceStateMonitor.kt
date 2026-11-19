package fr.pocketblackscreen

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager

internal class DeviceStateMonitor(
    private val context: Context,
    private val onTransition: (screenOff: Boolean) -> Unit,
) {
    private val power = context.getSystemService(PowerManager::class.java)
    private val keyguard = context.getSystemService(KeyguardManager::class.java)
    private var registered = false

    val canShowOverlay: Boolean
        get() = power.isInteractive && !keyguard.isKeyguardLocked && !keyguard.isDeviceLocked

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> onTransition(true)
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> onTransition(false)
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        // Only protected broadcasts. USER_PRESENT can come from the privileged System UI process.
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        registered = true
    }

    fun stop() {
        if (!registered) return
        registered = false
        context.unregisterReceiver(receiver)
    }
}
