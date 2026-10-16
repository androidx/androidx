package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("DeprecatedFunctionService", "app_functions_v2")
abstract class DeprecatedFunction : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    @Deprecated("Deprecated Message")
    fun deprecatedFunction() {}
}