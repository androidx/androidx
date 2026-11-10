package com.test

import androidx.appfunctions.AppFunctionBehaviorHints
import androidx.appfunctions.AppFunctionSignature
import androidx.appfunctions.metadata.AppFunctionMetadata

@AppFunctionSignature(
    scope = AppFunctionMetadata.SCOPE_GLOBAL,
    appFunctionXmlFileName = "app_level_app_functions",
    isDescribedByKDoc = false,
)
@AppFunctionBehaviorHints(
    isReadOnly = true,
    isOpenWorld = true,
)
fun interface BehaviorHintsSignature {
    suspend fun doSomething(a: Int): String
}
