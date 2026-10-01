package io.github.ieswar23.forkly.util

/** Abstraction over wall-clock time so time-based logic is testable with virtual time. */
fun interface Clock {
    fun now(): Long

    companion object {
        val System = Clock { java.lang.System.currentTimeMillis() }
    }
}
