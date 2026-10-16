package com.testdata

import androidx.appfunctions.AppFunctionSchemaDefinition

private const val FAKE_CATEGORY = "fake_schema_category"

@AppFunctionSchemaDefinition(name = "noArg", version = 1, category = FAKE_CATEGORY)
interface FakeNoArg {
    fun noArg()
}

@AppFunctionSchemaDefinition(name = "allPrimitiveParams", version = 1, category = FAKE_CATEGORY)
interface FakeAllPrimitiveParams {
    fun allPrimitiveParams(
        arg1: Int,
        arg2: Long,
        arg3: Float,
        arg4: Double,
        arg5: String,
        arg6: Boolean,
    )
}

@AppFunctionSchemaDefinition(name = "allPrimitiveArrayParams", version = 1, category = FAKE_CATEGORY)
interface FakeAllPrimitiveArrayParams {
    fun allPrimitiveArrayParams(
        arg1: IntArray,
        arg2: LongArray,
        arg3: FloatArray,
        arg4: DoubleArray,
        arg5: List<String>,
        arg6: BooleanArray,
        arg7: ByteArray,
        arg8: List<String?>,
    )
}

@AppFunctionSchemaDefinition(
    name = "allNullablePrimitiveParamsWithDefault",
    version = 1,
    category = FAKE_CATEGORY
)
interface FakeAllNullablePrimitiveParamsWithDefaultValues {
    fun allNullablePrimitiveParamsWithDefault(
        arg1: Int? = null,
        arg2: Long? = null,
        arg3: Float? = null,
        arg4: Double? = null,
        arg5: String? = null,
        arg6: Boolean? = null,
    )
}

@AppFunctionSchemaDefinition(
    name = "allNullablePrimitiveArrayParamsWithDefault",
    version = 1,
    category = FAKE_CATEGORY
)
interface FakeAllNullablePrimitiveArrayParamsWithDefaultValues {
    fun allNullablePrimitiveArrayParamsWithDefault(
        arg1: IntArray? = intArrayOf(),
        arg2: LongArray? = longArrayOf(),
        arg3: FloatArray? = floatArrayOf(),
        arg4: DoubleArray? = doubleArrayOf(),
        arg5: List<String> = emptyList(),
        arg6: BooleanArray? = booleanArrayOf(),
        arg7: ByteArray? = byteArrayOf(),
    )
}

@AppFunctionSchemaDefinition(name = "unitReturn", version = 1, category = FAKE_CATEGORY)
interface UnitReturn {
    fun unitReturn()
}

@AppFunctionSchemaDefinition(name = "byteArrayReturn", version = 1, category = FAKE_CATEGORY)
interface ByteArrayReturn {
    fun byteArrayReturn(): ByteArray
}

@AppFunctionSchemaDefinition(name = "intReturn", version = 1, category = FAKE_CATEGORY)
interface IntReturn {
    fun intReturn(): Int
}

@AppFunctionSchemaDefinition(name = "longReturn", version = 1, category = FAKE_CATEGORY)
interface LongReturn {
    fun longReturn(): Long
}

@AppFunctionSchemaDefinition(name = "floatReturn", version = 1, category = FAKE_CATEGORY)
interface FloatReturn {
    fun floatReturn(): Float
}

@AppFunctionSchemaDefinition(name = "doubleReturn", version = 1, category = FAKE_CATEGORY)
interface DoubleReturn {
    fun doubleReturn(): Double
}

@AppFunctionSchemaDefinition(name = "stringReturn", version = 1, category = FAKE_CATEGORY)
interface StringReturn {
    fun stringReturn(): String
}

@AppFunctionSchemaDefinition(name = "booleanReturn", version = 1, category = FAKE_CATEGORY)
interface BooleanReturn {
    fun booleanReturn(): Boolean
}

@AppFunctionSchemaDefinition(name = "nullableByteArrayReturn", version = 1, category = FAKE_CATEGORY)
interface NullableByteArrayReturn {
    fun nullableByteArrayReturn(): ByteArray?
}

@AppFunctionSchemaDefinition(name = "nullableIntReturn", version = 1, category = FAKE_CATEGORY)
interface NullableIntReturn {
    fun nullableIntReturn(): Int?
}

@AppFunctionSchemaDefinition(name = "nullableLongReturn", version = 1, category = FAKE_CATEGORY)
interface NullableLongReturn {
    fun nullableLongReturn(): Long?
}

@AppFunctionSchemaDefinition(name = "nullableFloatReturn", version = 1, category = FAKE_CATEGORY)
interface NullableFloatReturn {
    fun nullableFloatReturn(): Float?
}

@AppFunctionSchemaDefinition(name = "nullableDoubleReturn", version = 1, category = FAKE_CATEGORY)
interface NullableDoubleReturn {
    fun nullableDoubleReturn(): Double?
}

@AppFunctionSchemaDefinition(name = "nullableStringReturn", version = 1, category = FAKE_CATEGORY)
interface NullableStringReturn {
    fun nullableStringReturn(): String?
}

@AppFunctionSchemaDefinition(name = "nullableBooleanReturn", version = 1, category = FAKE_CATEGORY)
interface NullableBooleanReturn {
    fun nullableBooleanReturn(): Boolean?
}

@AppFunctionSchemaDefinition(name = "intArrayReturn", version = 1, category = FAKE_CATEGORY)
interface IntArrayReturn {
    fun intArrayReturn(): IntArray
}

@AppFunctionSchemaDefinition(name = "longArrayReturn", version = 1, category = FAKE_CATEGORY)
interface LongArrayReturn {
    fun longArrayReturn(): LongArray
}

@AppFunctionSchemaDefinition(name = "floatArrayReturn", version = 1, category = FAKE_CATEGORY)
interface FloatArrayReturn {
    fun floatArrayReturn(): FloatArray
}

@AppFunctionSchemaDefinition(name = "doubleArrayReturn", version = 1, category = FAKE_CATEGORY)
interface DoubleArrayReturn {
    fun doubleArrayReturn(): DoubleArray
}

@AppFunctionSchemaDefinition(name = "stringListReturn", version = 1, category = FAKE_CATEGORY)
interface StringListReturn {
    fun stringListReturn(): List<String>
}

@AppFunctionSchemaDefinition(name = "booleanArrayReturn", version = 1, category = FAKE_CATEGORY)
interface BooleanArrayReturn {
    fun booleanArrayReturn(): BooleanArray
}

@AppFunctionSchemaDefinition(name = "nullableStringListReturn", version = 1, category = FAKE_CATEGORY)
interface NullableStringListReturn {
    fun nullableStringListReturn(): List<String>?
}

@AppFunctionSchemaDefinition(name = "nullableIntArrayReturn", version = 1, category = FAKE_CATEGORY)
interface NullableIntArrayReturn {
    fun nullableIntArrayReturn(): IntArray?
}

@AppFunctionSchemaDefinition(name = "nullableLongArrayReturn", version = 1, category = FAKE_CATEGORY)
interface NullableLongArrayReturn {
    fun nullableLongArrayReturn(): LongArray?
}

@AppFunctionSchemaDefinition(name = "nullableFloatArrayReturn", version = 1, category = FAKE_CATEGORY)
interface NullableFloatArrayReturn {
    fun nullableFloatArrayReturn(): FloatArray?
}

@AppFunctionSchemaDefinition(name = "nullableDoubleArrayReturn", version = 1, category = FAKE_CATEGORY)
interface NullableDoubleArrayReturn {
    fun nullableDoubleArrayReturn(): DoubleArray?
}

@AppFunctionSchemaDefinition(name = "nullableBooleanArrayReturn", version = 1, category = FAKE_CATEGORY)
interface NullableBooleanArrayReturn {
    fun nullableBooleanArrayReturn(): BooleanArray?
}

@AppFunctionSchemaDefinition(name = "nullableStringItemListReturn", version = 1, category = FAKE_CATEGORY)
interface NullableStringItemListReturn {
    fun nullableStringItemListReturn(): List<String?>
}
