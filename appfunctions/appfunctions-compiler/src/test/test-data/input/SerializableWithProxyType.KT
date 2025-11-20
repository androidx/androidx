package com.testdata

import android.net.Uri
import androidx.appfunctions.AppFunctionSerializable
import java.time.LocalDateTime

@AppFunctionSerializable
class SerializableWithProxyType(
    val localDateTime: LocalDateTime,
    val localDateTimeList: List<LocalDateTime>,
    val localDateTimeNullable: LocalDateTime?,
    val androidUriList: List<Uri>,
    val androidUriNullable: List<Uri>?
)
