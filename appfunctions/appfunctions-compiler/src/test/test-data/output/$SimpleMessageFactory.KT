package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SimpleMessageFactory` : AppFunctionSerializableFactory<SimpleMessage> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): SimpleMessage {
    val simpleMessageSimpleAttachmentFactory = `$SimpleMessage$SimpleAttachmentFactory`()

    val title = checkNotNull(appFunctionData.getStringOrNull("title"))
    val content = checkNotNull(appFunctionData.getStringOrNull("content"))
    val attachmentData = checkNotNull(appFunctionData.getAppFunctionData("attachment"))
    val attachment = simpleMessageSimpleAttachmentFactory.fromAppFunctionData(attachmentData)

    val resultSimpleMessage = SimpleMessage(title, content, attachment)
    return resultSimpleMessage
  }

  override fun toAppFunctionData(appFunctionSerializable: SimpleMessage): AppFunctionData {
    val simpleMessage_appFunctionSerializable = appFunctionSerializable
    val simpleMessageSimpleAttachmentFactory = `$SimpleMessage$SimpleAttachmentFactory`()

    val builder = AppFunctionData.Builder("com.testdata.SimpleMessage")
    val title = simpleMessage_appFunctionSerializable.title
    builder.setString("title", title)
    val content = simpleMessage_appFunctionSerializable.content
    builder.setString("content", content)
    val attachment = simpleMessage_appFunctionSerializable.attachment
    builder.setAppFunctionData("attachment", simpleMessageSimpleAttachmentFactory.toAppFunctionData(attachment))

    return builder.build()
  }
}
