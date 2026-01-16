package com.testdata

import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionParcelableTypeMetadata
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
 * ParcelablePrimitiveFunctions.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$ParcelablePrimitiveFunctions_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.ParcelablePrimitiveFunctions#listPendingIntentInput" to ComTestdataParcelablePrimitiveFunctionsListPendingIntentInputMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.ParcelablePrimitiveFunctions#listPendingIntentOutput" to ComTestdataParcelablePrimitiveFunctionsListPendingIntentOutputMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.ParcelablePrimitiveFunctions#pendingIntentInput" to ComTestdataParcelablePrimitiveFunctionsPendingIntentInputMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.ParcelablePrimitiveFunctions#pendingIntentOutput" to ComTestdataParcelablePrimitiveFunctionsPendingIntentOutputMetadataObject.APP_FUNCTION_METADATA,
  )

  private val componentsMetadataDataTypesMap: Map<String, AppFunctionDataTypeMetadata> = mapOf(
    )

  override val componentsMetadata: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
          dataTypes = componentsMetadataDataTypesMap
      )

  private object ComTestdataParcelablePrimitiveFunctionsListPendingIntentInputMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LISTPENDINGINTENT_ARRAY_DATA_TYPE_PARCELABLE_ITEM_TYPE:
        AppFunctionParcelableTypeMetadata = AppFunctionParcelableTypeMetadata(
            qualifiedName = "android.app.PendingIntent",
            isNullable = false,
            description = ""
        )

    private val PARAMETER_METADATA_LISTPENDINGINTENT_ARRAY_DATA_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = PARAMETER_METADATA_LISTPENDINGINTENT_ARRAY_DATA_TYPE_PARCELABLE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val LISTPENDINGINTENT_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "listPendingIntent",
            isRequired = true,
            dataType = PARAMETER_METADATA_LISTPENDINGINTENT_ARRAY_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      LISTPENDINGINTENT_PARAMETER_METADATA,
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
            id = "com.testdata.ParcelablePrimitiveFunctions#listPendingIntentInput",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataParcelablePrimitiveFunctionsListPendingIntentOutputMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PARCELABLE_ITEM_TYPE: AppFunctionParcelableTypeMetadata =
        AppFunctionParcelableTypeMetadata(
            qualifiedName = "android.app.PendingIntent",
            isNullable = false,
            description = ""
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PARCELABLE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.ParcelablePrimitiveFunctions#listPendingIntentOutput",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataParcelablePrimitiveFunctionsPendingIntentInputMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_PENDINGINTENT_PARCELABLE_DATA_TYPE:
        AppFunctionParcelableTypeMetadata = AppFunctionParcelableTypeMetadata(
            qualifiedName = "android.app.PendingIntent",
            isNullable = false,
            description = ""
        )

    private val PENDINGINTENT_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "pendingIntent",
            isRequired = true,
            dataType = PARAMETER_METADATA_PENDINGINTENT_PARCELABLE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      PENDINGINTENT_PARAMETER_METADATA,
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
            id = "com.testdata.ParcelablePrimitiveFunctions#pendingIntentInput",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object ComTestdataParcelablePrimitiveFunctionsPendingIntentOutputMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PARCELABLE_RESPONSE_VALUE_TYPE: AppFunctionParcelableTypeMetadata =
        AppFunctionParcelableTypeMetadata(
            qualifiedName = "android.app.PendingIntent",
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PARCELABLE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.ParcelablePrimitiveFunctions#pendingIntentOutput",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }
}
