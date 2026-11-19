package fr.pocketblackscreen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {
    private val machine = SessionStateMachine()
    private var clock = 100L
    private val scheduled = mutableListOf<Long>()
    private var cancellations = 0
    private val timeout = SessionTimeout({ clock }, { scheduled.add(it) }, { cancellations++ })

    @Test fun expirationUsesElapsedTimeIncludingTimeWithoutCallbacks() {
        timeout.start(120_000)
        assertEquals(listOf(120_100L), scheduled)
        clock = 120_099
        assertFalse(timeout.hasExpired())
        clock = 120_100
        assertTrue(timeout.hasExpired())
    }

    @Test fun stoppingCancelsTheDeadlineAndAStaleCheckCannotExpireIt() {
        timeout.start(10_000)
        timeout.cancel()
        timeout.cancel()
        clock = 30_000
        assertEquals(1, cancellations)
        assertNull(timeout.deadlineMillis)
        assertFalse(timeout.hasExpired())
    }

    @Test fun transitionsThroughActiveAndSuspendedBeforeStopping() {
        assertFalse(machine.showOverlay())
        assertTrue(machine.start())
        assertEquals(SessionState.STARTING, machine.state)
        assertTrue(machine.showOverlay())
        assertEquals(SessionState.ACTIVE, machine.state)
        assertTrue(machine.suspendForLock())
        assertEquals(SessionState.SUSPENDED_FOR_LOCK, machine.state)
        assertTrue(machine.showOverlay())
        assertTrue(machine.stop())
        assertEquals(SessionState.STOPPING, machine.state)
        assertTrue(machine.finish())
        assertEquals(SessionState.INACTIVE, machine.state)
    }

    @Test fun activationIsIdempotentUntilTheOldSessionHasFinished() {
        assertTrue(machine.start())
        assertFalse(machine.start())
        assertTrue(machine.showOverlay())
        assertFalse(machine.start())
        assertTrue(machine.suspendForLock())
        assertFalse(machine.start())
        assertTrue(machine.stop())
        assertFalse(machine.start())
        assertTrue(machine.finish())
        assertTrue(machine.start())
    }

    @Test fun repeatedStopHasNoEffect() {
        assertFalse(machine.stop())
        assertTrue(machine.start())
        assertTrue(machine.stop())
        assertFalse(machine.stop())
        assertTrue(machine.finish())
        assertFalse(machine.stop())
    }

    @Test fun stopWinsWhenLockAndStopAreDispatchedInEitherOrder() {
        for (lockFirst in listOf(false, true)) {
            val session = SessionStateMachine()
            assertTrue(session.start())
            assertTrue(session.showOverlay())
            if (lockFirst) assertTrue(session.suspendForLock())
            assertTrue(session.stop())
            assertFalse(session.suspendForLock())
            assertFalse(session.showOverlay())
            assertTrue(session.finish())
            assertFalse(session.showOverlay())
            assertEquals(SessionState.INACTIVE, session.state)
        }
    }
}
