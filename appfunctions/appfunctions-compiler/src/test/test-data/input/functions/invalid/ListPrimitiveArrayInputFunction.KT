package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class ListPrimitiveArrayInputFunction {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun badFunctionInput(appFunctionContext: AppFunctionContext, badParam: List<IntArray>) {}
}
