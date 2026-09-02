package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class AllPrimitiveInputFunctions {
    @AppFunctionDeclaration
    fun simpleFunctionInt(appFunctionContext: AppFunctionContext, intParam: Int) {}

    @AppFunctionDeclaration
    fun simpleFunctionLong(appFunctionContext: AppFunctionContext, longParam: Long) {}

    @AppFunctionDeclaration
    fun simpleFunctionFloat(appFunctionContext: AppFunctionContext, floatParam: Float) {}

    @AppFunctionDeclaration
    fun simpleFunctionDouble(appFunctionContext: AppFunctionContext, doubleParam: Double) {}

    @AppFunctionDeclaration
    fun simpleFunctionBoolean(appFunctionContext: AppFunctionContext, booleanParam: Boolean) {}

    @AppFunctionDeclaration
    fun simpleFunctionString(appFunctionContext: AppFunctionContext, stringParam: String) {}

    // Nullable single
    @AppFunctionDeclaration
    fun simpleFunctionNullableInt(appFunctionContext: AppFunctionContext, intParam: Int?) {}

    @AppFunctionDeclaration
    fun simpleFunctionNullableLong(appFunctionContext: AppFunctionContext, longParam: Long?) {}

    @AppFunctionDeclaration
    fun simpleFunctionNullableFloat(appFunctionContext: AppFunctionContext, floatParam: Float?) {}

    @AppFunctionDeclaration
    fun simpleFunctionNullableDouble(appFunctionContext: AppFunctionContext, doubleParam: Double?) {}

    @AppFunctionDeclaration
    fun simpleFunctionNullableBoolean(appFunctionContext: AppFunctionContext, booleanParam: Boolean?) {}

    @AppFunctionDeclaration
    fun simpleFunctionNullableString(appFunctionContext: AppFunctionContext, stringParam: String?) {}

    // Array
    @AppFunctionDeclaration
    fun simpleFunctionIntArray(appFunctionContext: AppFunctionContext, intArrayParam: IntArray) {}

    @AppFunctionDeclaration
    fun simpleFunctionLongArray(
        appFunctionContext: AppFunctionContext,
        longArrayParam: LongArray
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionFloatArray(
        appFunctionContext: AppFunctionContext,
        floatArrayParam: FloatArray
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionDoubleArray(
        appFunctionContext: AppFunctionContext,
        doubleArrayParam: DoubleArray
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionBooleanArray(
        appFunctionContext: AppFunctionContext,
        booleanArrayParam: BooleanArray
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionStringList(
        appFunctionContext: AppFunctionContext,
        stringListParam: List<String>
    ) {}

    // Nullable array
    @AppFunctionDeclaration
    fun simpleFunctionNullableIntArray(
        appFunctionContext: AppFunctionContext,
        intArrayParam: IntArray?
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionNullableLongArray(
        appFunctionContext: AppFunctionContext,
        longArrayParam: LongArray?
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionNullableFloatArray(
        appFunctionContext: AppFunctionContext,
        floatArrayParam: FloatArray?
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionNullableDoubleArray(
        appFunctionContext: AppFunctionContext,
        doubleArrayParam: DoubleArray?
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionNullableBooleanArray(
        appFunctionContext: AppFunctionContext,
        booleanArrayParam: BooleanArray?
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionNullableStringList(
        appFunctionContext: AppFunctionContext,
        stringListParam: List<String>?
    ) {}
}
