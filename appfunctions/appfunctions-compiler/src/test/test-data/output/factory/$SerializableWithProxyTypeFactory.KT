package com.testdata

import android.net.Uri
import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import androidx.appfunctions.`internal`.serializableproxies.`$LocalDateFactory`
import androidx.appfunctions.`internal`.serializableproxies.`$LocalDateTimeFactory`
import androidx.appfunctions.`internal`.serializableproxies.`$LocalTimeFactory`
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SerializableWithProxyTypeFactory` : AppFunctionSerializableFactory<SerializableWithProxyType> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): SerializableWithProxyType {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.SerializableWithProxyType")
    val appFunctionLocalDateTimeFactory = `$LocalDateTimeFactory`()
    val appFunctionLocalDateFactory = `$LocalDateFactory`()
    val appFunctionLocalTimeFactory = `$LocalTimeFactory`()

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
    val androidUriString = checkNotNull(appFunctionDataWithSpec.getStringOrNull("androidUri"))
    val androidUri = Uri.parse(androidUriString)
    val androidUriListStringList = checkNotNull(appFunctionDataWithSpec.getStringList("androidUriList"))
    val androidUriList = androidUriListStringList.map { uriString -> Uri.parse(uriString) }
    val androidUriNullableString = appFunctionDataWithSpec.getStringOrNull("androidUriNullable")
    val androidUriNullable = if (androidUriNullableString != null) Uri.parse(androidUriNullableString) else null
    val androidUriListNullableStringList = appFunctionDataWithSpec.getStringList("androidUriListNullable")
    val androidUriListNullable = androidUriListNullableStringList?.map { uriString -> Uri.parse(uriString) }
    val localDateData = checkNotNull(appFunctionDataWithSpec.getAppFunctionData("localDate"))
    val localDate = appFunctionLocalDateFactory.fromAppFunctionData(localDateData)
    val localDateListData = checkNotNull(appFunctionDataWithSpec.getAppFunctionDataList("localDateList"))
    val localDateList = localDateListData.map { data ->
      appFunctionLocalDateFactory.fromAppFunctionData(data)
    }
    val localDateNullableData = appFunctionDataWithSpec.getAppFunctionData("localDateNullable")
    var localDateNullable: LocalDate? = null
    if (localDateNullableData != null) {
      localDateNullable = appFunctionLocalDateFactory.fromAppFunctionData(localDateNullableData)
    }
    val localTimeData = checkNotNull(appFunctionDataWithSpec.getAppFunctionData("localTime"))
    val localTime = appFunctionLocalTimeFactory.fromAppFunctionData(localTimeData)
    val localTimeListData = checkNotNull(appFunctionDataWithSpec.getAppFunctionDataList("localTimeList"))
    val localTimeList = localTimeListData.map { data ->
      appFunctionLocalTimeFactory.fromAppFunctionData(data)
    }
    val localTimeNullableData = appFunctionDataWithSpec.getAppFunctionData("localTimeNullable")
    var localTimeNullable: LocalTime? = null
    if (localTimeNullableData != null) {
      localTimeNullable = appFunctionLocalTimeFactory.fromAppFunctionData(localTimeNullableData)
    }

    val resultSerializableWithProxyType = SerializableWithProxyType(localDateTime, localDateTimeList, localDateTimeNullable, androidUri, androidUriList, androidUriNullable, androidUriListNullable, localDate, localDateList, localDateNullable, localTime, localTimeList, localTimeNullable)
    return resultSerializableWithProxyType
  }

  override fun toAppFunctionData(appFunctionSerializable: SerializableWithProxyType): AppFunctionData {
    val serializableWithProxyType_appFunctionSerializable = appFunctionSerializable
    val appFunctionLocalDateTimeFactory = `$LocalDateTimeFactory`()
    val appFunctionLocalDateFactory = `$LocalDateFactory`()
    val appFunctionLocalTimeFactory = `$LocalTimeFactory`()

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
    val androidUri = serializableWithProxyType_appFunctionSerializable.androidUri
    builder.setString("androidUri", androidUri.toString())
    val androidUriList = serializableWithProxyType_appFunctionSerializable.androidUriList
    builder.setStringList("androidUriList", androidUriList.map { uri -> uri.toString() })
    val androidUriNullable = serializableWithProxyType_appFunctionSerializable.androidUriNullable
    if (androidUriNullable != null) {
      builder.setString("androidUriNullable", androidUriNullable.toString())
    }
    val androidUriListNullable = serializableWithProxyType_appFunctionSerializable.androidUriListNullable
    if (androidUriListNullable != null) {
      builder.setStringList("androidUriListNullable", androidUriListNullable.map { uri -> uri.toString() })
    }
    val localDate = serializableWithProxyType_appFunctionSerializable.localDate
    builder.setAppFunctionData("localDate", appFunctionLocalDateFactory.toAppFunctionData(localDate))
    val localDateList = serializableWithProxyType_appFunctionSerializable.localDateList
    builder.setAppFunctionDataList("localDateList", localDateList.map{ appFunctionLocalDate ->
      appFunctionLocalDateFactory.toAppFunctionData(appFunctionLocalDate)
    })
    val localDateNullable = serializableWithProxyType_appFunctionSerializable.localDateNullable
    if (localDateNullable != null) {
      builder.setAppFunctionData("localDateNullable", appFunctionLocalDateFactory.toAppFunctionData(localDateNullable))
    }
    val localTime = serializableWithProxyType_appFunctionSerializable.localTime
    builder.setAppFunctionData("localTime", appFunctionLocalTimeFactory.toAppFunctionData(localTime))
    val localTimeList = serializableWithProxyType_appFunctionSerializable.localTimeList
    builder.setAppFunctionDataList("localTimeList", localTimeList.map{ appFunctionLocalTime ->
      appFunctionLocalTimeFactory.toAppFunctionData(appFunctionLocalTime)
    })
    val localTimeNullable = serializableWithProxyType_appFunctionSerializable.localTimeNullable
    if (localTimeNullable != null) {
      builder.setAppFunctionData("localTimeNullable", appFunctionLocalTimeFactory.toAppFunctionData(localTimeNullable))
    }

    return builder.build()
  }
}
