package com.testdata

import androidx.appfunctions.metadata.AppFunctionAllOfTypeMetadata
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
 * DerivedSerializableInputFunctions.....kt
 * DerivedSerializable.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$DerivedSerializableInputFunctions_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.DerivedSerializableInputFunctions#simpleFunctionWithDerivedSerializable" to ComTestdataDerivedSerializableInputFunctionsSimpleFunctionWithDerivedSerializableMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.DerivedSerializableInputFunctions#simpleFunctionWithDerivedSerializableList" to ComTestdataDerivedSerializableInputFunctionsSimpleFunctionWithDerivedSerializableListMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.DerivedSerializableInputFunctions#simpleFunctionWithNullableDerivedSerializableList" to ComTestdataDerivedSerializableInputFunctionsSimpleFunctionWithNullableDerivedSerializableListMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.DerivedSerializableInputFunctions#simpleFunctionWithNullableDerivedSerializable" to ComTestdataDerivedSerializableInputFunctionsSimpleFunctionWithNullableDerivedSerializableMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.DerivedSerializableInputFunctions#simpleFunctionWithDerivedSerializableListItemNullable" to ComTestdataDerivedSerializableInputFunctionsSimpleFunctionWithDerivedSerializableListItemNullableMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.DerivedSerializableInputFunctions#simpleFunctionWithBothDerivedSerializableListAndListItemNullable" to ComTestdataDerivedSerializableInputFunctionsSimpleFunctionWithBothDerivedSerializableListAndListItemNullableMetadataObject.APP_FUNCTION_METADATA,
  )

  private object ComTestdataDerivedSerializableInputFunctionsSimpleFunctionWithDerivedSerializableMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_REFERENCE_DATA_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.DerivedSerializable",
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

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_STRINGTOOPEN:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "stringToOpen" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_STRINGTOOPEN,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "stringToOpen",
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP,
            required = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.AppFunctionOpenable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_AGE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "age" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_AGE,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "age",
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP,
            required = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
        List<AppFunctionDataTypeMetadata> = listOf(
      COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
      COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE: AppFunctionAllOfTypeMetadata =
        AppFunctionAllOfTypeMetadata(
            matchAll = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
            qualifiedName = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
        List<AppFunctionDataTypeMetadata> = listOf(
      COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
    )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE: AppFunctionAllOfTypeMetadata =
        AppFunctionAllOfTypeMetadata(
            matchAll = COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
            qualifiedName = "com.testdata.DerivedSerializable",
            isNullable = true,
            description = ""
        )

    private val COMPONENTS_METADATA_DATA_TYPES_MAP: Map<String, AppFunctionDataTypeMetadata> =
        mapOf(
      "com.testdata.LongBaseSerializable" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE,
      "com.testdata.DerivedSerializable" to COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE,
      )

    private val COMPONENTS_METADATA: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
            dataTypes = COMPONENTS_METADATA_DATA_TYPES_MAP
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.DerivedSerializableInputFunctions#simpleFunctionWithDerivedSerializable",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataDerivedSerializableInputFunctionsSimpleFunctionWithDerivedSerializableListMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE_REFERENCE_ITEM_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.DerivedSerializable",
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

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_STRINGTOOPEN:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "stringToOpen" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_STRINGTOOPEN,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "stringToOpen",
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP,
            required = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.AppFunctionOpenable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_AGE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "age" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_AGE,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "age",
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP,
            required = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
        List<AppFunctionDataTypeMetadata> = listOf(
      COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
      COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE: AppFunctionAllOfTypeMetadata =
        AppFunctionAllOfTypeMetadata(
            matchAll = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
            qualifiedName = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
        List<AppFunctionDataTypeMetadata> = listOf(
      COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
    )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE: AppFunctionAllOfTypeMetadata =
        AppFunctionAllOfTypeMetadata(
            matchAll = COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
            qualifiedName = "com.testdata.DerivedSerializable",
            isNullable = true,
            description = ""
        )

    private val COMPONENTS_METADATA_DATA_TYPES_MAP: Map<String, AppFunctionDataTypeMetadata> =
        mapOf(
      "com.testdata.LongBaseSerializable" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE,
      "com.testdata.DerivedSerializable" to COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE,
      )

    private val COMPONENTS_METADATA: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
            dataTypes = COMPONENTS_METADATA_DATA_TYPES_MAP
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.DerivedSerializableInputFunctions#simpleFunctionWithDerivedSerializableList",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataDerivedSerializableInputFunctionsSimpleFunctionWithNullableDerivedSerializableListMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE_REFERENCE_ITEM_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.DerivedSerializable",
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

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_STRINGTOOPEN:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "stringToOpen" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_STRINGTOOPEN,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "stringToOpen",
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP,
            required = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.AppFunctionOpenable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_AGE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "age" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_AGE,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "age",
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP,
            required = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
        List<AppFunctionDataTypeMetadata> = listOf(
      COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
      COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE: AppFunctionAllOfTypeMetadata =
        AppFunctionAllOfTypeMetadata(
            matchAll = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
            qualifiedName = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
        List<AppFunctionDataTypeMetadata> = listOf(
      COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
    )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE: AppFunctionAllOfTypeMetadata =
        AppFunctionAllOfTypeMetadata(
            matchAll = COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
            qualifiedName = "com.testdata.DerivedSerializable",
            isNullable = true,
            description = ""
        )

    private val COMPONENTS_METADATA_DATA_TYPES_MAP: Map<String, AppFunctionDataTypeMetadata> =
        mapOf(
      "com.testdata.LongBaseSerializable" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE,
      "com.testdata.DerivedSerializable" to COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE,
      )

    private val COMPONENTS_METADATA: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
            dataTypes = COMPONENTS_METADATA_DATA_TYPES_MAP
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.DerivedSerializableInputFunctions#simpleFunctionWithNullableDerivedSerializableList",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataDerivedSerializableInputFunctionsSimpleFunctionWithNullableDerivedSerializableMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_REFERENCE_DATA_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.DerivedSerializable",
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

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_STRINGTOOPEN:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "stringToOpen" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_STRINGTOOPEN,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "stringToOpen",
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP,
            required = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.AppFunctionOpenable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_AGE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "age" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_AGE,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "age",
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP,
            required = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
        List<AppFunctionDataTypeMetadata> = listOf(
      COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
      COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE: AppFunctionAllOfTypeMetadata =
        AppFunctionAllOfTypeMetadata(
            matchAll = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
            qualifiedName = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
        List<AppFunctionDataTypeMetadata> = listOf(
      COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
    )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE: AppFunctionAllOfTypeMetadata =
        AppFunctionAllOfTypeMetadata(
            matchAll = COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
            qualifiedName = "com.testdata.DerivedSerializable",
            isNullable = true,
            description = ""
        )

    private val COMPONENTS_METADATA_DATA_TYPES_MAP: Map<String, AppFunctionDataTypeMetadata> =
        mapOf(
      "com.testdata.LongBaseSerializable" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE,
      "com.testdata.DerivedSerializable" to COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE,
      )

    private val COMPONENTS_METADATA: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
            dataTypes = COMPONENTS_METADATA_DATA_TYPES_MAP
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.DerivedSerializableInputFunctions#simpleFunctionWithNullableDerivedSerializable",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataDerivedSerializableInputFunctionsSimpleFunctionWithDerivedSerializableListItemNullableMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE_REFERENCE_ITEM_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.DerivedSerializable",
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

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_STRINGTOOPEN:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "stringToOpen" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_STRINGTOOPEN,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "stringToOpen",
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP,
            required = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.AppFunctionOpenable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_AGE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "age" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_AGE,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "age",
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP,
            required = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
        List<AppFunctionDataTypeMetadata> = listOf(
      COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
      COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE: AppFunctionAllOfTypeMetadata =
        AppFunctionAllOfTypeMetadata(
            matchAll = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
            qualifiedName = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
        List<AppFunctionDataTypeMetadata> = listOf(
      COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
    )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE: AppFunctionAllOfTypeMetadata =
        AppFunctionAllOfTypeMetadata(
            matchAll = COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
            qualifiedName = "com.testdata.DerivedSerializable",
            isNullable = true,
            description = ""
        )

    private val COMPONENTS_METADATA_DATA_TYPES_MAP: Map<String, AppFunctionDataTypeMetadata> =
        mapOf(
      "com.testdata.LongBaseSerializable" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE,
      "com.testdata.DerivedSerializable" to COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE,
      )

    private val COMPONENTS_METADATA: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
            dataTypes = COMPONENTS_METADATA_DATA_TYPES_MAP
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.DerivedSerializableInputFunctions#simpleFunctionWithDerivedSerializableListItemNullable",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataDerivedSerializableInputFunctionsSimpleFunctionWithBothDerivedSerializableListAndListItemNullableMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_ARRAY_DATA_TYPE_REFERENCE_ITEM_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.DerivedSerializable",
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

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_STRINGTOOPEN:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "stringToOpen" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_STRINGTOOPEN,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "stringToOpen",
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP,
            required = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.AppFunctionOpenable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_AGE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "age" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_AGE,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "age",
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP,
            required = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
        List<AppFunctionDataTypeMetadata> = listOf(
      COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
      COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1,
    )

    private val COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE: AppFunctionAllOfTypeMetadata =
        AppFunctionAllOfTypeMetadata(
            matchAll = COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
            qualifiedName = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.LongBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
        List<AppFunctionDataTypeMetadata> = listOf(
      COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
    )

    private val COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE: AppFunctionAllOfTypeMetadata =
        AppFunctionAllOfTypeMetadata(
            matchAll = COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
            qualifiedName = "com.testdata.DerivedSerializable",
            isNullable = true,
            description = ""
        )

    private val COMPONENTS_METADATA_DATA_TYPES_MAP: Map<String, AppFunctionDataTypeMetadata> =
        mapOf(
      "com.testdata.LongBaseSerializable" to COM_TESTDATA_LONGBASESERIALIZABLE_ALL_OF_DATA_TYPE,
      "com.testdata.DerivedSerializable" to COM_TESTDATA_DERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE,
      )

    private val COMPONENTS_METADATA: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
            dataTypes = COMPONENTS_METADATA_DATA_TYPES_MAP
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.DerivedSerializableInputFunctions#simpleFunctionWithBothDerivedSerializableListAndListItemNullable",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }
}
