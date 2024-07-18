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

package androidx.work.impl.testutils

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class TrackingWorkerFactory : WorkerFactory() {
    private val factory = object : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? = null
    }
    val createdWorkers = MutableStateFlow<Map<UUID, ListenableWorker>>(emptyMap())

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
            createdWorkers.value = createdWorkers.value + (it.id to it)
        }
    }

    fun awaitWorker(id: UUID) = runBlocking { await(id) }

    suspend fun await(id: UUID): ListenableWorker {
        return createdWorkers.map { it[id] }.filterNotNull().first()
    }
}
