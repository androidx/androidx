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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlin.collections.getOrPut
import kotlin.coroutines.CoroutineContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
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
    private val stateStore: MutableMap<AppStateKey<*>, MutableState<*>> = mutableMapOf()

    // Map of AppStateTokens to CoroutineScopes so that we maintain the running listeners
    private val appStateListeners = mutableMapOf<AppStateToken, CoroutineScope>()

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
     * [AppStateKey.predicate].
     *
     * @param stateKey the key to set a value for
     * @param value the value to set inside the AppState
     */
    public fun <T> setState(stateKey: AppStateKey<T>, value: T) {
        if (!stateStore.contains(stateKey) && stateKey.autoClearKey != null) {
            addAppStateListener { map ->
                val key = stateKey.autoClearKey
                val currentValue = map[key]?.value
                val initialValue = remember { currentValue }
                if (currentValue != initialValue && stateKey.predicate(this@AppState)) {
                    LaunchedEffect(currentValue) {
                        stateStore.remove(stateKey)
                        removeAppStateListener(this@addAppStateListener)
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
    public fun <T> updateState(stateKey: AppStateKey<T>, defaultValue: T, update: (T) -> T) {
        val currentState = getState(stateKey, defaultValue)
        setState(stateKey, update(currentState.value))
    }

    /**
     * Adds a listener that can be used to listen for particular [State] from this AppState.
     *
     * @param context the context that should be used to execute the listener
     * @param listener composable lambda that should be used to observe specific states
     * @return an AppStateToken that should be used with [removeAppStateListener]
     */
    public fun addAppStateListener(
        context: CoroutineContext = Dispatchers.Default,
        listener: @Composable AppStateToken.(Map<AppStateKey<*>, State<*>>) -> Unit,
    ): AppStateToken {
        // TODO: implement listener with transform
        return AppStateToken()
    }

    /**
     * Removes the listener associated with the given token.
     *
     * @param token the token of the listener to remove.
     */
    public fun removeAppStateListener(token: AppStateToken) {
        val scope = appStateListeners.remove(token)
        scope?.cancel()
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
 *   automatically cleared.
 * @param predicate the condition to determine whether the state should be cleared or not.
 */
@Serializable
public open class AppStateKey<T>(
    @Transient public val autoClearKey: Any? = null,
    @Transient public val predicate: (AppState) -> Boolean = { true },
)

/**
 * Token that should be returned to callers of [AppState.addAppStateListener] and passed to
 * [AppState.removeAppStateListener] to stop the listener
 */
public class AppStateToken internal constructor() {
    private val id: String = Uuid.random().toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as AppStateToken
        return id == other.id
    }

    override fun hashCode(): Int {
        return 31 * id.hashCode()
    }

    override fun toString(): String {
        return "AppStateToken(id='$id')"
    }
}
