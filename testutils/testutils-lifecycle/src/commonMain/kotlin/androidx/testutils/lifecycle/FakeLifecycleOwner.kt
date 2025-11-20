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

package androidx.testutils.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

public class FakeLifecycleOwner(initialState: Lifecycle.State? = null) : LifecycleOwner {

    private val registry: LifecycleRegistry = LifecycleRegistry.createUnsafe(owner = this)

    init {
        if (initialState != null) {
            setState(initialState)
        }
    }

    override val lifecycle: Lifecycle
        get() = registry

    public fun setState(state: Lifecycle.State) {
        registry.currentState = state
    }

    public fun pause() {
        runBlockingIfPossible(Dispatchers.Main) { setState(Lifecycle.State.STARTED) }
    }

    public fun destroy() {
        runBlockingIfPossible(Dispatchers.Main) { setState(Lifecycle.State.DESTROYED) }
    }

    public fun create() {
        runBlockingIfPossible(Dispatchers.Main) { setState(Lifecycle.State.CREATED) }
    }

    public fun start() {
        runBlockingIfPossible(Dispatchers.Main) { setState(Lifecycle.State.STARTED) }
    }

    public fun resume() {
        runBlockingIfPossible(Dispatchers.Main) { setState(Lifecycle.State.RESUMED) }
    }
}

/**
 * Executes the given block, blocking the current thread if the target platform supports it (e.g.,
 * JVM, Native).
 *
 * On single-threaded platforms like Kotlin/JS and Wasm, `runBlocking` is not supported as it would
 * freeze the only available thread. On these targets, this function will simply execute the block
 * directly without blocking.
 */
internal expect fun <T> runBlockingIfPossible(dispatcher: CoroutineDispatcher, block: () -> T): T
