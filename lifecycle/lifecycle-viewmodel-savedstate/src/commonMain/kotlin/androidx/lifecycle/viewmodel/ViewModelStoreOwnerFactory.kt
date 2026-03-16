/*
 * Copyright 2026 The Android Open Source Project
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

@file:JvmName("ViewModelStoreOwnerFactory")

package androidx.lifecycle.viewmodel

import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SAVED_STATE_KEY
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.savedState
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

/** Creates a [ViewModelStoreOwner] by composing a store, factory, and creation extras. */
@JvmName("create")
@JvmOverloads
public fun ViewModelStoreOwner(
    viewModelStore: ViewModelStore,
    defaultArgs: SavedState = savedState(),
    defaultCreationExtras: CreationExtras = CreationExtras.Empty,
    defaultFactory: Factory = SavedStateViewModelFactory(),
): ViewModelStoreOwner {
    return object : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
        public override val viewModelStore
            get() = viewModelStore

        public override val defaultViewModelProviderFactory
            get() = defaultFactory

        public override val defaultViewModelCreationExtras: CreationExtras
            get() =
                MutableCreationExtras(defaultCreationExtras).also { extras ->
                    extras[DEFAULT_ARGS_KEY] = defaultArgs
                    extras[VIEW_MODEL_STORE_OWNER_KEY] = this
                }
    }
}

/**
 * Creates a [ViewModelStoreOwner] that also acts as a [SavedStateRegistryOwner], composing a store,
 * state registry, lifecycle, factory, and creation extras.
 *
 * While the simpler [ViewModelStoreOwner] factory handles standard ViewModels, this overload
 * provides the necessary infrastructure for ViewModels that rely on [SavedStateHandle]. By bundling
 * the [SavedStateRegistryOwner] with the store, it creates a fully equipped scope capable of
 * properly wiring up state-saving mechanisms for its ViewModels.
 */
@JvmName("create")
@JvmOverloads
public fun ViewModelStoreOwner(
    viewModelStore: ViewModelStore,
    savedStateRegistryOwner: SavedStateRegistryOwner,
    defaultArgs: SavedState = savedState(),
    defaultCreationExtras: CreationExtras = CreationExtras.Empty,
    defaultFactory: Factory = SavedStateViewModelFactory(),
): ViewModelStoreOwner {
    return ViewModelStoreOwner(
        viewModelStore,
        savedStateRegistryOwner.savedStateRegistry,
        savedStateRegistryOwner.lifecycle,
        defaultArgs,
        defaultCreationExtras,
        defaultFactory,
    )
}

/**
 * Creates a [ViewModelStoreOwner] that also acts as a [SavedStateRegistryOwner], composing a store,
 * state registry, lifecycle, factory, and creation extras.
 *
 * While the simpler [ViewModelStoreOwner] factory handles standard ViewModels, this overload
 * provides the necessary infrastructure for ViewModels that rely on [SavedStateHandle]. By bundling
 * the [SavedStateRegistry] and [Lifecycle] with the store, it creates a fully equipped scope
 * capable of properly wiring up state-saving mechanisms for its ViewModels.
 */
@JvmName("create")
@JvmOverloads
public fun ViewModelStoreOwner(
    viewModelStore: ViewModelStore,
    savedStateRegistry: SavedStateRegistry,
    lifecycle: Lifecycle,
    defaultArgs: SavedState = savedState(),
    defaultCreationExtras: CreationExtras = CreationExtras.Empty,
    defaultFactory: Factory = SavedStateViewModelFactory(),
): ViewModelStoreOwner {
    return object :
        ViewModelStoreOwner, HasDefaultViewModelProviderFactory, SavedStateRegistryOwner {

        override val viewModelStore
            get() = viewModelStore

        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistry

        override val lifecycle: Lifecycle
            get() = lifecycle

        override val defaultViewModelProviderFactory: Factory
            get() = defaultFactory

        override val defaultViewModelCreationExtras: CreationExtras
            get() =
                MutableCreationExtras(defaultCreationExtras).also { extras ->
                    extras[DEFAULT_ARGS_KEY] = defaultArgs
                    extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
                    extras[VIEW_MODEL_STORE_OWNER_KEY] = this
                }

        init {
            // The parent SavedStateRegistry may be reused across multiple child scopes.
            // If the provider is already registered, saved state is already enabled for
            // this registry, and we can safely skip the Lifecycle preconditions.
            if (savedStateRegistry.getSavedStateProvider(SAVED_STATE_KEY) == null) {
                enableSavedStateHandles()
            }
        }
    }
}
