package com.test

import androidx.appfunctions.AppFunctionSignature

@AppFunctionSignature(
    scope = 100,
    appFunctionXmlFileName = "app_level_app_functions",
)
fun interface InvalidScopeSignature {
    suspend fun doSomething(a: Int, b: String): String
}
