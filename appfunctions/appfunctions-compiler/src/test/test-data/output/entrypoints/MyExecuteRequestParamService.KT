package com.testdata

import android.os.CancellationSignal
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.`internal`.AppFunctionExecutionDispatcher
import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.`internal`.Dispatchers
import androidx.appfunctions.metadata.AppFunctionMetadata
import java.util.function.Consumer
import javax.`annotation`.processing.Generated
import kotlin.String
import kotlin.Unit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class MyExecuteRequestParamService : ExecuteRequestParamEntryPoint() {
  private lateinit var scope: CoroutineScope

  override fun onCreate() {
    super.onCreate()
    scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  override fun onExecuteFunction(
    request: ExecuteAppFunctionRequest,
    metadata: AppFunctionMetadata,
    cancellationSignal: CancellationSignal,
    callback: Consumer<ExecuteAppFunctionResponse>,
  ): Unit = AppFunctionExecutionDispatcher.dispatchExecuteAppFunction(
    scope,
    request,
    metadata,
    cancellationSignal,
    callback,
  ) { parameters ->
    when (request.functionIdentifier) {
      FUNCTION_ID_REQUEST_ONLY -> {
        this.requestOnly(
          request,
        )
      }
      FUNCTION_ID_REQUEST_WITH_PARAM -> {
        this.requestWithParam(
          request,
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

  override fun resolveInventory(): AppFunctionInventory = `$ExecuteRequestParamEntryPoint_AppFunctionInventory`()

  public companion object {
    public const val FUNCTION_ID_REQUEST_ONLY: String =
        "com.testdata.ExecuteRequestParamEntryPoint#requestOnly"

    public const val FUNCTION_ID_REQUEST_WITH_PARAM: String =
        "com.testdata.ExecuteRequestParamEntryPoint#requestWithParam"
  }
}
