package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("FunctionWithEmptySerializableService", "app_functions_v2")
abstract class FunctionWithEmptySerializable : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithEmptySerializable(empty: Empty) {}
}

@AppFunctionSerializable(isDescribedByKDoc = false) class Empty()
