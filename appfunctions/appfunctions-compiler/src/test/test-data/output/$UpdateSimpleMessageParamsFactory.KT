package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated
import kotlin.String
import kotlin.collections.List

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$UpdateSimpleMessageParamsFactory` : AppFunctionSerializableFactory<UpdateSimpleMessageParams> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): UpdateSimpleMessageParams {
    val simpleSetFieldKotlinStringFactory = `$SimpleSetFieldFactory`<String>(
      AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(String::class.java))
    val simpleSetFieldKotlinStringNullableFactory = `$SimpleSetFieldFactory`<String?>(
      @Suppress("UNCHECKED_CAST") AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(String::class.java as Class<String?>))
    val simpleSetFieldKotlinCollectionsListComTestdataSimplemessageSimpleattachmentFactory = `$SimpleSetFieldFactory`<List<SimpleMessage.SimpleAttachment>>(
      AppFunctionSerializableFactory.TypeParameter.SerializableListTypeParameter(SimpleMessage.SimpleAttachment::class.java, `$SimpleMessage$SimpleAttachmentFactory`()))

    val titleData = appFunctionData.getAppFunctionData("title")
    var title: SimpleSetField<String>? = null
    if (titleData != null) {
      title = simpleSetFieldKotlinStringFactory.fromAppFunctionData(titleData)
    }
    val contentData = appFunctionData.getAppFunctionData("content")
    var content: SimpleSetField<String?>? = null
    if (contentData != null) {
      content = simpleSetFieldKotlinStringNullableFactory.fromAppFunctionData(contentData)
    }
    val attachmentsData = appFunctionData.getAppFunctionData("attachments")
    var attachments: SimpleSetField<List<SimpleMessage.SimpleAttachment>>? = null
    if (attachmentsData != null) {
      attachments = simpleSetFieldKotlinCollectionsListComTestdataSimplemessageSimpleattachmentFactory.fromAppFunctionData(attachmentsData)
    }

    val resultUpdateSimpleMessageParams = UpdateSimpleMessageParams(title, content, attachments)
    return resultUpdateSimpleMessageParams
  }

  override fun toAppFunctionData(appFunctionSerializable: UpdateSimpleMessageParams): AppFunctionData {
    val updateSimpleMessageParams_appFunctionSerializable = appFunctionSerializable
    val simpleSetFieldKotlinStringFactory = `$SimpleSetFieldFactory`<String>(
      AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(String::class.java))
    val simpleSetFieldKotlinStringNullableFactory = `$SimpleSetFieldFactory`<String?>(
      @Suppress("UNCHECKED_CAST") AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(String::class.java as Class<String?>))
    val simpleSetFieldKotlinCollectionsListComTestdataSimplemessageSimpleattachmentFactory = `$SimpleSetFieldFactory`<List<SimpleMessage.SimpleAttachment>>(
      AppFunctionSerializableFactory.TypeParameter.SerializableListTypeParameter(SimpleMessage.SimpleAttachment::class.java, `$SimpleMessage$SimpleAttachmentFactory`()))

    val builder = AppFunctionData.Builder("com.testdata.UpdateSimpleMessageParams")
    val title = updateSimpleMessageParams_appFunctionSerializable.title
    if (title != null) {
      builder.setAppFunctionData("title", simpleSetFieldKotlinStringFactory.toAppFunctionData(title))
    }
    val content = updateSimpleMessageParams_appFunctionSerializable.content
    if (content != null) {
      builder.setAppFunctionData("content", simpleSetFieldKotlinStringNullableFactory.toAppFunctionData(content))
    }
    val attachments = updateSimpleMessageParams_appFunctionSerializable.attachments
    if (attachments != null) {
      builder.setAppFunctionData("attachments", simpleSetFieldKotlinCollectionsListComTestdataSimplemessageSimpleattachmentFactory.toAppFunctionData(attachments))
    }

    return builder.build()
  }
}
