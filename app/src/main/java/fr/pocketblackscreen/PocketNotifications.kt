package fr.pocketblackscreen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon

internal class PocketNotifications(private val context: Context) {
    private val notifications = context.getSystemService(NotificationManager::class.java)

    fun createChannel() {
        notifications.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.channel_description) },
        )
    }

    fun areAvailable(): Boolean =
        notifications.areNotificationsEnabled() &&
            notifications.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE

    fun build(stop: PendingIntent, suspended: Boolean): Notification {
        val text = if (suspended) R.string.notification_suspended else R.string.notification_active
        return Notification.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(text))
            .setSmallIcon(R.drawable.ic_lock)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_lock),
                    context.getString(R.string.notification_stop),
                    stop,
                ).build(),
            )
            .build()
    }

    fun update(stop: PendingIntent, suspended: Boolean) {
        notifications.notify(NOTIFICATION_ID, build(stop, suspended))
    }

    companion object {
        const val CHANNEL_ID = "protection"
        const val NOTIFICATION_ID = 1
    }
}
