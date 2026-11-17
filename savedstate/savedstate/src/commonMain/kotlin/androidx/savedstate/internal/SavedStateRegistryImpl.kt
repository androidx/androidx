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
import androidx.savedstate.SavedStateRegistry.SavedStateProvider

internal class SavedStateRegistryImpl(
    val container: SavedStateContainer = SavedStateContainer(),
    private val onConsumeRestoredStateForKey: (key: String) -> Unit = {},
) : SavedStateProvider, SavedStateRegistry.SavedStateRestorer {

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

    override fun saveState(): SavedState = container.saveState()

    override fun restoreState(savedState: SavedState?) {
        isRestored = true
        container.restoreState(savedState)
    }

    @MainThread
    fun consumeRestoredStateForKey(key: String): SavedState? {
        onConsumeRestoredStateForKey(key)
        val valueHolder = container.get<SavedStateProvider>(key)
        if (valueHolder != null && valueHolder is ProviderSavedStateValue) {
            return valueHolder.savedState.also { valueHolder.savedState = null }
        }
        return container.consumeForKey(key)
    }

    @MainThread
    fun registerSavedStateProvider(key: String, provider: SavedStateProvider) {
        val providerValue = ProviderSavedStateValue(provider)
        if (container.register(key, providerValue)) {
            if (isRestored && !providerValue.isRestored) {
                providerValue.restoreState(null)
            }
        }
    }

    fun getSavedStateProvider(key: String): SavedStateProvider? =
        container.get<SavedStateProvider>(key)?.value

    @MainThread
    fun unregisterSavedStateProvider(key: String) {
        container.remove<SavedStateProvider>(key)
    }
}
