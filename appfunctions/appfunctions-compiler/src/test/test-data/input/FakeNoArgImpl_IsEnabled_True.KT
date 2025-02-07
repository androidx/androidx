package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext

class FakeNoArgImpl_IsEnabled_True : FakeNoArg {
    @AppFunction(isEnabled = true) override fun noArg(appFunctionContext: AppFunctionContext) {}
}
