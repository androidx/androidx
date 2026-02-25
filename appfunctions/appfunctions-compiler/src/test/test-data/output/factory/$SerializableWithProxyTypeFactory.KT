package com.testdata

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import androidx.appfunctions.`internal`.serializableproxies.`$LocalDateTimeFactory`
import androidx.appfunctions.`internal`.serializableproxies.`$UriFactory`
import java.time.LocalDateTime
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SerializableWithProxyTypeFactory` : AppFunctionSerializableFactory<SerializableWithProxyType> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): SerializableWithProxyType {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.SerializableWithProxyType")
    val appFunctionLocalDateTimeFactory = `$LocalDateTimeFactory`()
    val appFunctionUriFactory = `$UriFactory`()

    val localDateTimeData = checkNotNull(appFunctionDataWithSpec.getAppFunctionData("localDateTime"))
    val localDateTime = appFunctionLocalDateTimeFactory.fromAppFunctionData(localDateTimeData)
    val localDateTimeListData = checkNotNull(appFunctionDataWithSpec.getAppFunctionDataList("localDateTimeList"))
    val localDateTimeList = localDateTimeListData.map { data ->
      appFunctionLocalDateTimeFactory.fromAppFunctionData(data)
    }
    val localDateTimeNullableData = appFunctionDataWithSpec.getAppFunctionData("localDateTimeNullable")
    var localDateTimeNullable: LocalDateTime? = null
    if (localDateTimeNullableData != null) {
      localDateTimeNullable = appFunctionLocalDateTimeFactory.fromAppFunctionData(localDateTimeNullableData)
    }
    val androidUriListData = checkNotNull(appFunctionDataWithSpec.getAppFunctionDataList("androidUriList"))
    val androidUriList = androidUriListData.map { data ->
      appFunctionUriFactory.fromAppFunctionData(data)
    }
    val androidUriNullableData = appFunctionDataWithSpec.getAppFunctionDataList("androidUriNullable")
    val androidUriNullable = androidUriNullableData?.map { data ->
      appFunctionUriFactory.fromAppFunctionData(data)
    }

    val resultSerializableWithProxyType = SerializableWithProxyType(localDateTime, localDateTimeList, localDateTimeNullable, androidUriList, androidUriNullable)
    return resultSerializableWithProxyType
  }

  override fun toAppFunctionData(appFunctionSerializable: SerializableWithProxyType): AppFunctionData {
    val serializableWithProxyType_appFunctionSerializable = appFunctionSerializable
    val appFunctionLocalDateTimeFactory = `$LocalDateTimeFactory`()
    val appFunctionUriFactory = `$UriFactory`()

    val builder = getAppFunctionDataBuilder("com.testdata.SerializableWithProxyType")
    val localDateTime = serializableWithProxyType_appFunctionSerializable.localDateTime
    builder.setAppFunctionData("localDateTime", appFunctionLocalDateTimeFactory.toAppFunctionData(localDateTime))
    val localDateTimeList = serializableWithProxyType_appFunctionSerializable.localDateTimeList
    builder.setAppFunctionDataList("localDateTimeList", localDateTimeList.map{ appFunctionLocalDateTime ->
      appFunctionLocalDateTimeFactory.toAppFunctionData(appFunctionLocalDateTime)
    })
    val localDateTimeNullable = serializableWithProxyType_appFunctionSerializable.localDateTimeNullable
    if (localDateTimeNullable != null) {
      builder.setAppFunctionData("localDateTimeNullable", appFunctionLocalDateTimeFactory.toAppFunctionData(localDateTimeNullable))
    }
    val androidUriList = serializableWithProxyType_appFunctionSerializable.androidUriList
    builder.setAppFunctionDataList("androidUriList", androidUriList.map{ appFunctionUri ->
      appFunctionUriFactory.toAppFunctionData(appFunctionUri)
    })
    val androidUriNullable = serializableWithProxyType_appFunctionSerializable.androidUriNullable
    if (androidUriNullable != null) {
      builder.setAppFunctionDataList("androidUriNullable", androidUriNullable.map{ appFunctionUri ->
        appFunctionUriFactory.toAppFunctionData(appFunctionUri)
      })
    }

    return builder.build()
  }
}
