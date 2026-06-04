package com.testdata

import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.`internal`.AppFunctionExecutionDispatcher
import androidx.appfunctions.`internal`.AppFunctionInventory
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class MyExtensionService : ExtensionEntryPoint() {
  override suspend fun executeFunction(request: ExecuteAppFunctionRequest): ExecuteAppFunctionResponse = AppFunctionExecutionDispatcher.executeAppFunction(
    resolveInventory(),
    request
  ) { parameters ->
    when (request.functionIdentifier) {
      FUNCTION_ID_SIMPLE_FUNCTION -> {
        this.simpleFunction(
          parameters["int"] as Int,
          parameters["string"] as String,
        )
      }
      else -> {
        throw AppFunctionFunctionNotFoundException(
                "${request.functionIdentifier} is not available"
            )
      }
    }
  }

  override fun resolveInventory(): AppFunctionInventory = `$ExtensionEntryPoint_AppFunctionInventory`()

  public companion object {
    public const val FUNCTION_ID_SIMPLE_FUNCTION: String =
        "com.testdata.ExtensionEntryPoint#simpleFunction"
  }
}
