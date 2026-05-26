package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext

class DeprecatedFunction {
    @AppFunction
    @Deprecated("Deprecated Message")
    fun deprecatedFunction(appFunctionContext: AppFunctionContext) {}
}