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
@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.health.connect.client.impl.converters.records

import androidx.annotation.RestrictTo
import androidx.health.connect.client.records.InstantaneousRecord
import androidx.health.connect.client.records.IntervalRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.DeviceTypes
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.platform.client.proto.DataProto
import java.time.Instant

internal fun protoDataType(dataTypeName: String): DataProto.DataType =
    DataProto.DataType.newBuilder().setName(dataTypeName).build()

@SuppressWarnings("GoodTime") // Suppress GoodTime for serialize/de-serialize.
internal fun InstantaneousRecord.instantaneousProto(): DataProto.DataPoint.Builder {
    val builder =
        DataProto.DataPoint.newBuilder()
            .setMetadata(metadata)
            .setInstantTimeMillis(time.toEpochMilli())
    zoneOffset?.let { builder.setZoneOffsetSeconds(it.totalSeconds) }
    return builder
}

@SuppressWarnings("GoodTime") // Suppress GoodTime for serialize/de-serialize.
internal fun IntervalRecord.intervalProto(): DataProto.DataPoint.Builder {
    val builder =
        DataProto.DataPoint.newBuilder()
            .setMetadata(metadata)
            .setStartTimeMillis(startTime.toEpochMilli())
            .setEndTimeMillis(endTime.toEpochMilli())
    startZoneOffset?.let { builder.setStartZoneOffsetSeconds(it.totalSeconds) }
    endZoneOffset?.let { builder.setEndZoneOffsetSeconds(it.totalSeconds) }
    return builder
}

@SuppressWarnings("GoodTime") // Suppress GoodTime for serialize/de-serialize.
private fun DataProto.DataPoint.Builder.setMetadata(metadata: Metadata) = apply {
    if (metadata.id != Metadata.EMPTY_ID) {
        uid = metadata.id
    }
    if (metadata.dataOrigin.packageName.isNotEmpty()) {
        dataOrigin =
            DataProto.DataOrigin.newBuilder()
                .setApplicationId(metadata.dataOrigin.packageName)
                .build()
    }

    if (metadata.lastModifiedTime.isAfter(Instant.EPOCH)) {
        updateTimeMillis = metadata.lastModifiedTime.toEpochMilli()
    }

    metadata.clientRecordId?.let { setClientId(it) }
    if (metadata.clientRecordVersion > 0) {
        clientVersion = metadata.clientRecordVersion
    }
    metadata.device?.let { setDevice(it.toProto()) }
    if (metadata.recordingMethod > 0) {
        recordingMethod = metadata.recordingMethod
    }
}

internal fun Device.toProto(): DataProto.Device {
    val obj = this
    return DataProto.Device.newBuilder()
        .apply {
            obj.manufacturer?.let { setManufacturer(it) }
            obj.model?.let { setModel(it) }
            setType(DEVICE_TYPE_INT_TO_STRING_MAP.getOrDefault(obj.type, DeviceTypes.UNKNOWN))
        }
        .build()
}

internal fun SleepSessionRecord.Stage.toProto(): DataProto.SubTypeDataValue {
    return DataProto.SubTypeDataValue.newBuilder()
        .setStartTimeMillis(startTime.toEpochMilli())
        .setEndTimeMillis(endTime.toEpochMilli())
        .apply {
            enumValFromInt(stage, SleepSessionRecord.STAGE_TYPE_INT_TO_STRING_MAP)?.let {
                putValues("stage", it)
            }
        }.build()
}