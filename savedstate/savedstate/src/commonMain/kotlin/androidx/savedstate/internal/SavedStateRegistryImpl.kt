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
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateContainer
import androidx.savedstate.SavedStateRegistry

internal class SavedStateRegistryImpl(
    val container: SavedStateContainer = SavedStateContainer(),
    private val onConsumeRestoredStateForKey: (key: String) -> Unit = {},
) : SavedStateRegistry.SavedStateProvider, SavedStateRegistry.SavedStateRestorer {

    constructor(
        initialState: SavedState?,
        onConsumeRestoredStateForKey: (key: String) -> Unit = {},
    ) : this(SavedStateContainer(), onConsumeRestoredStateForKey) {
        if (initialState != null) {
            container.restoreState(initialState)
        }
    }

    @get:MainThread
    var isRestored = false
        private set

    internal var isAllowingSavingState: Boolean = true

    fun asContainer(): SavedStateContainer = container

    fun getOrCreateContainer(key: String): SavedStateContainer = container.getOrCreateContainer(key)

    override fun saveState(): SavedState {
        return container.saveState()
    }

    override fun restoreState(savedState: SavedState?) {
        isRestored = true
        container.restoreState(savedState)
    }

    @MainThread
    fun consumeRestoredStateForKey(key: String): SavedState? {
        onConsumeRestoredStateForKey(key)
        val savedStateValue =
            container.getSavedStateValue<
                SavedStateRegistry.SavedStateProvider,
                ProviderSavedStateValue,
            >(
                key
            )
        if (savedStateValue != null) {
            return savedStateValue.savedState.also {
                savedStateValue.savedState = null
            }
        }
        return container.consumeForKey(key)
    }

    @MainThread
    fun registerSavedStateProvider(key: String, provider: SavedStateRegistry.SavedStateProvider) {
        val providerValue = ProviderSavedStateValue(provider)
        if (container.registerSavedStateValue(key, providerValue)) {
            if (isRestored && !providerValue.isRestored) {
                providerValue.restoreState(null)
            }
        }
    }

    fun getSavedStateProvider(key: String): SavedStateRegistry.SavedStateProvider? {
        return container
            .getSavedStateValue<
                SavedStateRegistry.SavedStateProvider,
                ProviderSavedStateValue,
            >(
                key
            )
            ?.value
    }

    @MainThread
    fun unregisterSavedStateProvider(key: String) {
        container.removeSavedStateValue<
            SavedStateRegistry.SavedStateProvider,
            ProviderSavedStateValue,
        >(
            key
        )
    }
}
