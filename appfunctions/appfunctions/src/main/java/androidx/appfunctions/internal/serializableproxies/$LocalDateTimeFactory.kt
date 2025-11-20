/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appfunctions.internal.serializableproxies

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import java.time.LocalDateTime

// TODO(b/413622177): Temporary workaround of supporting proxy before being able to apply KSP on
// appfunctions module.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class `$LocalDateTimeFactory` : AppFunctionSerializableFactory<LocalDateTime> {
    override fun fromAppFunctionData(appFunctionData: AppFunctionData): LocalDateTime {
        val appFunctionDataWithSpec =
            getAppFunctionDataWithSpec(
                appFunctionData = appFunctionData,
                qualifiedName = "java.time.LocalDateTime",
            )
        val year = checkNotNull(appFunctionDataWithSpec.getIntOrNull("year"))
        val month = checkNotNull(appFunctionDataWithSpec.getIntOrNull("month"))
        val dayOfMonth = checkNotNull(appFunctionDataWithSpec.getIntOrNull("dayOfMonth"))
        val hour = checkNotNull(appFunctionDataWithSpec.getIntOrNull("hour"))
        val minute = checkNotNull(appFunctionDataWithSpec.getIntOrNull("minute"))
        val second = checkNotNull(appFunctionDataWithSpec.getIntOrNull("second"))
        val nanoOfSecond = checkNotNull(appFunctionDataWithSpec.getIntOrNull("nanoOfSecond"))

        val resultAppFunctionLocalDateTime =
            AppFunctionLocalDateTime(year, month, dayOfMonth, hour, minute, second, nanoOfSecond)
        return resultAppFunctionLocalDateTime.toLocalDateTime()
    }

    override fun toAppFunctionData(appFunctionSerializable: LocalDateTime): AppFunctionData {
        val appFunctionLocalDateTime_appFunctionSerializable =
            AppFunctionLocalDateTime.fromLocalDateTime(appFunctionSerializable)

        val builder = getAppFunctionDataBuilder("java.time.LocalDateTime")
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
