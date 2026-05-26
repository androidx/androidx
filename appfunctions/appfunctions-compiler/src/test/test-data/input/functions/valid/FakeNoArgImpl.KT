package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext

class FakeNoArgImpl : FakeNoArg {
    @AppFunction override fun noArg(appFunctionContext: AppFunctionContext) {}
}
