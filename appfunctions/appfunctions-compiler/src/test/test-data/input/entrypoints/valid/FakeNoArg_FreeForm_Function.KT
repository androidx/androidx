package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("FakeNoArgService", "app_functions_v2")
abstract class FakeNoArg : AppFunctionService() {
    /** A free-form AppFunction without parameters. */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    fun noArg() {}
}
