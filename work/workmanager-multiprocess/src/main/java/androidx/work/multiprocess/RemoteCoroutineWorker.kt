/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.work.multiprocess

import android.content.Context
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.await
import androidx.work.impl.utils.futures.SettableFuture
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * An implementation of [RemoteListenableWorker] that can bind to a remote process.
 *
 * To be able to bind to a remote process, A [RemoteCoroutineWorker] needs additional
 * arguments as part of its input [androidx.work.Data].
 *
 * The arguments [RemoteListenableWorker.ARGUMENT_PACKAGE_NAME],
 * [RemoteListenableWorker.ARGUMENT_CLASS_NAME] are used to determine the [android.app.Service]
 * that the [RemoteCoroutineWorker] can bind to.
 *
 * [doRemoteWork] is then subsequently called in the process that the [android.app.Service] is
 * running in.
 */
public abstract class RemoteCoroutineWorker(context: Context, parameters: WorkerParameters) :
    RemoteListenableWorker(context, parameters) {

    private val job = Job()
    private val future: SettableFuture<Result> = SettableFuture.create()

    init {
        future.addListener(
            Runnable {
                if (future.isCancelled) {
                    job.cancel()
                }
            },
            taskExecutor.backgroundExecutor
        )
    }

    /**
     * Override this method to define the work that needs to run in the remote process.
     * [Dispatchers.Default] is the coroutine dispatcher being used when this method is called.
     *
     * A [RemoteCoroutineWorker] has a well defined
     * [execution window](https://d.android.com/reference/android/app/job/JobScheduler) to finish
     * its execution and return a [androidx.work.ListenableWorker.Result]. Note that the
     * execution window also includes the cost of binding to the remote process.
     */
    public abstract suspend fun doRemoteWork(): Result

    override fun startRemoteWork(): ListenableFuture<Result> {
        val scope = CoroutineScope(Dispatchers.Default + job)
        scope.launch {
            try {
                val result = doRemoteWork()
                future.set(result)
            } catch (exception: Throwable) {
                future.setException(exception)
            }
        }
        return future
    }

    /**
     * Updates the progress for the [RemoteCoroutineWorker]. This is a suspending function unlike
     * [setProgressAsync] API which returns a [ListenableFuture].
     *
     * @param data The progress [Data]
     */
    public suspend fun setProgress(data: Data) {
        setProgressAsync(data).await()
    }

    public final override fun onStopped() {
        super.onStopped()
        future.cancel(true)
    }
}
