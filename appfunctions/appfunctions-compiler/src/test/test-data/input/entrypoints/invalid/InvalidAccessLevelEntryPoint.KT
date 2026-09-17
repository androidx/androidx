package com.testdata

import androidx.appfunctions.AppFunctionAccessLevel
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("InvalidAccessLevelService", "invalid_access_level_service")
abstract class InvalidAccessLevelEntryPoint : AppFunctionService() {
    @AppFunctionDeclaration
    @AppFunctionAccessLevel(
        level = 999,
        isCompatEnforcementEnabled = true,
    )
    internal fun invalidFunction() {}
}
