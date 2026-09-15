package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class DeprecatedFunction {
    @AppFunctionDeclaration
    @Deprecated("Deprecated Message")
    fun deprecatedFunction(appFunctionContext: AppFunctionContext) {}
}