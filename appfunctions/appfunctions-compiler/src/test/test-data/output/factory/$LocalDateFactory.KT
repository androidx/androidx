package androidx.appfunctions.`internal`.serializableproxies

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import java.time.LocalDate
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$LocalDateFactory` : AppFunctionSerializableFactory<LocalDate> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): LocalDate {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "androidx.appfunctions.internal.serializableproxies.AppFunctionLocalDate")

    val year = checkNotNull(appFunctionDataWithSpec.getIntOrNull("year"))
    val month = checkNotNull(appFunctionDataWithSpec.getIntOrNull("month"))
    val dayOfMonth = checkNotNull(appFunctionDataWithSpec.getIntOrNull("dayOfMonth"))

    val resultAppFunctionLocalDate = AppFunctionLocalDate(year, month, dayOfMonth)
    return resultAppFunctionLocalDate.toLocalDate()
  }

  override fun toAppFunctionData(appFunctionSerializable: LocalDate): AppFunctionData {
    val appFunctionLocalDate_appFunctionSerializable = AppFunctionLocalDate.fromLocalDate(appFunctionSerializable)

    val builder = getAppFunctionDataBuilder("androidx.appfunctions.internal.serializableproxies.AppFunctionLocalDate")
    val year = appFunctionLocalDate_appFunctionSerializable.year
    builder.setInt("year", year)
    val month = appFunctionLocalDate_appFunctionSerializable.month
    builder.setInt("month", month)
    val dayOfMonth = appFunctionLocalDate_appFunctionSerializable.dayOfMonth
    builder.setInt("dayOfMonth", dayOfMonth)

    return builder.build()
  }
}
