package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("AnyTypedInputFunctionService", "app_functions_v2")
abstract class AnyTypedInputFunction : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun badFunctionInput(badParam: Any) {}
}