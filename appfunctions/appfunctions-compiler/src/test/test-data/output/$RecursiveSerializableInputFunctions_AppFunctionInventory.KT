package com.testdata

import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
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
 * RecursiveSerializableInputFunctions.....kt
 * RecursiveSerializable.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$RecursiveSerializableInputFunctions_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.RecursiveSerializableInputFunctions#simpleFunction" to ComTestdataRecursiveSerializableInputFunctionsSimpleFunctionMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.RecursiveSerializableInputFunctions#simpleFunctionSerializableList" to ComTestdataRecursiveSerializableInputFunctionsSimpleFunctionSerializableListMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.RecursiveSerializableInputFunctions#simpleFunctionNullableSerializableList" to ComTestdataRecursiveSerializableInputFunctionsSimpleFunctionNullableSerializableListMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.RecursiveSerializableInputFunctions#simpleFunctionNullableSerializable" to ComTestdataRecursiveSerializableInputFunctionsSimpleFunctionNullableSerializableMetadataObject.APP_FUNCTION_METADATA,
  )

  private val COM_TESTDATA_RECURSIVESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_NESTED:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "com.testdata.RecursiveSerializable",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_RECURSIVESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "nested" to COM_TESTDATA_RECURSIVESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_NESTED,
  )

  private val COM_TESTDATA_RECURSIVESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
      List<String> = listOf(
    "nested",
  )

  private val COM_TESTDATA_RECURSIVESERIALIZABLE_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
      AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_RECURSIVESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = COM_TESTDATA_RECURSIVESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "com.testdata.RecursiveSerializable",
          isNullable = true,
          description = ""
      )

  private val componentsMetadataDataTypesMap: Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "com.testdata.RecursiveSerializable" to COM_TESTDATA_RECURSIVESERIALIZABLE_OBJECT_DATA_TYPE,
    )

  override val componentsMetadata: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
          dataTypes = componentsMetadataDataTypesMap
      )

  private object ComTestdataRecursiveSerializableInputFunctionsSimpleFunctionMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_REFERENCE_DATA_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.RecursiveSerializable",
            isNullable = false,
            description = ""
        )

    private val SERIALIZABLE_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "serializable",
            isRequired = true,
            dataType = PARAMETER_METADATA_SERIALIZABLE_REFERENCE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      SERIALIZABLE_PARAMETER_METADATA,
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
            id = "com.testdata.RecursiveSerializableInputFunctions#simpleFunction",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataRecursiveSerializableInputFunctionsSimpleFunctionSerializableListMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE_REFERENCE_ITEM_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.RecursiveSerializable",
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE_REFERENCE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val SERIALIZABLE_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "serializable",
            isRequired = true,
            dataType = PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      SERIALIZABLE_PARAMETER_METADATA,
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
            id = "com.testdata.RecursiveSerializableInputFunctions#simpleFunctionSerializableList",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataRecursiveSerializableInputFunctionsSimpleFunctionNullableSerializableListMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE_REFERENCE_ITEM_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.RecursiveSerializable",
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE_REFERENCE_ITEM_TYPE,
            isNullable = true,
            description = ""
        )

    private val SERIALIZABLE_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "serializable",
            isRequired = true,
            dataType = PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      SERIALIZABLE_PARAMETER_METADATA,
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
            id = "com.testdata.RecursiveSerializableInputFunctions#simpleFunctionNullableSerializableList",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataRecursiveSerializableInputFunctionsSimpleFunctionNullableSerializableMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_REFERENCE_DATA_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.RecursiveSerializable",
            isNullable = true,
            description = ""
        )

    private val SERIALIZABLE_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "serializable",
            isRequired = true,
            dataType = PARAMETER_METADATA_SERIALIZABLE_REFERENCE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      SERIALIZABLE_PARAMETER_METADATA,
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
            id = "com.testdata.RecursiveSerializableInputFunctions#simpleFunctionNullableSerializable",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }
}
