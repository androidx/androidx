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

package androidx.navigation

import android.net.Uri

/**
 * A [NavType] for [Collection] such as arrays, lists, maps.
 *
 * @param T the type of the data that is supported by this NavType
 * @param isNullableAllowed whether the argument of this type can hold a null value
 */
public abstract class CollectionNavType<T>(
    /**
     * Check if an argument with this type can hold a null value.
     *
     * @return Returns true if this type allows null values, false otherwise.
     */
    isNullableAllowed: Boolean
) : NavType<T>(isNullableAllowed) {

    /**
     * Serialize a value of this NavType into a list of String.
     *
     * Each element in the collection should be converted to an individual String element of the
     * returned list.
     *
     * Note: Elements should be encoded with [Uri.encode]
     *
     * @param value a value of this NavType
     * @return List containing encoded and serialized String representation of [value]
     */
    public abstract fun serializeAsValues(value: T): List<String>

    /**
     * Create and return an empty collection of type [T]
     *
     * For example, [T] of type List<MyType> should return emptyList<MyType>().
     */
    public abstract fun emptyCollection(): T
}
