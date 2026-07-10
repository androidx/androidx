/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.projected

import androidx.annotation.IntRange

/**
 * Information about the battery state of the projected device.
 *
 * @property isCharging true if the battery is currently charging, false otherwise
 * @property batteryLevel the current battery level, from 0 to 100 inclusive
 */
public class BatteryState
internal constructor(
    @get:JvmName("isCharging") public val isCharging: Boolean,
    @param:IntRange(from = 0, to = 100) public val batteryLevel: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BatteryState) return false
        return isCharging == other.isCharging && batteryLevel == other.batteryLevel
    }

    override fun hashCode(): Int {
        var result = isCharging.hashCode()
        result = 31 * result + batteryLevel
        return result
    }
}
