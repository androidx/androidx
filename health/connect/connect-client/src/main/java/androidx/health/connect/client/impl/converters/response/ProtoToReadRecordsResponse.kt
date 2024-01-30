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

package androidx.health.connect.client.impl.converters.response

import androidx.annotation.RestrictTo
import androidx.health.connect.client.impl.converters.records.toRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.platform.client.proto.ResponseProto

/**
 * Converts public API object into internal proto for ipc.
 */
@Suppress("UNCHECKED_CAST") // Safe to cast as the type should match
fun <T : Record> toReadRecordsResponse(
    proto: ResponseProto.ReadDataRangeResponse
): ReadRecordsResponse<T> =
    ReadRecordsResponse(
        records = proto.dataPointList.map { toRecord(it) as T },
        pageToken = proto.pageToken
    )
