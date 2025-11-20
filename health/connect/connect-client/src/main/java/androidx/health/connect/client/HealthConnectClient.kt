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
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.core.content.pm.PackageInfoCompat
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_UNAVAILABLE
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
import androidx.health.connect.client.HealthConnectClient.Companion.getOrCreate
import androidx.health.connect.client.HealthConnectClient.Companion.getSdkStatus
import androidx.health.connect.client.HealthConnectFeatures.Companion.FEATURE_PERSONAL_HEALTH_RECORD
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.feature.FEATURE_CONSTANT_NAME_PHR
import androidx.health.connect.client.feature.HealthConnectFeaturesUnavailableImpl
import androidx.health.connect.client.feature.createExceptionDueToFeatureUnavailable
import androidx.health.connect.client.impl.HealthConnectClientImpl
import androidx.health.connect.client.impl.HealthConnectClientUpsideDownImpl
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_MEDICAL_DATA_VACCINES
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_WRITE_MEDICAL_DATA
import androidx.health.connect.client.records.FhirResource
import androidx.health.connect.client.records.MedicalDataSource
import androidx.health.connect.client.records.MedicalResource
import androidx.health.connect.client.records.MedicalResourceId
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.CreateMedicalDataSourceRequest
import androidx.health.connect.client.request.DeleteMedicalResourcesRequest
import androidx.health.connect.client.request.GetMedicalDataSourcesRequest
import androidx.health.connect.client.request.ReadMedicalResourcesInitialRequest
import androidx.health.connect.client.request.ReadMedicalResourcesPageRequest
import androidx.health.connect.client.request.ReadMedicalResourcesRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.request.UpsertMedicalResourceRequest
import androidx.health.connect.client.response.ChangesResponse
import androidx.health.connect.client.response.InsertRecordsResponse
import androidx.health.connect.client.response.ReadMedicalResourcesResponse
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

    /** Access operations related to feature availability. */
    val features: HealthConnectFeatures
        get() = HealthConnectFeaturesUnavailableImpl

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
     * @param recordType Which type of [Record] to delete, such as `StepsRecord::class`
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
        recordId: String,
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
        request: AggregateGroupByDurationRequest
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
        request: AggregateGroupByPeriodRequest
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
     * Retrieves changes in Health Connect, from a specific point in time represented by provided
     * [changesToken].
     *
     * The response returned may not provide all the changes due to IPC or memory limits, see
     * [ChangesResponse.hasMore]. Clients can make more api calls to fetch more changes from Health
     * Connect with updated [ChangesResponse.nextChangesToken].
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

    /**
     * Inserts or updates a list of [MedicalResource]s using [UpsertMedicalResourceRequest]s.
     *
     * In each request, from [UpsertMedicalResourceRequest.dataSourceId], fhir resource type and
     * fhir resource ID extracted from [UpsertMedicalResourceRequest.data], a [MedicalResourceId]
     * will be constructed. If there already exists a [MedicalResource] with that ID in Health
     * Connect, then it will be updated, otherwise a new [MedicalResource] will be inserted.
     *
     * For each [UpsertMedicalResourceRequest], one [MedicalResource] will be returned, regardless
     * whether it's updated or inserted. The order of the [MedicalResource]s in the returned list
     * will be the same as their corresponding [UpsertMedicalResourceRequest]s in the input list.
     *
     * Note that a [MedicalDataSource] needs to be created using [createMedicalDataSource] before
     * any [MedicalResource]s can be upserted for this source.
     *
     * Regarding permissions:
     * - Caller must hold [PERMISSION_WRITE_MEDICAL_DATA] in order to call this API, otherwise a
     *   [SecurityException] will be thrown.
     * - With [PERMISSION_WRITE_MEDICAL_DATA] granted, caller is permitted to call this API in
     *   either foreground or background.
     *
     * Medical data is represented using the
     * [Fast Healthcare Interoperability Resources (FHIR)]("https://hl7.org/fhir/") standard. The
     * FHIR resource provided in [UpsertMedicalResourceRequest.data] is expected to be valid for the
     * specified [FHIR version][UpsertMedicalResourceRequest.fhirVersion] according to the
     * [FHIR spec](https://hl7.org/fhir/resourcelist.html). Structural validation checks such as
     * resource structure, field types and presence of required fields will be performed, however
     * these checks may not cover all FHIR spec requirements and are dependant on the backing
     * implementation of Health Connect.
     *
     * Data written to Health Connect should be for a single individual only. However, the API
     * allows for multiple Patient resources to be written to account for the possibility of
     * multiple Patient resources being present in one individual's medical record.
     *
     * Each [UpsertMedicalResourceRequest] also has to meet the following requirements:
     * - [UpsertMedicalResourceRequest.data] must contain an "id" field and a "resourceType" field.
     *   The "resource type" must be one of the items in the accepted list of resource types in
     *   [FhirResource].
     * - The FHIR resource does not have a "contained" field (holds
     *   [contained resources](https://build.fhir.org/references.html#contained)).
     * - [FHIR version][UpsertMedicalResourceRequest.fhirVersion] of each request must match
     *   [MedicalDataSource.fhirVersion] of [UpsertMedicalResourceRequest.dataSourceId]'s
     *   corresponding [MedicalDataSource].
     *
     * If any request is failed to be processed for any reason, none of the requests will be
     * inserted or updated in one transaction.
     *
     * This feature is dependent on the version of HealthConnect installed on the device. To check
     * if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
     * [FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An [UnsupportedOperationException] would be
     * thrown if the feature is not available.
     *
     * @param requests List of upsert requests.
     * @throws IllegalArgumentException if any request is failed to be processed for any reason such
     *   as invalid [UpsertMedicalResourceRequest.dataSourceId]
     * @throws SecurityException if caller does not hold [PERMISSION_WRITE_MEDICAL_DATA].
     * @sample androidx.health.connect.client.samples.UpsertMedicalResourcesSample
     */
    @RequiresPermission("android.permission.health.WRITE_MEDICAL_DATA")
    @ExperimentalPersonalHealthRecordApi
    suspend fun upsertMedicalResources(
        requests: List<UpsertMedicalResourceRequest>
    ): List<MedicalResource> =
        throw createExceptionDueToFeatureUnavailable(
            FEATURE_CONSTANT_NAME_PHR,
            "HealthConnectClient#upsetMedicalResources()",
        )

    /**
     * Reads [MedicalResource]s by request, either [ReadMedicalResourcesInitialRequest] or
     * [ReadMedicalResourcesPageRequest].
     *
     * A typical flow to read all [MedicalResource]s that satisfy a certain criteria would be:
     * 1. Create a [ReadMedicalResourcesInitialRequest] with desired criteria, and make a request.
     *    If successful, a [ReadMedicalResourcesResponse] should be returned.
     * 2. Use returned [ReadMedicalResourcesResponse.nextPageToken] to create a
     *    [ReadMedicalResourcesPageRequest] and make another request. Again, if successful, a
     *    [ReadMedicalResourcesResponse] should be returned.
     * 3. Repeat step 2 until [ReadMedicalResourcesResponse.nextPageToken] is null.
     *
     * Regarding permissions, only permitted [MedicalResource]s are returned. Specifically:
     * - A caller without any read medical permissions such as
     *   [PERMISSION_READ_MEDICAL_DATA_VACCINES], or the write medical permission
     *   [PERMISSION_WRITE_MEDICAL_DATA] is not permitted to read any [MedicalResource], including
     *   ones that it created.
     * - A caller with the write permission is permitted to read its own [MedicalResource]s of any
     *   type regardless whether it's in foreground or background.
     * - A caller with only a read permission, when in background, is only permitted to read its own
     *   [MedicalResource]s of the corresponding type. For example, if a caller only holds
     *   [PERMISSION_READ_MEDICAL_DATA_VACCINES], then when in background it is only permitted to
     *   read its own [MedicalResource]s with [MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES].
     *   However, when it is in foreground or if it holds
     *   [PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND], it is permitted to read all vaccines
     *   [MedicalResource]s, including ones that were written by other apps.
     *
     * Each returned [MedicalResource] is not guaranteed to meet all requirements of the <a
     * href="https://hl7.org/fhir/resourcelist.html">Fast Healthcare Interoperability Resources
     * (FHIR) spec</a>. If required, clients should perform their own validations.
     *
     * This feature is dependent on the version of HealthConnect installed on the device. To check
     * if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
     * [FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An [UnsupportedOperationException] would be
     * thrown if the feature is not available.
     *
     * @sample androidx.health.connect.client.samples.ReadMedicalResourcesByRequestSample
     */
    @ExperimentalPersonalHealthRecordApi
    suspend fun readMedicalResources(
        request: ReadMedicalResourcesRequest
    ): ReadMedicalResourcesResponse =
        throw createExceptionDueToFeatureUnavailable(
            FEATURE_CONSTANT_NAME_PHR,
            "HealthConnectClient#readMedicalResources(request: ReadMedicalResourcesRequest)",
        )

    /**
     * Reads a collection of [MedicalResource]s given a list of [MedicalResourceId]s.
     *
     * The number and order of medical resources returned by this API is not guaranteed, depending
     * on a number of factors:
     * - If an empty list of IDs is provided, an empty list will be returned.
     * - If any ID does not exist, no medical resource will be returned for that ID.
     * - Only permitted [MedicalResource]s are returned. Specifically:
     *     - A caller without any read medical permissions such as
     *       [PERMISSION_READ_MEDICAL_DATA_VACCINES], or the write medical permission
     *       [PERMISSION_WRITE_MEDICAL_DATA] is not permitted to read any [MedicalResource],
     *       including ones that it created.
     *     - A caller with the write permission is permitted to read its own [MedicalResource]s of
     *       any type regardless whether it's in foreground or background.
     *     - A caller with only a read permission, when in background, is only permitted to read its
     *       own [MedicalResource]s of the corresponding type. For example, if a caller only holds
     *       [PERMISSION_READ_MEDICAL_DATA_VACCINES], then when in background it is only permitted
     *       to read its own [MedicalResource]s with
     *       [MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES]. However, when it is in foreground or
     *       if it holds [PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND], it is permitted to read all
     *       vaccines [MedicalResource]s, including ones that were written by other apps.
     *
     * Each returned [MedicalResource] is not guaranteed to meet all requirements of the <a
     * href="https://hl7.org/fhir/resourcelist.html">Fast Healthcare Interoperability Resources
     * (FHIR) spec</a>. If required, clients should perform their own validations.
     *
     * This feature is dependent on the version of HealthConnect installed on the device. To check
     * if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
     * [FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An [UnsupportedOperationException] would be
     * thrown if the feature is not available.
     *
     * @throws IllegalArgumentException if the size of [ids] is too large or any ID is deemed as
     *   invalid.
     * @sample androidx.health.connect.client.samples.ReadMedicalResourcesByIdsSample
     */
    @ExperimentalPersonalHealthRecordApi
    suspend fun readMedicalResources(ids: List<MedicalResourceId>): List<MedicalResource> =
        throw createExceptionDueToFeatureUnavailable(
            FEATURE_CONSTANT_NAME_PHR,
            "HealthConnectClient#readMedicalResources(ids: List<MedicalResourceId>)",
        )

    /**
     * Deletes a list of [MedicalResource]s by the provided list of [MedicalResourceId]s.
     * - If any ID in [ids] is invalid, the API will throw an [IllegalArgumentException], and
     *   nothing will be deleted.
     * - If any ID in [ids] does not exist, that ID will be ignored, while deletion on other IDs
     *   will be performed.
     *
     * Regarding permissions:
     * - Caller must hold [PERMISSION_WRITE_MEDICAL_DATA] in order to call this API, even then, it
     *   can only delete its own data. If any of the items in [ids] belongs to another app, they
     *   will be ignored.
     * - Deletes are permitted in the foreground or background.
     *
     * This feature is dependent on the version of HealthConnect installed on the device. To check
     * if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
     * [FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An [UnsupportedOperationException] would be
     * thrown if the feature is not available.
     *
     * @param ids The ids to delete.
     * @throws SecurityException if caller does not hold [PERMISSION_WRITE_MEDICAL_DATA].
     * @sample androidx.health.connect.client.samples.DeleteMedicalResourcesSample
     */
    @RequiresPermission("android.permission.health.WRITE_MEDICAL_DATA")
    @ExperimentalPersonalHealthRecordApi
    suspend fun deleteMedicalResources(ids: List<MedicalResourceId>): Unit =
        throw createExceptionDueToFeatureUnavailable(
            FEATURE_CONSTANT_NAME_PHR,
            "HealthConnectClient#deleteMedicalResources(ids: List<MedicalResourceId>)",
        )

    /**
     * Deletes [MedicalResource]s based on given filters in
     * [request][DeleteMedicalResourcesRequest].
     *
     * Only [MedicalResource]s inserted by the calling app will be deleted. If the [request] matches
     * any other [MedicalResource]s, these will be ignored.
     *
     * Regarding permissions:
     * - Caller must hold [PERMISSION_WRITE_MEDICAL_DATA] in order to call this API.
     * - Deletes are permitted in the foreground or background.
     *
     * This feature is dependent on the version of HealthConnect installed on the device. To check
     * if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
     * [FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An [UnsupportedOperationException] would be
     * thrown if the feature is not available.
     *
     * @param request The request that contains the filters to delete.
     * @throws SecurityException if caller does not hold [PERMISSION_WRITE_MEDICAL_DATA].
     * @sample androidx.health.connect.client.samples.DeleteMedicalResourcesByRequestSample
     */
    @RequiresPermission("android.permission.health.WRITE_MEDICAL_DATA")
    @ExperimentalPersonalHealthRecordApi
    suspend fun deleteMedicalResources(request: DeleteMedicalResourcesRequest): Unit =
        throw createExceptionDueToFeatureUnavailable(
            FEATURE_CONSTANT_NAME_PHR,
            "HealthConnectClient#deleteMedicalResources(request: DeleteMedicalResourcesRequest)",
        )

    /**
     * Creates a [MedicalDataSource] using a [CreateMedicalDataSourceRequest].
     *
     * Regarding permissions:
     * - Caller must hold [PERMISSION_WRITE_MEDICAL_DATA] in order to call this API, otherwise a
     *   [SecurityException] will be thrown.
     * - With [PERMISSION_WRITE_MEDICAL_DATA] granted, caller is permitted to call this API in
     *   either foreground or background.
     *
     * Medical data is represented using the
     * [Fast Healthcare Interoperability Resources (FHIR)](https://hl7.org/fhir/) standard.
     *
     * A [MedicalDataSource] needs to be created before any [MedicalResource]s for that source can
     * be inserted. Separate [MedicalDataSource]s should be created for medical records coming from
     * different sources (e.g. different FHIR endpoints, different healthcare systems), unless the
     * data has been reconciled and all records have a unique combination of resource type and
     * resource id.
     *
     * The [CreateMedicalDataSourceRequest.displayName] must be unique across all medical data
     * sources created by an app, otherwise an [IllegalArgumentException] will be thrown. See
     * [CreateMedicalDataSourceRequest.fhirBaseUri] for more details on the FHIR base URI. The data
     * source can not be updated after creation.
     *
     * This feature is dependent on the version of HealthConnect installed on the device. To check
     * if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
     * [FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An [UnsupportedOperationException] would be
     * thrown if the feature is not available.
     *
     * @param request request containing details of the [MedicalDataSource] to be created
     * @return the created [MedicalDataSource]
     * @throws [SecurityException] if caller does not hold [PERMISSION_WRITE_MEDICAL_DATA].
     * @sample androidx.health.connect.client.samples.CreateMedicalDataSourceSample
     */
    @ExperimentalPersonalHealthRecordApi
    @RequiresPermission("android.permission.health.WRITE_MEDICAL_DATA")
    suspend fun createMedicalDataSource(
        request: CreateMedicalDataSourceRequest
    ): MedicalDataSource {
        throw createExceptionDueToFeatureUnavailable(
            FEATURE_CONSTANT_NAME_PHR,
            "HealthConnectClient#createMedicalDataSource()",
        )
    }

    /**
     * Deletes a [MedicalDataSource] and all data contained within it.
     *
     * Regarding permissions:
     * - Caller must hold [PERMISSION_WRITE_MEDICAL_DATA] in order to call this API, otherwise a
     *   [SecurityException] will be thrown.
     * - With [PERMISSION_WRITE_MEDICAL_DATA] granted, caller is permitted to call this API in
     *   either foreground or background.
     * - Caller may only delete data sources it created.
     *
     * Medical data is represented using the
     * [Fast Healthcare Interoperability Resources (FHIR)](https://hl7.org/fhir/) standard.
     *
     * This feature is dependent on the version of HealthConnect installed on the device. To check
     * if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
     * [FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An [UnsupportedOperationException] would be
     * thrown if the feature is not available.
     *
     * @param id The id of the data source to delete.
     * @throws IllegalArgumentException if [id] is invalid, does not exist, or owned by another app.
     * @sample androidx.health.connect.client.samples.DeleteMedicalDataSourceWithDataSample
     */
    @ExperimentalPersonalHealthRecordApi
    @RequiresPermission("android.permission.health.WRITE_MEDICAL_DATA")
    suspend fun deleteMedicalDataSourceWithData(id: String) {
        throw createExceptionDueToFeatureUnavailable(
            FEATURE_CONSTANT_NAME_PHR,
            "HealthConnectClient#deleteMedicalDataSourceWithData()",
        )
    }

    /**
     * Gets the requested [MedicalDataSource]s using [GetMedicalDataSourcesRequest]. Number of data
     * sources returned by this API will depend based on below factors:
     * - If an empty [GetMedicalDataSourcesRequest] is passed, all data sources for all apps are
     *   requested, and all which the caller is permitted to get will be returned. See below.
     * - If [GetMedicalDataSourcesRequest.packageNames] is not empty, then only the data sources
     *   created by those packages is being requested. All data sources created by those packages
     *   which the caller is permitted to get will be returned. See below.
     *
     * There is no specific read permission for getting data sources. Instead, permission to read
     * data sources is based on whether the caller has permission to read the data currently
     * contained in that data source. Specifically:
     * - A caller without any read medical permissions such as
     *   [PERMISSION_READ_MEDICAL_DATA_VACCINES] or the write permission
     *   [PERMISSION_WRITE_MEDICAL_DATA] is not permitted get any [MedicalDataSource]s, including
     *   ones that it created.
     * - A caller with the write permission is permitted to read its own [MedicalDataSource]s
     *   regardless whether it's in foreground or background.
     * - A caller with only a read permission, when in background, is only permitted to read its own
     *   [MedicalDataSource]s that contains at least one [MedicalResource] with the type that the
     *   read permission covers. However, when the caller is in foreground or if it holds
     *   [PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND], it can also read other apps'
     *   [MedicalDataSource]s as long as each of those [MedicalDataSource] contains at least one
     *   [MedicalResource] with the corresponding type.
     *     - For example, a client `A` created a [MedicalDataSource] `DS1`, then used `DS1` to
     *       insert a [MedicalResource] `MR1` with type
     *       [MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES]. Similarly another client `B` created
     *       `DS2`, then used `DS2` to insert another [MedicalResource] `MR2` with the same
     *       [MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES]. If `A` holds
     *       [PERMISSION_READ_MEDICAL_DATA_VACCINES] and it is in background, it can only read
     *       `DS1`. However, if `A` is in foreground or holds
     *       [PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND], it can read both `DS1` and `DS2`.
     *
     * This feature is dependent on the version of HealthConnect installed on the device. To check
     * if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
     * [FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An [UnsupportedOperationException] would be
     * thrown if the feature is not available.
     *
     * @param request containing details of the [MedicalDataSource]s to retrieve
     * @return [MedicalDataSource]s matching the provided request
     * @sample androidx.health.connect.client.samples.GetMedicalDataSourcesByRequestSample
     */
    @ExperimentalPersonalHealthRecordApi
    suspend fun getMedicalDataSources(
        request: GetMedicalDataSourcesRequest
    ): List<MedicalDataSource> {
        throw createExceptionDueToFeatureUnavailable(
            FEATURE_CONSTANT_NAME_PHR,
            "HealthConnectClient#getMedicalDataSources()",
        )
    }

    /**
     * Gets [MedicalDataSource]s for the provided list of [ids][MedicalDataSource.id].
     *
     * The returned list of data sources will be in the same order as the [ids].
     *
     * Number of data sources returned by this API will depend based on below factors:
     * - If an empty list of [ids] is provided, no data sources will be returned.
     * - If an id is invalid or non-existent, no data source will be returned for that id, this
     *   means the returned list might have fewer elements than [ids].
     * - Callers will only get data sources they are permitted to get. See below.
     *
     * There is no specific read permission for getting data sources. Instead, permission to read
     * data sources is based on whether the caller has permission to read the data currently
     * contained in that data source. Being permitted to get data sources is dependent on the
     * following logic, in priority order, earlier statements take precedence.
     * - A caller without any read medical permissions such as
     *   [PERMISSION_READ_MEDICAL_DATA_VACCINES] or the write permission
     *   [PERMISSION_WRITE_MEDICAL_DATA] is not permitted get any [MedicalDataSource]s, including
     *   ones that it created.
     * - A caller with the write permission is permitted to read its own [MedicalDataSource]s
     *   regardless whether it's in foreground or background.
     * - A caller with only a read permission, when in background, is only permitted to read its own
     *   [MedicalDataSource]s that contains at least one [MedicalResource] with the type that the
     *   read permission covers. However, when the caller is in foreground or if it holds
     *   [PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND], it can also read other apps'
     *   [MedicalDataSource]s as long as each of those [MedicalDataSource] contains at least one
     *   [MedicalResource] with the corresponding type.
     *     - For example, if a caller `A` created a [MedicalDataSource] `DS1`, then used `DS1` to
     *       insert a [MedicalResource] `MR1` with type
     *       [MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES], and another caller `B` created `DS2`.
     *       If `A` holds [PERMISSION_READ_MEDICAL_DATA_VACCINES], it can only read `DS1` in
     *       background. However, if it is in foreground or holds
     *       [PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND], it can read both `DS1` and `DS2`.
     *
     * This feature is dependent on the version of HealthConnect installed on the device. To check
     * if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
     * [FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An [UnsupportedOperationException] would be
     * thrown if the feature is not available.
     *
     * @param ids ids of the [MedicalDataSource]s to retrieve
     * @return [MedicalDataSource]s matching the provided [ids]
     * @sample androidx.health.connect.client.samples.GetMedicalDataSourcesByIdsSample
     */
    @ExperimentalPersonalHealthRecordApi
    suspend fun getMedicalDataSources(ids: List<String>): List<MedicalDataSource> {
        throw createExceptionDueToFeatureUnavailable(
            FEATURE_CONSTANT_NAME_PHR,
            "HealthConnectClient#getMedicalDataSources()",
        )
    }

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
        @IntDef(value = [SDK_UNAVAILABLE, SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED, SDK_AVAILABLE])
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
            return when (Build.VERSION.SDK_INT) {
                in Build.VERSION_CODES.UPSIDE_DOWN_CAKE..Int.MAX_VALUE ->
                    Api34Impl.getSdkStatus(context)
                in Build.VERSION_CODES.P..Build.VERSION_CODES.TIRAMISU ->
                    if (isPackageInstalled(context.packageManager, providerPackageName)) {
                        SDK_AVAILABLE
                    } else {
                        SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
                    }
                else -> return SDK_UNAVAILABLE
            }
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
            return HealthConnectClientImpl(context, providerPackageName)
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
                getSdkStatus(context, providerPackageName) == SDK_AVAILABLE &&
                    pm.resolveActivity(manageDataIntent, /* flags */ 0) != null
            ) {
                manageDataIntent
            } else {
                Intent(ACTION_HEALTH_CONNECT_SETTINGS)
            }
        }

        private fun isPackageInstalled(
            packageManager: PackageManager,
            packageName: String,
            versionCode: Int = DEFAULT_PROVIDER_MIN_VERSION_CODE,
        ): Boolean {
            val packageInfo: PackageInfo =
                try {
                    @Suppress("Deprecation") // getPackageInfo deprecated in T
                    packageManager.getPackageInfo(packageName, /* flags= */ 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    return false
                }
            return (packageInfo.applicationInfo?.enabled == true) &&
                (packageName != DEFAULT_PROVIDER_PACKAGE_NAME ||
                    PackageInfoCompat.getLongVersionCode(packageInfo) >= versionCode) &&
                hasBindableService(packageManager, packageName)
        }

        internal fun hasBindableService(
            packageManager: PackageManager,
            packageName: String,
        ): Boolean {
            val bindIntent = Intent()
            bindIntent.setPackage(packageName)
            bindIntent.setAction(HealthDataService.ANDROID_HEALTH_PLATFORM_SERVICE_BIND_ACTION)
            @Suppress("Deprecation") // deprecated in T
            return packageManager.queryIntentServices(bindIntent, 0).isNotEmpty()
        }

        /** Tag used in SDK debug logs. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal const val HEALTH_CONNECT_CLIENT_TAG = "HealthConnectClient"
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private object Api34Impl {
        @JvmStatic
        @AvailabilityStatus
        fun getSdkStatus(context: Context): Int {
            return if (
                isProfile(context) ||
                    context.getSystemService(Context.HEALTHCONNECT_SERVICE) == null
            ) {
                SDK_UNAVAILABLE
            } else {
                SDK_AVAILABLE
            }
        }

        private fun isProfile(context: Context): Boolean {
            return (context.getSystemService(Context.USER_SERVICE) as UserManager).isProfile
        }
    }
}
