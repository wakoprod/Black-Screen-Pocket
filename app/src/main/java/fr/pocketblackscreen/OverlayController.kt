package fr.pocketblackscreen

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Handler
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.roundToInt

internal class OverlayController(context: Context, private val handler: Handler) {
    private val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    private val defaultDisplay = checkNotNull(
        context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY),
    )
    private val windowContext = context.createWindowContext(defaultDisplay, type, null)
    private val windows = windowContext.getSystemService(WindowManager::class.java)
    private var view: ProtectionView? = null
    private val hideIndicator = Runnable { view?.showIndicator(false) }

    fun show(onTouchStart: () -> Boolean, onDoubleTap: () -> Unit) {
        check(view == null)
        val protection = ProtectionView(
            windowContext,
            onTouchStart = {
                if (onTouchStart()) {
                    flashIndicator()
                    true
                } else {
                    false
                }
            },
            onDoubleTap = onDoubleTap,
        )
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE,
        ).apply {
            alpha = 1f
            fitInsetsTypes = 0
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            title = windowContext.getString(R.string.app_name)
        }

        view = protection
        windows.addView(protection, params)
        flashIndicator()
    }

    fun hide() {
        handler.removeCallbacks(hideIndicator)
        val protection = view ?: return
        view = null
        protection.resetGesture()
        protection.visibility = View.GONE
        if (protection.parent != null) {
            try {
                windows.removeViewImmediate(protection)
            } catch (_: IllegalArgumentException) {
                // The system has already removed this window.
            } catch (failure: RuntimeException) {
                reportFailure("Retrait de la fenêtre", failure)
                try {
                    windows.removeView(protection)
                } catch (fallbackFailure: RuntimeException) {
                    reportFailure("Retrait différé de la fenêtre", fallbackFailure)
                }
            }
        }
    }

    private fun flashIndicator() {
        view?.showIndicator(true)
        handler.removeCallbacks(hideIndicator)
        handler.postDelayed(hideIndicator, 3_000)
    }
}

// Created programmatically with the callbacks for the current session.
@SuppressLint("ViewConstructor")
internal class ProtectionView(
    context: Context,
    private val onTouchStart: () -> Boolean,
    private val onDoubleTap: () -> Unit,
) : FrameLayout(context) {
    private val detector = ViewConfiguration.get(context).let {
        DoubleTapDetector(
            it.scaledTouchSlop.toFloat(),
            it.scaledDoubleTapSlop.toFloat(),
            ViewConfiguration.getLongPressTimeout().toLong(),
        )
    }
    private val indicator = ImageView(context).apply {
        setImageResource(R.drawable.ic_lock)
        setColorFilter(Color.rgb(62, 62, 62))
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        visibility = INVISIBLE
    }

    init {
        setBackgroundColor(Color.BLACK)
        contentDescription = context.getString(R.string.notification_stop)
        isClickable = true
        setPadding(0, dp(32), 0, 0)
        addView(indicator, LayoutParams(dp(18), dp(18), Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        setOnApplyWindowInsetsListener { _, insets ->
            val safe = insets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            )
            setPadding(safe.left, safe.top + dp(12), safe.right, safe.bottom)
            insets
        }
    }

    fun showIndicator(visible: Boolean) {
        indicator.visibility = if (visible) VISIBLE else INVISIBLE
    }

    fun resetGesture() = detector.cancel()

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        resetGesture()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        resetGesture()
        requestApplyInsets()
    }

    override fun performClick(): Boolean {
        super.performClick()
        onDoubleTap()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount != 1) {
            resetGesture()
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (onTouchStart()) detector.down(event.x, event.y, event.eventTime)
                else resetGesture()
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                for (index in 0 until event.historySize) {
                    detector.move(
                        event.getHistoricalX(index),
                        event.getHistoricalY(index),
                        event.getHistoricalEventTime(index),
                    )
                }
                if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                    detector.move(event.x, event.y, event.eventTime)
                } else if (detector.up(event.x, event.y, event.eventTime)) {
                    performClick()
                }
            }
            else -> resetGesture()
        }
        return true
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}
