package com.testdata

import androidx.appfunctions.service.AppFunction
import androidx.appfunctions.AppFunctionContext

class FakeNoArgImpl_IsEnabled_False : FakeNoArg {
    @AppFunction(isEnabled = false) override fun noArg(appFunctionContext: AppFunctionContext) {}
}
