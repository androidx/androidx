package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$CreateSimpleNoteParamsFactory` : AppFunctionSerializableFactory<CreateSimpleNoteParams> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): CreateSimpleNoteParams {
    val simpleNoteSimpleAttachmentFactory = `$SimpleNote$SimpleAttachmentFactory`()

    val title = checkNotNull(appFunctionData.getStringOrNull("title"))
    val content = appFunctionData.getStringOrNull("content")
    val attachmentsData = appFunctionData.getAppFunctionDataList("attachments") ?: emptyList()
    val attachments = attachmentsData.map { data ->
      simpleNoteSimpleAttachmentFactory.fromAppFunctionData(data)
    }

    val resultCreateSimpleNoteParams = CreateSimpleNoteParams(title, content, attachments)
    return resultCreateSimpleNoteParams
  }

  override fun toAppFunctionData(appFunctionSerializable: CreateSimpleNoteParams): AppFunctionData {
    val createSimpleNoteParams_appFunctionSerializable = appFunctionSerializable
    val simpleNoteSimpleAttachmentFactory = `$SimpleNote$SimpleAttachmentFactory`()

    val builder = AppFunctionData.Builder("com.testdata.CreateSimpleNoteParams")
    val title = createSimpleNoteParams_appFunctionSerializable.title
    builder.setString("title", title)
    val content = createSimpleNoteParams_appFunctionSerializable.content
    if (content != null) {
      builder.setString("content", content)
    }
    val attachments = createSimpleNoteParams_appFunctionSerializable.attachments
    builder.setAppFunctionDataList("attachments", attachments.map{ simpleNoteSimpleAttachment ->
      simpleNoteSimpleAttachmentFactory.toAppFunctionData(simpleNoteSimpleAttachment)
    })

    return builder.build()
  }
}
