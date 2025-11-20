package com.testdata

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SimpleNote$SimpleAttachmentFactory` : AppFunctionSerializableFactory<SimpleNote.SimpleAttachment> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): SimpleNote.SimpleAttachment {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.SimpleNote${'$'}SimpleAttachment")

    val displayName = checkNotNull(appFunctionDataWithSpec.getStringOrNull("displayName"))
    val mimeType = appFunctionDataWithSpec.getStringOrNull("mimeType")

    val resultSimpleAttachment = SimpleNote.SimpleAttachment(displayName, mimeType)
    return resultSimpleAttachment
  }

  override fun toAppFunctionData(appFunctionSerializable: SimpleNote.SimpleAttachment): AppFunctionData {
    val simpleAttachment_appFunctionSerializable = appFunctionSerializable

    val builder = getAppFunctionDataBuilder("com.testdata.SimpleNote${'$'}SimpleAttachment")
    val displayName = simpleAttachment_appFunctionSerializable.displayName
    builder.setString("displayName", displayName)
    val mimeType = simpleAttachment_appFunctionSerializable.mimeType
    if (mimeType != null) {
      builder.setString("mimeType", mimeType)
    }

    return builder.build()
  }
}
