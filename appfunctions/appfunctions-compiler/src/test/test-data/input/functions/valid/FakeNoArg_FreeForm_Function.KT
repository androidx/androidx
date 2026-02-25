package com.testdata

import androidx.appfunctions.service.AppFunction
import androidx.appfunctions.AppFunctionContext

class FakeNoArg {
    /** A free-form AppFunction without parameters. */
    @AppFunction(isDescribedByKDoc = true)
    fun noArg(appFunctionContext: AppFunctionContext) {}
}
