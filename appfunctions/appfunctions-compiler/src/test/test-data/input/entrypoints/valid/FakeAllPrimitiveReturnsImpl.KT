package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("FakeAllPrimitiveReturnsService", "app_functions_v2")
abstract class FakeAllPrimitiveReturnsImpl :
    AppFunctionService(),
    UnitReturn,
    ByteArrayReturn,
    IntReturn,
    LongReturn,
    FloatReturn,
    DoubleReturn,
    StringReturn,
    BooleanReturn,
    NullableByteArrayReturn,
    NullableIntReturn,
    NullableLongReturn,
    NullableFloatReturn,
    NullableDoubleReturn,
    NullableStringReturn,
    NullableBooleanReturn,
    IntArrayReturn,
    LongArrayReturn,
    FloatArrayReturn,
    DoubleArrayReturn,
    StringListReturn,
    BooleanArrayReturn,
    NullableStringListReturn,
    NullableIntArrayReturn,
    NullableLongArrayReturn,
    NullableFloatArrayReturn,
    NullableDoubleArrayReturn,
    NullableBooleanArrayReturn,
    NullableStringItemListReturn {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun unitReturn() {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun byteArrayReturn(): ByteArray = byteArrayOf()

    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun intReturn(): Int = 0

    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun longReturn(): Long = 0L

    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun floatReturn(): Float = 0.0f

    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun doubleReturn(): Double = 0.0

    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun stringReturn(): String = ""

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun booleanReturn(): Boolean = false

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun nullableByteArrayReturn(): ByteArray? = null

    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun nullableIntReturn(): Int? = null

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun nullableLongReturn(): Long? = null

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun nullableFloatReturn(): Float? = null

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun nullableDoubleReturn(): Double? = null

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun nullableStringReturn(): String? = null

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun nullableBooleanReturn(): Boolean? = null

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun intArrayReturn(): IntArray = intArrayOf()

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun longArrayReturn(): LongArray = longArrayOf()

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun floatArrayReturn(): FloatArray = floatArrayOf()

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun doubleArrayReturn(): DoubleArray = doubleArrayOf()

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun stringListReturn(): List<String> = listOf()

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun booleanArrayReturn(): BooleanArray = booleanArrayOf()

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun nullableStringListReturn(): List<String>? = null

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun nullableIntArrayReturn(): IntArray? = null

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun nullableLongArrayReturn(): LongArray? = null

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun nullableFloatArrayReturn(): FloatArray? = null

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun nullableDoubleArrayReturn(): DoubleArray? = null

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun nullableBooleanArrayReturn(): BooleanArray? = null

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun nullableStringItemListReturn(): List<String?> = emptyList()
}