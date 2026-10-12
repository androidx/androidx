package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable

class FunctionWithEmptySerializable {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionWithEmptySerializable(appFunctionContext: AppFunctionContext, empty: Empty) {}
}

@AppFunctionSerializable(isDescribedByKDoc = false) class Empty()
