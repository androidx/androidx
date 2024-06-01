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

package androidx.work.multiprocess

import android.content.Context
import androidx.concurrent.futures.SuspendToFutureAdapter.launchFuture
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkerExceptionInfo
import androidx.work.WorkerParameters
import androidx.work.impl.awaitWithin
import androidx.work.impl.utils.safeAccept
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher

internal fun executeRemoteWorker(
    context: Context,
    configuration: Configuration,
    workerClassName: String,
    workerParameters: WorkerParameters,
    job: Job,
    taskExecutor: TaskExecutor,
): ListenableFuture<ListenableWorker.Result> {
    val dispatcher = taskExecutor.mainThreadExecutor.asCoroutineDispatcher()
    val future =
        launchFuture<ListenableWorker.Result>(dispatcher + job, launchUndispatched = false) {
            val worker =
                try {
                    configuration.workerFactory.createWorkerWithDefaultFallback(
                        context,
                        workerClassName,
                        workerParameters
                    )
                } catch (throwable: Throwable) {
                    configuration.workerInitializationExceptionHandler?.let { handler ->
                        taskExecutor.executeOnTaskThread {
                            handler.safeAccept(
                                WorkerExceptionInfo(workerClassName, workerParameters, throwable),
                                ListenableWorkerImpl.TAG
                            )
                        }
                    }
                    throw throwable
                }
            when (worker) {
                is RemoteListenableWorker -> worker.startRemoteWork().awaitWithin(worker)
                else ->
                    worker.startWork().awaitWithin(worker) // Just treat it as a delegated worker
            }
        }
    return future
}
