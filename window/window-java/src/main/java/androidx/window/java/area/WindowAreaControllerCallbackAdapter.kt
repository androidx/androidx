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

package androidx.window.java.area

import androidx.core.util.Consumer
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaInfo
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

/**
 * An adapter for [WindowAreaController] to provide callback APIs.
 */
class WindowAreaControllerCallbackAdapter(
    private val controller: WindowAreaController
) : WindowAreaController by controller {

    /**
     * A [ReentrantLock] to protect against concurrent access to [consumerToJobMap].
     */
    private val lock = ReentrantLock()
    private val consumerToJobMap = mutableMapOf<Consumer<*>, Job>()

    /**
     * Registers a listener that is interested in the current list of [WindowAreaInfo] available to
     * be interacted with.
     *
     * The [listener] will receive an initial value on registration, as soon as it becomes
     * available.
     *
     * @param executor to handle sending listener updates.
     * @param listener to receive updates to the list of [WindowAreaInfo].
     * @see WindowAreaController.transferActivityToWindowArea
     * @see WindowAreaController.presentContentOnWindowArea
     */
    fun addWindowAreaInfoListListener(
        executor: Executor,
        listener: Consumer<List<WindowAreaInfo>>
    ) {
        // TODO(274013517): Extract adapter pattern out of each class
        val statusFlow = controller.windowAreaInfos
        lock.withLock {
            if (consumerToJobMap[listener] == null) {
                val scope = CoroutineScope(executor.asCoroutineDispatcher())
                consumerToJobMap[listener] = scope.launch {
                    statusFlow.collect { listener.accept(it) }
                }
            }
        }
    }

    /**
     * Removes a listener of available [WindowAreaInfo] records. If the listener is not present then
     * this method is a no-op.
     *
     * @param listener to remove from receiving status updates.
     * @see WindowAreaController.transferActivityToWindowArea
     * @see WindowAreaController.presentContentOnWindowArea
     */
    fun removeWindowAreaInfoListListener(listener: Consumer<List<WindowAreaInfo>>) {
        lock.withLock {
            consumerToJobMap[listener]?.cancel()
            consumerToJobMap.remove(listener)
        }
    }
 }