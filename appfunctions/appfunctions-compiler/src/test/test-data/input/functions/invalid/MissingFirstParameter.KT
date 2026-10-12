package com.testdata

import androidx.appfunctions.AppFunctionDeclaration

class MissingFirstParameter {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun missingFirstParameter() {}
}