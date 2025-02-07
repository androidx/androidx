package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$EntityWithValidPropertiesFactory` :
    AppFunctionSerializableFactory<EntityWithValidProperties> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): EntityWithValidProperties {
    val inputSerializableFactory = `$InputSerializableFactory`()

    val longParam = appFunctionData.getLong("longParam")
    val doubleParam = appFunctionData.getDouble("doubleParam")
    val boolParam = appFunctionData.getBoolean("boolParam")
    val stringParam = appFunctionData.getString("stringParam")
    val longArray = appFunctionData.getLongArray("longArray") ?: LongArray(0)
    val doubleArray = appFunctionData.getDoubleArray("doubleArray") ?: DoubleArray(0)
    val boolArray = appFunctionData.getBooleanArray("boolArray") ?: BooleanArray(0)
    val stringList = appFunctionData.getStringList("stringList") ?: emptyList()
    val inputSerializable =
        inputSerializableFactory.fromAppFunctionData(appFunctionData.getAppFunctionData("inputSerializable"))
    val serializableListData = appFunctionData.getAppFunctionDataList("serializableList") ?:
        emptyList()
    val serializableList = serializableListData.map {data ->
        inputSerializableFactory.fromAppFunctionData(data)}

    return EntityWithValidProperties(longParam, doubleParam, boolParam, stringParam, longArray,
        doubleArray, boolArray, stringList, inputSerializable, serializableList)
  }

  override fun toAppFunctionData(entityWithValidProperties: EntityWithValidProperties):
      AppFunctionData {
    val inputSerializableFactory = `$InputSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.EntityWithValidProperties")
    builder.setLong("longParam", entityWithValidProperties.longParam)
    builder.setDouble("doubleParam", entityWithValidProperties.doubleParam)
    builder.setBoolean("boolParam", entityWithValidProperties.boolParam)
    builder.setString("stringParam", entityWithValidProperties.stringParam)
    builder.setLongArray("longArray", entityWithValidProperties.longArray)
    builder.setDoubleArray("doubleArray", entityWithValidProperties.doubleArray)
    builder.setBooleanArray("boolArray", entityWithValidProperties.boolArray)
    builder.setStringList("stringList", entityWithValidProperties.stringList)
    builder.setAppFunctionData("inputSerializable",
        inputSerializableFactory.toAppFunctionData(entityWithValidProperties.inputSerializable))
    builder.setAppFunctionDataList("serializableList",
        entityWithValidProperties.serializableList.map{ inputSerializable ->
        inputSerializableFactory.toAppFunctionData(inputSerializable) })
    return builder.build()
  }
}
