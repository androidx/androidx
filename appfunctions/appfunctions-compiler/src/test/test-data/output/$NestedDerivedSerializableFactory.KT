package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$NestedDerivedSerializableFactory` : AppFunctionSerializableFactory<NestedDerivedSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): NestedDerivedSerializable {
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()
    val nestedDerivedSerializableFactory = `$NestedDerivedSerializableFactory`()

    val stringBaseName = checkNotNull(appFunctionData.getStringOrNull("stringBaseName"))
    val nestedBaseData = appFunctionData.getAppFunctionData("nestedBase")
    var nestedBase: NestedBaseSerializable? = null
    if (nestedBaseData != null) {
      nestedBase = nestedBaseSerializableFactory.fromAppFunctionData(nestedBaseData)
    }
    val longBaseValue = checkNotNull(appFunctionData.getLongOrNull("longBaseValue"))
    val nestedDerivedData = appFunctionData.getAppFunctionData("nestedDerived")
    var nestedDerived: NestedDerivedSerializable? = null
    if (nestedDerivedData != null) {
      nestedDerived = nestedDerivedSerializableFactory.fromAppFunctionData(nestedDerivedData)
    }

    val resultNestedDerivedSerializable = NestedDerivedSerializable(stringBaseName, nestedBase, longBaseValue, nestedDerived)
    return resultNestedDerivedSerializable
  }

  override fun toAppFunctionData(appFunctionSerializable: NestedDerivedSerializable): AppFunctionData {
    val nestedDerivedSerializable_appFunctionSerializable = appFunctionSerializable
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()
    val nestedDerivedSerializableFactory = `$NestedDerivedSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.NestedDerivedSerializable")
    val stringBaseName = nestedDerivedSerializable_appFunctionSerializable.stringBaseName
    builder.setString("stringBaseName", stringBaseName)
    val nestedBase = nestedDerivedSerializable_appFunctionSerializable.nestedBase
    if (nestedBase != null) {
      builder.setAppFunctionData("nestedBase", nestedBaseSerializableFactory.toAppFunctionData(nestedBase))
    }
    val longBaseValue = nestedDerivedSerializable_appFunctionSerializable.longBaseValue
    builder.setLong("longBaseValue", longBaseValue)
    val nestedDerived = nestedDerivedSerializable_appFunctionSerializable.nestedDerived
    if (nestedDerived != null) {
      builder.setAppFunctionData("nestedDerived", nestedDerivedSerializableFactory.toAppFunctionData(nestedDerived))
    }

    return builder.build()
  }
}
