package fr.pocketblackscreen

internal enum class SessionState {
    INACTIVE, STARTING, ACTIVE, SUSPENDED_FOR_LOCK, STOPPING,
}

internal class SessionStateMachine {
    var state = SessionState.INACTIVE
        private set

    fun start() = transition(SessionState.STARTING, SessionState.INACTIVE)

    fun showOverlay() =
        transition(SessionState.ACTIVE, SessionState.STARTING, SessionState.SUSPENDED_FOR_LOCK)

    fun suspendForLock() =
        transition(SessionState.SUSPENDED_FOR_LOCK, SessionState.STARTING, SessionState.ACTIVE)

    fun stop() = transition(
        SessionState.STOPPING,
        SessionState.STARTING,
        SessionState.ACTIVE,
        SessionState.SUSPENDED_FOR_LOCK,
    )

    fun finish() = transition(SessionState.INACTIVE, SessionState.STOPPING)

    private fun transition(next: SessionState, vararg allowedFrom: SessionState): Boolean {
        if (state !in allowedFrom) return false
        state = next
        return true
    }
}

internal class SessionTimeout(
    private val elapsedRealtime: () -> Long,
    private val schedule: (Long) -> Unit,
    private val unschedule: () -> Unit,
) {
    var deadlineMillis: Long? = null
        private set

    fun start(durationMillis: Long) {
        cancel()
        val deadline = elapsedRealtime() + durationMillis
        deadlineMillis = deadline
        schedule(deadline)
    }

    fun hasExpired(): Boolean = deadlineMillis?.let { elapsedRealtime() >= it } ?: false

    fun cancel() {
        if (deadlineMillis == null) return
        deadlineMillis = null
        unschedule()
    }
}
