package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("FakeNoArgImplIsEnabledFalseService", "app_functions_v2")
abstract class FakeNoArgImpl_IsEnabled_False : AppFunctionService(), FakeNoArg {
    @AppFunctionDeclaration(isDescribedByKDoc = false, isEnabled = false) override fun noArg() {}
}
