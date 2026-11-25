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
package androidx.appstate.statestore

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.serialization.Serializable

/**
 * Class that contains a store that maintains compose state beyond individual Android components.
 *
 * There is no limitation on where these instance should be instantiated as that is left up to the
 * developer.
 */
public class StateStore {
    // The map of the compose state being stored inside the StateStore
    private val stateStore: SnapshotStateMap<StateStoreKey<*>, MutableState<*>> =
        mutableStateMapOf()

    /**
     * The set of keys that are currently added to this StateStore.
     *
     * This can be used in conjunction with the androidx.stateStore.transform APIs to provide a
     * mechanism for reacting to state changes.
     *
     * ```
     * val scope = CoroutineScope(context + SupervisorJob())
     * val job = scope.launch {
     *     listen {
     *         val keys = stateStore.keys
     *         // Do some processing of the keys
     *     }
     * }
     * ```
     */
    public val keys: Set<StateStoreKey<*>> = stateStore.keys

    /**
     * Returns the state associated with the given key.
     *
     * @param stateKey the key of the state to retrieve
     * @param defaultValue the value to put if there is no value for the key
     */
    public fun <T> getState(
        stateKey: StateStoreKey<T>,
        defaultValue: T = stateKey.defaultValue,
    ): State<T> {
        @Suppress("UNCHECKED_CAST")
        return stateStore.getOrPut(stateKey) { mutableStateOf(defaultValue) } as MutableState<T>
    }

    /**
     * Sets the state for the given key.
     *
     * To automatically reset dependent state when another key changes (for example, clearing form
     * state when a user logs out or completes a flow), observe the trigger key with `listener` and
     * reset each dependent key back to its [StateStoreKey.defaultValue] inside a `LaunchedEffect`:
     * ```kotlin
     * scope.launch {
     *     listener {
     *         val userId by stateStore.getState(UserIdKey)
     *         LaunchedEffect(userId) {
     *             if (userId == null) {
     *                 stateStore.setState(DraftMessageKey, DraftMessageKey.defaultValue)
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * @param stateKey the key to set a value for
     * @param value the value to set inside the StateStore
     */
    public fun <T> setState(stateKey: StateStoreKey<T>, value: T) {
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
    public inline fun <T> updateState(
        stateKey: StateStoreKey<T>,
        defaultValue: T = stateKey.defaultValue,
        update: (T) -> T,
    ) {
        val currentState = getState(stateKey, defaultValue)
        setState(stateKey, update(currentState.value))
    }
}

/**
 * Open class that should be extended to define specific key-value types to be stored in the
 * StateStore to ensure it is type safe.
 *
 * The class T should be the value of the type that that should be retrieved and should also
 * be @Serializable along with the extending class.
 *
 * @param defaultValue an instance of the type [T] that is associated with this key by default.
 */
@Serializable public open class StateStoreKey<T>(public val defaultValue: T)
