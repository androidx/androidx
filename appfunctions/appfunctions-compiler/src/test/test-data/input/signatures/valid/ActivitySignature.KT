package com.test

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSignature
import androidx.appfunctions.metadata.AppFunctionMetadata

@AppFunctionSignature(scope = AppFunctionMetadata.SCOPE_ACTIVITY, appFunctionXmlFileName = "app_level_app_functions")
fun interface ActivitySignature {
    suspend fun doSomething(a: Int, b: String): String
}
