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

import android.healthconnect.TimeRangeFilter as PlatformTimeRangeFilter
import android.healthconnect.datatypes.Record as PlatformRecord
import android.healthconnect.ChangeLogTokenRequest
import android.healthconnect.ReadRecordsRequestUsingFilters
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.health.connect.client.impl.platform.time.TimeSource
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

fun ReadRecordsRequest<out Record>.toPlatformReadRecordsRequestUsingFilters(
    timeSource: TimeSource
):
    ReadRecordsRequestUsingFilters<out PlatformRecord> {
    return ReadRecordsRequestUsingFilters
        .Builder(recordType.toPlatformRecordClass())
        .setTimeRangeFilter(timeRangeFilter.toPlatformTimeRangeFilter(timeSource))
        .apply {
            // TODO(b/262691771): revisit data origin filter once privacy decision is finalized
            dataOriginFilter.forEach { addDataOrigins(it.toPlatformDataOrigin()) }
        }
        .build()
}

fun TimeRangeFilter.toPlatformTimeRangeFilter(
    timeSource: TimeSource
): PlatformTimeRangeFilter {
    // TODO(b/262571990): pass nullable Instant start/end
    // TODO(b/262571990): pass nullable LocalDateTime start/end
    return PlatformTimeRangeFilter.Builder(startTime ?: Instant.EPOCH, endTime ?: timeSource.now)
        .build()
}

fun ChangesTokenRequest.toPlatformChangeLogTokenRequest(): ChangeLogTokenRequest {
    return ChangeLogTokenRequest.Builder()
        .apply {
            // TODO(b/262691771): revisit data origin filter once privacy decision is finalized
            dataOriginFilters.forEach { addDataOriginFilter(it.toPlatformDataOrigin()) }
            recordTypes.forEach { addRecordType(it.toPlatformRecordClass()) }
        }
        .build()
}