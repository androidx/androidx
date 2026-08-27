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

package androidx.core.uwb

import androidx.core.uwb.RangingParameters.Companion.CONFIG_PROVISIONED_UNICAST_DS_TWR
import androidx.core.uwb.RangingParameters.Companion.CONFIG_PROVISIONED_UNICAST_DS_TWR_NO_AOA
import androidx.core.uwb.RangingParameters.Companion.CONFIG_UNICAST_DS_TWR
import androidx.core.uwb.RangingParameters.Companion.CONFIG_UNICAST_DS_TWR_NO_AOA

/**
 * A set of parameters for a sensor fusion session.
 *
 * @property rangingParameters The UWB ranging parameters to use.
 * @property dataStalenessThresholdMillis The timeout after which raw range measurements and
 *   estimates from the algorithm are considered stale.
 */
public class SensorFusionParameters
@JvmOverloads
constructor(
    public val rangingParameters: RangingParameters,
    public val dataStalenessThresholdMillis: Long = 1000,
) {
    init {
        require(dataStalenessThresholdMillis > 0) { "Data staleness threshold must be > 0" }
        require(
            rangingParameters.uwbConfigType == CONFIG_UNICAST_DS_TWR ||
                rangingParameters.uwbConfigType == CONFIG_UNICAST_DS_TWR_NO_AOA ||
                rangingParameters.uwbConfigType == CONFIG_PROVISIONED_UNICAST_DS_TWR ||
                rangingParameters.uwbConfigType == CONFIG_PROVISIONED_UNICAST_DS_TWR_NO_AOA
        ) {
            "Sensor fusion is only supported with unicast config types."
        }
        require(rangingParameters.peerDevices.size == 1) {
            "Sensor fusion is only supported for unicast sessions."
        }
    }
}
