/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.glance.session

import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class TimeoutCancellationException(
    override val message: String,
    internal val block: Int,
) : CancellationException(message) {
    override fun toString() = "TimeoutCancellationException($message, $block)"
    override fun fillInStackTrace() = this
}

/**
 * This interface is similar to [kotlin.time.TimeSource], which is still marked experimental.
 */
internal fun interface TimeSource {
    /**
     * Current time in milliseconds.
     */
    fun markNow(): Long

    companion object {
        val Monotonic = TimeSource { System.currentTimeMillis() }
    }
}

/**
 * TimerScope is a CoroutineScope that allows setting an adjustable timeout for all of the
 * coroutines in the scope.
 */
internal interface TimerScope : CoroutineScope {
    /**
     * Amount of time left before this timer cancels the scope. This is not valid before
     * [startTimer] is called.
     */
    val timeLeft: Duration

    /**
     * Start the timer with an [initialTimeout].
     *
     * Once the [initialTimeout] has passed, the scope is cancelled. If [startTimer] is called again
     * while the timer is running, it will reset the timer if [initialTimeout] is less than
     * [timeLeft]. If [initialTimeout] is larger than [timeLeft], the current timer is kept.
     *
     * In order to extend the deadline, call [addTime].
     */
    fun startTimer(initialTimeout: Duration)

    /**
     *  Shift the deadline for this timer forward by [time].
     */
    fun addTime(time: Duration)
}

internal suspend fun <T> withTimer(
    timeSource: TimeSource = TimeSource.Monotonic,
    block: suspend TimerScope.() -> T,
): T = coroutineScope {
    val timerScope = this
    val timerJob: AtomicReference<Job?> = AtomicReference(null)
    coroutineScope {
        val blockScope = object : TimerScope, CoroutineScope by this {
            override val timeLeft: Duration
                get() = (deadline.get()?.minus(timeSource.markNow()))?.milliseconds
                    ?: Duration.INFINITE
            private val deadline: AtomicReference<Long?> = AtomicReference(null)

            override fun addTime(time: Duration) {
                deadline.update {
                    checkNotNull(it) { "Start the timer with startTimer before calling addTime" }
                    require(time.isPositive()) { "Cannot call addTime with a negative duration" }
                    it + time.inWholeMilliseconds
                }
            }

            override fun startTimer(initialTimeout: Duration) {
                if (initialTimeout.inWholeMilliseconds <= 0) {
                    timerScope.cancel(
                        TimeoutCancellationException("Timed out immediately", block.hashCode())
                    )
                    return
                }
                if (timeLeft < initialTimeout) return

                deadline.set(timeSource.markNow() + initialTimeout.inWholeMilliseconds)
                // Loop until the deadline is reached.
                timerJob.getAndSet(
                    timerScope.launch {
                        while (deadline.get()!! > timeSource.markNow()) {
                            delay(timeLeft)
                        }
                        timerScope.cancel(
                            TimeoutCancellationException(
                                "Timed out of executing block.",
                                block.hashCode()
                            )
                        )
                    }
                )?.cancel()
            }
        }
        blockScope.block()
    }.also {
        timerJob.get()?.cancel()
    }
}

internal suspend fun <T> withTimerOrNull(
    timeSource: TimeSource = TimeSource.Monotonic,
    block: suspend TimerScope.() -> T,
): T? = try {
    withTimer(timeSource, block)
} catch (e: TimeoutCancellationException) {
    // Return null if it's our exception, else propagate it upstream in case there are nested
    // withTimers
    if (e.block == block.hashCode()) null else throw e
}

// Update the value of the AtomicReference using the given updater function. Will throw an error
// if unable to successfully set the value.
private fun <T> AtomicReference<T>.update(updater: (T) -> T) {
    while (true) {
        get().let {
            if (compareAndSet(it, updater(it))) return
        }
    }
}
