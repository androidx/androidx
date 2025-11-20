package com.testdata

import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
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
 * IntEnumValueFunctions.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$IntEnumValueFunctions_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.IntEnumValueFunctions#intEnumParamFunction" to ComTestdataIntEnumValueFunctionsIntEnumParamFunctionMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.IntEnumValueFunctions#intEnumReturnFunction" to ComTestdataIntEnumValueFunctionsIntEnumReturnFunctionMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.IntEnumValueFunctions#intEnumSerializableFunction" to ComTestdataIntEnumValueFunctionsIntEnumSerializableFunctionMetadataObject.APP_FUNCTION_METADATA,
  )

  private val COM_TESTDATA_INTENUMPROPSERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_INTPROP:
      AppFunctionIntTypeMetadata = AppFunctionIntTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = setOf(0, 1),
      )

  private val COM_TESTDATA_INTENUMPROPSERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "intProp" to COM_TESTDATA_INTENUMPROPSERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP_INTPROP,
  )

  private val COM_TESTDATA_INTENUMPROPSERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
      List<String> = listOf(
    "intProp",
  )

  private val COM_TESTDATA_INTENUMPROPSERIALIZABLE_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
      AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_INTENUMPROPSERIALIZABLE_OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = COM_TESTDATA_INTENUMPROPSERIALIZABLE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "com.testdata.IntEnumPropSerializable",
          isNullable = true,
          description = ""
      )

  private val componentsMetadataDataTypesMap: Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "com.testdata.IntEnumPropSerializable" to COM_TESTDATA_INTENUMPROPSERIALIZABLE_OBJECT_DATA_TYPE,
    )

  override val componentsMetadata: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
          dataTypes = componentsMetadataDataTypesMap
      )

  private object ComTestdataIntEnumValueFunctionsIntEnumParamFunctionMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_INTPARAM_PRIMITIVE_DATA_TYPE: AppFunctionIntTypeMetadata =
        AppFunctionIntTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = setOf(0, 1),
        )

    private val INTPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "intParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_INTPARAM_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      INTPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.IntEnumValueFunctions#intEnumParamFunction",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataIntEnumValueFunctionsIntEnumReturnFunctionMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionIntTypeMetadata =
        AppFunctionIntTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = setOf(0, 1),
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.IntEnumValueFunctions#intEnumReturnFunction",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataIntEnumValueFunctionsIntEnumSerializableFunctionMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_INTENUMPROPSERIALIZABLE_REFERENCE_DATA_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.IntEnumPropSerializable",
            isNullable = false,
            description = ""
        )

    private val INTENUMPROPSERIALIZABLE_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "intEnumPropSerializable",
            isRequired = true,
            dataType = PARAMETER_METADATA_INTENUMPROPSERIALIZABLE_REFERENCE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      INTENUMPROPSERIALIZABLE_PARAMETER_METADATA,
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
            id = "com.testdata.IntEnumValueFunctions#intEnumSerializableFunction",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }
}
