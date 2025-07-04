package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SimpleMessage$SimpleAttachmentFactory` : AppFunctionSerializableFactory<SimpleMessage.SimpleAttachment> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): SimpleMessage.SimpleAttachment {

    val displayName = checkNotNull(appFunctionData.getStringOrNull("displayName"))
    val mimeType = appFunctionData.getStringOrNull("mimeType")

    val resultSimpleAttachment = SimpleMessage.SimpleAttachment(displayName, mimeType)
    return resultSimpleAttachment
  }

  override fun toAppFunctionData(appFunctionSerializable: SimpleMessage.SimpleAttachment): AppFunctionData {
    val simpleAttachment_appFunctionSerializable = appFunctionSerializable

    val builder = AppFunctionData.Builder("com.testdata.SimpleMessage${'$'}SimpleAttachment")
    val displayName = simpleAttachment_appFunctionSerializable.displayName
    builder.setString("displayName", displayName)
    val mimeType = simpleAttachment_appFunctionSerializable.mimeType
    if (mimeType != null) {
      builder.setString("mimeType", mimeType)
    }

    return builder.build()
  }
}
