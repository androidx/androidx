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
import java.time.Instant

// TODO(b/413622177): Temporary workaround of supporting proxy before being able to apply KSP on
// appfunctions module.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class `$InstantFactory` : AppFunctionSerializableFactory<Instant> {
    override fun fromAppFunctionData(appFunctionData: AppFunctionData): Instant {
        val appFunctionDataWithSpec =
            getAppFunctionDataWithSpec(
                appFunctionData = appFunctionData,
                qualifiedName = "java.time.Instant",
            )
        val epochSecond = checkNotNull(appFunctionDataWithSpec.getLongOrNull("epochSecond"))
        val nanoAdjustment = checkNotNull(appFunctionDataWithSpec.getIntOrNull("nanoAdjustment"))

        val resultAppFunctionInstant = AppFunctionInstant(epochSecond, nanoAdjustment)
        return resultAppFunctionInstant.toInstant()
    }

    override fun toAppFunctionData(appFunctionSerializable: Instant): AppFunctionData {
        val appFunctionInstant_appFunctionSerializable =
            AppFunctionInstant.fromInstant(appFunctionSerializable)

        val builder = getAppFunctionDataBuilder("java.time.Instant")
        val epochSecond = appFunctionInstant_appFunctionSerializable.epochSecond
        builder.setLong("epochSecond", epochSecond)
        val nanoAdjustment = appFunctionInstant_appFunctionSerializable.nanoAdjustment
        builder.setInt("nanoAdjustment", nanoAdjustment)

        return builder.build()
    }
}
