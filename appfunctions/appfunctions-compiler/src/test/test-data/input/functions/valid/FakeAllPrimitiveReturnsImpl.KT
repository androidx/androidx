package com.testdata

import androidx.appfunctions.service.AppFunction
import androidx.appfunctions.AppFunctionContext

class UnitReturnImpl : UnitReturn {
    @AppFunction override fun unitReturn(appFunctionContext: AppFunctionContext) {}
}

class ByteArrayReturnImpl : ByteArrayReturn {
    @AppFunction override fun byteArrayReturn(appFunctionContext: AppFunctionContext): ByteArray = byteArrayOf()
}

class IntReturnImpl : IntReturn {
    @AppFunction override fun intReturn(appFunctionContext: AppFunctionContext): Int = 0
}

class LongReturnImpl : LongReturn {
    @AppFunction override fun longReturn(appFunctionContext: AppFunctionContext): Long = 0L
}

class FloatReturnImpl : FloatReturn {
    @AppFunction override fun floatReturn(appFunctionContext: AppFunctionContext): Float = 0.0f
}

class DoubleReturnImpl : DoubleReturn {
    @AppFunction override fun doubleReturn(appFunctionContext: AppFunctionContext): Double = 0.0
}

class StringReturnImpl : StringReturn {
    @AppFunction override fun stringReturn(appFunctionContext: AppFunctionContext): String = ""
}

class BooleanReturnImpl : BooleanReturn {
    @AppFunction override fun booleanReturn(appFunctionContext: AppFunctionContext): Boolean = false
}

class NullableByteArrayReturnImpl : NullableByteArrayReturn {
    @AppFunction override fun nullableByteArrayReturn(appFunctionContext: AppFunctionContext): ByteArray? = null
}

class NullableIntReturnImpl : NullableIntReturn {
    @AppFunction override fun nullableIntReturn(appFunctionContext: AppFunctionContext): Int? = null
}

class NullableLongReturnImpl : NullableLongReturn {
    @AppFunction override fun nullableLongReturn(appFunctionContext: AppFunctionContext): Long? = null
}

class NullableFloatReturnImpl : NullableFloatReturn {
    @AppFunction override fun nullableFloatReturn(appFunctionContext: AppFunctionContext): Float? = null
}

class NullableDoubleReturnImpl : NullableDoubleReturn {
    @AppFunction override fun nullableDoubleReturn(appFunctionContext: AppFunctionContext): Double? = null
}

class NullableStringReturnImpl : NullableStringReturn {
    @AppFunction override fun nullableStringReturn(appFunctionContext: AppFunctionContext): String? = null
}

class NullableBooleanReturnImpl : NullableBooleanReturn {
    @AppFunction override fun nullableBooleanReturn(appFunctionContext: AppFunctionContext): Boolean? = null
}

class IntArrayReturnImpl : IntArrayReturn {
    @AppFunction override fun intArrayReturn(appFunctionContext: AppFunctionContext): IntArray = intArrayOf()
}

class LongArrayReturnImpl : LongArrayReturn {
    @AppFunction override fun longArrayReturn(appFunctionContext: AppFunctionContext): LongArray = longArrayOf()
}

class FloatArrayReturnImpl : FloatArrayReturn {
    @AppFunction override fun floatArrayReturn(appFunctionContext: AppFunctionContext): FloatArray = floatArrayOf()
}

class DoubleArrayReturnImpl : DoubleArrayReturn {
    @AppFunction override fun doubleArrayReturn(appFunctionContext: AppFunctionContext): DoubleArray = doubleArrayOf()
}

class StringListReturnImpl : StringListReturn {
    @AppFunction override fun stringListReturn(appFunctionContext: AppFunctionContext): List<String> = listOf()
}

class BooleanArrayReturnImpl : BooleanArrayReturn {
    @AppFunction override fun booleanArrayReturn(appFunctionContext: AppFunctionContext): BooleanArray = booleanArrayOf()
}

class NullableStringListReturnImpl : NullableStringListReturn {
    @AppFunction override fun nullableStringListReturn(appFunctionContext: AppFunctionContext): List<String>? = null
}

class NullableIntArrayReturnImpl : NullableIntArrayReturn {
    @AppFunction override fun nullableIntArrayReturn(appFunctionContext: AppFunctionContext): IntArray? = null
}

class NullableLongArrayReturnImpl : NullableLongArrayReturn {
    @AppFunction override fun nullableLongArrayReturn(appFunctionContext: AppFunctionContext): LongArray? = null
}

class NullableFloatArrayReturnImpl : NullableFloatArrayReturn {
    @AppFunction override fun nullableFloatArrayReturn(appFunctionContext: AppFunctionContext): FloatArray? = null
}

class NullableDoubleArrayReturnImpl : NullableDoubleArrayReturn {
    @AppFunction override fun nullableDoubleArrayReturn(appFunctionContext: AppFunctionContext): DoubleArray? = null
}

class NullableBooleanArrayReturnImpl : NullableBooleanArrayReturn {
    @AppFunction override fun nullableBooleanArrayReturn(appFunctionContext: AppFunctionContext): BooleanArray? = null
}

class NullableStringItemListReturnImpl : NullableStringItemListReturn {
    @AppFunction override fun nullableStringItemListReturn(appFunctionContext: AppFunctionContext): List<String?> = emptyList()
}