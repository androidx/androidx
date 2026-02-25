package com.testdata

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$CreateSimpleNoteParamsFactory` : AppFunctionSerializableFactory<CreateSimpleNoteParams> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): CreateSimpleNoteParams {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.CreateSimpleNoteParams")
    val simpleNoteSimpleAttachmentFactory = `$SimpleNote$SimpleAttachmentFactory`()

    val title = checkNotNull(appFunctionDataWithSpec.getStringOrNull("title"))
    val content = appFunctionDataWithSpec.getStringOrNull("content")
    val attachmentsData = appFunctionDataWithSpec.getAppFunctionDataList("attachments") ?: emptyList()
    val attachments = attachmentsData.map { data ->
      simpleNoteSimpleAttachmentFactory.fromAppFunctionData(data)
    }

    val resultCreateSimpleNoteParams = CreateSimpleNoteParams(title, content, attachments)
    return resultCreateSimpleNoteParams
  }

  override fun toAppFunctionData(appFunctionSerializable: CreateSimpleNoteParams): AppFunctionData {
    val createSimpleNoteParams_appFunctionSerializable = appFunctionSerializable
    val simpleNoteSimpleAttachmentFactory = `$SimpleNote$SimpleAttachmentFactory`()

    val builder = getAppFunctionDataBuilder("com.testdata.CreateSimpleNoteParams")
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
