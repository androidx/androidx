/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.xr.scenecore.testing

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import com.google.common.base.Preconditions
import com.google.common.collect.Lists
import com.google.errorprone.annotations.CheckReturnValue
import java.lang.AutoCloseable
import java.time.Duration
import java.time.Instant
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
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.Volatile

/**
 * Fake implementation of [ScheduledExecutorService] that lets tests control when tasks are
 * executed.
 */
@SuppressLint("NewApi") // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class FakeScheduledExecutorService public constructor() :
    AbstractExecutorService(), ScheduledExecutorService, AutoCloseable {
    private val clock: Clock
    private val executeQueue: Queue<Runnable> = ConcurrentLinkedQueue()
    private val scheduledQueue = PriorityBlockingQueue<DelayedFuture<*>>()
    private val nextSequenceId = AtomicLong(0)

    @Volatile private var isRunning = true

    init {
        clock = Clock()
    }

    override fun isShutdown(): Boolean {
        return !isRunning
    }

    override fun isTerminated(): Boolean {
        return isShutdown && this.isEmpty()
    }

    override fun shutdown() {
        isRunning = false
    }

    override fun close() {
        shutdown()
    }

    override fun shutdownNow(): MutableList<Runnable> {
        isRunning = false
        val commands: MutableList<Runnable> = Lists.newArrayList()
        commands.addAll(executeQueue)
        commands.addAll(scheduledQueue)
        executeQueue.clear()
        scheduledQueue.clear()
        return commands
    }

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        Preconditions.checkState(!isRunning)
        while (!executeQueue.isEmpty()) {
            runNext()
        }
        simulateSleepExecutingAllTasks(durationFromClockUnit(timeout))
        return this.isEmpty()
    }

    override fun execute(command: Runnable) {
        assertRunning()
        executeQueue.add(command)
    }

    private fun assertRunning() {
        if (!isRunning) {
            throw RejectedExecutionException()
        }
    }

    override fun schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        assertRunning()
        val future: DelayedFuture<*> = DelayedFuture<Any>(command, delay, unit)
        scheduledQueue.add(future)
        return future
    }

    override fun <V> schedule(
        callable: Callable<V>,
        delay: Long,
        unit: TimeUnit,
    ): ScheduledFuture<V> {
        assertRunning()
        val future: DelayedFuture<V> = DelayedCallable(callable, delay, unit)
        scheduledQueue.add(future)
        return future
    }

    override fun scheduleAtFixedRate(
        command: Runnable,
        initialDelay: Long,
        period: Long,
        unit: TimeUnit,
    ): ScheduledFuture<*> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun scheduleWithFixedDelay(
        command: Runnable,
        initialDelay: Long,
        delay: Long,
        unit: TimeUnit,
    ): ScheduledFuture<*> {
        throw UnsupportedOperationException("not implemented")
    }

    /** Returns true if the [.execute] queue contains at least one runnable. */
    public fun hasNext(): Boolean {
        return !executeQueue.isEmpty()
    }

    /** Runs the next runnable in the [.execute] queue. */
    public fun runNext() {
        Preconditions.checkState(!executeQueue.isEmpty(), "execute queue must not be empty")
        val runnable = executeQueue.remove()
        runTaskWithInterruptIsolation(runnable)
    }

    /** Runs all the runnable that [.execute] enqueued. */
    public fun runAll() {
        while (hasNext()) {
            runNext()
        }
    }

    /** Returns whether any runnable is in the [.execute] or [.schedule] queue. */
    @CheckReturnValue
    public fun isEmpty(): Boolean {
        return executeQueue.isEmpty() && scheduledQueue.isEmpty()
    }

    /**
     * Executes tasks from the [.schedule] queue until the given amount of simulated time has
     * passed.
     */
    public fun simulateSleepExecutingAllTasks(duration: Duration) {
        val timeout: Long = toClockUnit(duration)
        Preconditions.checkArgument(timeout >= 0, "timeout (%s) cannot be negative", timeout)

        val stopTime: Long = clock.currentTimeMillis() + CLOCK_UNIT.toMillis(timeout)
        var done = false

        while (!done) {
            val delay = (stopTime - clock.currentTimeMillis())
            if (delay >= 0 && simulateSleepExecutingAtMostOneTask(durationFromClockUnit(delay))) {
                continue
            } else {
                done = true
            }
        }
    }

    /**
     * Simulates sleeping up to the given timeout before executing the next scheduled task, if any.
     */
    public fun simulateSleepExecutingAtMostOneTask(duration: Duration): Boolean {
        val timeout: Long = toClockUnit(duration)
        Preconditions.checkArgument(timeout >= 0, "timeout (%s) cannot be negative", timeout)
        if (scheduledQueue.isEmpty()) {
            clock.advanceBy(duration)
            return false
        }

        val future = scheduledQueue.peek()
        val delay = future!!.getDelay(CLOCK_UNIT)
        if (delay > timeout) {
            // Next event is too far in the future; delay the entire time
            clock.advanceBy(duration)
            return false
        }

        scheduledQueue.poll()
        runTaskWithInterruptIsolation(future)

        return true
    }

    /**
     * Simulates sleeping as long as necessary before executing the next scheduled task. Does
     * nothing if the [.schedule] queue is empty.
     */
    public fun simulateSleepExecutingAtMostOneTask(): Boolean {
        if (scheduledQueue.isEmpty()) {
            return false
        }

        val future = scheduledQueue.poll()
        runTaskWithInterruptIsolation(future!!)
        return true
    }

    /** Clears this thread's interrupt bit, runs the task, and restores any previous interrupt. */
    private fun runTaskWithInterruptIsolation(task: Runnable) {
        val interruptBitWasSet = Thread.interrupted()
        try {
            task.run()
        } finally {
            if (interruptBitWasSet) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private class Clock {
        private val nowReference = AtomicReference(Instant.EPOCH)

        fun currentTimeMillis(): Long {
            return nowReference.get()!!.toEpochMilli()
        }

        fun advanceBy(duration: Duration) {
            nowReference.getAndUpdate { now: Instant -> now.plus(duration) }
        }

        fun setTo(instant: Instant) {
            nowReference.set(instant)
        }
    }

    private open inner class DelayedFuture<T>(command: Runnable, delay: Long, unit: TimeUnit) :
        ScheduledFuture<T>, Runnable {
        protected val mTimeToRun: Long
        private val mSequenceId: Long
        private val mCommand: Runnable
        private var mCancelled = false
        private var mDone = false

        init {
            Preconditions.checkArgument(delay >= 0, "delay (%s) cannot be negative", delay)

            mCommand = command
            mTimeToRun = clock.currentTimeMillis() + unit.toMillis(delay)
            mSequenceId = nextSequenceId.getAndIncrement()
        }

        override fun getDelay(unit: TimeUnit): Long {
            return unit.convert(mTimeToRun - clock.currentTimeMillis(), TimeUnit.MILLISECONDS)
        }

        protected fun maybeReschedule() {
            mDone = true
        }

        override fun run() {
            if (clock.currentTimeMillis() < mTimeToRun) {
                clock.advanceBy(durationFromClockUnit(mTimeToRun - clock.currentTimeMillis()))
            }
            mCommand.run()
            maybeReschedule()
        }

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            mCancelled = true
            mDone = true
            return scheduledQueue.remove(this)
        }

        override fun isCancelled(): Boolean {
            return mCancelled
        }

        override fun isDone(): Boolean {
            return mDone
        }

        @Suppress("UNCHECKED_CAST")
        @Throws(InterruptedException::class, ExecutionException::class)
        override fun get(): T {
            return null as T
        }

        @Suppress("UNCHECKED_CAST")
        @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
        override fun get(timeout: Long, unit: TimeUnit): T {
            return null as T
        }

        override fun compareTo(other: Delayed): Int {
            if (other === this) {
                return 0
            }
            val that = other as DelayedFuture<*>
            val diff = mTimeToRun - that.mTimeToRun
            return if (diff < 0) {
                -1
            } else if (diff > 0) {
                1
            } else if (mSequenceId < that.mSequenceId) {
                -1
            } else {
                1
            }
        }
    }

    private inner class DelayedCallable<T>(
        private val mTask: FutureTask<T>,
        delay: Long,
        unit: TimeUnit,
    ) : DelayedFuture<T>(mTask, delay, unit) {

        constructor(
            callable: Callable<T>,
            delay: Long,
            unit: TimeUnit,
        ) : this(FutureTask<T>(callable), delay, unit)

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            mTask.cancel(mayInterruptIfRunning)
            return super.cancel(mayInterruptIfRunning)
        }

        @Throws(InterruptedException::class, ExecutionException::class)
        override fun get(): T {
            return mTask.get()
        }

        @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
        override fun get(timeout: Long, unit: TimeUnit): T {
            return mTask.get(timeout, unit)
        }
    }

    public companion object {
        private val CLOCK_UNIT = TimeUnit.MILLISECONDS

        private fun toClockUnit(duration: Duration): Long {
            return duration.toMillis()
        }

        private fun durationFromClockUnit(durationClockUnit: Long): Duration {
            return Duration.ofMillis(durationClockUnit)
        }
    }
}
