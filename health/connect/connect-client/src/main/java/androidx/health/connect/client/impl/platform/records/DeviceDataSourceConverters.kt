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

@file:RestrictTo(RestrictTo.Scope.LIBRARY)
@file:RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@file:OptIn(ExperimentalDeviceDataSourceApi::class)

package androidx.health.connect.client.impl.platform.records

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.health.connect.client.ExperimentalDeviceDataSourceApi
import androidx.health.connect.client.devicedatasource.DeviceDataSource
import androidx.health.connect.client.devicedatasource.DeviceDataSourceCapabilities
import androidx.health.connect.client.devicedatasource.DeviceDataTypeSource
import androidx.health.connect.client.devicedatasource.GetDeviceDataSourcesResponse
import androidx.health.connect.client.records.metadata.Device

@SuppressLint("NewApi")
internal fun PlatformDeviceDataTypeSource.toSdkDeviceDataTypeSource(): DeviceDataTypeSource? {
    if (symptomType != 0) {
        return null
    }
    val sdkRecordClass = dataType.toSdkRecordClass() ?: return null
    return DeviceDataTypeSource(
        dataType = sdkRecordClass,
        isAvailable = isAvailable,
        isUserEnabled = isUserEnabled,
    )
}

@SuppressLint("NewApi")
internal fun PlatformDeviceDataSource.toSdkDeviceDataSource(): DeviceDataSource {
    return DeviceDataSource(
        deviceDataOrigin = deviceDataOrigin.toSdkDataOrigin(),
        device =
            Device(
                type = device.type.toSdkDevice(),
                manufacturer = device.manufacturer,
                model = device.model,
            ),
        deviceDataTypeSources =
            deviceDataTypeSources.mapNotNull { it.toSdkDeviceDataTypeSource() }.toSet(),
    )
}

@SuppressLint("NewApi")
internal fun PlatformDeviceDataSourceCapabilities.toSdkDeviceDataSourceCapabilities():
    DeviceDataSourceCapabilities {
    return DeviceDataSourceCapabilities(
        recordTypes = recordTypes.mapNotNull { it.toSdkRecordClass() }.toSet()
    )
}

@SuppressLint("NewApi")
internal fun PlatformGetDeviceDataSourcesResponse.toSdkGetDeviceDataSourcesResponse():
    GetDeviceDataSourcesResponse {
    return GetDeviceDataSourcesResponse(
        deviceDataSources = deviceDataSources.map { it.toSdkDeviceDataSource() }
    )
}
