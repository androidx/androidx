package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class DerivedSerializableOutputFunctions {
    @AppFunctionDeclaration
    fun simpleFunction(appFunctionContext: AppFunctionContext): DerivedSerializable {
        return DerivedSerializable("testValue", 1L)
    }

    @AppFunctionDeclaration
    fun simpleFunctionSerializableList(
        appFunctionContext: AppFunctionContext,
    ): List<DerivedSerializable> {
        return listOf(DerivedSerializable("testValue", 1L))
    }

    @AppFunctionDeclaration
    fun simpleFunctionNullableSerializableList(
        appFunctionContext: AppFunctionContext,
    ): List<DerivedSerializable>? {
        return null
    }

    @AppFunctionDeclaration
    fun simpleFunctionNullableSerializable(
        appFunctionContext: AppFunctionContext,
    ): DerivedSerializable? {
        return null
    }
}