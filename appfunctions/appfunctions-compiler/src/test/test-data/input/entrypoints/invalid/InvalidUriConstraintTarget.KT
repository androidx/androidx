package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionUriValueConstraint

@AppFunctionServiceEntryPoint("InvalidUriService", "invalid_uri_service")
abstract class InvalidUriConstraintTarget : AppFunctionService() {
    @AppFunctionDeclaration
    fun invalidUriConstraint(
        @AppFunctionUriValueConstraint
        invalidParam: String
    ) {}
}
