/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.health.connect.client.impl

import android.content.Context
import android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED
import android.content.pm.PackageManager.GET_PERMISSIONS
import android.content.pm.PackageManager.PackageInfoFlags
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.ReadRecordsRequestUsingIds
import android.health.connect.RecordIdFilter
import android.health.connect.changelog.ChangeLogsRequest
import android.os.Build
import android.os.RemoteException
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.core.os.asOutcomeReceiver
import androidx.health.connect.client.ExperimentalDeduplicationApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.feature.HealthConnectFeaturesPlatformImpl
import androidx.health.connect.client.feature.withPhrFeatureCheckSuspend
import androidx.health.connect.client.impl.platform.aggregate.aggregateFallback
import androidx.health.connect.client.impl.platform.aggregate.isPlatformSupportedMetric
import androidx.health.connect.client.impl.platform.records.toPlatformRecord
import androidx.health.connect.client.impl.platform.records.toPlatformRecordClass
import androidx.health.connect.client.impl.platform.records.toSdkMedicalDataSource
import androidx.health.connect.client.impl.platform.records.toSdkMedicalResource
import androidx.health.connect.client.impl.platform.records.toSdkRecord
import androidx.health.connect.client.impl.platform.request.toPlatformLocalTimeRangeFilter
import androidx.health.connect.client.impl.platform.request.toPlatformRequest
import androidx.health.connect.client.impl.platform.request.toPlatformTimeRangeFilter
import androidx.health.connect.client.impl.platform.response.toKtResponse
import androidx.health.connect.client.impl.platform.response.toSdkResponse
import androidx.health.connect.client.impl.platform.toKtException
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_PREFIX
import androidx.health.connect.client.records.MedicalDataSource
import androidx.health.connect.client.records.MedicalResource
import androidx.health.connect.client.records.MedicalResourceId
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.CreateMedicalDataSourceRequest
import androidx.health.connect.client.request.DeleteMedicalResourcesRequest
import androidx.health.connect.client.request.GetMedicalDataSourcesRequest
import androidx.health.connect.client.request.ReadMedicalResourcesRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.request.ReadRecordsRequest.Companion.DEDUPLICATION_STRATEGY_DISABLED
import androidx.health.connect.client.request.UpsertMedicalResourceRequest
import androidx.health.connect.client.response.ChangesResponse
import androidx.health.connect.client.response.InsertRecordsResponse
import androidx.health.connect.client.response.ReadMedicalResourcesResponse
import androidx.health.connect.client.response.ReadRecordResponse
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import kotlin.reflect.KClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine

/** Implements the [HealthConnectClient] with APIs in UpsideDownCake. */
@RequiresApi(api = 34)
class HealthConnectClientUpsideDownImpl : HealthConnectClient, PermissionController {

    private val executor = Dispatchers.Default.asExecutor()

    private val context: Context
    private val healthConnectManager: HealthConnectManager
    private val revokePermissionsFunction: (Collection<String>) -> Unit

    constructor(context: Context) : this(context, context::revokeSelfPermissionsOnKill)

    @VisibleForTesting
    internal constructor(
        context: Context,
        revokePermissionsFunction: (Collection<String>) -> Unit,
    ) {
        this.context = context
        this.healthConnectManager =
            context.getSystemService(Context.HEALTHCONNECT_SERVICE) as HealthConnectManager
        this.revokePermissionsFunction = revokePermissionsFunction
    }

    override val permissionController: PermissionController
        get() = this

    override val features: HealthConnectFeatures = HealthConnectFeaturesPlatformImpl()

    override suspend fun insertRecords(records: List<Record>): InsertRecordsResponse {
        val response = wrapPlatformException {
            suspendCancellableCoroutine { continuation ->
                healthConnectManager.insertRecords(
                    records.map { it.toPlatformRecord() },
                    executor,
                    continuation.asOutcomeReceiver(),
                )
            }
        }
        return response.toKtResponse()
    }

    override suspend fun updateRecords(records: List<Record>) {
        wrapPlatformException {
            suspendCancellableCoroutine { continuation ->
                healthConnectManager.updateRecords(
                    records.map { it.toPlatformRecord() },
                    executor,
                    continuation.asOutcomeReceiver(),
                )
            }
        }
    }

    override suspend fun deleteRecords(
        recordType: KClass<out Record>,
        recordIdsList: List<String>,
        clientRecordIdsList: List<String>,
    ) {
        wrapPlatformException {
            suspendCancellableCoroutine { continuation ->
                healthConnectManager.deleteRecords(
                    buildList {
                        recordIdsList.forEach {
                            add(RecordIdFilter.fromId(recordType.toPlatformRecordClass(), it))
                        }
                        clientRecordIdsList.forEach {
                            add(
                                RecordIdFilter.fromClientRecordId(
                                    recordType.toPlatformRecordClass(),
                                    it,
                                )
                            )
                        }
                    },
                    executor,
                    continuation.asOutcomeReceiver(),
                )
            }
        }
    }

    override suspend fun deleteRecords(
        recordType: KClass<out Record>,
        timeRangeFilter: TimeRangeFilter,
    ) {
        wrapPlatformException {
            suspendCancellableCoroutine { continuation ->
                healthConnectManager.deleteRecords(
                    recordType.toPlatformRecordClass(),
                    timeRangeFilter.toPlatformTimeRangeFilter(),
                    executor,
                    continuation.asOutcomeReceiver(),
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST") // Safe to cast as the type should match
    override suspend fun <T : Record> readRecord(
        recordType: KClass<T>,
        recordId: String,
    ): ReadRecordResponse<T> {
        val response = wrapPlatformException {
            suspendCancellableCoroutine { continuation ->
                healthConnectManager.readRecords(
                    ReadRecordsRequestUsingIds.Builder(recordType.toPlatformRecordClass())
                        .addId(recordId)
                        .build(),
                    executor,
                    continuation.asOutcomeReceiver(),
                )
            }
        }
        if (response.records.isEmpty()) {
            throw RemoteException("No records")
        }
        return ReadRecordResponse(response.records[0].toSdkRecord() as T)
    }

    @OptIn(ExperimentalDeduplicationApi::class)
    @Suppress("UNCHECKED_CAST") // Safe to cast as the type should match
    override suspend fun <T : Record> readRecords(
        request: ReadRecordsRequest<T>
    ): ReadRecordsResponse<T> {
        if (request.deduplicateStrategy != DEDUPLICATION_STRATEGY_DISABLED) {
            TODO("Not yet implemented")
        }
        val response = wrapPlatformException {
            suspendCancellableCoroutine { continuation ->
                healthConnectManager.readRecords(
                    request.toPlatformRequest(),
                    executor,
                    continuation.asOutcomeReceiver(),
                )
            }
        }
        return ReadRecordsResponse(
            response.records.map { it.toSdkRecord() as T },
            pageToken = response.nextPageToken.takeUnless { it == -1L }?.toString(),
        )
    }

    override suspend fun aggregate(request: AggregateRequest): AggregationResult {
        requireAggregationMetrics(request.metrics)

        val fallbackResponse = aggregateFallback(request)

        val platformSupportedMetrics =
            request.metrics.filter { it.isPlatformSupportedMetric() }.toSet()

        if (platformSupportedMetrics.isEmpty()) {
            return fallbackResponse
        }

        val platformResponse =
            wrapPlatformException {
                    suspendCancellableCoroutine { continuation ->
                        healthConnectManager.aggregate(
                            request.toPlatformRequest(),
                            executor,
                            continuation.asOutcomeReceiver(),
                        )
                    }
                }
                .toSdkResponse(platformSupportedMetrics)

        return platformResponse + fallbackResponse
    }

    override suspend fun aggregateGroupByDuration(
        request: AggregateGroupByDurationRequest
    ): List<AggregationResultGroupedByDuration> {
        requireAggregationMetrics(request.metrics)

        val fallbackResponse = aggregateFallback(request)

        val platformSupportedMetrics =
            request.metrics.filter { it.isPlatformSupportedMetric() }.toSet()

        if (platformSupportedMetrics.isEmpty()) {
            return fallbackResponse
        }

        val platformResponse =
            wrapPlatformException {
                    suspendCancellableCoroutine { continuation ->
                        healthConnectManager.aggregateGroupByDuration(
                            request.toPlatformRequest(),
                            request.timeRangeSlicer,
                            executor,
                            continuation.asOutcomeReceiver(),
                        )
                    }
                }
                .map { it.toSdkResponse(platformSupportedMetrics) }

        return (fallbackResponse + platformResponse)
            .groupingBy { it.startTime }
            .reduce { startTime, accumulator, element ->
                AggregationResultGroupedByDuration(
                    result = accumulator.result + element.result,
                    startTime = startTime,
                    endTime = accumulator.endTime,
                    zoneOffset = accumulator.zoneOffset,
                )
            }
            .values
            .sortedBy { it.startTime }
    }

    override suspend fun aggregateGroupByPeriod(
        request: AggregateGroupByPeriodRequest
    ): List<AggregationResultGroupedByPeriod> {
        requireAggregationMetrics(request.metrics)

        val fallbackResponse = aggregateFallback(request)

        val platformSupportedMetrics =
            request.metrics.filter { it.isPlatformSupportedMetric() }.toSet()

        if (platformSupportedMetrics.isEmpty()) {
            return fallbackResponse
        }

        val platformResponse =
            wrapPlatformException {
                    suspendCancellableCoroutine { continuation ->
                        healthConnectManager.aggregateGroupByPeriod(
                            request.toPlatformRequest(),
                            request.timeRangeSlicer,
                            executor,
                            continuation.asOutcomeReceiver(),
                        )
                    }
                }
                .mapIndexed { index, response ->
                    if (
                        SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >=
                            10 ||
                            (request.timeRangeSlicer.months == 0 &&
                                request.timeRangeSlicer.years == 0)
                    ) {
                        response.toSdkResponse(platformSupportedMetrics)
                    } else {
                        // Handle bug in the Platform for versions of mainline module before SDK
                        // extension 10, where bucket endTime < bucket startTime (b/298290400)
                        val requestTimeRangeFilter =
                            request.timeRangeFilter.toPlatformLocalTimeRangeFilter()
                        val bucketStartTime =
                            requestTimeRangeFilter.startTime!! +
                                request.timeRangeSlicer.multipliedBy(index)
                        response.toSdkResponse(
                            metrics = platformSupportedMetrics,
                            bucketStartTime = bucketStartTime,
                            bucketEndTime =
                                minOf(
                                    bucketStartTime + request.timeRangeSlicer,
                                    requestTimeRangeFilter.endTime!!,
                                ),
                        )
                    }
                }

        return (fallbackResponse + platformResponse)
            .groupingBy { it.startTime }
            .reduce { startTime, accumulator, element ->
                AggregationResultGroupedByPeriod(
                    result = accumulator.result + element.result,
                    startTime = startTime,
                    endTime = accumulator.endTime,
                )
            }
            .values
            .sortedBy { it.startTime }
    }

    private fun requireAggregationMetrics(metrics: Set<AggregateMetric<*>>) {
        require(metrics.isNotEmpty()) { "At least one of the aggregation types must be set" }
    }

    override suspend fun getChangesToken(request: ChangesTokenRequest): String {
        return wrapPlatformException {
                suspendCancellableCoroutine { continuation ->
                    healthConnectManager.getChangeLogToken(
                        request.toPlatformRequest(),
                        executor,
                        continuation.asOutcomeReceiver(),
                    )
                }
            }
            .token
    }

    override suspend fun getChanges(changesToken: String): ChangesResponse {
        try {
            val response = suspendCancellableCoroutine { continuation ->
                healthConnectManager.getChangeLogs(
                    ChangeLogsRequest.Builder(changesToken).build(),
                    executor,
                    continuation.asOutcomeReceiver(),
                )
            }
            return ChangesResponse(
                buildList {
                    response.upsertedRecords.forEach { add(UpsertionChange(it.toSdkRecord())) }
                    response.deletedLogs.forEach { add(DeletionChange(it.deletedRecordId)) }
                },
                response.nextChangesToken,
                response.hasMorePages(),
                changesTokenExpired = false,
            )
        } catch (e: HealthConnectException) {
            // Handle invalid token
            if (e.errorCode == HealthConnectException.ERROR_INVALID_ARGUMENT) {
                return ChangesResponse(
                    changes = listOf(),
                    nextChangesToken = "",
                    hasMore = false,
                    changesTokenExpired = true,
                )
            }
            throw e.toKtException()
        }
    }

    override suspend fun getGrantedPermissions(): Set<String> {
        context.packageManager
            .getPackageInfo(context.packageName, PackageInfoFlags.of(GET_PERMISSIONS.toLong()))
            .let {
                return buildSet {
                    val requestedPermissions = it.requestedPermissions ?: emptyArray()
                    for (i in requestedPermissions.indices) {
                        if (
                            requestedPermissions[i].startsWith(PERMISSION_PREFIX) &&
                                it.requestedPermissionsFlags!![i] and REQUESTED_PERMISSION_GRANTED >
                                    0
                        ) {
                            add(requestedPermissions[i])
                        }
                    }
                }
            }
    }

    override suspend fun revokeAllPermissions() {
        val requestedPermissions =
            context.packageManager
                .getPackageInfo(context.packageName, PackageInfoFlags.of(GET_PERMISSIONS.toLong()))
                .requestedPermissions ?: emptyArray()
        val allHealthPermissions = requestedPermissions.filter { it.startsWith(PERMISSION_PREFIX) }
        if (allHealthPermissions.isNotEmpty()) {
            revokePermissionsFunction(allHealthPermissions)
        }
    }

    @ExperimentalPersonalHealthRecordApi
    @RequiresPermission("android.permission.health.WRITE_MEDICAL_DATA")
    @RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 16)
    override suspend fun createMedicalDataSource(
        request: CreateMedicalDataSourceRequest
    ): MedicalDataSource =
        withPhrFeatureCheckSuspend(
            this::class,
            "createMedicalDataSource(request: CreateMedicalDataSourceRequest)",
        ) {
            wrapPlatformException {
                    suspendCancellableCoroutine { continuation ->
                        healthConnectManager.createMedicalDataSource(
                            request.platformCreateMedicalDataSourceRequest,
                            executor,
                            continuation.asOutcomeReceiver(),
                        )
                    }
                }
                .toSdkMedicalDataSource()
        }

    @ExperimentalPersonalHealthRecordApi
    @RequiresPermission("android.permission.health.WRITE_MEDICAL_DATA")
    @RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 16)
    override suspend fun deleteMedicalDataSourceWithData(id: String) {
        withPhrFeatureCheckSuspend(this::class, "deleteMedicalDataSourceWithData(id: String)") {
            wrapPlatformException {
                suspendCancellableCoroutine { continuation ->
                    healthConnectManager.deleteMedicalDataSourceWithData(
                        id,
                        executor,
                        continuation.asOutcomeReceiver(),
                    )
                }
            }
        }
    }

    @ExperimentalPersonalHealthRecordApi
    @RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 16)
    override suspend fun getMedicalDataSources(
        request: GetMedicalDataSourcesRequest
    ): List<MedicalDataSource> =
        withPhrFeatureCheckSuspend(
            this::class,
            "getMedicalDataSources(request: GetMedicalDataSourcesRequest)",
        ) {
            wrapPlatformException {
                    suspendCancellableCoroutine { continuation ->
                        healthConnectManager.getMedicalDataSources(
                            request.platformGetMedicalDataSourcesRequest,
                            executor,
                            continuation.asOutcomeReceiver(),
                        )
                    }
                }
                .map { it.toSdkMedicalDataSource() }
        }

    @ExperimentalPersonalHealthRecordApi
    @RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 16)
    override suspend fun getMedicalDataSources(ids: List<String>): List<MedicalDataSource> =
        withPhrFeatureCheckSuspend(this::class, "getMedicalDataSources(ids: List<String>)") {
            wrapPlatformException {
                    suspendCancellableCoroutine { continuation ->
                        healthConnectManager.getMedicalDataSources(
                            ids,
                            executor,
                            continuation.asOutcomeReceiver(),
                        )
                    }
                }
                .map { it.toSdkMedicalDataSource() }
        }

    @ExperimentalPersonalHealthRecordApi
    @RequiresPermission("android.permission.health.WRITE_MEDICAL_DATA")
    @RequiresExtension(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, 16)
    override suspend fun upsertMedicalResources(
        requests: List<UpsertMedicalResourceRequest>
    ): List<MedicalResource> =
        withPhrFeatureCheckSuspend(this::class, "upsertMedicalResources()") {
            wrapPlatformException {
                    suspendCancellableCoroutine { continuation ->
                        healthConnectManager.upsertMedicalResources(
                            requests.map { it.platformUpsertMedicalResourceRequest },
                            executor,
                            continuation.asOutcomeReceiver(),
                        )
                    }
                }
                .map { it.toSdkMedicalResource() }
        }

    @ExperimentalPersonalHealthRecordApi
    @RequiresExtension(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, 16)
    override suspend fun readMedicalResources(
        request: ReadMedicalResourcesRequest
    ): ReadMedicalResourcesResponse =
        withPhrFeatureCheckSuspend(
            this::class,
            "readMedicalResources(request: ReadMedicalResourcesRequest)",
        ) {
            wrapPlatformException {
                    suspendCancellableCoroutine { continuation ->
                        healthConnectManager.readMedicalResources(
                            request.platformReadMedicalResourcesRequest,
                            executor,
                            continuation.asOutcomeReceiver(),
                        )
                    }
                }
                .let { platformResponse ->
                    ReadMedicalResourcesResponse(
                        platformResponse.medicalResources.map { it.toSdkMedicalResource() },
                        platformResponse.nextPageToken,
                        platformResponse.remainingCount,
                    )
                }
        }

    @ExperimentalPersonalHealthRecordApi
    @RequiresExtension(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, 16)
    override suspend fun readMedicalResources(ids: List<MedicalResourceId>): List<MedicalResource> =
        withPhrFeatureCheckSuspend(
            this::class,
            "readMedicalResources(ids: List<MedicalResourceId>)",
        ) {
            wrapPlatformException {
                    suspendCancellableCoroutine { continuation ->
                        healthConnectManager.readMedicalResources(
                            ids.map { it.platformMedicalResourceId },
                            executor,
                            continuation.asOutcomeReceiver(),
                        )
                    }
                }
                .map { it.toSdkMedicalResource() }
        }

    @ExperimentalPersonalHealthRecordApi
    @RequiresPermission("android.permission.health.WRITE_MEDICAL_DATA")
    @RequiresExtension(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, 16)
    override suspend fun deleteMedicalResources(ids: List<MedicalResourceId>) {
        withPhrFeatureCheckSuspend(
            HealthConnectClientUpsideDownImpl::class,
            "deleteMedicalResources(ids: List<MedicalResourceId>)",
        ) {
            wrapPlatformException {
                suspendCancellableCoroutine { continuation ->
                    healthConnectManager.deleteMedicalResources(
                        ids.map { it.platformMedicalResourceId },
                        executor,
                        continuation.asOutcomeReceiver(),
                    )
                }
            }
        }
    }

    @ExperimentalPersonalHealthRecordApi
    @RequiresPermission("android.permission.health.WRITE_MEDICAL_DATA")
    @RequiresExtension(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, 16)
    override suspend fun deleteMedicalResources(request: DeleteMedicalResourcesRequest) {
        withPhrFeatureCheckSuspend(
            HealthConnectClientUpsideDownImpl::class,
            "deleteMedicalResources(request: DeleteMedicalResourcesRequest)",
        ) {
            wrapPlatformException {
                suspendCancellableCoroutine { continuation ->
                    healthConnectManager.deleteMedicalResources(
                        request.platformReadMedicalResourcesRequest,
                        executor,
                        continuation.asOutcomeReceiver(),
                    )
                }
            }
        }
    }

    private suspend fun <T> wrapPlatformException(function: suspend () -> T): T {
        return try {
            function()
        } catch (e: HealthConnectException) {
            throw e.toKtException()
        }
    }
}
