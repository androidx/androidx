package com.test

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSignature
import androidx.appfunctions.metadata.AppFunctionMetadata

@AppFunctionSignature(scope = AppFunctionMetadata.SCOPE_GLOBAL, appFunctionXmlFileName = "app_level_app_functions", isDescribedByKDoc = false)
fun interface GlobalSignature {
    suspend fun doSomething(a: Int, b: String): String
}
