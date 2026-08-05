package com.testdata

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionDataSpec
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import com.testdata.differentPackage.`$DiffPackageSerializableFactory`
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$EntityWithDiffPackageSerializablePropertyFactory` : AppFunctionSerializableFactory<EntityWithDiffPackageSerializableProperty> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): EntityWithDiffPackageSerializableProperty {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.EntityWithDiffPackageSerializableProperty")
    val diffPackageSerializableFactory = `$DiffPackageSerializableFactory`()

    val inputSerializableData = checkNotNull(appFunctionDataWithSpec.getAppFunctionData("inputSerializable"))
    val inputSerializable = diffPackageSerializableFactory.fromAppFunctionData(inputSerializableData)
    val serializableListData = checkNotNull(appFunctionDataWithSpec.getAppFunctionDataList("serializableList"))
    val serializableList = serializableListData.map { data ->
      diffPackageSerializableFactory.fromAppFunctionData(data)
    }

    val resultEntityWithDiffPackageSerializableProperty = EntityWithDiffPackageSerializableProperty(inputSerializable, serializableList)
    return resultEntityWithDiffPackageSerializableProperty
  }

  override fun toAppFunctionData(spec: AppFunctionDataSpec?, appFunctionSerializable: EntityWithDiffPackageSerializableProperty): AppFunctionData {
    val entityWithDiffPackageSerializableProperty_appFunctionSerializable = appFunctionSerializable
    val diffPackageSerializableFactory = `$DiffPackageSerializableFactory`()

    val builder = getAppFunctionDataBuilder(spec, "com.testdata.EntityWithDiffPackageSerializableProperty")
    val inputSerializable = entityWithDiffPackageSerializableProperty_appFunctionSerializable.inputSerializable
    builder.setAppFunctionData(
      "inputSerializable",
      diffPackageSerializableFactory.toAppFunctionData(
        spec?.getPropertyObjectSpec("inputSerializable", checkNotNull(inputSerializable::class.java.canonicalName)),
        inputSerializable
      )
    )
    val serializableList = entityWithDiffPackageSerializableProperty_appFunctionSerializable.serializableList
    builder.setAppFunctionDataList("serializableList", serializableList.map{ diffPackageSerializable ->
      diffPackageSerializableFactory.toAppFunctionData(
        spec?.getPropertyObjectSpec("serializableList",checkNotNull(diffPackageSerializable::class.java.canonicalName)),
        diffPackageSerializable
      )
    })

    return builder.build()
  }
}
