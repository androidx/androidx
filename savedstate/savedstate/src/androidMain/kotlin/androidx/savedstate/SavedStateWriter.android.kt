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

package androidx.savedstate

import android.os.Parcelable

@Suppress("NOTHING_TO_INLINE")
@JvmInline
actual value class SavedStateWriter actual constructor(actual val source: SavedState) {

    actual inline fun putBoolean(key: String, value: Boolean) {
        source.putBoolean(key, value)
    }

    actual inline fun putDouble(key: String, value: Double) {
        source.putDouble(key, value)
    }

    actual inline fun putFloat(key: String, value: Float) {
        source.putFloat(key, value)
    }

    actual inline fun putInt(key: String, value: Int) {
        source.putInt(key, value)
    }

    actual inline fun putLong(key: String, value: Long) {
        source.putLong(key, value)
    }

    /**
     * Stores an [Parcelable] value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [Parcelable] value to store.
     */
    inline fun <reified T : Parcelable> putParcelable(key: String, value: T) {
        source.putParcelable(key, value)
    }

    actual inline fun putString(key: String, value: String) {
        source.putString(key, value)
    }

    actual inline fun putIntList(key: String, values: List<Int>) {
        source.putIntegerArrayList(key, values.toArrayListUnsafe())
    }

    actual inline fun putStringList(key: String, values: List<String>) {
        source.putStringArrayList(key, values.toArrayListUnsafe())
    }

    /**
     * Stores a list of elements of [Parcelable] associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param values The list of elements to store.
     */
    inline fun <reified T : Parcelable> putParcelableList(key: String, values: List<T>) {
        source.putParcelableArrayList(key, values.toArrayListUnsafe())
    }

    actual inline fun putSavedState(key: String, value: SavedState) {
        source.putBundle(key, value)
    }

    actual inline fun putAll(values: SavedState) {
        source.putAll(values)
    }

    actual inline fun remove(key: String) {
        source.remove(key)
    }

    actual inline fun clear() {
        source.clear()
    }

    @Suppress("UNCHECKED_CAST", "ConcreteCollection")
    @PublishedApi
    internal inline fun <reified T : Any> Collection<*>.toArrayListUnsafe(): ArrayList<T> {
        return if (this is ArrayList<*>) this as ArrayList<T> else ArrayList(this as Collection<T>)
    }
}
