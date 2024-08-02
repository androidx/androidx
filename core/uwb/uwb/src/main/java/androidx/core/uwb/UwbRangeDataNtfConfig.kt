/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.uwb

/**
 * Represents the configurable range data notification reports for a UWB session.
 *
 * @property configType the config type of the range data notification.
 * @property ntfProximityNearCm the proximity near distance in centimeters.
 * @property ntfProximityFarCm the proximity far distance in centimeters.
 */
public class UwbRangeDataNtfConfig(
    public val configType: Int,
    public val ntfProximityNearCm: Int,
    public val ntfProximityFarCm: Int
) {
    override fun hashCode(): Int {
        var result = configType
        result = 31 * result + ntfProximityNearCm
        result = 31 * result + ntfProximityFarCm
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UwbRangeDataNtfConfig

        if (configType != other.configType) return false
        if (ntfProximityNearCm != other.ntfProximityNearCm) return false
        if (ntfProximityFarCm != other.ntfProximityFarCm) return false

        return true
    }
}
