package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable

class SerializableInputFunctions {
    @AppFunctionDeclaration
    fun simpleFunction(appFunctionContext: AppFunctionContext, serializable: SampleSerializable) {}

    @AppFunctionDeclaration
    fun simpleFunctionSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<SampleSerializable>,
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionNullableSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<SampleSerializable>?,
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionNullableSerializable(
        appFunctionContext: AppFunctionContext,
        serializable: SampleSerializable?,
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionSerializableListItemNullable(
        appFunctionContext: AppFunctionContext,
        serializable: List<SampleSerializable?>,
    ) {}

    @AppFunctionDeclaration
    fun simpleFunctionBothSerializableListAndListItemNullable(
        appFunctionContext: AppFunctionContext,
        serializable: List<SampleSerializable?>?,
    ) {}
}

@AppFunctionSerializable class SampleSerializable(val value: String, val listValue: List<String>) {}
