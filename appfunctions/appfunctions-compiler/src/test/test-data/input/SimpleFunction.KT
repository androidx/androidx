package com.testdata

import androidx.appfunctions.service.AppFunction
import androidx.appfunctions.AppFunctionContext

class SimpleFunction {
    @AppFunction
    fun simpleFunction(appFunctionContext: AppFunctionContext) {}
}