package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class AnyTypedInputFunction {
    @AppFunctionDeclaration
    fun badFunctionInput(appFunctionContext: AppFunctionContext, badParam: Any) {}
}