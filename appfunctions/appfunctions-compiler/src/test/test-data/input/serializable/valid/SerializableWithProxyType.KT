package com.testdata

import android.net.Uri
import androidx.appfunctions.AppFunctionSerializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@AppFunctionSerializable
class SerializableWithProxyType(
    val localDateTime: LocalDateTime,
    val localDateTimeList: List<LocalDateTime>,
    val localDateTimeNullable: LocalDateTime?,
    val androidUri: Uri,
    val androidUriList: List<Uri>,
    val androidUriNullable: Uri?,
    val androidUriListNullable: List<Uri>?,
    val localDate: LocalDate,
    val localDateList: List<LocalDate>,
    val localDateNullable: LocalDate?,
    val localTime: LocalTime,
    val localTimeList: List<LocalTime>,
    val localTimeNullable: LocalTime?
)


