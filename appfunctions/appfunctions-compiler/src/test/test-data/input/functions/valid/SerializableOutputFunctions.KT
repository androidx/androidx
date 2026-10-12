package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable

class SerializableOutputFunctions {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunction(appFunctionContext: AppFunctionContext): SampleSerializable {
        return SampleSerializable("testValue", listOf("testListValue"))
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionSerializableList(
        appFunctionContext: AppFunctionContext,
    ): List<SampleSerializable> {
        return listOf(SampleSerializable("testValue", listOf("testListValue")))
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableSerializableList(
        appFunctionContext: AppFunctionContext,
    ): List<SampleSerializable>? {
        return null
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableSerializable(
        appFunctionContext: AppFunctionContext,
    ): SampleSerializable? {
        return null
    }
}

@AppFunctionSerializable(isDescribedByKDoc = false) class SampleSerializable(val value: String, val listValue: List<String>) {}
