package com.testdata

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$DerivedSerializableFactory` : AppFunctionSerializableFactory<DerivedSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): DerivedSerializable {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.DerivedSerializable")

    val stringToOpen = checkNotNull(appFunctionDataWithSpec.getStringOrNull("stringToOpen"))
    val age = checkNotNull(appFunctionDataWithSpec.getLongOrNull("age"))

    val resultDerivedSerializable = DerivedSerializable(stringToOpen, age)
    return resultDerivedSerializable
  }

  override fun toAppFunctionData(appFunctionSerializable: DerivedSerializable): AppFunctionData {
    val derivedSerializable_appFunctionSerializable = appFunctionSerializable

    val builder = getAppFunctionDataBuilder("com.testdata.DerivedSerializable")
    val stringToOpen = derivedSerializable_appFunctionSerializable.stringToOpen
    builder.setString("stringToOpen", stringToOpen)
    val age = derivedSerializable_appFunctionSerializable.age
    builder.setLong("age", age)

    return builder.build()
  }
}
