package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class RecursiveSerializableInputFunctions {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunction(
        appFunctionContext: AppFunctionContext,
        serializable: RecursiveSerializable
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<RecursiveSerializable>
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<RecursiveSerializable>?
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableSerializable(
        appFunctionContext: AppFunctionContext,
        serializable: RecursiveSerializable?
    ) {}
}
