/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.work

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A class that performs work synchronously on a background thread provided by [WorkManager].
 *
 * Worker classes are instantiated at runtime by WorkManager and the [.doWork] method is
 * called on a pre-specified background thread (see [Configuration.executor]).  This
 * method is for **synchronous** processing of your work, meaning that once you return from that
 * method, the Worker is considered to be finished and will be destroyed.  If you need to do your
 * work asynchronously or call asynchronous APIs, you should use [ListenableWorker].
 *
 *
 * In case the work is preempted for any reason, the same instance of Worker is not reused.  This
 * means that [.doWork] is called exactly once per Worker instance.  A new Worker is created
 * if a unit of work needs to be rerun.
 *
 *
 * A Worker is given a maximum of ten minutes to finish its execution and return a
 * [androidx.work.ListenableWorker.Result].  After this time has expired, the Worker will be
 * signalled to stop.
 */
abstract class Worker(
    context: Context,
    workerParams: WorkerParameters
) : ListenableWorker(context, workerParams) {

    /**
     * Override this method to do your actual background processing.  This method is called on a
     * background thread - you are required to **synchronously** do your work and return the
     * [androidx.work.ListenableWorker.Result] from this method.  Once you return from this
     * method, the Worker is considered to have finished what its doing and will be destroyed.  If
     * you need to do your work asynchronously on a thread of your own choice, see
     * [ListenableWorker].
     *
     *
     * A Worker has a well defined
     * [execution window](https://d.android.com/reference/android/app/job/JobScheduler)
     * to finish its execution and return a [androidx.work.ListenableWorker.Result].  After
     * this time has expired, the Worker will be signalled to stop.
     *
     * @return The [androidx.work.ListenableWorker.Result] of the computation; note that
     * dependent work will not execute if you use
     * [androidx.work.ListenableWorker.Result.failure] or
     * [androidx.work.ListenableWorker.Result.failure]
     */
    @WorkerThread
    abstract fun doWork(): Result

    final override fun startWork(): ListenableFuture<Result> = backgroundExecutor.future {
        doWork()
    }

    override fun getForegroundInfoAsync(): ListenableFuture<ForegroundInfo> =
        backgroundExecutor.future { getForegroundInfo() }

    /**
     * An instance of [ForegroundInfo] if the [WorkRequest] is important to
     * the user.  In this case, WorkManager provides a signal to the OS that the process should
     * be kept alive while this work is executing.
     *
     *
     * Prior to Android S, WorkManager manages and runs a foreground service on your behalf to
     * execute the WorkRequest, showing the notification provided in the [ForegroundInfo].
     * To update this notification subsequently, the application can use
     * [android.app.NotificationManager].
     *
     *
     * Starting in Android S and above, WorkManager manages this WorkRequest using an immediate job.
     *
     * @return A [ForegroundInfo] instance if the WorkRequest is marked immediate.
     * For more information look at [WorkRequest.Builder.setExpedited].
     * @throws IllegalStateException if it is not overridden and worker tries to go to foreground
     */
    @WorkerThread
    open fun getForegroundInfo(): ForegroundInfo {
        throw IllegalStateException(
            "Expedited WorkRequests require a Worker to provide an implementation for " +
                "`getForegroundInfo()`"
        )
    }
}

private fun <T> Executor.future(
    block: () -> T
): ListenableFuture<T> = CallbackToFutureAdapter.getFuture {
    val cancelled = AtomicBoolean(false)
    it.addCancellationListener({ cancelled.set(true) }, DirectExecutor.INSTANCE)
    execute {
        if (cancelled.get()) return@execute
        try {
            it.set(block())
        } catch (t: Throwable) {
            it.setException(t)
        }
    }
}
