/*
 * Copyright (C) 2019 The Android Open Source Project
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

/**
 * A set of collection libraries suited for small data sets which are also optimized for Android,
 * usually by sacrificing performance for efficiency in memory.
 * <ul>
 *     <li>
 *         <b>{@link androidx.collection.ArraySet} / {@link androidx.collection.ArrayMap}</b>
 *         <p>
 *         Implementations of {@link java.util.Set} and {@link java.util.Map}, respectively, which
 *         are backed by an array with lookups done by a binary search.
 *     </li>
 *     <li>
 *         <b>{@link androidx.collection.SparseArrayCompat} /
 *         {@link androidx.collection.LongSparseArray}</b>
 *         <p>
 *         Map-like structures whose keys are {@code int} and {@code long}, respectively, which
 *         prevents boxing compared to a traditional {@link java.util.Map}.
 *     </li>
 *     <li>
 *         <b>{@link androidx.collection.LruCache}</b>
 *         <p>
 *         A map-like cache which keeps frequently-used entries and automatically evicts others.
 *     </li>
 *     <li>
 *         <b>{@link androidx.collection.CircularArray} /
 *         {@link androidx.collection.CircularIntArray}</b>
 *         <p>
 *         List-like structures which can efficiently prepend and append elements.
 *     </li>
 * </ul>
 */
package androidx.collection;
