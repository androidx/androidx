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

package androidx.window.java.core

import androidx.annotation.GuardedBy
import androidx.core.util.Consumer
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * A utility class to convert from a [Consumer<T>] to a [Flow<T>].
 */
internal class CallbackToFlowAdapter {

    private val globalLock = ReentrantLock()

    @GuardedBy("globalLock")
    private val consumerToJobMap = mutableMapOf<Consumer<*>, Job>()

    /**
     * Generic method for registering a [Consumer] to collect the values from a [Flow].
     * Registering the same [Consumer] is a no-op.
     */
    fun <T : Any> connect(executor: Executor, consumer: Consumer<T>, flow: Flow<T>) {
        globalLock.withLock {
            if (consumerToJobMap[consumer] == null) {
                val scope = CoroutineScope(executor.asCoroutineDispatcher())
                consumerToJobMap[consumer] = scope.launch {
                    flow.collect { value ->
                        consumer.accept(value)
                    }
                }
            }
        }
    }

    /**
     * Generic method for canceling a [Job] related to a consumer. Canceling twice in a row is a
     * no-op.
     */
    fun disconnect(consumer: Consumer<*>) {
        globalLock.withLock {
            consumerToJobMap[consumer]?.cancel()
            consumerToJobMap.remove(consumer)
        }
    }
}
