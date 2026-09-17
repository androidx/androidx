package com.testdata

import androidx.appfunctions.AppFunctionAccessLevel
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.metadata.AppFunctionMetadata

@AppFunctionServiceEntryPoint("MyAccessLevelService", "my_access_level_service")
abstract class AccessLevelEntryPoint : AppFunctionService() {
    @AppFunctionDeclaration
    @AppFunctionAccessLevel(
        level = AppFunctionMetadata.ACCESS_LEVEL_SELF,
        isCompatEnforcementEnabled = false,
    )
    internal fun selfFunction() {}

    @AppFunctionDeclaration
    @AppFunctionAccessLevel(
        level = AppFunctionMetadata.ACCESS_LEVEL_SYSTEM,
        isCompatEnforcementEnabled = true,
    )
    internal fun systemFunction() {}
}
