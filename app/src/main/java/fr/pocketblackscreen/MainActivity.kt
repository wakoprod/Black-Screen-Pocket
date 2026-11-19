package fr.pocketblackscreen

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private lateinit var overlayRow: PermissionRow
    private lateinit var phoneRow: PermissionRow
    private lateinit var notificationRow: PermissionRow
    private lateinit var setupHelp: TextView
    private val requestHistory by lazy { getSharedPreferences("permission_requests", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PocketNotifications(this).createChannel()
        window.setDecorFitsSystemWindows(false)

        val scroll = ScrollView(this).apply {
            id = android.R.id.list
            isFillViewport = true
            clipToPadding = false
            isVerticalScrollBarEnabled = false
            setOnApplyWindowInsetsListener { view, insets ->
                val safe = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
                insets
            }
        }
        val page = column().apply { setPadding(dp(20), dp(24), dp(20), dp(20)) }

        val header = column().apply {
            val logo = ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.ic_lock)
                setColorFilter(getColor(R.color.accent))
                setPadding(dp(10), dp(10), dp(10), dp(10))
                background = backgroundShape(R.color.surface)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            addView(logo, LinearLayout.LayoutParams(dp(44), dp(44)))
            addSpaced(text(R.string.app_name, 30f, bold = true), 12)
            addSpaced(text(R.string.tagline, 15f, secondary = true), 4)
        }
        page.addSpaced(header, 0)

        val permissions = card(R.string.permissions_title)
        setupHelp = text(R.string.setup_required, 14f, secondary = true)
        permissions.addSpaced(setupHelp, 8)
        overlayRow = permissions.addPermissionRow(R.string.overlay_title, R.string.overlay_explanation)
        phoneRow = permissions.addPermissionRow(R.string.phone_title, R.string.phone_explanation)
        notificationRow =
            permissions.addPermissionRow(R.string.notifications_title, R.string.notifications_explanation)
        page.addSpaced(permissions, 20)

        val repository = SettingsRepository(this)
        val duration = card(R.string.duration_title)
        duration.addSpaced(text(R.string.duration_explanation, 14f, secondary = true), 6)
        val durations = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            val accent = ColorStateList.valueOf(getColor(R.color.accent))
            SettingsRepository.durations.forEach { minutes ->
                val item = RadioButton(this@MainActivity).apply {
                    id = View.generateViewId()
                    tag = minutes
                    text = getString(R.string.duration_value, minutes)
                    textSize = 15f
                    buttonTintList = accent
                    minHeight = dp(48)
                }
                addView(item)
                if (minutes == repository.durationMinutes) check(item.id)
            }
            setOnCheckedChangeListener { group, checkedId ->
                group.findViewById<RadioButton>(checkedId)?.let {
                    repository.durationMinutes = it.tag as Int
                }
            }
        }
        duration.addSpaced(durations, 4)
        page.addSpaced(duration, 12)
        page.addSpaced(informationCard(R.string.usage_title, R.string.usage_body), 12)
        page.addSpaced(informationCard(R.string.safety_title, R.string.safety_body), 12)
        page.addSpaced(informationCard(R.string.compat_title, R.string.compat_body), 12)
        page.addSpaced(
            text(R.string.version, 13f, secondary = true).apply {
                text = getString(R.string.version, BuildConfig.VERSION_NAME)
                gravity = Gravity.CENTER
            },
            16,
        )

        scroll.addView(page)
        setContentView(scroll)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updatePermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        val status = PermissionStatus.read(this)
        setupHelp.visibility = if (!status.ready && intent.getBooleanExtra(FROM_TILE, false)) {
            View.VISIBLE
        } else {
            View.GONE
        }

        overlayRow.render(status.overlay, R.string.grant_overlay) {
            openSystemPage(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri()))
        }

        val phoneSettings = mustUseSettings(Manifest.permission.READ_PHONE_STATE)
        phoneRow.render(
            status.phone,
            if (phoneSettings) R.string.allow_phone_in_settings else R.string.grant_phone,
            statusLabel = if (!status.hasTelephony) R.string.status_unneeded else null,
        ) {
            if (phoneSettings) openSystemPage(appDetailsIntent())
            else askFor(Manifest.permission.READ_PHONE_STATE, PHONE_REQUEST)
        }

        val canRequestNotifications = Build.VERSION.SDK_INT >= 33 &&
            !mustUseSettings(Manifest.permission.POST_NOTIFICATIONS) &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        notificationRow.render(
            status.notifications,
            if (canRequestNotifications) R.string.grant_notifications else R.string.open_notification_settings,
        ) {
            if (Build.VERSION.SDK_INT >= 33 && canRequestNotifications) {
                askFor(Manifest.permission.POST_NOTIFICATIONS, NOTIFICATION_REQUEST)
            } else {
                openSystemPage(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
                )
            }
        }
        PocketTileService.refresh()
    }

    private fun mustUseSettings(permission: String): Boolean =
        requestHistory.getBoolean(permission, false) && !shouldShowRequestPermissionRationale(permission)

    private fun askFor(permission: String, code: Int) {
        requestHistory.edit().putBoolean(permission, true).apply()
        requestPermissions(arrayOf(permission), code)
    }

    private fun packageUri(): Uri = Uri.fromParts("package", packageName, null)

    private fun appDetailsIntent() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri())

    private fun openSystemPage(intent: Intent) {
        try {
            startActivity(intent)
        } catch (failure: ActivityNotFoundException) {
            reportFailure("Ouverture des paramètres Android", failure)
            try {
                startActivity(appDetailsIntent())
            } catch (fallbackFailure: RuntimeException) {
                reportFailure("Ouverture des informations de l’application", fallbackFailure)
                showSettingsMessage()
            }
        } catch (failure: SecurityException) {
            reportFailure("Ouverture des paramètres Android", failure)
            showSettingsMessage()
        }
    }

    private fun showSettingsMessage() {
        Toast.makeText(this, R.string.system_settings_unavailable, Toast.LENGTH_LONG).show()
    }

    private fun informationCard(title: Int, body: Int) = card(title).apply {
        addSpaced(text(body, 15f, secondary = true), 8)
    }

    private fun card(title: Int) = column().apply {
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = backgroundShape(R.color.surface)
        addSpaced(text(title, 19f, bold = true).apply { isAccessibilityHeading = true }, 0)
    }

    private fun LinearLayout.addPermissionRow(title: Int, explanation: Int): PermissionRow {
        val status = TextView(this@MainActivity).apply { textSize = 13f }
        val action = Button(this@MainActivity).apply {
            isAllCaps = false
            minHeight = dp(48)
        }
        addSpaced(
            column().apply {
                addSpaced(text(title, 16f, bold = true), 0)
                addSpaced(status, 3)
                addSpaced(text(explanation, 14f, secondary = true), 5)
                addSpaced(action, 5)
            },
            13,
        )
        return PermissionRow(status, action)
    }

    private inner class PermissionRow(private val status: TextView, private val action: Button) {
        fun render(granted: Boolean, actionText: Int, statusLabel: Int? = null, onAction: () -> Unit) {
            status.setText(statusLabel ?: if (granted) R.string.status_granted else R.string.status_missing)
            status.setTextColor(getColor(if (granted) R.color.accent else R.color.on_surface_variant))
            action.visibility = if (granted) View.GONE else View.VISIBLE
            action.setText(actionText)
            action.setOnClickListener { onAction() }
        }
    }

    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

    private fun LinearLayout.addSpaced(view: View, margin: Int) {
        addView(view, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(margin) })
    }

    private fun text(resource: Int, size: Float, bold: Boolean = false, secondary: Boolean = false) =
        TextView(this).apply {
            setText(resource)
            textSize = size
            setTextColor(getColor(if (secondary) R.color.on_surface_variant else R.color.on_surface))
            setLineSpacing(dp(3).toFloat(), 1f)
            if (bold) setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        }

    private fun backgroundShape(color: Int) = GradientDrawable().apply {
        setColor(getColor(color))
        setStroke(dp(1), getColor(R.color.outline))
        cornerRadius = dp(16).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    internal companion object {
        const val FROM_TILE = "from_tile"
        private const val PHONE_REQUEST = 1
        private const val NOTIFICATION_REQUEST = 2
    }
}
