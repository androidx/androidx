/*
 * Copyright 2019 The Android Open Source Project
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
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A [WorkerFactory] which delegates to other factories. Factories can register themselves
 * as delegates, and they will be invoked in order until a delegated factory returns a
 * non-null [ListenableWorker] instance.
 */
open class DelegatingWorkerFactory : WorkerFactory() {
    // Use a CopyOnWriteArrayList here to allow modifying a list of factories during
    // iteration. This allows createWorker() to call addFactory().
    private val factories: MutableList<WorkerFactory> = CopyOnWriteArrayList()

    /**
     * Adds a [WorkerFactory] to the list of delegates.
     *
     * @param workerFactory The [WorkerFactory] instance.
     */
    fun addFactory(workerFactory: WorkerFactory) {
        factories.add(workerFactory)
    }

    final override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        // If none of the delegates can instantiate a ListenableWorker return null
        // so we can fallback to the default factory which is based on reflection.
        return factories.firstNotNullOfOrNull { factory ->
            try {
                factory.createWorker(appContext, workerClassName, workerParameters)
            } catch (throwable: Throwable) {
                val message = "Unable to instantiate a ListenableWorker ($workerClassName)"
                Logger.get().error(TAG, message, throwable)
                throw throwable
            }
        }
    }
}

private val TAG = Logger.tagWithPrefix("DelegatingWkrFctry")
