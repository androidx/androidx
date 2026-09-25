package com.test

import androidx.appfunctions.AppFunctionAccessLevel
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSignature
import androidx.appfunctions.metadata.AppFunctionMetadata

@AppFunctionSignature(scope = AppFunctionMetadata.SCOPE_GLOBAL, appFunctionXmlFileName = "app_level_app_functions")
@AppFunctionAccessLevel(
    level = AppFunctionMetadata.ACCESS_LEVEL_SELF,
    isCompatEnforcementEnabled = true,
)
fun interface AccessLevelSignature {
    suspend fun doSomething(a: Int): String
}
