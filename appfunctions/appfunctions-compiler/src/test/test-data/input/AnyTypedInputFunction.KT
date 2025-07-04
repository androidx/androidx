package com.testdata

import androidx.appfunctions.service.AppFunction
import androidx.appfunctions.AppFunctionContext

class AnyTypedInputFunction {
    @AppFunction
    fun badFunctionInput(appFunctionContext: AppFunctionContext, badParam: Any) {}
}