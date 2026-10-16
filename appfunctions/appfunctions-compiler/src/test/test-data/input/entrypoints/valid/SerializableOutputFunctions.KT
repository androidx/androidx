package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("SerializableOutputFunctionsService", "app_functions_v2")
abstract class SerializableOutputFunctions : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunction(): SampleSerializable {
        return SampleSerializable("testValue", listOf("testListValue"))
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionSerializableList(): List<SampleSerializable> {
        return listOf(SampleSerializable("testValue", listOf("testListValue")))
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableSerializableList(): List<SampleSerializable>? {
        return null
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableSerializable(): SampleSerializable? {
        return null
    }
}

@AppFunctionSerializable(isDescribedByKDoc = false) class SampleSerializable(val value: String, val listValue: List<String>) {}
