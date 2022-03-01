/*
 * Copyright (C) 2021 The Android Open Source Project
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
import androidx.health.data.client.permission.Permission
import androidx.health.data.client.records.Record
import androidx.health.data.client.response.InsertRecordResponse
import androidx.health.data.client.response.ReadRecordResponse
import androidx.health.data.client.time.TimeRangeFilter
import kotlin.reflect.KClass

/** Interface to access health and fitness records. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface HealthDataClient {
    /**
     * Returns a set of [Permission]s granted by the user to this app, out of the input
     * [permissions] set.
     *
     * @throws RemoteException For any IPC transportation failures.
     * @throws IOException For any disk I/O issues.
     */
    suspend fun getGrantedPermissions(permissions: Set<Permission>): Set<Permission>

    /**
     * Inserts one or more [Record] points and returns newly assigned
     * [androidx.health.data.client.metadata.Metadata.uid] generated. Insertion of multiple [Record]
     * s is executed in a transaction - if one fails, none is inserted.
     *
     * @param records List of records to insert
     * @return List of unique identifiers in the order of inserted records.
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     */
    suspend fun insertRecords(records: List<Record>): InsertRecordResponse

    /**
     * Updates one or more [Record] points of given UIDs to newly specified values. Update of
     * multiple [Record]s is executed in a transaction - if one fails, none is inserted.
     *
     * @param records List of records to update
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     */
    // TODO(b/220277197): Add client ID to documentation.
    suspend fun updateRecords(records: List<Record>)

    /**
     * Deletes one or more [Record] points. Deletion of multiple [Record]s is executed in a
     * transaction - if one fails, none is deleted.
     *
     * @param recordType Which type of [Record]s to delete, such as `Steps::class`
     * @param uidsList List of uids of [Record]s to delete
     * @param clientIdsList List of client IDs of [Record]s to delete
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     */
    suspend fun deleteRecords(
        recordType: KClass<out Record>,
        uidsList: List<String>,
        clientIdsList: List<String>
    )

    /**
     * Deletes one or more [Record] points of the given [Record] type in the given range
     * (automatically filtered to [Record]s belonging to this application). Deletion of multiple
     * [Record]s is executed in a transaction - if one fails, none is deleted.
     *
     * When a field is null in [TimeRangeFilter] then the filtered range is open-ended in that
     * direction. Hence if all fields are null in [TimeRangeFilter] then all data of the requested
     * [Record] type is deleted. By default timeRangeFilter is such empty [TimeRangeFilter].
     *
     * @param recordType Which type of [Record]s to delete, such as `Steps::class`
     * @param timeRangeFilter The [TimeRangeFilter] to delete from
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     */
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
    suspend fun <T : Record> readRecord(recordType: KClass<T>, uid: String): ReadRecordResponse<T>

    // TODO(b/219327548): Expand this to reuse readRecords time range filter and data origin
    // filters.
    suspend fun aggregate(
        aggregateMetrics: Set<AggregateMetric>,
        timeRangeFilter: TimeRangeFilter,
    ): AggregateDataRow

    // TODO(b/219327548): Adds overload with groupBy that return a list
}