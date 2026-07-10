package com.testdata

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SimpleMessage$SimpleAttachmentFactory` : AppFunctionSerializableFactory<SimpleMessage.SimpleAttachment> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): SimpleMessage.SimpleAttachment {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.SimpleMessage${'$'}SimpleAttachment")

    val displayName = checkNotNull(appFunctionDataWithSpec.getStringOrNull("displayName"))
    val mimeType = appFunctionDataWithSpec.getStringOrNull("mimeType")

    val resultSimpleAttachment = SimpleMessage.SimpleAttachment(displayName, mimeType)
    return resultSimpleAttachment
  }

  override fun toAppFunctionData(appFunctionSerializable: SimpleMessage.SimpleAttachment): AppFunctionData {
    val simpleAttachment_appFunctionSerializable = appFunctionSerializable

    val builder = getAppFunctionDataBuilder("com.testdata.SimpleMessage${'$'}SimpleAttachment")
    val displayName = simpleAttachment_appFunctionSerializable.displayName
    builder.setString("displayName", displayName)
    val mimeType = simpleAttachment_appFunctionSerializable.mimeType
    if (mimeType != null) {
      builder.setString("mimeType", mimeType)
    }

    return builder.build()
  }
}
