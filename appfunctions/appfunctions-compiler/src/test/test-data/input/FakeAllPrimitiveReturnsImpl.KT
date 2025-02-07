package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext

class FakeAllPrimitiveReturnsImpl : FakeAllPrimitiveReturns {

    @AppFunction override fun unitReturn(appFunctionContext: AppFunctionContext) {}

    @AppFunction
    override fun byteArrayReturn(appFunctionContext: AppFunctionContext): ByteArray {
        return byteArrayOf()
    }

    @AppFunction
    override fun intReturn(appFunctionContext: AppFunctionContext): Int {
        return 0
    }

    @AppFunction
    override fun longReturn(appFunctionContext: AppFunctionContext): Long {
        return 0L
    }

    @AppFunction
    override fun floatReturn(appFunctionContext: AppFunctionContext): Float {
        return 0.0f
    }

    @AppFunction
    override fun doubleReturn(appFunctionContext: AppFunctionContext): Double {
        return 0.0
    }

    @AppFunction
    override fun stringReturn(appFunctionContext: AppFunctionContext): String {
        return ""
    }

    @AppFunction
    override fun booleanReturn(appFunctionContext: AppFunctionContext): Boolean {
        return false
    }

    @AppFunction
    override fun nullableByteArrayReturn(appFunctionContext: AppFunctionContext): ByteArray? {
        return null
    }

    @AppFunction
    override fun nullableIntReturn(appFunctionContext: AppFunctionContext): Int? {
        return null
    }

    @AppFunction
    override fun nullableLongReturn(appFunctionContext: AppFunctionContext): Long? {
        return null
    }

    @AppFunction
    override fun nullableFloatReturn(appFunctionContext: AppFunctionContext): Float? {
        return null
    }

    @AppFunction
    override fun nullableDoubleReturn(appFunctionContext: AppFunctionContext): Double? {
        return null
    }

    @AppFunction
    override fun nullableStringReturn(appFunctionContext: AppFunctionContext): String? {
        return null
    }

    @AppFunction
    override fun nullableBooleanReturn(appFunctionContext: AppFunctionContext): Boolean? {
        return null
    }

    @AppFunction
    override fun intArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): IntArray = intArrayOf()

    @AppFunction
    override fun byteArrayListReturn(
        appFunctionContext: AppFunctionContext,
    ): List<ByteArray> = listOf()

    @AppFunction
    override fun longArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): LongArray = longArrayOf()

    @AppFunction
    override fun floatArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): FloatArray = floatArrayOf()

    @AppFunction
    override fun doubleArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): DoubleArray = doubleArrayOf()

    @AppFunction
    override fun stringListReturn(
        appFunctionContext: AppFunctionContext,
    ): List<String> = listOf()

    @AppFunction
    override fun booleanArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): BooleanArray = booleanArrayOf()

    @AppFunction
    override fun nullableStringListReturn(
        appFunctionContext: AppFunctionContext,
    ): List<String>? = null

    @AppFunction
    override fun nullableByteArrayListReturn(
        appFunctionContext: AppFunctionContext,
    ): List<ByteArray>? = null

    @AppFunction
    override fun nullableIntArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): IntArray? = null

    @AppFunction
    override fun nullableLongArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): LongArray? = null

    @AppFunction
    override fun nullableDoubleArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): DoubleArray? = null

    @AppFunction
    override fun nullableBooleanArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): BooleanArray? = null

    @AppFunction
    override fun nullableFloatArrayReturn(
        appFunctionContext: AppFunctionContext,
    ): FloatArray? = null
}
