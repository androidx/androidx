package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext

class DerivedSerializableOutputFunctions {
    @AppFunction
    fun simpleFunction(appFunctionContext: AppFunctionContext): DerivedSerializable {
        return DerivedSerializable("testValue", 1L)
    }

    @AppFunction
    fun simpleFunctionSerializableList(
        appFunctionContext: AppFunctionContext,
    ): List<DerivedSerializable> {
        return listOf(DerivedSerializable("testValue", 1L))
    }

    @AppFunction
    fun simpleFunctionNullableSerializableList(
        appFunctionContext: AppFunctionContext,
    ): List<DerivedSerializable>? {
        return null
    }

    @AppFunction
    fun simpleFunctionNullableSerializable(
        appFunctionContext: AppFunctionContext,
    ): DerivedSerializable? {
        return null
    }
}