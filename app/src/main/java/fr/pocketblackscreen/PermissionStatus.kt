package fr.pocketblackscreen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

internal data class PermissionStatus(
    val overlay: Boolean,
    val notifications: Boolean,
    val hasTelephony: Boolean,
    val phone: Boolean,
) {
    val ready: Boolean get() = overlay && notifications && phone

    companion object {
        fun read(context: Context): PermissionStatus {
            val hasTelephony = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
            return PermissionStatus(
                overlay = Settings.canDrawOverlays(context),
                notifications = PocketNotifications(context).areAvailable(),
                hasTelephony = hasTelephony,
                phone = !hasTelephony ||
                    context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) ==
                    PackageManager.PERMISSION_GRANTED,
            )
        }
    }
}
