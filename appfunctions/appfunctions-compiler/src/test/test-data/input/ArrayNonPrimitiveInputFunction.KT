package com.testdata

import androidx.appfunctions.service.AppFunction
import androidx.appfunctions.AppFunctionContext

class ArrayNonPrimitiveInputFunction {
    @AppFunction
    fun badFunctionInput(appFunctionContext: AppFunctionContext, input: Array<String>): String {}
}
