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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class FakeLifecycleOwner(initialState: Lifecycle.State? = null) : LifecycleOwner {
    private val registry: LifecycleRegistry = LifecycleRegistry(this)

    init {
        initialState?.let {
            setState(it)
        }
    }

    override fun getLifecycle(): Lifecycle = registry

    fun setState(state: Lifecycle.State) {
        registry.currentState = state
    }

    fun pause() {
        runBlocking(Dispatchers.Main) {
            setState(Lifecycle.State.STARTED)
        }
    }

    fun destroy() {
        runBlocking(Dispatchers.Main) {
            setState(Lifecycle.State.DESTROYED)
        }
    }

    fun create() {
        runBlocking(Dispatchers.Main) {
            setState(Lifecycle.State.CREATED)
        }
    }

    fun start() {
        runBlocking(Dispatchers.Main) {
            setState(Lifecycle.State.STARTED)
        }
    }

    fun resume() {
        runBlocking(Dispatchers.Main) {
            setState(Lifecycle.State.RESUMED)
        }
    }

    private suspend fun getObserverCount(): Int {
        return withContext(Dispatchers.Main) {
            registry.observerCount
        }
    }
}