package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("NestedDerivedSerializableOutputFunctionsService", "app_functions_v2")
abstract class NestedDerivedSerializableOutputFunctions : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunction(): NestedDerivedSerializable {
        return NestedDerivedSerializable("testValue", null, 2, null)
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionSerializableList(): List<NestedDerivedSerializable> {
        return listOf(NestedDerivedSerializable("testValue", null, 2, null))
    }
}
