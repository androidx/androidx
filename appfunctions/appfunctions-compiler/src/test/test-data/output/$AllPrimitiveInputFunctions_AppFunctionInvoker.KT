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
public class `$AllPrimitiveInputFunctions_AppFunctionInvoker` : AppFunctionInvoker {
  override val supportedFunctionIds: Set<String> = setOf(
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionInt",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionLong",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionFloat",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionDouble",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionBoolean",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionString",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableInt",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableLong",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableFloat",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableDouble",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableBoolean",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableString",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionIntArray",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionLongArray",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionFloatArray",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionDoubleArray",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionBooleanArray",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionStringList",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableIntArray",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableLongArray",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableFloatArray",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableDoubleArray",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableBooleanArray",
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableStringList",
  )

  override suspend fun unsafeInvoke(
    appFunctionContext: AppFunctionContext,
    functionIdentifier: String,
    parameters: Map<String, Any?>,
  ): Any? {
    val result: Any? = when (functionIdentifier) {
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionInt" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionInt(appFunctionContext, parameters["intParam"] as kotlin.Int)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionLong" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionLong(appFunctionContext, parameters["longParam"] as kotlin.Long)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionFloat" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionFloat(appFunctionContext, parameters["floatParam"] as kotlin.Float)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionDouble" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionDouble(appFunctionContext, parameters["doubleParam"] as kotlin.Double)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionBoolean" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionBoolean(appFunctionContext, parameters["booleanParam"] as kotlin.Boolean)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionString" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionString(appFunctionContext, parameters["stringParam"] as kotlin.String)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableInt" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionNullableInt(appFunctionContext, parameters["intParam"] as kotlin.Int?)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableLong" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionNullableLong(appFunctionContext, parameters["longParam"] as kotlin.Long?)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableFloat" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionNullableFloat(appFunctionContext, parameters["floatParam"] as kotlin.Float?)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableDouble" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionNullableDouble(appFunctionContext, parameters["doubleParam"] as kotlin.Double?)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableBoolean" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionNullableBoolean(appFunctionContext, parameters["booleanParam"] as kotlin.Boolean?)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableString" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionNullableString(appFunctionContext, parameters["stringParam"] as kotlin.String?)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionIntArray" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionIntArray(appFunctionContext, parameters["intArrayParam"] as kotlin.IntArray)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionLongArray" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionLongArray(appFunctionContext, parameters["longArrayParam"] as kotlin.LongArray)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionFloatArray" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionFloatArray(appFunctionContext, parameters["floatArrayParam"] as kotlin.FloatArray)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionDoubleArray" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionDoubleArray(appFunctionContext, parameters["doubleArrayParam"] as kotlin.DoubleArray)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionBooleanArray" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionBooleanArray(appFunctionContext, parameters["booleanArrayParam"] as kotlin.BooleanArray)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionStringList" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionStringList(appFunctionContext, @Suppress("UNCHECKED_CAST") (parameters["stringListParam"] as kotlin.collections.List<kotlin.String>))
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableIntArray" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionNullableIntArray(appFunctionContext, parameters["intArrayParam"] as kotlin.IntArray?)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableLongArray" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionNullableLongArray(appFunctionContext, parameters["longArrayParam"] as kotlin.LongArray?)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableFloatArray" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionNullableFloatArray(appFunctionContext, parameters["floatArrayParam"] as kotlin.FloatArray?)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableDoubleArray" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionNullableDoubleArray(appFunctionContext, parameters["doubleArrayParam"] as kotlin.DoubleArray?)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableBooleanArray" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionNullableBooleanArray(appFunctionContext, parameters["booleanArrayParam"] as kotlin.BooleanArray?)
      }
      "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableStringList" -> {
        ConfigurableAppFunctionFactory<AllPrimitiveInputFunctions>(
          appFunctionContext.context
        ) { AllPrimitiveInputFunctions() }
        .createEnclosingClass(AllPrimitiveInputFunctions::class.java)
        .simpleFunctionNullableStringList(appFunctionContext, @Suppress("UNCHECKED_CAST") (parameters["stringListParam"] as kotlin.collections.List<kotlin.String>?))
      }
      else -> {
        throw AppFunctionFunctionNotFoundException("Unable to find $functionIdentifier")
      }
    }
    return result
  }
}
