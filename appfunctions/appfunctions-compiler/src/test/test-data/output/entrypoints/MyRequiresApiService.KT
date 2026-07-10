package com.testdata

import android.os.CancellationSignal
import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.`internal`.AppFunctionExecutionDispatcher
import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.`internal`.Dispatchers
import java.util.function.Consumer
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
@RequiresApi(
  value = 34,
  api = 1,
)
public class MyRequiresApiService : RequiresApiEntryPoint() {
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
    cancellationSignal: CancellationSignal,
    callback: Consumer<ExecuteAppFunctionResponse>,
  ): Unit = AppFunctionExecutionDispatcher.dispatchExecuteAppFunction(
    scope,
    request,
    resolveInventory(),
    cancellationSignal,
    callback,
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

  override fun resolveInventory(): AppFunctionInventory = `$RequiresApiEntryPoint_AppFunctionInventory`()

  public companion object {
    public const val FUNCTION_ID_SIMPLE_FUNCTION: String =
        "com.testdata.RequiresApiEntryPoint#simpleFunction"
  }
}
