package com.testdata

import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata
import androidx.appfunctions.service.`internal`.AppFunctionInventory
import javax.`annotation`.processing.Generated
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

/**
 * Source Files:
 * SerializableOutputFunctions.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SerializableOutputFunctions_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.SerializableOutputFunctions#simpleFunction" to ComTestdataSerializableOutputFunctionsSimpleFunctionMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.SerializableOutputFunctions#simpleFunctionSerializableList" to ComTestdataSerializableOutputFunctionsSimpleFunctionSerializableListMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.SerializableOutputFunctions#simpleFunctionNullableSerializableList" to ComTestdataSerializableOutputFunctionsSimpleFunctionNullableSerializableListMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.SerializableOutputFunctions#simpleFunctionNullableSerializable" to ComTestdataSerializableOutputFunctionsSimpleFunctionNullableSerializableMetadataObject.APP_FUNCTION_METADATA,
  )

  private object ComTestdataSerializableOutputFunctionsSimpleFunctionMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val REFERENCE_RESPONSE_VALUE_TYPE: AppFunctionReferenceTypeMetadata =
        AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.SampleSerializable",
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = REFERENCE_RESPONSE_VALUE_TYPE
        )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LISTVALUE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LISTVALUE:
        AppFunctionArrayTypeMetadata = AppFunctionArrayTypeMetadata(
            itemType = COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LISTVALUE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "value" to COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE,
      "listValue" to COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LISTVALUE,
    )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "value",
      "listValue",
    )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
        AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP,
            required = COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.SampleSerializable",
            isNullable = true,
            description = ""
        )

    private val COMPONENTS_METADATA_DATA_TYPES_MAP: Map<String, AppFunctionDataTypeMetadata> =
        mapOf(
      "com.testdata.SampleSerializable" to COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE,
      )

    private val COMPONENTS_METADATA: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
            dataTypes = COMPONENTS_METADATA_DATA_TYPES_MAP
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.SerializableOutputFunctions#simpleFunction",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataSerializableOutputFunctionsSimpleFunctionSerializableListMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_REFERENCE_ITEM_TYPE: AppFunctionReferenceTypeMetadata =
        AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.SampleSerializable",
            isNullable = false,
            description = ""
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_REFERENCE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LISTVALUE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LISTVALUE:
        AppFunctionArrayTypeMetadata = AppFunctionArrayTypeMetadata(
            itemType = COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LISTVALUE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "value" to COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE,
      "listValue" to COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LISTVALUE,
    )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "value",
      "listValue",
    )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
        AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP,
            required = COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.SampleSerializable",
            isNullable = true,
            description = ""
        )

    private val COMPONENTS_METADATA_DATA_TYPES_MAP: Map<String, AppFunctionDataTypeMetadata> =
        mapOf(
      "com.testdata.SampleSerializable" to COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE,
      )

    private val COMPONENTS_METADATA: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
            dataTypes = COMPONENTS_METADATA_DATA_TYPES_MAP
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.SerializableOutputFunctions#simpleFunctionSerializableList",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataSerializableOutputFunctionsSimpleFunctionNullableSerializableListMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_REFERENCE_ITEM_TYPE: AppFunctionReferenceTypeMetadata =
        AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.SampleSerializable",
            isNullable = false,
            description = ""
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_REFERENCE_ITEM_TYPE,
            isNullable = true,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LISTVALUE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LISTVALUE:
        AppFunctionArrayTypeMetadata = AppFunctionArrayTypeMetadata(
            itemType = COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LISTVALUE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "value" to COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE,
      "listValue" to COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LISTVALUE,
    )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "value",
      "listValue",
    )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
        AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP,
            required = COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.SampleSerializable",
            isNullable = true,
            description = ""
        )

    private val COMPONENTS_METADATA_DATA_TYPES_MAP: Map<String, AppFunctionDataTypeMetadata> =
        mapOf(
      "com.testdata.SampleSerializable" to COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE,
      )

    private val COMPONENTS_METADATA: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
            dataTypes = COMPONENTS_METADATA_DATA_TYPES_MAP
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.SerializableOutputFunctions#simpleFunctionNullableSerializableList",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataSerializableOutputFunctionsSimpleFunctionNullableSerializableMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val REFERENCE_RESPONSE_VALUE_TYPE: AppFunctionReferenceTypeMetadata =
        AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.SampleSerializable",
            isNullable = true,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = REFERENCE_RESPONSE_VALUE_TYPE
        )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LISTVALUE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LISTVALUE:
        AppFunctionArrayTypeMetadata = AppFunctionArrayTypeMetadata(
            itemType = COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LISTVALUE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "value" to COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE,
      "listValue" to COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LISTVALUE,
    )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "value",
      "listValue",
    )

    private val COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
        AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP,
            required = COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.SampleSerializable",
            isNullable = true,
            description = ""
        )

    private val COMPONENTS_METADATA_DATA_TYPES_MAP: Map<String, AppFunctionDataTypeMetadata> =
        mapOf(
      "com.testdata.SampleSerializable" to COM_TESTDATA_SAMPLESERIALIZABLE_OBJECT_DATA_TYPE,
      )

    private val COMPONENTS_METADATA: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
            dataTypes = COMPONENTS_METADATA_DATA_TYPES_MAP
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.SerializableOutputFunctions#simpleFunctionNullableSerializable",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }
}
