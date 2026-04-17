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

package androidx.navigation3.runtime.result

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** Local for receiving results in a [ResultEventBus] */
public object LocalResultEventBus {
    private val LocalResultEventBus: ProvidableCompositionLocal<ResultEventBus?> =
        compositionLocalOf {
            null
        }

    /** The current [ResultEventBus] */
    public val current: ResultEventBus
        @Composable
        get() = LocalResultEventBus.current ?: error("No ResultEventBus has been provided")

    /**
     * Provides a [ResultEventBus] to the composition
     *
     * @param eventBus the [ResultEventBus] to provide.
     */
    public infix fun provides(eventBus: ResultEventBus): ProvidedValue<ResultEventBus?> {
        return LocalResultEventBus.provides(eventBus)
    }
}

/**
 * An EventBus for passing results between multiple sets of screens.
 *
 * It provides solutions for both event and state based results.
 *
 * For Event results, use a [ResultEffect] to receive all results.
 *
 * For State results, use [conflateAsState] to get only the latest result for a particular key.
 *
 * These results are not saved across configuration changes or process death.
 */
public class ResultEventBus {

    /** Map from the result key to a channel of results. */
    internal val channelMap: SnapshotStateMap<String, Channel<Any?>> = mutableStateMapOf()

    /**
     * Provides a single [State] from the eventBus for the given class type.
     *
     * Developers should use this to retrieve result updates as [State].
     *
     * @param T the type for the result to be returned as [State].
     * @param defaultValue the default value for the state when none is available.
     */
    @Composable
    public inline fun <reified T> conflateAsState(defaultValue: T): State<T> {
        @Suppress("UNCHECKED_CAST")
        return conflateAsState(T::class.toString(), defaultValue)
    }

    /**
     * Provides a single [State] from the eventBus for the given class type.
     *
     * Developers should use this to retrieve result updates as [State].
     *
     * @param T the type of the [State] return for the result.
     * @param resultKey the key for the [State] that will be returned.
     * @param defaultValue the default value for the state when none is available.
     */
    @Composable
    public fun <T> conflateAsState(resultKey: String, defaultValue: T): State<T> {
        val flow = remember(resultKey) { getResultFlow(resultKey) }
        @Suppress("UNCHECKED_CAST")
        return flow.collectAsState(defaultValue) as State<T>
    }

    /**
     * Provides a flow for the given resultKey.
     *
     * @param resultKey the key of the result to retrieve.
     */
    internal fun getResultFlow(resultKey: String): Flow<Any?> {
        if (!channelMap.contains(resultKey)) {
            channelMap[resultKey] =
                Channel(capacity = BUFFERED, onBufferOverflow = BufferOverflow.SUSPEND)
        }
        return channelMap[resultKey]!!.receiveAsFlow()
    }

    /**
     * Sends a result into the channel associated with the given resultKey.
     *
     * @param T the type of the key for the result.
     * @param result the result to send.
     */
    public inline fun <reified T> sendResult(result: T) {
        sendResult(T::class.toString(), result)
    }

    /**
     * Sends a result into the channel associated with the given resultKey.
     *
     * @param resultKey the key for the result that is being sent.
     * @param result the result to send.
     */
    public fun <T> sendResult(resultKey: String, result: T) {
        if (!channelMap.contains(resultKey)) {
            channelMap[resultKey] =
                Channel(capacity = BUFFERED, onBufferOverflow = BufferOverflow.SUSPEND)
        }
        channelMap[resultKey]?.trySend(result)
    }

    /**
     * Removes all results associated with the given type from the store and stops the bus from
     * providing any additional events.
     *
     * @param T the type of the result to be removed.
     */
    public inline fun <reified T> removeResult() {
        removeResult(T::class.toString())
    }

    /**
     * Removes all results associated with the given key from the store and stops the bus from
     * providing any additional events.
     *
     * @param resultKey the key of the result to remove.
     */
    public fun removeResult(resultKey: String) {
        channelMap.remove(resultKey)?.close()
    }
}
