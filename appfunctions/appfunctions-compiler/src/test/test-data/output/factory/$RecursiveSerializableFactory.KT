package com.testdata

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$RecursiveSerializableFactory` : AppFunctionSerializableFactory<RecursiveSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): RecursiveSerializable {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.RecursiveSerializable")
    val recursiveSerializableFactory = `$RecursiveSerializableFactory`()

    val nestedData = appFunctionDataWithSpec.getAppFunctionData("nested")
    var nested: RecursiveSerializable? = null
    if (nestedData != null) {
      nested = recursiveSerializableFactory.fromAppFunctionData(nestedData)
    }

    val resultRecursiveSerializable = RecursiveSerializable(nested)
    return resultRecursiveSerializable
  }

  override fun toAppFunctionData(appFunctionSerializable: RecursiveSerializable): AppFunctionData {
    val recursiveSerializable_appFunctionSerializable = appFunctionSerializable
    val recursiveSerializableFactory = `$RecursiveSerializableFactory`()

    val builder = getAppFunctionDataBuilder("com.testdata.RecursiveSerializable")
    val nested = recursiveSerializable_appFunctionSerializable.nested
    if (nested != null) {
      builder.setAppFunctionData("nested", recursiveSerializableFactory.toAppFunctionData(nested))
    }

    return builder.build()
  }
}
