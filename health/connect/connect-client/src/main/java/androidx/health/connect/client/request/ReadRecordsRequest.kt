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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.connect.client.ExperimentalDeduplicationApi
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
 * [androidx.health.connect.client.response.ReadRecordsResponse.pageToken] if number of records
 * exceeds [pageSize]. Use this if you expect an unbound number of records within specified time
 * ranges. Stops at any time once desired amount of records are processed.
 *
 * @param T type of [Record], such as `Steps`.
 * @param recordType Which type of [Record] to read, such as `Steps::class`.
 * @param timeRangeFilter The [TimeRangeFilter] to read from.
 * @param dataOriginFilter List of [DataOrigin] to read from, or empty for no filter.
 * @param ascendingOrder Whether the [Record] should be returned in ascending or descending order by
 *   time. Default is true for ascending.
 * @param pageSize Maximum number of [Record] within one page. If there's more data remaining (and
 *   the next page should be read), the response will contain a [pageToken] to be used in the
 *   subsequent read request. Must be positive, default to 1000.
 * @param pageToken Continuation token to access the next page, returned in the response to the
 *   previous page read request, or `null` for the initial request for the first page.
 * @see androidx.health.connect.client.response.ReadRecordsResponse
 * @see androidx.health.connect.client.HealthConnectClient.readRecords
 */
public class ReadRecordsRequest<T : Record>
@RestrictTo(RestrictTo.Scope.LIBRARY)
@ExperimentalDeduplicationApi
constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val recordType: KClass<T>,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val timeRangeFilter: TimeRangeFilter,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val dataOriginFilter: Set<DataOrigin> = emptySet(),
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val ascendingOrder: Boolean = true,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val pageSize: Int = 1000,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val pageToken: String? = null,
    @DeduplicationStrategy
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val deduplicateStrategy: Int = DEDUPLICATION_STRATEGY_ENABLED_DEFAULT,
) {
    @OptIn(ExperimentalDeduplicationApi::class)
    constructor(
        recordType: KClass<T>,
        timeRangeFilter: TimeRangeFilter,
        dataOriginFilter: Set<DataOrigin> = emptySet(),
        ascendingOrder: Boolean = true,
        pageSize: Int = 1000,
        pageToken: String? = null
    ) : this(
        recordType = recordType,
        timeRangeFilter = timeRangeFilter,
        dataOriginFilter = dataOriginFilter,
        ascendingOrder = ascendingOrder,
        pageSize = pageSize,
        pageToken = pageToken,
        deduplicateStrategy = DEDUPLICATION_STRATEGY_DISABLED,
    )

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
        if (deduplicateStrategy != other.deduplicateStrategy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = recordType.hashCode()
        result = 31 * result + timeRangeFilter.hashCode()
        result = 31 * result + dataOriginFilter.hashCode()
        result = 31 * result + ascendingOrder.hashCode()
        result = 31 * result + pageSize
        result = 31 * result + (pageToken?.hashCode() ?: 0)
        result = 31 * result + (deduplicateStrategy.hashCode())
        return result
    }

    /**
     * Available strategies used to handle duplicate records in the
     * [androidx.health.connect.client.response.ReadRecordsResponse.records].
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value =
            [
                DEDUPLICATION_STRATEGY_DISABLED,
                DEDUPLICATION_STRATEGY_ENABLED_DEFAULT,
                DEDUPLICATION_STRATEGY_ENABLED_PRIORITIZE_CALLING_APP,
            ]
    )
    @OptIn(ExperimentalDeduplicationApi::class)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    annotation class DeduplicationStrategy

    @ExperimentalDeduplicationApi
    internal companion object {
        /**
         * No deduplication handled. Returns all raw data.
         *
         * This is the default option.
         */
        @ExperimentalDeduplicationApi
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val DEDUPLICATION_STRATEGY_DISABLED = 0

        /**
         * Uses the default deduplication strategy recommended by Health Connect. This may change
         * over time, it's not guaranteed the strategy remains the same over different updates.
         *
         * <p>Currently this is {@code DEDUPLICATION_STRATEGY_ENABLED_DEDUPE_ALL}. To stick to a
         * specified strategy over updates, set the desired strategy directly.
         */
        @ExperimentalDeduplicationApi
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val DEDUPLICATION_STRATEGY_ENABLED_DEFAULT = 1

        /**
         * Strips all duplicate records in the database and returns a fully deduplicated list
         * available via {@link ReadRecordsResponse#getRecords}. If duplications are detected, the
         * record belongs to the calling app will be the winner.
         *
         * <p>Only records of session types like {@link ExerciseSessionRecord} and {@link
         * SleepSessionRecord} are affected. It's no-op for other record types.
         */
        @ExperimentalDeduplicationApi
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val DEDUPLICATION_STRATEGY_ENABLED_PRIORITIZE_CALLING_APP = 2
    }
}
