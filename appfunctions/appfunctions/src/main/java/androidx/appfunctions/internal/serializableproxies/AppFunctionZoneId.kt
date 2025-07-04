/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import androidx.annotation.RestrictTo.Scope
import androidx.appfunctions.AppFunctionSerializableProxy
import java.time.ZoneId

@RestrictTo(Scope.LIBRARY_GROUP)
@AppFunctionSerializableProxy(targetClass = ZoneId::class)
@RequiresApi(Build.VERSION_CODES.O)
public data class AppFunctionZoneId(val zoneID: String) {
    public fun toZoneId(): ZoneId {
        return ZoneId.of(zoneID)
    }

    public companion object {
        public fun fromZoneId(zoneId: ZoneId): AppFunctionZoneId {
            return AppFunctionZoneId(zoneId.id)
        }
    }
}
