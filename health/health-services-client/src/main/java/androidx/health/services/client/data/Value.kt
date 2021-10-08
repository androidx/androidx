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

import android.os.Parcelable
import androidx.health.services.client.proto.DataProto

/** A [Parcelable] wrapper that can hold a value of a specified type. */
@Suppress("DataClassPrivateConstructor", "ParcelCreator")
public class Value internal constructor(proto: DataProto.Value) :
    ProtoParcelable<DataProto.Value>() {

    /** @hide */
    override val proto: DataProto.Value by lazy { proto }

    public val format: Int =
        when (proto.valueCase) {
            DataProto.Value.ValueCase.BOOL_VAL -> FORMAT_BOOLEAN
            DataProto.Value.ValueCase.DOUBLE_VAL -> FORMAT_DOUBLE
            DataProto.Value.ValueCase.LONG_VAL -> FORMAT_LONG
            DataProto.Value.ValueCase.DOUBLE_ARRAY_VAL -> FORMAT_DOUBLE_ARRAY
            else -> throw IllegalStateException("Unexpected format: ${proto.valueCase}")
        }

    /**
     * Returns this [Value] as a `String`.
     *
     * @throws IllegalStateException if [format] is unknown
     */
    override fun toString(): String =
        "Value(format=$format, " +
            when (format) {
                FORMAT_BOOLEAN -> "boolVal=${proto.boolVal})"
                FORMAT_LONG -> "longVal=${proto.longVal})"
                FORMAT_DOUBLE -> "doubleVal=${proto.doubleVal})"
                FORMAT_DOUBLE_ARRAY -> "doubleArrayVal=${proto.doubleArrayVal.doubleArrayList})"
                else -> throw IllegalStateException("Unexpected format: ${proto.valueCase}")
            }

    /**
     * Returns this [Value] represented as an `long`.
     *
     * @throws IllegalStateException if [isLong] is `false`
     */
    public fun asLong(): Long {
        check(isLong) { "Attempted to read value as long, but value is not of type long" }
        return proto.longVal
    }

    /**
     * Returns this [Value] represented as an `boolean`.
     *
     * @throws IllegalStateException if [isBoolean] is `false`
     */
    public fun asBoolean(): Boolean {
        check(isBoolean) { "Attempted to read value as boolean, but value is not of type boolean" }
        return proto.boolVal
    }

    /**
     * Returns this [Value] represented as a `double`.
     *
     * @throws IllegalStateException if [isDouble] is `false`
     */
    public fun asDouble(): Double {
        check(isDouble) { "Attempted to read value as double, but value is not of type double" }
        return proto.doubleVal
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
        return proto.doubleArrayVal.doubleArrayList.toDoubleArray()
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
        public val CREATOR: Parcelable.Creator<Value> = newCreator {
            Value(DataProto.Value.parseFrom(it))
        }

        /** Creates a [Value] that represents a `long`. */
        @JvmStatic
        public fun ofLong(value: Long): Value =
            Value(DataProto.Value.newBuilder().setLongVal(value).build())

        /** Creates a [Value] that represents an `boolean`. */
        @JvmStatic
        public fun ofBoolean(value: Boolean): Value =
            Value(DataProto.Value.newBuilder().setBoolVal(value).build())

        /** Creates a [Value] that represents a `double`. */
        @JvmStatic
        public fun ofDouble(value: Double): Value =
            Value(DataProto.Value.newBuilder().setDoubleVal(value).build())

        /** Creates a [Value] that represents a `double[]`. */
        @JvmStatic
        public fun ofDoubleArray(vararg doubleArray: Double): Value =
            Value(
                DataProto.Value.newBuilder()
                    .setDoubleArrayVal(
                        DataProto.Value.DoubleArray.newBuilder()
                            .addAllDoubleArray(doubleArray.toList())
                    )
                    .build()
            )

        /**
         * Compares two [Value] s based on their representation.
         *
         * @throws IllegalStateException if `first` and `second` do not share the same format or are
         * represented as a `double[]`
         */
        @JvmStatic
        public fun compare(first: Value, second: Value): Int {
            check(first.format == second.format) {
                "Attempted to compare Values with different formats"
            }
            when (first.format) {
                FORMAT_LONG -> return first.asLong().compareTo(second.asLong())
                FORMAT_BOOLEAN -> return first.asBoolean().compareTo(second.asBoolean())
                FORMAT_DOUBLE -> return first.asDouble().compareTo(second.asDouble())
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
         * @throws IllegalArgumentException if `first` and `second` do not share the same format or
         * are represented as a `double[]` or `boolean`
         */
        @JvmStatic
        public fun sum(first: Value, second: Value): Value {
            require(first.format == second.format) {
                "Attempted to add Values with different formats"
            }
            return when (first.format) {
                FORMAT_LONG -> ofLong(first.asLong() + second.asLong())
                FORMAT_DOUBLE -> ofDouble(first.asDouble() + second.asDouble())
                FORMAT_BOOLEAN ->
                    throw IllegalArgumentException(
                        "Attempted to add Values with invalid format (boolean)"
                    )
                FORMAT_DOUBLE_ARRAY ->
                    throw IllegalArgumentException(
                        "Attempted to add Values with invalid format (double array)"
                    )
                else -> throw IllegalArgumentException("Unexpected format: ${first.format}")
            }
        }

        /**
         * Subtracts two [Value] s based on their representation (i.e. returns `first` - `second`).
         *
         * @throws IllegalArgumentException if `first` and `second` do not share the same format or
         * are represented as a `double[]` or `boolean`
         *
         * @hide
         */
        @JvmStatic
        public fun difference(first: Value, second: Value): Value {
            require(first.format == second.format) {
                "Attempted to subtract Values with different formats"
            }
            require(first.format == FORMAT_LONG || first.format == FORMAT_DOUBLE) {
                "Provided values are not supported for difference"
            }
            return when (first.format) {
                FORMAT_LONG -> ofLong(first.asLong() - second.asLong())
                FORMAT_DOUBLE -> ofDouble(first.asDouble() - second.asDouble())
                else -> throw IllegalArgumentException("Unexpected format: ${first.format}")
            }
        }

        /**
         * Gets the modulo of two [Value] s based on their representation. (i.e. returns `first` %
         * `second`)
         *
         * @throws IllegalArgumentException if `first` and `second` do not share the same format or
         * are represented as a `double[]` or `boolean`
         *
         * @hide
         */
        @JvmStatic
        public fun modulo(first: Value, second: Value): Value {
            require(first.format == second.format) {
                "Attempted to modulo Values with different formats"
            }
            require(first.format == FORMAT_LONG || first.format == FORMAT_DOUBLE) {
                "Provided values are not supported for modulo"
            }

            return when (first.format) {
                FORMAT_LONG -> ofLong(first.asLong() % second.asLong())
                FORMAT_DOUBLE -> ofDouble(first.asDouble() % second.asDouble())
                else -> throw IllegalArgumentException("Unexpected format: ${first.format}")
            }
        }

        /**
         * Checks if a given [Value] is zero or not.
         *
         * @throws IllegalArgumentException if `value` is a `double[]` or `bolean`
         *
         * @hide
         */
        @JvmStatic
        public fun isZero(value: Value): Boolean {
            return when (value.format) {
                FORMAT_LONG -> value.asLong() == 0L
                FORMAT_DOUBLE -> value.asDouble() == 0.0
                else -> throw IllegalArgumentException("Unexpected format: ${value.format}")
            }
        }
    }
}
