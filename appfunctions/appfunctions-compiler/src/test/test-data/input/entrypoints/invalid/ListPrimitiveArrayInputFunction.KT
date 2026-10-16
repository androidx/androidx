package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("ListPrimitiveArrayInputFunctionService", "app_functions_v2")
abstract class ListPrimitiveArrayInputFunction : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun badFunctionInput(badParam: List<IntArray>) {}
}
