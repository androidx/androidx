/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.navigation

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.internal.NavBackStackEntryImpl
import androidx.navigation.internal.NavContext
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.random.Random

public actual class NavBackStackEntry
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
private constructor(
    internal actual val context: NavContext?,
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public actual var destination: NavDestination,
    internal actual val immutableArgs: SavedState? = null,
    internal actual var hostLifecycleState: Lifecycle.State = Lifecycle.State.CREATED,
    internal actual val viewModelStoreProvider: NavViewModelStoreProvider? = null,
    public actual val id: String = randomUUID(),
    internal actual val savedState: SavedState? = null,
) :
    LifecycleOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual constructor(
        entry: NavBackStackEntry,
        arguments: SavedState?,
    ) : this(
        entry.context,
        entry.destination,
        arguments,
        entry.hostLifecycleState,
        entry.viewModelStoreProvider,
        entry.id,
        entry.savedState,
    ) {
        impl.hostLifecycleState = entry.hostLifecycleState
        impl.maxLifecycle = entry.maxLifecycle
    }

    public actual companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun create(
            context: NavContext?,
            destination: NavDestination,
            arguments: SavedState?,
            hostLifecycleState: Lifecycle.State,
            viewModelStoreProvider: NavViewModelStoreProvider?,
            id: String,
            savedState: SavedState?,
        ): NavBackStackEntry =
            NavBackStackEntry(
                context,
                destination,
                arguments,
                hostLifecycleState,
                viewModelStoreProvider,
                id,
                savedState,
            )

        @OptIn(ExperimentalStdlibApi::class)
        internal actual fun randomUUID(): String {
            val bytes =
                Random.nextBytes(16).also {
                    it[6] = it[6] and 0x0f // clear version
                    it[6] = it[6] or 0x40 // set to version 4
                    it[8] = it[8] and 0x3f // clear variant
                    it[8] = it[8] or 0x80.toByte() // set to IETF variant
                }
            return buildString(capacity = 36) {
                append(bytes.toHexString(0, 4))
                append('-')
                append(bytes.toHexString(4, 6))
                append('-')
                append(bytes.toHexString(6, 8))
                append('-')
                append(bytes.toHexString(8, 10))
                append('-')
                append(bytes.toHexString(10))
            }
        }
    }

    private val impl: NavBackStackEntryImpl = NavBackStackEntryImpl(this)

    public actual val arguments: SavedState? by impl::arguments

    @get:MainThread
    public actual val savedStateHandle: SavedStateHandle by lazy { impl.savedStateHandle }

    actual override val lifecycle: Lifecycle by impl::lifecycle

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual var maxLifecycle: Lifecycle.State
        get() = impl.maxLifecycle
        set(value) {
            impl.maxLifecycle = value
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun handleLifecycleEvent(event: Lifecycle.Event) {
        impl.handleLifecycleEvent(event)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun updateState() {
        impl.updateState()
    }

    public actual override val viewModelStore: ViewModelStore by impl::viewModelStore

    actual override val defaultViewModelProviderFactory: ViewModelProvider.Factory by
        impl::defaultViewModelProviderFactory

    actual override val defaultViewModelCreationExtras: CreationExtras by
        impl::defaultViewModelCreationExtras

    actual override val savedStateRegistry: SavedStateRegistry by impl::savedStateRegistry

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun saveState(outBundle: SavedState) {
        impl.saveState(outBundle)
    }
}
