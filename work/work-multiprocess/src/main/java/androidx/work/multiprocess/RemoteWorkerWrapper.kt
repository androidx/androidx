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
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.Logger
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import androidx.work.impl.utils.futures.SettableFuture
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger

internal class RemoteWorkerWrapper(val future: ListenableFuture<ListenableWorker.Result>) {
    internal val stopReason = AtomicInteger(WorkInfo.STOP_REASON_NOT_STOPPED)

    fun interrupt(stopReason: Int) {
        this.stopReason.set(stopReason)
        future.cancel(true)
    }
}

@JvmName("create")
internal fun RemoteWorkerWrapper(
    context: Context,
    configuration: Configuration,
    workerClassName: String,
    workerParameters: WorkerParameters,
    taskExecutor: TaskExecutor,
): RemoteWorkerWrapper {
    val future = SettableFuture.create<ListenableWorker.Result>()
    val wrapper = RemoteWorkerWrapper(future)

    taskExecutor.mainThreadExecutor.execute {
        try {
            if (future.isCancelled) return@execute
            val worker = configuration.workerFactory
                .createWorkerWithDefaultFallback(context, workerClassName, workerParameters)
            if (worker == null) {
                val message = "Unable to create an instance of $workerClassName"
                Logger.get().error(ListenableWorkerImpl.TAG, message)
                future.setException(IllegalStateException(message))
                return@execute
            }
            if (worker !is RemoteListenableWorker) {
                val message = "$workerClassName does not extend " +
                    RemoteListenableWorker::class.java.name
                Logger.get().error(ListenableWorkerImpl.TAG, message)
                future.setException(IllegalStateException(message))
                return@execute
            }
            future.addListener({
                try {
                    future.get()
                } catch (e: CancellationException) {
                    worker.stop(wrapper.stopReason.get())
                } catch (_: Throwable) {
                    // Here we handle only cancellations.
                    // Other exceptions are handled through over channels
                }
            }, taskExecutor.serialTaskExecutor)
            future.setFuture(worker.startRemoteWork())
        } catch (throwable: Throwable) {
            future.setException(throwable)
        }
    }
    return wrapper
}
