/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.datastore.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

/** This is where a [DataStoreImpl] instance keeps its actual data. */
internal class DataStoreInMemoryCache<T> {
    @Suppress("UNCHECKED_CAST")
    private val cachedValue: MutableStateFlow<State<T>> =
        MutableStateFlow(UnInitialized as State<T>)

    val currentState: State<T>
        get() = cachedValue.value

    val flow: Flow<State<T>>
        get() = cachedValue

    /**
     * Tries to update the current value if an only if the new given state's version is equal to or
     * higher than the current state.
     */
    fun tryUpdate(newState: State<T>): State<T> {
        val updated =
            cachedValue.updateAndGet { cached ->
                when (cached) {
                    is ReadException<T>,
                    UnInitialized -> {
                        // for ReadException and UnInitialized; we can always accept the new state.
                        // this is especially useful when multiple reads fail so each can
                        // send their own exception.
                        newState
                    }
                    is Data<T> -> {
                        // When overriding Data, only accept newer values.
                        // Note that, when we have Data, we'll only every try to read again
                        // if version changed, and it will arrive here as either new data or
                        // new error with its new version.
                        //
                        // If a read happens in parallel to a write ("dirty read"), we will not
                        // update
                        // the cache here but make it local in the flow that does the dirty read. In
                        // this cache we guarantee the version matches with the data because only
                        // reads
                        // that have file lock can set the cache.
                        if (newState.version > cached.version) {
                            newState
                        } else {
                            cached
                        }
                    }
                    is Final<T> -> {
                        // no going back from final state
                        cached
                    }
                }
            }
        return updated
    }
}
