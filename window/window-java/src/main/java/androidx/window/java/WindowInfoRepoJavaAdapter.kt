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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A Java compatible interface of [WindowInfoRepo].
 */
public class WindowInfoRepoJavaAdapter(private val repo: WindowInfoRepo) : WindowInfoRepo by repo {

    /**
     * A [ReentrantLock] to protect against concurrent access to [consumerToJobMap].
     */
    private val lock = ReentrantLock()
    private val consumerToJobMap = mutableMapOf<Consumer<WindowLayoutInfo>, Job>()

    /**
     * Register a listener to consume [WindowLayoutInfo] values. If the same consumer is
     * registered twice then this method is a no-op.
     * @see WindowInfoRepo.getWindowLayoutInfo
     */
    public fun addWindowLayoutInfoListener(
        executor: Executor,
        consumer: Consumer<WindowLayoutInfo>
    ) {
        lock.withLock {
            if (consumerToJobMap[consumer] == null) {
                val scope = CoroutineScope(executor.asCoroutineDispatcher())
                consumerToJobMap[consumer] = scope.launch {
                    repo.windowLayoutInfo.collect { consumer.accept(it) }
                }
            }
        }
    }

    /**
     * Remove a listener to stop consuming [WindowLayoutInfo] values. If the listener has already
     * been removed then this is a no-op.
     * @see WindowInfoRepo.getWindowLayoutInfo
     */
    public fun removeWindowLayoutInfoListener(consumer: Consumer<WindowLayoutInfo>) {
        lock.withLock {
            consumerToJobMap[consumer]?.cancel()
            consumerToJobMap.remove(consumer)
        }
    }
}