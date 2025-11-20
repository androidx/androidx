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

@file:JvmName("SavedStateReaderKt")
@file:JvmMultifileClass

package androidx.savedstate

import android.os.IBinder
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import androidx.core.os.BundleCompat.getParcelable
import androidx.core.os.BundleCompat.getParcelableArray
import androidx.core.os.BundleCompat.getParcelableArrayList
import androidx.core.os.BundleCompat.getSerializable
import androidx.core.os.BundleCompat.getSparseParcelableArray
import java.io.Serializable
import kotlin.reflect.KClass

@Suppress("ValueClassDefinition")
@JvmInline
public actual value class SavedStateReader
@PublishedApi
internal actual constructor(private actual val source: SavedState) {

    /**
     * Retrieves an [IBinder] value associated with the specified [key], or throws an
     * [IllegalArgumentException] if this [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException If associated value is `null`.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getBinder(key: String): IBinder {
        return source.getBinder(key) ?: keyOrValueNotFoundError(key)
    }

    /**
     * Retrieves a [IBinder] value associated with the specified [key], or `null` if this
     * [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key], or `null` if no valid value is found.
     */
    public fun getBinderOrNull(key: String): IBinder? {
        return source.getBinder(key)
    }

    public actual fun getBoolean(key: String): Boolean {
        val result = source.getBoolean(key, false)
        if (result == false) {
            val reference = source.getBoolean(key, true)
            if (reference == true) {
                keyOrValueNotFoundError(key)
            }
        }
        return result
    }

    @Suppress("AutoBoxing")
    public actual fun getBooleanOrNull(key: String): Boolean? {
        val result = source.getBoolean(key, false)
        if (result == false) {
            val reference = source.getBoolean(key, true) == true
            if (reference == true) {
                return null
            }
        }
        return result
    }

    public actual fun getChar(key: String): Char {
        val result = source.getChar(key, Char.MIN_VALUE)
        if (result == Char.MIN_VALUE) {
            val reference = source.getChar(key, Char.MAX_VALUE)
            if (reference == Char.MAX_VALUE) {
                keyOrValueNotFoundError(key)
            }
        }
        return result
    }

    public actual fun getCharOrNull(key: String): Char? {
        val result = source.getChar(key, Char.MIN_VALUE)
        if (result == Char.MIN_VALUE) {
            val reference = source.getChar(key, Char.MAX_VALUE)
            if (reference == Char.MAX_VALUE) {
                return null
            }
        }
        return result
    }

    public actual fun getCharSequence(key: String): CharSequence {
        return source.getCharSequence(key) ?: keyOrValueNotFoundError(key)
    }

    public actual fun getCharSequenceOrNull(key: String): CharSequence? {
        return source.getCharSequence(key)
    }

    public actual fun getDouble(key: String): Double {
        val result = source.getDouble(key, Double.MIN_VALUE)
        if (result == Double.MIN_VALUE) {
            val reference = source.getDouble(key, Double.MAX_VALUE)
            if (reference == Double.MAX_VALUE) {
                keyOrValueNotFoundError(key)
            }
        }
        return result
    }

    @Suppress("AutoBoxing")
    public actual fun getDoubleOrNull(key: String): Double? {
        val result = source.getDouble(key, Double.MIN_VALUE)
        if (result == Double.MIN_VALUE) {
            val reference = source.getDouble(key, Double.MAX_VALUE)
            if (reference == Double.MAX_VALUE) {
                return null
            }
        }
        return result
    }

    public actual fun getFloat(key: String): Float {
        val result = source.getFloat(key, Float.MIN_VALUE)
        if (result == Float.MIN_VALUE) {
            val reference = source.getFloat(key, Float.MAX_VALUE)
            if (reference == Float.MAX_VALUE) {
                keyOrValueNotFoundError(key)
            }
        }
        return result
    }

    @Suppress("AutoBoxing")
    public actual fun getFloatOrNull(key: String): Float? {
        val result = source.getFloat(key, Float.MIN_VALUE)
        if (result == Float.MIN_VALUE) {
            val reference = source.getFloat(key, Float.MAX_VALUE)
            if (reference == Float.MAX_VALUE) {
                return null
            }
        }
        return result
    }

    public actual fun getInt(key: String): Int {
        val result = source.getInt(key, Int.MIN_VALUE)
        if (result == Int.MIN_VALUE) {
            val reference = source.getInt(key, Int.MAX_VALUE)
            if (reference == Int.MAX_VALUE) {
                keyOrValueNotFoundError(key)
            }
        }
        return result
    }

    @Suppress("AutoBoxing")
    public actual fun getIntOrNull(key: String): Int? {
        val result = source.getInt(key, Int.MIN_VALUE)
        if (result == Int.MIN_VALUE) {
            val reference = source.getInt(key, Int.MAX_VALUE)
            if (reference == Int.MAX_VALUE) {
                return null
            }
        }
        return result
    }

    public actual fun getLong(key: String): Long {
        val result = source.getLong(key, Long.MIN_VALUE)
        if (result == Long.MIN_VALUE) {
            val reference = source.getLong(key, Long.MAX_VALUE)
            if (reference == Long.MAX_VALUE) {
                keyOrValueNotFoundError(key)
            }
        }
        return result
    }

    @Suppress("AutoBoxing")
    public actual fun getLongOrNull(key: String): Long? {
        val result = source.getLong(key, Long.MIN_VALUE)
        if (result == Long.MIN_VALUE) {
            val reference = source.getLong(key, Long.MAX_VALUE)
            if (reference == Long.MAX_VALUE) {
                return null
            }
        }
        return result
    }

    /**
     * Retrieves a [Parcelable] value associated with the specified [key], or throws an
     * [IllegalArgumentException] if this [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the value for.
     * @param parcelableClass The expected type of the value.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException If associated value is `null`.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun <T : Parcelable> getParcelable(key: String, parcelableClass: KClass<T>): T {
        return getParcelable(source, key, parcelableClass.java) ?: keyOrValueNotFoundError(key)
    }

    /**
     * Retrieves a [Parcelable] value associated with the specified [key], or throws an
     * [IllegalArgumentException] if this [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the value for.
     * @param T The expected type of the value.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException If associated value is `null`.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public inline fun <reified T : Parcelable> getParcelable(key: String): T {
        return getParcelable(key, T::class)
    }

    /**
     * Retrieves a [Parcelable] value associated with the specified [key], or `null` if this
     * [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the value for.
     * @param parcelableClass The expected type of the value.
     * @return The value associated with the [key], or `null` if no valid value is found.
     */
    public fun <T : Parcelable> getParcelableOrNull(key: String, parcelableClass: KClass<T>): T? {
        return getParcelable(source, key, parcelableClass.java)
    }

    /**
     * Retrieves a [Parcelable] value associated with the specified [key], or `null` if this
     * [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the value for.
     * @param T The expected type of the value.
     * @return The value associated with the [key], or `null` if no valid value is found.
     */
    public inline fun <reified T : Parcelable> getParcelableOrNull(key: String): T? {
        return getParcelableOrNull(key, T::class)
    }

    /**
     * Retrieves a [Serializable] value associated with the specified [key], or throws an
     * [IllegalArgumentException] if this [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the value for.
     * @param serializableClass The expected type of the value.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException If associated value is `null`.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun <T : Serializable> getJavaSerializable(
        key: String,
        serializableClass: KClass<T>,
    ): T {
        return getSerializable(source, key, serializableClass.java) ?: keyOrValueNotFoundError(key)
    }

    /**
     * Retrieves a [Serializable] value associated with the specified [key], or throws an
     * [IllegalArgumentException] if this [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the value for.
     * @param T The expected type of the value.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException If associated value is `null`.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public inline fun <reified T : Serializable> getJavaSerializable(key: String): T {
        return getJavaSerializable(key, T::class)
    }

    /**
     * Retrieves a [Serializable] value associated with the specified [key], or `null` if this
     * [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the value for.
     * @param serializableClass The expected type of the value.
     * @return The value associated with the [key], or `null` if no valid value is found.
     */
    public fun <T : Serializable> getJavaSerializableOrNull(
        key: String,
        serializableClass: KClass<T>,
    ): T? {
        return getSerializable(source, key, serializableClass.java)
    }

    /**
     * Retrieves a [Serializable] value associated with the specified [key], or `null` if this
     * [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the value for.
     * @param T The expected type of the value.
     * @return The value associated with the [key], or `null` if no valid value is found.
     */
    public inline fun <reified T : Serializable> getJavaSerializableOrNull(key: String): T? {
        return getJavaSerializableOrNull(key, T::class)
    }

    /**
     * Retrieves a [Size] value associated with the specified [key], or throws an
     * [IllegalArgumentException] if this [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException If associated value is `null`.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getSize(key: String): Size {
        return source.getSize(key) ?: keyOrValueNotFoundError(key)
    }

    /**
     * Retrieves a [Size] value associated with the specified [key], or `null` if this [SavedState]
     * does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key], or `null` if no valid value is found.
     */
    public fun getSizeOrNull(key: String): Size? {
        return source.getSize(key)
    }

    /**
     * Retrieves a [SizeF] value associated with the specified [key], or throws an
     * [IllegalArgumentException] if this [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException If associated value is `null`.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getSizeF(key: String): SizeF {
        return source.getSizeF(key) ?: keyOrValueNotFoundError(key)
    }

    /**
     * Retrieves a [SizeF] value associated with the specified [key], or `null` if this [SavedState]
     * does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key], or `null` if no valid value is found.
     */
    public fun getSizeFOrNull(key: String): SizeF? {
        return source.getSizeF(key)
    }

    @Suppress("ArrayReturn")
    public actual fun getSavedStateArray(key: String): Array<SavedState> {
        return getParcelableArray(key)
    }

    @Suppress("ArrayReturn", "NullableCollection")
    public actual fun getSavedStateArrayOrNull(key: String): Array<SavedState>? {
        return getParcelableArrayOrNull(key)
    }

    public actual fun getString(key: String): String {
        return source.getString(key) ?: keyOrValueNotFoundError(key)
    }

    public actual fun getStringOrNull(key: String): String? {
        return source.getString(key)
    }

    public actual fun getIntList(key: String): List<Int> {
        return source.getIntegerArrayList(key) ?: keyOrValueNotFoundError(key)
    }

    @Suppress("NullableCollection")
    public actual fun getIntListOrNull(key: String): List<Int>? {
        return source.getIntegerArrayList(key)
    }

    public actual fun getCharSequenceList(key: String): List<CharSequence> {
        return source.getCharSequenceArrayList(key) ?: keyOrValueNotFoundError(key)
    }

    @Suppress("NullableCollection")
    public actual fun getCharSequenceListOrNull(key: String): List<CharSequence>? {
        return source.getCharSequenceArrayList(key)
    }

    public actual fun getSavedStateList(key: String): List<SavedState> {
        return getParcelableList(key)
    }

    @Suppress("NullableCollection")
    public actual fun getSavedStateListOrNull(key: String): List<SavedState>? {
        return getParcelableListOrNull(key)
    }

    public actual fun getStringList(key: String): List<String> {
        return source.getStringArrayList(key) ?: keyOrValueNotFoundError(key)
    }

    @Suppress("NullableCollection")
    public actual fun getStringListOrNull(key: String): List<String>? {
        return source.getStringArrayList(key)
    }

    /**
     * Retrieves a [List] of [Parcelable] values associated with the specified [key], or throws an
     * [IllegalArgumentException] if this [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the values for.
     * @param parcelableClass The expected type of the values.
     * @return The values associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException If associated value is `null`.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun <T : Parcelable> getParcelableList(
        key: String,
        parcelableClass: KClass<T>,
    ): List<T> {
        return getParcelableArrayList(source, key, parcelableClass.java)
            ?: keyOrValueNotFoundError(key)
    }

    /**
     * Retrieves a [List] of [Parcelable] values associated with the specified [key], or throws an
     * [IllegalArgumentException] if this [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the values for.
     * @param T The expected type of the values.
     * @return The values associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException If associated value is `null`.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public inline fun <reified T : Parcelable> getParcelableList(key: String): List<T> {
        return getParcelableList(key, T::class)
    }

    /**
     * Retrieves a [List] of [Parcelable] values associated with the specified [key], or `null` if
     * this [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the values for.
     * @param parcelableClass The expected type of the values.
     * @return The values associated with the [key], or `null` if no valid value is found.
     */
    @Suppress("NullableCollection")
    public fun <T : Parcelable> getParcelableListOrNull(
        key: String,
        parcelableClass: KClass<T>,
    ): List<T>? {
        return getParcelableArrayList(source, key, parcelableClass.java)
    }

    /**
     * Retrieves a [List] of [Parcelable] values associated with the specified [key], or `null` if
     * this [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the values for.
     * @param T The expected type of the values.
     * @return The value associated with the [key], or `null` if no valid value is found.
     */
    @Suppress("NullableCollection")
    public inline fun <reified T : Parcelable> getParcelableListOrNull(key: String): List<T>? {
        return getParcelableListOrNull(key, T::class)
    }

    public actual fun getBooleanArray(key: String): BooleanArray {
        return source.getBooleanArray(key) ?: keyOrValueNotFoundError(key)
    }

    public actual fun getBooleanArrayOrNull(key: String): BooleanArray? {
        return source.getBooleanArray(key)
    }

    public actual fun getCharArray(key: String): CharArray {
        return source.getCharArray(key) ?: keyOrValueNotFoundError(key)
    }

    public actual fun getCharArrayOrNull(key: String): CharArray? {
        return source.getCharArray(key)
    }

    @Suppress("ArrayReturn")
    public actual fun getCharSequenceArray(key: String): Array<CharSequence> {
        return source.getCharSequenceArray(key) ?: keyOrValueNotFoundError(key)
    }

    @Suppress("ArrayReturn", "NullableCollection")
    public actual fun getCharSequenceArrayOrNull(key: String): Array<CharSequence>? {
        return source.getCharSequenceArray(key)
    }

    public actual fun getDoubleArray(key: String): DoubleArray {
        return source.getDoubleArray(key) ?: keyOrValueNotFoundError(key)
    }

    public actual fun getDoubleArrayOrNull(key: String): DoubleArray? {
        return source.getDoubleArray(key)
    }

    public actual fun getFloatArray(key: String): FloatArray {
        return source.getFloatArray(key) ?: keyOrValueNotFoundError(key)
    }

    public actual fun getFloatArrayOrNull(key: String): FloatArray? {
        return source.getFloatArray(key)
    }

    public actual fun getIntArray(key: String): IntArray {
        return source.getIntArray(key) ?: keyOrValueNotFoundError(key)
    }

    public actual fun getIntArrayOrNull(key: String): IntArray? {
        return source.getIntArray(key)
    }

    public actual fun getLongArray(key: String): LongArray {
        return source.getLongArray(key) ?: keyOrValueNotFoundError(key)
    }

    public actual fun getLongArrayOrNull(key: String): LongArray? {
        return source.getLongArray(key)
    }

    public actual fun getStringArray(key: String): Array<String> {
        return source.getStringArray(key) ?: keyOrValueNotFoundError(key)
    }

    @Suppress("NullableCollection")
    public actual fun getStringArrayOrNull(key: String): Array<String>? {
        return source.getStringArray(key)
    }

    /**
     * Retrieves an [Array] of [Parcelable] values associated with the specified [key], or throws an
     * [IllegalArgumentException] if this [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the values for.
     * @param parcelableClass The expected type of the values.
     * @return The values associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException If associated value is `null`.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    @Suppress("ArrayReturn")
    public fun <T : Parcelable> getParcelableArray(
        key: String,
        parcelableClass: KClass<T>,
    ): Array<T> {
        return getParcelableArrayOrNull(key, parcelableClass) ?: keyOrValueNotFoundError(key)
    }

    /**
     * Retrieves an [Array] of [Parcelable] values associated with the specified [key], or throws an
     * [IllegalArgumentException] if this [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the values for.
     * @param T The expected type of the values.
     * @return The values associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException If associated value is `null`.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    @Suppress("ArrayReturn")
    public inline fun <reified T : Parcelable> getParcelableArray(key: String): Array<T> {
        return getParcelableArray(key, T::class)
    }

    /**
     * Retrieves an [Array] of [Parcelable] values associated with the specified [key], or `null` if
     * this [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the values for.
     * @param parcelableClass The expected type of the values.
     * @return The values associated with the [key], or `null` if no valid value is found.
     */
    @Suppress("ArrayReturn", "NullableCollection")
    public fun <T : Parcelable> getParcelableArrayOrNull(
        key: String,
        parcelableClass: KClass<T>,
    ): Array<T>? {
        @Suppress("UNCHECKED_CAST")
        return getParcelableArray(source, key, parcelableClass.java) as? Array<T>
    }

    /**
     * Retrieves an [Array] of [Parcelable] values associated with the specified [key], or `null` if
     * this [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the values for.
     * @param T The expected type of the values.
     * @return The values associated with the [key], or `null` if no valid value is found.
     */
    @Suppress("ArrayReturn", "NullableCollection")
    public inline fun <reified T : Parcelable> getParcelableArrayOrNull(key: String): Array<T>? {
        return getParcelableArrayOrNull(key, T::class)
    }

    /**
     * Retrieves a [SparseArray] of [Parcelable] values associated with the specified [key], or
     * throws an [IllegalArgumentException] if this [SavedState] does not contain a valid value for
     * the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the values for.
     * @param parcelableClass The expected type of the values.
     * @return The values associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException If associated value is `null`.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun <T : Parcelable> getSparseParcelableArray(
        key: String,
        parcelableClass: KClass<T>,
    ): SparseArray<T> {
        return getSparseParcelableArrayOrNull(key, parcelableClass) ?: keyOrValueNotFoundError(key)
    }

    /**
     * Retrieves a [SparseArray] of [Parcelable] values associated with the specified [key], or
     * throws an [IllegalArgumentException] if this [SavedState] does not contain a valid value for
     * the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the values for.
     * @param T The expected type of the values.
     * @return The values associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException If associated value is `null`.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public inline fun <reified T : Parcelable> getSparseParcelableArray(
        key: String
    ): SparseArray<T> {
        return getSparseParcelableArray(key, T::class)
    }

    /**
     * Retrieves a [Array] of [Parcelable] values associated with the specified [key], or `null` if
     * this [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the values for.
     * @param parcelableClass The expected type of the values.
     * @return The values associated with the [key], or `null` if no valid value is found.
     */
    public fun <T : Parcelable> getSparseParcelableArrayOrNull(
        key: String,
        parcelableClass: KClass<T>,
    ): SparseArray<T>? {
        return getSparseParcelableArray(source, key, parcelableClass.java)
    }

    /**
     * Retrieves a [Array] of [Parcelable] values associated with the specified [key], or `null` if
     * this [SavedState] does not contain a valid value for the key.
     *
     * More specifically, a [SavedState] is considered to contain a valid value for a [key] if the
     * [key] exists, the associated value is not `null`, and it is of the expected type.
     *
     * @param key The [key] to retrieve the values for.
     * @param T The expected type of the values.
     * @return The values associated with the [key], or `null` if no valid value is found.
     */
    public inline fun <reified T : Parcelable> getSparseParcelableArrayOrNull(
        key: String
    ): SparseArray<T>? {
        return getSparseParcelableArrayOrNull(key, T::class)
    }

    public actual fun getSavedState(key: String): SavedState {
        return source.getBundle(key) ?: keyOrValueNotFoundError(key)
    }

    @Suppress("NullableCollection")
    public actual fun getSavedStateOrNull(key: String): SavedState? {
        return source.getBundle(key)
    }

    public actual fun size(): Int = source.size()

    public actual fun isEmpty(): Boolean = source.isEmpty

    public actual fun isNull(key: String): Boolean {
        // Using `getString` to check for `null` is unreliable as it returns null for type
        // mismatches. To reliably determine if the value is actually `null`, we use the
        // deprecated `Bundle.get`.
        @Suppress("DEPRECATION")
        return contains(key) && source[key] == null
    }

    public actual operator fun contains(key: String): Boolean = source.containsKey(key)

    public actual fun contentDeepEquals(other: SavedState): Boolean =
        source.contentDeepEquals(other)

    public actual fun contentDeepHashCode(): Int = source.contentDeepHashCode()

    public actual fun contentDeepToString(): String {
        // in order not to overflow Int.MAX_VALUE
        val length = source.size().coerceAtMost((Int.MAX_VALUE - 2) / 5) * 5 + 2
        return buildString(length) { source.contentDeepToString(result = this, mutableListOf()) }
    }

    public actual fun toMap(): Map<String, Any?> {
        return buildMap(capacity = source.size()) {
            for (key in source.keySet()) {
                @Suppress("DEPRECATION") put(key, source[key])
            }
        }
    }
}

private fun SavedState.contentDeepEquals(other: SavedState): Boolean {
    if (this === other) return true
    if (this.size() != other.size()) return false

    for (k in this.keySet()) {
        @Suppress("DEPRECATION") val v1 = this[k]
        @Suppress("DEPRECATION") val v2 = other[k]

        when {
            v1 === v2 -> continue
            v1 == v2 -> continue
            v1 == null || v2 == null -> return false

            // container types
            v1 is SavedState && v2 is SavedState -> if (!v1.contentDeepEquals(v2)) return false
            v1 is Array<*> && v2 is Array<*> -> if (!v1.contentDeepEquals(v2)) return false

            // primitive arrays
            v1 is ByteArray && v2 is ByteArray -> if (!v1.contentEquals(v2)) return false
            v1 is ShortArray && v2 is ShortArray -> if (!v1.contentEquals(v2)) return false
            v1 is IntArray && v2 is IntArray -> if (!v1.contentEquals(v2)) return false
            v1 is LongArray && v2 is LongArray -> if (!v1.contentEquals(v2)) return false
            v1 is FloatArray && v2 is FloatArray -> if (!v1.contentEquals(v2)) return false
            v1 is DoubleArray && v2 is DoubleArray -> if (!v1.contentEquals(v2)) return false
            v1 is CharArray && v2 is CharArray -> if (!v1.contentEquals(v2)) return false
            v1 is BooleanArray && v2 is BooleanArray -> if (!v1.contentEquals(v2)) return false

            // if nothing else works
            else -> if (v1 != v2) return false
        }
    }
    return true
}

private fun SavedState.contentDeepHashCode(): Int {
    var result = 1

    for (k in this.keySet()) {
        val elementHash =
            when (@Suppress("DEPRECATION") val element = this[k]) {
                // container types
                is SavedState -> element.contentDeepHashCode()
                is Array<*> -> element.contentDeepHashCode()

                // primitive arrays
                is ByteArray -> element.contentHashCode()
                is ShortArray -> element.contentHashCode()
                is IntArray -> element.contentHashCode()
                is LongArray -> element.contentHashCode()
                is FloatArray -> element.contentHashCode()
                is DoubleArray -> element.contentHashCode()
                is CharArray -> element.contentHashCode()
                is BooleanArray -> element.contentHashCode()

                // if nothing else works
                else -> element.hashCode()
            }
        result = 31 * result + elementHash
    }

    return result
}

private fun SavedState.contentDeepToString(
    result: StringBuilder,
    processed: MutableList<SavedState>,
) {
    if (this in processed) {
        result.append("[...]")
        return
    }
    processed += this
    result.append('[')

    for ((i, k) in keySet().withIndex()) {
        if (i != 0) {
            result.append(", ")
        }
        result.append("$k=")
        when (@Suppress("DEPRECATION") val element = this[k]) {
            null -> result.append("null")
            // container types
            is SavedState -> element.contentDeepToString(result, processed)
            is Array<*> -> result.append(element.contentDeepToString())

            // primitive arrays
            is ByteArray -> result.append(element.contentToString())
            is ShortArray -> result.append(element.contentToString())
            is IntArray -> result.append(element.contentToString())
            is LongArray -> result.append(element.contentToString())
            is FloatArray -> result.append(element.contentToString())
            is DoubleArray -> result.append(element.contentToString())
            is CharArray -> result.append(element.contentToString())
            is BooleanArray -> result.append(element.contentToString())

            // if nothing else works
            else -> result.append(element.toString())
        }
    }

    result.append(']')
    processed.removeAt(processed.lastIndex)
}
