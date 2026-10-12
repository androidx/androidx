package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class FakeNoArgImpl_IsEnabled_False : FakeNoArg {
    @AppFunctionDeclaration(isDescribedByKDoc = false, isEnabled = false) override fun noArg(appFunctionContext: AppFunctionContext) {}
}
