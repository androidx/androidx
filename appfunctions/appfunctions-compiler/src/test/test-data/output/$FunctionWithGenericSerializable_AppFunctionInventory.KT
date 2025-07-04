package com.testdata

import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
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
 * FunctionWithGenericSerializable.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$FunctionWithGenericSerializable_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "com.testdata.FunctionWithGenericSerializable#test" to ComTestdataFunctionWithGenericSerializableTestMetadataObject.APP_FUNCTION_METADATA,
  )

  private object ComTestdataFunctionWithGenericSerializableTestMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_PARAMS_REFERENCE_DATA_TYPE: AppFunctionReferenceTypeMetadata =
        AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.UpdateNoteParams",
            isNullable = false,
            description = ""
        )

    private val PARAMS_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "params",
            isRequired = true,
            dataType = PARAMETER_METADATA_PARAMS_REFERENCE_DATA_TYPE
        )

    private val PARAMETER_METADATA_TOPLEVELGENERICPARAM_REFERENCE_DATA_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.SetField<kotlin.Int>",
            isNullable = true,
            description = ""
        )

    private val TOPLEVELGENERICPARAM_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "topLevelGenericParam",
            isRequired = true,
            dataType = PARAMETER_METADATA_TOPLEVELGENERICPARAM_REFERENCE_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      PARAMS_PARAMETER_METADATA,
      TOPLEVELGENERICPARAM_PARAMETER_METADATA,
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

    private val COM_TESTDATA_SETFIELD_KOTLIN_STRING__OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_SETFIELD_KOTLIN_STRING__OBJECT_DATA_TYPE_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "value" to COM_TESTDATA_SETFIELD_KOTLIN_STRING__OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE,
    )

    private val COM_TESTDATA_SETFIELD_KOTLIN_STRING__OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "value",
    )

    private val COM_TESTDATA_SETFIELD_KOTLIN_STRING__OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata
        = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_SETFIELD_KOTLIN_STRING__OBJECT_DATA_TYPE_PROPERTIES_MAP,
            required = COM_TESTDATA_SETFIELD_KOTLIN_STRING__OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.SetField<kotlin.String>",
            isNullable = true,
            description = "An example generic AppFunctionSerializable. "
        )

    private val COM_TESTDATA_SETFIELD_KOTLIN_STRING_NULLABLE__OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_SETFIELD_KOTLIN_STRING_NULLABLE__OBJECT_DATA_TYPE_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "value" to COM_TESTDATA_SETFIELD_KOTLIN_STRING_NULLABLE__OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE,
    )

    private val COM_TESTDATA_SETFIELD_KOTLIN_STRING_NULLABLE__OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "value",
    )

    private val COM_TESTDATA_SETFIELD_KOTLIN_STRING_NULLABLE__OBJECT_DATA_TYPE:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_SETFIELD_KOTLIN_STRING_NULLABLE__OBJECT_DATA_TYPE_PROPERTIES_MAP,
            required = COM_TESTDATA_SETFIELD_KOTLIN_STRING_NULLABLE__OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.SetField<kotlin.String?>",
            isNullable = true,
            description = "An example generic AppFunctionSerializable. "
        )

    private val COM_TESTDATA_ATTACHMENT_OBJECT_DATA_TYPE_PROPERTIES_MAP_URI:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_ATTACHMENT_OBJECT_DATA_TYPE_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "uri" to COM_TESTDATA_ATTACHMENT_OBJECT_DATA_TYPE_PROPERTIES_MAP_URI,
    )

    private val COM_TESTDATA_ATTACHMENT_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST: List<String> =
        listOf(
      "uri",
    )

    private val COM_TESTDATA_ATTACHMENT_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
        AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_ATTACHMENT_OBJECT_DATA_TYPE_PROPERTIES_MAP,
            required = COM_TESTDATA_ATTACHMENT_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.Attachment",
            isNullable = true,
            description = "An attachment for a note entity. "
        )

    private val COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT___OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE_REFERENCE_ITEM_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.Attachment",
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT___OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE:
        AppFunctionArrayTypeMetadata = AppFunctionArrayTypeMetadata(
            itemType = COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT___OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE_REFERENCE_ITEM_TYPE,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT___OBJECT_DATA_TYPE_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "value" to COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT___OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE,
    )

    private val COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT___OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "value",
    )

    private val COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT___OBJECT_DATA_TYPE:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT___OBJECT_DATA_TYPE_PROPERTIES_MAP,
            required = COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT___OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.SetField<kotlin.collections.List<com.testdata.Attachment>>",
            isNullable = true,
            description = "An example generic AppFunctionSerializable. "
        )

    private val COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT__NULLABLE__OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE_REFERENCE_ITEM_TYPE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.Attachment",
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT__NULLABLE__OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE:
        AppFunctionArrayTypeMetadata = AppFunctionArrayTypeMetadata(
            itemType = COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT__NULLABLE__OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE_REFERENCE_ITEM_TYPE,
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT__NULLABLE__OBJECT_DATA_TYPE_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "value" to COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT__NULLABLE__OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE,
    )

    private val COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT__NULLABLE__OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "value",
    )

    private val COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT__NULLABLE__OBJECT_DATA_TYPE:
        AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT__NULLABLE__OBJECT_DATA_TYPE_PROPERTIES_MAP,
            required = COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT__NULLABLE__OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.SetField<kotlin.collections.List<com.testdata.Attachment>?>",
            isNullable = true,
            description = "An example generic AppFunctionSerializable. "
        )

    private val COM_TESTDATA_UPDATENOTEPARAMS_OBJECT_DATA_TYPE_PROPERTIES_MAP_TITLE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.SetField<kotlin.String>",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_UPDATENOTEPARAMS_OBJECT_DATA_TYPE_PROPERTIES_MAP_NULLABLETITLE:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.SetField<kotlin.String?>",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_UPDATENOTEPARAMS_OBJECT_DATA_TYPE_PROPERTIES_MAP_ATTACHMENTS:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.SetField<kotlin.collections.List<com.testdata.Attachment>>",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_UPDATENOTEPARAMS_OBJECT_DATA_TYPE_PROPERTIES_MAP_NULLABLEATTACHMENTS:
        AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.SetField<kotlin.collections.List<com.testdata.Attachment>?>",
            isNullable = true,
            description = ""
        )

    private val COM_TESTDATA_UPDATENOTEPARAMS_OBJECT_DATA_TYPE_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "title" to COM_TESTDATA_UPDATENOTEPARAMS_OBJECT_DATA_TYPE_PROPERTIES_MAP_TITLE,
      "nullableTitle" to COM_TESTDATA_UPDATENOTEPARAMS_OBJECT_DATA_TYPE_PROPERTIES_MAP_NULLABLETITLE,
      "attachments" to COM_TESTDATA_UPDATENOTEPARAMS_OBJECT_DATA_TYPE_PROPERTIES_MAP_ATTACHMENTS,
      "nullableAttachments" to COM_TESTDATA_UPDATENOTEPARAMS_OBJECT_DATA_TYPE_PROPERTIES_MAP_NULLABLEATTACHMENTS,
    )

    private val COM_TESTDATA_UPDATENOTEPARAMS_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
    )

    private val COM_TESTDATA_UPDATENOTEPARAMS_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
        AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_UPDATENOTEPARAMS_OBJECT_DATA_TYPE_PROPERTIES_MAP,
            required = COM_TESTDATA_UPDATENOTEPARAMS_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.UpdateNoteParams",
            isNullable = true,
            description = "Parameters to update a note entity. "
        )

    private val COM_TESTDATA_SETFIELD_KOTLIN_INT__OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE:
        AppFunctionPrimitiveTypeMetadata = AppFunctionPrimitiveTypeMetadata(
            type = 7,
            isNullable = false,
            description = ""
        )

    private val COM_TESTDATA_SETFIELD_KOTLIN_INT__OBJECT_DATA_TYPE_PROPERTIES_MAP:
        Map<String, AppFunctionDataTypeMetadata> = mapOf(
      "value" to COM_TESTDATA_SETFIELD_KOTLIN_INT__OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE,
    )

    private val COM_TESTDATA_SETFIELD_KOTLIN_INT__OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
        List<String> = listOf(
      "value",
    )

    private val COM_TESTDATA_SETFIELD_KOTLIN_INT__OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
        AppFunctionObjectTypeMetadata(
            properties = COM_TESTDATA_SETFIELD_KOTLIN_INT__OBJECT_DATA_TYPE_PROPERTIES_MAP,
            required = COM_TESTDATA_SETFIELD_KOTLIN_INT__OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
            qualifiedName = "com.testdata.SetField<kotlin.Int>",
            isNullable = true,
            description = "An example generic AppFunctionSerializable. "
        )

    private val COMPONENTS_METADATA_DATA_TYPES_MAP: Map<String, AppFunctionDataTypeMetadata> =
        mapOf(
      "com.testdata.SetField<kotlin.String>" to COM_TESTDATA_SETFIELD_KOTLIN_STRING__OBJECT_DATA_TYPE,
      "com.testdata.SetField<kotlin.String?>" to COM_TESTDATA_SETFIELD_KOTLIN_STRING_NULLABLE__OBJECT_DATA_TYPE,
      "com.testdata.Attachment" to COM_TESTDATA_ATTACHMENT_OBJECT_DATA_TYPE,
      "com.testdata.SetField<kotlin.collections.List<com.testdata.Attachment>>" to COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT___OBJECT_DATA_TYPE,
      "com.testdata.SetField<kotlin.collections.List<com.testdata.Attachment>?>" to COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT__NULLABLE__OBJECT_DATA_TYPE,
      "com.testdata.UpdateNoteParams" to COM_TESTDATA_UPDATENOTEPARAMS_OBJECT_DATA_TYPE,
      "com.testdata.SetField<kotlin.Int>" to COM_TESTDATA_SETFIELD_KOTLIN_INT__OBJECT_DATA_TYPE,
      )

    private val COMPONENTS_METADATA: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
            dataTypes = COMPONENTS_METADATA_DATA_TYPES_MAP
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "com.testdata.FunctionWithGenericSerializable#test",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
            components = COMPONENTS_METADATA
        )
  }
}
