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
package androidx.health.connect.client.request

import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.time.TimeRangeFilter
import kotlin.reflect.KClass

/**
 * A convenience factory function for [ReadRecordsRequest] with `reified` record type [T].
 *
 * @see [ReadRecordsRequest] for more information.
 */
inline fun <reified T : Record> ReadRecordsRequest(
    timeRangeFilter: TimeRangeFilter,
    dataOriginFilter: Set<DataOrigin> = emptySet(),
    ascendingOrder: Boolean = true,
    pageSize: Int = 1000,
    pageToken: String? = null,
): ReadRecordsRequest<T> =
    ReadRecordsRequest(
        recordType = T::class,
        timeRangeFilter = timeRangeFilter,
        dataOriginFilter = dataOriginFilter,
        ascendingOrder = ascendingOrder,
        pageSize = pageSize,
        pageToken = pageToken,
    )

/**
 * Request object to read [Record]s in Android Health Platform determined by time range and other
 * filters.
 *
 * Returned collection will contain a
 * [androidx.health.data.client.response.ReadRecordsResponse.pageToken] if number of records exceeds
 * [pageSize]. Use this if you expect an unbound number of records within specified time ranges.
 * Stops at any time once desired amount of records are processed.
 *
 * @param T type of [Record], such as `Steps`.
 * @param recordType Which type of [Record] to read, such as `Steps::class`.
 * @param timeRangeFilter The [TimeRangeFilter] to read from.
 * @param dataOriginFilter List of [DataOrigin] to read from, or empty for no filter.
 * @param ascendingOrder Whether the [Record] should be returned in ascending or descending order by
 * time. Default is true for ascending.
 * @param pageSize Maximum number of [Record] within one page. If there's more data remaining (and
 * the next page should be read), the response will contain a [pageToken] to be used in the
 * subsequent read request. Must be positive, default to 1000.
 * @param pageToken Continuation token to access the next page, returned in the response to the
 * previous page read request, or `null` for the initial request for the first page.
 *
 * @see androidx.health.connect.client.response.ReadRecordsResponse
 * @see androidx.health.connect.client.HealthConnectClient.readRecords
 */
public class ReadRecordsRequest<T : Record>(
    internal val recordType: KClass<T>,
    internal val timeRangeFilter: TimeRangeFilter,
    internal val dataOriginFilter: Set<DataOrigin> = emptySet(),
    internal val ascendingOrder: Boolean = true,
    internal val pageSize: Int = 1000,
    internal val pageToken: String? = null,
) {
    init {
        require(pageSize > 0) { "pageSize must be positive." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReadRecordsRequest<*>

        if (recordType != other.recordType) return false
        if (timeRangeFilter != other.timeRangeFilter) return false
        if (dataOriginFilter != other.dataOriginFilter) return false
        if (ascendingOrder != other.ascendingOrder) return false
        if (pageSize != other.pageSize) return false
        if (pageToken != other.pageToken) return false

        return true
    }

    override fun hashCode(): Int {
        var result = recordType.hashCode()
        result = 31 * result + timeRangeFilter.hashCode()
        result = 31 * result + dataOriginFilter.hashCode()
        result = 31 * result + ascendingOrder.hashCode()
        result = 31 * result + pageSize
        result = 31 * result + (pageToken?.hashCode() ?: 0)
        return result
    }
}
