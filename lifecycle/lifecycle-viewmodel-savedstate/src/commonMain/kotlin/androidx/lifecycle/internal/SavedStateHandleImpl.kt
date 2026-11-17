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

package androidx.lifecycle.internal

import androidx.annotation.MainThread
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateContainer
import androidx.savedstate.SavedStateRegistry.SavedStateProvider
import androidx.savedstate.getValue
import androidx.savedstate.read
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val SAVED_STATE_VALUE_KEY = "androidx.lifecycle.savedstate.value"

internal class SavedStateHandleImpl(val container: SavedStateContainer) {

    constructor(initialState: Map<String, Any?> = emptyMap()) : this(SavedStateContainer()) {
        for ((key, value) in initialState) {
            container[key] = SimpleSavedStateValue(initialValue = unwrapSavedStateValue(value))
        }
    }

    fun asContainer(): SavedStateContainer = container

    fun getOrCreateContainer(key: String): SavedStateContainer {
        val existing = container.get<Any?>(key)
        if (existing is SimpleSavedStateValue<*>) {
            val savedState = existing.value as? SavedState
            container.remove<Any?>(key)
            return container.getOrCreateContainer(key).also { childContainer ->
                savedState?.read {
                    for ((childKey, childValue) in toMap()) {
                        childContainer[childKey] =
                            SimpleSavedStateValue(initialValue = unwrapSavedStateValue(childValue))
                    }
                }
            }
        }
        return container.getOrCreateContainer(key)
    }

    @MainThread
    operator fun contains(key: String): Boolean {
        if (key !in container) return false
        // Registering a SavedStateProvider puts a SavedStateProviderSavedStateValue into the
        // container immediately, before any state has been saved or restored. To preserve the
        // behavior where this only returns true once a value is associated with the key
        // (while still returning true for explicit null values in SimpleSavedStateValue or
        // StateFlowSavedStateValue), we make sure the value is set for the provider to return true.
        val savedStateValue = container.get<Any?>(key)
        return savedStateValue !is SavedStateProviderSavedStateValue ||
            savedStateValue.value != null
    }

    @MainThread
    fun <T> getStateFlow(key: String, initialValue: T): StateFlow<T> {
        return getMutableStateFlow(key, initialValue).asStateFlow()
    }

    @MainThread
    fun <T> getMutableStateFlow(key: String, initialValue: T): MutableStateFlow<T> {
        val savedStateValue =
            when (val existing = container.get<T>(key)) {
                is StateFlowSavedStateValue<T> -> existing
                is SimpleSavedStateValue<T> -> {
                    StateFlowSavedStateValue(existing.value).also {
                        container[key] = it
                    }
                }
                null -> {
                    StateFlowSavedStateValue(initialValue).also {
                        container[key] = it
                    }
                }
                else -> {
                    StateFlowSavedStateValue(get(key) ?: initialValue).also {
                        container[key] = it
                    }
                }
            }
        return savedStateValue.flow
    }

    @MainThread fun keys(): Set<String> = container.keys()

    @MainThread
    operator fun <T> get(key: String): T? {
        return try {
            container.get<T>(key)?.value
        } catch (_: ClassCastException) {
            // Instead of failing on ClassCastException, we remove the value from the
            // SavedStateHandle and return null.
            remove<T>(key)
            null
        }
    }

    @MainThread
    operator fun <T> set(key: String, value: T?) {
        when (val currentSavedStateValue = container.get<T?>(key)) {
            is SimpleSavedStateValue<T?> -> {
                currentSavedStateValue.value = value
            }

            is StateFlowSavedStateValue<T?> -> {
                currentSavedStateValue.value = value
            }

            is SavedStateProviderSavedStateValue -> {
                currentSavedStateValue.value = value as? SavedState
            }

            null -> {
                container[key] = SimpleSavedStateValue(initialValue = value)
            }
        }
    }

    @MainThread
    fun <T> remove(key: String): T? {
        val latestValue = get<T>(key)
        container.remove<T>(key)
        return latestValue
    }

    @MainThread
    fun setSavedStateProvider(key: String, provider: SavedStateProvider) {
        container[key] = SavedStateProviderSavedStateValue(value = get(key), provider = provider)
    }

    @MainThread
    fun clearSavedStateProvider(key: String) {
        val removed by container.remove<SavedState?>(key)
        if (removed != null) {
            set(key, removed)
        }
    }
}

internal expect fun isAcceptableType(value: Any?): Boolean

internal expect fun <T> unwrapSavedStateValue(value: T): T
