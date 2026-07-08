package com.test

import androidx.appfunctions.AppFunctionAdapter
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.HandleAppFunctionRequest
import androidx.appfunctions.SuspendingAppFunction
import androidx.appfunctions.`internal`.AppFunctionParameterSpec
import androidx.appfunctions.`internal`.AppFunctionResponseSpec
import androidx.appfunctions.`internal`.unsafeBuildReturnValue
import androidx.appfunctions.`internal`.unsafeGetParameterValue
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$GlobalSignature_AppFunctionAdapter` : AppFunctionAdapter<GlobalSignature> {
  override fun getFunctionId(): String = "com.test.GlobalSignature#doSomething"

  override fun adapt(instance: GlobalSignature): HandleAppFunctionRequest = HandleAppFunctionRequest(
    functionIdentifier = getFunctionId(),
    appFunction = SuspendingAppFunction { request ->
      withExtractedArguments(request) { a, b ->
        val result = instance.doSomething(a, b)
        toExecuteAppFunctionResponse(result)
      }
    }
  )

  private inline fun <R> withExtractedArguments(request: ExecuteAppFunctionRequest, block: (a: Int, b: String) -> R): R {
    val a = request.functionParameters.unsafeGetParameterValue(A_PARAMETER_SPEC) as Int
    val b = request.functionParameters.unsafeGetParameterValue(B_PARAMETER_SPEC) as String
    return block(a, b)
  }

  private fun toExecuteAppFunctionResponse(result: String): ExecuteAppFunctionResponse {
    val returnValue = RESPONSE_SPEC.unsafeBuildReturnValue(result)
    return ExecuteAppFunctionResponse.Success(returnValue)
  }

  public companion object {
    private val A_PARAMETER_SPEC: AppFunctionParameterSpec = AppFunctionParameterSpec(
      type = 7,
      name = "a",
      isRequired = true,
      isNullable = false,
    )

    private val B_PARAMETER_SPEC: AppFunctionParameterSpec = AppFunctionParameterSpec(
      type = 8,
      name = "b",
      isRequired = true,
      isNullable = false,
    )

    private val RESPONSE_SPEC: AppFunctionResponseSpec = AppFunctionResponseSpec(
      type = 8,
      isNullable = false,
    )
  }
}
