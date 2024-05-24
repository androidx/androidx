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

package androidx.camera.impl.utils.executor

import android.os.Handler
import android.os.SystemClock
import androidx.camera.impl.utils.futures.Futures
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.Callable
import java.util.concurrent.Delayed
import java.util.concurrent.ExecutionException
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.RunnableScheduledFuture
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

/**
 * An implementation of [ScheduledExecutorService] which delegates all scheduled task to the given
 * [Handler].
 *
 * Currently, can only be used to schedule future non-repeating tasks.
 */
internal class HandlerScheduledExecutorService(private val handler: Handler) :
    AbstractExecutorService(), ScheduledExecutorService {
    override fun schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        val wrapper: Callable<Void> = Callable {
            command.run()
            null
        }
        return schedule(wrapper, delay, unit)
    }

    override fun <V> schedule(
        callable: Callable<V>,
        delay: Long,
        unit: TimeUnit
    ): ScheduledFuture<V> {
        val runAtMillis = SystemClock.uptimeMillis() + TimeUnit.MILLISECONDS.convert(delay, unit)
        val future = HandlerScheduledFuture(handler, runAtMillis, callable)
        return if (handler.postAtTime(future, runAtMillis)) {
            future
        } else Futures.immediateFailedScheduledFuture(createPostFailedException())
    }

    override fun scheduleAtFixedRate(
        command: Runnable,
        initialDelay: Long,
        period: Long,
        unit: TimeUnit
    ): ScheduledFuture<*> {
        throw UnsupportedOperationException(
            HandlerScheduledExecutorService::class.java.getSimpleName() +
                " does not yet support fixed-rate scheduling."
        )
    }

    override fun scheduleWithFixedDelay(
        command: Runnable,
        initialDelay: Long,
        delay: Long,
        unit: TimeUnit
    ): ScheduledFuture<*> {
        throw UnsupportedOperationException(
            HandlerScheduledExecutorService::class.java.getSimpleName() +
                " does not yet support fixed-delay scheduling."
        )
    }

    override fun shutdown() {
        throw UnsupportedOperationException(
            HandlerScheduledExecutorService::class.java.getSimpleName() +
                " cannot be shut down. Use Looper.quitSafely()."
        )
    }

    override fun shutdownNow(): List<Runnable> {
        throw UnsupportedOperationException(
            HandlerScheduledExecutorService::class.java.getSimpleName() +
                " cannot be shut down. Use Looper.quitSafely()."
        )
    }

    override fun isShutdown(): Boolean {
        return false
    }

    override fun isTerminated(): Boolean {
        return false
    }

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        throw UnsupportedOperationException(
            HandlerScheduledExecutorService::class.java.getSimpleName() +
                " cannot be shut down. Use Looper.quitSafely()."
        )
    }

    override fun execute(command: Runnable) {
        if (!handler.post(command)) {
            throw createPostFailedException()
        }
    }

    private fun createPostFailedException(): RejectedExecutionException {
        return RejectedExecutionException("$handler is shutting down")
    }

    private class HandlerScheduledFuture<V>(
        handler: Handler,
        private val mRunAtMillis: Long,
        private val mTask: Callable<V>
    ) : RunnableScheduledFuture<V> {
        val mCompleter = AtomicReference<CallbackToFutureAdapter.Completer<V>?>(null)
        private val mDelegate: ListenableFuture<V>

        init {
            mDelegate =
                CallbackToFutureAdapter.getFuture { completer ->
                    completer.addCancellationListener(
                        { // Remove the completer if we're cancelled so the task won't
                            // run.
                            if (mCompleter.getAndSet(null) != null) {
                                handler.removeCallbacks(this@HandlerScheduledFuture)
                            }
                        },
                        ViewfinderExecutors.directExecutor()
                    )
                    mCompleter.set(completer)
                    "HandlerScheduledFuture-$mTask"
                }
        }

        override fun isPeriodic(): Boolean {
            return false
        }

        override fun getDelay(unit: TimeUnit): Long {
            return unit.convert(mRunAtMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
        }

        override fun compareTo(other: Delayed): Int {
            return getDelay(TimeUnit.MILLISECONDS).compareTo(other.getDelay(TimeUnit.MILLISECONDS))
        }

        override fun run() {
            // If completer is null, it has already run or is cancelled.
            val completer = mCompleter.getAndSet(null)
            if (completer != null) {
                try {
                    completer.set(mTask.call())
                } catch (e: Exception) {
                    completer.setException(e)
                }
            }
        }

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            return mDelegate.cancel(mayInterruptIfRunning)
        }

        override fun isCancelled(): Boolean {
            return mDelegate.isCancelled
        }

        override fun isDone(): Boolean {
            return mDelegate.isDone
        }

        @Throws(ExecutionException::class, InterruptedException::class)
        override fun get(): V {
            return mDelegate.get()
        }

        @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
        override fun get(timeout: Long, unit: TimeUnit): V {
            return mDelegate[timeout, unit]
        }
    }
}
