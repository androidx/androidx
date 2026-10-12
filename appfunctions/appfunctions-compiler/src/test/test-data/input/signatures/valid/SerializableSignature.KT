package com.test

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionSignature
import androidx.appfunctions.metadata.AppFunctionMetadata

@AppFunctionSerializable(isDescribedByKDoc = false)
class InputData(
    val message: String,
    val code: Int
)

@AppFunctionSerializable(isDescribedByKDoc = false)
class OutputData(
    val resultMessage: String,
    val success: Boolean
)

@AppFunctionSignature(scope = AppFunctionMetadata.SCOPE_GLOBAL, appFunctionXmlFileName = "app_level_app_functions", isDescribedByKDoc = false)
fun interface SerializableSignature {
    suspend fun processData(input: InputData): OutputData
}
