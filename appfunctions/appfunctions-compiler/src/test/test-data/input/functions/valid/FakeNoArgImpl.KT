package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class FakeNoArgImpl : FakeNoArg {
    @AppFunctionDeclaration override fun noArg(appFunctionContext: AppFunctionContext) {}
}
