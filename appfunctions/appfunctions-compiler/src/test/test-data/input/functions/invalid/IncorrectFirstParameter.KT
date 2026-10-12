package com.testdata

import androidx.appfunctions.AppFunctionDeclaration

class IncorrectFirstParameter {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun incorrectFirstParameter(x: Int) {}
}