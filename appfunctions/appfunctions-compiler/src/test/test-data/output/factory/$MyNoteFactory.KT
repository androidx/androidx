package com.testdata

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$CreateNoteFunction$Data$MyNoteFactory` : AppFunctionSerializableFactory<CreateNoteFunction.Data.MyNote> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): CreateNoteFunction.Data.MyNote {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.CreateNoteFunction${'$'}Data${'$'}MyNote")
    val myAttachmentFactory = `$MyAttachmentFactory`()

    val id = checkNotNull(appFunctionDataWithSpec.getStringOrNull("id"))
    val attachmentData = checkNotNull(appFunctionDataWithSpec.getAppFunctionData("attachment"))
    val attachment = myAttachmentFactory.fromAppFunctionData(attachmentData)

    val resultMyNote = CreateNoteFunction.Data.MyNote(id, attachment)
    return resultMyNote
  }

  override fun toAppFunctionData(appFunctionSerializable: CreateNoteFunction.Data.MyNote): AppFunctionData {
    val myNote_appFunctionSerializable = appFunctionSerializable
    val myAttachmentFactory = `$MyAttachmentFactory`()

    val builder = getAppFunctionDataBuilder("com.testdata.CreateNoteFunction${'$'}Data${'$'}MyNote")
    val id = myNote_appFunctionSerializable.id
    builder.setString("id", id)
    val attachment = myNote_appFunctionSerializable.attachment
    builder.setAppFunctionData("attachment", myAttachmentFactory.toAppFunctionData(attachment))

    return builder.build()
  }
}
