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

package androidx.lifecycle

import androidx.lifecycle.SavedStateHandleController.Companion.SAVED_STATE_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistry.SavedStateProvider
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.write

/**
 * Orchestrates state restoration, persistence, and creation of [SavedStateHandle]s.
 *
 * This controller registers itself as a [SavedStateProvider] to save state, and observes the
 * component's lifecycle to restore state at [Lifecycle.Event.ON_CREATE].
 *
 * **Architectural Pathways:** Active [SavedStateHandle]s are managed through one of two primary
 * pathways. In the modern pathway, the component calls [enableSavedStateHandles] to register the
 * controller under [SAVED_STATE_KEY] without creating handles. Later during [ViewModel] creation,
 * [CreationExtras.createSavedStateHandle] retrieves the controller and calls [getOrCreateHandle] to
 * build the handle. In the legacy pathway, factories like [SavedStateViewModelFactory] receive the
 * [SavedStateRegistryOwner] directly and call [getOrPut] to construct and register the controller,
 * then invoke [getOrCreateHandle].
 *
 * **State Persistence and Reattachment:** All active handles are retained in [StateHolder] (a
 * [ViewModel] subclass) to survive configuration changes. Because the [SavedStateRegistry] is
 * recreated from scratch on rotation, the controller reattaches itself to the new registry when
 * [getOrPut] is called during [ViewModel] instantiation on the recreated host, re-registering under
 * [SAVED_STATE_KEY]. During state saving, the controller collects handle states and serializes them
 * into a nested [SavedState] map. Component relationships are visualized below:
 * ```
 *       +---------------------------------+      +---------------------------------+
 *       |     SavedStateRegistryOwner     |      |       ViewModelStoreOwner       |
 *       +----------------+----------------+      +----------------+----------------+
 *                        |                                        |
 *                        v (registers under SAVED_STATE_KEY)      v (scopes/hosts)
 *       +----------------+----------------+                       |
 *       |    SavedStateHandleController   |<----------------------+
 *       +----------------+----------------+
 *                        |
 *                        v (holds lazily)
 *       +----------------+----------------+
 *       |           StateHolder           |  <--- Survives rotation (ViewModel)
 *       +----------------+----------------+
 *                        |
 *                        +=======> [ViewModel Key A] ---> SavedStateHandle A
 *                        |
 *                        +=======> [ViewModel Key B] ---> SavedStateHandle B
 * ```
 *
 * @param savedStateRegistryOwner registry owner to associate with
 * @param viewModelStoreOwner store owner to store handles in
 */
internal class SavedStateHandleController
private constructor(
    savedStateRegistryOwner: SavedStateRegistryOwner,
    viewModelStoreOwner: ViewModelStoreOwner,
) :
    SavedStateProvider,
    SavedStateRegistryOwner by savedStateRegistryOwner,
    ViewModelStoreOwner by viewModelStoreOwner {

    private var _stateHolder: StateHolder? = null

    private var restoredState: SavedState? = null
    private var isRestored = false

    /** Holds active [SavedStateHandle] instances. */
    private val stateHolder: StateHolder
        get() {
            // Invalidate cached reference when cleared. Forces creation of a new StateHolder
            // to prevent stale handle retention and memory leaks.
            if (_stateHolder?.isCleared == true) {
                _stateHolder = null
            }

            // Recreate StateHolder to reset state and resolve updated default args on new query.
            if (_stateHolder == null) {
                val factory = viewModelFactory { initializer { StateHolder() } }
                val provider = ViewModelProvider.create(owner = this, factory)

                // Cache instance in backing field. Prevents ViewModelProvider.get() from throwing
                // an exception when controller is accessed while Lifecycle is DESTROYED.
                _stateHolder = provider.get<StateHolder>()
            }

            return _stateHolder!!
        }

    /**
     * Saves the state of all managed [SavedStateHandle]s.
     *
     * @return new [SavedState] containing the saved states
     */
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

    /** Restores the state from the [SavedStateRegistry] if not already restored. */
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

    /**
     * Consumes and returns the restored state for the given [key].
     *
     * @param key identifier for the restored state
     * @return restored [SavedState], or `null` if none exists
     */
    fun consumeRestoredStateForKey(key: String): SavedState? {
        performRestore()
        val state = restoredState ?: return null
        if (!state.read { contains(key) }) return null

        val result = state.read { getSavedStateOrNull(key) ?: savedState() }
        state.write { remove(key) }
        if (state.read { isEmpty() }) {
            restoredState = null
        }
        return result
    }

    /**
     * Creates a new [SavedStateHandle] or returns the existing one for the given [key].
     *
     * @param key unique identifier for the [SavedStateHandle]
     * @param defaultArgs default state to initialize the handle with
     * @return new or existing [SavedStateHandle]
     */
    internal fun getOrCreateHandle(key: String, defaultArgs: SavedState? = null): SavedStateHandle {
        return stateHolder.handles.getOrPut(key) {
            SavedStateHandle.createHandle(
                restoredState = consumeRestoredStateForKey(key),
                defaultState = defaultArgs,
            )
        }
    }

    /**
     * Retains active [SavedStateHandle]s across configuration changes.
     *
     * Inherits from [ViewModel] to ensure handles survive rotation and are correctly cleared when
     * the host scope is destroyed.
     */
    private class StateHolder : ViewModel() {
        val handles = mutableMapOf<String, SavedStateHandle>()
        var isCleared = false
            private set

        override fun onCleared() {
            isCleared = true
        }
    }

    companion object {

        /** Key used to register and retrieve the controller provider. */
        private const val SAVED_STATE_KEY = "androidx.lifecycle.internal.SavedStateHandlesProvider"

        /**
         * Returns the registered [SavedStateHandleController] for the [owner], or `null` if none.
         *
         * @param owner component registry owner to query
         * @return existing controller, or `null` if not registered
         */
        fun getOrNull(owner: SavedStateRegistryOwner): SavedStateHandleController? {
            val provider =
                owner.savedStateRegistry.getSavedStateProvider(SAVED_STATE_KEY)
                    // Null indicates saved state handles are not enabled yet, which is valid.
                    ?: return null

            // Registered provider must be our controller. Incorrect type indicates key conflict.
            check(provider is SavedStateHandleController) {
                "The registered SavedStateProvider under SAVED_STATE_KEY is not of type " +
                    "SavedStateHandleController"
            }
            return provider
        }

        /**
         * Returns the existing [SavedStateHandleController] or creates and registers a new one.
         *
         * @param owner component registry owner
         * @param viewModelStoreOwner store owner to save handles
         * @return controller instance
         */
        internal fun getOrCreate(
            owner: SavedStateRegistryOwner,
            viewModelStoreOwner: ViewModelStoreOwner,
        ): SavedStateHandleController {
            var controller = getOrNull(owner)
            if (controller == null) {
                controller = SavedStateHandleController(owner, viewModelStoreOwner)
                owner.savedStateRegistry.registerSavedStateProvider(SAVED_STATE_KEY, controller)

                // If the Lifecycle is already CREATED, restore the state immediately. Otherwise,
                // register an observer to restore the state when the Lifecycle reaches CREATED.
                if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                    controller.performRestore()
                } else {
                    owner.lifecycle.addObserver { source, event ->
                        check(event == Lifecycle.Event.ON_CREATE) {
                            "Next event must be ON_CREATE, it was $event"
                        }
                        source.lifecycle.removeObserver(this)
                        controller.performRestore()
                    }
                }
            }
            return controller
        }
    }
}
