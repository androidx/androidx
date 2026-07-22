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

@file:JvmName("SavedStateHandleSupport")

package androidx.lifecycle

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModelProvider.Companion.VIEW_MODEL_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry.SavedStateProvider
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.write
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName

/**
 * Enables support for [SavedStateHandle] in a component.
 *
 * After calling this method, [CreationExtras.createSavedStateHandle] can be invoked on
 * [CreationExtras] containing the [SavedStateRegistryOwner] and [ViewModelStoreOwner].
 *
 * This must be called while the component is in the [Lifecycle.State.INITIALIZED] or
 * [Lifecycle.State.CREATED] state, and before requesting any [ViewModel] that requires a
 * [SavedStateHandle].
 */
@MainThread
public fun <T> T.enableSavedStateHandles()
    where T : SavedStateRegistryOwner, T : ViewModelStoreOwner {
    val currentState = lifecycle.currentState
    require(
        currentState == Lifecycle.State.INITIALIZED || currentState == Lifecycle.State.CREATED
    ) {
        "Failed to enable `SavedStateHandle` for `$this`. The `Lifecycle.State` must be " +
            "`INITIALIZED` or `CREATED`, but was `$currentState`. You must call " +
            "`enableSavedStateHandles()` before the `Lifecycle.State` moves to `STARTED`."
    }

    // Register the provider to save SavedStateHandles if it is not already registered.
    if (savedStateRegistry.getSavedStateProvider(SAVED_STATE_KEY) == null) {
        val controller = SavedStateHandleSupportController(this, this)
        savedStateRegistry.registerSavedStateProvider(SAVED_STATE_KEY, controller)
        lifecycle.addObserver(controller)
    }
}

/**
 * Creates a [SavedStateHandle] for use in your [ViewModel]s.
 *
 * This function requires a call to [enableSavedStateHandles] during component initialization.
 * Modern Jetpack components (such as `ComponentActivity`, `Fragment`, and `NavBackStackEntry`)
 * perform this call automatically.
 *
 * The [CreationExtras] must contain [SAVED_STATE_REGISTRY_OWNER_KEY], [VIEW_MODEL_STORE_OWNER_KEY],
 * and [VIEW_MODEL_KEY].
 *
 * @return new [SavedStateHandle] instance
 * @throws IllegalArgumentException If the [CreationExtras] is missing any of the required keys:
 *   [VIEW_MODEL_STORE_OWNER_KEY], [SAVED_STATE_REGISTRY_OWNER_KEY], or [VIEW_MODEL_KEY].
 */
@MainThread
public fun CreationExtras.createSavedStateHandle(): SavedStateHandle {
    val savedStateRegistryOwner =
        requireNotNull(this[SAVED_STATE_REGISTRY_OWNER_KEY]) {
            "CreationExtras must have a value by `SAVED_STATE_REGISTRY_OWNER_KEY`"
        }
    val key =
        requireNotNull(this[VIEW_MODEL_KEY]) {
            "CreationExtras must have a value by `VIEW_MODEL_KEY`"
        }
    requireNotNull(this[VIEW_MODEL_STORE_OWNER_KEY]) {
        "CreationExtras must have a value by `VIEW_MODEL_STORE_OWNER_KEY`"
    }

    val controller =
        savedStateRegistryOwner.savedStateRegistry.getSavedStateProvider(SAVED_STATE_KEY)
    check(controller is SavedStateHandleSupportController) {
        "enableSavedStateHandles() wasn't called prior to createSavedStateHandle() call"
    }

    // Reuse the previously created SavedStateHandle if it exists in the ViewModel.
    // Otherwise, create a new instance with any restored state.
    return controller.stateHolder.handles.getOrPut(key) {
        SavedStateHandle.createHandle(
            restoredState = controller.consumeRestoredStateForKey(key),
            defaultState = this[DEFAULT_ARGS_KEY],
        )
    }
}

/**
 * A [CreationExtras.Key] to retrieve the [SavedStateRegistryOwner] associated with the [ViewModel]
 * being created.
 */
@JvmField
public val SAVED_STATE_REGISTRY_OWNER_KEY: CreationExtras.Key<SavedStateRegistryOwner> =
    CreationExtras.Key<SavedStateRegistryOwner>()

/**
 * A [CreationExtras.Key] to retrieve the [ViewModelStoreOwner] associated with the [ViewModel]
 * being created.
 */
@JvmField
public val VIEW_MODEL_STORE_OWNER_KEY: CreationExtras.Key<ViewModelStoreOwner> =
    CreationExtras.Key<ViewModelStoreOwner>()

/**
 * A [CreationExtras.Key] to retrieve the default [SavedState] arguments to be passed to the
 * [SavedStateHandle].
 */
@JvmField
public val DEFAULT_ARGS_KEY: CreationExtras.Key<SavedState> = CreationExtras.Key<SavedState>()

/**
 * A key to retrieve the [SavedStateProvider] responsible for orchestrating the restoration,
 * caching, and serialization of all [SavedStateHandle] instances associated with a component.
 */
internal const val SAVED_STATE_KEY = "androidx.lifecycle.internal.SavedStateHandlesProvider"

/**
 * A [SavedStateProvider] and [LifecycleEventObserver] responsible for orchestrating the
 * restoration, caching, and serialization of all [SavedStateHandle] instances associated with a
 * component.
 */
private class SavedStateHandleSupportController(
    private val savedStateRegistryOwner: SavedStateRegistryOwner,
    private val viewModelStoreOwner: ViewModelStoreOwner,
) :
    SavedStateProvider,
    LifecycleEventObserver,
    SavedStateRegistryOwner by savedStateRegistryOwner,
    ViewModelStoreOwner by viewModelStoreOwner {

    private var isRestored = false
    private var restoredState: SavedState? = null

    val stateHolder: StateHolder by
        ViewModelLazy(
            viewModelClass = StateHolder::class,
            storeProducer = { viewModelStore },
            factoryProducer = { viewModelFactory { initializer { StateHolder() } } },
        )

    override fun saveState(): SavedState {
        return savedState {
            // Retain restored state for any ViewModels that have not been recreated yet.
            restoredState?.let { putAll(it) }

            // Prefer the state of active ViewModels over the restored state.
            for ((key, handle) in stateHolder.handles) {
                val savedState = handle.savedStateProvider().saveState()
                if (savedState.read { !isEmpty() }) {
                    putSavedState(key, savedState)
                }
            }

            // Allow restoring state a second time after saving.
            isRestored = false
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        check(event == Lifecycle.Event.ON_CREATE) { "Next event must be ON_CREATE, it was $event" }
        source.lifecycle.removeObserver(this)
        performRestore()
    }

    /** Restore the state from the SavedStateRegistry if it hasn't already been restored. */
    fun performRestore() {
        if (!isRestored) {
            val newState = savedStateRegistry.consumeRestoredStateForKey(SAVED_STATE_KEY)
            restoredState = savedState {
                restoredState?.let { putAll(it) }
                newState?.let { putAll(it) }
            }
            isRestored = true

            // Eagerly evaluate the ViewModel provider. This ensures we can still retrieve the VM
            // during state saving even if the lifecycle reaches DESTROYED.
            stateHolder
        }
    }

    /** Restore the state associated with a particular SavedStateHandle, identified by its [key] */
    fun consumeRestoredStateForKey(key: String): SavedState? {
        performRestore()
        val state = restoredState ?: return null
        if (state.read { !contains(key) }) return null

        val result = state.read { getSavedStateOrNull(key) ?: savedState() }
        state.write { remove(key) }
        if (state.read { isEmpty() }) {
            this.restoredState = null
        }

        return result
    }

    class StateHolder : ViewModel() {
        val handles = mutableMapOf<String, SavedStateHandle>()
    }
}
