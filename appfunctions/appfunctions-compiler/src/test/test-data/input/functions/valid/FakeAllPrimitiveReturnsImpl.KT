package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class UnitReturnImpl : UnitReturn {
    @AppFunctionDeclaration override fun unitReturn(appFunctionContext: AppFunctionContext) {}
}

class ByteArrayReturnImpl : ByteArrayReturn {
    @AppFunctionDeclaration override fun byteArrayReturn(appFunctionContext: AppFunctionContext): ByteArray = byteArrayOf()
}

class IntReturnImpl : IntReturn {
    @AppFunctionDeclaration override fun intReturn(appFunctionContext: AppFunctionContext): Int = 0
}

class LongReturnImpl : LongReturn {
    @AppFunctionDeclaration override fun longReturn(appFunctionContext: AppFunctionContext): Long = 0L
}

class FloatReturnImpl : FloatReturn {
    @AppFunctionDeclaration override fun floatReturn(appFunctionContext: AppFunctionContext): Float = 0.0f
}

class DoubleReturnImpl : DoubleReturn {
    @AppFunctionDeclaration override fun doubleReturn(appFunctionContext: AppFunctionContext): Double = 0.0
}

class StringReturnImpl : StringReturn {
    @AppFunctionDeclaration override fun stringReturn(appFunctionContext: AppFunctionContext): String = ""
}

class BooleanReturnImpl : BooleanReturn {
    @AppFunctionDeclaration override fun booleanReturn(appFunctionContext: AppFunctionContext): Boolean = false
}

class NullableByteArrayReturnImpl : NullableByteArrayReturn {
    @AppFunctionDeclaration override fun nullableByteArrayReturn(appFunctionContext: AppFunctionContext): ByteArray? = null
}

class NullableIntReturnImpl : NullableIntReturn {
    @AppFunctionDeclaration override fun nullableIntReturn(appFunctionContext: AppFunctionContext): Int? = null
}

class NullableLongReturnImpl : NullableLongReturn {
    @AppFunctionDeclaration override fun nullableLongReturn(appFunctionContext: AppFunctionContext): Long? = null
}

class NullableFloatReturnImpl : NullableFloatReturn {
    @AppFunctionDeclaration override fun nullableFloatReturn(appFunctionContext: AppFunctionContext): Float? = null
}

class NullableDoubleReturnImpl : NullableDoubleReturn {
    @AppFunctionDeclaration override fun nullableDoubleReturn(appFunctionContext: AppFunctionContext): Double? = null
}

class NullableStringReturnImpl : NullableStringReturn {
    @AppFunctionDeclaration override fun nullableStringReturn(appFunctionContext: AppFunctionContext): String? = null
}

class NullableBooleanReturnImpl : NullableBooleanReturn {
    @AppFunctionDeclaration override fun nullableBooleanReturn(appFunctionContext: AppFunctionContext): Boolean? = null
}

class IntArrayReturnImpl : IntArrayReturn {
    @AppFunctionDeclaration override fun intArrayReturn(appFunctionContext: AppFunctionContext): IntArray = intArrayOf()
}

class LongArrayReturnImpl : LongArrayReturn {
    @AppFunctionDeclaration override fun longArrayReturn(appFunctionContext: AppFunctionContext): LongArray = longArrayOf()
}

class FloatArrayReturnImpl : FloatArrayReturn {
    @AppFunctionDeclaration override fun floatArrayReturn(appFunctionContext: AppFunctionContext): FloatArray = floatArrayOf()
}

class DoubleArrayReturnImpl : DoubleArrayReturn {
    @AppFunctionDeclaration override fun doubleArrayReturn(appFunctionContext: AppFunctionContext): DoubleArray = doubleArrayOf()
}

class StringListReturnImpl : StringListReturn {
    @AppFunctionDeclaration override fun stringListReturn(appFunctionContext: AppFunctionContext): List<String> = listOf()
}

class BooleanArrayReturnImpl : BooleanArrayReturn {
    @AppFunctionDeclaration override fun booleanArrayReturn(appFunctionContext: AppFunctionContext): BooleanArray = booleanArrayOf()
}

class NullableStringListReturnImpl : NullableStringListReturn {
    @AppFunctionDeclaration override fun nullableStringListReturn(appFunctionContext: AppFunctionContext): List<String>? = null
}

class NullableIntArrayReturnImpl : NullableIntArrayReturn {
    @AppFunctionDeclaration override fun nullableIntArrayReturn(appFunctionContext: AppFunctionContext): IntArray? = null
}

class NullableLongArrayReturnImpl : NullableLongArrayReturn {
    @AppFunctionDeclaration override fun nullableLongArrayReturn(appFunctionContext: AppFunctionContext): LongArray? = null
}

class NullableFloatArrayReturnImpl : NullableFloatArrayReturn {
    @AppFunctionDeclaration override fun nullableFloatArrayReturn(appFunctionContext: AppFunctionContext): FloatArray? = null
}

class NullableDoubleArrayReturnImpl : NullableDoubleArrayReturn {
    @AppFunctionDeclaration override fun nullableDoubleArrayReturn(appFunctionContext: AppFunctionContext): DoubleArray? = null
}

class NullableBooleanArrayReturnImpl : NullableBooleanArrayReturn {
    @AppFunctionDeclaration override fun nullableBooleanArrayReturn(appFunctionContext: AppFunctionContext): BooleanArray? = null
}

class NullableStringItemListReturnImpl : NullableStringItemListReturn {
    @AppFunctionDeclaration override fun nullableStringItemListReturn(appFunctionContext: AppFunctionContext): List<String?> = emptyList()
}