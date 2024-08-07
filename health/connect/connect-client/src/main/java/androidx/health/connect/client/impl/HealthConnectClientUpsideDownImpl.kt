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
import androidx.annotation.VisibleForTesting
import androidx.core.os.asOutcomeReceiver
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.feature.ExperimentalFeatureAvailabilityApi
import androidx.health.connect.client.feature.HealthConnectFeaturesPlatformImpl
import androidx.health.connect.client.impl.platform.aggregate.aggregateFallback
import androidx.health.connect.client.impl.platform.aggregate.platformMetrics
import androidx.health.connect.client.impl.platform.aggregate.plus
import androidx.health.connect.client.impl.platform.records.toPlatformRecord
import androidx.health.connect.client.impl.platform.records.toPlatformRecordClass
import androidx.health.connect.client.impl.platform.records.toSdkRecord
import androidx.health.connect.client.impl.platform.request.toPlatformLocalTimeRangeFilter
import androidx.health.connect.client.impl.platform.request.toPlatformRequest
import androidx.health.connect.client.impl.platform.request.toPlatformTimeRangeFilter
import androidx.health.connect.client.impl.platform.response.toKtResponse
import androidx.health.connect.client.impl.platform.response.toSdkResponse
import androidx.health.connect.client.impl.platform.toKtException
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_PREFIX
import androidx.health.connect.client.records.Record
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
import kotlin.reflect.KClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine

/** Implements the [HealthConnectClient] with APIs in UpsideDownCake. */
@RequiresApi(api = 34)
@OptIn(ExperimentalFeatureAvailabilityApi::class)
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
                    continuation.asOutcomeReceiver()
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
                    continuation.asOutcomeReceiver()
                )
            }
        }
    }

    override suspend fun deleteRecords(
        recordType: KClass<out Record>,
        recordIdsList: List<String>,
        clientRecordIdsList: List<String>
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
                                    it
                                )
                            )
                        }
                    },
                    executor,
                    continuation.asOutcomeReceiver()
                )
            }
        }
    }

    override suspend fun deleteRecords(
        recordType: KClass<out Record>,
        timeRangeFilter: TimeRangeFilter
    ) {
        wrapPlatformException {
            suspendCancellableCoroutine { continuation ->
                healthConnectManager.deleteRecords(
                    recordType.toPlatformRecordClass(),
                    timeRangeFilter.toPlatformTimeRangeFilter(),
                    executor,
                    continuation.asOutcomeReceiver()
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST") // Safe to cast as the type should match
    override suspend fun <T : Record> readRecord(
        recordType: KClass<T>,
        recordId: String
    ): ReadRecordResponse<T> {
        val response = wrapPlatformException {
            suspendCancellableCoroutine { continuation ->
                healthConnectManager.readRecords(
                    ReadRecordsRequestUsingIds.Builder(recordType.toPlatformRecordClass())
                        .addId(recordId)
                        .build(),
                    executor,
                    continuation.asOutcomeReceiver()
                )
            }
        }
        if (response.records.isEmpty()) {
            throw RemoteException("No records")
        }
        return ReadRecordResponse(response.records[0].toSdkRecord() as T)
    }

    @Suppress("UNCHECKED_CAST") // Safe to cast as the type should match
    override suspend fun <T : Record> readRecords(
        request: ReadRecordsRequest<T>
    ): ReadRecordsResponse<T> {
        val response = wrapPlatformException {
            suspendCancellableCoroutine { continuation ->
                healthConnectManager.readRecords(
                    request.toPlatformRequest(),
                    executor,
                    continuation.asOutcomeReceiver()
                )
            }
        }
        return ReadRecordsResponse(
            response.records.map { it.toSdkRecord() as T },
            pageToken = response.nextPageToken.takeUnless { it == -1L }?.toString()
        )
    }

    override suspend fun aggregate(request: AggregateRequest): AggregationResult {
        if (request.metrics.isEmpty()) {
            throw IllegalArgumentException("Requested record types must not be empty.")
        }

        val fallbackResponse = aggregateFallback(request)

        if (request.platformMetrics.isEmpty()) {
            return fallbackResponse
        }

        val platformResponse =
            wrapPlatformException {
                    suspendCancellableCoroutine { continuation ->
                        healthConnectManager.aggregate(
                            request.toPlatformRequest(),
                            executor,
                            continuation.asOutcomeReceiver()
                        )
                    }
                }
                .toSdkResponse(request.platformMetrics)

        return platformResponse + fallbackResponse
    }

    override suspend fun aggregateGroupByDuration(
        request: AggregateGroupByDurationRequest
    ): List<AggregationResultGroupedByDuration> {
        return wrapPlatformException {
                suspendCancellableCoroutine { continuation ->
                    healthConnectManager.aggregateGroupByDuration(
                        request.toPlatformRequest(),
                        request.timeRangeSlicer,
                        executor,
                        continuation.asOutcomeReceiver()
                    )
                }
            }
            .map { it.toSdkResponse(request.metrics) }
    }

    override suspend fun aggregateGroupByPeriod(
        request: AggregateGroupByPeriodRequest
    ): List<AggregationResultGroupedByPeriod> {
        return wrapPlatformException {
                suspendCancellableCoroutine { continuation ->
                    healthConnectManager.aggregateGroupByPeriod(
                        request.toPlatformRequest(),
                        request.timeRangeSlicer,
                        executor,
                        continuation.asOutcomeReceiver()
                    )
                }
            }
            .mapIndexed { index, platformResponse ->
                if (
                    SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10 ||
                        (request.timeRangeSlicer.months == 0 && request.timeRangeSlicer.years == 0)
                ) {
                    platformResponse.toSdkResponse(request.metrics)
                } else {
                    // Handle bug in the Platform for versions of module before SDK extensions 10
                    val requestTimeRangeFilter =
                        request.timeRangeFilter.toPlatformLocalTimeRangeFilter()
                    val bucketStartTime =
                        requestTimeRangeFilter.startTime!!.plus(
                            request.timeRangeSlicer.multipliedBy(index)
                        )
                    val bucketEndTime = bucketStartTime.plus(request.timeRangeSlicer)
                    platformResponse.toSdkResponse(
                        metrics = request.metrics,
                        bucketStartTime = bucketStartTime,
                        bucketEndTime =
                            if (requestTimeRangeFilter.endTime!!.isBefore(bucketEndTime)) {
                                requestTimeRangeFilter.endTime!!
                            } else {
                                bucketEndTime
                            }
                    )
                }
            }
    }

    override suspend fun getChangesToken(request: ChangesTokenRequest): String {
        return wrapPlatformException {
                suspendCancellableCoroutine { continuation ->
                    healthConnectManager.getChangeLogToken(
                        request.toPlatformRequest(),
                        executor,
                        continuation.asOutcomeReceiver()
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
                    continuation.asOutcomeReceiver()
                )
            }
            return ChangesResponse(
                buildList {
                    response.upsertedRecords.forEach { add(UpsertionChange(it.toSdkRecord())) }
                    response.deletedLogs.forEach { add(DeletionChange(it.deletedRecordId)) }
                },
                response.nextChangesToken,
                response.hasMorePages(),
                changesTokenExpired = false
            )
        } catch (e: HealthConnectException) {
            // Handle invalid token
            if (e.errorCode == HealthConnectException.ERROR_INVALID_ARGUMENT) {
                return ChangesResponse(
                    changes = listOf(),
                    nextChangesToken = "",
                    hasMore = false,
                    changesTokenExpired = true
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

    private suspend fun <T> wrapPlatformException(function: suspend () -> T): T {
        return try {
            function()
        } catch (e: HealthConnectException) {
            throw e.toKtException()
        }
    }
}
