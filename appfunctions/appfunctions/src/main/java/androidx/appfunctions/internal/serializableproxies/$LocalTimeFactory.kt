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

package androidx.appfunctions.`internal`.serializableproxies

import androidx.`annotation`.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import java.time.LocalTime
import javax.`annotation`.processing.Generated

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$LocalTimeFactory` : AppFunctionSerializableFactory<LocalTime> {
    override fun fromAppFunctionData(appFunctionData: AppFunctionData): LocalTime {
        val appFunctionDataWithSpec =
            getAppFunctionDataWithSpec(
                appFunctionData = appFunctionData,
                qualifiedName =
                    "androidx.appfunctions.internal.serializableproxies.AppFunctionLocalTime",
            )

        val hour = checkNotNull(appFunctionDataWithSpec.getIntOrNull("hour"))
        val minute = checkNotNull(appFunctionDataWithSpec.getIntOrNull("minute"))
        val second = checkNotNull(appFunctionDataWithSpec.getIntOrNull("second"))
        val nanoOfSecond = checkNotNull(appFunctionDataWithSpec.getIntOrNull("nanoOfSecond"))

        val resultAppFunctionLocalTime = AppFunctionLocalTime(hour, minute, second, nanoOfSecond)
        return resultAppFunctionLocalTime.toLocalTime()
    }

    override fun toAppFunctionData(appFunctionSerializable: LocalTime): AppFunctionData {
        val appFunctionLocalTime_appFunctionSerializable =
            AppFunctionLocalTime.fromLocalTime(appFunctionSerializable)

        val builder =
            getAppFunctionDataBuilder(
                "androidx.appfunctions.internal.serializableproxies.AppFunctionLocalTime"
            )
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
