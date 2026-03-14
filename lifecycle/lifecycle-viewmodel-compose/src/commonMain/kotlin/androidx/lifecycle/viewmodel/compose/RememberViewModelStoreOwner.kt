/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.lifecycle.viewmodel.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.defaultViewModelCreationExtras
import androidx.lifecycle.defaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner

/**
 * Remembers a [ViewModelStoreOwner] scoped to the current composable.
 *
 * This function creates an owner that is unique to this specific call site in the composition
 * hierarchy. It allows creating ViewModels that are strictly scoped to this composable's lifecycle:
 * they are created when this composable enters the composition and cleared immediately when it
 * leaves.
 *
 * The owner is linked to the [parent], ensuring that configuration changes (like rotation) are
 * handled correctly: the ViewModels survive rotation if the parent does, but are destroyed if the
 * parent is destroyed.
 *
 * **Null parent:** If [parent] is **EXPLICITLY** `null`, this creates a root provider that runs
 * independently. By default, it requires a parent from the [LocalViewModelStoreOwner] and will
 * throw an [IllegalStateException] if one is not present.
 *
 * @param parent The [ViewModelStoreOwner] to use as the parent. Defaults to the owner from
 *   [LocalViewModelStoreOwner].
 * @param savedStateRegistryOwner An optional [SavedStateRegistryOwner] to delegate saved state
 *   operations. When `null`, ViewModels created in this scope do not support saved state.
 * @param defaultCreationExtras The [CreationExtras] to use. Defaults to the [parent]'s default
 *   extras.
 * @param defaultFactory The [ViewModelProvider.Factory] to use for creating ViewModels in this
 *   scope. Defaults to the [parent]'s default factory.
 * @return A [ViewModelStoreOwner] that is remembered across compositions and scoped to this call
 *   site.
 */
@Composable
public fun rememberViewModelStoreOwner(
    parent: ViewModelStoreOwner? =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "CompositionLocal LocalViewModelStoreOwner not present"
        },
    savedStateRegistryOwner: SavedStateRegistryOwner? = LocalSavedStateRegistryOwner.current,
    defaultCreationExtras: CreationExtras = parent.defaultViewModelCreationExtras,
    defaultFactory: ViewModelProvider.Factory = parent.defaultViewModelProviderFactory,
): ViewModelStoreOwner {
    val provider = rememberViewModelStoreProvider(parent, defaultCreationExtras, defaultFactory)
    return rememberViewModelStoreOwner(
        provider = provider,
        savedStateRegistryOwner = savedStateRegistryOwner,
    )
}

/**
 * Remembers a [ViewModelStoreOwner] scoped to the current composable using an existing [provider].
 *
 * This function creates an owner unique to this specific call site in the composition hierarchy.
 * While this composable is active, it maintains an active reference to the underlying
 * [ViewModelStore], preventing it from being cleared.
 *
 * **Note:** Unlike many other scoped owners, ViewModels created with this owner are **not**
 * automatically cleared simply because this composable leaves the composition. The [ViewModelStore]
 * is only cleared when [ViewModelStoreProvider.clearKey] is explicitly called for this [key].
 *
 * This function is responsible for releasing its reference to the store when it leaves the
 * composition, allowing the [provider] to perform cleanup if the store has been marked for
 * clearing.
 *
 * @param provider The [ViewModelStoreProvider] that manages the creation and cleanup of the
 *   underlying [ViewModelStore].
 * @param key A unique identifier for this call site to isolate its store from others. Defaults to
 *   [currentCompositeKeyHashCode]. If called multiple times in the same scope or loop, provide a
 *   custom key to ensure each instance gets its own [ViewModelStore].
 * @param savedStateRegistryOwner An optional [SavedStateRegistryOwner] to delegate saved state
 *   operations. When `null`, ViewModels created in this scope do not support saved state.
 * @return A [ViewModelStoreOwner] remembered across compositions and scoped to this call site.
 */
@Composable
public fun rememberViewModelStoreOwner(
    provider: ViewModelStoreProvider,
    key: Any? = currentCompositeKeyHashCode,
    savedStateRegistryOwner: SavedStateRegistryOwner? = LocalSavedStateRegistryOwner.current,
): ViewModelStoreOwner {
    val owner =
        remember(provider, key, savedStateRegistryOwner) {
            provider.getOrCreateOwner(key, savedStateRegistryOwner)
        }

    DisposableEffect(owner) {
        val token = provider.acquireToken(key)
        onDispose { token.close() }
    }

    return owner
}
