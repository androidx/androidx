package com.testdata

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$LongBaseSerializableFactory` : AppFunctionSerializableFactory<LongBaseSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): LongBaseSerializable {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.LongBaseSerializable")

    val age = checkNotNull(appFunctionDataWithSpec.getLongOrNull("age"))
    val stringToOpen = checkNotNull(appFunctionDataWithSpec.getStringOrNull("stringToOpen"))

    val resultLongBaseSerializable = LongBaseSerializable(age, stringToOpen)
    return resultLongBaseSerializable
  }

  override fun toAppFunctionData(appFunctionSerializable: LongBaseSerializable): AppFunctionData {
    val longBaseSerializable_appFunctionSerializable = appFunctionSerializable

    val builder = getAppFunctionDataBuilder("com.testdata.LongBaseSerializable")
    val age = longBaseSerializable_appFunctionSerializable.age
    builder.setLong("age", age)
    val stringToOpen = longBaseSerializable_appFunctionSerializable.stringToOpen
    builder.setString("stringToOpen", stringToOpen)

    return builder.build()
  }
}
