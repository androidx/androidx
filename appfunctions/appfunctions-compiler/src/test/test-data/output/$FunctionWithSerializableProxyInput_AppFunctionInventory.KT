package com.testdata

import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
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
 * FunctionWithSerializableProxyInput.....kt
 * SerializableWithProxyType.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$FunctionWithSerializableProxyInput_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.FunctionWithSerializableProxyInput#simpleFunctionWithLocalDateTime" to ComTestdataFunctionWithSerializableProxyInputSimpleFunctionWithLocalDateTimeMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.FunctionWithSerializableProxyInput#simpleFunctionWithSerializableWithProxy" to ComTestdataFunctionWithSerializableProxyInputSimpleFunctionWithSerializableWithProxyMetadataObject.APP_FUNCTION_METADATA,
  )

  private val JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_PROPERTIES_MAP_YEAR:
      AppFunctionIntTypeMetadata = AppFunctionIntTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_PROPERTIES_MAP_MONTH:
      AppFunctionIntTypeMetadata = AppFunctionIntTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_PROPERTIES_MAP_DAYOFMONTH:
      AppFunctionIntTypeMetadata = AppFunctionIntTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_PROPERTIES_MAP_HOUR:
      AppFunctionIntTypeMetadata = AppFunctionIntTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_PROPERTIES_MAP_MINUTE:
      AppFunctionIntTypeMetadata = AppFunctionIntTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_PROPERTIES_MAP_SECOND:
      AppFunctionIntTypeMetadata = AppFunctionIntTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_PROPERTIES_MAP_NANOOFSECOND:
      AppFunctionIntTypeMetadata = AppFunctionIntTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "year" to JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_PROPERTIES_MAP_YEAR,
    "month" to JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_PROPERTIES_MAP_MONTH,
    "dayOfMonth" to JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_PROPERTIES_MAP_DAYOFMONTH,
    "hour" to JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_PROPERTIES_MAP_HOUR,
    "minute" to JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_PROPERTIES_MAP_MINUTE,
    "second" to JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_PROPERTIES_MAP_SECOND,
    "nanoOfSecond" to JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_PROPERTIES_MAP_NANOOFSECOND,
  )

  private val JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST: List<String> =
      listOf(
    "year",
    "month",
    "dayOfMonth",
    "hour",
    "minute",
    "second",
    "nanoOfSecond",
  )

  private val JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
      AppFunctionObjectTypeMetadata(
          properties = JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "java.time.LocalDateTime",
          isNullable = true,
          description = ""
      )

  private val ANDROID_NET_URI_OBJECT_DATA_TYPE_PROPERTIES_MAP_URISTRING:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val ANDROID_NET_URI_OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "uriString" to ANDROID_NET_URI_OBJECT_DATA_TYPE_PROPERTIES_MAP_URISTRING,
  )

  private val ANDROID_NET_URI_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST: List<String> = listOf(
    "uriString",
  )

  private val ANDROID_NET_URI_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
      AppFunctionObjectTypeMetadata(
          properties = ANDROID_NET_URI_OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = ANDROID_NET_URI_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "android.net.Uri",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LOCALDATETIME:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "java.time.LocalDateTime",
          isNullable = false,
          description = ""
      )

  private val COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LOCALDATETIMELIST_REFERENCE_ITEM_TYPE:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "java.time.LocalDateTime",
          isNullable = false,
          description = ""
      )

  private val COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LOCALDATETIMELIST:
      AppFunctionArrayTypeMetadata = AppFunctionArrayTypeMetadata(
          itemType = COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LOCALDATETIMELIST_REFERENCE_ITEM_TYPE,
          isNullable = false,
          description = ""
      )

  private val COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LOCALDATETIMENULLABLE:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "java.time.LocalDateTime",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP_ANDROIDURILIST_REFERENCE_ITEM_TYPE:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "android.net.Uri",
          isNullable = false,
          description = ""
      )

  private val COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP_ANDROIDURILIST:
      AppFunctionArrayTypeMetadata = AppFunctionArrayTypeMetadata(
          itemType = COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP_ANDROIDURILIST_REFERENCE_ITEM_TYPE,
          isNullable = false,
          description = ""
      )

  private val COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP_ANDROIDURINULLABLE_REFERENCE_ITEM_TYPE:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "android.net.Uri",
          isNullable = false,
          description = ""
      )

  private val COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP_ANDROIDURINULLABLE:
      AppFunctionArrayTypeMetadata = AppFunctionArrayTypeMetadata(
          itemType = COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP_ANDROIDURINULLABLE_REFERENCE_ITEM_TYPE,
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "localDateTime" to COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LOCALDATETIME,
    "localDateTimeList" to COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LOCALDATETIMELIST,
    "localDateTimeNullable" to COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP_LOCALDATETIMENULLABLE,
    "androidUriList" to COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP_ANDROIDURILIST,
    "androidUriNullable" to COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP_ANDROIDURINULLABLE,
  )

  private val COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
      List<String> = listOf(
    "localDateTime",
    "localDateTimeList",
    "localDateTimeNullable",
    "androidUriList",
    "androidUriNullable",
  )

  private val COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata
      = AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "com.testdata.SerializableWithProxyType",
          isNullable = true,
          description = ""
      )

  private val componentsMetadataDataTypesMap: Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "java.time.LocalDateTime" to JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE,
    "android.net.Uri" to ANDROID_NET_URI_OBJECT_DATA_TYPE,
    "com.testdata.SerializableWithProxyType" to COM_TESTDATA_SERIALIZABLEWITHPROXYTYPE_OBJECT_DATA_TYPE,
    )

  override val componentsMetadata: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
          dataTypes = componentsMetadataDataTypesMap
      )

  private object ComTestdataFunctionWithSerializableProxyInputSimpleFunctionWithLocalDateTimeMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LOCALDATETIME_REFERENCE_DATA_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "java.time.LocalDateTime",
            isNullable = false,
            description = ""
        )

    private val LOCALDATETIME_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "localDateTime",
            isRequired = true,
            dataType = PARAMETER_METADATA_LOCALDATETIME_REFERENCE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      LOCALDATETIME_PARAMETER_METADATA,
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
            id = "com.testdata.FunctionWithSerializableProxyInput#simpleFunctionWithLocalDateTime",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataFunctionWithSerializableProxyInputSimpleFunctionWithSerializableWithProxyMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLEWITHPROXYTYPE_REFERENCE_DATA_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.SerializableWithProxyType",
            isNullable = false,
            description = ""
        )

    private val SERIALIZABLEWITHPROXYTYPE_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "serializableWithProxyType",
            isRequired = true,
            dataType = PARAMETER_METADATA_SERIALIZABLEWITHPROXYTYPE_REFERENCE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      SERIALIZABLEWITHPROXYTYPE_PARAMETER_METADATA,
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
            id = "com.testdata.FunctionWithSerializableProxyInput#simpleFunctionWithSerializableWithProxy",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }
}
