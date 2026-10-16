package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("ArrayNonPrimitiveInputFunctionService", "app_functions_v2")
abstract class ArrayNonPrimitiveInputFunction : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun badFunctionInput(input: Array<String>): String {}
}
