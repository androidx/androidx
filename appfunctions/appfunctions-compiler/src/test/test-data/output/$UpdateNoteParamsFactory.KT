package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated
import kotlin.String
import kotlin.collections.List

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$UpdateNoteParamsFactory` : AppFunctionSerializableFactory<UpdateNoteParams> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): UpdateNoteParams {
    val setFieldKotlinStringFactory = `$SetFieldFactory`<String>(
      AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(String::class.java))
    val setFieldKotlinStringNullableFactory = `$SetFieldFactory`<String?>(
      @Suppress("UNCHECKED_CAST") AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(String::class.java as Class<String?>))
    val setFieldKotlinCollectionsListComTestdataAttachmentFactory = `$SetFieldFactory`<List<Attachment>>(
      AppFunctionSerializableFactory.TypeParameter.SerializableListTypeParameter(Attachment::class.java, `$AttachmentFactory`()))
    val setFieldKotlinCollectionsListComTestdataAttachmentNullableFactory = `$SetFieldFactory`<List<Attachment>?>(
      AppFunctionSerializableFactory.TypeParameter.SerializableListTypeParameter(Attachment::class.java, `$AttachmentFactory`()))

    val titleData = appFunctionData.getAppFunctionData("title")
    var title: SetField<String>? = null
    if (titleData != null) {
      title = setFieldKotlinStringFactory.fromAppFunctionData(titleData)
    }
    val nullableTitleData = appFunctionData.getAppFunctionData("nullableTitle")
    var nullableTitle: SetField<String?>? = null
    if (nullableTitleData != null) {
      nullableTitle = setFieldKotlinStringNullableFactory.fromAppFunctionData(nullableTitleData)
    }
    val attachmentsData = appFunctionData.getAppFunctionData("attachments")
    var attachments: SetField<List<Attachment>>? = null
    if (attachmentsData != null) {
      attachments = setFieldKotlinCollectionsListComTestdataAttachmentFactory.fromAppFunctionData(attachmentsData)
    }
    val nullableAttachmentsData = appFunctionData.getAppFunctionData("nullableAttachments")
    var nullableAttachments: SetField<List<Attachment>?>? = null
    if (nullableAttachmentsData != null) {
      nullableAttachments = setFieldKotlinCollectionsListComTestdataAttachmentNullableFactory.fromAppFunctionData(nullableAttachmentsData)
    }

    val resultUpdateNoteParams = UpdateNoteParams(title, nullableTitle, attachments, nullableAttachments)
    return resultUpdateNoteParams
  }

  override fun toAppFunctionData(appFunctionSerializable: UpdateNoteParams): AppFunctionData {
    val updateNoteParams_appFunctionSerializable = appFunctionSerializable
    val setFieldKotlinStringFactory = `$SetFieldFactory`<String>(
      AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(String::class.java))
    val setFieldKotlinStringNullableFactory = `$SetFieldFactory`<String?>(
      @Suppress("UNCHECKED_CAST") AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(String::class.java as Class<String?>))
    val setFieldKotlinCollectionsListComTestdataAttachmentFactory = `$SetFieldFactory`<List<Attachment>>(
      AppFunctionSerializableFactory.TypeParameter.SerializableListTypeParameter(Attachment::class.java, `$AttachmentFactory`()))
    val setFieldKotlinCollectionsListComTestdataAttachmentNullableFactory = `$SetFieldFactory`<List<Attachment>?>(
      AppFunctionSerializableFactory.TypeParameter.SerializableListTypeParameter(Attachment::class.java, `$AttachmentFactory`()))

    val builder = AppFunctionData.Builder("com.testdata.UpdateNoteParams")
    val title = updateNoteParams_appFunctionSerializable.title
    if (title != null) {
      builder.setAppFunctionData("title", setFieldKotlinStringFactory.toAppFunctionData(title))
    }
    val nullableTitle = updateNoteParams_appFunctionSerializable.nullableTitle
    if (nullableTitle != null) {
      builder.setAppFunctionData("nullableTitle", setFieldKotlinStringNullableFactory.toAppFunctionData(nullableTitle))
    }
    val attachments = updateNoteParams_appFunctionSerializable.attachments
    if (attachments != null) {
      builder.setAppFunctionData("attachments", setFieldKotlinCollectionsListComTestdataAttachmentFactory.toAppFunctionData(attachments))
    }
    val nullableAttachments = updateNoteParams_appFunctionSerializable.nullableAttachments
    if (nullableAttachments != null) {
      builder.setAppFunctionData("nullableAttachments", setFieldKotlinCollectionsListComTestdataAttachmentNullableFactory.toAppFunctionData(nullableAttachments))
    }

    return builder.build()
  }
}
