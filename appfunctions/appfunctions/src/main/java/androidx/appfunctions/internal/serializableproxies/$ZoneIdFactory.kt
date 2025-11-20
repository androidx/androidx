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
import java.time.ZoneId

// TODO(b/413622177): Temporary workaround of supporting proxy before being able to apply KSP on
// appfunctions module.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class `$ZoneIdFactory` : AppFunctionSerializableFactory<ZoneId> {
    override fun fromAppFunctionData(appFunctionData: AppFunctionData): ZoneId {
        val appFunctionDataWithSpec =
            getAppFunctionDataWithSpec(
                appFunctionData = appFunctionData,
                qualifiedName = "java.time.ZoneId",
            )
        val zoneID = checkNotNull(appFunctionDataWithSpec.getStringOrNull("zoneID"))

        val resultAppFunctionZoneId = AppFunctionZoneId(zoneID)
        return resultAppFunctionZoneId.toZoneId()
    }

    override fun toAppFunctionData(appFunctionSerializable: ZoneId): AppFunctionData {
        val appFunctionZoneId_appFunctionSerializable =
            AppFunctionZoneId.fromZoneId(appFunctionSerializable)

        val builder = getAppFunctionDataBuilder("java.time.ZoneId")
        val zoneID = appFunctionZoneId_appFunctionSerializable.zoneID
        builder.setString("zoneID", zoneID)

        return builder.build()
    }
}
