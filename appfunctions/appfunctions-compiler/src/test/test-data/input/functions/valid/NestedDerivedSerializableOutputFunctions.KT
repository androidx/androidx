package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext

class NestedDerivedSerializableOutputFunctions {
    @AppFunction
    fun simpleFunction(appFunctionContext: AppFunctionContext): NestedDerivedSerializable {
        return NestedDerivedSerializable("testValue", null, 2, null)
    }

    @AppFunction
    fun simpleFunctionSerializableList(
        appFunctionContext: AppFunctionContext,
    ): List<NestedDerivedSerializable> {
        return listOf(NestedDerivedSerializable("testValue", null, 2, null))
    }
}
