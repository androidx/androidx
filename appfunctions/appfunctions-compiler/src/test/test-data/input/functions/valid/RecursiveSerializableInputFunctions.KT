package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class RecursiveSerializableInputFunctions {
    @AppFunctionDeclaration
    fun simpleFunction(
        appFunctionContext: AppFunctionContext,
        serializable: RecursiveSerializable
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<RecursiveSerializable>
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionNullableSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<RecursiveSerializable>?
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionNullableSerializable(
        appFunctionContext: AppFunctionContext,
        serializable: RecursiveSerializable?
    ) {}
}
