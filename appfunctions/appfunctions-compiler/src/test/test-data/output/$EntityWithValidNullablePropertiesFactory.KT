package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$EntityWithValidNullablePropertiesFactory` : AppFunctionSerializableFactory<EntityWithValidNullableProperties> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): EntityWithValidNullableProperties {
    val inputSerializableFactory = `$InputSerializableFactory`()

    val longParam = appFunctionData.getLongOrNull("longParam")
    val doubleParam = appFunctionData.getDoubleOrNull("doubleParam")
    val boolParam = appFunctionData.getBooleanOrNull("boolParam")
    val stringParam = appFunctionData.getStringOrNull("stringParam")
    val longArray = appFunctionData.getLongArray("longArray")
    val doubleArray = appFunctionData.getDoubleArray("doubleArray")
    val boolArray = appFunctionData.getBooleanArray("boolArray")
    val stringList = appFunctionData.getStringList("stringList")
    val inputSerializableData = appFunctionData.getAppFunctionData("inputSerializable")
    var inputSerializable: InputSerializable? = null
    if (inputSerializableData != null) {
      inputSerializable = inputSerializableFactory.fromAppFunctionData(inputSerializableData)
    }
    val serializableListData = appFunctionData.getAppFunctionDataList("serializableList")
    val serializableList = serializableListData?.map { data ->
      inputSerializableFactory.fromAppFunctionData(data)
    }

    val resultEntityWithValidNullableProperties = EntityWithValidNullableProperties(longParam, doubleParam, boolParam, stringParam, longArray, doubleArray, boolArray, stringList, inputSerializable, serializableList)
    return resultEntityWithValidNullableProperties
  }

  override fun toAppFunctionData(appFunctionSerializable: EntityWithValidNullableProperties): AppFunctionData {
    val entityWithValidNullableProperties_appFunctionSerializable = appFunctionSerializable
    val inputSerializableFactory = `$InputSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.EntityWithValidNullableProperties")
    val longParam = entityWithValidNullableProperties_appFunctionSerializable.longParam
    if (longParam != null) {
      builder.setLong("longParam", longParam)
    }
    val doubleParam = entityWithValidNullableProperties_appFunctionSerializable.doubleParam
    if (doubleParam != null) {
      builder.setDouble("doubleParam", doubleParam)
    }
    val boolParam = entityWithValidNullableProperties_appFunctionSerializable.boolParam
    if (boolParam != null) {
      builder.setBoolean("boolParam", boolParam)
    }
    val stringParam = entityWithValidNullableProperties_appFunctionSerializable.stringParam
    if (stringParam != null) {
      builder.setString("stringParam", stringParam)
    }
    val longArray = entityWithValidNullableProperties_appFunctionSerializable.longArray
    if (longArray != null) {
      builder.setLongArray("longArray", longArray)
    }
    val doubleArray = entityWithValidNullableProperties_appFunctionSerializable.doubleArray
    if (doubleArray != null) {
      builder.setDoubleArray("doubleArray", doubleArray)
    }
    val boolArray = entityWithValidNullableProperties_appFunctionSerializable.boolArray
    if (boolArray != null) {
      builder.setBooleanArray("boolArray", boolArray)
    }
    val stringList = entityWithValidNullableProperties_appFunctionSerializable.stringList
    if (stringList != null) {
      builder.setStringList("stringList", stringList)
    }
    val inputSerializable = entityWithValidNullableProperties_appFunctionSerializable.inputSerializable
    if (inputSerializable != null) {
      builder.setAppFunctionData("inputSerializable", inputSerializableFactory.toAppFunctionData(inputSerializable))
    }
    val serializableList = entityWithValidNullableProperties_appFunctionSerializable.serializableList
    if (serializableList != null) {
      builder.setAppFunctionDataList("serializableList", serializableList.map{ inputSerializable ->
        inputSerializableFactory.toAppFunctionData(inputSerializable)
      })
    }

    return builder.build()
  }
}
