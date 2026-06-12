package com.test

import androidx.appfunctions.AppFunctionSignature
import androidx.appfunctions.metadata.AppFunctionMetadata

@AppFunctionSignature(scope = AppFunctionMetadata.SCOPE_GLOBAL, appFunctionXmlFileName = "app_level_app_functions", isDescribedByKDoc = true)
fun interface SignatureWithDescription {
    /**
     * Does something cool.
     *
     * @param a The first param.
     * @param b The second param.
     * @return The result.
     */
    suspend fun doSomething(a: Int, b: String): String
}
