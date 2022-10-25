/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata

internal fun Record.toPlatformRecord(): android.healthconnect.datatypes.Record {
    return when (this) {
        is StepsRecord ->
            android.healthconnect.datatypes.StepsRecord.Builder(
                    metadata.toPlatformMetadata(),
                    startTime,
                    endTime,
                    count
                )
                .apply {
                    startZoneOffset?.let { setStartZoneOffset(it) }
                    endZoneOffset?.let { setEndZoneOffset(it) }
                }
                .build()
        else -> throw IllegalArgumentException("Unsupported record $this")
    }
}

internal fun Metadata.toPlatformMetadata(): android.healthconnect.datatypes.Metadata {
    return android.healthconnect.datatypes.Metadata.Builder()
        .apply {
            device?.toPlatformDevice()?.let { setDevice(it) }
            setLastModifiedTime(lastModifiedTime)
            setId(id)
            setDataOrigin(dataOrigin.toPlatformDataOrigin())
            setClientRecordId(clientRecordId)
            setClientRecordVersion(clientRecordVersion)
        }
        .build()
}

internal fun Device.toPlatformDevice(): android.healthconnect.datatypes.Device {
    @Suppress("WrongConstant") // Platform intdef and jetpack intdef matches in value.
    return android.healthconnect.datatypes.Device.Builder()
        .apply {
            setType(type)
            manufacturer?.let { setManufacturer(it) }
            model?.let { setModel(it) }
        }
        .build()
}

internal fun DataOrigin.toPlatformDataOrigin(): android.healthconnect.datatypes.DataOrigin {
    return android.healthconnect.datatypes.DataOrigin.Builder()
        .apply { setPackageName(packageName) }
        .build()
}
