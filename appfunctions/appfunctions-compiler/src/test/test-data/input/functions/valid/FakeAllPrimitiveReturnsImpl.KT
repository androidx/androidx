package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class UnitReturnImpl : UnitReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun unitReturn(appFunctionContext: AppFunctionContext) {}
}

class ByteArrayReturnImpl : ByteArrayReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun byteArrayReturn(appFunctionContext: AppFunctionContext): ByteArray = byteArrayOf()
}

class IntReturnImpl : IntReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun intReturn(appFunctionContext: AppFunctionContext): Int = 0
}

class LongReturnImpl : LongReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun longReturn(appFunctionContext: AppFunctionContext): Long = 0L
}

class FloatReturnImpl : FloatReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun floatReturn(appFunctionContext: AppFunctionContext): Float = 0.0f
}

class DoubleReturnImpl : DoubleReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun doubleReturn(appFunctionContext: AppFunctionContext): Double = 0.0
}

class StringReturnImpl : StringReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun stringReturn(appFunctionContext: AppFunctionContext): String = ""
}

class BooleanReturnImpl : BooleanReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun booleanReturn(appFunctionContext: AppFunctionContext): Boolean = false
}

class NullableByteArrayReturnImpl : NullableByteArrayReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun nullableByteArrayReturn(appFunctionContext: AppFunctionContext): ByteArray? = null
}

class NullableIntReturnImpl : NullableIntReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun nullableIntReturn(appFunctionContext: AppFunctionContext): Int? = null
}

class NullableLongReturnImpl : NullableLongReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun nullableLongReturn(appFunctionContext: AppFunctionContext): Long? = null
}

class NullableFloatReturnImpl : NullableFloatReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun nullableFloatReturn(appFunctionContext: AppFunctionContext): Float? = null
}

class NullableDoubleReturnImpl : NullableDoubleReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun nullableDoubleReturn(appFunctionContext: AppFunctionContext): Double? = null
}

class NullableStringReturnImpl : NullableStringReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun nullableStringReturn(appFunctionContext: AppFunctionContext): String? = null
}

class NullableBooleanReturnImpl : NullableBooleanReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun nullableBooleanReturn(appFunctionContext: AppFunctionContext): Boolean? = null
}

class IntArrayReturnImpl : IntArrayReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun intArrayReturn(appFunctionContext: AppFunctionContext): IntArray = intArrayOf()
}

class LongArrayReturnImpl : LongArrayReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun longArrayReturn(appFunctionContext: AppFunctionContext): LongArray = longArrayOf()
}

class FloatArrayReturnImpl : FloatArrayReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun floatArrayReturn(appFunctionContext: AppFunctionContext): FloatArray = floatArrayOf()
}

class DoubleArrayReturnImpl : DoubleArrayReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun doubleArrayReturn(appFunctionContext: AppFunctionContext): DoubleArray = doubleArrayOf()
}

class StringListReturnImpl : StringListReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun stringListReturn(appFunctionContext: AppFunctionContext): List<String> = listOf()
}

class BooleanArrayReturnImpl : BooleanArrayReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun booleanArrayReturn(appFunctionContext: AppFunctionContext): BooleanArray = booleanArrayOf()
}

class NullableStringListReturnImpl : NullableStringListReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun nullableStringListReturn(appFunctionContext: AppFunctionContext): List<String>? = null
}

class NullableIntArrayReturnImpl : NullableIntArrayReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun nullableIntArrayReturn(appFunctionContext: AppFunctionContext): IntArray? = null
}

class NullableLongArrayReturnImpl : NullableLongArrayReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun nullableLongArrayReturn(appFunctionContext: AppFunctionContext): LongArray? = null
}

class NullableFloatArrayReturnImpl : NullableFloatArrayReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun nullableFloatArrayReturn(appFunctionContext: AppFunctionContext): FloatArray? = null
}

class NullableDoubleArrayReturnImpl : NullableDoubleArrayReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun nullableDoubleArrayReturn(appFunctionContext: AppFunctionContext): DoubleArray? = null
}

class NullableBooleanArrayReturnImpl : NullableBooleanArrayReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun nullableBooleanArrayReturn(appFunctionContext: AppFunctionContext): BooleanArray? = null
}

class NullableStringItemListReturnImpl : NullableStringItemListReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun nullableStringItemListReturn(appFunctionContext: AppFunctionContext): List<String?> = emptyList()
}