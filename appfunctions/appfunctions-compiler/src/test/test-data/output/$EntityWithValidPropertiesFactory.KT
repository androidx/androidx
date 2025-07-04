package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$EntityWithValidPropertiesFactory` : AppFunctionSerializableFactory<EntityWithValidProperties> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): EntityWithValidProperties {
    val inputSerializableFactory = `$InputSerializableFactory`()

    val longParam = checkNotNull(appFunctionData.getLongOrNull("longParam"))
    val doubleParam = checkNotNull(appFunctionData.getDoubleOrNull("doubleParam"))
    val boolParam = checkNotNull(appFunctionData.getBooleanOrNull("boolParam"))
    val stringParam = checkNotNull(appFunctionData.getStringOrNull("stringParam"))
    val longArray = checkNotNull(appFunctionData.getLongArray("longArray"))
    val doubleArray = checkNotNull(appFunctionData.getDoubleArray("doubleArray"))
    val boolArray = checkNotNull(appFunctionData.getBooleanArray("boolArray"))
    val stringList = checkNotNull(appFunctionData.getStringList("stringList"))
    val inputSerializableData = checkNotNull(appFunctionData.getAppFunctionData("inputSerializable"))
    val inputSerializable = inputSerializableFactory.fromAppFunctionData(inputSerializableData)
    val serializableListData = checkNotNull(appFunctionData.getAppFunctionDataList("serializableList"))
    val serializableList = serializableListData.map { data ->
      inputSerializableFactory.fromAppFunctionData(data)
    }

    val resultEntityWithValidProperties = EntityWithValidProperties(longParam, doubleParam, boolParam, stringParam, longArray, doubleArray, boolArray, stringList, inputSerializable, serializableList)
    return resultEntityWithValidProperties
  }

  override fun toAppFunctionData(appFunctionSerializable: EntityWithValidProperties): AppFunctionData {
    val entityWithValidProperties_appFunctionSerializable = appFunctionSerializable
    val inputSerializableFactory = `$InputSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.EntityWithValidProperties")
    val longParam = entityWithValidProperties_appFunctionSerializable.longParam
    builder.setLong("longParam", longParam)
    val doubleParam = entityWithValidProperties_appFunctionSerializable.doubleParam
    builder.setDouble("doubleParam", doubleParam)
    val boolParam = entityWithValidProperties_appFunctionSerializable.boolParam
    builder.setBoolean("boolParam", boolParam)
    val stringParam = entityWithValidProperties_appFunctionSerializable.stringParam
    builder.setString("stringParam", stringParam)
    val longArray = entityWithValidProperties_appFunctionSerializable.longArray
    builder.setLongArray("longArray", longArray)
    val doubleArray = entityWithValidProperties_appFunctionSerializable.doubleArray
    builder.setDoubleArray("doubleArray", doubleArray)
    val boolArray = entityWithValidProperties_appFunctionSerializable.boolArray
    builder.setBooleanArray("boolArray", boolArray)
    val stringList = entityWithValidProperties_appFunctionSerializable.stringList
    builder.setStringList("stringList", stringList)
    val inputSerializable = entityWithValidProperties_appFunctionSerializable.inputSerializable
    builder.setAppFunctionData("inputSerializable", inputSerializableFactory.toAppFunctionData(inputSerializable))
    val serializableList = entityWithValidProperties_appFunctionSerializable.serializableList
    builder.setAppFunctionDataList("serializableList", serializableList.map{ inputSerializable ->
      inputSerializableFactory.toAppFunctionData(inputSerializable)
    })

    return builder.build()
  }
}
