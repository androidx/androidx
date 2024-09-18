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

package androidx.savedstate.internal

import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistry.SavedStateProvider
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.write

internal class SavedStateRegistryImpl(
    private val owner: SavedStateRegistryOwner,
    internal val onAttach: () -> Unit = {},
) {

    private val lock = SynchronizedObject()
    private val keyToProviders = mutableMapOf<String, SavedStateProvider>()
    private var attached = false
    private var restoredState: SavedState? = null

    @get:MainThread
    var isRestored = false
        private set

    internal var isAllowingSavingState = true

    @MainThread
    fun consumeRestoredStateForKey(key: String): SavedState? {
        check(isRestored) {
            "You can 'consumeRestoredStateForKey' only after the corresponding component has " +
                "moved to the 'CREATED' state"
        }

        val state = restoredState ?: return null

        val consumed = state.read { if (contains(key)) getSavedState(key) else null }
        state.write { remove(key) }
        if (state.read { isEmpty() }) {
            restoredState = null
        }

        return consumed
    }

    @MainThread
    fun registerSavedStateProvider(key: String, provider: SavedStateProvider) {
        synchronized(lock) {
            require(key !in keyToProviders) {
                "SavedStateProvider with the given key is already registered"
            }
            keyToProviders[key] = provider
        }
    }

    fun getSavedStateProvider(key: String): SavedStateProvider? {
        return synchronized(lock) {
            keyToProviders.firstNotNullOfOrNull { (k, provider) ->
                if (k == key) provider else null
            }
        }
    }

    @MainThread
    fun unregisterSavedStateProvider(key: String) {
        synchronized(lock) { keyToProviders.remove(key) }
    }

    @MainThread
    fun performAttach() {
        check(owner.lifecycle.currentState == Lifecycle.State.INITIALIZED) {
            "Restarter must be created only during owner's initialization stage"
        }
        check(!attached) { "SavedStateRegistry was already attached." }

        onAttach()
        owner.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) {
                    isAllowingSavingState = true
                } else if (event == Lifecycle.Event.ON_STOP) {
                    isAllowingSavingState = false
                }
            }
        )
        attached = true
    }

    /** An interface for an owner of this [SavedStateRegistry] to restore saved state. */
    @MainThread
    internal fun performRestore(savedState: SavedState?) {
        // To support backward compatibility with libraries that do not explicitly
        // call performAttach(), we make sure that work is done here
        if (!attached) {
            performAttach()
        }
        check(!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            "performRestore cannot be called when owner is ${owner.lifecycle.currentState}"
        }
        check(!isRestored) { "SavedStateRegistry was already restored." }

        restoredState =
            savedState?.read {
                if (contains(SAVED_COMPONENTS_KEY)) getSavedState(SAVED_COMPONENTS_KEY) else null
            }
        isRestored = true
    }

    /**
     * An interface for an owner of this [SavedStateRegistry] to perform state saving, it will call
     * all registered providers and merge with unconsumed state.
     *
     * @param outBundle SavedState in which to place a saved state
     */
    @MainThread
    internal fun performSave(outBundle: SavedState) {
        val inState = savedState {
            restoredState?.let { putAll(it) }
            synchronized(lock) {
                for ((key, provider) in keyToProviders) {
                    putSavedState(key, provider.saveState())
                }
            }
        }

        if (inState.read { !isEmpty() }) {
            outBundle.write { putSavedState(SAVED_COMPONENTS_KEY, inState) }
        }
    }

    private companion object {
        private const val SAVED_COMPONENTS_KEY =
            "androidx.lifecycle.BundlableSavedStateRegistry.key"
    }
}
