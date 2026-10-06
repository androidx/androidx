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
import androidx.savedstate.SavedStateValue
import androidx.savedstate.read
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val SAVED_STATE_VALUE_KEY = "androidx.lifecycle.savedstate.value"

internal class SavedStateHandleImpl(val container: SavedStateContainer) {

    constructor(initialState: Map<String, Any?> = emptyMap()) : this(SavedStateContainer()) {
        for ((key, value) in initialState) {
            container.putSavedStateValue(
                key,
                SimpleSavedStateValue(initialValue = unwrapSavedStateValue(value)),
            )
        }
    }

    fun asContainer(): SavedStateContainer = container

    fun getOrCreateContainer(key: String): SavedStateContainer {
        val existing = container.getSavedStateValue<Any?, SavedStateValue<Any?>>(key)
        if (existing is SimpleSavedStateValue<*>) {
            val savedState = existing.value as? SavedState
            container.removeSavedStateValue<Any?, SavedStateValue<Any?>>(key)
            return container.getOrCreateContainer(key).also { childContainer ->
                if (savedState != null) {
                    savedState.read {
                        for ((childKey, childValue) in toMap()) {
                            childContainer.putSavedStateValue(
                                childKey,
                                SimpleSavedStateValue(
                                    initialValue = unwrapSavedStateValue(childValue)
                                ),
                            )
                        }
                    }
                }
            }
        }
        return container.getOrCreateContainer(key)
    }

    @MainThread operator fun contains(key: String): Boolean = key in container

    @MainThread
    fun <T> getStateFlow(key: String, initialValue: T): StateFlow<T> {
        return getMutableStateFlow(key, initialValue).asStateFlow()
    }

    @MainThread
    fun <T> getMutableStateFlow(key: String, initialValue: T): MutableStateFlow<T> {
        val existing = container.getSavedStateValue<T, SavedStateValue<T>>(key)
        val savedStateValue =
            when (existing) {
                is StateFlowSavedStateValue<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    existing as StateFlowSavedStateValue<T>
                }
                is SimpleSavedStateValue<*> -> {
                    @Suppress("UNCHECKED_CAST") val existingValue = existing.value as T
                    StateFlowSavedStateValue(existingValue).also {
                        container.putSavedStateValue(key, it)
                    }
                }
                null -> {
                    StateFlowSavedStateValue(initialValue).also {
                        container.putSavedStateValue(key, it)
                    }
                }
                else -> {
                    StateFlowSavedStateValue(get(key) ?: initialValue).also {
                        container.putSavedStateValue(key, it)
                    }
                }
            }
        return savedStateValue.value
    }

    @MainThread fun keys(): Set<String> = container.keys()

    @MainThread
    operator fun <T> get(key: String): T? {
        val savedStateValue = container.getSavedStateValue<T, SavedStateValue<T>>(key)
        return try {
            @Suppress("UNCHECKED_CAST")
            (when (savedStateValue) {
                is StateFlowSavedStateValue<*> -> savedStateValue.value.value
                is SavedStateProviderSavedStateValue -> savedStateValue.savedState
                else -> savedStateValue?.value
            })
                as T?
        } catch (e: ClassCastException) {
            // Instead of failing on ClassCastException, we remove the value from the
            // SavedStateHandle and return null.
            remove<T>(key)
            null
        }
    }

    @MainThread
    operator fun <T> set(key: String, value: T?) {
        val currentSavedStateValue = container.getSavedStateValue<T, SavedStateValue<T>>(key)
        when (currentSavedStateValue) {
            is SimpleSavedStateValue<*> -> {
                @Suppress("UNCHECKED_CAST")
                (currentSavedStateValue as SimpleSavedStateValue<T?>).value = value
            }

            is StateFlowSavedStateValue<*> -> {
                @Suppress("UNCHECKED_CAST")
                (currentSavedStateValue.value as MutableStateFlow<T?>).value = value
            }

            is SavedStateProviderSavedStateValue -> {
                currentSavedStateValue.savedState = value as? SavedState
            }

            null -> {
                container.putSavedStateValue(key, SimpleSavedStateValue(initialValue = value))
            }
        }
    }

    @MainThread
    fun <T> remove(key: String): T? {
        val latestValue = get<T>(key)
        container.removeSavedStateValue<T, SavedStateValue<T>>(key)
        return latestValue
    }

    @MainThread
    fun setSavedStateProvider(key: String, provider: SavedStateProvider) {
        container.putSavedStateValue(
            key,
            SavedStateProviderSavedStateValue(value = provider, savedState = get(key)),
        )
    }

    @MainThread
    fun clearSavedStateProvider(key: String) {
        container.removeSavedStateValue<SavedStateProvider, SavedStateProviderSavedStateValue>(key)
    }
}

internal expect fun isAcceptableType(value: Any?): Boolean

internal expect fun <T> unwrapSavedStateValue(value: T): T
