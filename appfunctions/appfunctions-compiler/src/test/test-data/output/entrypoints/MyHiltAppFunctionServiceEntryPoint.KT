package com.testdata

import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.`internal`.AppFunctionExecutionDispatcher
import androidx.appfunctions.`internal`.AppFunctionInventory
import javax.`annotation`.processing.Generated
import kotlin.String

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class MyHiltAppFunctionServiceEntryPoint : HiltAppFunctionServiceEntryPoint() {
  override suspend fun executeFunction(request: ExecuteAppFunctionRequest): ExecuteAppFunctionResponse = AppFunctionExecutionDispatcher.executeAppFunction(
    resolveInventory(),
    request
  ) { parameters ->
    when (request.functionIdentifier) {
      FUNCTION_ID_HELLO_WORLD -> {
        this.helloWorld(
        )
      }
      else -> {
        throw AppFunctionFunctionNotFoundException(
                "${request.functionIdentifier} is not available"
            )
      }
    }
  }

  override fun resolveInventory(): AppFunctionInventory = `$HiltAppFunctionServiceEntryPoint_AppFunctionInventory`()

  public companion object {
    public const val FUNCTION_ID_HELLO_WORLD: String =
        "com.testdata.HiltAppFunctionServiceEntryPoint#helloWorld"
  }
}
