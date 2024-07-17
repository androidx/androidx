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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

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
        runBlocking(Dispatchers.Main) { setState(Lifecycle.State.STARTED) }
    }

    public fun destroy() {
        runBlocking(Dispatchers.Main) { setState(Lifecycle.State.DESTROYED) }
    }

    public fun create() {
        runBlocking(Dispatchers.Main) { setState(Lifecycle.State.CREATED) }
    }

    public fun start() {
        runBlocking(Dispatchers.Main) { setState(Lifecycle.State.STARTED) }
    }

    public fun resume() {
        runBlocking(Dispatchers.Main) { setState(Lifecycle.State.RESUMED) }
    }
}
