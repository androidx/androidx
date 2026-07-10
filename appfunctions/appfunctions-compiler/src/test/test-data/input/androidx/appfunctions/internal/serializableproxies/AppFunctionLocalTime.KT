package androidx.appfunctions.internal.serializableproxies

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.appfunctions.AppFunctionSerializableProxy
import java.time.LocalTime

/**
 * A proxy class for [LocalTime] that can be used to serialize and deserialize [LocalTime]
 * objects across the App Functions boundary.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
@AppFunctionSerializableProxy(targetClass = LocalTime::class)
@RequiresApi(Build.VERSION_CODES.O)
public data class AppFunctionLocalTime(
    val hour: Int,
    val minute: Int,
    val second: Int,
    val nanoOfSecond: Int
) {

    public fun toLocalTime(): LocalTime {
        return LocalTime.of(hour, minute, second, nanoOfSecond)
    }

    public companion object {
        public fun fromLocalTime(localTime: LocalTime): AppFunctionLocalTime {
            return AppFunctionLocalTime(
                localTime.hour,
                localTime.minute,
                localTime.second,
                localTime.nano
            )
        }
    }
}
