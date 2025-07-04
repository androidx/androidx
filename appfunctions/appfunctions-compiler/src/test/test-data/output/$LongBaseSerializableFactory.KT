package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$LongBaseSerializableFactory` : AppFunctionSerializableFactory<LongBaseSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): LongBaseSerializable {

    val age = checkNotNull(appFunctionData.getLongOrNull("age"))
    val stringToOpen = checkNotNull(appFunctionData.getStringOrNull("stringToOpen"))

    val resultLongBaseSerializable = LongBaseSerializable(age, stringToOpen)
    return resultLongBaseSerializable
  }

  override fun toAppFunctionData(appFunctionSerializable: LongBaseSerializable): AppFunctionData {
    val longBaseSerializable_appFunctionSerializable = appFunctionSerializable

    val builder = AppFunctionData.Builder("com.testdata.LongBaseSerializable")
    val age = longBaseSerializable_appFunctionSerializable.age
    builder.setLong("age", age)
    val stringToOpen = longBaseSerializable_appFunctionSerializable.stringToOpen
    builder.setString("stringToOpen", stringToOpen)

    return builder.build()
  }
}
