package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class FakeNoArgImpl_IsEnabled_True : FakeNoArg {
    @AppFunctionDeclaration(isEnabled = true) override fun noArg(appFunctionContext: AppFunctionContext) {}
}
