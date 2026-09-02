package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable

class FunctionWithEmptySerializable {
    @AppFunctionDeclaration
    fun simpleFunctionWithEmptySerializable(appFunctionContext: AppFunctionContext, empty: Empty) {}
}

@AppFunctionSerializable class Empty()
