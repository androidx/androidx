package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SetFieldFactory`<T>(
  private val tTypeParameter: AppFunctionSerializableFactory.TypeParameter<T>,
) : AppFunctionSerializableFactory<SetField<T>> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): SetField<T> {

    val value = tTypeParameter.getFromAppFunctionData(appFunctionData,"value")

    val resultSetField = SetField(value)
    return resultSetField
  }

  override fun toAppFunctionData(appFunctionSerializable: SetField<T>): AppFunctionData {
    val setField_appFunctionSerializable = appFunctionSerializable

    val builder = AppFunctionData.Builder("com.testdata.SetField")
    val value = setField_appFunctionSerializable.value
    tTypeParameter.setValueInAppFunctionData(builder, "value", value)

    return builder.build()
  }
}
