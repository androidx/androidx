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

import androidx.savedstate.SavedStateRegistry
import java.io.Closeable

internal class SavedStateHandleController(private val key: String, val handle: SavedStateHandle) :
    LifecycleEventObserver, Closeable {

    var isAttached = false
        private set

    fun attachToLifecycle(registry: SavedStateRegistry, lifecycle: Lifecycle) {
        check(!isAttached) { "Already attached to lifecycleOwner" }
        isAttached = true
        lifecycle.addObserver(this)
        registry.registerSavedStateProvider(key, handle.savedStateProvider())
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event === Lifecycle.Event.ON_DESTROY) {
            isAttached = false
            source.lifecycle.removeObserver(this)
        }
    }

    override fun close() {
        // This class has nothing to actually close, but all objects added via
        // ViewModel's addCloseable(key, Closeable) must be Closeable.
    }
}
