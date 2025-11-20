package com.testdata

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSchemaDefinition

private const val FAKE_CATEGORY = "fake_schema_category"

@AppFunctionSchemaDefinition(name = "noArg", version = 1, category = FAKE_CATEGORY)
interface FakeNoArg {
    fun noArg(appFunctionContext: AppFunctionContext)
}

@AppFunctionSchemaDefinition(name = "allPrimitiveParams", version = 1, category = FAKE_CATEGORY)
interface FakeAllPrimitiveParams {
    fun allPrimitiveParams(
        appFunctionContext: AppFunctionContext,
        arg1: Int,
        arg2: Long,
        arg3: Float,
        arg4: Double,
        arg5: String,
        arg6: Boolean,
    )

    fun allPrimitiveArrayParams(
        appFunctionContext: AppFunctionContext,
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
        appFunctionContext: AppFunctionContext,
        arg1: Int? = null,
        arg2: Long? = null,
        arg3: Float? = null,
        arg4: Double? = null,
        arg5: String? = null,
        arg6: Boolean? = null,
    )

    fun allNullablePrimitiveArrayParamsWithDefault(
        appFunctionContext: AppFunctionContext,
        arg1: IntArray? = intArrayOf(),
        arg2: LongArray? = longArrayOf(),
        arg3: FloatArray? = floatArrayOf(),
        arg4: DoubleArray? = doubleArrayOf(),
        arg5: List<String> = emptyList(),
        arg6: BooleanArray? = booleanArrayOf(),
        arg7: ByteArray? = byteArrayOf(),
    )
}

@AppFunctionSchemaDefinition(name = "allPrimitiveReturns", version = 1, category = FAKE_CATEGORY)
interface FakeAllPrimitiveReturns {
    fun unitReturn(
        appFunctionContext: AppFunctionContext,
    )

    fun byteArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): ByteArray

    fun intReturn(
        appFunctionContext: AppFunctionContext,
    ): Int

    fun longReturn(
        appFunctionContext: AppFunctionContext,
    ): Long

    fun floatReturn(
        appFunctionContext: AppFunctionContext,
    ): Float

    fun doubleReturn(
        appFunctionContext: AppFunctionContext,
    ): Double

    fun stringReturn(
        appFunctionContext: AppFunctionContext,
    ): String

    fun booleanReturn(
        appFunctionContext: AppFunctionContext,
    ): Boolean

    fun nullableByteArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): ByteArray?

    fun nullableIntReturn(
        appFunctionContext: AppFunctionContext,
    ): Int?

    fun nullableLongReturn(
        appFunctionContext: AppFunctionContext,
    ): Long?

    fun nullableFloatReturn(
        appFunctionContext: AppFunctionContext,
    ): Float?

    fun nullableDoubleReturn(
        appFunctionContext: AppFunctionContext,
    ): Double?

    fun nullableStringReturn(
        appFunctionContext: AppFunctionContext,
    ): String?

    fun nullableBooleanReturn(
        appFunctionContext: AppFunctionContext,
    ): Boolean?

    fun intArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): IntArray

    fun longArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): LongArray

    fun floatArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): FloatArray

    fun doubleArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): DoubleArray

    fun stringListReturn(
        appFunctionContext: AppFunctionContext,
    ): List<String>

    fun booleanArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): BooleanArray

    fun nullableStringListReturn(
        appFunctionContext: AppFunctionContext,
    ): List<String>?

    fun nullableIntArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): IntArray?

    fun nullableLongArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): LongArray?

    fun nullableFloatArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): FloatArray?

    fun nullableDoubleArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): DoubleArray?

    fun nullableBooleanArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): BooleanArray?

    fun nullableStringItemListReturn(
        appFunctionContext: AppFunctionContext,
    ): List<String?>
}
