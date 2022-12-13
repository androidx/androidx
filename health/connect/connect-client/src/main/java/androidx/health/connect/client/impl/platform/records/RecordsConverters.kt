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

import android.healthconnect.datatypes.DataOrigin as PlatformDataOrigin
import android.healthconnect.datatypes.Device as PlatformDevice
import android.healthconnect.datatypes.Metadata as PlatformMetadata
import android.healthconnect.datatypes.Record as PlatformRecord
import android.healthconnect.datatypes.StepsRecord as PlatformStepsRecord
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import kotlin.reflect.KClass

internal fun KClass<out Record>.toPlatformRecordClass():
    Class<out PlatformRecord> {
    return when (this) {
        StepsRecord::class -> PlatformStepsRecord::class.java
        else -> throw IllegalArgumentException("Unsupported record type $this")
    }
}

internal fun Record.toPlatformRecord(): PlatformRecord {
    return when (this) {
        is StepsRecord ->
            PlatformStepsRecord.Builder(
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

internal fun PlatformRecord.toSdkRecord(): Record {
    return when (this) {
        is PlatformStepsRecord ->
            StepsRecord(
                startTime,
                startZoneOffset,
                endTime,
                endZoneOffset,
                count,
                metadata.toSdkMetadata()
            )

        else -> throw IllegalArgumentException("Unsupported record $this")
    }
}

internal fun Metadata.toPlatformMetadata(): PlatformMetadata {
    return PlatformMetadata.Builder()
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

internal fun PlatformMetadata.toSdkMetadata(): Metadata {
    return Metadata(
        id,
        dataOrigin.toSdkDataOrigin(),
        lastModifiedTime,
        clientRecordId,
        clientRecordVersion,
        device.toSdkDevice()
    )
}

internal fun Device.toPlatformDevice(): PlatformDevice {
    @Suppress("WrongConstant") // Platform intdef and jetpack intdef match in value.
    return PlatformDevice.Builder()
        .apply {
            setType(type)
            manufacturer?.let { setManufacturer(it) }
            model?.let { setModel(it) }
        }
        .build()
}

internal fun PlatformDevice.toSdkDevice(): Device {
    @Suppress("WrongConstant") // Platform intdef and jetpack intdef match in value.
    return Device(manufacturer, model, type)
}

internal fun DataOrigin.toPlatformDataOrigin(): PlatformDataOrigin {
    return PlatformDataOrigin.Builder()
        .apply { setPackageName(packageName) }
        .build()
}

internal fun PlatformDataOrigin.toSdkDataOrigin(): DataOrigin {
    return DataOrigin(packageName ?: "")
}
