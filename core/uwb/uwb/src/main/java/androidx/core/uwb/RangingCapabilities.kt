/*
 * Copyright (C) 2022 The Android Open Source Project
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
 * Describes UWB ranging capabilities for the current device.
 * @property isDistanceSupported - Whether distance ranging is supported
 * @property isAzimuthalAngleSupported - Whether azimuthal angle of arrival is supported
 * @property isElevationAngleSupported - Whether elevation angle of arrival is supported
 * @property minRangingInterval - Minimum ranging interval
 * @property supportedChannels - Array of supported channels
 * @property supportedConfigIds - Array of supported config ids
 **/
class RangingCapabilities(
    val isDistanceSupported: Boolean,
    val isAzimuthalAngleSupported: Boolean,
    val isElevationAngleSupported: Boolean,
    val minRangingInterval: Int,
    val supportedChannels: Set<Int>,
    val supportedConfigIds: Set<Int>
)
