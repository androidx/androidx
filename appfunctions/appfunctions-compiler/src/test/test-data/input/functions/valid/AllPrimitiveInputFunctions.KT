package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class AllPrimitiveInputFunctions {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionInt(appFunctionContext: AppFunctionContext, intParam: Int) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionLong(appFunctionContext: AppFunctionContext, longParam: Long) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionFloat(appFunctionContext: AppFunctionContext, floatParam: Float) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionDouble(appFunctionContext: AppFunctionContext, doubleParam: Double) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionBoolean(appFunctionContext: AppFunctionContext, booleanParam: Boolean) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionString(appFunctionContext: AppFunctionContext, stringParam: String) {}

    // Nullable single
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableInt(appFunctionContext: AppFunctionContext, intParam: Int?) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableLong(appFunctionContext: AppFunctionContext, longParam: Long?) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableFloat(appFunctionContext: AppFunctionContext, floatParam: Float?) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableDouble(appFunctionContext: AppFunctionContext, doubleParam: Double?) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableBoolean(appFunctionContext: AppFunctionContext, booleanParam: Boolean?) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableString(appFunctionContext: AppFunctionContext, stringParam: String?) {}

    // Array
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionIntArray(appFunctionContext: AppFunctionContext, intArrayParam: IntArray) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionLongArray(
        appFunctionContext: AppFunctionContext,
        longArrayParam: LongArray
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionFloatArray(
        appFunctionContext: AppFunctionContext,
        floatArrayParam: FloatArray
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionDoubleArray(
        appFunctionContext: AppFunctionContext,
        doubleArrayParam: DoubleArray
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionBooleanArray(
        appFunctionContext: AppFunctionContext,
        booleanArrayParam: BooleanArray
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionStringList(
        appFunctionContext: AppFunctionContext,
        stringListParam: List<String>
    ) {}

    // Nullable array
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableIntArray(
        appFunctionContext: AppFunctionContext,
        intArrayParam: IntArray?
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableLongArray(
        appFunctionContext: AppFunctionContext,
        longArrayParam: LongArray?
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableFloatArray(
        appFunctionContext: AppFunctionContext,
        floatArrayParam: FloatArray?
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableDoubleArray(
        appFunctionContext: AppFunctionContext,
        doubleArrayParam: DoubleArray?
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableBooleanArray(
        appFunctionContext: AppFunctionContext,
        booleanArrayParam: BooleanArray?
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableStringList(
        appFunctionContext: AppFunctionContext,
        stringListParam: List<String>?
    ) {}
}
