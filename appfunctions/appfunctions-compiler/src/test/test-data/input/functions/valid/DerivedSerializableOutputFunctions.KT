package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class DerivedSerializableOutputFunctions {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunction(appFunctionContext: AppFunctionContext): DerivedSerializable {
        return DerivedSerializable("testValue", 1L)
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionSerializableList(
        appFunctionContext: AppFunctionContext,
    ): List<DerivedSerializable> {
        return listOf(DerivedSerializable("testValue", 1L))
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableSerializableList(
        appFunctionContext: AppFunctionContext,
    ): List<DerivedSerializable>? {
        return null
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableSerializable(
        appFunctionContext: AppFunctionContext,
    ): DerivedSerializable? {
        return null
    }
}