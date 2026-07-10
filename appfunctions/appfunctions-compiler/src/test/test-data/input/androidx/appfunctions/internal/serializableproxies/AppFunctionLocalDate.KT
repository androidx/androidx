package androidx.appfunctions.internal.serializableproxies

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.appfunctions.AppFunctionSerializableProxy
import java.time.LocalDate

/**
 * A proxy class for [LocalDate] that can be used to serialize and deserialize [LocalDate]
 * objects across the App Functions boundary.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
@AppFunctionSerializableProxy(targetClass = LocalDate::class)
@RequiresApi(Build.VERSION_CODES.O)
public data class AppFunctionLocalDate(
    val year: Int,
    val month: Int,
    val dayOfMonth: Int
) {

    public fun toLocalDate(): LocalDate {
        return LocalDate.of(year, month, dayOfMonth)
    }

    public companion object {
        public fun fromLocalDate(localDate: LocalDate): AppFunctionLocalDate {
            return AppFunctionLocalDate(
                localDate.year,
                localDate.monthValue,
                localDate.dayOfMonth
            )
        }
    }
}
