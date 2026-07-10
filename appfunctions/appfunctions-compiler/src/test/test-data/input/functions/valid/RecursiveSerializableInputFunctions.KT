package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext

class RecursiveSerializableInputFunctions {
    @AppFunction
    fun simpleFunction(
        appFunctionContext: AppFunctionContext,
        serializable: RecursiveSerializable
    ) {}

    @AppFunction
    fun simpleFunctionSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<RecursiveSerializable>
    ) {}

    @AppFunction
    fun simpleFunctionNullableSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<RecursiveSerializable>?
    ) {}

    @AppFunction
    fun simpleFunctionNullableSerializable(
        appFunctionContext: AppFunctionContext,
        serializable: RecursiveSerializable?
    ) {}
}
