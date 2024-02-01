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
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

/**
 * A [ListenableWorker] implementation that provides interop with Kotlin Coroutines.  Override
 * the [doWork] function to do your suspending work.
 * <p>
 * By default, CoroutineWorker runs on [Dispatchers.Default]; this can be modified by
 * overriding [coroutineContext].
 * <p>
 * A CoroutineWorker is given a maximum of ten minutes to finish its execution and return a
 * [ListenableWorker.Result].  After this time has expired, the worker will be signalled to stop.
 */
public abstract class CoroutineWorker(
    appContext: Context,
    params: WorkerParameters
) : ListenableWorker(appContext, params) {

    /**
     * The coroutine context on which [doWork] will run. By default, this is [Dispatchers.Default].
     */
    @Deprecated(message = "use withContext(...) inside doWork() instead.")
    public open val coroutineContext: CoroutineDispatcher = Dispatchers.Default

    @Suppress("DEPRECATION")
    public final override fun startWork(): ListenableFuture<Result> {
        return launchFuture(coroutineContext + Job()) { doWork() }
    }

    /**
     * A suspending method to do your work.
     * <p>
     * To specify which [CoroutineDispatcher] your work should run on, use `withContext()`
     * within `doWork()`.
     * If there is no other dispatcher declared, [Dispatchers.Default] will be used.
     * <p>
     * A CoroutineWorker is given a maximum of ten minutes to finish its execution and return a
     * [ListenableWorker.Result].  After this time has expired, the worker will be signalled to
     * stop.
     *
     * @return The [ListenableWorker.Result] of the result of the background work; note that
     * dependent work will not execute if you return [ListenableWorker.Result.failure]
     */
    public abstract suspend fun doWork(): Result

    /**
     * @return The [ForegroundInfo] instance if the [WorkRequest] is marked as expedited.
     *
     * @throws [IllegalStateException] when not overridden. Override this method when the
     * corresponding [WorkRequest] is marked expedited.
     */
    public open suspend fun getForegroundInfo(): ForegroundInfo {
        throw IllegalStateException("Not implemented")
    }

    /**
     * Updates the progress for the [CoroutineWorker]. This is a suspending function unlike the
     * [setProgressAsync] API which returns a [ListenableFuture].
     *
     * @param data The progress [Data]
     */
    public suspend fun setProgress(data: Data) {
        setProgressAsync(data).await()
    }

    /**
     * Makes the [CoroutineWorker] run in the context of a foreground [android.app.Service]. This
     * is a suspending function unlike the [setForegroundAsync] API which returns a
     * [ListenableFuture].
     *
     * Calling [setForeground] will throw an [IllegalStateException] if the process is subject to
     * foreground service restrictions. Consider using  [WorkRequest.Builder.setExpedited]
     * and [getForegroundInfo] instead.
     *
     * @param foregroundInfo The [ForegroundInfo]
     */
    public suspend fun setForeground(foregroundInfo: ForegroundInfo) {
        setForegroundAsync(foregroundInfo).await()
    }

    @Suppress("DEPRECATION")
    public final override fun getForegroundInfoAsync(): ListenableFuture<ForegroundInfo> {
        return launchFuture(coroutineContext + Job()) { getForegroundInfo() }
    }

    public final override fun onStopped() {
        super.onStopped()
    }
}
