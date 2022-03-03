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
package androidx.health.data.client

import androidx.annotation.RestrictTo
import androidx.health.data.client.aggregate.AggregateDataRow
import androidx.health.data.client.aggregate.AggregateMetric
import androidx.health.data.client.metadata.DataOrigin
import androidx.health.data.client.permission.Permission
import androidx.health.data.client.records.Record
import androidx.health.data.client.response.InsertRecordResponse
import androidx.health.data.client.response.ReadRecordResponse
import androidx.health.data.client.response.ReadRecordsResponse
import androidx.health.data.client.time.TimeRangeFilter
import kotlin.reflect.KClass

/** Interface to access health and fitness records. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface HealthDataClient {
    /**
     * Returns a set of [Permission] granted by the user to this app, out of the input [permissions]
     * set.
     *
     * @throws RemoteException For any IPC transportation failures.
     * @throws IOException For any disk I/O issues.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    suspend fun getGrantedPermissions(permissions: Set<Permission>): Set<Permission>

    /**
     * Inserts one or more [Record] and returns newly assigned
     * [androidx.health.data.client.metadata.Metadata.uid] generated. Insertion of multiple
     * [records] is executed in a transaction - if one fails, none is inserted.
     *
     * @param records List of records to insert
     * @return List of unique identifiers in the order of inserted records.
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    suspend fun insertRecords(records: List<Record>): InsertRecordResponse

    /**
     * Updates one or more [Record] of given UIDs to newly specified values. Update of multiple
     * [records] is executed in a transaction - if one fails, none is inserted.
     *
     * @param records List of records to update
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY) suspend fun updateRecords(records: List<Record>)

    /**
     * Deletes one or more [Record] by their identifiers. Deletion of multiple [Record] is executed
     * in single transaction - if one fails, none is deleted.
     *
     * @param recordType Which type of [Record] to delete, such as `Steps::class`
     * @param uidsList List of uids of [Record] to delete
     * @param clientIdsList List of client IDs of [Record] to delete
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    suspend fun deleteRecords(
        recordType: KClass<out Record>,
        uidsList: List<String>,
        clientIdsList: List<String>
    )

    /**
     * Deletes one or more [Record] points of the given [recordType] in the given range
     * (automatically filtered to [Record] belonging to this application). Deletion of multiple
     * [Record] is executed in a transaction - if one fails, none is deleted.
     *
     * When a field is null in [TimeRangeFilter] then the filtered range is open-ended in that
     * direction. Hence if all fields are null in [TimeRangeFilter] then all data of the requested
     * [Record] type is deleted.
     *
     * @param recordType Which type of [Record] to delete, such as `Steps::class`
     * @param timeRangeFilter The [TimeRangeFilter] to delete from
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    suspend fun deleteRecords(recordType: KClass<out Record>, timeRangeFilter: TimeRangeFilter)

    /**
     * Reads one [Record] point determined by its data type and UID.
     *
     * @param recordType Which type of [Record] to read, such as `Steps::class`
     * @param uid Uid of [Record] to read
     * @return The [Record] data point.
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    suspend fun <T : Record> readRecord(recordType: KClass<T>, uid: String): ReadRecordResponse<T>

    /**
     * Reads a set of [Record] points determined by time range and other filters.
     *
     * @param recordType Which type of [Record] to read, such as `Steps::class`.
     * @param timeRangeFilter The [TimeRangeFilter] to read from. If open-ended in any direction,
     * [limit] must be set.
     * @param dataOriginFilter List of [DataOrigin] to read from, or empty for no filter.
     * @param ascendingOrder Whether the [Record] should be returned in ascending or descending
     * order by time. Default is true.
     * @param limit Maximum number of [Record] to read. Cannot be set together with [pageSize]. Must
     * be set if [timeRangeFilter] is open-ended in any direction.
     * @param pageSize Maximum number of [Record] within one page. If there's more data remaining
     * (and the next page should be read), the response will contain a [pageToken] to be used in the
     * subsequent read request. Cannot be set together with [limit].
     * @param pageToken Continuation token to access the next page, returned in the response to the
     * previous page read request. [pageSize] must be set together with [pageToken].
     * @return The list of [Record] data points and optionally a [pageToken] to read the next page.
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     * @throws IllegalStateException For incorrectly set parameters.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    suspend fun <T : Record> readRecords(
        recordType: KClass<T>,
        timeRangeFilter: TimeRangeFilter,
        dataOriginFilter: List<DataOrigin>,
        ascendingOrder: Boolean,
        limit: Int?,
        pageSize: Int?,
        pageToken: String?
    ): ReadRecordsResponse<T>

    /** See [HealthDataClient.readRecords]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    suspend fun <T : Record> readRecords(
        recordType: KClass<T>,
        timeRangeFilter: TimeRangeFilter,
        ascendingOrder: Boolean,
        limit: Int
    ): ReadRecordsResponse<T> =
        readRecords(
            recordType = recordType,
            timeRangeFilter = timeRangeFilter,
            dataOriginFilter = emptyList(),
            ascendingOrder = ascendingOrder,
            limit = limit,
            pageSize = null,
            pageToken = null
        )

    /** See [HealthDataClient.readRecords]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    suspend fun <T : Record> readRecords(
        recordType: KClass<T>,
        timeRangeFilter: TimeRangeFilter,
        limit: Int
    ): ReadRecordsResponse<T> =
        readRecords(
            recordType = recordType,
            timeRangeFilter = timeRangeFilter,
            dataOriginFilter = emptyList(),
            ascendingOrder = true,
            limit = limit,
            pageSize = null,
            pageToken = null
        )

    /** See [HealthDataClient.readRecords]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    suspend fun <T : Record> readRecords(
        recordType: KClass<T>,
        timeRangeFilter: TimeRangeFilter,
        ascendingOrder: Boolean,
        pageSize: Int,
        pageToken: String?
    ): ReadRecordsResponse<T> =
        readRecords(
            recordType = recordType,
            timeRangeFilter = timeRangeFilter,
            dataOriginFilter = emptyList(),
            ascendingOrder = ascendingOrder,
            limit = null,
            pageSize = pageSize,
            pageToken = pageToken
        )

    /** See [HealthDataClient.readRecords]. */
    suspend fun <T : Record> readRecords(
        recordType: KClass<T>,
        timeRangeFilter: TimeRangeFilter,
        pageSize: Int,
        pageToken: String?
    ): ReadRecordsResponse<T> =
        readRecords(
            recordType = recordType,
            timeRangeFilter = timeRangeFilter,
            dataOriginFilter = emptyList(),
            ascendingOrder = true,
            limit = null,
            pageSize = pageSize,
            pageToken = pageToken
        )

    /** See [HealthDataClient.readRecords]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    suspend fun <T : Record> readRecords(
        recordType: KClass<T>,
        timeRangeFilter: TimeRangeFilter,
        ascendingOrder: Boolean,
    ): ReadRecordsResponse<T> =
        readRecords(
            recordType = recordType,
            timeRangeFilter = timeRangeFilter,
            dataOriginFilter = emptyList(),
            ascendingOrder = ascendingOrder,
            limit = null,
            pageSize = null,
            pageToken = null
        )

    /** See [HealthDataClient.readRecords]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    suspend fun <T : Record> readRecords(
        recordType: KClass<T>,
        timeRangeFilter: TimeRangeFilter,
    ): ReadRecordsResponse<T> =
        readRecords(
            recordType = recordType,
            timeRangeFilter = timeRangeFilter,
            dataOriginFilter = emptyList(),
            ascendingOrder = true,
            limit = null,
            pageSize = null,
            pageToken = null
        )

    /** See [HealthDataClient.readRecords]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    suspend fun <T : Record> readRecords(
        recordType: KClass<T>,
        timeRangeFilter: TimeRangeFilter,
        dataOriginFilter: List<DataOrigin>,
    ): ReadRecordsResponse<T> =
        readRecords(
            recordType = recordType,
            timeRangeFilter = timeRangeFilter,
            dataOriginFilter = dataOriginFilter,
            ascendingOrder = true,
            limit = null,
            pageSize = null,
            pageToken = null
        )

    // TODO(b/219327548): Expand this to reuse readRecords time range filter and data origin
    // filters.
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    suspend fun aggregate(
        aggregateMetrics: Set<AggregateMetric>,
        timeRangeFilter: TimeRangeFilter,
    ): AggregateDataRow

    // TODO(b/219327548): Adds overload with groupBy that return a list
}
