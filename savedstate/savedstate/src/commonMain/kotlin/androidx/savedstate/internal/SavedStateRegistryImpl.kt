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
import androidx.collection.mutableScatterMapOf
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateProvider
import androidx.savedstate.SavedStateRestorer
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.write

internal class SavedStateRegistryImpl(
    initialState: SavedState? = null,
    private val onConsumeRestoredStateForKey: (key: String) -> Unit = {},
) : SavedStateProvider, SavedStateRestorer {

    private val lock = SynchronizedObject()
    private val keyToProviders = mutableScatterMapOf<String, SavedStateProvider>()
    private var restoredState: SavedState? = initialState

    @get:MainThread
    var isRestored = false
        private set

    internal var isAllowingSavingState: Boolean = true

    override fun saveState(): SavedState {
        return savedState {
            // Keep unconsumed state from previous restore.
            restoredState?.let { putAll(from = it) }
            synchronized(lock) {
                // Collect state from all registered providers.
                keyToProviders.forEach { key, provider -> putSavedState(key, provider.saveState()) }
            }
        }
    }

    override fun restoreState(savedState: SavedState?) {
        // Merge incoming state with existing restored state so prior state is not lost.
        val mergedState =
            savedState(initialState = savedState ?: savedState()) {
                restoredState?.let { putAll(from = it) }
            }
        restoredState = mergedState
        isRestored = true

        synchronized(lock) {
            keyToProviders.forEach { key, provider ->
                // Automatically restore components that implement SavedStateRestorer.
                if (provider is SavedStateRestorer && isRestored) {
                    provider.restoreState(savedState = consumeRestoredStateForKey(key))
                }
            }
        }
    }

    @MainThread
    fun consumeRestoredStateForKey(key: String): SavedState? {
        onConsumeRestoredStateForKey(key)
        val state = restoredState ?: return null

        val consumed = state.read { getSavedStateOrNull(key) }
        if (consumed != null) {
            state.write { remove(key) }
            if (state.read { isEmpty() }) {
                restoredState = null
            }
        }

        return consumed
    }

    @MainThread
    fun registerSavedStateProvider(key: String, provider: SavedStateProvider) {
        synchronized(lock) {
            val oldProvider = keyToProviders.put(key, provider)

            // Allow idempotent re-registration of the exact same provider instance.
            if (oldProvider === provider) {
                return@synchronized
            }

            // Prevent key collisions between different provider instances.
            require(oldProvider == null) {
                "SavedStateProvider with key '$key' already registered. Existing instance: '$oldProvider'. New instance: '$provider'."
            }

            // If registry is restored, restore state immediately for late registration.
            if (provider is SavedStateRestorer && isRestored) {
                provider.restoreState(savedState = consumeRestoredStateForKey(key))
            }
        }
    }

    fun getSavedStateProvider(key: String): SavedStateProvider? {
        return synchronized(lock) { keyToProviders[key] }
    }

    @MainThread
    fun unregisterSavedStateProvider(key: String) {
        synchronized(lock) { keyToProviders.remove(key) }
    }
}
