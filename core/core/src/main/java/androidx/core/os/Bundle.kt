/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Cannot be updated, the Kt name has been released
@file:Suppress("FacadeClassJvmName")

package androidx.core.os

import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import java.io.Serializable
import java.util.ArrayList

/**
 * Returns a new [Bundle] with the given key/value pairs as elements.
 *
 * @throws IllegalArgumentException When a value is not a supported type of [Bundle].
 */
@Deprecated(
    message =
        "This method does not provide type safety at compile time. Use the platform `Bundle` " +
            "class directly instead."
)
public fun bundleOf(vararg pairs: Pair<String, Any?>): Bundle =
    Bundle(pairs.size).apply {
        for ((key, value) in pairs) {
            when (value) {
                null -> putString(key, null) // Any nullable type will suffice.

                // Scalars
                is Boolean -> putBoolean(key, value)
                is Byte -> putByte(key, value)
                is Char -> putChar(key, value)
                is Double -> putDouble(key, value)
                is Float -> putFloat(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Short -> putShort(key, value)

                // References
                is Bundle -> putBundle(key, value)
                is CharSequence -> putCharSequence(key, value)
                is Parcelable -> putParcelable(key, value)

                // Scalar arrays
                is BooleanArray -> putBooleanArray(key, value)
                is ByteArray -> putByteArray(key, value)
                is CharArray -> putCharArray(key, value)
                is DoubleArray -> putDoubleArray(key, value)
                is FloatArray -> putFloatArray(key, value)
                is IntArray -> putIntArray(key, value)
                is LongArray -> putLongArray(key, value)
                is ShortArray -> putShortArray(key, value)

                // Reference arrays
                is Array<*> -> {
                    val componentType = value::class.java.componentType!!
                    @Suppress("UNCHECKED_CAST") // Checked by reflection.
                    when {
                        Parcelable::class.java.isAssignableFrom(componentType) -> {
                            putParcelableArray(key, value as Array<Parcelable>)
                        }
                        String::class.java.isAssignableFrom(componentType) -> {
                            putStringArray(key, value as Array<String>)
                        }
                        CharSequence::class.java.isAssignableFrom(componentType) -> {
                            putCharSequenceArray(key, value as Array<CharSequence>)
                        }
                        Serializable::class.java.isAssignableFrom(componentType) -> {
                            putSerializable(key, value)
                        }
                        else -> {
                            val valueType = componentType.canonicalName
                            throw IllegalArgumentException(
                                "Illegal value array type $valueType for key \"$key\""
                            )
                        }
                    }
                }

                // Last resort. Also we must check this after Array<*> as all arrays are
                // serializable.
                is Serializable -> putSerializable(key, value)
                else -> {
                    if (value is IBinder) {
                        this.putBinder(key, value)
                    } else if (value is Size) {
                        putSize(key, value)
                    } else if (value is SizeF) {
                        putSizeF(key, value)
                    } else {
                        val valueType = value.javaClass.canonicalName
                        throw IllegalArgumentException(
                            "Illegal value type $valueType for key \"$key\""
                        )
                    }
                }
            }
        }
    }

/** Returns a new empty [Bundle]. */
public fun bundleOf(): Bundle = Bundle(0)

/**
 * Returns the value associated with the given key or `null` if:
 * - No mapping of the desired type exists for the given key.
 * - A `null` value is explicitly associated with the key.
 * - The object is not of type [T].
 *
 * **Note:** if the expected value is not a class provided by the Android platform, you must call
 * [Bundle.setClassLoader] with the proper [ClassLoader] first. Otherwise, this method might throw
 * an exception or return `null`.
 *
 * Compatibility behavior:
 * - SDK 34 and above, this method matches platform behavior.
 * - SDK 33 and below, the object type is checked after deserialization.
 *
 * @param key a String, or `null`
 * @return a Parcelable value, or `null`
 */
public inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? =
    BundleCompat.getParcelable(this, key, T::class.java)

/**
 * Returns the value associated with the given key or `null` if:
 * - No mapping of the desired type exists for the given key.
 * - A `null` value is explicitly associated with the key.
 * - The object is not of type [T].
 *
 * **Note:** if the expected value is not a class provided by the Android platform, you must call
 * [Bundle.setClassLoader] with the proper [ClassLoader] first. Otherwise, this method might throw
 * an exception or return `null`.
 *
 * Compatibility behavior:
 * - SDK 34 and above, this method matches platform behavior.
 * - SDK 33 and below, this method will not check the array elements' types.
 *
 * @param key a String, or `null`
 * @return an Array<Parcelable> value, or `null`
 */
@Suppress("NullableCollection")
public inline fun <reified T : Parcelable> Bundle.getParcelableArrayCompat(
    key: String
): Array<out Parcelable>? = BundleCompat.getParcelableArray(this, key, T::class.java)

/**
 * Returns the value associated with the given key or `null` if:
 * - No mapping of the desired type exists for the given key.
 * - A `null` value is explicitly associated with the key.
 * - The object is not of type [T].
 *
 * **Note:** if the expected value is not a class provided by the Android platform, you must call
 * [Bundle.setClassLoader] with the proper [ClassLoader] first. Otherwise, this method might throw
 * an exception or return `null`.
 *
 * Compatibility behavior:
 * - SDK 34 and above, this method matches platform behavior.
 * - SDK 33 and below, this method will not check the list elements' types.
 *
 * @param key a String, or `null`
 * @return an ArrayList<T> value, or `null`
 */
@Suppress("ConcreteCollection", "NullableCollection")
public inline fun <reified T : Parcelable> Bundle.getParcelableArrayListCompat(
    key: String
): ArrayList<T>? = BundleCompat.getParcelableArrayList(this, key, T::class.java)

/**
 * Returns the value associated with the given key or `null` if:
 * - No mapping of the desired type exists for the given key.
 * - A `null` value is explicitly associated with the key.
 * - The object is not of type [T].
 *
 * Compatibility behavior:
 * - SDK 34 and above, this method matches platform behavior.
 * - SDK 33 and below, this method will not check the array elements' types.
 *
 * @param key a String, or `null`
 * @return a SparseArray of T values, or `null`
 */
public inline fun <reified T : Parcelable> Bundle.getSparseParcelableArrayCompat(
    key: String
): SparseArray<T>? = BundleCompat.getSparseParcelableArray(this, key, T::class.java)

/**
 * Returns the value associated with the given key or `null` if:
 * - No mapping of the desired type exists for the given key.
 * - A `null` value is explicitly associated with the key.
 * - The object is not of type [T].
 *
 * Compatibility behavior:
 * - SDK 34 and above, this method matches platform behavior.
 * - SDK 33 and below, the object type is checked after deserialization.
 *
 * @param key a String, or `null`
 * @return a Serializable value, or `null`
 */
public inline fun <reified T : Serializable> Bundle.getSerializableCompat(key: String): T? =
    BundleCompat.getSerializable(this, key, T::class.java)
