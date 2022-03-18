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
package androidx.health.data.client.request

import androidx.annotation.RestrictTo
import androidx.health.data.client.metadata.DataOrigin
import androidx.health.data.client.records.Record
import androidx.health.data.client.time.TimeRangeFilter
import kotlin.reflect.KClass

/**
 * Request object to read [Record]s in Android Health Platform determined by time range and other
 * filters.
 *
 * @property recordType Which type of [Record] to read, such as `Steps::class`.
 * @property timeRangeFilter The [TimeRangeFilter] to read from. If open-ended in any direction,
 * [limit] or [pageSize] must be set.
 * @property dataOriginFilter List of [DataOrigin] to read from, or empty for no filter.
 * @property ascendingOrder Whether the [Record] should be returned in ascending or descending order
 * by time. Default is true.
 * @property limit Maximum number of [Record] to read. Cannot be set together with [pageSize]. Must
 * be set if [timeRangeFilter] is open-ended in any direction.
 * @property pageSize Maximum number of [Record] within one page. If there's more data remaining
 * (and the next page should be read), the response will contain a [pageToken] to be used in the
 * subsequent read request. Cannot be set together with [limit].
 * @property pageToken Continuation token to access the next page, returned in the response to the
 * previous page read request. [pageSize] must be set together with [pageToken].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ReadRecordsRequest<T : Record>(
    val recordType: KClass<T>,
    val timeRangeFilter: TimeRangeFilter,
    val dataOriginFilter: List<DataOrigin> = emptyList(),
    val ascendingOrder: Boolean = true,
    val limit: Int = 0,
    val pageSize: Int = 0,
    val pageToken: String? = null
) {
    init {
        require(limit == 0 || pageSize == 0) { "pageSize and limit can't be used at the same time" }
        if (timeRangeFilter.isOpenEnded()) {
            require(limit > 0 || pageSize > 0) {
                "When timeRangeFilter is open-ended, either limit or pageSize must be set"
            }
        }
        if (pageToken != null) {
            require(pageSize > 0) { "pageToken must be set with pageSize" }
        }
    }

    internal fun hasLimit(): Boolean = limit > 0

    internal fun hasPageSize(): Boolean = pageSize > 0
}
