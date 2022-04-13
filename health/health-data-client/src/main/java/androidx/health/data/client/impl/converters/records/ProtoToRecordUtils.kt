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

import androidx.health.data.client.metadata.DataOrigin
import androidx.health.data.client.metadata.Device
import androidx.health.data.client.metadata.Metadata
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.proto.DataProto.DataPointOrBuilder
import androidx.health.platform.client.proto.DataProto.SeriesValueOrBuilder
import java.time.Instant
import java.time.ZoneOffset

/** Internal helper functions to convert proto to records. */
@get:SuppressWarnings("GoodTime") // Safe to use for deserialization
internal val DataProto.DataPoint.startTime: Instant
    get() = Instant.ofEpochMilli(startTimeMillis)

@get:SuppressWarnings("GoodTime") // Safe to use for deserialization
internal val DataProto.DataPoint.endTime: Instant
    get() = Instant.ofEpochMilli(endTimeMillis)

@get:SuppressWarnings("GoodTime") // Safe to use for deserialization
internal val DataProto.DataPoint.time: Instant
    get() = Instant.ofEpochMilli(instantTimeMillis)

@get:SuppressWarnings("GoodTime") // Safe to use for deserialization
internal val DataProto.DataPoint.startZoneOffset: ZoneOffset?
    get() =
        if (hasStartZoneOffsetSeconds()) ZoneOffset.ofTotalSeconds(startZoneOffsetSeconds) else null

@get:SuppressWarnings("GoodTime") // Safe to use for deserialization
internal val DataProto.DataPoint.endZoneOffset: ZoneOffset?
    get() = if (hasEndZoneOffsetSeconds()) ZoneOffset.ofTotalSeconds(endZoneOffsetSeconds) else null

@get:SuppressWarnings("GoodTime") // HealthDataClientImplSafe to use for deserialization
internal val DataProto.DataPoint.zoneOffset: ZoneOffset?
    get() = if (hasZoneOffsetSeconds()) ZoneOffset.ofTotalSeconds(zoneOffsetSeconds) else null

internal fun DataPointOrBuilder.getLong(key: String, defaultVal: Long = 0): Long =
    valuesMap[key]?.longVal ?: defaultVal

internal fun DataPointOrBuilder.getDouble(key: String, defaultVal: Double = 0.0): Double =
    valuesMap[key]?.doubleVal ?: defaultVal

internal fun DataPointOrBuilder.getString(key: String): String? = valuesMap[key]?.stringVal

internal fun DataPointOrBuilder.getEnum(key: String): String? {
    return valuesMap[key]?.enumVal
}

internal fun SeriesValueOrBuilder.getLong(key: String, defaultVal: Long = 0): Long =
    valuesMap[key]?.longVal ?: defaultVal

internal fun SeriesValueOrBuilder.getDouble(key: String, defaultVal: Double = 0.0): Double =
    valuesMap[key]?.doubleVal ?: defaultVal

internal fun SeriesValueOrBuilder.getString(key: String): String? = valuesMap[key]?.stringVal

internal fun SeriesValueOrBuilder.getEnum(key: String): String? = valuesMap[key]?.enumVal

@get:SuppressWarnings("GoodTime") // Safe to use for deserialization
internal val DataProto.DataPoint.metadata: Metadata
    get() =
        Metadata(
            uid = if (hasUid()) uid else null,
            dataOrigin = DataOrigin(dataOrigin.applicationId),
            lastModifiedTime = Instant.ofEpochMilli(updateTimeMillis),
            clientId = if (hasClientId()) clientId else null,
            clientVersion = clientVersion,
            device = toDevice(device)
        )

private fun toDevice(proto: DataProto.Device): Device {
    return with(proto) {
        Device(
            identifier = if (hasIdentifier()) identifier else null,
            manufacturer = if (hasManufacturer()) manufacturer else null,
            model = if (hasModel()) model else null,
            type = if (hasType()) type else null
        )
    }
}
