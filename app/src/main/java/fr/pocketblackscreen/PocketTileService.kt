package fr.pocketblackscreen

import android.annotation.SuppressLint
import android.app.Activity
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import java.lang.ref.WeakReference

internal class PocketTileService : TileService() {
    override fun onTileAdded() = render()

    override fun onStartListening() {
        listeningTile = WeakReference(this)
        render()
    }

    override fun onStopListening() = clearListeningTile()

    override fun onDestroy() {
        clearListeningTile()
        super.onDestroy()
    }

    private fun clearListeningTile() {
        if (listeningTile?.get() === this) listeningTile = null
    }

    override fun onClick() {
        if (PocketService.stopIfRunning()) {
            render()
            return
        }
        if (isLocked) unlockAndRun(::launchFromTile)
        else launchFromTile()
    }

    // Android 12 and 13 only provide the Intent overload.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Suppress("DEPRECATION")
    private fun launchFromTile() {
        val target = if (PermissionStatus.read(this).ready) {
            Intent(this, ActivationActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java).putExtra(MainActivity.FROM_TILE, true)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(
                    PendingIntent.getActivity(
                        this, 0, target, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT,
                    ),
                )
            } else {
                startActivityAndCollapse(target)
            }
        } catch (failure: RuntimeException) {
            reportFailure("Ouverture depuis la tuile", failure)
            Toast.makeText(this, R.string.startup_failed, Toast.LENGTH_SHORT).show()
        }
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val state = PocketService.state
        val label = when (state) {
            SessionState.STARTING -> R.string.tile_starting
            SessionState.ACTIVE -> R.string.tile_active
            SessionState.SUSPENDED_FOR_LOCK -> R.string.tile_suspended
            SessionState.INACTIVE, SessionState.STOPPING -> {
                if (PermissionStatus.read(this).ready) R.string.tile_inactive else R.string.tile_setup
            }
        }
        tile.state = when (state) {
            SessionState.ACTIVE, SessionState.SUSPENDED_FOR_LOCK -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = getString(R.string.app_name)
        tile.subtitle = getString(label)
        tile.stateDescription = getString(label)
        tile.updateTile()
    }

    companion object {
        private var listeningTile: WeakReference<PocketTileService>? = null

        fun refresh() {
            listeningTile?.get()?.render()
        }
    }
}

internal class ActivationActivity : Activity() {
    private var submitted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        submitted = savedInstanceState?.getBoolean(SUBMITTED) == true
        if (submitted) finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(SUBMITTED, submitted)
        super.onSaveInstanceState(outState)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || submitted || isFinishing) return

        submitted = true
        try {
            if (getSystemService(KeyguardManager::class.java).isKeyguardLocked) return
            if (!PermissionStatus.read(this).ready) {
                startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.FROM_TILE, true))
            } else {
                PocketService.start(this)
            }
        } catch (failure: RuntimeException) {
            reportFailure("Lancement du service", failure)
            Toast.makeText(this, R.string.startup_failed, Toast.LENGTH_LONG).show()
        } finally {
            finish()
        }
    }

    private companion object {
        const val SUBMITTED = "activation_submitted"
    }
}
