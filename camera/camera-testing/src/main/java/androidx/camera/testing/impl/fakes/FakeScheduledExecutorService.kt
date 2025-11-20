/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.testing.impl.fakes

import androidx.annotation.RestrictTo
import com.google.common.base.Preconditions
import java.util.Queue
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Delayed
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.Volatile
import kotlin.math.max

/**
 * Fake implementation of [ScheduledExecutorService] that lets tests control when tasks are
 * executed.
 *
 * This executor does not run tasks automatically. Time must be advanced manually via the
 * [advanceTimeBy] method, which will execute any tasks scheduled within that time frame. Tasks
 * submitted with `execute()` are queued and can be run with [runNext] or [runAll].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeScheduledExecutorService :
    AbstractExecutorService(), ScheduledExecutorService, AutoCloseable {

    private val clock: Clock = Clock()
    private val executeQueue: Queue<Runnable> = ConcurrentLinkedQueue()
    private val scheduledQueue = PriorityBlockingQueue<DelayedFuture<*>>()
    private val nextSequenceId = AtomicLong(0)

    @Volatile private var isRunning = true

    override fun isShutdown(): Boolean = !isRunning

    override fun isTerminated(): Boolean = isShutdown && isEmpty

    override fun shutdown() {
        isRunning = false
    }

    override fun close(): Unit = shutdown()

    override fun shutdownNow(): List<Runnable> {
        shutdown()
        val remainingTasks = executeQueue.toList() + scheduledQueue.toList()
        executeQueue.clear()
        scheduledQueue.clear()
        return remainingTasks
    }

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        Preconditions.checkState(isShutdown)
        advanceTimeBy(timeout, unit)
        runAll() // Run any remaining non-delayed tasks
        return isTerminated
    }

    override fun execute(command: Runnable) {
        assertRunning()
        executeQueue.add(command)
    }

    private fun assertRunning() {
        if (!isRunning) {
            throw RejectedExecutionException("Executor has been shut down.")
        }
    }

    override fun schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        assertRunning()
        val future = DelayedFuture<Void?>(command, triggerTime(delay, unit))
        scheduledQueue.add(future)
        return future
    }

    override fun <V> schedule(
        callable: Callable<V>,
        delay: Long,
        unit: TimeUnit,
    ): ScheduledFuture<V> {
        assertRunning()
        val future = DelayedCallable(callable, triggerTime(delay, unit))
        scheduledQueue.add(future)
        return future
    }

    override fun scheduleAtFixedRate(
        command: Runnable,
        initialDelay: Long,
        period: Long,
        unit: TimeUnit,
    ): ScheduledFuture<*> {
        assertRunning()
        val periodMs = unit.toMillis(period)
        val future: DelayedFuture<out Any?> =
            PeriodicFuture<Void>(
                command,
                triggerTime(initialDelay, unit),
                periodMs,
                isFixedRate = true,
            )
        scheduledQueue.add(future)
        return future
    }

    override fun scheduleWithFixedDelay(
        command: Runnable,
        initialDelay: Long,
        delay: Long,
        unit: TimeUnit,
    ): ScheduledFuture<*> {
        assertRunning()
        val delayMs = unit.toMillis(delay)
        val future: DelayedFuture<out Any?> =
            PeriodicFuture<Void>(
                command,
                triggerTime(initialDelay, unit),
                delayMs,
                isFixedRate = false,
            )
        scheduledQueue.add(future)
        return future
    }

    /** Returns true if the [execute] queue contains at least one runnable. */
    public fun hasNext(): Boolean = executeQueue.isNotEmpty()

    /** Runs the next runnable in the [execute] queue. */
    public fun runNext() {
        Preconditions.checkState(hasNext(), "Execute queue is empty.")
        runTaskWithInterruptIsolation(executeQueue.remove())
    }

    /** Runs all runnables that have been enqueued via [execute]. */
    public fun runAll() {
        while (hasNext()) {
            runNext()
        }
    }

    /** Returns whether any runnable is in the [execute] or [schedule] queue. */
    public val isEmpty: Boolean
        get() = executeQueue.isEmpty() && scheduledQueue.isEmpty()

    /**
     * Advances the simulated clock by the given duration, executing any scheduled tasks that become
     * due during this time period.
     */
    public fun advanceTimeBy(delay: Long, unit: TimeUnit) {
        val durationMs = unit.toMillis(delay)
        Preconditions.checkArgument(delay >= 0, "Delay cannot be negative.")

        val stopTimeMs = clock.currentTimeMillis() + durationMs
        while (true) {
            val nextTask = scheduledQueue.peek() ?: break
            val nextTaskTimeMs = nextTask.timeToRunMs

            if (nextTaskTimeMs <= stopTimeMs) {
                val taskToRun = scheduledQueue.poll()
                if (taskToRun != null) {
                    // Next task is within the time window, so execute it
                    clock.setTo(nextTaskTimeMs)
                    runTaskWithInterruptIsolation(taskToRun)

                    // If it's a periodic task, reschedule it
                    if (taskToRun is PeriodicFuture && !taskToRun.isCancelled()) {
                        taskToRun.reschedule(clock.currentTimeMillis())
                    }
                }
            } else {
                // Next task is after the time window
                break
            }
        }
        // Set the clock to the final time
        clock.setTo(stopTimeMs)
    }

    private fun triggerTime(delay: Long, unit: TimeUnit): Long =
        clock.currentTimeMillis() + unit.toMillis(delay)

    private open inner class DelayedFuture<T>(
        private val command: Runnable,
        val timeToRunMs: Long,
        private val sequenceId: Long = nextSequenceId.getAndIncrement(),
    ) : ScheduledFuture<T>, Runnable {

        @Volatile protected var _isDone = false
        @Volatile private var _isCancelled = false

        override fun getDelay(unit: TimeUnit): Long {
            val remainingMs = max(0, timeToRunMs - clock.currentTimeMillis())
            return unit.convert(remainingMs, TimeUnit.MILLISECONDS)
        }

        override fun run() {
            if (_isCancelled) return
            command.run()
            _isDone = true
        }

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            if (_isDone) return false
            _isCancelled = true
            _isDone = true
            return scheduledQueue.remove(this)
        }

        override fun isCancelled(): Boolean = _isCancelled

        override fun isDone(): Boolean = _isDone

        @Throws(InterruptedException::class, ExecutionException::class)
        @Suppress("UNCHECKED_CAST")
        override fun get(): T = null as T // Runnable-based futures don't have a result

        @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
        @Suppress("UNCHECKED_CAST")
        override fun get(timeout: Long, unit: TimeUnit): T {
            // This fake implementation does not support blocking on get()
            if (!_isDone) throw TimeoutException()
            return null as T
        }

        override fun compareTo(other: Delayed): Int {
            if (other === this) return 0
            if (other is DelayedFuture<*>) {
                val timeDiff = this.timeToRunMs - other.timeToRunMs
                if (timeDiff < 0) return -1
                if (timeDiff > 0) return 1
                if (this.sequenceId < other.sequenceId) return -1
                if (this.sequenceId > other.sequenceId) return 1
            }
            val delayDiff =
                this.getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS)
            return if (delayDiff < 0) -1 else if (delayDiff > 0) 1 else 0
        }
    }

    private inner class DelayedCallable<T>(private val task: FutureTask<T>, timeToRunMs: Long) :
        DelayedFuture<T>(task, timeToRunMs) {
        constructor(
            callable: Callable<T>,
            timeToRunMs: Long,
        ) : this(FutureTask(callable), timeToRunMs)

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            task.cancel(mayInterruptIfRunning)
            return super.cancel(mayInterruptIfRunning)
        }

        @Throws(InterruptedException::class, ExecutionException::class)
        override fun get(): T = task.get()

        @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
        override fun get(timeout: Long, unit: TimeUnit): T = task.get(timeout, unit)
    }

    private inner class PeriodicFuture<T>(
        private val command: Runnable,
        initialTimeToRunMs: Long,
        private val periodMs: Long,
        private val isFixedRate: Boolean,
    ) : DelayedFuture<T?>(command, initialTimeToRunMs) {

        fun reschedule(lastExecutionTimeMs: Long) {
            val nextTimeMs =
                if (isFixedRate) {
                    timeToRunMs + periodMs
                } else {
                    lastExecutionTimeMs + periodMs
                }
            val rescheduled = PeriodicFuture<Void?>(command, nextTimeMs, periodMs, isFixedRate)
            scheduledQueue.add(rescheduled)
        }
    }

    private class Clock {
        private val nowReference = AtomicLong(0L)

        fun currentTimeMillis(): Long = nowReference.get()

        fun setTo(timeMs: Long) = nowReference.set(timeMs)
    }

    private fun runTaskWithInterruptIsolation(task: Runnable) {
        val wasInterrupted = Thread.interrupted()
        try {
            task.run()
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt()
            }
        }
    }
}
