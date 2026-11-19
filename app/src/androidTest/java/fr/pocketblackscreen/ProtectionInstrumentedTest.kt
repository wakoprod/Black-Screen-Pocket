package fr.pocketblackscreen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProtectionInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun launcherOnlyOpensSettings() {
        assertEquals(SessionState.INACTIVE, PocketService.state)
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        try {
            assertTrue(activity is MainActivity)
            assertEquals(SessionState.INACTIVE, PocketService.state)
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }

    @Test fun protectionViewDrawsOpaqueBlackWhenTheIndicatorIsHidden() = onMain {
        val view = ProtectionView(instrumentation.targetContext, { true }, {})
        view.showIndicator(false)
        val size = 256
        val measure = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
        view.measure(measure, measure)
        view.layout(0, 0, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.MAGENTA)
            view.draw(Canvas(bitmap))
            val pixels = IntArray(size * size)
            bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
            assertTrue(pixels.all { it == Color.BLACK })
        } finally {
            bitmap.recycle()
        }
    }

    @Test fun androidTouchDispatchConsumesEventsAndOnlyDismissesOnTheSecondRelease() = onMain {
        var dismissals = 0
        val view = ProtectionView(instrumentation.targetContext, { true }) { dismissals++ }
        val t = SystemClock.uptimeMillis()
        touch(view, t, t, MotionEvent.ACTION_DOWN)
        touch(view, t, t + 20, MotionEvent.ACTION_UP)
        assertEquals(0, dismissals)
        touch(view, t + 100, t + 100, MotionEvent.ACTION_DOWN)
        assertEquals(0, dismissals)
        touch(view, t + 100, t + 130, MotionEvent.ACTION_UP)
        assertEquals(1, dismissals)
    }

    @Test fun pointerChangesAndCancellationConsumeInputAndResetTheSequence() = onMain {
        var dismissed = false
        val view = ProtectionView(instrumentation.targetContext, { true }) { dismissed = true }
        val t = SystemClock.uptimeMillis()
        touch(view, t, t, MotionEvent.ACTION_DOWN)
        touch(view, t, t + 20, MotionEvent.ACTION_UP)
        touch(view, t + 100, t + 100, MotionEvent.ACTION_DOWN)
        pointerDown(view, t + 100, t + 105)
        touch(view, t + 100, t + 110, MotionEvent.ACTION_CANCEL)
        touch(view, t + 150, t + 150, MotionEvent.ACTION_DOWN)
        touch(view, t + 150, t + 170, MotionEvent.ACTION_UP)
        assertFalse(dismissed)
    }

    private fun touch(view: View, downTime: Long, time: Long, action: Int) {
        val event = MotionEvent.obtain(downTime, time, action, 50f, 80f, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        try {
            assertTrue(view.dispatchTouchEvent(event))
        } finally {
            event.recycle()
        }
    }

    private fun pointerDown(view: View, downTime: Long, eventTime: Long) {
        val properties = Array(2) { index ->
            MotionEvent.PointerProperties().apply {
                id = index
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }
        val positions = Array(2) { index ->
            MotionEvent.PointerCoords().apply {
                x = 50f + index * 30
                y = 80f
                pressure = 1f
                size = 1f
            }
        }
        val event = MotionEvent.obtain(
            downTime, eventTime, MotionEvent.ACTION_POINTER_DOWN or
                (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            2, properties, positions, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
        try {
            assertTrue(view.dispatchTouchEvent(event))
        } finally {
            event.recycle()
        }
    }

    private fun onMain(test: () -> Unit) {
        var failure: Throwable? = null
        instrumentation.runOnMainSync {
            runCatching(test).onFailure { failure = it }
        }
        failure?.let { throw it }
    }
}
