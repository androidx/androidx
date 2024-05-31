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
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamConstants
import java.util.Collections
import java.util.Objects

/**
 * A persistable set of key/value pairs which are used as inputs and outputs for
 * [ListenableWorker]s. Keys are Strings, and values can be Strings, primitive types, or their array
 * variants.
 *
 * This is a lightweight container, and should not be considered your data store. As such, there is
 * an enforced [.MAX_DATA_BYTES] limit on the serialized (byte array) size of the payloads. This
 * class will throw [IllegalStateException]s if you try to serialize or deserialize past this limit.
 */
class Data {
    private val values: Map<String, Any?>

    /** Copy constructor */
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
     * @param key The key for the argument
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
     * @param key The key for the argument
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
     * @param key The key for the argument
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
     * @param key The key for the argument
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
     * @param key The key for the argument
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
     * @param key The key for the argument
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
         *   be used for reads only.
         */
        get() = Collections.unmodifiableMap(values)

    /**
     * Converts this Data to a byte array suitable for sending to other processes in your
     * application. There are no versioning guarantees with this byte array, so you should not use
     * this for IPCs between applications or persistence.
     *
     * @return The byte array representation of the input
     * @throws IllegalStateException if the serialized payload is bigger than [.MAX_DATA_BYTES]
     */
    fun toByteArray(): ByteArray = toByteArrayInternalV1(this)

    /**
     * Returns `true` if the instance of [Data] has a non-null value corresponding to the given
     * [String] key with the expected type of `T`.
     *
     * @param key The [String] key
     * @param klass The [Class] container for the expected type
     * @return `true` If the instance of [Data] has a value for the given [String] key with the
     *   expected type.
     */
    fun <T> hasKeyWithValueOfType(key: String, klass: Class<T>): Boolean {
        val value = values[key]
        return value != null && klass.isAssignableFrom(value.javaClass)
    }

    /**
     * Returns `true` if the instance of [Data] has a non-null value corresponding to the given
     * [String] key with the expected type of [T].
     *
     * @param key The [String] key
     * @return `true` If the instance of [Data] has a value for the given [String] key with the
     *   expected type.
     */
    internal inline fun <reified T> hasKey(key: String): Boolean {
        return hasKeyWithValueOfType(key, T::class.java)
    }

    /** @return The number of elements in this Data object. */
    @VisibleForTesting @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) fun size(): Int = values.size

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
            val equal =
                if (value == null || otherValue == null) {
                    value === otherValue
                } else if (
                    value is Array<*> &&
                        value.isArrayOf<Any>() &&
                        otherValue is Array<*> &&
                        otherValue.isArrayOf<Any>()
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
            h +=
                if (value is Array<*>) {
                    Objects.hashCode(entry.key) xor value.contentDeepHashCode()
                } else {
                    entry.hashCode()
                }
        }
        return 31 * h
    }

    override fun toString(): String = buildString {
        append("Data {")
        val content =
            values.entries.joinToString { (key, value) ->
                "$key : ${if (value is Array<*>) value.contentToString() else value}"
            }
        append(content)
        append("}")
    }

    /** A builder for [Data] objects. */
    class Builder {
        private val values: MutableMap<String, Any?> = mutableMapOf()

        private fun putDirect(key: String, value: Any?): Builder {
            values[key] = value
            return this
        }

        /**
         * Puts a boolean into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putBoolean(key: String, value: Boolean): Builder = putDirect(key, value)

        /**
         * Puts a boolean array into the arguments.
         *
         * @param key The key for this argument
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
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putByte(key: String, value: Byte): Builder = putDirect(key, value)

        /**
         * Puts an integer array into the arguments.
         *
         * @param key The key for this argument
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
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putInt(key: String, value: Int): Builder = putDirect(key, value)

        /**
         * Puts an integer array into the arguments.
         *
         * @param key The key for this argument
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
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putLong(key: String, value: Long): Builder = putDirect(key, value)

        /**
         * Puts a long array into the arguments.
         *
         * @param key The key for this argument
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
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putFloat(key: String, value: Float): Builder = putDirect(key, value)

        /**
         * Puts a float array into the arguments.
         *
         * @param key The key for this argument
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
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putDouble(key: String, value: Double): Builder = putDirect(key, value)

        /**
         * Puts a double array into the arguments.
         *
         * @param key The key for this argument
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
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putString(key: String, value: String?): Builder = putDirect(key, value)

        /**
         * Puts a String array into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The [Builder]
         */
        fun putStringArray(key: String, value: Array<String?>): Builder = putDirect(key, value)

        /**
         * Puts all input key-value pairs from a [Data] into the Builder.
         *
         * Valid value types are: Boolean, Integer, Long, Float, Double, String, and their array
         * versions. Invalid types will throw an [IllegalArgumentException].
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
         * versions. Invalid types will throw an [IllegalArgumentException].
         *
         * @param values A [Map] of key-value pairs to add
         * @return The [Builder]
         */
        fun putAll(values: Map<String, Any?>): Builder {
            values.forEach { (key, value) -> put(key, value) }
            return this
        }

        /**
         * Puts an input key-value pair into the Builder. Valid types are: Boolean, Integer, Long,
         * Float, Double, String, and array versions of each of those types. Invalid types throw an
         * [IllegalArgumentException].
         *
         * @param key A [String] key to add
         * @param value A nullable [Object] value to add of the valid types
         * @return The [Builder]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun put(key: String, value: Any?): Builder {
            values[key] =
                if (value == null) {
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
                        else ->
                            throw IllegalArgumentException("Key $key has invalid type $valueType")
                    }
                }
            return this
        }

        /**
         * Builds a [Data] object.
         *
         * @return The [Data] object containing all key-value pairs specified by this [Builder].
         */
        fun build(): Data {
            val data = Data(values)
            // Make sure we catch Data objects that are too large at build() instead of later.  This
            // method will throw an exception if data is too big.
            toByteArrayInternalV1(data)
            return data
        }
    }

    companion object {
        /** An empty Data object with no elements. */
        @JvmField val EMPTY = Builder().build()

        /**
         * The maximum number of bytes for Data when it is serialized (converted to a byte array).
         * Please see the class-level Javadoc for more information.
         */
        @SuppressLint("MinMaxConstant") const val MAX_DATA_BYTES = 10 * 1024 // 10KB

        /** The list of supported types. */
        private const val TYPE_NULL: Byte = 0
        private const val TYPE_BOOLEAN: Byte = 1
        private const val TYPE_BYTE: Byte = 2
        private const val TYPE_INTEGER: Byte = 3
        private const val TYPE_LONG: Byte = 4
        private const val TYPE_FLOAT: Byte = 5
        private const val TYPE_DOUBLE: Byte = 6
        private const val TYPE_STRING: Byte = 7
        private const val TYPE_BOOLEAN_ARRAY: Byte = 8
        private const val TYPE_BYTE_ARRAY: Byte = 9
        private const val TYPE_INTEGER_ARRAY: Byte = 10
        private const val TYPE_LONG_ARRAY: Byte = 11
        private const val TYPE_FLOAT_ARRAY: Byte = 12
        private const val TYPE_DOUBLE_ARRAY: Byte = 13
        private const val TYPE_STRING_ARRAY: Byte = 14

        /** Denotes `null` in a String array. */
        private const val NULL_STRING_V1 = "androidx.work.Data-95ed6082-b8e9-46e8-a73f-ff56f00f5d9d"

        /** Magic number used in stream header. */
        private const val STREAM_MAGIC: Short = 0xabef.toShort()

        /** Version number used in stream header. */
        private const val STREAM_VERSION: Short = 1

        /**
         * Converts [Data] to a byte array for persistent storage.
         *
         * @param data The [Data] object to convert
         * @return The byte array representation of the input
         * @throws IllegalStateException if the serialized payload is bigger than [.MAX_DATA_BYTES]
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Deprecated(
            message = "This is kept for testing migration",
            replaceWith = ReplaceWith("toByteArrayInternalV1")
        )
        fun toByteArrayInternalV0(data: Data): ByteArray {
            return try {
                val stream =
                    ByteArrayOutputStream().use { outputStream ->
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

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @TypeConverter
        fun toByteArrayInternalV1(data: Data): ByteArray {
            fun DataOutputStream.writeHeader() {
                // We use our own magic and it's different from the
                // `ObjectStreamConstants.STREAM_MAGIC` used in V0.
                writeShort(STREAM_MAGIC.toInt())
                writeShort(STREAM_VERSION.toInt())
            }

            fun DataOutputStream.writeArray(array: Array<*>) {
                val type =
                    when (array::class) {
                        Array<Boolean>::class -> TYPE_BOOLEAN_ARRAY
                        Array<Byte>::class -> TYPE_BYTE_ARRAY
                        Array<Int>::class -> TYPE_INTEGER_ARRAY
                        Array<Long>::class -> TYPE_LONG_ARRAY
                        Array<Float>::class -> TYPE_FLOAT_ARRAY
                        Array<Double>::class -> TYPE_DOUBLE_ARRAY
                        Array<String>::class -> TYPE_STRING_ARRAY
                        else -> {
                            throw IllegalArgumentException(
                                "Unsupported value type ${array::class.qualifiedName}"
                            )
                        }
                    }
                writeByte(type.toInt())
                writeInt(array.size)
                for (element in array) {
                    when (type) {
                        TYPE_BOOLEAN_ARRAY -> writeBoolean(element as? Boolean ?: false)
                        TYPE_BYTE_ARRAY -> writeByte((element as? Byte)?.toInt() ?: 0)
                        TYPE_INTEGER_ARRAY -> writeInt(element as? Int ?: 0)
                        TYPE_LONG_ARRAY -> writeLong(element as? Long ?: 0L)
                        TYPE_FLOAT_ARRAY -> writeFloat(element as? Float ?: 0f)
                        TYPE_DOUBLE_ARRAY -> writeDouble(element as? Double ?: 0.0)
                        TYPE_STRING_ARRAY -> writeUTF(element as? String ?: NULL_STRING_V1)
                    }
                }
            }

            fun DataOutputStream.writeEntry(key: String, value: Any?) {
                // type + value
                when (value) {
                    null -> writeByte(TYPE_NULL.toInt())
                    is Boolean -> {
                        writeByte(TYPE_BOOLEAN.toInt())
                        writeBoolean(value)
                    }
                    is Byte -> {
                        writeByte(TYPE_BYTE.toInt())
                        writeByte(value.toInt())
                    }
                    is Int -> {
                        writeByte(TYPE_INTEGER.toInt())
                        writeInt(value)
                    }
                    is Long -> {
                        writeByte(TYPE_LONG.toInt())
                        writeLong(value)
                    }
                    is Float -> {
                        writeByte(TYPE_FLOAT.toInt())
                        writeFloat(value)
                    }
                    is Double -> {
                        writeByte(TYPE_DOUBLE.toInt())
                        writeDouble(value)
                    }
                    is String -> {
                        writeByte(TYPE_STRING.toInt())
                        writeUTF(value)
                    }
                    is Array<*> -> {
                        writeArray(value)
                    }
                    else -> {
                        // Exhaustive check
                        throw IllegalArgumentException(
                            "Unsupported value type ${value::class.simpleName}"
                        )
                    }
                }
                // key
                writeUTF(key)
            }

            return try {
                ByteArrayOutputStream().let { outputStream ->
                    DataOutputStream(outputStream).use {
                        it.apply {
                            writeHeader()
                            writeInt(data.size())
                            for ((key, value) in data.values) {
                                writeEntry(key, value)
                            }
                            flush()
                        }
                        check(it.size() <= MAX_DATA_BYTES) {
                            "Data cannot occupy more than $MAX_DATA_BYTES bytes when serialized"
                        }
                        outputStream.toByteArray()
                    }
                }
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
            fun ByteArrayInputStream.isObjectStream(): Boolean {
                val header = ByteArray(2)
                read(header)
                val magic = ObjectStreamConstants.STREAM_MAGIC.toInt()
                val magicLow = magic.toByte()
                val magicHigh = (magic ushr 8).toByte()
                val result = (header[0] == magicHigh) && (header[1] == magicLow)
                reset()
                return result
            }
            fun DataInputStream.readHeader() {
                readShort().let { magic ->
                    check(magic == STREAM_MAGIC) { "Magic number doesn't match: $magic" }
                }
                readShort().let { version ->
                    check(version == STREAM_VERSION) { "Unsupported version number: $version" }
                }
            }
            fun DataInputStream.readValue(type: Byte): Any? {
                return when (type) {
                    TYPE_NULL -> null
                    TYPE_BOOLEAN -> readBoolean()
                    TYPE_BYTE -> readByte()
                    TYPE_INTEGER -> readInt()
                    TYPE_LONG -> readLong()
                    TYPE_FLOAT -> readFloat()
                    TYPE_DOUBLE -> readDouble()
                    TYPE_STRING -> readUTF()
                    TYPE_BOOLEAN_ARRAY -> {
                        Array(readInt()) { readBoolean() }
                    }
                    TYPE_BYTE_ARRAY -> {
                        Array(readInt()) { readByte() }
                    }
                    TYPE_INTEGER_ARRAY -> {
                        Array(readInt()) { readInt() }
                    }
                    TYPE_LONG_ARRAY -> {
                        Array(readInt()) { readLong() }
                    }
                    TYPE_FLOAT_ARRAY -> {
                        Array(readInt()) { readFloat() }
                    }
                    TYPE_DOUBLE_ARRAY -> {
                        Array(readInt()) { readDouble() }
                    }
                    TYPE_STRING_ARRAY -> {
                        Array(readInt()) {
                            readUTF().let {
                                if (it == NULL_STRING_V1) {
                                    null
                                } else {
                                    it
                                }
                            }
                        }
                    }
                    else -> {
                        throw IllegalStateException("Unsupported type $type")
                    }
                }
            }
            check(bytes.size <= MAX_DATA_BYTES) {
                "Data cannot occupy more than $MAX_DATA_BYTES bytes when serialized"
            }
            if (bytes.isEmpty()) return EMPTY

            val map = mutableMapOf<String, Any?>()
            try {
                ByteArrayInputStream(bytes).let { inputStream ->
                    if (inputStream.isObjectStream()) { // V0
                        ObjectInputStream(inputStream).use {
                            it.apply { repeat(readInt()) { map[readUTF()] = readObject() } }
                        }
                    } else { // V1
                        DataInputStream(inputStream).use {
                            it.apply {
                                readHeader()
                                repeat(readInt()) {
                                    val type = readByte()
                                    val value = readValue(type)
                                    val key = readUTF()
                                    map[key] = value
                                }
                            }
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

private fun convertPrimitiveArray(value: IntArray): Array<Int> = Array(value.size) { i -> value[i] }

private fun convertPrimitiveArray(value: LongArray): Array<Long> =
    Array(value.size) { i -> value[i] }

private fun convertPrimitiveArray(value: FloatArray): Array<Float> =
    Array(value.size) { i -> value[i] }

private fun convertPrimitiveArray(value: DoubleArray): Array<Double> =
    Array(value.size) { i -> value[i] }

private val TAG = Logger.tagWithPrefix("Data")
