package com.testdata

import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionBehaviorHintsMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import androidx.appfunctions.metadata.AppFunctionUnitTypeMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata
import javax.`annotation`.processing.Generated
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

/**
 * Source Files:
 * BehaviorHintsEntryPoint.kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$BehaviorHintsEntryPoint_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.BehaviorHintsEntryPoint#readOnlyFunction" to ComTestdataBehaviorHintsEntryPointReadOnlyFunctionMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.BehaviorHintsEntryPoint#destructiveIdempotentFunction" to ComTestdataBehaviorHintsEntryPointDestructiveIdempotentFunctionMetadataObject.APP_FUNCTION_METADATA,
  )

  private val componentsMetadataDataTypesMap: Map<String, AppFunctionDataTypeMetadata> = mapOf(
    )

  override val componentsMetadata: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
          dataTypes = componentsMetadataDataTypesMap
      )

  private object ComTestdataBehaviorHintsEntryPointReadOnlyFunctionMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionUnitTypeMetadata =
        AppFunctionUnitTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    private val BEHAVIOR_HINTS_METADATA: AppFunctionBehaviorHintsMetadata? =
        AppFunctionBehaviorHintsMetadata(isReadOnly = true, isDestructive = true, isIdempotent = false, isOpenWorld = true)

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.BehaviorHintsEntryPoint#readOnlyFunction",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            behaviorHints = BEHAVIOR_HINTS_METADATA,
        )
  }

  private object ComTestdataBehaviorHintsEntryPointDestructiveIdempotentFunctionMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionUnitTypeMetadata =
        AppFunctionUnitTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    private val BEHAVIOR_HINTS_METADATA: AppFunctionBehaviorHintsMetadata? =
        AppFunctionBehaviorHintsMetadata(isReadOnly = false, isDestructive = true, isIdempotent = true, isOpenWorld = false)

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.BehaviorHintsEntryPoint#destructiveIdempotentFunction",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            behaviorHints = BEHAVIOR_HINTS_METADATA,
        )
  }
}
