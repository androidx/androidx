/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TrackingRemoteWorkerFactory : WorkerFactory() {
    private val factory = object : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? = null
    }
    val createdWorkers = MutableStateFlow<Map<UUID, Workers>>(emptyMap())

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker {
        return factory.createWorkerWithDefaultFallback(
            appContext,
            workerClassName,
            workerParameters
        )!!.also {
            val oldWorkers = createdWorkers.value[it.id]
            val workers = if (oldWorkers == null)
                Workers(localProxy = it)
            else
                Workers(oldWorkers.localProxy, it)

            createdWorkers.value = createdWorkers.value + (it.id to workers)
        }
    }

    suspend fun awaitRemote(id: UUID): ListenableWorker =
        createdWorkers.map { it[id]?.remote }.filterNotNull().first()

    data class Workers(val localProxy: ListenableWorker, val remote: ListenableWorker? = null)
}
