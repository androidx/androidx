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
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.DoNotInline
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.content.pm.PackageInfoCompat
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.impl.HealthConnectClientImpl
import androidx.health.connect.client.impl.HealthConnectClientUpsideDownImpl
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
import kotlin.reflect.KClass

@JvmDefaultWithCompatibility
/** Interface to access health and fitness records. */
interface HealthConnectClient {

    /** Access operations related to permissions. */
    val permissionController: PermissionController

    /**
     * Inserts one or more [Record] and returns newly assigned
     * [androidx.health.connect.client.records.metadata.Metadata.id] generated. Insertion of
     * multiple [records] is executed in a transaction - if one fails, none is inserted.
     *
     * @param records List of records to insert
     * @return List of unique identifiers in the order of inserted records.
     * @throws android.os.RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws java.io.IOException For any disk I/O issues.
     *
     * For example, to insert basic data like step counts:
     *
     * @sample androidx.health.connect.client.samples.InsertSteps
     *
     * To insert more complex data like nutrition for a user who’s eaten a banana:
     *
     * @sample androidx.health.connect.client.samples.InsertNutrition
     *
     * To insert some heart rate data:
     *
     * @sample androidx.health.connect.client.samples.InsertHeartRateSeries
     *
     * [androidx.health.connect.client.records.metadata.Metadata.clientRecordId] can be used to
     * deduplicate data with a client provided unique identifier. When a subsequent [insertRecords]
     * is called with the same
     * [androidx.health.connect.client.records.metadata.Metadata.clientRecordId], whichever [Record]
     * with the higher
     * [androidx.health.connect.client.records.metadata.Metadata.clientRecordVersion] takes
     * precedence.
     */
    suspend fun insertRecords(records: List<Record>): InsertRecordsResponse

    /**
     * Updates one or more [Record] of given UIDs to newly specified values. Update of multiple
     * [records] is executed in a transaction - if one fails, none is inserted.
     *
     * @param records List of records to update
     * @throws android.os.RemoteException For any IPC transportation failures. Update with invalid
     *   identifiers will result in IPC failure.
     * @throws SecurityException For requests with unpermitted access.
     * @throws java.io.IOException For any disk I/O issues.
     */
    suspend fun updateRecords(records: List<Record>)

    /**
     * Deletes one or more [Record] by their identifiers. Deletion of multiple [Record] is executed
     * in single transaction - if one fails, none is deleted.
     *
     * @param recordType Which type of [Record] to delete, such as `Steps::class`
     * @param recordIdsList List of [androidx.health.connect.client.records.metadata.Metadata.id] of
     *   [Record] to delete
     * @param clientRecordIdsList List of client record IDs of [Record] to delete
     * @throws android.os.RemoteException For any IPC transportation failures. Deleting by invalid
     *   identifiers such as a non-existing identifier or deleting the same record multiple times
     *   will result in IPC failure.
     * @throws SecurityException For requests with unpermitted access.
     * @throws java.io.IOException For any disk I/O issues.
     *
     * Example usage to delete written steps data by its unique identifier:
     *
     * @sample androidx.health.connect.client.samples.DeleteByUniqueIdentifier
     */
    suspend fun deleteRecords(
        recordType: KClass<out Record>,
        recordIdsList: List<String>,
        clientRecordIdsList: List<String>,
    )

    /**
     * Deletes any [Record] of the given [recordType] in the given [timeRangeFilter] (automatically
     * filtered to [Record] belonging to the calling application). Deletion of multiple [Record] is
     * executed in a transaction - if one fails, none is deleted.
     *
     * @param recordType Which type of [Record] to delete, such as `Steps::class`
     * @param timeRangeFilter The [TimeRangeFilter] to delete from
     * @throws android.os.RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws java.io.IOException For any disk I/O issues.
     *
     * Example usage to delete written steps data in a time range:
     *
     * @sample androidx.health.connect.client.samples.DeleteByTimeRange
     */
    suspend fun deleteRecords(recordType: KClass<out Record>, timeRangeFilter: TimeRangeFilter)

    /**
     * Reads one [Record] point with its [recordType] and [recordId].
     *
     * @param recordType Which type of [Record] to read, such as `Steps::class`
     * @param recordId [androidx.health.connect.client.records.metadata.Metadata.id] of [Record] to
     *   read
     * @return The [Record] data point.
     * @throws android.os.RemoteException For any IPC transportation failures. Update with invalid
     *   identifiers will result in IPC failure.
     * @throws SecurityException For requests with unpermitted access.
     * @throws java.io.IOException For any disk I/O issues.
     */
    suspend fun <T : Record> readRecord(
        recordType: KClass<T>,
        recordId: String
    ): ReadRecordResponse<T>

    /**
     * Retrieves a collection of [Record]s.
     *
     * @param T the type of [Record]
     * @param request [ReadRecordsRequest] object specifying time range and other filters
     * @return a response containing a collection of [Record]s.
     * @throws android.os.RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws java.io.IOException For any disk I/O issues.
     *
     * Example code to read basic data like step counts:
     *
     * @sample androidx.health.connect.client.samples.ReadStepsRange
     */
    suspend fun <T : Record> readRecords(request: ReadRecordsRequest<T>): ReadRecordsResponse<T>

    /**
     * Reads [AggregateMetric]s according to requested read criteria: [Record]s from
     * [AggregateRequest.dataOriginFilter] and within [AggregateRequest.timeRangeFilter].
     *
     * @param request [AggregateRequest] object specifying [AggregateMetric]s to aggregate and other
     *   filters.
     * @return the [AggregationResult] that contains aggregated values.
     * @throws android.os.RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws java.io.IOException For any disk I/O issues.
     *
     * Example code to aggregate cumulative data like distance:
     *
     * @sample androidx.health.connect.client.samples.AggregateDistance
     *
     * Example code to retrieve statistical aggregates like maximum or minimum heart rate:
     *
     * @sample androidx.health.connect.client.samples.AggregateHeartRate
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
     * @param request [AggregateGroupByDurationRequest] object specifying [AggregateMetric]s to
     *   aggregate and other filters.
     * @return a list of [AggregationResultGroupedByDuration]s, each contains aggregated values and
     *   start/end time of the row. The list is sorted by time in ascending order.
     * @throws android.os.RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws java.io.IOException For any disk I/O issues.
     *
     * Example code to retrieve cumulative step count for each minute within provided time range:
     *
     * @sample androidx.health.connect.client.samples.AggregateIntoMinutes
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
     * @param request [AggregateGroupByPeriodRequest] object specifying [AggregateMetric]s to
     *   aggregate and other filters.
     * @return a list of [AggregationResultGroupedByPeriod]s, each contains aggregated values and
     *   start/end time of the row. The list is sorted by time in ascending order.
     * @throws android.os.RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @throws java.io.IOException For any disk I/O issues.
     *
     * Example code to retrieve cumulative step count for each month within provided time range:
     *
     * @sample androidx.health.connect.client.samples.AggregateIntoMonths
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
     * @throws android.os.RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
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
     *   Health Platform.
     * @return a [ChangesResponse] with changes since provided [changesToken].
     * @throws android.os.RemoteException For any IPC transportation failures.
     * @throws SecurityException For requests with unpermitted access.
     * @see getChangesToken
     */
    suspend fun getChanges(changesToken: String): ChangesResponse

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal const val DEFAULT_PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal const val DEFAULT_PROVIDER_MIN_VERSION_CODE = 68623
        /**
         * Intent action to open Health Connect settings on this phone. Developers should use this
         * if they want to re-direct the user to Health Connect.
         */
        @get:JvmName("getHealthConnectSettingsAction")
        @JvmStatic
        val ACTION_HEALTH_CONNECT_SETTINGS =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                "android.health.connect.action.HEALTH_HOME_SETTINGS"
            else "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal val ACTION_HEALTH_CONNECT_MANAGE_DATA =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                "android.health.connect.action.MANAGE_HEALTH_DATA"
            else "androidx.health.ACTION_MANAGE_HEALTH_DATA"

        /**
         * The Health Connect SDK is unavailable on this device at the time. This can be due to the
         * device running a lower than required Android Version.
         *
         * Apps should hide any integration points to Health Connect in this case.
         */
        const val SDK_UNAVAILABLE = 1

        /**
         * The Health Connect SDK APIs are currently unavailable, the provider is either not
         * installed or needs to be updated.
         *
         * Apps may choose to redirect to package installers to find a suitable APK.
         */
        const val SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED = 2

        /**
         * The Health Connect SDK APIs are available.
         *
         * Apps can subsequently call [getOrCreate] to get an instance of [HealthConnectClient].
         */
        const val SDK_AVAILABLE = 3

        /** Availability Status. */
        @Retention(AnnotationRetention.SOURCE)
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @IntDef(
            value =
                [
                    SDK_UNAVAILABLE,
                    SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
                    SDK_AVAILABLE,
                ]
        )
        annotation class AvailabilityStatus

        /**
         * Determines whether the Health Connect SDK is available on this device at the moment.
         *
         * @param context the context
         * @param providerPackageName optional package provider to choose for backend implementation
         * @return One of [SDK_UNAVAILABLE], [SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED], or
         *   [SDK_AVAILABLE]
         * @sample androidx.health.connect.client.samples.AvailabilityCheckSamples
         */
        @JvmOverloads
        @JvmStatic
        @AvailabilityStatus
        fun getSdkStatus(
            context: Context,
            providerPackageName: String = DEFAULT_PROVIDER_PACKAGE_NAME,
        ): Int {
            if (!isSdkVersionSufficient() || isProfileInAndroidU(context)) {
                return SDK_UNAVAILABLE
            }
            if (!isProviderAvailable(context, providerPackageName)) {
                return SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
            }
            return SDK_AVAILABLE
        }

        /**
         * Retrieves an IPC-backed [HealthConnectClient] instance binding to an available
         * implementation.
         *
         * @param context the context
         * @param providerPackageName optional alternative package provider to choose for backend
         *   implementation
         * @return instance of [HealthConnectClient] ready for issuing requests
         * @throws UnsupportedOperationException if service not available due to SDK version too low
         *   or running in a profile
         * @throws IllegalStateException if the SDK is not available
         * @see getSdkStatus
         */
        @JvmOverloads
        @JvmStatic
        public fun getOrCreate(
            context: Context,
            providerPackageName: String = DEFAULT_PROVIDER_PACKAGE_NAME,
        ): HealthConnectClient {
            val status = getSdkStatus(context, providerPackageName)
            if (status == SDK_UNAVAILABLE) {
                throw UnsupportedOperationException("SDK version too low or running in a profile")
            }
            if (status == SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
                throw IllegalStateException("Service not available")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                return HealthConnectClientUpsideDownImpl(context)
            }
            return HealthConnectClientImpl(
                HealthDataService.getClient(context, providerPackageName)
            )
        }

        /**
         * Intent to open Health Connect data management screen on this phone. Developers should use
         * this if they want to re-direct the user to Health Connect data management.
         *
         * @param context the context
         * @param providerPackageName optional alternative package provider to choose for backend
         *   implementation
         * @return Intent to open Health Connect data management screen.
         */
        @JvmOverloads
        @JvmStatic
        fun getHealthConnectManageDataIntent(
            context: Context,
            providerPackageName: String = DEFAULT_PROVIDER_PACKAGE_NAME,
        ): Intent {
            val pm = context.packageManager
            val manageDataIntent = Intent(ACTION_HEALTH_CONNECT_MANAGE_DATA)

            return if (
                isProviderAvailable(context, providerPackageName) &&
                    pm.resolveActivity(manageDataIntent, /* flags */ 0) != null
            ) {
                manageDataIntent
            } else {
                Intent(ACTION_HEALTH_CONNECT_SETTINGS)
            }
        }

        @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
        internal fun isSdkVersionSufficient() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        /**
         * Determines whether an implementation of [HealthConnectClient] is available on this device
         * at the moment.
         */
        internal fun isProviderAvailable(
            context: Context,
            providerPackageName: String = DEFAULT_PROVIDER_PACKAGE_NAME,
            providerVersionCode: Int = DEFAULT_PROVIDER_MIN_VERSION_CODE
        ): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                return true
            }
            return isPackageInstalled(
                context.packageManager,
                providerPackageName,
                providerVersionCode
            )
        }

        private fun isPackageInstalled(
            packageManager: PackageManager,
            packageName: String,
            versionCode: Int = DEFAULT_PROVIDER_MIN_VERSION_CODE
        ): Boolean {
            val packageInfo: PackageInfo =
                try {
                    @Suppress("Deprecation") // getPackageInfo deprecated in T
                    packageManager.getPackageInfo(packageName, /* flags= */ 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    return false
                }
            return packageInfo.applicationInfo.enabled &&
                (packageName != DEFAULT_PROVIDER_PACKAGE_NAME ||
                    PackageInfoCompat.getLongVersionCode(packageInfo) >= versionCode) &&
                hasBindableService(packageManager, packageName)
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

        private fun isProfileInAndroidU(context: Context): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                Api33Impl.isProfile(context)
        }

        /** Tag used in SDK debug logs. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal const val HEALTH_CONNECT_CLIENT_TAG = "HealthConnectClient"
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private object Api33Impl {
        @JvmStatic
        @DoNotInline
        fun isProfile(context: Context): Boolean {
            return (context.getSystemService(Context.USER_SERVICE) as UserManager).isProfile
        }
    }
}
