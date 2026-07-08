package com.test

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionSignature
import androidx.appfunctions.metadata.AppFunctionMetadata

@AppFunctionSerializable
class InputData(
    val message: String,
    val code: Int
)

@AppFunctionSerializable
class OutputData(
    val resultMessage: String,
    val success: Boolean
)

@AppFunctionSignature(scope = AppFunctionMetadata.SCOPE_GLOBAL, appFunctionXmlFileName = "app_level_app_functions")
fun interface SerializableSignature {
    suspend fun processData(input: InputData): OutputData
}
