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
import androidx.savedstate.SavedStateRegistry.SavedStateProvider
import androidx.savedstate.savedState
import kotlin.js.JsName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class SavedStateHandleImpl(initialState: Map<String, Any?> = emptyMap()) {

    val regular = initialState.toMutableMap()
    private val providers = mutableMapOf<String, SavedStateProvider>()
    private val flows = mutableMapOf<String, MutableStateFlow<Any?>>()
    val mutableFlows = mutableMapOf<String, MutableStateFlow<Any?>>()

    val savedStateProvider = SavedStateProvider {
        // Synchronize the current value of a MutableStateFlow with the regular values.
        // It copies the original map to avoid re-entrance.
        for ((key, mutableFlow) in mutableFlows.toMap()) {
            set(key, mutableFlow.value)
        }

        // Get the saved state from each SavedStateProvider registered with this
        // SavedStateHandle, iterating through a copy to avoid re-entrance
        for ((key, provider) in providers.toMap()) {
            set(key, provider.saveState())
        }

        savedState(initialState = regular)
    }

    @JsName("fun_savedStateProvider")
    fun savedStateProvider(): SavedStateProvider = savedStateProvider

    @MainThread operator fun contains(key: String): Boolean = key in regular

    @MainThread
    fun <T> getStateFlow(key: String, initialValue: T): StateFlow<T> {
        // If a flow exists we should just return it, and since it is a StateFlow and a value must
        // always be set, we know a value must already be available
        val flow =
            flows.getOrPut(key) {
                // If there is not a value associated with the key, add the initial value,
                // otherwise, use the one we already have.
                if (key !in regular) {
                    regular[key] = initialValue
                }
                MutableStateFlow(regular[key])
            }
        @Suppress("UNCHECKED_CAST")
        return flow.asStateFlow() as StateFlow<T>
    }

    @MainThread
    fun <T> getMutableStateFlow(key: String, initialValue: T): MutableStateFlow<T> {
        // If a flow exists we should just return it, and since it is a StateFlow and a value must
        // always be set, we know a value must already be available
        val flow =
            mutableFlows.getOrPut(key) {
                // If there is not a value associated with the key, add the initial value,
                // otherwise, use the one we already have.
                if (key !in regular) {
                    regular[key] = initialValue
                }
                MutableStateFlow(regular[key])
            }
        @Suppress("UNCHECKED_CAST")
        return flow as MutableStateFlow<T>
    }

    @MainThread fun keys(): Set<String> = regular.keys + providers.keys

    @MainThread
    operator fun <T> get(key: String): T? {
        return try {
            @Suppress("UNCHECKED_CAST")
            (mutableFlows[key]?.value ?: regular[key]) as T?
        } catch (e: ClassCastException) {
            // Instead of failing on ClassCastException, we remove the value from the
            // SavedStateHandle and return null.
            remove<T>(key)
            null
        }
    }

    @MainThread
    operator fun <T> set(key: String, value: T?) {
        regular[key] = value
        flows[key]?.value = value
        mutableFlows[key]?.value = value
    }

    @MainThread
    fun <T> remove(key: String): T? {
        @Suppress("UNCHECKED_CAST") val latestValue = regular.remove(key) as T?
        flows.remove(key)
        mutableFlows.remove(key)
        return latestValue
    }

    @MainThread
    fun setSavedStateProvider(key: String, provider: SavedStateProvider) {
        providers[key] = provider
    }

    @MainThread
    fun clearSavedStateProvider(key: String) {
        providers.remove(key)
    }
}

internal expect fun isAcceptableType(value: Any?): Boolean
