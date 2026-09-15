package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class ArrayNonPrimitiveInputFunction {
    @AppFunctionDeclaration
    fun badFunctionInput(appFunctionContext: AppFunctionContext, input: Array<String>): String {}
}
