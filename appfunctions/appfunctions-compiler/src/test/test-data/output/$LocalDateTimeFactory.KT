package androidx.appfunctions.`internal`.serializableproxies

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import java.time.LocalDateTime
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$LocalDateTimeFactory` : AppFunctionSerializableFactory<LocalDateTime> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): LocalDateTime {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "androidx.appfunctions.internal.serializableproxies.AppFunctionLocalDateTime")

    val year = checkNotNull(appFunctionDataWithSpec.getIntOrNull("year"))
    val month = checkNotNull(appFunctionDataWithSpec.getIntOrNull("month"))
    val dayOfMonth = checkNotNull(appFunctionDataWithSpec.getIntOrNull("dayOfMonth"))
    val hour = checkNotNull(appFunctionDataWithSpec.getIntOrNull("hour"))
    val minute = checkNotNull(appFunctionDataWithSpec.getIntOrNull("minute"))
    val second = checkNotNull(appFunctionDataWithSpec.getIntOrNull("second"))
    val nanoOfSecond = checkNotNull(appFunctionDataWithSpec.getIntOrNull("nanoOfSecond"))

    val resultAppFunctionLocalDateTime = AppFunctionLocalDateTime(year, month, dayOfMonth, hour, minute, second, nanoOfSecond)
    return resultAppFunctionLocalDateTime.toLocalDateTime()
  }

  override fun toAppFunctionData(appFunctionSerializable: LocalDateTime): AppFunctionData {
    val appFunctionLocalDateTime_appFunctionSerializable = AppFunctionLocalDateTime.fromLocalDateTime(appFunctionSerializable)

    val builder = getAppFunctionDataBuilder("androidx.appfunctions.internal.serializableproxies.AppFunctionLocalDateTime")
    val year = appFunctionLocalDateTime_appFunctionSerializable.year
    builder.setInt("year", year)
    val month = appFunctionLocalDateTime_appFunctionSerializable.month
    builder.setInt("month", month)
    val dayOfMonth = appFunctionLocalDateTime_appFunctionSerializable.dayOfMonth
    builder.setInt("dayOfMonth", dayOfMonth)
    val hour = appFunctionLocalDateTime_appFunctionSerializable.hour
    builder.setInt("hour", hour)
    val minute = appFunctionLocalDateTime_appFunctionSerializable.minute
    builder.setInt("minute", minute)
    val second = appFunctionLocalDateTime_appFunctionSerializable.second
    builder.setInt("second", second)
    val nanoOfSecond = appFunctionLocalDateTime_appFunctionSerializable.nanoOfSecond
    builder.setInt("nanoOfSecond", nanoOfSecond)

    return builder.build()
  }
}
