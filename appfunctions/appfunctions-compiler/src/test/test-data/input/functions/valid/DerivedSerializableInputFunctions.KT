package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class DerivedSerializableInputFunctions {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithDerivedSerializable(
        appFunctionContext: AppFunctionContext,
        serializable: DerivedSerializable
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithDerivedSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<DerivedSerializable>,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithNullableDerivedSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<DerivedSerializable>?,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithNullableDerivedSerializable(
        appFunctionContext: AppFunctionContext,
        serializable: DerivedSerializable?,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithDerivedSerializableListItemNullable(
        appFunctionContext: AppFunctionContext,
        serializable: List<DerivedSerializable?>,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithBothDerivedSerializableListAndListItemNullable(
        appFunctionContext: AppFunctionContext,
        serializable: List<DerivedSerializable?>?,
    ) {}
}