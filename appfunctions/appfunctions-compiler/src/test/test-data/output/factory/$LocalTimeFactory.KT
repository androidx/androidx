package androidx.appfunctions.`internal`.serializableproxies

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import java.time.LocalTime
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$LocalTimeFactory` : AppFunctionSerializableFactory<LocalTime> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): LocalTime {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "androidx.appfunctions.internal.serializableproxies.AppFunctionLocalTime")

    val hour = checkNotNull(appFunctionDataWithSpec.getIntOrNull("hour"))
    val minute = checkNotNull(appFunctionDataWithSpec.getIntOrNull("minute"))
    val second = checkNotNull(appFunctionDataWithSpec.getIntOrNull("second"))
    val nanoOfSecond = checkNotNull(appFunctionDataWithSpec.getIntOrNull("nanoOfSecond"))

    val resultAppFunctionLocalTime = AppFunctionLocalTime(hour, minute, second, nanoOfSecond)
    return resultAppFunctionLocalTime.toLocalTime()
  }

  override fun toAppFunctionData(appFunctionSerializable: LocalTime): AppFunctionData {
    val appFunctionLocalTime_appFunctionSerializable = AppFunctionLocalTime.fromLocalTime(appFunctionSerializable)

    val builder = getAppFunctionDataBuilder("androidx.appfunctions.internal.serializableproxies.AppFunctionLocalTime")
    val hour = appFunctionLocalTime_appFunctionSerializable.hour
    builder.setInt("hour", hour)
    val minute = appFunctionLocalTime_appFunctionSerializable.minute
    builder.setInt("minute", minute)
    val second = appFunctionLocalTime_appFunctionSerializable.second
    builder.setInt("second", second)
    val nanoOfSecond = appFunctionLocalTime_appFunctionSerializable.nanoOfSecond
    builder.setInt("nanoOfSecond", nanoOfSecond)

    return builder.build()
  }
}
