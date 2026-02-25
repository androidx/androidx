package com.testdata

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.appfunctions.AppFunctionSerializableProxy
import java.time.LocalDateTime

/**
 * A proxy class for [LocalDateTime] that can be used to serialize and deserialize [LocalDateTime]
 * objects across the App Functions boundary.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
@AppFunctionSerializableProxy(targetClass = LocalDateTime::class)
public data class AppFunctionLocalDateTime(
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val nanoOfSecond: Int
) {
    @RequiresApi(Build.VERSION_CODES.O)
    public fun toLocalDateTime(): LocalDateTime {
        return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond)
    }

    public companion object {}
}
