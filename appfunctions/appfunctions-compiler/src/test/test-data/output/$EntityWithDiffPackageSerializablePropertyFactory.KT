package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import com.testdata.differentPackage.`$DiffPackageInputSerializableFactory`
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$EntityWithDiffPackageSerializablePropertyFactory` :
    AppFunctionSerializableFactory<EntityWithDiffPackageSerializableProperty> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData):
      EntityWithDiffPackageSerializableProperty {
    val diffPackageInputSerializableFactory = `$DiffPackageInputSerializableFactory`()

    val inputSerializable =
        diffPackageInputSerializableFactory.fromAppFunctionData(appFunctionData.getAppFunctionData("inputSerializable"))
    val serializableListData = appFunctionData.getAppFunctionDataList("serializableList") ?:
        emptyList()
    val serializableList = serializableListData.map {data ->
        diffPackageInputSerializableFactory.fromAppFunctionData(data)}

    return EntityWithDiffPackageSerializableProperty(inputSerializable, serializableList)
  }

  override
      fun toAppFunctionData(entityWithDiffPackageSerializableProperty: EntityWithDiffPackageSerializableProperty):
      AppFunctionData {
    val diffPackageInputSerializableFactory = `$DiffPackageInputSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.EntityWithDiffPackageSerializableProperty")
    builder.setAppFunctionData("inputSerializable",
        diffPackageInputSerializableFactory.toAppFunctionData(entityWithDiffPackageSerializableProperty.inputSerializable))
    builder.setAppFunctionDataList("serializableList",
        entityWithDiffPackageSerializableProperty.serializableList.map{
        diffPackageInputSerializable ->
        diffPackageInputSerializableFactory.toAppFunctionData(diffPackageInputSerializable) })
    return builder.build()
  }
}
