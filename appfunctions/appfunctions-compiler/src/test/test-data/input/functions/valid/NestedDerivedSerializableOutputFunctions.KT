package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class NestedDerivedSerializableOutputFunctions {
    @AppFunctionDeclaration
    fun simpleFunction(appFunctionContext: AppFunctionContext): NestedDerivedSerializable {
        return NestedDerivedSerializable("testValue", null, 2, null)
    }

    @AppFunctionDeclaration
    fun simpleFunctionSerializableList(
        appFunctionContext: AppFunctionContext,
    ): List<NestedDerivedSerializable> {
        return listOf(NestedDerivedSerializable("testValue", null, 2, null))
    }
}
