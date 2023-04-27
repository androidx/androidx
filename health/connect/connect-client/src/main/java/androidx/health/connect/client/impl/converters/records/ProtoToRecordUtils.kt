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
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord.Companion.STAGE_TYPE_STRING_TO_INT_MAP
import androidx.health.connect.client.records.ExerciseLap
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSegment.Companion.EXERCISE_SEGMENT_TYPE_UNKNOWN
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.meters
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

/** Maps a string enum field to public API integers. */
internal fun DataPointOrBuilder.mapEnum(
    key: String,
    stringToIntMap: Map<String, Int>,
    default: Int
): Int {
    val value = getEnum(key) ?: return default
    return stringToIntMap.getOrDefault(value, default)
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
            id = if (hasUid()) uid else Metadata.EMPTY_ID,
            dataOrigin = DataOrigin(dataOrigin.applicationId),
            lastModifiedTime = Instant.ofEpochMilli(updateTimeMillis),
            clientRecordId = if (hasClientId()) clientId else null,
            clientRecordVersion = clientVersion,
            device = if (hasDevice()) device.toDevice() else null,
            recordingMethod = recordingMethod
        )

internal fun DataProto.Device.toDevice(): Device {
    return Device(
        manufacturer = if (hasManufacturer()) manufacturer else null,
        model = if (hasModel()) model else null,
        type = DEVICE_TYPE_STRING_TO_INT_MAP.getOrDefault(type, Device.TYPE_UNKNOWN)
    )
}

internal fun DataProto.DataPoint.SubTypeDataList.toStageList(): List<SleepSessionRecord.Stage> {
    return valuesList.map {
        SleepSessionRecord.Stage(
            startTime = Instant.ofEpochMilli(it.startTimeMillis),
            endTime = Instant.ofEpochMilli(it.endTimeMillis),
            stage = STAGE_TYPE_STRING_TO_INT_MAP[it.valuesMap["stage"]?.enumVal]
                ?: SleepSessionRecord.STAGE_TYPE_UNKNOWN
        )
    }
}
internal fun DataProto.DataPoint.SubTypeDataList.toSegmentList(): List<ExerciseSegment> {
    return valuesList.map {
        ExerciseSegment(
            startTime = Instant.ofEpochMilli(it.startTimeMillis),
            endTime = Instant.ofEpochMilli(it.endTimeMillis),
            segmentType = (it.valuesMap["type"]?.longVal
                ?: EXERCISE_SEGMENT_TYPE_UNKNOWN).toInt(),
            repetitions = it.valuesMap["reps"]?.longVal?.toInt() ?: 0
        )
    }
}

internal fun DataProto.DataPoint.SubTypeDataList.toLapList(): List<ExerciseLap> {
    return valuesList.map {
        ExerciseLap(
            startTime = Instant.ofEpochMilli(it.startTimeMillis),
            endTime = Instant.ofEpochMilli(it.endTimeMillis),
            length = it.valuesMap["length"]?.doubleVal?.meters,
        )
    }
}

internal fun DataProto.DataPoint.SubTypeDataList.toLocationList(): List<ExerciseRoute.Location> {
    return valuesList.map {
        ExerciseRoute.Location(
            time = Instant.ofEpochMilli(it.startTimeMillis),
            latitude = it.valuesMap["latitude"]?.doubleVal ?: 0.0,
            longitude = it.valuesMap["longitude"]?.doubleVal ?: 0.0,
            altitude = it.valuesMap["altitude"]?.doubleVal?.meters,
            horizontalAccuracy = it.valuesMap["horizontal_accuracy"]?.doubleVal?.meters,
            verticalAccuracy = it.valuesMap["vertical_accuracy"]?.doubleVal?.meters,
        )
    }
}
