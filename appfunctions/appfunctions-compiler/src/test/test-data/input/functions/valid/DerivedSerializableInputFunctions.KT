package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext

class DerivedSerializableInputFunctions {
    @AppFunction
    fun simpleFunctionWithDerivedSerializable(
        appFunctionContext: AppFunctionContext,
        serializable: DerivedSerializable
    ) {}

    @AppFunction
    fun simpleFunctionWithDerivedSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<DerivedSerializable>,
    ) {}

    @AppFunction
    fun simpleFunctionWithNullableDerivedSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<DerivedSerializable>?,
    ) {}

    @AppFunction
    fun simpleFunctionWithNullableDerivedSerializable(
        appFunctionContext: AppFunctionContext,
        serializable: DerivedSerializable?,
    ) {}

    @AppFunction
    fun simpleFunctionWithDerivedSerializableListItemNullable(
        appFunctionContext: AppFunctionContext,
        serializable: List<DerivedSerializable?>,
    ) {}

    @AppFunction
    fun simpleFunctionWithBothDerivedSerializableListAndListItemNullable(
        appFunctionContext: AppFunctionContext,
        serializable: List<DerivedSerializable?>?,
    ) {}
}