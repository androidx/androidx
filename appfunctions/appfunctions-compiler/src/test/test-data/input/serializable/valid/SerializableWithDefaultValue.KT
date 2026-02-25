package com.testdata

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable
class NestedSerializable(val intValue: Int)

@AppFunctionSerializable
class SerializableWithDefaultValue(
    // Int
    val optionalNonNullInt: Int = 0,
    val optionalNullableInt: Int? = null,

    // Long
    val optionalNonNullLong: Long = 0L,
    val optionalNullableLong: Long? = null,

    // Boolean
    val optionalNonNullBoolean: Boolean = false,
    val optionalNullableBoolean: Boolean? = null,

    // Float
    val optionalNonNullFloat: Float = 0.0f,
    val optionalNullableFloat: Float? = null,

    // Double
    val optionalNonNullDouble: Double = 0.0,
    val optionalNullableDouble: Double? = null,

    // String
    val optionalNullableString: String? = null,

    // Serializable
    val optionalNullableSerializable: NestedSerializable? = null,

    // IntArray
    val optionalNonNullIntArray: IntArray = intArrayOf(),
    val optionalNullableIntArray: IntArray? = null,

    // LongArray
    val optionalNonNullLongArray: LongArray = longArrayOf(),
    val optionalNullableLongArray: LongArray? = null,

    // BooleanArray
    val optionalNonNullBooleanArray: BooleanArray = booleanArrayOf(),
    val optionalNullableBooleanArray: BooleanArray? = null,

    // FloatArray
    val optionalNonNullFloatArray: FloatArray = floatArrayOf(),
    val optionalNullableFloatArray: FloatArray? = null,

    // DoubleArray
    val optionalNonNullDoubleArray: DoubleArray = doubleArrayOf(),
    val optionalNullableDoubleArray: DoubleArray? = null,

    // ByteArray
    val optionalNonNullByteArray: ByteArray = byteArrayOf(),
    val optionalNullableByteArray: ByteArray? = null,

    // List<String>
    val optionalNonNullListString: List<String> = emptyList(),
    val optionalNullableListString: List<String>? = null,

    // List of Serializable
    val optionalNonNullSerializableList: List<NestedSerializable> = emptyList(),
    val optionalNullableSerializableList: List<NestedSerializable>? = null,
)