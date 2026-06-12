package com.test

import androidx.appfunctions.AppFunctionSignature
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.AppFunctionInstruction

@AppFunctionSignature(scope = AppFunctionMetadata.SCOPE_GLOBAL, appFunctionXmlFileName = "app_level_app_functions", isDescribedByKDoc = true)
fun interface SignatureWithKDocAndInstruction {
    /**
     * KDoc for method.
     *
     * @param a KDoc for param a.
     * @param b KDoc for param b.
     * @return KDoc for return.
     */
    @AppFunctionInstruction("Instruction for method.")
    suspend fun doSomething(
        @AppFunctionInstruction("Instruction for param a.") a: Int,
        @AppFunctionInstruction("Instruction for param b.") b: String
    ): @AppFunctionInstruction("Instruction for return.") String
}
