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

package androidx.health.connect.client.devicedatasource

import androidx.health.connect.client.ExperimentalDeviceDataSourceApi
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device

/**
 * Device that acts as a source of health and fitness data.
 *
 * @property deviceDataOrigin origin of the device data, containing the synthetic package name
 * @property device [Device] metadata
 * @property deviceDataTypeSources set of [DeviceDataTypeSource]s provided by this device
 */
@ExperimentalDeviceDataSourceApi
class DeviceDataSource(
    val deviceDataOrigin: DataOrigin,
    val device: Device,
    val deviceDataTypeSources: Set<DeviceDataTypeSource>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeviceDataSource) return false

        if (deviceDataOrigin != other.deviceDataOrigin) return false
        if (device != other.device) return false
        if (deviceDataTypeSources != other.deviceDataTypeSources) return false

        return true
    }

    override fun hashCode(): Int {
        var result = deviceDataOrigin.hashCode()
        result = 31 * result + device.hashCode()
        result = 31 * result + deviceDataTypeSources.hashCode()
        return result
    }

    override fun toString(): String {
        return "DeviceDataSource(deviceDataOrigin=$deviceDataOrigin, device=$device, deviceDataTypeSources=$deviceDataTypeSources)"
    }
}
