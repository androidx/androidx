package androidx.appfunctions.`internal`

import androidx.appfunctions.metadata.AppFunctionAllOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPendingIntentTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata
import javax.`annotation`.processing.Generated
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SchemaAppFunctionInventory_Impl` : SchemaAppFunctionInventory() {
  override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = mapOf(
    "note/showNote/2" to NoteShowNote2MetadataObject.APP_FUNCTION_METADATA,
    "note/updateNote/2" to NoteUpdateNote2MetadataObject.APP_FUNCTION_METADATA,
  )

  private val COM_TESTDATA_SHOWNOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_PROPERTIES_MAP_NOTEID:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val COM_TESTDATA_SHOWNOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "noteId" to COM_TESTDATA_SHOWNOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_PROPERTIES_MAP_NOTEID,
  )

  private val COM_TESTDATA_SHOWNOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
      List<String> = listOf(
    "noteId",
  )

  private val COM_TESTDATA_SHOWNOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE:
      AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_SHOWNOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = COM_TESTDATA_SHOWNOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "com.testdata.ShowNoteFunction${'$'}Parameters",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_SHOWNOTEFUNCTION_RESPONSE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_PENDINGINTENT:
      AppFunctionPendingIntentTypeMetadata = AppFunctionPendingIntentTypeMetadata(
          isNullable = false,
          description = ""
      )

  private val COM_TESTDATA_SHOWNOTEFUNCTION_RESPONSE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "pendingIntent" to COM_TESTDATA_SHOWNOTEFUNCTION_RESPONSE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP_PENDINGINTENT,
  )

  private val COM_TESTDATA_SHOWNOTEFUNCTION_RESPONSE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_REQUIRED_PROPERTIES_LIST:
      List<String> = listOf(
    "pendingIntent",
  )

  private val COM_TESTDATA_SHOWNOTEFUNCTION_RESPONSE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0:
      AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_SHOWNOTEFUNCTION_RESPONSE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_PROPERTIES_MAP,
          required = COM_TESTDATA_SHOWNOTEFUNCTION_RESPONSE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "com.testdata.AppFunctionOpenable",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_SHOWNOTEFUNCTION_RESPONSE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST:
      List<AppFunctionDataTypeMetadata> = listOf(
    COM_TESTDATA_SHOWNOTEFUNCTION_RESPONSE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST_ITEM_0,
  )

  private val COM_TESTDATA_SHOWNOTEFUNCTION_RESPONSE_ALL_OF_DATA_TYPE: AppFunctionAllOfTypeMetadata
      = AppFunctionAllOfTypeMetadata(
          matchAll = COM_TESTDATA_SHOWNOTEFUNCTION_RESPONSE_ALL_OF_DATA_TYPE_MATCH_ALL_LIST,
          qualifiedName = "com.testdata.ShowNoteFunction${'$'}Response",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_SETFIELD_KOTLIN_STRING__OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val COM_TESTDATA_SETFIELD_KOTLIN_STRING__OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "value" to COM_TESTDATA_SETFIELD_KOTLIN_STRING__OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE,
  )

  private val COM_TESTDATA_SETFIELD_KOTLIN_STRING__OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
      List<String> = listOf(
    "value",
  )

  private val COM_TESTDATA_SETFIELD_KOTLIN_STRING__OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
      AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_SETFIELD_KOTLIN_STRING__OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = COM_TESTDATA_SETFIELD_KOTLIN_STRING__OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "com.testdata.SetField<kotlin.String>",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_SETFIELD_KOTLIN_STRING_NULLABLE__OBJECT_DATA_TYPE_PROPERTIES_MAP_VALUE:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = true,
          description = "",
          enumValues = null,
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
          description = ""
      )

  private val COM_TESTDATA_ATTACHMENT_OBJECT_DATA_TYPE_PROPERTIES_MAP_URI:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
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
          description = ""
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
          description = ""
      )

  private val COM_TESTDATA_UPDATENOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_PROPERTIES_MAP_NOTEID:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val COM_TESTDATA_UPDATENOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_PROPERTIES_MAP_TITLE:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "com.testdata.SetField<kotlin.String>",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_UPDATENOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_PROPERTIES_MAP_CONTENT:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "com.testdata.SetField<kotlin.String?>",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_UPDATENOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_PROPERTIES_MAP_ATTACHMENTS:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "com.testdata.SetField<kotlin.collections.List<com.testdata.Attachment>>",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_UPDATENOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "noteId" to COM_TESTDATA_UPDATENOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_PROPERTIES_MAP_NOTEID,
    "title" to COM_TESTDATA_UPDATENOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_PROPERTIES_MAP_TITLE,
    "content" to COM_TESTDATA_UPDATENOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_PROPERTIES_MAP_CONTENT,
    "attachments" to COM_TESTDATA_UPDATENOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_PROPERTIES_MAP_ATTACHMENTS,
  )

  private val COM_TESTDATA_UPDATENOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
      List<String> = listOf(
    "noteId",
    "title",
    "content",
    "attachments",
  )

  private val COM_TESTDATA_UPDATENOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE:
      AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_UPDATENOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = COM_TESTDATA_UPDATENOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "com.testdata.UpdateNoteFunction${'$'}Parameters",
          isNullable = true,
          description = ""
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

  private val COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_PROPERTIES_MAP_ID: AppFunctionStringTypeMetadata =
      AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_PROPERTIES_MAP_TITLE: AppFunctionStringTypeMetadata
      = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_PROPERTIES_MAP_CONTENT_PRIMITIVE_ITEM_TYPE:
      AppFunctionStringTypeMetadata = AppFunctionStringTypeMetadata(
          isNullable = false,
          description = "",
          enumValues = null,
      )

  private val COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_PROPERTIES_MAP_CONTENT:
      AppFunctionArrayTypeMetadata = AppFunctionArrayTypeMetadata(
          itemType = COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_PROPERTIES_MAP_CONTENT_PRIMITIVE_ITEM_TYPE,
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_PROPERTIES_MAP_ATTACHMENTS_REFERENCE_ITEM_TYPE:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "com.testdata.Attachment",
          isNullable = false,
          description = ""
      )

  private val COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_PROPERTIES_MAP_ATTACHMENTS:
      AppFunctionArrayTypeMetadata = AppFunctionArrayTypeMetadata(
          itemType = COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_PROPERTIES_MAP_ATTACHMENTS_REFERENCE_ITEM_TYPE,
          isNullable = false,
          description = ""
      )

  private val COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_PROPERTIES_MAP_CREATETIME:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "java.time.LocalDateTime",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "id" to COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_PROPERTIES_MAP_ID,
    "title" to COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_PROPERTIES_MAP_TITLE,
    "content" to COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_PROPERTIES_MAP_CONTENT,
    "attachments" to COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_PROPERTIES_MAP_ATTACHMENTS,
    "createTime" to COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_PROPERTIES_MAP_CREATETIME,
  )

  private val COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST: List<String> = listOf(
    "id",
    "title",
    "content",
    "attachments",
    "createTime",
  )

  private val COM_TESTDATA_NOTE_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
      AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = COM_TESTDATA_NOTE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "com.testdata.Note",
          isNullable = true,
          description = ""
      )

  private val COM_TESTDATA_UPDATENOTEFUNCTION_RESPONSE_OBJECT_DATA_TYPE_PROPERTIES_MAP_NOTE:
      AppFunctionReferenceTypeMetadata = AppFunctionReferenceTypeMetadata(
          referenceDataType = "com.testdata.Note",
          isNullable = false,
          description = ""
      )

  private val COM_TESTDATA_UPDATENOTEFUNCTION_RESPONSE_OBJECT_DATA_TYPE_PROPERTIES_MAP:
      Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "note" to COM_TESTDATA_UPDATENOTEFUNCTION_RESPONSE_OBJECT_DATA_TYPE_PROPERTIES_MAP_NOTE,
  )

  private val COM_TESTDATA_UPDATENOTEFUNCTION_RESPONSE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST:
      List<String> = listOf(
    "note",
  )

  private val COM_TESTDATA_UPDATENOTEFUNCTION_RESPONSE_OBJECT_DATA_TYPE:
      AppFunctionObjectTypeMetadata = AppFunctionObjectTypeMetadata(
          properties = COM_TESTDATA_UPDATENOTEFUNCTION_RESPONSE_OBJECT_DATA_TYPE_PROPERTIES_MAP,
          required = COM_TESTDATA_UPDATENOTEFUNCTION_RESPONSE_OBJECT_DATA_TYPE_REQUIRED_PROPERTIES_LIST,
          qualifiedName = "com.testdata.UpdateNoteFunction${'$'}Response",
          isNullable = true,
          description = ""
      )

  private val componentsMetadataDataTypesMap: Map<String, AppFunctionDataTypeMetadata> = mapOf(
    "com.testdata.ShowNoteFunction${'$'}Parameters" to COM_TESTDATA_SHOWNOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE,
    "com.testdata.ShowNoteFunction${'$'}Response" to COM_TESTDATA_SHOWNOTEFUNCTION_RESPONSE_ALL_OF_DATA_TYPE,
    "com.testdata.SetField<kotlin.String>" to COM_TESTDATA_SETFIELD_KOTLIN_STRING__OBJECT_DATA_TYPE,
    "com.testdata.SetField<kotlin.String?>" to COM_TESTDATA_SETFIELD_KOTLIN_STRING_NULLABLE__OBJECT_DATA_TYPE,
    "com.testdata.Attachment" to COM_TESTDATA_ATTACHMENT_OBJECT_DATA_TYPE,
    "com.testdata.SetField<kotlin.collections.List<com.testdata.Attachment>>" to COM_TESTDATA_SETFIELD_KOTLIN_COLLECTIONS_LIST_COM_TESTDATA_ATTACHMENT___OBJECT_DATA_TYPE,
    "com.testdata.UpdateNoteFunction${'$'}Parameters" to COM_TESTDATA_UPDATENOTEFUNCTION_PARAMETERS_OBJECT_DATA_TYPE,
    "java.time.LocalDateTime" to JAVA_TIME_LOCALDATETIME_OBJECT_DATA_TYPE,
    "com.testdata.Note" to COM_TESTDATA_NOTE_OBJECT_DATA_TYPE,
    "com.testdata.UpdateNoteFunction${'$'}Response" to COM_TESTDATA_UPDATENOTEFUNCTION_RESPONSE_OBJECT_DATA_TYPE,
    )

  override val componentsMetadata: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(
          dataTypes = componentsMetadataDataTypesMap
      )

  private object NoteShowNote2MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "note", name="showNote", version=2)

    private val PARAMETER_METADATA_PARAMETERS_REFERENCE_DATA_TYPE: AppFunctionReferenceTypeMetadata
        = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.ShowNoteFunction${'$'}Parameters",
            isNullable = false,
            description = ""
        )

    private val PARAMETERS_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "parameters",
            isRequired = true,
            dataType = PARAMETER_METADATA_PARAMETERS_REFERENCE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      PARAMETERS_PARAMETER_METADATA,
    )

    private val REFERENCE_RESPONSE_VALUE_TYPE: AppFunctionReferenceTypeMetadata =
        AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.ShowNoteFunction${'$'}Response",
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = REFERENCE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "note/showNote/2",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }

  private object NoteUpdateNote2MetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? =
        AppFunctionSchemaMetadata(category= "note", name="updateNote", version=2)

    private val PARAMETER_METADATA_PARAMETERS_REFERENCE_DATA_TYPE: AppFunctionReferenceTypeMetadata
        = AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.UpdateNoteFunction${'$'}Parameters",
            isNullable = false,
            description = ""
        )

    private val PARAMETERS_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "parameters",
            isRequired = true,
            dataType = PARAMETER_METADATA_PARAMETERS_REFERENCE_DATA_TYPE,
            description = "",
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      PARAMETERS_PARAMETER_METADATA,
    )

    private val REFERENCE_RESPONSE_VALUE_TYPE: AppFunctionReferenceTypeMetadata =
        AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.UpdateNoteFunction${'$'}Response",
            isNullable = false,
            description = ""
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = REFERENCE_RESPONSE_VALUE_TYPE,
            description = ""
        )

    public val APP_FUNCTION_METADATA: CompileTimeAppFunctionMetadata =
        CompileTimeAppFunctionMetadata(
            id = "note/updateNote/2",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA,
        )
  }
}
