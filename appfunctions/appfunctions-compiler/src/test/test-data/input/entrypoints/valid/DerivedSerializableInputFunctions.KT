package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("DerivedSerializableInputFunctionsService", "app_functions_v2")
abstract class DerivedSerializableInputFunctions : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithDerivedSerializable(
        serializable: DerivedSerializable
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithDerivedSerializableList(
        serializable: List<DerivedSerializable>,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithNullableDerivedSerializableList(
        serializable: List<DerivedSerializable>?,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithNullableDerivedSerializable(
        serializable: DerivedSerializable?,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithDerivedSerializableListItemNullable(
        serializable: List<DerivedSerializable?>,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithBothDerivedSerializableListAndListItemNullable(
        serializable: List<DerivedSerializable?>?,
    ) {}
}