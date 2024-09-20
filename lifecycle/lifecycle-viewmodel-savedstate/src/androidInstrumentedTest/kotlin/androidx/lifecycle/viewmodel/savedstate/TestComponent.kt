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
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

class TestComponent(
    val vmStore: ViewModelStore = ViewModelStore(),
    bundle: Bundle? = null,
) : SavedStateRegistryOwner, LifecycleOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    init {
        savedStateController.performRestore(bundle)
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override val viewModelStore: ViewModelStore = vmStore

    fun resume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun recreate(keepingViewModels: Boolean): TestComponent {
        val bundle = Bundle()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        performSave(bundle)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        if (!keepingViewModels) vmStore.clear()
        return TestComponent(vmStore.takeIf { keepingViewModels } ?: ViewModelStore(), bundle)
    }

    fun performSave(bundle: Bundle) {
        savedStateController.performSave(bundle)
    }

    fun createSavedStateHandle(key: String, bundle: Bundle? = null): SavedStateHandle {
        val extras = MutableCreationExtras()
        extras[VIEW_MODEL_STORE_OWNER_KEY] = this
        extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
        extras[ViewModelProvider.NewInstanceFactory.VIEW_MODEL_KEY] = key
        if (bundle != null) extras[DEFAULT_ARGS_KEY] = bundle
        return extras.createSavedStateHandle()
    }
}
