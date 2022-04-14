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
package androidx.health.data.client.impl.converters.time

import androidx.health.data.client.time.TimeRangeFilter
import androidx.health.platform.client.proto.TimeProto

/** Converts public API object into internal proto for ipc. */
@SuppressWarnings("NewApi") // TODO(b/208786847) figure a way to suppress false positive NewApi
fun TimeRangeFilter.toProto(): TimeProto.TimeSpec {
    val obj = this
    return TimeProto.TimeSpec.newBuilder()
        .apply {
            obj.startTime?.let { setStartTimeEpochMs(it.toEpochMilli()) }
            obj.endTime?.let { setEndTimeEpochMs(it.toEpochMilli()) }
            obj.localStartTime?.let { setStartLocalDateTime(it.toString()) }
            obj.localEndTime?.let { setEndLocalDateTime(it.toString()) }
        }
        .build()
}
