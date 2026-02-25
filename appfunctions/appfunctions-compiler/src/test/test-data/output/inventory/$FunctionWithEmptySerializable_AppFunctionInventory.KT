package com.testdata

import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
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
 * FunctionWithEmptySerializable.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$FunctionWithEmptySerializable_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.FunctionWithEmptySerializable#simpleFunctionWithEmptySerializable" to ComTestdataFunctionWithEmptySerializableSimpleFunctionWithEmptySerializableMetadataObject.APP_FUNCTION_METADATA,
  )

  private val COM_TESTDATA_EMPTY_OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
  )

  private val COM_TESTDATA_EMPTY_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST: List<String> = listOf(
  )

  private val COM_TESTDATA_EMPTY_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
      AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_EMPTY_OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = COM_TESTDATA_EMPTY_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "com.testdata.Empty",
          isNullable = true,
          description = ""
      )

  private val componentsMetadataDataTypesMap: Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "com.testdata.Empty" to COM_TESTDATA_EMPTY_OBJECT_DATA_TYPE,
    )

  override val componentsMetadata: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
          dataTypes = componentsMetadataDataTypesMap
      )

  private object ComTestdataFunctionWithEmptySerializableSimpleFunctionWithEmptySerializableMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_EMPTY_REFERENCE_DATA_TYPE: AppFunctionReferenceTypeMetadata =
        AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.Empty",
            isNullable = false,
            description = ""
        )

    private val EMPTY_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "empty",
            isRequired = true,
            dataType = PARAMETER_METADATA_EMPTY_REFERENCE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      EMPTY_PARAMETER_METADATA,
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

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.FunctionWithEmptySerializable#simpleFunctionWithEmptySerializable",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }
}
