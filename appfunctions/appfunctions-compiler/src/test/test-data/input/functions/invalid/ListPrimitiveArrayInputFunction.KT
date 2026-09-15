package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class ListPrimitiveArrayInputFunction {
    @AppFunctionDeclaration
    fun badFunctionInput(appFunctionContext: AppFunctionContext, badParam: List<IntArray>) {}
}
