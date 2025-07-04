package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$StringBaseSerializableFactory` : AppFunctionSerializableFactory<StringBaseSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): StringBaseSerializable {

    val stringToOpen = checkNotNull(appFunctionData.getString("stringToOpen"))

    return StringBaseSerializable(stringToOpen)
  }

  override fun toAppFunctionData(appFunctionSerializable: StringBaseSerializable): AppFunctionData {

    val builder = AppFunctionData.Builder("com.testdata.StringBaseSerializable")
    val stringToOpen = appFunctionSerializable.stringToOpen
    builder.setString("stringToOpen", stringToOpen)

    return builder.build()
  }
}
