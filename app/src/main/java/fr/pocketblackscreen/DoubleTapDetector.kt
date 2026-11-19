package fr.pocketblackscreen

internal class DoubleTapDetector(
    private val touchSlop: Float,
    private val doubleTapSlop: Float,
    private val longPressTimeoutMillis: Long,
) {
    private data class Down(val x: Float, val y: Float, val time: Long)

    private var current: Down? = null
    private var firstTap: Down? = null

    fun down(x: Float, y: Float, time: Long) {
        if (current != null) cancel()
        current = Down(x, y, time)
        firstTap = firstTap?.takeIf { first ->
            time - first.time in 0..MAX_INTERVAL_MILLIS &&
                withinSlop(first, x, y, doubleTapSlop)
        }
    }

    fun move(x: Float, y: Float, time: Long) {
        val down = current ?: return
        if (time - down.time !in 0 until longPressTimeoutMillis ||
            !withinSlop(down, x, y, touchSlop)
        ) {
            cancel()
        }
    }

    fun up(x: Float, y: Float, time: Long): Boolean {
        move(x, y, time)
        val down = current ?: return false
        current = null
        if (firstTap != null) {
            firstTap = null
            return true
        }
        firstTap = down
        return false
    }

    fun cancel() {
        current = null
        firstTap = null
    }

    private fun withinSlop(down: Down, x: Float, y: Float, slop: Float): Boolean {
        val dx = x - down.x
        val dy = y - down.y
        return dx * dx + dy * dy <= slop * slop
    }

    private companion object {
        const val MAX_INTERVAL_MILLIS = 300L
    }
}
