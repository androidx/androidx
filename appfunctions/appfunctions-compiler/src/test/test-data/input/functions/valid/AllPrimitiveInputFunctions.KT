package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext

class AllPrimitiveInputFunctions {
    @AppFunction
    fun simpleFunctionInt(appFunctionContext: AppFunctionContext, intParam: Int) {}

    @AppFunction
    fun simpleFunctionLong(appFunctionContext: AppFunctionContext, longParam: Long) {}

    @AppFunction
    fun simpleFunctionFloat(appFunctionContext: AppFunctionContext, floatParam: Float) {}

    @AppFunction
    fun simpleFunctionDouble(appFunctionContext: AppFunctionContext, doubleParam: Double) {}

    @AppFunction
    fun simpleFunctionBoolean(appFunctionContext: AppFunctionContext, booleanParam: Boolean) {}

    @AppFunction
    fun simpleFunctionString(appFunctionContext: AppFunctionContext, stringParam: String) {}

    // Nullable single
    @AppFunction
    fun simpleFunctionNullableInt(appFunctionContext: AppFunctionContext, intParam: Int?) {}

    @AppFunction
    fun simpleFunctionNullableLong(appFunctionContext: AppFunctionContext, longParam: Long?) {}

    @AppFunction
    fun simpleFunctionNullableFloat(appFunctionContext: AppFunctionContext, floatParam: Float?) {}

    @AppFunction
    fun simpleFunctionNullableDouble(appFunctionContext: AppFunctionContext, doubleParam: Double?) {}

    @AppFunction
    fun simpleFunctionNullableBoolean(appFunctionContext: AppFunctionContext, booleanParam: Boolean?) {}

    @AppFunction
    fun simpleFunctionNullableString(appFunctionContext: AppFunctionContext, stringParam: String?) {}

    // Array
    @AppFunction
    fun simpleFunctionIntArray(appFunctionContext: AppFunctionContext, intArrayParam: IntArray) {}

    @AppFunction
    fun simpleFunctionLongArray(
        appFunctionContext: AppFunctionContext,
        longArrayParam: LongArray
    ) {}

    @AppFunction
    fun simpleFunctionFloatArray(
        appFunctionContext: AppFunctionContext,
        floatArrayParam: FloatArray
    ) {}

    @AppFunction
    fun simpleFunctionDoubleArray(
        appFunctionContext: AppFunctionContext,
        doubleArrayParam: DoubleArray
    ) {}

    @AppFunction
    fun simpleFunctionBooleanArray(
        appFunctionContext: AppFunctionContext,
        booleanArrayParam: BooleanArray
    ) {}

    @AppFunction
    fun simpleFunctionStringList(
        appFunctionContext: AppFunctionContext,
        stringListParam: List<String>
    ) {}

    // Nullable array
    @AppFunction
    fun simpleFunctionNullableIntArray(
        appFunctionContext: AppFunctionContext,
        intArrayParam: IntArray?
    ) {}

    @AppFunction
    fun simpleFunctionNullableLongArray(
        appFunctionContext: AppFunctionContext,
        longArrayParam: LongArray?
    ) {}

    @AppFunction
    fun simpleFunctionNullableFloatArray(
        appFunctionContext: AppFunctionContext,
        floatArrayParam: FloatArray?
    ) {}

    @AppFunction
    fun simpleFunctionNullableDoubleArray(
        appFunctionContext: AppFunctionContext,
        doubleArrayParam: DoubleArray?
    ) {}

    @AppFunction
    fun simpleFunctionNullableBooleanArray(
        appFunctionContext: AppFunctionContext,
        booleanArrayParam: BooleanArray?
    ) {}

    @AppFunction
    fun simpleFunctionNullableStringList(
        appFunctionContext: AppFunctionContext,
        stringListParam: List<String>?
    ) {}
}
