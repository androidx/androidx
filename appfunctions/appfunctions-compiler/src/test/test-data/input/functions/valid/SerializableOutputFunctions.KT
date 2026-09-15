package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable

class SerializableOutputFunctions {
    @AppFunctionDeclaration
    fun simpleFunction(appFunctionContext: AppFunctionContext): SampleSerializable {
        return SampleSerializable("testValue", listOf("testListValue"))
    }

    @AppFunctionDeclaration
    fun simpleFunctionSerializableList(
        appFunctionContext: AppFunctionContext,
    ): List<SampleSerializable> {
        return listOf(SampleSerializable("testValue", listOf("testListValue")))
    }

    @AppFunctionDeclaration
    fun simpleFunctionNullableSerializableList(
        appFunctionContext: AppFunctionContext,
    ): List<SampleSerializable>? {
        return null
    }

    @AppFunctionDeclaration
    fun simpleFunctionNullableSerializable(
        appFunctionContext: AppFunctionContext,
    ): SampleSerializable? {
        return null
    }
}

@AppFunctionSerializable class SampleSerializable(val value: String, val listValue: List<String>) {}
