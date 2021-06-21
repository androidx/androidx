/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.data

import android.os.Parcel
import android.os.Parcelable

/** A [Parcelable] wrapper that can hold a value of a specified type. */
@Suppress("DataClassPrivateConstructor")
public data class Value
private constructor(
    // TODO(b/175054913): Investigate using a AutoOneOf instead.
    val format: Int,
    val doubleList: List<Double>,
    val longValue: Long,
) : Parcelable {

    /**
     * Returns this [Value] represented as an `long`.
     *
     * @throws IllegalStateException if [isLong] is `false`
     */
    public fun asLong(): Long {
        check(isLong) { "Attempted to read value as long, but value is not of type long" }
        return longValue
    }

    /**
     * Returns this [Value] represented as an `boolean`.
     *
     * @throws IllegalStateException if [isBoolean] is `false`
     */
    public fun asBoolean(): Boolean {
        check(isBoolean) { "Attempted to read value as boolean, but value is not of type boolean" }
        return longValue != 0L
    }

    /**
     * Returns this [Value] represented as a `double`.
     *
     * @throws IllegalStateException if [isDouble] is `false`
     */
    public fun asDouble(): Double {
        check(isDouble) { "Attempted to read value as double, but value is not of type double" }
        return doubleList[0]
    }

    /**
     * Returns this [Value] represented as a `double[]`.
     *
     * @throws IllegalStateException if [isDoubleArray] is `false`
     */
    public fun asDoubleArray(): DoubleArray {
        check(isDoubleArray) {
            "Attempted to read value as double array, but value is not correct type"
        }
        return doubleList.toDoubleArray()
    }

    /** Whether or not this [Value] can be represented as an `long`. */
    public val isLong: Boolean
        get() = format == FORMAT_LONG

    /** Whether or not this [Value] can be represented as an `boolean`. */
    public val isBoolean: Boolean
        get() = format == FORMAT_BOOLEAN

    /** Whether or not this [Value] can be represented as a `double`. */
    public val isDouble: Boolean
        get() = format == FORMAT_DOUBLE

    /** Whether or not this [Value] can be represented as a `double[]`. */
    public val isDoubleArray: Boolean
        get() = format == FORMAT_DOUBLE_ARRAY

    override fun describeContents(): Int {
        return 0
    }

    /**
     * Writes the value of this object to [dest].
     *
     * @throws IllegalStateException if [format] is invalid
     */
    override fun writeToParcel(dest: Parcel, flags: Int) {
        val format = format
        dest.writeInt(format)
        when (format) {
            FORMAT_BOOLEAN, FORMAT_LONG -> {
                dest.writeLong(longValue)
                return
            }
            FORMAT_DOUBLE -> {
                dest.writeDouble(asDouble())
                return
            }
            FORMAT_DOUBLE_ARRAY -> {
                val doubleArray = asDoubleArray()
                dest.writeInt(doubleArray.size)
                dest.writeDoubleArray(doubleArray)
                return
            }
            else -> {}
        }
        throw IllegalStateException(String.format("Unexpected format: %s", format))
    }

    public companion object {
        /** The format used for a [Value] represented as a `double`. */
        public const val FORMAT_DOUBLE: Int = 1

        /** The format used for a [Value] represented as an `long`. */
        public const val FORMAT_LONG: Int = 2

        /** The format used for a [Value] represented as an `boolean`. */
        public const val FORMAT_BOOLEAN: Int = 4

        /** The format used for a [Value] represented as a `double[]`. */
        public const val FORMAT_DOUBLE_ARRAY: Int = 3

        @JvmField
        public val CREATOR: Parcelable.Creator<Value> =
            object : Parcelable.Creator<Value> {
                override fun createFromParcel(parcel: Parcel): Value {
                    val format = parcel.readInt()
                    when (format) {
                        FORMAT_BOOLEAN, FORMAT_LONG ->
                            return Value(format, listOf(), parcel.readLong())
                        FORMAT_DOUBLE ->
                            return Value(format, listOf(parcel.readDouble()), /* longValue= */ 0)
                        FORMAT_DOUBLE_ARRAY -> {
                            val doubleArray = DoubleArray(parcel.readInt())
                            parcel.readDoubleArray(doubleArray)
                            return Value(format, doubleArray.toList(), /* longValue= */ 0)
                        }
                        else -> {}
                    }
                    throw IllegalStateException(String.format("Unexpected format: %s", format))
                }

                override fun newArray(size: Int): Array<Value?> {
                    return arrayOfNulls(size)
                }
            }

        /** Creates a [Value] that represents a `long`. */
        @JvmStatic
        public fun ofLong(value: Long): Value {
            return Value(FORMAT_LONG, listOf(), value)
        }

        /** Creates a [Value] that represents an `boolean`. */
        @JvmStatic
        public fun ofBoolean(value: Boolean): Value {
            return Value(FORMAT_BOOLEAN, listOf(), if (value) 1 else 0)
        }

        /** Creates a [Value] that represents a `double`. */
        @JvmStatic
        public fun ofDouble(value: Double): Value {
            return Value(FORMAT_DOUBLE, listOf(value), longValue = 0)
        }

        /** Creates a [Value] that represents a `double[]`. */
        @JvmStatic
        public fun ofDoubleArray(vararg doubleArray: Double): Value {
            return Value(FORMAT_DOUBLE_ARRAY, doubleArray.toList(), longValue = 0)
        }

        /**
         * Compares two [Value] s based on their representation.
         *
         * @throws IllegalStateException if `first` and `second` do not share the same format or are
         * represented as a `double[]`
         */
        internal fun compare(first: Value, second: Value): Int {
            check(first.format == second.format) {
                "Attempted to compare Values with different formats"
            }
            when (first.format) {
                FORMAT_LONG -> return first.longValue.compareTo(second.longValue)
                FORMAT_BOOLEAN -> return first.asBoolean().compareTo(second.asBoolean())
                FORMAT_DOUBLE -> return first.doubleList[0].compareTo(second.doubleList[0])
                FORMAT_DOUBLE_ARRAY ->
                    throw IllegalStateException(
                        "Attempted to compare Values with invalid format (double array)"
                    )
                else -> {}
            }
            throw IllegalStateException(String.format("Unexpected format: %s", first.format))
        }

        /**
         * Adds two [Value] s based on their representation.
         *
         * @throws IllegalStateException if `first` and `second` do not share the same format or are
         * represented as a `double[]` or `boolean`
         */
        @JvmStatic
        public fun sum(first: Value, second: Value): Value {
            require(first.format == second.format) {
                "Attempted to add Values with different formats"
            }
            when (first.format) {
                FORMAT_LONG -> return ofLong(first.asLong() + second.asLong())
                FORMAT_DOUBLE -> return ofDouble(first.asDouble() + second.asDouble())
                FORMAT_BOOLEAN ->
                    throw IllegalStateException(
                        "Attempted to add Values with invalid format (boolean)"
                    )
                FORMAT_DOUBLE_ARRAY ->
                    throw IllegalStateException(
                        "Attempted to add Values with invalid format (double array)"
                    )
                else -> {}
            }
            throw IllegalStateException(String.format("Unexpected format: %s", first.format))
        }
    }
}
