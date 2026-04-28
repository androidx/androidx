package com.testdata

import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.service.`internal`.AppFunctionExecutionDispatcher
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class MySimpleService : SimpleEntryPoint() {
  override suspend fun executeFunction(request: ExecuteAppFunctionRequest): ExecuteAppFunctionResponse = AppFunctionExecutionDispatcher.executeAppFunction(
    resolveInventory(),
    request
  ) { parameters ->
    when (request.functionIdentifier) {
      "com.testdata.SimpleEntryPoint#simpleFunction" -> {
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

  override fun resolveInventory(): AppFunctionInventory = `$SimpleEntryPoint_AppFunctionInventory`()
}
