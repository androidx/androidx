package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class AnyTypedInputFunction {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun badFunctionInput(appFunctionContext: AppFunctionContext, badParam: Any) {}
}