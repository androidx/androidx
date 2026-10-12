package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class DeprecatedFunction {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    @Deprecated("Deprecated Message")
    fun deprecatedFunction(appFunctionContext: AppFunctionContext) {}
}