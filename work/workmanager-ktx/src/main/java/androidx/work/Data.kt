/*
 * Copyright 2018 The Android Open Source Project
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

// Always inline ktx extension methods unless we have additional call site costs.
@file:Suppress("NOTHING_TO_INLINE")

package androidx.work

/**
 * Converts a list of pairs to a [Data] object.
 *
 * If multiple pairs have the same key, the resulting map will contain the value
 * from the last of those pairs.
 *
 * Entries of the map are iterated in the order they were specified.
 */
public inline fun workDataOf(vararg pairs: Pair<String, Any?>): Data {
    val dataBuilder = Data.Builder()
    for (pair in pairs) {
        dataBuilder.put(pair.first, pair.second)
    }
    return dataBuilder.build()
}

/**
 * Returns true if the instance of [Data] has a value corresponding to the given [key] with an
 * expected type [T].
 */
public inline fun <reified T : Any> Data.hasKeyWithValueOfType(key: String): Boolean =
    hasKeyWithValueOfType(key, T::class.java)
