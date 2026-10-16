package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("NestedDerivedSerializableInputFunctionsService", "app_functions_v2")
abstract class NestedDerivedSerializableInputFunctions : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun functionWithDerivedSerializable(
        serializable: NestedDerivedSerializable
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun functionWithDerivedSerializableList(
        serializable: List<NestedDerivedSerializable>,
    ) {}
}
