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
 * SerializableInputFunctions.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SerializableInputFunctions_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.SerializableInputFunctions#simpleFunction" to ComTestdataSerializableInputFunctionsSimpleFunctionMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.SerializableInputFunctions#simpleFunctionSerializableList" to ComTestdataSerializableInputFunctionsSimpleFunctionSerializableListMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.SerializableInputFunctions#simpleFunctionNullableSerializableList" to ComTestdataSerializableInputFunctionsSimpleFunctionNullableSerializableListMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.SerializableInputFunctions#simpleFunctionNullableSerializable" to ComTestdataSerializableInputFunctionsSimpleFunctionNullableSerializableMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.SerializableInputFunctions#simpleFunctionSerializableListItemNullable" to ComTestdataSerializableInputFunctionsSimpleFunctionSerializableListItemNullableMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.SerializableInputFunctions#simpleFunctionBothSerializableListAndListItemNullable" to ComTestdataSerializableInputFunctionsSimpleFunctionBothSerializableListAndListItemNullableMetadataObject.APP_FUNCTION_METADATA,
  )

  private object ComTestdataSerializableInputFunctionsSimpleFunctionMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_REFERENCE_DATA_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.SampleSerializable",
            isNullable = false,
            description = ""
        )

    private val SERIALIZABLE_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "serializable",
            isRequired = true,
            dataType = PARAMETER_METADATA_SERIALIZABLE_REFERENCE_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      SERIALIZABLE_PARAMETER_METADATA,
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 0,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
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
            id = "com.testdata.SerializableInputFunctions#simpleFunction",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataSerializableInputFunctionsSimpleFunctionSerializableListMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE_REFERENCE_ITEM_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.SampleSerializable",
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
            dataType = PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      SERIALIZABLE_PARAMETER_METADATA,
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 0,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
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
            id = "com.testdata.SerializableInputFunctions#simpleFunctionSerializableList",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataSerializableInputFunctionsSimpleFunctionNullableSerializableListMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE_REFERENCE_ITEM_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.SampleSerializable",
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
            dataType = PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      SERIALIZABLE_PARAMETER_METADATA,
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 0,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
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
            id = "com.testdata.SerializableInputFunctions#simpleFunctionNullableSerializableList",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataSerializableInputFunctionsSimpleFunctionNullableSerializableMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_REFERENCE_DATA_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.SampleSerializable",
            isNullable = true,
            description = ""
        )

    private val SERIALIZABLE_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "serializable",
            isRequired = true,
            dataType = PARAMETER_METADATA_SERIALIZABLE_REFERENCE_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      SERIALIZABLE_PARAMETER_METADATA,
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 0,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
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
            id = "com.testdata.SerializableInputFunctions#simpleFunctionNullableSerializable",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataSerializableInputFunctionsSimpleFunctionSerializableListItemNullableMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE_REFERENCE_ITEM_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.SampleSerializable",
            isNullable = true,
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
            dataType = PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      SERIALIZABLE_PARAMETER_METADATA,
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 0,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
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
            id = "com.testdata.SerializableInputFunctions#simpleFunctionSerializableListItemNullable",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataSerializableInputFunctionsSimpleFunctionBothSerializableListAndListItemNullableMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE_REFERENCE_ITEM_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.SampleSerializable",
            isNullable = true,
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
            dataType = PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      SERIALIZABLE_PARAMETER_METADATA,
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 0,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
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
            id = "com.testdata.SerializableInputFunctions#simpleFunctionBothSerializableListAndListItemNullable",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }
}
