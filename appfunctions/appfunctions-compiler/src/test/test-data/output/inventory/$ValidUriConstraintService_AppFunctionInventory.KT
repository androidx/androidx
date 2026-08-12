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
 * ValidUriConstraintService.kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$ValidUriConstraintService_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.ValidUriConstraintService#functionWithContentUri" to ComTestdataValidUriConstraintServiceFunctionWithContentUriMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.ValidUriConstraintService#functionWithMultiSchemeUri" to ComTestdataValidUriConstraintServiceFunctionWithMultiSchemeUriMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.ValidUriConstraintService#functionWithUnconstrainedUri" to ComTestdataValidUriConstraintServiceFunctionWithUnconstrainedUriMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.ValidUriConstraintService#functionWithStringConstraintNoEnumValues" to ComTestdataValidUriConstraintServiceFunctionWithStringConstraintNoEnumValuesMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.ValidUriConstraintService#functionWithSerializableValueConstraints" to ComTestdataValidUriConstraintServiceFunctionWithSerializableValueConstraintsMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.ValidUriConstraintService#functionWithUriReturn" to ComTestdataValidUriConstraintServiceFunctionWithUriReturnMetadataObject.APP_FUNCTION_METADATA,
  )

  private val COM_TESTDATA_SERIALIZABLEWITHVALUECONSTRAINTS_OBJECT_DATA_TYPE_PROPERTIES_MAP_CONTENTURI:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
          pattern = "^\\Qcontent\\E:.*",
          format = "uri",
      )

  private val COM_TESTDATA_SERIALIZABLEWITHVALUECONSTRAINTS_OBJECT_DATA_TYPE_PROPERTIES_MAP_NUMERICSTRING:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
          pattern = "^[0-9]+${'$'}",
          format = "numeric",
      )

  private val COM_TESTDATA_SERIALIZABLEWITHVALUECONSTRAINTS_OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "contentUri" to COM_TESTDATA_SERIALIZABLEWITHVALUECONSTRAINTS_OBJECT_DATA_TYPE_PROPERTIES_MAP_CONTENTURI,
    "numericString" to COM_TESTDATA_SERIALIZABLEWITHVALUECONSTRAINTS_OBJECT_DATA_TYPE_PROPERTIES_MAP_NUMERICSTRING,
  )

  private val COM_TESTDATA_SERIALIZABLEWITHVALUECONSTRAINTS_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
      List<String> = listOf(
    "contentUri",
    "numericString",
  )

  private val COM_TESTDATA_SERIALIZABLEWITHVALUECONSTRAINTS_OBJECT_DATA_TYPE:
      AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_SERIALIZABLEWITHVALUECONSTRAINTS_OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = COM_TESTDATA_SERIALIZABLEWITHVALUECONSTRAINTS_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "com.testdata.SerializableWithValueConstraints",
          isNullable = true,
          description = ""
      )

  private val componentsMetadataDataTypesMap: Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "com.testdata.SerializableWithValueConstraints" to COM_TESTDATA_SERIALIZABLEWITHVALUECONSTRAINTS_OBJECT_DATA_TYPE,
    )

  override val componentsMetadata: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
          dataTypes = componentsMetadataDataTypesMap
      )

  private object ComTestdataValidUriConstraintServiceFunctionWithContentUriMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_FILEURI_PRIMITIVE_DATA_TYPE: AppFunctionStringTypeMetadata =
        AppFunctionStringTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = null,
            pattern = "^\\Qcontent\\E:.*",
            format = "uri",
        )

    private val FILEURI_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "fileUri",
            isRequired = true,
            dataType = PARAMETER_METADATA_FILEURI_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      FILEURI_PARAMETER_METADATA,
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
            id = "com.testdata.ValidUriConstraintService#functionWithContentUri",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataValidUriConstraintServiceFunctionWithMultiSchemeUriMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_MULTIURI_PRIMITIVE_DATA_TYPE: AppFunctionStringTypeMetadata =
        AppFunctionStringTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = null,
            pattern = "^(\\Qcontent\\E|\\Qfile\\E):.*",
            format = "uri",
        )

    private val MULTIURI_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "multiUri",
            isRequired = true,
            dataType = PARAMETER_METADATA_MULTIURI_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      MULTIURI_PARAMETER_METADATA,
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
            id = "com.testdata.ValidUriConstraintService#functionWithMultiSchemeUri",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataValidUriConstraintServiceFunctionWithUnconstrainedUriMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_PLAINURI_PRIMITIVE_DATA_TYPE: AppFunctionStringTypeMetadata =
        AppFunctionStringTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = null,
            pattern = null,
            format = "uri",
        )

    private val PLAINURI_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "plainUri",
            isRequired = true,
            dataType = PARAMETER_METADATA_PLAINURI_PRIMITIVE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      PLAINURI_PARAMETER_METADATA,
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
            id = "com.testdata.ValidUriConstraintService#functionWithUnconstrainedUri",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataValidUriConstraintServiceFunctionWithStringConstraintNoEnumValuesMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_STRINGPARAM_PRIMITIVE_DATA_TYPE: AppFunctionStringTypeMetadata =
        AppFunctionStringTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = null,
            pattern = "^[a-z]+${'$'}",
            format = "custom",
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
            id = "com.testdata.ValidUriConstraintService#functionWithStringConstraintNoEnumValues",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataValidUriConstraintServiceFunctionWithSerializableValueConstraintsMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_PARAM_REFERENCE_DATA_TYPE: AppFunctionReferenceTypeMetadata =
        AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.SerializableWithValueConstraints",
            isNullable = false,
            description = ""
        )

    private val PARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "param",
            isRequired = true,
            dataType = PARAMETER_METADATA_PARAM_REFERENCE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      PARAM_PARAMETER_METADATA,
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
            id = "com.testdata.ValidUriConstraintService#functionWithSerializableValueConstraints",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataValidUriConstraintServiceFunctionWithUriReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionStringTypeMetadata =
        AppFunctionStringTypeMetadata(
            isNullable = false,
            description = "",
            enumValues = null,
            pattern = "^\\Qcontent\\E:.*",
            format = "uri",
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.ValidUriConstraintService#functionWithUriReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }
}
