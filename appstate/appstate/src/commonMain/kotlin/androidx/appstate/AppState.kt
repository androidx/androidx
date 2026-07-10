/*
 * Copyright 2026 The Android Open Source Project
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
@file:OptIn(ExperimentalUuidApi::class)

package androidx.appstate

import androidx.appstate.transform.listener
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Class that contains a store that maintains compose state beyond individual Android components.
 *
 * There is no limitation on where these instance should be instantiated as that is left up to the
 * developer.
 */
public class AppState {
    // The map of the compose state being stored inside the AppState
    private val stateStore: SnapshotStateMap<AppStateKey<*>, MutableState<*>> = mutableStateMapOf()

    /**
     * The set of keys that are currently added to this AppState.
     *
     * This can be used in conjunction with the androidx.appstate.transform APIs to provide a
     * mechanism for reacting to state changes.
     *
     * ```
     * val scope = CoroutineScope(context + SupervisorJob())
     * val job = scope.launch {
     *     listen {
     *         val keys = appState.keys
     *         // Do some processing of the keys
     *     }
     * }
     * ```
     */
    public val keys: Set<AppStateKey<*>> = stateStore.keys

    /**
     * Returns the state associated with the given key.
     *
     * @param stateKey the key of the state to retrieve
     * @param defaultValue the value to put if there is no value for the key
     */
    public fun <T> getState(stateKey: AppStateKey<T>, defaultValue: T): State<T> {
        @Suppress("UNCHECKED_CAST")
        return stateStore.getOrPut(stateKey) { mutableStateOf(defaultValue) } as MutableState<T>
    }

    /**
     * Sets the state for the given key.
     *
     * If this is the first time the state is being set a listener is added to allow the given key
     * to be automatically cleared from the map based on the [AppStateKey.autoClearKey] and
     * [AppStateKey.shouldClearState]. For the automatic clearing, [AppStateKey.autoClearKey] must
     * be non-null, the value associated with this key must be non-null, and the
     * [AppStateKey.shouldClearState] condition must return true. AS long as all 3 of those
     * conditions are not met, the listener will not be removed.
     *
     * @param stateKey the key to set a value for
     * @param value the value to set inside the AppState
     */
    public fun <T> setState(stateKey: AppStateKey<T>, value: T) {
        if (!stateStore.contains(stateKey) && stateKey.autoClearKey != null) {
            val key = stateKey.autoClearKey
            val initialTriggerValue = stateStore[key]?.value
            val dispatcher = Dispatchers.Unconfined
            val scope = CoroutineScope(dispatcher + SupervisorJob())
            scope.launch {
                listener(dispatcher) {
                    val currentValue = stateStore[key]?.value
                    if (
                        currentValue != initialTriggerValue &&
                            stateKey.shouldClearState(this@AppState)
                    ) {
                        LaunchedEffect(currentValue) {
                            stateStore.remove(stateKey)
                            scope.cancel()
                        }
                    }
                }
            }
        }
        (getState(stateKey, value) as MutableState<T>).value = value
    }

    /**
     * Retrieves the current state and uses that with the update lambda to set a new state for the
     * specified key.
     *
     * @param stateKey the key to update
     * @param defaultValue the value to use if no state has been set for the key
     * @param update lambda used to update the state using the current state
     */
    public inline fun <T> updateState(stateKey: AppStateKey<T>, defaultValue: T, update: (T) -> T) {
        val currentState = getState(stateKey, defaultValue)
        setState(stateKey, update(currentState.value))
    }
}

/**
 * Open class that should be extended to define specific key-value types to be stored in the
 * AppState to ensure it is type safe.
 *
 * The class T should be the value of the type that that should be retrieved and should also
 * be @Serializable along with the extending class.
 *
 * @param autoClearKey set this if you would like the state associated with this key to be
 *   automatically cleared from the [AppState] when [shouldClearState] is `true`.
 * @param shouldClearState the condition to determine whether the state should be cleared or not.
 */
@Serializable
public open class AppStateKey<T>(
    @Transient public val autoClearKey: AppStateKey<T>? = null,
    @Transient public val shouldClearState: (AppState) -> Boolean = { true },
)
