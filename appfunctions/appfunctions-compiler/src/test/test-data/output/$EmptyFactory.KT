package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$EmptyFactory` : AppFunctionSerializableFactory<Empty> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): Empty {


    val resultEmpty = Empty()
    return resultEmpty
  }

  override fun toAppFunctionData(appFunctionSerializable: Empty): AppFunctionData {
    val empty_appFunctionSerializable = appFunctionSerializable

    val builder = AppFunctionData.Builder("com.testdata.Empty")

    return builder.build()
  }
}
