package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("FakeNoArgImplIsEnabledTrueService", "app_functions_v2")
abstract class FakeNoArgImpl_IsEnabled_True : AppFunctionService(), FakeNoArg {
    @AppFunctionDeclaration(isDescribedByKDoc = false, isEnabled = true) override fun noArg() {}
}
