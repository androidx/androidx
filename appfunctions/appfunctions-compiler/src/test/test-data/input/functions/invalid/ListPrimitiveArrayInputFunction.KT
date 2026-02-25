package com.testdata

import androidx.appfunctions.service.AppFunction
import androidx.appfunctions.AppFunctionContext

class ListPrimitiveArrayInputFunction {
    @AppFunction
    fun badFunctionInput(appFunctionContext: AppFunctionContext, badParam: List<IntArray>) {}
}
