package com.test

import androidx.appfunctions.AppFunctionSignature

@AppFunctionSignature(
    scope = "invalid_scope",
    appFunctionXmlFileName = "app_level_app_functions",
)
fun interface InvalidScopeSignature {
    suspend fun doSomething(a: Int, b: String): String
}
