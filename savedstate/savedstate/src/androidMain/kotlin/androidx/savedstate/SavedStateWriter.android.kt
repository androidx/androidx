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

@file:JvmName("SavedStateWriterKt")
@file:JvmMultifileClass
@file:Suppress("NOTHING_TO_INLINE")

package androidx.savedstate

import android.os.IBinder
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import java.io.Serializable

@Suppress("ValueClassDefinition")
@JvmInline
public actual value class SavedStateWriter
@PublishedApi
internal actual constructor(
    @PublishedApi internal actual val source: SavedState,
) {

    /**
     * Stores an [IBinder] value associated with the specified key in the [IBinder].
     *
     * @param key The key to associate the value with.
     * @param value The [IBinder] value to store.
     */
    public inline fun putBinder(key: String, value: IBinder) {
        source.putBinder(key, value)
    }

    public actual inline fun putBoolean(key: String, value: Boolean) {
        source.putBoolean(key, value)
    }

    public actual inline fun putChar(key: String, value: Char) {
        source.putChar(key, value)
    }

    public actual inline fun putCharSequence(key: String, value: CharSequence) {
        source.putCharSequence(key, value)
    }

    public actual inline fun putDouble(key: String, value: Double) {
        source.putDouble(key, value)
    }

    public actual inline fun putFloat(key: String, value: Float) {
        source.putFloat(key, value)
    }

    public actual inline fun putInt(key: String, value: Int) {
        source.putInt(key, value)
    }

    public actual inline fun putLong(key: String, value: Long) {
        source.putLong(key, value)
    }

    public actual inline fun putNull(key: String) {
        source.putString(key, null)
    }

    /**
     * Stores an [Parcelable] value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [Parcelable] value to store.
     */
    public inline fun <reified T : Parcelable> putParcelable(key: String, value: T) {
        source.putParcelable(key, value)
    }

    /**
     * Stores an [Serializable] value associated with the specified key in the [Serializable].
     *
     * @param key The key to associate the value with.
     * @param value The [Serializable] value to store.
     */
    public inline fun <reified T : Serializable> putJavaSerializable(key: String, value: T) {
        source.putSerializable(key, value)
    }

    /**
     * Stores an [Size] value associated with the specified key in the [Size].
     *
     * @param key The key to associate the value with.
     * @param value The [Size] value to store.
     */
    public inline fun putSize(key: String, value: Size) {
        source.putSize(key, value)
    }

    /**
     * Stores an [SizeF] value associated with the specified key in the [SizeF].
     *
     * @param key The key to associate the value with.
     * @param value The [SizeF] value to store.
     */
    public inline fun putSizeF(key: String, value: SizeF) {
        source.putSizeF(key, value)
    }

    public actual inline fun putString(key: String, value: String) {
        source.putString(key, value)
    }

    public actual inline fun putIntList(key: String, value: List<Int>) {
        source.putIntegerArrayList(key, value.toArrayListUnsafe())
    }

    public actual inline fun putCharSequenceList(key: String, value: List<CharSequence>) {
        source.putCharSequenceArrayList(key, value.toArrayListUnsafe())
    }

    public actual inline fun putSavedStateList(key: String, value: List<SavedState>) {
        putParcelableList(key, value)
    }

    public actual inline fun putStringList(key: String, value: List<String>) {
        source.putStringArrayList(key, value.toArrayListUnsafe())
    }

    /**
     * Stores a [List] of elements of [Parcelable] associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [List] of elements to store.
     */
    public inline fun <reified T : Parcelable> putParcelableList(key: String, value: List<T>) {
        source.putParcelableArrayList(key, value.toArrayListUnsafe())
    }

    public actual inline fun putBooleanArray(key: String, value: BooleanArray) {
        source.putBooleanArray(key, value)
    }

    public actual inline fun putCharArray(key: String, value: CharArray) {
        source.putCharArray(key, value)
    }

    public actual inline fun putCharSequenceArray(
        key: String,
        @Suppress("ArrayReturn") value: Array<CharSequence>
    ) {
        source.putCharSequenceArray(key, value)
    }

    public actual inline fun putDoubleArray(key: String, value: DoubleArray) {
        source.putDoubleArray(key, value)
    }

    public actual inline fun putFloatArray(key: String, value: FloatArray) {
        source.putFloatArray(key, value)
    }

    public actual inline fun putIntArray(key: String, value: IntArray) {
        source.putIntArray(key, value)
    }

    public actual inline fun putLongArray(key: String, value: LongArray) {
        source.putLongArray(key, value)
    }

    public actual inline fun putSavedStateArray(
        key: String,
        @Suppress("ArrayReturn") value: Array<SavedState>,
    ) {
        putParcelableArray(key, value)
    }

    public actual inline fun putStringArray(key: String, value: Array<String>) {
        source.putStringArray(key, value)
    }

    /**
     * Stores a [Array] of elements of [Parcelable] associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [Array] of elements to store.
     */
    public inline fun <reified T : Parcelable> putParcelableArray(
        key: String,
        @Suppress("ArrayReturn") value: Array<T>
    ) {
        source.putParcelableArray(key, value)
    }

    /**
     * Stores a [SparseArray] of elements of [Parcelable] associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [SparseArray] of elements to store.
     */
    public inline fun <reified T : Parcelable> putSparseParcelableArray(
        key: String,
        value: SparseArray<T>
    ) {
        source.putSparseParcelableArray(key, value)
    }

    public actual inline fun putSavedState(key: String, value: SavedState) {
        source.putBundle(key, value)
    }

    public actual inline fun putAll(from: SavedState) {
        source.putAll(from)
    }

    public actual inline fun remove(key: String) {
        source.remove(key)
    }

    public actual inline fun clear() {
        source.clear()
    }
}

@Suppress("UNCHECKED_CAST", "ConcreteCollection")
@PublishedApi
internal inline fun <reified T : Any> Collection<*>.toArrayListUnsafe(): ArrayList<T> {
    return if (this is ArrayList<*>) this as ArrayList<T> else ArrayList(this as Collection<T>)
}
