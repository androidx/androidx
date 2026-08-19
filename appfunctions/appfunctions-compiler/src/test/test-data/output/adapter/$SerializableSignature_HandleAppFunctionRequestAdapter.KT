package com.test

import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.ExperimentalAppFunctionsApi
import androidx.appfunctions.HandleAppFunctionRequest
import androidx.appfunctions.HandleAppFunctionRequestAdapter
import androidx.appfunctions.SuspendingAppFunction
import androidx.appfunctions.`internal`.AppFunctionParameterSpec
import androidx.appfunctions.`internal`.AppFunctionResponseSpec
import androidx.appfunctions.`internal`.unsafeBuildReturnValue
import androidx.appfunctions.`internal`.unsafeGetParameterValue
import javax.`annotation`.processing.Generated
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
@OptIn(ExperimentalAppFunctionsApi::class)
public class `$SerializableSignature_HandleAppFunctionRequestAdapter` : HandleAppFunctionRequestAdapter<SerializableSignature> {
  override val functionIdentifier: String = "com.test.SerializableSignature#processData"

  override fun adapt(instance: SerializableSignature): HandleAppFunctionRequest = HandleAppFunctionRequest(
    functionIdentifier = functionIdentifier,
    appFunction = SuspendingAppFunction { request ->
      withExtractedArguments(request) { input ->
        val result = instance.processData(input)
        toExecuteAppFunctionResponse(result)
      }
    }
  )

  @Suppress("UNCHECKED_CAST")
  private inline fun <R> withExtractedArguments(request: ExecuteAppFunctionRequest, block: (input: InputData) -> R): R {
    val input = request.functionParameters.unsafeGetParameterValue(INPUT_PARAMETER_SPEC) as InputData
    return block(input)
  }

  private fun toExecuteAppFunctionResponse(result: OutputData): ExecuteAppFunctionResponse {
    val returnValue = RESPONSE_SPEC.unsafeBuildReturnValue(result)
    return ExecuteAppFunctionResponse.Success(returnValue)
  }

  public companion object {
    private val INPUT_PARAMETER_SPEC: AppFunctionParameterSpec = AppFunctionParameterSpec(
      type = 3,
      name = "input",
      isRequired = true,
      isNullable = false,
      objectQualifiedName = "com.test.InputData",
    )

    private val RESPONSE_SPEC: AppFunctionResponseSpec = AppFunctionResponseSpec(
      type = 3,
      isNullable = false,
      objectQualifiedName = "com.test.OutputData",
    )
  }
}
