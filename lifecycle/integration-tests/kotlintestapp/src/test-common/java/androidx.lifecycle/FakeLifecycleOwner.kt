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

package androidx.lifecycle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class FakeLifecycleOwner(initialState: Lifecycle.State? = null) : LifecycleOwner {
    private val registry: LifecycleRegistry = LifecycleRegistry(this)

    init {
        initialState?.let {
            setState(it)
        }
    }

    override fun getLifecycle(): Lifecycle = registry

    fun setState(state: Lifecycle.State) {
        registry.markState(state)
    }

    suspend fun awaitExactObserverCount(count: Int, timeout: Long = 1000L): Boolean =
    // just give job some time to start
        withTimeoutOrNull(timeout) {
            while (getObserverCount(count) != count) {
                delay(50)
            }
            true
        } ?: false

    private suspend fun getObserverCount(count: Int): Int {
        return withContext(Dispatchers.Main) {
            registry.observerCount
        }
    }
}