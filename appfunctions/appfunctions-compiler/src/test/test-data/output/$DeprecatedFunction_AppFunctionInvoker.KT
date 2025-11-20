package com.testdata

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.service.`internal`.AppFunctionInvoker
import androidx.appfunctions.service.`internal`.ConfigurableAppFunctionFactory
import javax.`annotation`.processing.Generated
import kotlin.Any
import kotlin.String
import kotlin.collections.Map
import kotlin.collections.Set

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$DeprecatedFunction_AppFunctionInvoker` : AppFunctionInvoker {
  override val supportedFunctionIds: Set<String> = setOf(
    "com.testdata.DeprecatedFunction#deprecatedFunction",
  )

  override suspend fun unsafeInvoke(
    appFunctionContext: AppFunctionContext,
    functionIdentifier: String,
    parameters: Map<String, Any?>,
  ): Any? {
    val result: Any? = when (functionIdentifier) {
      "com.testdata.DeprecatedFunction#deprecatedFunction" -> {
        @Suppress("DEPRECATION")
        ConfigurableAppFunctionFactory<DeprecatedFunction>(
          appFunctionContext.context
        ) { DeprecatedFunction() }
        .createEnclosingClass(DeprecatedFunction::class.java)
        .deprecatedFunction(appFunctionContext)
      }
      else -> {
        throw AppFunctionFunctionNotFoundException("Unable to find $functionIdentifier")
      }
    }
    return result
  }
}
