package com.testdata

import android.net.Uri
import android.os.CancellationSignal
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.`internal`.AppFunctionExecutionDispatcher
import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.`internal`.Dispatchers
import java.util.function.Consumer
import javax.`annotation`.processing.Generated
import kotlin.String
import kotlin.Unit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class ValidUriService : ValidUriConstraintService() {
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
      FUNCTION_ID_FUNCTION_WITH_CONTENT_URI -> {
        this.functionWithContentUri(
          parameters["fileUri"] as Uri,
        )
      }
      FUNCTION_ID_FUNCTION_WITH_MULTI_SCHEME_URI -> {
        this.functionWithMultiSchemeUri(
          parameters["multiUri"] as Uri,
        )
      }
      FUNCTION_ID_FUNCTION_WITH_UNCONSTRAINED_URI -> {
        this.functionWithUnconstrainedUri(
          parameters["plainUri"] as Uri,
        )
      }
      FUNCTION_ID_FUNCTION_WITH_STRING_CONSTRAINT_NO_ENUM_VALUES -> {
        this.functionWithStringConstraintNoEnumValues(
          parameters["stringParam"] as String,
        )
      }
      FUNCTION_ID_FUNCTION_WITH_SERIALIZABLE_VALUE_CONSTRAINTS -> {
        this.functionWithSerializableValueConstraints(
          parameters["param"] as SerializableWithValueConstraints,
        )
      }
      FUNCTION_ID_FUNCTION_WITH_URI_RETURN -> {
        this.functionWithUriReturn(
        )
      }
      else -> {
        throw AppFunctionFunctionNotFoundException(
                "${request.functionIdentifier} is not available"
            )
      }
    }
  }

  override fun resolveInventory(): AppFunctionInventory = `$ValidUriConstraintService_AppFunctionInventory`()

  public companion object {
    public const val FUNCTION_ID_FUNCTION_WITH_CONTENT_URI: String =
        "com.testdata.ValidUriConstraintService#functionWithContentUri"

    public const val FUNCTION_ID_FUNCTION_WITH_MULTI_SCHEME_URI: String =
        "com.testdata.ValidUriConstraintService#functionWithMultiSchemeUri"

    public const val FUNCTION_ID_FUNCTION_WITH_UNCONSTRAINED_URI: String =
        "com.testdata.ValidUriConstraintService#functionWithUnconstrainedUri"

    public const val FUNCTION_ID_FUNCTION_WITH_STRING_CONSTRAINT_NO_ENUM_VALUES: String =
        "com.testdata.ValidUriConstraintService#functionWithStringConstraintNoEnumValues"

    public const val FUNCTION_ID_FUNCTION_WITH_SERIALIZABLE_VALUE_CONSTRAINTS: String =
        "com.testdata.ValidUriConstraintService#functionWithSerializableValueConstraints"

    public const val FUNCTION_ID_FUNCTION_WITH_URI_RETURN: String =
        "com.testdata.ValidUriConstraintService#functionWithUriReturn"
  }
}
