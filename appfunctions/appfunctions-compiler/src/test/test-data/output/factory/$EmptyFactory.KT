package com.testdata

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$EmptyFactory` : AppFunctionSerializableFactory<Empty> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): Empty {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.Empty")


    val resultEmpty = Empty()
    return resultEmpty
  }

  override fun toAppFunctionData(appFunctionSerializable: Empty): AppFunctionData {
    val empty_appFunctionSerializable = appFunctionSerializable

    val builder = getAppFunctionDataBuilder("com.testdata.Empty")

    return builder.build()
  }
}
