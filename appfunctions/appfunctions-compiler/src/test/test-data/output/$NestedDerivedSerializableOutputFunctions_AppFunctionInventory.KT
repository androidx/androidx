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
 * NestedDerivedSerializableOutputFunctions.....kt
 * NestedDerivedSerializable.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$NestedDerivedSerializableOutputFunctions_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.NestedDerivedSerializableOutputFunctions#simpleFunction" to ComTestdataNestedDerivedSerializableOutputFunctionsSimpleFunctionMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.NestedDerivedSerializableOutputFunctions#simpleFunctionSerializableList" to ComTestdataNestedDerivedSerializableOutputFunctionsSimpleFunctionSerializableListMetadataObject.APP_FUNCTION_METADATA,
  )

  private object ComTestdataNestedDerivedSerializableOutputFunctionsSimpleFunctionMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val REFERENCE_RESPONSE_VALUE_TYPE: AppFunctionReferenceTypeMetadata =
        AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.NestedDerivedSerializable",
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = REFERENCE_RESPONSE_VALUE_TYPE
        )

    private val COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_STRINGBASENAME:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_NESTEDBASE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.NestedBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "stringBaseName" to COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_STRINGBASENAME,
      "nestedBase" to COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_NESTEDBASE,
    )

    private val COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "stringBaseName",
      "nestedBase",
    )

    private val COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata
        = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP,
            required = COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.NestedBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.NestedBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_LONGBASEVALUE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_NESTEDDERIVED:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.NestedDerivedSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "longBaseValue" to COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_LONGBASEVALUE,
      "nestedDerived" to COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_NESTEDDERIVED,
    )

    private val COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "longBaseValue",
      "nestedDerived",
    )

    private val COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP,
            required = COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.NonNestedChildSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
        List<AppFunctionDataTypeMetadata> = listOf(
      COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
      COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1,
    )

    private val COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE:
        AppFunctionAllOfTypeMetadata = AppFunctionAllOfTypeMetadata(
            matchAll = COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
            qualifiedName = "com.testdata.NonNestedChildSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_NESTEDDERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.NonNestedChildSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_NESTEDDERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
        List<AppFunctionDataTypeMetadata> = listOf(
      COM_TESTDATA_NESTEDDERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
    )

    private val COM_TESTDATA_NESTEDDERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE:
        AppFunctionAllOfTypeMetadata = AppFunctionAllOfTypeMetadata(
            matchAll = COM_TESTDATA_NESTEDDERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
            qualifiedName = "com.testdata.NestedDerivedSerializable",
            isNullable = true,
            description = ""
        )

    private val COMPONENTS_METADATA_DATA_TYPES_MAP: Map<String, AppFunctionDataTypeMetadata> =
        mapOf(
      "com.testdata.NestedBaseSerializable" to COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE,
      "com.testdata.NonNestedChildSerializable" to COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE,
      "com.testdata.NestedDerivedSerializable" to COM_TESTDATA_NESTEDDERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE,
      )

    private val COMPONENTS_METADATA: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
            dataTypes = COMPONENTS_METADATA_DATA_TYPES_MAP
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.NestedDerivedSerializableOutputFunctions#simpleFunction",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataNestedDerivedSerializableOutputFunctionsSimpleFunctionSerializableListMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_REFERENCE_ITEM_TYPE: AppFunctionReferenceTypeMetadata =
        AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.NestedDerivedSerializable",
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

    private val COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_STRINGBASENAME:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_NESTEDBASE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.NestedBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "stringBaseName" to COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_STRINGBASENAME,
      "nestedBase" to COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_NESTEDBASE,
    )

    private val COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "stringBaseName",
      "nestedBase",
    )

    private val COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata
        = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP,
            required = COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.NestedBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.NestedBaseSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_LONGBASEVALUE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_NESTEDDERIVED:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.NestedDerivedSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "longBaseValue" to COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_LONGBASEVALUE,
      "nestedDerived" to COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP_NESTEDDERIVED,
    )

    private val COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "longBaseValue",
      "nestedDerived",
    )

    private val COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_PROPERTIES_MAP,
            required = COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.NonNestedChildSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
        List<AppFunctionDataTypeMetadata> = listOf(
      COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
      COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_1,
    )

    private val COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE:
        AppFunctionAllOfTypeMetadata = AppFunctionAllOfTypeMetadata(
            matchAll = COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
            qualifiedName = "com.testdata.NonNestedChildSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_NESTEDDERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.NonNestedChildSerializable",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_NESTEDDERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
        List<AppFunctionDataTypeMetadata> = listOf(
      COM_TESTDATA_NESTEDDERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
    )

    private val COM_TESTDATA_NESTEDDERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE:
        AppFunctionAllOfTypeMetadata = AppFunctionAllOfTypeMetadata(
            matchAll = COM_TESTDATA_NESTEDDERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
            qualifiedName = "com.testdata.NestedDerivedSerializable",
            isNullable = true,
            description = ""
        )

    private val COMPONENTS_METADATA_DATA_TYPES_MAP: Map<String, AppFunctionDataTypeMetadata> =
        mapOf(
      "com.testdata.NestedBaseSerializable" to COM_TESTDATA_NESTEDBASESERIALIZABLE_OBJECT_DATA_TYPE,
      "com.testdata.NonNestedChildSerializable" to COM_TESTDATA_NONNESTEDCHILDSERIALIZABLE_ALL_OF_DATA_TYPE,
      "com.testdata.NestedDerivedSerializable" to COM_TESTDATA_NESTEDDERIVEDSERIALIZABLE_ALL_OF_DATA_TYPE,
      )

    private val COMPONENTS_METADATA: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
            dataTypes = COMPONENTS_METADATA_DATA_TYPES_MAP
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.NestedDerivedSerializableOutputFunctions#simpleFunctionSerializableList",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }
}
