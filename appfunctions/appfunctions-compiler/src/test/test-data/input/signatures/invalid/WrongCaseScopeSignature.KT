package com.test

import androidx.appfunctions.AppFunctionSignature

@AppFunctionSignature(
    scope = "Global",
    appFunctionXmlFileName = "app_level_app_functions",
)
fun interface WrongCaseScopeSignature {
    suspend fun doSomething(a: Int, b: String): String
}
