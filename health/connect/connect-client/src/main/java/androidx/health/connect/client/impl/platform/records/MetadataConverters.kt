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

@file:RestrictTo(RestrictTo.Scope.LIBRARY)
@file:RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

package androidx.health.connect.client.impl.platform.records

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata

internal fun PlatformMetadata.toSdkMetadata(): Metadata {
    return Metadata(
        id = id,
        dataOrigin = dataOrigin.toSdkDataOrigin(),
        lastModifiedTime = lastModifiedTime,
        clientRecordId = clientRecordId,
        clientRecordVersion = clientRecordVersion,
        recordingMethod = recordingMethod.toSdkRecordingMethod(),
        device = device.toSdkDevice()
    )
}

internal fun PlatformDevice.toSdkDevice(): Device {
    @Suppress("WrongConstant") // Platform intdef and jetpack intdef match in value.
    return Device(manufacturer = manufacturer, model = model, type = type)
}

internal fun PlatformDataOrigin.toSdkDataOrigin(): DataOrigin {
    return DataOrigin(packageName)
}

internal fun Metadata.toPlatformMetadata(): PlatformMetadata {
    return PlatformMetadataBuilder()
        .apply {
            device?.toPlatformDevice()?.let { setDevice(it) }
            setLastModifiedTime(lastModifiedTime)
            setId(id)
            setDataOrigin(dataOrigin.toPlatformDataOrigin())
            setClientRecordId(clientRecordId)
            setClientRecordVersion(clientRecordVersion)
            setRecordingMethod(recordingMethod.toPlatformRecordingMethod())
        }
        .build()
}

internal fun DataOrigin.toPlatformDataOrigin(): PlatformDataOrigin {
    return PlatformDataOriginBuilder().apply { setPackageName(packageName) }.build()
}

internal fun Device.toPlatformDevice(): PlatformDevice {
    @Suppress("WrongConstant") // Platform intdef and jetpack intdef match in value.
    return PlatformDeviceBuilder()
        .apply {
            setType(type)
            manufacturer?.let { setManufacturer(it) }
            model?.let { setModel(it) }
        }
        .build()
}
