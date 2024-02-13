/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.work.impl.utils

import android.content.Context
import android.os.Build
import androidx.concurrent.futures.await
import androidx.work.ForegroundUpdater
import androidx.work.ListenableWorker
import androidx.work.Logger
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.logd
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext

suspend fun workForeground(
    context: Context,
    spec: WorkSpec,
    worker: ListenableWorker,
    foregroundUpdater: ForegroundUpdater,
    taskExecutor: TaskExecutor
) {
    if (!spec.expedited || Build.VERSION.SDK_INT >= 31) return

    val dispatcher = taskExecutor.mainThreadExecutor.asCoroutineDispatcher()
    withContext(dispatcher) {
        val foregroundInfo = worker.getForegroundInfoAsync().await()
        if (foregroundInfo == null) {
            val message =
                "Worker was marked important (${spec.workerClassName}) " +
                    "but did not provide ForegroundInfo"
            throw IllegalStateException(message)
        }
        logd(TAG) { "Updating notification for ${spec.workerClassName}" }
        foregroundUpdater.setForegroundAsync(context, worker.id, foregroundInfo).await()
    }
}

private val TAG = Logger.tagWithPrefix("WorkForegroundRunnable")
