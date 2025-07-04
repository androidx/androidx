package com.testdata

import androidx.appfunctions.service.AppFunction
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable

class FunctionWithEmptySerializable {
    @AppFunction
    fun simpleFunctionWithEmptySerializable(appFunctionContext: AppFunctionContext, empty: Empty) {}
}

@AppFunctionSerializable class Empty()
