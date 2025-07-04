package com.testdata

import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
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
 * FakeAllPrimitiveParamsImpl.....kt
 * FakeSchemas.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$FakeAllPrimitiveParamsImpl_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.FakeAllPrimitiveParamsImpl#allPrimitiveParams" to ComTestdataFakeAllPrimitiveParamsImplAllPrimitiveParamsMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveParamsImpl#allPrimitiveArrayParams" to ComTestdataFakeAllPrimitiveParamsImplAllPrimitiveArrayParamsMetadataObject.APP_FUNCTION_METADATA,
  )

  private object ComTestdataFakeAllPrimitiveParamsImplAllPrimitiveParamsMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveParams", version=1)

    private val PARAMETER_METADATA_ARG1_PRIMITIVE_DATA_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 7,
            isNullable = false,
            description = ""
        )

    private val ARG1_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg1",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG1_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_ARG2_PRIMITIVE_DATA_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = false,
            description = ""
        )

    private val ARG2_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg2",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG2_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_ARG3_PRIMITIVE_DATA_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 5,
            isNullable = false,
            description = ""
        )

    private val ARG3_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg3",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG3_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_ARG4_PRIMITIVE_DATA_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 4,
            isNullable = false,
            description = ""
        )

    private val ARG4_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg4",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG4_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_ARG5_PRIMITIVE_DATA_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val ARG5_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg5",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG5_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_ARG6_PRIMITIVE_DATA_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 1,
            isNullable = false,
            description = ""
        )

    private val ARG6_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg6",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG6_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      ARG1_PARAMETER_METADATA,
      ARG2_PARAMETER_METADATA,
      ARG3_PARAMETER_METADATA,
      ARG4_PARAMETER_METADATA,
      ARG5_PARAMETER_METADATA,
      ARG6_PARAMETER_METADATA,
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

    private val COMPONENTS_METADATA_DATA_TYPES_MAP: Map<String, AppFunctionDataTypeMetadata> =
        mapOf(
      )

    private val COMPONENTS_METADATA: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
            dataTypes = COMPONENTS_METADATA_DATA_TYPES_MAP
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveParamsImpl#allPrimitiveParams",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveParamsImplAllPrimitiveArrayParamsMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveParams", version=1)

    private val PARAMETER_METADATA_ARG1_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 7,
            isNullable = false,
            description = ""
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
            dataType = PARAMETER_METADATA_ARG1_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_ARG2_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 6,
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
            dataType = PARAMETER_METADATA_ARG2_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_ARG3_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 5,
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
            dataType = PARAMETER_METADATA_ARG3_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_ARG4_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 4,
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
            dataType = PARAMETER_METADATA_ARG4_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_ARG5_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
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
            dataType = PARAMETER_METADATA_ARG5_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_ARG6_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 1,
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
            dataType = PARAMETER_METADATA_ARG6_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_ARG7_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 2,
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_ARG7_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_ARG7_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val ARG7_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg7",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG7_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_ARG8_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = true,
            description = ""
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
            dataType = PARAMETER_METADATA_ARG8_ARRAY_DATA_TYPE
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

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 0,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
        )

    private val COMPONENTS_METADATA_DATA_TYPES_MAP: Map<String, AppFunctionDataTypeMetadata> =
        mapOf(
      )

    private val COMPONENTS_METADATA: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
            dataTypes = COMPONENTS_METADATA_DATA_TYPES_MAP
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveParamsImpl#allPrimitiveArrayParams",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }
}
