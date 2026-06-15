/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.InternalComposeUiApi
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.read
import androidx.savedstate.savedState

/**
 * Provides platform-specific component owners.
 */
@InternalComposeUiApi
interface PlatformArchitectureComponentsOwner {
    val lifecycleOwner: LifecycleOwner
    val navigationEventDispatcherOwner: NavigationEventDispatcherOwner
    val viewModelStoreOwner: ViewModelStoreOwner?
    val savedStateRegistryOwner: SavedStateRegistryOwner
}

/**
 * Default implementation of [PlatformArchitectureComponentsOwner].
 */
@InternalComposeUiApi
class DefaultArchitectureComponentsOwner(
    savedState: SavedState? = null,
    override val viewModelStore: ViewModelStore = ViewModelStore(),
    enforceMainThread: Boolean = true,
) : PlatformArchitectureComponentsOwner,
    LifecycleOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    NavigationEventDispatcherOwner,
    SavedStateRegistryOwner {
    override val lifecycleOwner get() = this
    override val navigationEventDispatcherOwner get() = this
    override val viewModelStoreOwner get() = this
    override val savedStateRegistryOwner get() = this
    override val lifecycle = if (enforceMainThread) {
        LifecycleRegistry(this)
    } else {
        LifecycleRegistry.createUnsafe(this)
    }
    override val navigationEventDispatcher = NavigationEventDispatcher()

    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override val defaultViewModelProviderFactory = SavedStateViewModelFactory()
    override val defaultViewModelCreationExtras = defaultViewModelCreationExtras(this, this)

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(savedState)
    }

    fun saveState(): SavedState {
        val savedState = savedState()
        savedStateController.performSave(savedState)
        return savedState
    }

    fun setLifecycleState(state: State) {
        lifecycle.currentState = state
        if (state == State.DESTROYED) {
            viewModelStore.clear()
        }
    }
}

internal fun defaultViewModelCreationExtras(
    savedStateRegistryOwner: SavedStateRegistryOwner,
    viewModelStoreOwner: ViewModelStoreOwner
): CreationExtras = MutableCreationExtras().also {
    it[SAVED_STATE_REGISTRY_OWNER_KEY] = savedStateRegistryOwner
    it[VIEW_MODEL_STORE_OWNER_KEY] = viewModelStoreOwner
}
