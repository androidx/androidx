package com.testdata

import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBooleanTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBytesTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDoubleTypeMetadata
import androidx.appfunctions.metadata.AppFunctionFloatTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
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
 * FakeAllPrimitiveParamsImpl.....kt
 * FakeSchemas.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$FakeAllPrimitiveArrayParamsImpl_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.FakeAllPrimitiveArrayParamsImpl#allPrimitiveArrayParams" to ComTestdataFakeAllPrimitiveArrayParamsImplAllPrimitiveArrayParamsMetadataObject.APP_FUNCTION_METADATA,
  )

  private val componentsMetadataDataTypesMap: Map<String, AppFunctionDataTypeMetadata> = mapOf(
    )

  override val componentsMetadata: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
          dataTypes = componentsMetadataDataTypesMap
      )

  private object ComTestdataFakeAllPrimitiveArrayParamsImplAllPrimitiveArrayParamsMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveArrayParams", version=1)

    private val PARAMETER_METADATA_ARG1_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionIntTypeMetadata = AppFunctionIntTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = null,
        )

    private val PARAMETER_METADATA_ARG1_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_ARG1_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val ARG1_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg1",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG1_ARRAY_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG2_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionLongTypeMetadata = AppFunctionLongTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_ARG2_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_ARG2_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val ARG2_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg2",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG2_ARRAY_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG3_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionFloatTypeMetadata = AppFunctionFloatTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_ARG3_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_ARG3_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val ARG3_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg3",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG3_ARRAY_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG4_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionDoubleTypeMetadata = AppFunctionDoubleTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_ARG4_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_ARG4_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val ARG4_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg4",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG4_ARRAY_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG5_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = null,
        )

    private val PARAMETER_METADATA_ARG5_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_ARG5_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val ARG5_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg5",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG5_ARRAY_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG6_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionBooleanTypeMetadata = AppFunctionBooleanTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_ARG6_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_ARG6_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val ARG6_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg6",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG6_ARRAY_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG7_PRIMITIVE_DATA_TYPE: AppFunctionBytesTypeMetadata =
        AppFunctionBytesTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val ARG7_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg7",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG7_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG8_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
            isNullable = true,
            description = "",
            enumValues = null,
        )

    private val PARAMETER_METADATA_ARG8_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_ARG8_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val ARG8_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg8",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG8_ARRAY_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      ARG1_PARAMETER_METADATA,
      ARG2_PARAMETER_METADATA,
      ARG3_PARAMETER_METADATA,
      ARG4_PARAMETER_METADATA,
      ARG5_PARAMETER_METADATA,
      ARG6_PARAMETER_METADATA,
      ARG7_PARAMETER_METADATA,
      ARG8_PARAMETER_METADATA,
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
            id = "com.testdata.FakeAllPrimitiveArrayParamsImpl#allPrimitiveArrayParams",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }
}
