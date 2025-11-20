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

package androidx.navigation.internal

import androidx.annotation.RestrictTo
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavViewModelStoreProvider
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.savedState

internal class NavBackStackEntryImpl(val entry: NavBackStackEntry) {

    internal val context: NavContext? = entry.context
    internal var destination: NavDestination = entry.destination
    internal val immutableArgs: SavedState? = entry.immutableArgs
    internal var hostLifecycleState: Lifecycle.State = entry.hostLifecycleState
    internal val viewModelStoreProvider: NavViewModelStoreProvider? = entry.viewModelStoreProvider
    internal val id: String = entry.id
    internal val savedState: SavedState? = entry.savedState

    internal val savedStateRegistryController = SavedStateRegistryController.create(entry)
    internal var savedStateRegistryAttached = false
    internal val defaultFactory by lazy { SavedStateViewModelFactory() }

    internal val arguments: SavedState?
        get() =
            if (immutableArgs == null) {
                null
            } else {
                savedState { putAll(immutableArgs) }
            }

    internal val savedStateHandle: SavedStateHandle
        get() {
            check(savedStateRegistryAttached) {
                "You cannot access the NavBackStackEntry's SavedStateHandle until it is added to " +
                    "the NavController's back stack (i.e., the Lifecycle of the NavBackStackEntry " +
                    "reaches the CREATED state)."
            }
            check(lifecycle.currentState != Lifecycle.State.DESTROYED) {
                "You cannot access the NavBackStackEntry's SavedStateHandle after the " +
                    "NavBackStackEntry is destroyed."
            }
            return ViewModelProvider.create(entry, navResultSavedStateFactory)[
                    SavedStateViewModel::class]
                .handle
        }

    internal val lifecycle = LifecycleRegistry(entry)

    internal var maxLifecycle: Lifecycle.State = Lifecycle.State.INITIALIZED
        set(maxState) {
            field = maxState
            updateState()
        }

    internal fun handleLifecycleEvent(event: Lifecycle.Event) {
        entry.hostLifecycleState = event.targetState
        hostLifecycleState = event.targetState
        updateState()
    }

    internal fun updateState() {
        if (!savedStateRegistryAttached) {
            savedStateRegistryController.performAttach()
            savedStateRegistryAttached = true
            if (viewModelStoreProvider != null) {
                entry.enableSavedStateHandles()
            }
            // Perform the restore just once, the first time updateState() is called
            // and specifically *before* we move up the Lifecycle
            savedStateRegistryController.performRestore(savedState)
        }
        if (hostLifecycleState.ordinal < maxLifecycle.ordinal) {
            lifecycle.currentState = hostLifecycleState
        } else {
            lifecycle.currentState = maxLifecycle
        }
    }

    internal val viewModelStore: ViewModelStore
        get() {
            check(savedStateRegistryAttached) {
                "You cannot access the NavBackStackEntry's ViewModels until it is added to " +
                    "the NavController's back stack (i.e., the Lifecycle of the " +
                    "NavBackStackEntry reaches the CREATED state)."
            }
            check(lifecycle.currentState != Lifecycle.State.DESTROYED) {
                "You cannot access the NavBackStackEntry's ViewModels after the " +
                    "NavBackStackEntry is destroyed."
            }
            checkNotNull(viewModelStoreProvider) {
                "You must call setViewModelStore() on your NavHostController before " +
                    "accessing the ViewModelStore of a navigation graph."
            }
            return viewModelStoreProvider.getViewModelStore(id)
        }

    internal val defaultViewModelProviderFactory: ViewModelProvider.Factory = defaultFactory

    internal val defaultViewModelCreationExtras: MutableCreationExtras
        get() {
            val extras = MutableCreationExtras()
            extras[SAVED_STATE_REGISTRY_OWNER_KEY] = entry
            extras[VIEW_MODEL_STORE_OWNER_KEY] = entry
            arguments?.let { args -> extras[DEFAULT_ARGS_KEY] = args }
            return extras
        }

    internal val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal fun saveState(outBundle: SavedState) {
        savedStateRegistryController.performSave(outBundle)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(entry::class.simpleName)
        sb.append("($id)")
        sb.append(" destination=")
        sb.append(destination)
        return sb.toString()
    }

    /** Used to create the {SavedStateViewModel} */
    private val navResultSavedStateFactory by lazy {
        viewModelFactory { initializer { SavedStateViewModel(createSavedStateHandle()) } }
    }

    private class SavedStateViewModel(val handle: SavedStateHandle) : ViewModel()
}
