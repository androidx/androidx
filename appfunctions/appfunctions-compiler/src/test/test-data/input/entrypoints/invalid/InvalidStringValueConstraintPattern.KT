package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionStringValueConstraint

@AppFunctionServiceEntryPoint("InvalidStringPatternService", "invalid_string_pattern_service")
abstract class InvalidStringValueConstraintPattern : AppFunctionService() {
    @AppFunctionDeclaration
    fun invalidPattern(
        @AppFunctionStringValueConstraint(pattern = "[invalid_regex") param: String
    ): String {
        return param
    }
}
