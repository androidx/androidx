package com.test

import androidx.appfunctions.AppFunctionSignature
import androidx.appfunctions.metadata.AppFunctionMetadata

@AppFunctionSignature(
    scope = AppFunctionMetadata.SCOPE_GLOBAL,
    appFunctionXmlFileName = "",
)
fun interface EmptyAppFunctionXmlFileNameSignature {
    suspend fun doSomething(a: Int, b: String): String
}
