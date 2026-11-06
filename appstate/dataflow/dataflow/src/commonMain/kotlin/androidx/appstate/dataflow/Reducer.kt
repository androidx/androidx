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

package androidx.appstate.dataflow

import androidx.compose.runtime.Composable

/**
 * Core primitive for data reduction via composition.
 *
 * Reducers compute data of type [S] for a [DataKey] of type [K] via the [reduce] function.
 *
 * @param K the [DataKey] type passed to [reduce]
 * @param S the data type produced by this reducer
 */
public interface Reducer<in K : DataKey<*>, out S : Any> {

    // TODO: Remove once transform also removes the need for initial value
    public val initialValue: S

    /** Composable function that computes the latest data for [key]. */
    @Composable public fun reduce(key: K): S

    /** Factory for creating a [Reducer]. */
    public fun interface Factory<in K : DataKey<*>, out S : Any> {
        /** Creates a [Reducer]. */
        public fun create(): Reducer<K, S>
    }
}
