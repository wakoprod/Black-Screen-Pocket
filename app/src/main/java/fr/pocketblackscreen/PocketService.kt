package fr.pocketblackscreen

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class PocketService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val session = SessionStateMachine()
    private val device by lazy { DeviceStateMonitor(this, ::reconcile) }
    private val notifications by lazy { PocketNotifications(this) }
    private val alarmManager by lazy { getSystemService(AlarmManager::class.java) }
    private val timeout = SessionTimeout(SystemClock::elapsedRealtime, ::schedule, ::unschedule)
    private var overlay: OverlayController? = null
    private var calls: CallStateMonitor? = null
    private var sessionId: String? = null
    private var stopSignal: PendingIntent? = null
    private var deadlineSignal: PendingIntent? = null
    private var inForeground = false

    private val timeoutCheck = object : Runnable {
        override fun run() {
            if (timeout.hasExpired()) {
                endSession()
            } else {
                timeout.deadlineMillis?.let {
                    handler.postDelayed(this, (it - SystemClock.elapsedRealtime()).coerceAtLeast(1))
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) {
            if (session.state == SessionState.INACTIVE) stopSelf(startId)
            return START_NOT_STICKY
        }
        if (!session.start()) return START_NOT_STICKY

        current = this
        updateTile()
        try {
            notifications.createChannel()
            val id = UUID.randomUUID().toString()
            sessionId = id
            val stop = PocketEventReceiver.signal(this, PocketEventReceiver.STOP, id)
            stopSignal = stop
            val foregroundType = if (Build.VERSION.SDK_INT >= 34) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
            }
            startForeground(
                PocketNotifications.NOTIFICATION_ID,
                notifications.build(stop, suspended = !device.canShowOverlay),
                foregroundType,
            )
            inForeground = true

            val permissions = PermissionStatus.read(this)
            if (!permissions.ready) {
                endSession()
                Toast.makeText(this, R.string.setup_required, Toast.LENGTH_LONG).show()
                return START_NOT_STICKY
            }
            device.start()
            if (permissions.hasTelephony) {
                val monitor = CallStateMonitor(this, ::endSession) { failure ->
                    reportFailure("Écoute téléphonique", failure)
                    endSession()
                }
                calls = monitor
                monitor.start()
            }
            timeout.start(TimeUnit.MINUTES.toMillis(SettingsRepository(this).durationMinutes.toLong()))
            reconcile()
        } catch (failure: RuntimeException) {
            failSession("Démarrage de la protection", failure)
        }
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        reconcile()
    }

    override fun onDestroy() {
        endSession(stopService = false)
        super.onDestroy()
    }

    private fun reconcile(screenOff: Boolean = false) {
        if (session.state == SessionState.INACTIVE || session.state == SessionState.STOPPING) return

        try {
            val canShowOverlay = !screenOff && device.canShowOverlay
            if (!canShowOverlay) overlay?.hide()
            if (timeout.hasExpired() || !PermissionStatus.read(this).ready || calls?.isRinging() == true) {
                endSession()
                return
            }
            if (!canShowOverlay) {
                if (session.suspendForLock()) publishState(suspended = true)
            } else if (session.state != SessionState.ACTIVE) {
                val controller = overlay ?: OverlayController(this, handler).also { overlay = it }
                controller.show(
                    onTouchStart = {
                        if (timeout.hasExpired()) endSession()
                        session.state == SessionState.ACTIVE
                    },
                    onDoubleTap = { endSession() },
                )
                if (session.showOverlay()) publishState(suspended = false)
            }
        } catch (failure: RuntimeException) {
            failSession("Mise à jour de la protection", failure)
        }
    }

    private fun publishState(suspended: Boolean) {
        stopSignal?.let { notifications.update(it, suspended) }
        updateTile()
    }

    private fun schedule(deadline: Long) {
        handler.postDelayed(timeoutCheck, (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0))
        val id = checkNotNull(sessionId)
        val signal = PocketEventReceiver.signal(this, PocketEventReceiver.DEADLINE, id)
        deadlineSignal = signal
        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, deadline, signal)
        } catch (failure: RuntimeException) {
            reportFailure("Planification du délai de secours", failure)
        }
    }

    private fun unschedule() {
        handler.removeCallbacks(timeoutCheck)
        val signal = deadlineSignal ?: return
        deadlineSignal = null
        try {
            alarmManager.cancel(signal)
        } finally {
            signal.cancel()
        }
    }

    private fun endSession() = endSession(stopService = true)

    private fun endSession(stopService: Boolean) {
        if (!session.stop()) return
        try {
            cleanUp("Retrait de la surcouche") { overlay?.hide() }
            overlay = null
            sessionId = null
            cleanUp("Annulation du délai") { timeout.cancel() }
            cleanUp("Arrêt de l’écoute de l’écran") { device.stop() }
            cleanUp("Arrêt de l’écoute téléphonique") { calls?.stop() }
            calls = null
            cleanUp("Annulation de l’action de secours") { stopSignal?.cancel() }
            stopSignal = null
            if (inForeground) {
                cleanUp("Retrait de la notification") { stopForeground(STOP_FOREGROUND_REMOVE) }
                inForeground = false
            }
        } finally {
            session.finish()
            if (current === this) current = null
            updateTile()
            if (stopService) stopSelf()
        }
    }

    private fun failSession(operation: String, failure: RuntimeException) {
        reportFailure(operation, failure)
        endSession()
        Toast.makeText(this, R.string.startup_failed, Toast.LENGTH_LONG).show()
    }

    private fun updateTile() = cleanUp("Actualisation de la tuile") {
        PocketTileService.refresh()
    }

    companion object {
        private const val ACTION_START = "fr.pocketblackscreen.START"
        private var current: PocketService? = null

        val state: SessionState get() = current?.session?.state ?: SessionState.INACTIVE

        fun start(context: Context) {
            context.startForegroundService(Intent(context, PocketService::class.java).setAction(ACTION_START))
        }

        fun stopIfRunning(): Boolean {
            val instance = current ?: return false
            instance.endSession()
            return true
        }

        fun receiveSignal(action: String?, id: String?) {
            val instance = current ?: return
            if (id == null || id != instance.sessionId) return
            when (action) {
                PocketEventReceiver.STOP -> instance.endSession()
                PocketEventReceiver.DEADLINE -> {
                    if (instance.timeout.hasExpired()) instance.endSession()
                }
            }
        }
    }
}

internal class PocketEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PocketService.receiveSignal(intent.action, intent.data?.lastPathSegment)
    }

    companion object {
        const val STOP = "fr.pocketblackscreen.STOP"
        const val DEADLINE = "fr.pocketblackscreen.DEADLINE"

        fun signal(context: Context, action: String, id: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, PocketEventReceiver::class.java).setAction(action).setData(
                    Uri.Builder().scheme("pocketblackscreen").authority("session").appendPath(id).build(),
                ),
                PendingIntent.FLAG_IMMUTABLE,
            )
    }
}

private inline fun cleanUp(operation: String, action: () -> Unit) {
    try {
        action()
    } catch (failure: RuntimeException) {
        reportFailure(operation, failure)
    }
}

internal fun reportFailure(operation: String, failure: Throwable) {
    Log.w("PocketBlackScreen", "$operation (${failure.javaClass.simpleName})")
}
