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
package androidx.health.data.client.impl.converters.records

import androidx.health.data.client.metadata.Device
import androidx.health.data.client.metadata.Metadata
import androidx.health.data.client.records.InstantaneousRecord
import androidx.health.data.client.records.IntervalRecord
import androidx.health.platform.client.proto.DataProto

internal fun protoDataType(dataTypeName: String): DataProto.DataType =
    DataProto.DataType.newBuilder().setName(dataTypeName).build()

@SuppressWarnings("GoodTime", "NewApi") // Suppress GoodTime for serialize/de-serialize.
internal fun InstantaneousRecord.instantaneousProto(): DataProto.DataPoint.Builder {
    val builder =
        DataProto.DataPoint.newBuilder()
            .setMetadata(metadata)
            .setInstantTimeMillis(time.toEpochMilli())
    zoneOffset?.let { builder.setZoneOffsetSeconds(it.totalSeconds) }
    return builder
}

@SuppressWarnings("GoodTime", "NewApi") // Suppress GoodTime for serialize/de-serialize.
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

@SuppressWarnings("GoodTime", "NewApi") // Suppress GoodTime for serialize/de-serialize.
private fun DataProto.DataPoint.Builder.setMetadata(metadata: Metadata) = apply {
    metadata.uid?.let { setUid(it) }
    setDataOrigin(
        DataProto.DataOrigin.newBuilder().setApplicationId(metadata.dataOrigin.packageName).build()
    )
    metadata.lastModifiedTime.let { setUpdateTimeMillis(it.toEpochMilli()) }

    metadata.clientId?.let { setClientId(it) }
    metadata.clientVersion.let { setClientVersion(it) }
    metadata.device?.let { setDevice(it.toProto()) }
}

private fun Device.toProto(): DataProto.Device {
    val obj = this
    return DataProto.Device.newBuilder()
        .apply {
            obj.identifier?.let { setIdentifier(it) }
            obj.manufacturer?.let { setManufacturer(it) }
            obj.model?.let { setModel(it) }
            obj.type?.let { setType(it) }
        }
        .build()
}
