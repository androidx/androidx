package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable

class SerializableInputFunctions {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunction(appFunctionContext: AppFunctionContext, serializable: SampleSerializable) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<SampleSerializable>,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<SampleSerializable>?,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableSerializable(
        appFunctionContext: AppFunctionContext,
        serializable: SampleSerializable?,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionSerializableListItemNullable(
        appFunctionContext: AppFunctionContext,
        serializable: List<SampleSerializable?>,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionBothSerializableListAndListItemNullable(
        appFunctionContext: AppFunctionContext,
        serializable: List<SampleSerializable?>?,
    ) {}
}

@AppFunctionSerializable(isDescribedByKDoc = false) class SampleSerializable(val value: String, val listValue: List<String>) {}
