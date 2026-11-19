package fr.pocketblackscreen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoubleTapDetectorTest {
    private val detector = DoubleTapDetector(8f, 40f, 500)

    @Test fun acceptsTwoNearbyShortTapsAtTheIntervalLimit() {
        assertFalse(tap(100))
        assertTrue(tap(400, x = 19f))
    }

    @Test fun aSingleTapDoesNotDismiss() {
        assertFalse(tap(100))
    }

    @Test fun tapsMoreThan300MillisecondsApartDoNotDismiss() {
        assertFalse(tap(100))
        assertFalse(tap(401))
    }

    @Test fun tapsTooFarApartDoNotDismiss() {
        assertFalse(tap(100))
        assertFalse(tap(180, x = 51f))
    }

    @Test fun dragReturningToItsOriginDoesNotDismiss() {
        assertFalse(tap(100))
        detector.down(10f, 10f, 180)
        detector.move(30f, 10f, 190)
        assertFalse(detector.up(10f, 10f, 210))
        assertFalse(tap(230))
    }

    @Test fun aLongFirstPressDoesNotSeedTheNextTap() {
        detector.down(10f, 10f, 100)
        assertFalse(detector.up(10f, 10f, 600))
        assertFalse(tap(620))
    }

    @Test fun aLongSecondPressDoesNotDismiss() {
        assertFalse(tap(100))
        detector.down(10f, 10f, 180)
        assertFalse(detector.up(10f, 10f, 680))
    }

    @Test fun multitouchCancellationDropsBothTapsInProgress() {
        assertFalse(tap(100))
        detector.down(10f, 10f, 180)
        detector.cancel()
        assertFalse(detector.up(10f, 10f, 205))
        assertFalse(tap(230))
    }

    @Test fun changingOrientationResetsTheFirstTap() {
        assertFalse(tap(100))
        detector.cancel()
        assertFalse(tap(180))
        assertTrue(tap(250))
    }

    private fun tap(time: Long, x: Float = 10f): Boolean {
        detector.down(x, 10f, time)
        return detector.up(x, 10f, time + 20)
    }
}
