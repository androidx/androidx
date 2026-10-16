package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("RecursiveSerializableInputFunctionsService", "app_functions_v2")
abstract class RecursiveSerializableInputFunctions : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunction(
        serializable: RecursiveSerializable
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionSerializableList(
        serializable: List<RecursiveSerializable>
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableSerializableList(
        serializable: List<RecursiveSerializable>?
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableSerializable(
        serializable: RecursiveSerializable?
    ) {}
}
