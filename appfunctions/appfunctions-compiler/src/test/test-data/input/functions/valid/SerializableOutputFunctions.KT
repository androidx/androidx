package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable

class SerializableOutputFunctions {
    @AppFunction
    fun simpleFunction(appFunctionContext: AppFunctionContext): SampleSerializable {
        return SampleSerializable("testValue", listOf("testListValue"))
    }

    @AppFunction
    fun simpleFunctionSerializableList(
        appFunctionContext: AppFunctionContext,
    ): List<SampleSerializable> {
        return listOf(SampleSerializable("testValue", listOf("testListValue")))
    }

    @AppFunction
    fun simpleFunctionNullableSerializableList(
        appFunctionContext: AppFunctionContext,
    ): List<SampleSerializable>? {
        return null
    }

    @AppFunction
    fun simpleFunctionNullableSerializable(
        appFunctionContext: AppFunctionContext,
    ): SampleSerializable? {
        return null
    }
}

@AppFunctionSerializable class SampleSerializable(val value: String, val listValue: List<String>) {}
