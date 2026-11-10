package com.test

import androidx.appfunctions.AppFunctionBehaviorHints
import androidx.appfunctions.AppFunctionSignature
import androidx.appfunctions.metadata.AppFunctionMetadata

@AppFunctionSignature(
    scope = AppFunctionMetadata.SCOPE_GLOBAL,
    appFunctionXmlFileName = "app_level_app_functions",
    isDescribedByKDoc = false,
)
fun interface BehaviorHintsOnSignatureFunction {
    @AppFunctionBehaviorHints(isReadOnly = true)
    suspend fun doSomething(a: Int): String
}
