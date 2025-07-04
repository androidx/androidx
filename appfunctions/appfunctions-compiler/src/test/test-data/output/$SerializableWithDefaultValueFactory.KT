package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SerializableWithDefaultValueFactory` : AppFunctionSerializableFactory<SerializableWithDefaultValue> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): SerializableWithDefaultValue {
    val nestedSerializableFactory = `$NestedSerializableFactory`()

    val optionalNonNullInt = appFunctionData.getIntOrNull("optionalNonNullInt") ?: 0
    val optionalNullableInt = appFunctionData.getIntOrNull("optionalNullableInt")
    val optionalNonNullLong = appFunctionData.getLongOrNull("optionalNonNullLong") ?: 0L
    val optionalNullableLong = appFunctionData.getLongOrNull("optionalNullableLong")
    val optionalNonNullBoolean = appFunctionData.getBooleanOrNull("optionalNonNullBoolean") ?: false
    val optionalNullableBoolean = appFunctionData.getBooleanOrNull("optionalNullableBoolean")
    val optionalNonNullFloat = appFunctionData.getFloatOrNull("optionalNonNullFloat") ?: 0.0f
    val optionalNullableFloat = appFunctionData.getFloatOrNull("optionalNullableFloat")
    val optionalNonNullDouble = appFunctionData.getDoubleOrNull("optionalNonNullDouble") ?: 0.0
    val optionalNullableDouble = appFunctionData.getDoubleOrNull("optionalNullableDouble")
    val optionalNullableString = appFunctionData.getStringOrNull("optionalNullableString")
    val optionalNullableSerializableData = appFunctionData.getAppFunctionData("optionalNullableSerializable")
    var optionalNullableSerializable: NestedSerializable? = null
    if (optionalNullableSerializableData != null) {
      optionalNullableSerializable = nestedSerializableFactory.fromAppFunctionData(optionalNullableSerializableData)
    }
    val optionalNonNullIntArray = appFunctionData.getIntArray("optionalNonNullIntArray") ?: intArrayOf()
    val optionalNullableIntArray = appFunctionData.getIntArray("optionalNullableIntArray")
    val optionalNonNullLongArray = appFunctionData.getLongArray("optionalNonNullLongArray") ?: longArrayOf()
    val optionalNullableLongArray = appFunctionData.getLongArray("optionalNullableLongArray")
    val optionalNonNullBooleanArray = appFunctionData.getBooleanArray("optionalNonNullBooleanArray") ?: booleanArrayOf()
    val optionalNullableBooleanArray = appFunctionData.getBooleanArray("optionalNullableBooleanArray")
    val optionalNonNullFloatArray = appFunctionData.getFloatArray("optionalNonNullFloatArray") ?: floatArrayOf()
    val optionalNullableFloatArray = appFunctionData.getFloatArray("optionalNullableFloatArray")
    val optionalNonNullDoubleArray = appFunctionData.getDoubleArray("optionalNonNullDoubleArray") ?: doubleArrayOf()
    val optionalNullableDoubleArray = appFunctionData.getDoubleArray("optionalNullableDoubleArray")
    val optionalNonNullByteArray = appFunctionData.getByteArray("optionalNonNullByteArray") ?: byteArrayOf()
    val optionalNullableByteArray = appFunctionData.getByteArray("optionalNullableByteArray")
    val optionalNonNullListString = appFunctionData.getStringList("optionalNonNullListString") ?: emptyList()
    val optionalNullableListString = appFunctionData.getStringList("optionalNullableListString")
    val optionalNonNullSerializableListData = appFunctionData.getAppFunctionDataList("optionalNonNullSerializableList") ?: emptyList()
    val optionalNonNullSerializableList = optionalNonNullSerializableListData.map { data ->
      nestedSerializableFactory.fromAppFunctionData(data)
    }
    val optionalNullableSerializableListData = appFunctionData.getAppFunctionDataList("optionalNullableSerializableList")
    val optionalNullableSerializableList = optionalNullableSerializableListData?.map { data ->
      nestedSerializableFactory.fromAppFunctionData(data)
    }

    val resultSerializableWithDefaultValue = SerializableWithDefaultValue(optionalNonNullInt, optionalNullableInt, optionalNonNullLong, optionalNullableLong, optionalNonNullBoolean, optionalNullableBoolean, optionalNonNullFloat, optionalNullableFloat, optionalNonNullDouble, optionalNullableDouble, optionalNullableString, optionalNullableSerializable, optionalNonNullIntArray, optionalNullableIntArray, optionalNonNullLongArray, optionalNullableLongArray, optionalNonNullBooleanArray, optionalNullableBooleanArray, optionalNonNullFloatArray, optionalNullableFloatArray, optionalNonNullDoubleArray, optionalNullableDoubleArray, optionalNonNullByteArray, optionalNullableByteArray, optionalNonNullListString, optionalNullableListString, optionalNonNullSerializableList, optionalNullableSerializableList)
    return resultSerializableWithDefaultValue
  }

  override fun toAppFunctionData(appFunctionSerializable: SerializableWithDefaultValue): AppFunctionData {
    val serializableWithDefaultValue_appFunctionSerializable = appFunctionSerializable
    val nestedSerializableFactory = `$NestedSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.SerializableWithDefaultValue")
    val optionalNonNullInt = serializableWithDefaultValue_appFunctionSerializable.optionalNonNullInt
    builder.setInt("optionalNonNullInt", optionalNonNullInt)
    val optionalNullableInt = serializableWithDefaultValue_appFunctionSerializable.optionalNullableInt
    if (optionalNullableInt != null) {
      builder.setInt("optionalNullableInt", optionalNullableInt)
    }
    val optionalNonNullLong = serializableWithDefaultValue_appFunctionSerializable.optionalNonNullLong
    builder.setLong("optionalNonNullLong", optionalNonNullLong)
    val optionalNullableLong = serializableWithDefaultValue_appFunctionSerializable.optionalNullableLong
    if (optionalNullableLong != null) {
      builder.setLong("optionalNullableLong", optionalNullableLong)
    }
    val optionalNonNullBoolean = serializableWithDefaultValue_appFunctionSerializable.optionalNonNullBoolean
    builder.setBoolean("optionalNonNullBoolean", optionalNonNullBoolean)
    val optionalNullableBoolean = serializableWithDefaultValue_appFunctionSerializable.optionalNullableBoolean
    if (optionalNullableBoolean != null) {
      builder.setBoolean("optionalNullableBoolean", optionalNullableBoolean)
    }
    val optionalNonNullFloat = serializableWithDefaultValue_appFunctionSerializable.optionalNonNullFloat
    builder.setFloat("optionalNonNullFloat", optionalNonNullFloat)
    val optionalNullableFloat = serializableWithDefaultValue_appFunctionSerializable.optionalNullableFloat
    if (optionalNullableFloat != null) {
      builder.setFloat("optionalNullableFloat", optionalNullableFloat)
    }
    val optionalNonNullDouble = serializableWithDefaultValue_appFunctionSerializable.optionalNonNullDouble
    builder.setDouble("optionalNonNullDouble", optionalNonNullDouble)
    val optionalNullableDouble = serializableWithDefaultValue_appFunctionSerializable.optionalNullableDouble
    if (optionalNullableDouble != null) {
      builder.setDouble("optionalNullableDouble", optionalNullableDouble)
    }
    val optionalNullableString = serializableWithDefaultValue_appFunctionSerializable.optionalNullableString
    if (optionalNullableString != null) {
      builder.setString("optionalNullableString", optionalNullableString)
    }
    val optionalNullableSerializable = serializableWithDefaultValue_appFunctionSerializable.optionalNullableSerializable
    if (optionalNullableSerializable != null) {
      builder.setAppFunctionData("optionalNullableSerializable", nestedSerializableFactory.toAppFunctionData(optionalNullableSerializable))
    }
    val optionalNonNullIntArray = serializableWithDefaultValue_appFunctionSerializable.optionalNonNullIntArray
    builder.setIntArray("optionalNonNullIntArray", optionalNonNullIntArray)
    val optionalNullableIntArray = serializableWithDefaultValue_appFunctionSerializable.optionalNullableIntArray
    if (optionalNullableIntArray != null) {
      builder.setIntArray("optionalNullableIntArray", optionalNullableIntArray)
    }
    val optionalNonNullLongArray = serializableWithDefaultValue_appFunctionSerializable.optionalNonNullLongArray
    builder.setLongArray("optionalNonNullLongArray", optionalNonNullLongArray)
    val optionalNullableLongArray = serializableWithDefaultValue_appFunctionSerializable.optionalNullableLongArray
    if (optionalNullableLongArray != null) {
      builder.setLongArray("optionalNullableLongArray", optionalNullableLongArray)
    }
    val optionalNonNullBooleanArray = serializableWithDefaultValue_appFunctionSerializable.optionalNonNullBooleanArray
    builder.setBooleanArray("optionalNonNullBooleanArray", optionalNonNullBooleanArray)
    val optionalNullableBooleanArray = serializableWithDefaultValue_appFunctionSerializable.optionalNullableBooleanArray
    if (optionalNullableBooleanArray != null) {
      builder.setBooleanArray("optionalNullableBooleanArray", optionalNullableBooleanArray)
    }
    val optionalNonNullFloatArray = serializableWithDefaultValue_appFunctionSerializable.optionalNonNullFloatArray
    builder.setFloatArray("optionalNonNullFloatArray", optionalNonNullFloatArray)
    val optionalNullableFloatArray = serializableWithDefaultValue_appFunctionSerializable.optionalNullableFloatArray
    if (optionalNullableFloatArray != null) {
      builder.setFloatArray("optionalNullableFloatArray", optionalNullableFloatArray)
    }
    val optionalNonNullDoubleArray = serializableWithDefaultValue_appFunctionSerializable.optionalNonNullDoubleArray
    builder.setDoubleArray("optionalNonNullDoubleArray", optionalNonNullDoubleArray)
    val optionalNullableDoubleArray = serializableWithDefaultValue_appFunctionSerializable.optionalNullableDoubleArray
    if (optionalNullableDoubleArray != null) {
      builder.setDoubleArray("optionalNullableDoubleArray", optionalNullableDoubleArray)
    }
    val optionalNonNullByteArray = serializableWithDefaultValue_appFunctionSerializable.optionalNonNullByteArray
    builder.setByteArray("optionalNonNullByteArray", optionalNonNullByteArray)
    val optionalNullableByteArray = serializableWithDefaultValue_appFunctionSerializable.optionalNullableByteArray
    if (optionalNullableByteArray != null) {
      builder.setByteArray("optionalNullableByteArray", optionalNullableByteArray)
    }
    val optionalNonNullListString = serializableWithDefaultValue_appFunctionSerializable.optionalNonNullListString
    builder.setStringList("optionalNonNullListString", optionalNonNullListString)
    val optionalNullableListString = serializableWithDefaultValue_appFunctionSerializable.optionalNullableListString
    if (optionalNullableListString != null) {
      builder.setStringList("optionalNullableListString", optionalNullableListString)
    }
    val optionalNonNullSerializableList = serializableWithDefaultValue_appFunctionSerializable.optionalNonNullSerializableList
    builder.setAppFunctionDataList("optionalNonNullSerializableList", optionalNonNullSerializableList.map{ nestedSerializable ->
      nestedSerializableFactory.toAppFunctionData(nestedSerializable)
    })
    val optionalNullableSerializableList = serializableWithDefaultValue_appFunctionSerializable.optionalNullableSerializableList
    if (optionalNullableSerializableList != null) {
      builder.setAppFunctionDataList("optionalNullableSerializableList", optionalNullableSerializableList.map{ nestedSerializable ->
        nestedSerializableFactory.toAppFunctionData(nestedSerializable)
      })
    }

    return builder.build()
  }
}
