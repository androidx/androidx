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

package androidx.window.java

import androidx.core.util.Consumer
import androidx.window.WindowInfoRepo
import androidx.window.WindowLayoutInfo
import androidx.window.WindowMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * An adapted interface for [WindowInfoRepo] that allows listening for events via a callback
 * shaped API.
 */
class WindowInfoRepoCallbackAdapter(
    private val repo: WindowInfoRepo
) : WindowInfoRepo by repo {

    /**
     * A [ReentrantLock] to protect against concurrent access to [consumerToJobMap].
     */
    private val lock = ReentrantLock()
    private val consumerToJobMap = mutableMapOf<Consumer<*>, Job>()

    /**
     * Register a listener to consume [WindowMetrics] values. If the same consumer is registered
     * twice then this method is a no-op.
     * @see WindowInfoRepo.currentWindowMetrics
     */
    fun addCurrentWindowMetricsListener(executor: Executor, consumer: Consumer<WindowMetrics>) {
        addListener(executor, consumer, repo.currentWindowMetrics)
    }

    /**
     * Remove a listener to stop consuming [WindowMetrics] values. If the listener has already
     * been removed then this is a no-op.
     * @see WindowInfoRepo.currentWindowMetrics
     */
    fun removeCurrentWindowMetricsListener(consumer: Consumer<WindowMetrics>) {
        removeListener(consumer)
    }

    /**
     * Register a listener to consume [WindowLayoutInfo] values. If the same consumer is
     * registered twice then this method is a no-op.
     * @see WindowInfoRepo.windowLayoutInfo
     */
    fun addWindowLayoutInfoListener(
        executor: Executor,
        consumer: Consumer<WindowLayoutInfo>
    ) {
        addListener(executor, consumer, repo.windowLayoutInfo)
    }

    /**
     * Remove a listener to stop consuming [WindowLayoutInfo] values. If the listener has already
     * been removed then this is a no-op.
     * @see WindowInfoRepo.windowLayoutInfo
     */
    fun removeWindowLayoutInfoListener(consumer: Consumer<WindowLayoutInfo>) {
        removeListener(consumer)
    }

    /**
     * Generic method for registering a [Consumer] to collect the values from a [Flow].
     * Registering the same [Consumer] is a no-op.
     */
    private fun <T> addListener(executor: Executor, consumer: Consumer<T>, flow: Flow<T>) {
        lock.withLock {
            if (consumerToJobMap[consumer] == null) {
                val scope = CoroutineScope(executor.asCoroutineDispatcher())
                consumerToJobMap[consumer] = scope.launch {
                    flow.collect { consumer.accept(it) }
                }
            }
        }
    }

    /**
     * Generic method for canceling a [Job] related to a consumer. Canceling twice in a row is a
     * no-op.
     */
    private fun removeListener(consumer: Consumer<*>) {
        lock.withLock {
            consumerToJobMap[consumer]?.cancel()
            consumerToJobMap.remove(consumer)
        }
    }
}