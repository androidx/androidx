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
import java.time.LocalDate
import javax.`annotation`.processing.Generated

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$LocalDateFactory` : AppFunctionSerializableFactory<LocalDate> {
    override fun fromAppFunctionData(appFunctionData: AppFunctionData): LocalDate {
        val appFunctionDataWithSpec =
            getAppFunctionDataWithSpec(
                appFunctionData = appFunctionData,
                qualifiedName =
                    "androidx.appfunctions.internal.serializableproxies.AppFunctionLocalDate",
            )

        val year = checkNotNull(appFunctionDataWithSpec.getIntOrNull("year"))
        val month = checkNotNull(appFunctionDataWithSpec.getIntOrNull("month"))
        val dayOfMonth = checkNotNull(appFunctionDataWithSpec.getIntOrNull("dayOfMonth"))

        val resultAppFunctionLocalDate = AppFunctionLocalDate(year, month, dayOfMonth)
        return resultAppFunctionLocalDate.toLocalDate()
    }

    override fun toAppFunctionData(appFunctionSerializable: LocalDate): AppFunctionData {
        val appFunctionLocalDate_appFunctionSerializable =
            AppFunctionLocalDate.fromLocalDate(appFunctionSerializable)

        val builder =
            getAppFunctionDataBuilder(
                "androidx.appfunctions.internal.serializableproxies.AppFunctionLocalDate"
            )
        val year = appFunctionLocalDate_appFunctionSerializable.year
        builder.setInt("year", year)
        val month = appFunctionLocalDate_appFunctionSerializable.month
        builder.setInt("month", month)
        val dayOfMonth = appFunctionLocalDate_appFunctionSerializable.dayOfMonth
        builder.setInt("dayOfMonth", dayOfMonth)

        return builder.build()
    }
}
