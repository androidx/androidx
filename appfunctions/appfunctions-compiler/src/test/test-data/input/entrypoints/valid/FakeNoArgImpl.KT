package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("FakeNoArgImplService", "app_functions_v2")
abstract class FakeNoArgImpl : AppFunctionService(), FakeNoArg {
    @AppFunctionDeclaration(isDescribedByKDoc = false) override fun noArg() {}
}
