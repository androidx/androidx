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
 * FakeAllPrimitiveReturnsImpl.....kt
 * FakeSchemas.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$FakeAllPrimitiveReturnsImpl_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.FakeAllPrimitiveReturnsImpl#unitReturn" to ComTestdataFakeAllPrimitiveReturnsImplUnitReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#byteArrayReturn" to ComTestdataFakeAllPrimitiveReturnsImplByteArrayReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#intReturn" to ComTestdataFakeAllPrimitiveReturnsImplIntReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#longReturn" to ComTestdataFakeAllPrimitiveReturnsImplLongReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#floatReturn" to ComTestdataFakeAllPrimitiveReturnsImplFloatReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#doubleReturn" to ComTestdataFakeAllPrimitiveReturnsImplDoubleReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#stringReturn" to ComTestdataFakeAllPrimitiveReturnsImplStringReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#booleanReturn" to ComTestdataFakeAllPrimitiveReturnsImplBooleanReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#nullableByteArrayReturn" to ComTestdataFakeAllPrimitiveReturnsImplNullableByteArrayReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#nullableIntReturn" to ComTestdataFakeAllPrimitiveReturnsImplNullableIntReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#nullableLongReturn" to ComTestdataFakeAllPrimitiveReturnsImplNullableLongReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#nullableFloatReturn" to ComTestdataFakeAllPrimitiveReturnsImplNullableFloatReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#nullableDoubleReturn" to ComTestdataFakeAllPrimitiveReturnsImplNullableDoubleReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#nullableStringReturn" to ComTestdataFakeAllPrimitiveReturnsImplNullableStringReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#nullableBooleanReturn" to ComTestdataFakeAllPrimitiveReturnsImplNullableBooleanReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#intArrayReturn" to ComTestdataFakeAllPrimitiveReturnsImplIntArrayReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#longArrayReturn" to ComTestdataFakeAllPrimitiveReturnsImplLongArrayReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#floatArrayReturn" to ComTestdataFakeAllPrimitiveReturnsImplFloatArrayReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#doubleArrayReturn" to ComTestdataFakeAllPrimitiveReturnsImplDoubleArrayReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#stringListReturn" to ComTestdataFakeAllPrimitiveReturnsImplStringListReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#booleanArrayReturn" to ComTestdataFakeAllPrimitiveReturnsImplBooleanArrayReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#nullableStringListReturn" to ComTestdataFakeAllPrimitiveReturnsImplNullableStringListReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#nullableIntArrayReturn" to ComTestdataFakeAllPrimitiveReturnsImplNullableIntArrayReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#nullableLongArrayReturn" to ComTestdataFakeAllPrimitiveReturnsImplNullableLongArrayReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#nullableDoubleArrayReturn" to ComTestdataFakeAllPrimitiveReturnsImplNullableDoubleArrayReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#nullableBooleanArrayReturn" to ComTestdataFakeAllPrimitiveReturnsImplNullableBooleanArrayReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#nullableFloatArrayReturn" to ComTestdataFakeAllPrimitiveReturnsImplNullableFloatArrayReturnMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FakeAllPrimitiveReturnsImpl#nullableStringItemListReturn" to ComTestdataFakeAllPrimitiveReturnsImplNullableStringItemListReturnMetadataObject.APP_FUNCTION_METADATA,
  )

  private val componentsMetadataDataTypesMap: Map<String, AppFunctionDataTypeMetadata> = mapOf(
    )

  override val componentsMetadata: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
          dataTypes = componentsMetadataDataTypesMap
      )

  private object ComTestdataFakeAllPrimitiveReturnsImplUnitReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#unitReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplByteArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#byteArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplIntReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#intReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplLongReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#longReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplFloatReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#floatReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplDoubleReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#doubleReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplStringReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#stringReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplBooleanReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#booleanReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableByteArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableByteArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableIntReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableIntReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableLongReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableLongReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableFloatReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableFloatReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableDoubleReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableDoubleReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableStringReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableStringReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableBooleanReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableBooleanReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplIntArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#intArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplLongArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#longArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplFloatArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#floatArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplDoubleArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#doubleArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplStringListReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#stringListReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplBooleanArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#booleanArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableStringListReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableStringListReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableIntArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableIntArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableLongArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableLongArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableDoubleArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableDoubleArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableBooleanArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableBooleanArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableFloatArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableFloatArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableStringItemListReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "fake_schema_category", name="allPrimitiveReturns", version=1)

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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableStringItemListReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }
}
