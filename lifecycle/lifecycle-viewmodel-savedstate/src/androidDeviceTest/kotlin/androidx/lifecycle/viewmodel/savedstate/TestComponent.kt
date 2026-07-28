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

package androidx.lifecycle.viewmodel.savedstate

import android.os.Bundle
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * A test component that implements [SavedStateRegistryOwner] and [ViewModelStoreOwner]. Used to
 * simulate `Activity`, `Fragment`, or `NavBackStackEntry` lifecycles.
 */
class TestComponent(
    val vmStore: ViewModelStore = ViewModelStore(),
    bundle: Bundle? = null,
    private var isDestroyed: Boolean = false,
) : SavedStateRegistryOwner, LifecycleOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() {
            // Simulate destroyed or uninitialized host.
            check(!isDestroyed) { "Already destroyed" }
            return vmStore
        }

    private val savedStateController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    init {
        savedStateController.performRestore(bundle)
    }

    fun resume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        isDestroyed = true
    }

    fun recreate(keepingViewModels: Boolean): TestComponent {
        val bundle = Bundle()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        performSave(bundle)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        if (!keepingViewModels) {
            vmStore.clear()
        }
        return TestComponent(
            vmStore = if (keepingViewModels) vmStore else ViewModelStore(),
            bundle = bundle,
        )
    }

    fun performSave(bundle: Bundle) {
        savedStateController.performSave(bundle)
    }

    fun createSavedStateHandle(key: String, bundle: Bundle? = null): SavedStateHandle {
        val extras = CreationExtras {
            this[VIEW_MODEL_STORE_OWNER_KEY] = this@TestComponent
            this[SAVED_STATE_REGISTRY_OWNER_KEY] = this@TestComponent
            this[ViewModelProvider.VIEW_MODEL_KEY] = key
            bundle?.let { this[DEFAULT_ARGS_KEY] = it }
        }
        return extras.createSavedStateHandle()
    }
}
