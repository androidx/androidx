package com.testdata

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$NonNestedChildSerializableFactory` : AppFunctionSerializableFactory<NonNestedChildSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): NonNestedChildSerializable {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.NonNestedChildSerializable")
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()

    val stringBaseName = checkNotNull(appFunctionDataWithSpec.getStringOrNull("stringBaseName"))
    val nestedBaseData = appFunctionDataWithSpec.getAppFunctionData("nestedBase")
    var nestedBase: NestedBaseSerializable? = null
    if (nestedBaseData != null) {
      nestedBase = nestedBaseSerializableFactory.fromAppFunctionData(nestedBaseData)
    }
    val longBaseValue = checkNotNull(appFunctionDataWithSpec.getLongOrNull("longBaseValue"))

    val resultNonNestedChildSerializable = NonNestedChildSerializable(stringBaseName, nestedBase, longBaseValue)
    return resultNonNestedChildSerializable
  }

  override fun toAppFunctionData(appFunctionSerializable: NonNestedChildSerializable): AppFunctionData {
    val nonNestedChildSerializable_appFunctionSerializable = appFunctionSerializable
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()

    val builder = getAppFunctionDataBuilder("com.testdata.NonNestedChildSerializable")
    val stringBaseName = nonNestedChildSerializable_appFunctionSerializable.stringBaseName
    builder.setString("stringBaseName", stringBaseName)
    val nestedBase = nonNestedChildSerializable_appFunctionSerializable.nestedBase
    if (nestedBase != null) {
      builder.setAppFunctionData("nestedBase", nestedBaseSerializableFactory.toAppFunctionData(nestedBase))
    }
    val longBaseValue = nonNestedChildSerializable_appFunctionSerializable.longBaseValue
    builder.setLong("longBaseValue", longBaseValue)

    return builder.build()
  }
}
