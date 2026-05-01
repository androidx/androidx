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
 * This function creates a provider scoped to this specific call site in the composition.
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
 *   to the owner from [LocalViewModelStoreOwner]. If this value changes, the provider will be
 *   recreated.
 * @param defaultArgs The [SavedState] containing default arguments to be passed to ViewModels
 *   created in this scope. These arguments are merged with any default arguments in
 *   [defaultCreationExtras]. If the same key exists in both, the value from [defaultArgs] takes
 *   precedence. This value is only read during the initial creation of the provider; subsequent
 *   recompositions with different values will not update the existing provider.
 * @param defaultCreationExtras The [CreationExtras] to use. Defaults to the [parent]'s default
 *   extras. This value is only read during the initial creation of the provider; subsequent
 *   recompositions with different values will not update or recreate the existing provider.
 * @param defaultFactory The [ViewModelProvider.Factory] to use for creating ViewModels in this
 *   scope. Defaults to the [parent]'s default factory. This value is only read during the initial
 *   creation of the provider; subsequent recompositions with different values will not update or
 *   recreate the existing provider.
 * @return A new [ViewModelStoreProvider] that is remembered across compositions and scoped to this
 *   call site.
 * @sample androidx.lifecycle.viewmodel.compose.samples.RememberViewModelStoreProviderSample
 */
@Composable
public fun rememberViewModelStoreProvider(
    parent: ViewModelStoreOwner? =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "CompositionLocal LocalViewModelStoreOwner not present"
        },
    defaultArgs: SavedState = savedState(),
    defaultCreationExtras: CreationExtras = parent.defaultViewModelCreationExtras,
    defaultFactory: ViewModelProvider.Factory = parent.defaultViewModelProviderFactory,
): ViewModelStoreProvider {
    return rememberViewModelStoreProvider(
        key = currentCompositeKeyHashCode,
        parent = parent,
        defaultArgs = defaultArgs,
        defaultCreationExtras = defaultCreationExtras,
        defaultFactory = defaultFactory,
    )
}

/**
 * Remembers a new [ViewModelStoreProvider] which creates a ViewModel scope linked to a parent found
 * in the composition, using a specific [key].
 *
 * This function allows you to scope a [ViewModelStoreProvider] to a custom [key]. This is useful in
 * cases where using a key in different parts of the UI should yield the same state or instance
 * (similar to how a ViewModel is shared). For example, you might use a key derived from navigation
 * arguments, such as `ViewModelNavEntryDecorator.contentKey`, to share the same owner across
 * different screens or components.
 *
 * The provider's lifecycle is automatically managed. It is created only once and automatically
 * disposed of when the composable leaves the composition. Crucially, it is aware of the parent's
 * state and will survive configuration changes (like device rotation) if the parent does.
 *
 * **Null parent:** If [parent] is **EXPLICITLY** `null`, this creates a root provider that runs
 * independently. By default, it requires a parent from the [LocalViewModelStoreOwner] and will
 * throw an [IllegalStateException] if one is not present.
 *
 * @param key A unique identifier to isolate this provider from others. Providing the same [key] and
 *   [parent] to multiple [rememberViewModelStoreProvider] calls will yield providers that share the
 *   same internal state, so matching child keys will share the same state. If this value changes,
 *   the provider will be recreated.
 * @param parent The [ViewModelStoreOwner] to use as the parent, or `null` if it is a root. Defaults
 *   to the owner from [LocalViewModelStoreOwner]. If this value changes, the provider will be
 *   recreated.
 * @param defaultArgs The [SavedState] containing default arguments to be passed to ViewModels
 *   created in this scope. These arguments are merged with any default arguments in
 *   [defaultCreationExtras]. If the same key exists in both, the value from [defaultArgs] takes
 *   precedence. This value is only read during the initial creation of the provider; subsequent
 *   recompositions with different values will not update the existing provider.
 * @param defaultCreationExtras The [CreationExtras] to use. Defaults to the [parent]'s default
 *   extras. This value is only read during the initial creation of the provider; subsequent
 *   recompositions with different values will not update or recreate the existing provider.
 * @param defaultFactory The [ViewModelProvider.Factory] to use for creating ViewModels in this
 *   scope. Defaults to the [parent]'s default factory. This value is only read during the initial
 *   creation of the provider; subsequent recompositions with different values will not update or
 *   recreate the existing provider.
 * @return A new [ViewModelStoreProvider] that is remembered across compositions and scoped to the
 *   provided [key].
 * @sample androidx.lifecycle.viewmodel.compose.samples.RememberViewModelStoreProviderWithKeySample
 */
@Composable
public fun rememberViewModelStoreProvider(
    key: Any?,
    parent: ViewModelStoreOwner? =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "CompositionLocal LocalViewModelStoreOwner not present"
        },
    defaultArgs: SavedState = savedState(),
    defaultCreationExtras: CreationExtras = parent.defaultViewModelCreationExtras,
    defaultFactory: ViewModelProvider.Factory = parent.defaultViewModelProviderFactory,
): ViewModelStoreProvider {
    val provider =
        remember(parent, key) {
            ViewModelStoreProvider(parent, key, defaultArgs, defaultCreationExtras, defaultFactory)
        }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(provider, lifecycle) {
        val token = provider.acquireToken(ViewModelStoreProvider.ProviderMarkerKey)
        onDispose {
            token.close()

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
