package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext

class ListPrimitiveArrayInputFunction {
    @AppFunction
    fun badFunctionInput(appFunctionContext: AppFunctionContext, badParam: List<IntArray>) {}
}
