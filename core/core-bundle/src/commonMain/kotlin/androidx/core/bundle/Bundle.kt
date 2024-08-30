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

package androidx.core.bundle

/**
 * @see android.os.Bundle
 */
expect class Bundle() {
    constructor(initialCapacity: Int)
    constructor(bundle: Bundle)

    fun size(): Int
    fun isEmpty(): Boolean
    fun clear()
    fun containsKey(key: String?): Boolean
    fun remove(key: String?)
    fun keySet(): Set<String?>
    fun putAll(bundle: Bundle)

    fun putBoolean(key: String?, value: Boolean)
    fun putByte(key: String?, value: Byte)
    fun putChar(key: String?, value: Char)
    fun putShort(key: String?, value: Short)
    fun putInt(key: String?, value: Int)
    fun putLong(key: String?, value: Long)
    fun putFloat(key: String?, value: Float)
    fun putDouble(key: String?, value: Double)
    fun putString(key: String?, value: String?)
    fun putCharSequence(key: String?, value: CharSequence?)
    fun putBundle(key: String?, value: Bundle?)
    fun putIntegerArrayList(key: String?, value: ArrayList<Int?>?)
    fun putStringArrayList(key: String?, value: ArrayList<String?>?)
    fun putBooleanArray(key: String?, value: BooleanArray?)
    fun putByteArray(key: String?, value: ByteArray?)
    fun putShortArray(key: String?, value: ShortArray?)
    fun putCharArray(key: String?, value: CharArray?)
    fun putIntArray(key: String?, value: IntArray?)
    fun putLongArray(key: String?, value: LongArray?)
    fun putFloatArray(key: String?, value: FloatArray?)
    fun putDoubleArray(key: String?, value: DoubleArray?)
    fun putStringArray(key: String?, value: Array<String?>?)
    fun putCharSequenceArray(key: String?, value: Array<CharSequence?>?)

    fun getBoolean(key: String?): Boolean
    fun getBoolean(key: String?, defaultValue: Boolean): Boolean
    fun getByte(key: String?): Byte
    fun getByte(key: String?, defaultValue: Byte): Byte
    fun getChar(key: String?): Char
    fun getChar(key: String?, defaultValue: Char): Char
    fun getShort(key: String?): Short
    fun getShort(key: String?, defaultValue: Short): Short
    fun getInt(key: String?): Int
    fun getInt(key: String?, defaultValue: Int): Int
    fun getLong(key: String?): Long
    fun getLong(key: String?, defaultValue: Long): Long
    fun getFloat(key: String?): Float
    fun getFloat(key: String?, defaultValue: Float): Float
    fun getDouble(key: String?): Double
    fun getDouble(key: String?, defaultValue: Double): Double
    fun getString(key: String?): String?
    fun getString(key: String?, defaultValue: String): String
    fun getCharSequence(key: String?): CharSequence?
    fun getCharSequence(key: String?, defaultValue: CharSequence): CharSequence
    fun getBundle(key: String?): Bundle?
    fun getIntegerArrayList(key: String?): ArrayList<Int?>?
    fun getStringArrayList(key: String?): ArrayList<String?>?
    fun getBooleanArray(key: String?): BooleanArray?
    fun getByteArray(key: String?): ByteArray?
    fun getShortArray(key: String?): ShortArray?
    fun getCharArray(key: String?): CharArray?
    fun getIntArray(key: String?): IntArray?
    fun getLongArray(key: String?): LongArray?
    fun getFloatArray(key: String?): FloatArray?
    fun getDoubleArray(key: String?): DoubleArray?
    fun getStringArray(key: String?): Array<String?>?
    fun getCharSequenceArray(key: String?): Array<CharSequence?>?

    @Deprecated("Use the type-safe specific APIs depending on the type of the item to be retrieved")
    operator fun get(key: String?): Any?
}

/**
 * Returns a new [Bundle] with the given key/value pairs as elements.
 *
 * @throws IllegalArgumentException When a value is not a supported type of [Bundle].
 */
expect fun bundleOf(vararg pairs: Pair<String, Any?>): Bundle
