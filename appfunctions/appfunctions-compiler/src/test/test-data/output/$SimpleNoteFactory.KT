package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SimpleNoteFactory` : AppFunctionSerializableFactory<SimpleNote> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): SimpleNote {
    val simpleNoteSimpleAttachmentFactory = `$SimpleNote$SimpleAttachmentFactory`()

    val title = checkNotNull(appFunctionData.getStringOrNull("title"))
    val content = checkNotNull(appFunctionData.getStringOrNull("content"))
    val attachmentData = checkNotNull(appFunctionData.getAppFunctionData("attachment"))
    val attachment = simpleNoteSimpleAttachmentFactory.fromAppFunctionData(attachmentData)

    val resultSimpleNote = SimpleNote(title, content, attachment)
    return resultSimpleNote
  }

  override fun toAppFunctionData(appFunctionSerializable: SimpleNote): AppFunctionData {
    val simpleNote_appFunctionSerializable = appFunctionSerializable
    val simpleNoteSimpleAttachmentFactory = `$SimpleNote$SimpleAttachmentFactory`()

    val builder = AppFunctionData.Builder("com.testdata.SimpleNote")
    val title = simpleNote_appFunctionSerializable.title
    builder.setString("title", title)
    val content = simpleNote_appFunctionSerializable.content
    builder.setString("content", content)
    val attachment = simpleNote_appFunctionSerializable.attachment
    builder.setAppFunctionData("attachment", simpleNoteSimpleAttachmentFactory.toAppFunctionData(attachment))

    return builder.build()
  }
}
