package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class ArrayNonPrimitiveInputFunction {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun badFunctionInput(appFunctionContext: AppFunctionContext, input: Array<String>): String {}
}
