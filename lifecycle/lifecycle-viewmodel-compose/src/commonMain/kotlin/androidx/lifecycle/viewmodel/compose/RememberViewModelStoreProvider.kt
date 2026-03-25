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
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.defaultViewModelCreationExtras
import androidx.lifecycle.defaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.savedstate.SavedState
import androidx.savedstate.savedState

/**
 * Remembers a new [ViewModelStoreProvider] which creates a ViewModel scope linked to a parent found
 * in the composition.
 *
 * This composable creates a provider that links to any parent found in the composition, forming a
 * parent-child relationship. If no parent exists, it automatically becomes a new root provider.
 * This is useful for isolating ViewModel instances within specific UI sections, such as a
 * self-contained feature screen, dialog, or tab, ensuring they are cleared when that section is
 * removed.
 *
 * The provider's lifecycle is automatically managed. It is created only once and automatically
 * disposed of when the composable leaves the composition. Crucially, it is aware of the parent's
 * state and will survive configuration changes (like device rotation) if the parent does.
 *
 * **Null parent:** If [parent] is **EXPLICITLY** `null`, this creates a root provider that runs
 * independently. By default, it requires a parent from the [LocalViewModelStoreOwner] and will
 * throw an [IllegalStateException] if one is not present.
 *
 * @param parent The [ViewModelStoreOwner] to use as the parent, or `null` if it is a root. Defaults
 *   to the owner from [LocalViewModelStoreOwner].
 * @param key A unique identifier for this call site to isolate its provider from others. Defaults
 *   to [currentCompositeKeyHashCode]. If called multiple times in the same scope or loop, provide a
 *   custom key to ensure each instance gets its own [ViewModelStoreProvider]. A `null` key is valid
 *   and is treated as a distinct scope.
 * @param defaultArgs The [SavedState] containing default arguments to be passed to ViewModels
 *   created in this scope. These arguments are merged with any default arguments in
 *   [defaultCreationExtras]. If the same key exists in both, the value from [defaultArgs] takes
 *   precedence.
 * @param defaultCreationExtras The [CreationExtras] to use. Defaults to the [parent]'s default
 *   extras.
 * @param defaultFactory The [ViewModelProvider.Factory] to use for creating ViewModels in this
 *   scope. Defaults to the [parent]'s default factory.
 * @return A new [ViewModelStoreProvider] that is remembered across compositions.
 */
@Composable
public fun rememberViewModelStoreProvider(
    parent: ViewModelStoreOwner? =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "CompositionLocal LocalViewModelStoreOwner not present"
        },
    key: Any? = currentCompositeKeyHashCode,
    defaultArgs: SavedState = savedState(),
    defaultCreationExtras: CreationExtras = parent.defaultViewModelCreationExtras,
    defaultFactory: ViewModelProvider.Factory = parent.defaultViewModelProviderFactory,
): ViewModelStoreProvider {
    val provider =
        remember(parent, key, defaultFactory, defaultCreationExtras) {
            ViewModelStoreProvider(parent, key, defaultArgs, defaultCreationExtras, defaultFactory)
        }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(provider, lifecycle) {
        onDispose {
            // We are NOT waiting for an ON_DESTROY event, instead we are executing a cleanup hook
            // that is guaranteed to run when this composable leaves the tree, and checking the
            // Parent's current state to decide *if* we should dispose.
            //
            // If the Parent Lifecycle is DESTROYED, it implies the Activity is either:
            // 1. Rotating (Configuration Change): We MUST NOT dispose, so the data survives.
            // 2. Finishing: The Parent's own ViewModelStore will clear everything anyway.
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                provider.clearAllKeys()
            }
        }
    }

    return provider
}
