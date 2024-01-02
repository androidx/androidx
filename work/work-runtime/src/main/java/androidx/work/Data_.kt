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
package androidx.work

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.room.TypeConverter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Collections
import java.util.Objects

/**
 * A persistable set of key/value pairs which are used as inputs and outputs for
 * [ListenableWorker]s.  Keys are Strings, and values can be Strings, primitive types, or
 * their array variants.
 *
 * This is a lightweight container, and should not be considered your data store.  As such, there is
 * an enforced [.MAX_DATA_BYTES] limit on the serialized (byte array) size of the payloads.
 * This class will throw [IllegalStateException]s if you try to serialize or deserialize past
 * this limit.
 */
class Data {
    private val values: Map<String, Any?>

    /**
     * Copy constructor
     */
    constructor(other: Data) {
        values = HashMap(other.values)
    }

    internal constructor(values: Map<String, *>) {
        this.values = HashMap(values)
    }

    private inline fun <reified T : Any> getOrDefault(key: String, defaultValue: T): T {
        val value = values[key]
        return if (value is T) value else defaultValue
    }

    private inline fun <reified T : Any, TArray> getTypedArray(
        key: String,
        constructor: (size: Int, init: (index: Int) -> T) -> TArray
    ): TArray? {
        val value = values[key]
        return if (value is Array<*> && value.isArrayOf<T>())
            constructor(value.size) { i -> value[i] as T }
        else null
    }

    /**
     * Gets the boolean value for the given key.
     *
     * @param key          The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    fun getBoolean(key: String, defaultValue: Boolean): Boolean = getOrDefault(key, defaultValue)

    /**
     * Gets the boolean array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; `null` otherwise
     */
    fun getBooleanArray(key: String): BooleanArray? = getTypedArray(key, ::BooleanArray)

    /**
     * Gets the byte value for the given key.
     *
     * @param key          The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    fun getByte(key: String, defaultValue: Byte): Byte = getOrDefault(key, defaultValue)

    /**
     * Gets the byte array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; `null` otherwise
     */
    fun getByteArray(key: String): ByteArray? = getTypedArray(key, ::ByteArray)

    /**
     * Gets the integer value for the given key.
     *
     * @param key          The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    fun getInt(key: String, defaultValue: Int): Int = getOrDefault(key, defaultValue)

    /**
     * Gets the integer array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; `null` otherwise
     */
    fun getIntArray(key: String): IntArray? = getTypedArray(key, ::IntArray)

    /**
     * Gets the long value for the given key.
     *
     * @param key          The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    fun getLong(key: String, defaultValue: Long): Long = getOrDefault(key, defaultValue)

    /**
     * Gets the long array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; `null` otherwise
     */
    fun getLongArray(key: String): LongArray? = getTypedArray(key, ::LongArray)

    /**
     * Gets the float value for the given key.
     *
     * @param key          The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    fun getFloat(key: String, defaultValue: Float): Float = getOrDefault(key, defaultValue)

    /**
     * Gets the float array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; `null` otherwise
     */
    fun getFloatArray(key: String): FloatArray? = getTypedArray(key, ::FloatArray)

    /**
     * Gets the double value for the given key.
     *
     * @param key          The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    fun getDouble(key: String, defaultValue: Double): Double = getOrDefault(key, defaultValue)

    /**
     * Gets the double array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; `null` otherwise
     */
    fun getDoubleArray(key: String): DoubleArray? = getTypedArray(key, ::DoubleArray)

    /**
     * Gets the String value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; `null` otherwise
     */
    fun getString(key: String): String? = values[key] as? String

    /**
     * Gets the String array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; `null` otherwise
     */
    fun getStringArray(key: String): Array<String>? = getTypedArray(key, ::Array)

    val keyValueMap: Map<String, Any?>
        /**
         * Gets all the values in this Data object.
         *
         * @return A [Map] of key-value pairs for this object; this Map is unmodifiable and should
         * be used for reads only.
         */
        get() = Collections.unmodifiableMap(values)

    /**
     * Converts this Data to a byte array suitable for sending to other processes in your
     * application.  There are no versioning guarantees with this byte array, so you should not
     * use this for IPCs between applications or persistence.
     *
     * @return The byte array representation of the input
     * @throws IllegalStateException if the serialized payload is bigger than
     * [.MAX_DATA_BYTES]
     */
    fun toByteArray(): ByteArray = toByteArrayInternal(this)

    /**
     * Returns `true` if the instance of [Data] has a non-null value corresponding to
     * the given [String] key with the expected type of `T`.
     *
     * @param key   The [String] key
     * @param klass The [Class] container for the expected type
     * @return `true` If the instance of [Data] has a value for the given
     * [String] key with the expected type.
     */
    fun <T> hasKeyWithValueOfType(key: String, klass: Class<T>): Boolean {
        val value = values[key]
        return value != null && klass.isAssignableFrom(value.javaClass)
    }

    /**
     * @return The number of elements in this Data object.
     */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun size(): Int = values.size

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val otherData = other as Data
        val keys = values.keys
        if (keys != otherData.values.keys) {
            return false
        }
        for (key in keys) {
            val value = values[key]
            val otherValue = otherData.values[key]
            val equal = if (value == null || otherValue == null) {
                value === otherValue
            } else if (value is Array<*> && value.isArrayOf<Any>() &&
                otherValue is Array<*> && otherValue.isArrayOf<Any>()
            ) {
                value.contentDeepEquals(otherValue)
            } else {
                value == otherValue
            }
            if (!equal) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var h = 0
        for (entry in values.entries) {
            val value = entry.value
            h += if (value is Array<*>) {
                Objects.hashCode(entry.key) xor value.contentDeepHashCode();
            } else {
                entry.hashCode()
            }
        }
        return 31 * h
    }

    override fun toString(): String = buildString {
        append("Data {")
        val content = values.entries.joinToString { (key, value) ->
            "$key : ${if (value is Array<*>) value.contentToString() else value}"
        }
        append(content)
        append("}")
    }

    /**
     * A builder for [Data] objects.
     */
    class Builder {
        private val values: MutableMap<String, Any?> = mutableMapOf()

        private fun putDirect(key: String, value: Any?): Builder {
            values[key] = value
            return this
        }

        /**
         * Puts a boolean into the arguments.
         *
         * @param key   The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putBoolean(key: String, value: Boolean): Builder = putDirect(key, value)

        /**
         * Puts a boolean array into the arguments.
         *
         * @param key   The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putBooleanArray(key: String, value: BooleanArray): Builder {
            values[key] = convertPrimitiveArray(value)
            return this
        }

        /**
         * Puts an byte into the arguments.
         *
         * @param key   The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putByte(key: String, value: Byte): Builder = putDirect(key, value)

        /**
         * Puts an integer array into the arguments.
         *
         * @param key   The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putByteArray(key: String, value: ByteArray): Builder {
            values[key] = convertPrimitiveArray(value)
            return this
        }

        /**
         * Puts an integer into the arguments.
         *
         * @param key   The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putInt(key: String, value: Int): Builder = putDirect(key, value)

        /**
         * Puts an integer array into the arguments.
         *
         * @param key   The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putIntArray(key: String, value: IntArray): Builder {
            values[key] = convertPrimitiveArray(value)
            return this
        }

        /**
         * Puts a long into the arguments.
         *
         * @param key   The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putLong(key: String, value: Long): Builder = putDirect(key, value)

        /**
         * Puts a long array into the arguments.
         *
         * @param key   The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putLongArray(key: String, value: LongArray): Builder {
            values[key] = convertPrimitiveArray(value)
            return this
        }

        /**
         * Puts a float into the arguments.
         *
         * @param key   The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putFloat(key: String, value: Float): Builder = putDirect(key, value)

        /**
         * Puts a float array into the arguments.
         *
         * @param key   The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putFloatArray(key: String, value: FloatArray): Builder {
            values[key] = convertPrimitiveArray(value)
            return this
        }

        /**
         * Puts a double into the arguments.
         *
         * @param key   The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putDouble(key: String, value: Double): Builder = putDirect(key, value)

        /**
         * Puts a double array into the arguments.
         *
         * @param key   The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putDoubleArray(key: String, value: DoubleArray): Builder {
            values[key] = convertPrimitiveArray(value)
            return this
        }

        /**
         * Puts a String into the arguments.
         *
         * @param key   The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putString(key: String, value: String?): Builder = putDirect(key, value)

        /**
         * Puts a String array into the arguments.
         *
         * @param key   The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putStringArray(key: String, value: Array<String?>): Builder = putDirect(key, value)

        /**
         * Puts all input key-value pairs from a [Data] into the Builder.
         *
         * Valid value types are: Boolean, Integer, Long, Float, Double, String, and their array
         * versions.  Invalid types will throw an [IllegalArgumentException].
         *
         * @param data [Data] containing key-value pairs to add
         * @return The [Builder]
         */
        fun putAll(data: Data): Builder {
            putAll(data.values)
            return this
        }

        /**
         * Puts all input key-value pairs from a [Map] into the Builder.
         *
         * Valid value types are: Boolean, Integer, Long, Float, Double, String, and their array
         * versions.  Invalid types will throw an [IllegalArgumentException].
         *
         * @param values A [Map] of key-value pairs to add
         * @return The [Builder]
         */
        fun putAll(values: Map<String, Any?>): Builder {
            values.forEach { (key, value) -> put(key, value) }
            return this
        }

        /**
         * Puts an input key-value pair into the Builder. Valid types are: Boolean, Integer,
         * Long, Float, Double, String, and array versions of each of those types.
         * Invalid types throw an [IllegalArgumentException].
         *
         * @param key   A [String] key to add
         * @param value A nullable [Object] value to add of the valid types
         * @return The [Builder]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun put(key: String, value: Any?): Builder {
            values[key] = if (value == null) {
                null
            } else {
                when (val valueType = value::class) {
                    Boolean::class,
                    Byte::class,
                    Int::class,
                    Long::class,
                    Float::class,
                    Double::class,
                    String::class,
                    Array<Boolean>::class,
                    Array<Byte>::class,
                    Array<Int>::class,
                    Array<Long>::class,
                    Array<Float>::class,
                    Array<Double>::class,
                    Array<String>::class -> value

                    BooleanArray::class -> convertPrimitiveArray(value as BooleanArray)
                    ByteArray::class -> convertPrimitiveArray(value as ByteArray)
                    IntArray::class -> convertPrimitiveArray(value as IntArray)
                    LongArray::class -> convertPrimitiveArray(value as LongArray)
                    FloatArray::class -> convertPrimitiveArray(value as FloatArray)
                    DoubleArray::class -> convertPrimitiveArray(value as DoubleArray)

                    else -> throw IllegalArgumentException("Key $key has invalid type $valueType")
                }
            }
            return this
        }

        /**
         * Builds a [Data] object.
         *
         * @return The [Data] object containing all key-value pairs specified by this
         * [Builder].
         */
        fun build(): Data {
            val data = Data(values)
            // Make sure we catch Data objects that are too large at build() instead of later.  This
            // method will throw an exception if data is too big.
            toByteArrayInternal(data)
            return data
        }
    }

    companion object {
        /**
         * An empty Data object with no elements.
         */
        @JvmField
        val EMPTY = Builder().build()

        /**
         * The maximum number of bytes for Data when it is serialized (converted to a byte array).
         * Please see the class-level Javadoc for more information.
         */
        @SuppressLint("MinMaxConstant")
        const val MAX_DATA_BYTES = 10 * 1024 // 10KB

        /**
         * Converts [Data] to a byte array for persistent storage.
         *
         * @param data The [Data] object to convert
         * @return The byte array representation of the input
         * @throws IllegalStateException if the serialized payload is bigger than
         * [.MAX_DATA_BYTES]
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @TypeConverter
        fun toByteArrayInternal(data: Data): ByteArray {
            return try {
                val stream = ByteArrayOutputStream().use { outputStream ->
                    ObjectOutputStream(outputStream).use { objectOutputStream ->
                        objectOutputStream.writeInt(data.size())
                        for ((key, value) in data.values) {
                            objectOutputStream.writeUTF(key)
                            objectOutputStream.writeObject(value)
                        }
                    }
                    outputStream
                }
                if (stream.size() > MAX_DATA_BYTES)
                    throw IllegalStateException(
                        "Data cannot occupy more than $MAX_DATA_BYTES bytes when serialized"
                    )

                stream.toByteArray()
            } catch (e: IOException) {
                loge(TAG, e) { "Error in Data#toByteArray: " }
                ByteArray(0)
            }
        }

        /**
         * Converts a byte array to [Data].
         *
         * @param bytes The byte array representation to convert
         * @return An [Data] object built from the input
         * @throws IllegalStateException if bytes is bigger than [.MAX_DATA_BYTES]
         */
        @JvmStatic
        @TypeConverter
        fun fromByteArray(bytes: ByteArray): Data {
            check(bytes.size <= MAX_DATA_BYTES) {
                "Data cannot occupy more than $MAX_DATA_BYTES bytes when serialized"
            }
            if (bytes.isEmpty()) return EMPTY

            val map = mutableMapOf<String, Any?>()
            try {
                ByteArrayInputStream(bytes).use { inputStream ->
                    ObjectInputStream(inputStream).use { objectInputStream ->
                        repeat(objectInputStream.readInt()) {
                            map[objectInputStream.readUTF()] = objectInputStream.readObject()
                        }
                    }
                }
            } catch (e: IOException) {
                loge(TAG, e) { "Error in Data#fromByteArray: " }
            } catch (e: ClassNotFoundException) {
                loge(TAG, e) { "Error in Data#fromByteArray: " }
            }
            return Data(map)
        }
    }
}

private fun convertPrimitiveArray(value: BooleanArray): Array<Boolean> =
    Array(value.size) { i -> value[i] }

private fun convertPrimitiveArray(value: ByteArray): Array<Byte> =
    Array(value.size) { i -> value[i] }

private fun convertPrimitiveArray(value: IntArray): Array<Int> =
    Array(value.size) { i -> value[i] }

private fun convertPrimitiveArray(value: LongArray): Array<Long> =
    Array(value.size) { i -> value[i] }

private fun convertPrimitiveArray(value: FloatArray): Array<Float> =
    Array(value.size) { i -> value[i] }

private fun convertPrimitiveArray(value: DoubleArray): Array<Double> =
    Array(value.size) { i -> value[i] }

private val TAG = Logger.tagWithPrefix("Data")
