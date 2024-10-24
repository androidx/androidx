/*
 * Copyright 2023 The Android Open Source Project
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
@file:JvmName("HealthConnectClientExt")

package androidx.health.connect.client

import android.os.RemoteException
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.response.ReadRecordResponse
import androidx.health.connect.client.time.TimeRangeFilter
import java.io.IOException

/**
 * Deletes one or more [Record] by their identifiers. Deletion of multiple [Record] is executed in
 * single transaction - if one fails, none is deleted.
 *
 * @param T Which type of [Record] to delete, such as `StepsRecord`.
 * @param recordIdsList List of [androidx.health.connect.client.records.metadata.Metadata.id] of
 *   [Record] to delete
 * @param clientRecordIdsList List of client record IDs of [Record] to delete
 * @throws RemoteException For any IPC transportation failures. Deleting by invalid identifiers such
 *   as a non-existing identifier or deleting the same record multiple times will result in IPC
 *   failure.
 * @throws SecurityException For requests with unpermitted access.
 * @throws IOException For any disk I/O issues.
 * @throws IllegalStateException If service is not available.
 * @sample androidx.health.connect.client.samples.DeleteByUniqueIdentifier
 * @see HealthConnectClient.deleteRecords
 *
 * Example usage to delete written steps data by its unique identifier:
 */
suspend inline fun <reified T : Record> HealthConnectClient.deleteRecords(
    recordIdsList: List<String>,
    clientRecordIdsList: List<String>,
) {
    deleteRecords(
        recordType = T::class,
        recordIdsList = recordIdsList,
        clientRecordIdsList = clientRecordIdsList,
    )
}

/**
 * Deletes any [Record] of type [T] in the given [timeRangeFilter] (automatically filtered to
 * [Record] belonging to the calling application). Deletion of multiple [Record] is executed in a
 * transaction - if one fails, none is deleted.
 *
 * @param T Which type of [Record] to delete, such as `StepsRecord`.
 * @param timeRangeFilter The [TimeRangeFilter] to delete from
 * @throws RemoteException For any IPC transportation failures.
 * @throws SecurityException For requests with unpermitted access.
 * @throws IOException For any disk I/O issues.
 * @throws IllegalStateException If service is not available.
 * @sample androidx.health.connect.client.samples.DeleteByTimeRange
 * @see HealthConnectClient.deleteRecords
 *
 * Example usage to delete written steps data in a time range:
 */
suspend inline fun <reified T : Record> HealthConnectClient.deleteRecords(
    timeRangeFilter: TimeRangeFilter,
) {
    deleteRecords(
        recordType = T::class,
        timeRangeFilter = timeRangeFilter,
    )
}

/**
 * Reads one [Record] point of type [T] and with the specified [recordId].
 *
 * @param T Which type of [Record] to read, such as `StepsRecord`.
 * @param recordId [androidx.health.connect.client.records.metadata.Metadata.id] of [Record] to read
 * @return The [Record] data point.
 * @throws RemoteException For any IPC transportation failures. Update with invalid identifiers will
 *   result in IPC failure.
 * @throws SecurityException For requests with unpermitted access.
 * @throws IOException For any disk I/O issues.
 * @throws IllegalStateException If service is not available.
 * @see HealthConnectClient.readRecord
 */
suspend inline fun <reified T : Record> HealthConnectClient.readRecord(
    recordId: String
): ReadRecordResponse<T> =
    readRecord(
        recordType = T::class,
        recordId = recordId,
    )
