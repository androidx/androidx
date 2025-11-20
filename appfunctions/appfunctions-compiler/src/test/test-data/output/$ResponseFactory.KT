package com.testdata

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$CreateNoteFunction$ResponseFactory` : AppFunctionSerializableFactory<CreateNoteFunction.Response> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): CreateNoteFunction.Response {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.CreateNoteFunction${'$'}Response")
    val createNoteFunctionDataMyNoteFactory = `$CreateNoteFunction$Data$MyNoteFactory`()

    val createdNoteData = checkNotNull(appFunctionDataWithSpec.getAppFunctionData("createdNote"))
    val createdNote = createNoteFunctionDataMyNoteFactory.fromAppFunctionData(createdNoteData)

    val resultResponse = CreateNoteFunction.Response(createdNote)
    return resultResponse
  }

  override fun toAppFunctionData(appFunctionSerializable: CreateNoteFunction.Response): AppFunctionData {
    val response_appFunctionSerializable = appFunctionSerializable
    val createNoteFunctionDataMyNoteFactory = `$CreateNoteFunction$Data$MyNoteFactory`()

    val builder = getAppFunctionDataBuilder("com.testdata.CreateNoteFunction${'$'}Response")
    val createdNote = response_appFunctionSerializable.createdNote
    builder.setAppFunctionData("createdNote", createNoteFunctionDataMyNoteFactory.toAppFunctionData(createdNote))

    return builder.build()
  }
}
