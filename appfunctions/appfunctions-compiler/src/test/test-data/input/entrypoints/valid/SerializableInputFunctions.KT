package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("SerializableInputFunctionsService", "app_functions_v2")
abstract class SerializableInputFunctions : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunction(serializable: SampleSerializable) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionSerializableList(
        serializable: List<SampleSerializable>,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableSerializableList(
        serializable: List<SampleSerializable>?,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableSerializable(
        serializable: SampleSerializable?,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionSerializableListItemNullable(
        serializable: List<SampleSerializable?>,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionBothSerializableListAndListItemNullable(
        serializable: List<SampleSerializable?>?,
    ) {}
}

@AppFunctionSerializable(isDescribedByKDoc = false) class SampleSerializable(val value: String, val listValue: List<String>) {}
