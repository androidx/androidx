package com.testdata

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionDataSpec
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SimpleNoteFactory` : AppFunctionSerializableFactory<SimpleNote> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): SimpleNote {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.SimpleNote")
    val simpleNoteSimpleAttachmentFactory = `$SimpleNote$SimpleAttachmentFactory`()

    val title = checkNotNull(appFunctionDataWithSpec.getStringOrNull("title"))
    val content = checkNotNull(appFunctionDataWithSpec.getStringOrNull("content"))
    val attachmentData = checkNotNull(appFunctionDataWithSpec.getAppFunctionData("attachment"))
    val attachment = simpleNoteSimpleAttachmentFactory.fromAppFunctionData(attachmentData)

    val resultSimpleNote = SimpleNote(title, content, attachment)
    return resultSimpleNote
  }

  override fun toAppFunctionData(spec: AppFunctionDataSpec?, appFunctionSerializable: SimpleNote): AppFunctionData {
    val simpleNote_appFunctionSerializable = appFunctionSerializable
    val simpleNoteSimpleAttachmentFactory = `$SimpleNote$SimpleAttachmentFactory`()

    val builder = getAppFunctionDataBuilder(spec, "com.testdata.SimpleNote")
    val title = simpleNote_appFunctionSerializable.title
    builder.setString("title", title)
    val content = simpleNote_appFunctionSerializable.content
    builder.setString("content", content)
    val attachment = simpleNote_appFunctionSerializable.attachment
    builder.setAppFunctionData(
      "attachment",
      simpleNoteSimpleAttachmentFactory.toAppFunctionData(
        spec?.getPropertyObjectSpec("attachment", checkNotNull(attachment::class.java.canonicalName)),
        attachment
      )
    )

    return builder.build()
  }
}
