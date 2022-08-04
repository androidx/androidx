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
package androidx.health.connect.client

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.RemoteException
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RestrictTo
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.impl.HealthConnectClientImpl
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ChangesResponse
import androidx.health.connect.client.response.InsertRecordsResponse
import androidx.health.connect.client.response.ReadRecordResponse
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.platform.client.HealthDataService
import java.io.IOException
import java.lang.IllegalStateException
import kotlin.reflect.KClass

/** Interface to access health and fitness records. */
interface HealthConnectClient {

    /** Access operations related to permissions. */
    val permissionController: PermissionController

    /**
     * Inserts one or more [Record] and returns newly assigned
     * [androidx.health.connect.client.records.metadata.Metadata.uid] generated. Insertion of
     * multiple [records] is executed in a transaction - if one fails, none is inserted.
     *
     * For example, to insert basic data like step counts:
     * @sample androidx.health.connect.client.samples.InsertSteps
     *
     * To insert more complex data like nutrition for a user who’s eaten a banana:
     * @sample androidx.health.connect.client.samples.InsertNutrition
     *
     * To insert some heart rate data:
     * @sample androidx.health.connect.client.samples.InsertHeartRateSeries
     *
     * [androidx.health.connect.client.records.metadata.Metadata.clientId] can be used to
     * deduplicate data with a client provided unique identifier. When a subsequent [insertRecords]
     * is called with the same [androidx.health.connect.client.records.metadata.Metadata.clientId],
     * whichever [Record] with the higher [androidx.health.connect.client.records.metadata.Metadata.clientVersion]
     * takes precedence.
     *
     * @param records List of records to insert
     * @return List of unique identifiers in the order of inserted records.
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     * @throws IllegalStateException If service is not available.
     */
    suspend fun insertRecords(records: List<Record>): InsertRecordsResponse

    /**
     * Updates one or more [Record] of given UIDs to newly specified values. Update of multiple
     * [records] is executed in a transaction - if one fails, none is inserted.
     *
     * @param records List of records to update
     * @throws RemoteException For any IPC transportation failures. Update with invalid identifiers
     * will result in IPC failure.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     * @throws IllegalStateException If service is not available.
     */
    suspend fun updateRecords(records: List<Record>)

    /**
     * Deletes one or more [Record] by their identifiers. Deletion of multiple [Record] is executed
     * in single transaction - if one fails, none is deleted.
     *
     * Example usage to delete written steps data by its unique identifier:
     * @sample androidx.health.connect.client.samples.DeleteByUniqueIdentifier
     *
     * @param recordType Which type of [Record] to delete, such as `Steps::class`
     * @param uidsList List of uids of [Record] to delete
     * @param clientIdsList List of client IDs of [Record] to delete
     * @throws RemoteException For any IPC transportation failures. Deleting by invalid identifiers
     * such as a non-existing identifier or deleting the same record multiple times will result in
     * IPC failure.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     * @throws IllegalStateException If service is not available.
     */
    suspend fun deleteRecords(
        recordType: KClass<out Record>,
        uidsList: List<String>,
        clientIdsList: List<String>,
    )

    /**
     * Deletes any [Record] of the given [recordType] in the given [timeRangeFilter] (automatically
     * filtered to [Record] belonging to the calling application). Deletion of multiple [Record] is
     * executed in a transaction - if one fails, none is deleted.
     *
     * Example usage to delete written steps data in a time range:
     * @sample androidx.health.connect.client.samples.DeleteByTimeRange
     *
     * @param recordType Which type of [Record] to delete, such as `Steps::class`
     * @param timeRangeFilter The [TimeRangeFilter] to delete from
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     * @throws IllegalStateException If service is not available.
     */
    suspend fun deleteRecords(recordType: KClass<out Record>, timeRangeFilter: TimeRangeFilter)

    /**
     * Reads one [Record] point with its [recordType] and [uid].
     *
     * @param recordType Which type of [Record] to read, such as `Steps::class`
     * @param uid Uid of [Record] to read
     * @return The [Record] data point.
     * @throws RemoteException For any IPC transportation failures. Update with invalid identifiers
     * will result in IPC failure.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     * @throws IllegalStateException If service is not available.
     */
    suspend fun <T : Record> readRecord(recordType: KClass<T>, uid: String): ReadRecordResponse<T>

    /**
     * Retrieves a collection of [Record]s.
     *
     * Example code to read basic data like step counts:
     * @sample androidx.health.connect.client.samples.ReadStepsRange
     *
     * @param T the type of [Record]
     * @param request [ReadRecordsRequest] object specifying time range and other filters
     *
     * @return a response containing a collection of [Record]s.
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     * @throws IllegalStateException If service is not available.
     */
    suspend fun <T : Record> readRecords(request: ReadRecordsRequest<T>): ReadRecordsResponse<T>

    /**
     * Reads [AggregateMetric]s according to requested read criteria: [Record]s from
     * [AggregateRequest.dataOriginFilter] and within [AggregateRequest.timeRangeFilter].
     *
     * Example code to aggregate cumulative data like distance:
     * @sample androidx.health.connect.client.samples.AggregateDistance
     *
     * Example code to retrieve statistical aggregates like maximum or minimum heart rate:
     * @sample androidx.health.connect.client.samples.AggregateHeartRate
     *
     * @param request [AggregateRequest] object specifying [AggregateMetric]s to aggregate and other
     * filters.
     *
     * @return the [AggregationResult] that contains aggregated values.
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     * @throws IllegalStateException If service is not available.
     */
    suspend fun aggregate(request: AggregateRequest): AggregationResult

    /**
     * Reads [AggregateMetric]s according to requested read criteria specified in
     * [AggregateGroupByDurationRequest].
     *
     * This method is similar to [aggregate] but instead of returning one [AggregationResult] for
     * the entire query's time interval, it returns a list of [AggregationResultGroupedByDuration],
     * with each row keyed by start and end time. For example: steps for today bucketed by hours.
     *
     * An [AggregationResultGroupedByDuration] is returned only if there are [Record] to aggregate
     * within start and end time of the row.
     *
     * Example code to retrieve cumulative step count for each minute within provided time range:
     * @sample androidx.health.connect.client.samples.AggregateIntoMinutes
     *
     * @param request [AggregateGroupByDurationRequest] object specifying [AggregateMetric]s to
     * aggregate and other filters.
     *
     * @return a list of [AggregationResultGroupedByDuration]s, each contains aggregated values and
     * start/end time of the row. The list is sorted by time in ascending order.
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     * @throws IllegalStateException If service is not available.
     */
    suspend fun aggregateGroupByDuration(
        request: AggregateGroupByDurationRequest,
    ): List<AggregationResultGroupedByDuration>

    /**
     * Reads [AggregateMetric]s according to requested read criteria specified in
     * [AggregateGroupByPeriodRequest].
     *
     * This method is similar to [aggregate] but instead of returning one [AggregationResult] for
     * the entire query's time interval, it returns a list of [AggregationResultGroupedByPeriod],
     * with each row keyed by start and end time. For example: steps for this month bucketed by day.
     *
     * An [AggregationResultGroupedByPeriod] is returned only if there are [Record] to aggregate
     * within start and end time of the row.
     *
     * Example code to retrieve cumulative step count for each month within provided time range:
     * @sample androidx.health.connect.client.samples.AggregateIntoMonths
     *
     * @param request [AggregateGroupByPeriodRequest] object specifying [AggregateMetric]s to
     * aggregate and other filters.
     *
     * @return a list of [AggregationResultGroupedByPeriod]s, each contains aggregated values and
     * start/end time of the row. The list is sorted by time in ascending order.
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IOException For any disk I/O issues.
     * @throws IllegalStateException If service is not available.
     */
    suspend fun aggregateGroupByPeriod(
        request: AggregateGroupByPeriodRequest,
    ): List<AggregationResultGroupedByPeriod>

    /**
     * Retrieves a changes-token, representing a point in time in the underlying Android Health
     * Platform for a given [ChangesTokenRequest]. Changes-tokens are used in [getChanges] to
     * retrieve changes since that point in time.
     *
     * Changes-tokens represent a point in time after which the client is interested in knowing the
     * changes for a set of interested types of [Record] and optional [DataOrigin] filters.
     *
     * Changes-tokens are only valid for 30 days after they're generated. Calls to [getChanges] with
     * an expired changes-token will lead to [ChangesResponse.changesTokenExpired]
     *
     * @param request Includes interested types of record to observe changes and optional filters.
     * @return a changes-token
     *
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IllegalStateException If service is not available.
     *
     * @see getChanges
     */
    suspend fun getChangesToken(request: ChangesTokenRequest): String

    /**
     * Retrieves changes in Android Health Platform, from a specific point in time represented by
     * provided [changesToken].
     *
     * The response returned may not provide all the changes due to IPC or memory limits, see
     * [ChangesResponse.hasMore]. Clients can make more api calls to fetch more changes from the
     * Android Health Platform with updated [ChangesResponse.nextChangesToken].
     *
     * Provided [changesToken] may have expired if clients have not synced for extended period of
     * time (such as a month). In this case [ChangesResponse.changesTokenExpired] will be set, and
     * clients should generate a new changes-token via [getChangesToken].
     *
     * ```
     * val response = client.getChanges(changesToken)
     * if (response.changesTokenExpired) {
     *   // Consider re-sync and fetch new changes token.
     * } else {
     *   // Process new insertion/deletions, either update local storage or upload to backends.
     * }
     * ```
     *
     * @param changesToken A Changes-Token that represents a specific point in time in Android
     * Health Platform.
     * @return a [ChangesResponse] with changes since provided [changesToken].
     *
     * @throws RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws IllegalStateException If service is not available.
     *
     * @see getChangesToken
     */
    suspend fun getChanges(changesToken: String): ChangesResponse

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal const val DEFAULT_PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"

        /**
         * Determines whether an implementation of [HealthConnectClient] is available on this device
         * at the moment.
         *
         * @param packageNames optional package provider to choose implementation from
         * @return whether the api is available
         */
        @JvmOverloads
        @JvmStatic
        public fun isAvailable(
            context: Context,
            packageNames: List<String> = listOf(DEFAULT_PROVIDER_PACKAGE_NAME),
        ): Boolean {
            if (!isSdkVersionSufficient()) {
                return false
            }
            return packageNames.any { isPackageInstalled(context.packageManager, it) }
        }

        /**
         * Retrieves an IPC-backed [HealthConnectClient] instance binding to an available
         * implementation.
         *
         * @param packageNames optional package provider to choose implementation from
         * @return instance of [HealthConnectClient] ready for issuing requests
         * @throws UnsupportedOperationException if service not available due to SDK version too low
         * @throws IllegalStateException if service not available due to not installed
         *
         * @see isAvailable
         */
        @JvmOverloads
        @JvmStatic
        public fun getOrCreate(
            context: Context,
            packageNames: List<String> = listOf(DEFAULT_PROVIDER_PACKAGE_NAME),
        ): HealthConnectClient {
            if (!isSdkVersionSufficient()) {
                throw UnsupportedOperationException("SDK version too low")
            }
            if (!isAvailable(context, packageNames)) {
                throw IllegalStateException("Service not available")
            }
            val enabledPackage =
                packageNames.first { isPackageInstalled(context.packageManager, it) }
            return HealthConnectClientImpl(
                enabledPackage,
                HealthDataService.getClient(context, enabledPackage)
            )
        }

        @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
        internal fun isSdkVersionSufficient() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        internal fun isPackageInstalled(
            packageManager: PackageManager,
            packageName: String,
        ): Boolean {
            val isPackageInstalledAndEnabled =
                try {
                    @Suppress("Deprecation") // getApplicationInfo deprecated in T
                    packageManager.getApplicationInfo(packageName, /* flags= */ 0).enabled
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                }
            return isPackageInstalledAndEnabled && hasBindableService(packageManager, packageName)
        }

        internal fun hasBindableService(
            packageManager: PackageManager,
            packageName: String
        ): Boolean {
            val bindIntent = Intent()
            bindIntent.setPackage(packageName)
            bindIntent.setAction(HealthDataService.ANDROID_HEALTH_PLATFORM_SERVICE_BIND_ACTION)
            @Suppress("Deprecation") // deprecated in T
            return packageManager.queryIntentServices(bindIntent, 0).isNotEmpty()
        }

        /**
         * Tag used in SDK debug logs.
         * @suppress
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal const val HEALTH_CONNECT_CLIENT_TAG = "HealthConnectClient"
    }
}
