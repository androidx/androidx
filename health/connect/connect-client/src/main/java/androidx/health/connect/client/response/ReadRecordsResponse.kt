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
package androidx.health.connect.client.response

import androidx.health.connect.client.records.Record

/**
 * Response of reading a collection of records.
 *
 * @param T the record type
 * @property records a collection of records
 * @property pageToken an optional page token to use for
 * [androidx.health.connect.client.request.ReadRecordsRequest.pageToken] in the next request if more
 * records can be fetched; contains value `null` if no more pages.
 *
 * @see androidx.health.connect.client.HealthConnectClient.readRecords
 */
class ReadRecordsResponse<T : Record>
internal constructor(val records: List<T>, val pageToken: String?)
