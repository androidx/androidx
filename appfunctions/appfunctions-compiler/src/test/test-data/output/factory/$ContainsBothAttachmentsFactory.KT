package com.testdata

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$ContainsBothAttachmentsFactory` : AppFunctionSerializableFactory<ContainsBothAttachments> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): ContainsBothAttachments {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.ContainsBothAttachments")
    val simpleNoteSimpleAttachmentFactory = `$SimpleNote$SimpleAttachmentFactory`()
    val simpleMessageSimpleAttachmentFactory = `$SimpleMessage$SimpleAttachmentFactory`()

    val noteAttachmentData = checkNotNull(appFunctionDataWithSpec.getAppFunctionData("noteAttachment"))
    val noteAttachment = simpleNoteSimpleAttachmentFactory.fromAppFunctionData(noteAttachmentData)
    val messageAttachmentData = checkNotNull(appFunctionDataWithSpec.getAppFunctionData("messageAttachment"))
    val messageAttachment = simpleMessageSimpleAttachmentFactory.fromAppFunctionData(messageAttachmentData)

    val resultContainsBothAttachments = ContainsBothAttachments(noteAttachment, messageAttachment)
    return resultContainsBothAttachments
  }

  override fun toAppFunctionData(appFunctionSerializable: ContainsBothAttachments): AppFunctionData {
    val containsBothAttachments_appFunctionSerializable = appFunctionSerializable
    val simpleNoteSimpleAttachmentFactory = `$SimpleNote$SimpleAttachmentFactory`()
    val simpleMessageSimpleAttachmentFactory = `$SimpleMessage$SimpleAttachmentFactory`()

    val builder = getAppFunctionDataBuilder("com.testdata.ContainsBothAttachments")
    val noteAttachment = containsBothAttachments_appFunctionSerializable.noteAttachment
    builder.setAppFunctionData("noteAttachment", simpleNoteSimpleAttachmentFactory.toAppFunctionData(noteAttachment))
    val messageAttachment = containsBothAttachments_appFunctionSerializable.messageAttachment
    builder.setAppFunctionData("messageAttachment", simpleMessageSimpleAttachmentFactory.toAppFunctionData(messageAttachment))

    return builder.build()
  }
}
