package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class DerivedSerializableInputFunctions {
    @AppFunctionDeclaration
    fun simpleFunctionWithDerivedSerializable(
        appFunctionContext: AppFunctionContext,
        serializable: DerivedSerializable
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionWithDerivedSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<DerivedSerializable>,
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionWithNullableDerivedSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<DerivedSerializable>?,
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionWithNullableDerivedSerializable(
        appFunctionContext: AppFunctionContext,
        serializable: DerivedSerializable?,
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionWithDerivedSerializableListItemNullable(
        appFunctionContext: AppFunctionContext,
        serializable: List<DerivedSerializable?>,
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionWithBothDerivedSerializableListAndListItemNullable(
        appFunctionContext: AppFunctionContext,
        serializable: List<DerivedSerializable?>?,
    ) {}
}