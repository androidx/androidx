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

/**
 * Response to [androidx.health.connect.client.HealthConnectClient.getDeviceDataSources] containing
 * a list of [DeviceDataSource]s.
 *
 * @property deviceDataSources list of [DeviceDataSource]s
 * @see [androidx.health.connect.client.HealthConnectClient.getDeviceDataSources]
 */
@ExperimentalDeviceDataSourceApi
class GetDeviceDataSourcesResponse(val deviceDataSources: List<DeviceDataSource>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GetDeviceDataSourcesResponse) return false

        if (deviceDataSources != other.deviceDataSources) return false

        return true
    }

    override fun hashCode(): Int {
        return deviceDataSources.hashCode()
    }

    override fun toString(): String {
        return "GetDeviceDataSourcesResponse(deviceDataSources=$deviceDataSources)"
    }
}
