package com.testdata

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SerializableWithDefaultValueFactory` : AppFunctionSerializableFactory<SerializableWithDefaultValue> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): SerializableWithDefaultValue {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.SerializableWithDefaultValue")
    val nestedSerializableFactory = `$NestedSerializableFactory`()

    val optionalNonNullInt = appFunctionDataWithSpec.getIntOrNull("optionalNonNullInt") ?: 0
    val optionalNullableInt = appFunctionDataWithSpec.getIntOrNull("optionalNullableInt")
    val optionalNonNullLong = appFunctionDataWithSpec.getLongOrNull("optionalNonNullLong") ?: 0L
    val optionalNullableLong = appFunctionDataWithSpec.getLongOrNull("optionalNullableLong")
    val optionalNonNullBoolean = appFunctionDataWithSpec.getBooleanOrNull("optionalNonNullBoolean") ?: false
    val optionalNullableBoolean = appFunctionDataWithSpec.getBooleanOrNull("optionalNullableBoolean")
    val optionalNonNullFloat = appFunctionDataWithSpec.getFloatOrNull("optionalNonNullFloat") ?: 0.0f
    val optionalNullableFloat = appFunctionDataWithSpec.getFloatOrNull("optionalNullableFloat")
    val optionalNonNullDouble = appFunctionDataWithSpec.getDoubleOrNull("optionalNonNullDouble") ?: 0.0
    val optionalNullableDouble = appFunctionDataWithSpec.getDoubleOrNull("optionalNullableDouble")
    val optionalNullableString = appFunctionDataWithSpec.getStringOrNull("optionalNullableString")
    val optionalNullableSerializableData = appFunctionDataWithSpec.getAppFunctionData("optionalNullableSerializable")
    var optionalNullableSerializable: NestedSerializable? = null
    if (optionalNullableSerializableData != null) {
      optionalNullableSerializable = nestedSerializableFactory.fromAppFunctionData(optionalNullableSerializableData)
    }
    val optionalNonNullIntArray = appFunctionDataWithSpec.getIntArray("optionalNonNullIntArray") ?: intArrayOf()
    val optionalNullableIntArray = appFunctionDataWithSpec.getIntArray("optionalNullableIntArray")
    val optionalNonNullLongArray = appFunctionDataWithSpec.getLongArray("optionalNonNullLongArray") ?: longArrayOf()
    val optionalNullableLongArray = appFunctionDataWithSpec.getLongArray("optionalNullableLongArray")
    val optionalNonNullBooleanArray = appFunctionDataWithSpec.getBooleanArray("optionalNonNullBooleanArray") ?: booleanArrayOf()
    val optionalNullableBooleanArray = appFunctionDataWithSpec.getBooleanArray("optionalNullableBooleanArray")
    val optionalNonNullFloatArray = appFunctionDataWithSpec.getFloatArray("optionalNonNullFloatArray") ?: floatArrayOf()
    val optionalNullableFloatArray = appFunctionDataWithSpec.getFloatArray("optionalNullableFloatArray")
    val optionalNonNullDoubleArray = appFunctionDataWithSpec.getDoubleArray("optionalNonNullDoubleArray") ?: doubleArrayOf()
    val optionalNullableDoubleArray = appFunctionDataWithSpec.getDoubleArray("optionalNullableDoubleArray")
    val optionalNonNullByteArray = appFunctionDataWithSpec.getByteArrayOrNull("optionalNonNullByteArray") ?: byteArrayOf()
    val optionalNullableByteArray = appFunctionDataWithSpec.getByteArrayOrNull("optionalNullableByteArray")
    val optionalNonNullListString = appFunctionDataWithSpec.getStringList("optionalNonNullListString") ?: emptyList()
    val optionalNullableListString = appFunctionDataWithSpec.getStringList("optionalNullableListString")
    val optionalNonNullSerializableListData = appFunctionDataWithSpec.getAppFunctionDataList("optionalNonNullSerializableList") ?: emptyList()
    val optionalNonNullSerializableList = optionalNonNullSerializableListData.map { data ->
      nestedSerializableFactory.fromAppFunctionData(data)
    }
    val optionalNullableSerializableListData = appFunctionDataWithSpec.getAppFunctionDataList("optionalNullableSerializableList")
    val optionalNullableSerializableList = optionalNullableSerializableListData?.map { data ->
      nestedSerializableFactory.fromAppFunctionData(data)
    }

    val resultSerializableWithDefaultValue = SerializableWithDefaultValue(optionalNonNullInt, optionalNullableInt, optionalNonNullLong, optionalNullableLong, optionalNonNullBoolean, optionalNullableBoolean, optionalNonNullFloat, optionalNullableFloat, optionalNonNullDouble, optionalNullableDouble, optionalNullableString, optionalNullableSerializable, optionalNonNullIntArray, optionalNullableIntArray, optionalNonNullLongArray, optionalNullableLongArray, optionalNonNullBooleanArray, optionalNullableBooleanArray, optionalNonNullFloatArray, optionalNullableFloatArray, optionalNonNullDoubleArray, optionalNullableDoubleArray, optionalNonNullByteArray, optionalNullableByteArray, optionalNonNullListString, optionalNullableListString, optionalNonNullSerializableList, optionalNullableSerializableList)
    return resultSerializableWithDefaultValue
  }

  override fun toAppFunctionData(appFunctionSerializable: SerializableWithDefaultValue): AppFunctionData {
    val serializableWithDefaultValue_appFunctionSerializable = appFunctionSerializable
    val nestedSerializableFactory = `$NestedSerializableFactory`()

    val builder = getAppFunctionDataBuilder("com.testdata.SerializableWithDefaultValue")
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
