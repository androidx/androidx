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

/**
 * Key that identifies data of type [S] and any runtime arguments needed to produce it in a
 * [DataflowRegistry].
 *
 * Implementations must properly implement [equals] and [hashCode] so that active reducers in a
 * [DataflowRegistry] can be matched and deduplicated. Keys are commonly declared as:
 * - A `data object` or `companion object` for parameter-less reducers:
 * ```
 * data class CounterData(val count: Int) {
 *     companion object : DataKey<CounterData>
 * }
 * data object UsernameKey : DataKey<String>
 * ```
 * - A `data class` for reducers that accept runtime parameters:
 * ```
 * data class AlbumDetailKey(val albumId: Int) : DataKey<AlbumDetailData>
 * ```
 *
 * @param S The data type produced by the [Reducer] associated with this key.
 */
public interface DataKey<out S : Any>
