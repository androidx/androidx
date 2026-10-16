package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("AllPrimitiveInputFunctionsService", "app_functions_v2")
abstract class AllPrimitiveInputFunctions : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionInt(intParam: Int) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionLong(longParam: Long) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionFloat(floatParam: Float) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionDouble(doubleParam: Double) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionBoolean(booleanParam: Boolean) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionString(stringParam: String) {}

    // Nullable single
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableInt(intParam: Int?) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableLong(longParam: Long?) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableFloat(floatParam: Float?) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableDouble(doubleParam: Double?) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableBoolean(booleanParam: Boolean?) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableString(stringParam: String?) {}

    // Array
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionIntArray(intArrayParam: IntArray) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionLongArray(longArrayParam: LongArray) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionFloatArray(floatArrayParam: FloatArray) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionDoubleArray(doubleArrayParam: DoubleArray) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionBooleanArray(booleanArrayParam: BooleanArray) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionStringList(stringListParam: List<String>) {}

    // Nullable array
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableIntArray(intArrayParam: IntArray?) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableLongArray(longArrayParam: LongArray?) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableFloatArray(floatArrayParam: FloatArray?) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableDoubleArray(doubleArrayParam: DoubleArray?) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableBooleanArray(booleanArrayParam: BooleanArray?) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableStringList(stringListParam: List<String>?) {}
}
