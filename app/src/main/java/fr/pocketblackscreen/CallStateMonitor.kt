package fr.pocketblackscreen

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager

@SuppressLint("MissingPermission")
internal class CallStateMonitor(
    private val context: Context,
    private val onRinging: () -> Unit,
    private val onFailure: (RuntimeException) -> Unit,
) {
    private val telephony = context.getSystemService(TelephonyManager::class.java)
    private val subscriptions = context.getSystemService(SubscriptionManager::class.java)
    private val registered = mutableMapOf<Int, Pair<TelephonyManager, CallListener>>()
    private var listeningForSubscriptions = false

    private inner class CallListener : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            if (state == TelephonyManager.CALL_STATE_RINGING && registered.values.any { it.second === this }) {
                onRinging()
            }
        }
    }

    private val subscriptionListener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
        override fun onSubscriptionsChanged() {
            if (!listeningForSubscriptions) return
            try {
                refresh()
            } catch (failure: RuntimeException) {
                onFailure(failure)
            }
        }
    }

    fun start() {
        refresh()
        subscriptions.addOnSubscriptionsChangedListener(context.mainExecutor, subscriptionListener)
        listeningForSubscriptions = true
    }

    @Suppress("DEPRECATION")
    fun isRinging(): Boolean =
        telephony.callState == TelephonyManager.CALL_STATE_RINGING ||
            registered.values.any { (manager, _) ->
                manager.callStateForSubscription == TelephonyManager.CALL_STATE_RINGING
            }

    fun stop() {
        if (listeningForSubscriptions) {
            listeningForSubscriptions = false
            runCatching { subscriptions.removeOnSubscriptionsChangedListener(subscriptionListener) }
                .onFailure { reportFailure("Arrêt de l’écoute des abonnements", it) }
        }
        registered.keys.toList().forEach(::unregister)
    }

    @Suppress("DEPRECATION")
    private fun refresh() {
        val ids = (0 until telephony.activeModemCount)
            .flatMap { slot ->
                // The slot-based API renamed in Android 14 returns IDs without subscriber details.
                if (Build.VERSION.SDK_INT >= 34) {
                    listOf(SubscriptionManager.getSubscriptionId(slot))
                } else {
                    subscriptions.getSubscriptionIds(slot)?.toList().orEmpty()
                }
            }
            .filter(SubscriptionManager::isValidSubscriptionId)
            .toSet()
            .ifEmpty { setOf(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) }

        (registered.keys - ids).forEach(::unregister)
        for (id in ids - registered.keys) {
            val manager = if (id == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) {
                telephony
            } else {
                telephony.createForSubscriptionId(id)
            }
            val listener = CallListener()
            manager.registerTelephonyCallback(context.mainExecutor, listener)
            registered[id] = manager to listener
        }
    }

    private fun unregister(id: Int) {
        val (manager, listener) = registered.remove(id) ?: return
        runCatching { manager.unregisterTelephonyCallback(listener) }
            .onFailure { reportFailure("Arrêt de l’écoute téléphonique", it) }
    }
}
