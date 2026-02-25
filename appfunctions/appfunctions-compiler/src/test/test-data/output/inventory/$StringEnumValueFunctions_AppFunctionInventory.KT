package com.testdata

import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.appfunctions.metadata.AppFunctionUnitTypeMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata
import javax.`annotation`.processing.Generated
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

/**
 * Source Files:
 * StringEnumValueFunctions.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$StringEnumValueFunctions_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.StringEnumValueFunctions#stringEnumParamFunction" to ComTestdataStringEnumValueFunctionsStringEnumParamFunctionMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.StringEnumValueFunctions#stringEnumReturnFunction" to ComTestdataStringEnumValueFunctionsStringEnumReturnFunctionMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.StringEnumValueFunctions#stringEnumSerializableFunction" to ComTestdataStringEnumValueFunctionsStringEnumSerializableFunctionMetadataObject.APP_FUNCTION_METADATA,
  )

  private val COM_TESTDATA_STRINGENUMPROPSERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_STRINGPROP:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = setOf("ON", "OFF"),
      )

  private val COM_TESTDATA_STRINGENUMPROPSERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "stringProp" to COM_TESTDATA_STRINGENUMPROPSERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_STRINGPROP,
  )

  private val COM_TESTDATA_STRINGENUMPROPSERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
      List<String> = listOf(
    "stringProp",
  )

  private val COM_TESTDATA_STRINGENUMPROPSERIALIZABLE_OBJECT_DATA_TYPE:
      AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_STRINGENUMPROPSERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = COM_TESTDATA_STRINGENUMPROPSERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "com.testdata.StringEnumPropSerializable",
          isNullable = true,
          description = ""
      )

  private val componentsMetadataDataTypesMap: Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "com.testdata.StringEnumPropSerializable" to COM_TESTDATA_STRINGENUMPROPSERIALIZABLE_OBJECT_DATA_TYPE,
    )

  override val componentsMetadata: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
          dataTypes = componentsMetadataDataTypesMap
      )

  private object ComTestdataStringEnumValueFunctionsStringEnumParamFunctionMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_STRINGPARAM_PRIMITIVE_DATA_TYPE: AppFunctionStringTypeMetadata =
        AppFunctionStringTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = setOf("ON", "OFF"),
        )

    private val STRINGPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "stringParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_STRINGPARAM_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      STRINGPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.StringEnumValueFunctions#stringEnumParamFunction",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataStringEnumValueFunctionsStringEnumReturnFunctionMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionStringTypeMetadata =
        AppFunctionStringTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = setOf("ON", "OFF"),
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.StringEnumValueFunctions#stringEnumReturnFunction",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataStringEnumValueFunctionsStringEnumSerializableFunctionMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_STRINGENUMPROPSERIALIZABLE_REFERENCE_DATA_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.StringEnumPropSerializable",
            isNullable = false,
            description = ""
        )

    private val STRINGENUMPROPSERIALIZABLE_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "stringEnumPropSerializable",
            isRequired = true,
            dataType = PARAMETER_METADATA_STRINGENUMPROPSERIALIZABLE_REFERENCE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      STRINGENUMPROPSERIALIZABLE_PARAMETER_METADATA,
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
            id = "com.testdata.StringEnumValueFunctions#stringEnumSerializableFunction",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }
}
