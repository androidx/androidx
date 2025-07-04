package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import com.testdata.differentPackage.`$DiffPackageSerializableFactory`
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$EntityWithDiffPackageSerializablePropertyFactory` : AppFunctionSerializableFactory<EntityWithDiffPackageSerializableProperty> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): EntityWithDiffPackageSerializableProperty {
    val diffPackageSerializableFactory = `$DiffPackageSerializableFactory`()

    val inputSerializableData = checkNotNull(appFunctionData.getAppFunctionData("inputSerializable"))
    val inputSerializable = diffPackageSerializableFactory.fromAppFunctionData(inputSerializableData)
    val serializableListData = checkNotNull(appFunctionData.getAppFunctionDataList("serializableList"))
    val serializableList = serializableListData.map { data ->
      diffPackageSerializableFactory.fromAppFunctionData(data)
    }

    val resultEntityWithDiffPackageSerializableProperty = EntityWithDiffPackageSerializableProperty(inputSerializable, serializableList)
    return resultEntityWithDiffPackageSerializableProperty
  }

  override fun toAppFunctionData(appFunctionSerializable: EntityWithDiffPackageSerializableProperty): AppFunctionData {
    val entityWithDiffPackageSerializableProperty_appFunctionSerializable = appFunctionSerializable
    val diffPackageSerializableFactory = `$DiffPackageSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.EntityWithDiffPackageSerializableProperty")
    val inputSerializable = entityWithDiffPackageSerializableProperty_appFunctionSerializable.inputSerializable
    builder.setAppFunctionData("inputSerializable", diffPackageSerializableFactory.toAppFunctionData(inputSerializable))
    val serializableList = entityWithDiffPackageSerializableProperty_appFunctionSerializable.serializableList
    builder.setAppFunctionDataList("serializableList", serializableList.map{ diffPackageSerializable ->
      diffPackageSerializableFactory.toAppFunctionData(diffPackageSerializable)
    })

    return builder.build()
  }
}
