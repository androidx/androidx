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
 * AllPrimitiveInputFunctions.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$AllPrimitiveInputFunctions_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionInt" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionIntMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionLong" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionLongMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionFloat" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionFloatMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionDouble" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionDoubleMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionBoolean" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionBooleanMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionString" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionStringMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableInt" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableIntMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableLong" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableLongMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableFloat" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableFloatMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableDouble" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableDoubleMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableBoolean" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableBooleanMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableString" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableStringMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionIntArray" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionIntArrayMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionLongArray" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionLongArrayMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionFloatArray" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionFloatArrayMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionDoubleArray" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionDoubleArrayMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionBooleanArray" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionBooleanArrayMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionStringList" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionStringListMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableIntArray" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableIntArrayMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableLongArray" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableLongArrayMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableFloatArray" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableFloatArrayMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableDoubleArray" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableDoubleArrayMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableBooleanArray" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableBooleanArrayMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableStringList" to ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableStringListMetadataObject.APP_FUNCTION_METADATA,
  )

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionIntMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_INTPARAM_PRIMITIVE_DATA_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 7,
            isNullable = false,
            description = ""
        )

    private val INTPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "intParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_INTPARAM_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      INTPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionInt",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionLongMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LONGPARAM_PRIMITIVE_DATA_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = false,
            description = ""
        )

    private val LONGPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "longParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_LONGPARAM_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      LONGPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionLong",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionFloatMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_FLOATPARAM_PRIMITIVE_DATA_TYPE: AppFunctionPrimitiveTypeMetadata
        = AppFunctionPrimitiveTypeMetadata(
            type = 5,
            isNullable = false,
            description = ""
        )

    private val FLOATPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "floatParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_FLOATPARAM_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      FLOATPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionFloat",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionDoubleMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_DOUBLEPARAM_PRIMITIVE_DATA_TYPE: AppFunctionPrimitiveTypeMetadata
        = AppFunctionPrimitiveTypeMetadata(
            type = 4,
            isNullable = false,
            description = ""
        )

    private val DOUBLEPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "doubleParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_DOUBLEPARAM_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      DOUBLEPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionDouble",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionBooleanMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_BOOLEANPARAM_PRIMITIVE_DATA_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 1,
            isNullable = false,
            description = ""
        )

    private val BOOLEANPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "booleanParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_BOOLEANPARAM_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      BOOLEANPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionBoolean",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionStringMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_STRINGPARAM_PRIMITIVE_DATA_TYPE: AppFunctionPrimitiveTypeMetadata
        = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val STRINGPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "stringParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_STRINGPARAM_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      STRINGPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionString",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableIntMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_INTPARAM_PRIMITIVE_DATA_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 7,
            isNullable = true,
            description = ""
        )

    private val INTPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "intParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_INTPARAM_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      INTPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableInt",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableLongMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LONGPARAM_PRIMITIVE_DATA_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = true,
            description = ""
        )

    private val LONGPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "longParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_LONGPARAM_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      LONGPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableLong",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableFloatMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_FLOATPARAM_PRIMITIVE_DATA_TYPE: AppFunctionPrimitiveTypeMetadata
        = AppFunctionPrimitiveTypeMetadata(
            type = 5,
            isNullable = true,
            description = ""
        )

    private val FLOATPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "floatParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_FLOATPARAM_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      FLOATPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableFloat",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableDoubleMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_DOUBLEPARAM_PRIMITIVE_DATA_TYPE: AppFunctionPrimitiveTypeMetadata
        = AppFunctionPrimitiveTypeMetadata(
            type = 4,
            isNullable = true,
            description = ""
        )

    private val DOUBLEPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "doubleParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_DOUBLEPARAM_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      DOUBLEPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableDouble",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableBooleanMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_BOOLEANPARAM_PRIMITIVE_DATA_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 1,
            isNullable = true,
            description = ""
        )

    private val BOOLEANPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "booleanParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_BOOLEANPARAM_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      BOOLEANPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableBoolean",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableStringMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_STRINGPARAM_PRIMITIVE_DATA_TYPE: AppFunctionPrimitiveTypeMetadata
        = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = true,
            description = ""
        )

    private val STRINGPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "stringParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_STRINGPARAM_PRIMITIVE_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      STRINGPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableString",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionIntArrayMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_INTARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 7,
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_INTARRAYPARAM_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_INTARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val INTARRAYPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "intArrayParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_INTARRAYPARAM_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      INTARRAYPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionIntArray",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionLongArrayMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LONGARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_LONGARRAYPARAM_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_LONGARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val LONGARRAYPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "longArrayParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_LONGARRAYPARAM_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      LONGARRAYPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionLongArray",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionFloatArrayMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_FLOATARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 5,
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_FLOATARRAYPARAM_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_FLOATARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val FLOATARRAYPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "floatArrayParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_FLOATARRAYPARAM_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      FLOATARRAYPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionFloatArray",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionDoubleArrayMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_DOUBLEARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 4,
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_DOUBLEARRAYPARAM_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_DOUBLEARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val DOUBLEARRAYPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "doubleArrayParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_DOUBLEARRAYPARAM_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      DOUBLEARRAYPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionDoubleArray",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionBooleanArrayMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_BOOLEANARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 1,
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_BOOLEANARRAYPARAM_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_BOOLEANARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val BOOLEANARRAYPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "booleanArrayParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_BOOLEANARRAYPARAM_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      BOOLEANARRAYPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionBooleanArray",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionStringListMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_STRINGLISTPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_STRINGLISTPARAM_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_STRINGLISTPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val STRINGLISTPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "stringListParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_STRINGLISTPARAM_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      STRINGLISTPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionStringList",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableIntArrayMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_INTARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 7,
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_INTARRAYPARAM_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_INTARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true,
            description = ""
        )

    private val INTARRAYPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "intArrayParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_INTARRAYPARAM_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      INTARRAYPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableIntArray",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableLongArrayMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LONGARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_LONGARRAYPARAM_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_LONGARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true,
            description = ""
        )

    private val LONGARRAYPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "longArrayParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_LONGARRAYPARAM_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      LONGARRAYPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableLongArray",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableFloatArrayMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_FLOATARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 5,
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_FLOATARRAYPARAM_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_FLOATARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true,
            description = ""
        )

    private val FLOATARRAYPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "floatArrayParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_FLOATARRAYPARAM_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      FLOATARRAYPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableFloatArray",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableDoubleArrayMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_DOUBLEARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 4,
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_DOUBLEARRAYPARAM_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_DOUBLEARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true,
            description = ""
        )

    private val DOUBLEARRAYPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "doubleArrayParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_DOUBLEARRAYPARAM_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      DOUBLEARRAYPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableDoubleArray",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableBooleanArrayMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_BOOLEANARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 1,
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_BOOLEANARRAYPARAM_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_BOOLEANARRAYPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true,
            description = ""
        )

    private val BOOLEANARRAYPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "booleanArrayParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_BOOLEANARRAYPARAM_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      BOOLEANARRAYPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableBooleanArray",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }

  private object ComTestdataAllPrimitiveInputFunctionsSimpleFunctionNullableStringListMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_STRINGLISTPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_STRINGLISTPARAM_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_STRINGLISTPARAM_ARRAY_DATA_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true,
            description = ""
        )

    private val STRINGLISTPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "stringListParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_STRINGLISTPARAM_ARRAY_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      STRINGLISTPARAM_PARAMETER_METADATA,
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
            id = "com.testdata.AllPrimitiveInputFunctions#simpleFunctionNullableStringList",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }
}
