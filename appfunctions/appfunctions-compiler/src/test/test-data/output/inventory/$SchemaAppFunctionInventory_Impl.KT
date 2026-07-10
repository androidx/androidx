package androidx.appfunctions.`internal`

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

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SchemaAppFunctionInventory_Impl` : SchemaAppFunctionInventory() {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "fake_schema_category/booleanArrayReturn/1" to FakeSchemaCategoryBooleanArrayReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/booleanReturn/1" to FakeSchemaCategoryBooleanReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/byteArrayReturn/1" to FakeSchemaCategoryByteArrayReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/doubleArrayReturn/1" to FakeSchemaCategoryDoubleArrayReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/doubleReturn/1" to FakeSchemaCategoryDoubleReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/allNullablePrimitiveArrayParamsWithDefault/1" to FakeSchemaCategoryAllNullablePrimitiveArrayParamsWithDefault1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/allNullablePrimitiveParamsWithDefault/1" to FakeSchemaCategoryAllNullablePrimitiveParamsWithDefault1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/allPrimitiveArrayParams/1" to FakeSchemaCategoryAllPrimitiveArrayParams1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/allPrimitiveParams/1" to FakeSchemaCategoryAllPrimitiveParams1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/noArg/1" to FakeSchemaCategoryNoArg1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/floatArrayReturn/1" to FakeSchemaCategoryFloatArrayReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/floatReturn/1" to FakeSchemaCategoryFloatReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/intArrayReturn/1" to FakeSchemaCategoryIntArrayReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/intReturn/1" to FakeSchemaCategoryIntReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/longArrayReturn/1" to FakeSchemaCategoryLongArrayReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/longReturn/1" to FakeSchemaCategoryLongReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/nullableBooleanArrayReturn/1" to FakeSchemaCategoryNullableBooleanArrayReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/nullableBooleanReturn/1" to FakeSchemaCategoryNullableBooleanReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/nullableByteArrayReturn/1" to FakeSchemaCategoryNullableByteArrayReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/nullableDoubleArrayReturn/1" to FakeSchemaCategoryNullableDoubleArrayReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/nullableDoubleReturn/1" to FakeSchemaCategoryNullableDoubleReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/nullableFloatArrayReturn/1" to FakeSchemaCategoryNullableFloatArrayReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/nullableFloatReturn/1" to FakeSchemaCategoryNullableFloatReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/nullableIntArrayReturn/1" to FakeSchemaCategoryNullableIntArrayReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/nullableIntReturn/1" to FakeSchemaCategoryNullableIntReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/nullableLongArrayReturn/1" to FakeSchemaCategoryNullableLongArrayReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/nullableLongReturn/1" to FakeSchemaCategoryNullableLongReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/nullableStringItemListReturn/1" to FakeSchemaCategoryNullableStringItemListReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/nullableStringListReturn/1" to FakeSchemaCategoryNullableStringListReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/nullableStringReturn/1" to FakeSchemaCategoryNullableStringReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/stringListReturn/1" to FakeSchemaCategoryStringListReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/stringReturn/1" to FakeSchemaCategoryStringReturn1MetadataObject.APP_FUNCTION_METADATA,
    "fake_schema_category/unitReturn/1" to FakeSchemaCategoryUnitReturn1MetadataObject.APP_FUNCTION_METADATA,
  )

  private val componentsMetadataDataTypesMap: Map<String, AppFunctionDataTypeMetadata> = mapOf(
    )

  override val componentsMetadata: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
          dataTypes = componentsMetadataDataTypesMap
      )

  private object FakeSchemaCategoryBooleanArrayReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="booleanArrayReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionBooleanTypeMetadata =
        AppFunctionBooleanTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/booleanArrayReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryBooleanReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="booleanReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionBooleanTypeMetadata =
        AppFunctionBooleanTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/booleanReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryByteArrayReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="byteArrayReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionBytesTypeMetadata =
        AppFunctionBytesTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/byteArrayReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryDoubleArrayReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="doubleArrayReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionDoubleTypeMetadata =
        AppFunctionDoubleTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/doubleArrayReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryDoubleReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="doubleReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionDoubleTypeMetadata =
        AppFunctionDoubleTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/doubleReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryAllNullablePrimitiveArrayParamsWithDefault1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allNullablePrimitiveArrayParamsWithDefault", version=1)

    private val PARAMETER_METADATA_ARG1_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionIntTypeMetadata = AppFunctionIntTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = null,
        )

    private val PARAMETER_METADATA_ARG1_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_ARG1_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true,
            description = ""
        )

    private val ARG1_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg1",
            isRequired = false,
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
            isNullable = true,
            description = ""
        )

    private val ARG2_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg2",
            isRequired = false,
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
            isNullable = true,
            description = ""
        )

    private val ARG3_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg3",
            isRequired = false,
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
            isNullable = true,
            description = ""
        )

    private val ARG4_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg4",
            isRequired = false,
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
            isRequired = false,
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
            isNullable = true,
            description = ""
        )

    private val ARG6_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg6",
            isRequired = false,
            dataType = PARAMETER_METADATA_ARG6_ARRAY_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG7_PRIMITIVE_DATA_TYPE: AppFunctionBytesTypeMetadata =
        AppFunctionBytesTypeMetadata(
            isNullable = true,
            description = ""
        )

    private val ARG7_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg7",
            isRequired = false,
            dataType = PARAMETER_METADATA_ARG7_PRIMITIVE_DATA_TYPE,
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
            id = "fake_schema_category/allNullablePrimitiveArrayParamsWithDefault/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryAllNullablePrimitiveParamsWithDefault1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allNullablePrimitiveParamsWithDefault", version=1)

    private val PARAMETER_METADATA_ARG1_PRIMITIVE_DATA_TYPE: AppFunctionIntTypeMetadata =
        AppFunctionIntTypeMetadata(
            isNullable = true,
            description = "",
            enumValues = null,
        )

    private val ARG1_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg1",
            isRequired = false,
            dataType = PARAMETER_METADATA_ARG1_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG2_PRIMITIVE_DATA_TYPE: AppFunctionLongTypeMetadata =
        AppFunctionLongTypeMetadata(
            isNullable = true,
            description = ""
        )

    private val ARG2_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg2",
            isRequired = false,
            dataType = PARAMETER_METADATA_ARG2_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG3_PRIMITIVE_DATA_TYPE: AppFunctionFloatTypeMetadata =
        AppFunctionFloatTypeMetadata(
            isNullable = true,
            description = ""
        )

    private val ARG3_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg3",
            isRequired = false,
            dataType = PARAMETER_METADATA_ARG3_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG4_PRIMITIVE_DATA_TYPE: AppFunctionDoubleTypeMetadata =
        AppFunctionDoubleTypeMetadata(
            isNullable = true,
            description = ""
        )

    private val ARG4_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg4",
            isRequired = false,
            dataType = PARAMETER_METADATA_ARG4_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG5_PRIMITIVE_DATA_TYPE: AppFunctionStringTypeMetadata =
        AppFunctionStringTypeMetadata(
            isNullable = true,
            description = "",
            enumValues = null,
        )

    private val ARG5_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg5",
            isRequired = false,
            dataType = PARAMETER_METADATA_ARG5_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG6_PRIMITIVE_DATA_TYPE: AppFunctionBooleanTypeMetadata =
        AppFunctionBooleanTypeMetadata(
            isNullable = true,
            description = ""
        )

    private val ARG6_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg6",
            isRequired = false,
            dataType = PARAMETER_METADATA_ARG6_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      ARG1_PARAMETER_METADATA,
      ARG2_PARAMETER_METADATA,
      ARG3_PARAMETER_METADATA,
      ARG4_PARAMETER_METADATA,
      ARG5_PARAMETER_METADATA,
      ARG6_PARAMETER_METADATA,
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
            id = "fake_schema_category/allNullablePrimitiveParamsWithDefault/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryAllPrimitiveArrayParams1MetadataObject {
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
            id = "fake_schema_category/allPrimitiveArrayParams/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryAllPrimitiveParams1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveParams", version=1)

    private val PARAMETER_METADATA_ARG1_PRIMITIVE_DATA_TYPE: AppFunctionIntTypeMetadata =
        AppFunctionIntTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = null,
        )

    private val ARG1_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg1",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG1_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG2_PRIMITIVE_DATA_TYPE: AppFunctionLongTypeMetadata =
        AppFunctionLongTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val ARG2_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg2",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG2_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG3_PRIMITIVE_DATA_TYPE: AppFunctionFloatTypeMetadata =
        AppFunctionFloatTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val ARG3_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg3",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG3_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG4_PRIMITIVE_DATA_TYPE: AppFunctionDoubleTypeMetadata =
        AppFunctionDoubleTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val ARG4_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg4",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG4_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG5_PRIMITIVE_DATA_TYPE: AppFunctionStringTypeMetadata =
        AppFunctionStringTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = null,
        )

    private val ARG5_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg5",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG5_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_ARG6_PRIMITIVE_DATA_TYPE: AppFunctionBooleanTypeMetadata =
        AppFunctionBooleanTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val ARG6_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "arg6",
            isRequired = true,
            dataType = PARAMETER_METADATA_ARG6_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      ARG1_PARAMETER_METADATA,
      ARG2_PARAMETER_METADATA,
      ARG3_PARAMETER_METADATA,
      ARG4_PARAMETER_METADATA,
      ARG5_PARAMETER_METADATA,
      ARG6_PARAMETER_METADATA,
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
            id = "fake_schema_category/allPrimitiveParams/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryNoArg1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="noArg", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
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
            id = "fake_schema_category/noArg/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryFloatArrayReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="floatArrayReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionFloatTypeMetadata =
        AppFunctionFloatTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/floatArrayReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryFloatReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="floatReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionFloatTypeMetadata =
        AppFunctionFloatTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/floatReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryIntArrayReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="intArrayReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionIntTypeMetadata =
        AppFunctionIntTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = null,
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/intArrayReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryIntReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="intReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionIntTypeMetadata =
        AppFunctionIntTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = null,
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/intReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryLongArrayReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="longArrayReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionLongTypeMetadata =
        AppFunctionLongTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/longArrayReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryLongReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="longReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionLongTypeMetadata =
        AppFunctionLongTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/longReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryNullableBooleanArrayReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="nullableBooleanArrayReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionBooleanTypeMetadata =
        AppFunctionBooleanTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/nullableBooleanArrayReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryNullableBooleanReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="nullableBooleanReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionBooleanTypeMetadata =
        AppFunctionBooleanTypeMetadata(
            isNullable = true,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/nullableBooleanReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryNullableByteArrayReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="nullableByteArrayReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionBytesTypeMetadata =
        AppFunctionBytesTypeMetadata(
            isNullable = true,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/nullableByteArrayReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryNullableDoubleArrayReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="nullableDoubleArrayReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionDoubleTypeMetadata =
        AppFunctionDoubleTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/nullableDoubleArrayReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryNullableDoubleReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="nullableDoubleReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionDoubleTypeMetadata =
        AppFunctionDoubleTypeMetadata(
            isNullable = true,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/nullableDoubleReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryNullableFloatArrayReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="nullableFloatArrayReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionFloatTypeMetadata =
        AppFunctionFloatTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/nullableFloatArrayReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryNullableFloatReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="nullableFloatReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionFloatTypeMetadata =
        AppFunctionFloatTypeMetadata(
            isNullable = true,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/nullableFloatReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryNullableIntArrayReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="nullableIntArrayReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionIntTypeMetadata =
        AppFunctionIntTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = null,
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/nullableIntArrayReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryNullableIntReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="nullableIntReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionIntTypeMetadata =
        AppFunctionIntTypeMetadata(
            isNullable = true,
            description = "",
            enumValues = null,
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/nullableIntReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryNullableLongArrayReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="nullableLongArrayReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionLongTypeMetadata =
        AppFunctionLongTypeMetadata(
            isNullable = false,
            description = ""
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/nullableLongArrayReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryNullableLongReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="nullableLongReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionLongTypeMetadata =
        AppFunctionLongTypeMetadata(
            isNullable = true,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/nullableLongReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryNullableStringItemListReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="nullableStringItemListReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionStringTypeMetadata =
        AppFunctionStringTypeMetadata(
            isNullable = true,
            description = "",
            enumValues = null,
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/nullableStringItemListReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryNullableStringListReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="nullableStringListReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionStringTypeMetadata =
        AppFunctionStringTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = null,
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/nullableStringListReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryNullableStringReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="nullableStringReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionStringTypeMetadata =
        AppFunctionStringTypeMetadata(
            isNullable = true,
            description = "",
            enumValues = null,
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/nullableStringReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryStringListReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="stringListReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionStringTypeMetadata =
        AppFunctionStringTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = null,
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/stringListReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryStringReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="stringReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionStringTypeMetadata =
        AppFunctionStringTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = null,
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "fake_schema_category/stringReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object FakeSchemaCategoryUnitReturn1MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="unitReturn", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
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
            id = "fake_schema_category/unitReturn/1",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }
}
