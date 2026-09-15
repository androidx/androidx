package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class FakeNoArg {
    /** A free-form AppFunction without parameters. */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    fun noArg(appFunctionContext: AppFunctionContext) {}
}
