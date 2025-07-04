package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$NonNestedLongBaseSerializableFactory` :
    AppFunctionSerializableFactory<NonNestedLongBaseSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData):
      NonNestedLongBaseSerializable {
    val nestedStringBaseSerializableFactory = `$NestedStringBaseSerializableFactory`()

    val stringBaseName = appFunctionData.getString("stringBaseName")
    val nestedStringBaseSerializableData =
        appFunctionData.getAppFunctionData("nestedStringBaseSerializable")
    val nestedStringBaseSerializable =
        nestedStringBaseSerializableFactory.fromAppFunctionData(nestedStringBaseSerializableData)
    val longBaseValue = appFunctionData.getLong("longBaseValue")

    return NonNestedLongBaseSerializable(stringBaseName, nestedStringBaseSerializable,
        longBaseValue)
  }

  override fun toAppFunctionData(nonNestedLongBaseSerializable: NonNestedLongBaseSerializable):
      AppFunctionData {
    val nestedStringBaseSerializableFactory = `$NestedStringBaseSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.NonNestedLongBaseSerializable")
    builder.setString("stringBaseName", nonNestedLongBaseSerializable.stringBaseName)
    builder.setAppFunctionData("nestedStringBaseSerializable",
        nestedStringBaseSerializableFactory.toAppFunctionData(nonNestedLongBaseSerializable.nestedStringBaseSerializable))
    builder.setLong("longBaseValue", nonNestedLongBaseSerializable.longBaseValue)

    return builder.build()
  }
}
