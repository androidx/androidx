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
package androidx.health.connect.client.impl

import android.content.Context
import android.os.DeadObjectException
import android.os.RemoteException
import android.os.TransactionTooLargeException
import androidx.annotation.VisibleForTesting
import androidx.health.connect.client.ExperimentalDeduplicationApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectClient.Companion.HEALTH_CONNECT_CLIENT_TAG
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.feature.ExperimentalFeatureAvailabilityApi
import androidx.health.connect.client.feature.HealthConnectFeaturesApkImpl
import androidx.health.connect.client.feature.HealthConnectFeaturesUnavailableImpl
import androidx.health.connect.client.impl.converters.aggregate.retrieveAggregateDataRow
import androidx.health.connect.client.impl.converters.aggregate.toAggregateDataRowGroupByDuration
import androidx.health.connect.client.impl.converters.aggregate.toAggregateDataRowGroupByPeriod
import androidx.health.connect.client.impl.converters.datatype.toDataType
import androidx.health.connect.client.impl.converters.datatype.toDataTypeIdPairProtoList
import androidx.health.connect.client.impl.converters.records.toProto
import androidx.health.connect.client.impl.converters.records.toRecord
import androidx.health.connect.client.impl.converters.request.toDeleteDataRangeRequestProto
import androidx.health.connect.client.impl.converters.request.toProto
import androidx.health.connect.client.impl.converters.request.toReadDataRangeRequestProto
import androidx.health.connect.client.impl.converters.request.toReadDataRequestProto
import androidx.health.connect.client.impl.converters.response.toChangesResponse
import androidx.health.connect.client.impl.converters.response.toReadRecordsResponse
import androidx.health.connect.client.permission.HealthPermission.Companion.ALL_PERMISSIONS
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.request.ReadRecordsRequest.Companion.DEDUPLICATION_STRATEGY_DISABLED
import androidx.health.connect.client.response.ChangesResponse
import androidx.health.connect.client.response.InsertRecordsResponse
import androidx.health.connect.client.response.ReadRecordResponse
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.platform.client.HealthDataAsyncClient
import androidx.health.platform.client.HealthDataService
import androidx.health.platform.client.impl.logger.Logger
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.proto.PermissionProto
import androidx.health.platform.client.proto.RequestProto
import kotlin.reflect.KClass
import kotlinx.coroutines.guava.await

/**
 * Kotlin extension implementation that exposes kotlin coroutines rather than guava
 * ListenableFutures.
 */
class HealthConnectClientImpl
@OptIn(ExperimentalFeatureAvailabilityApi::class)
@VisibleForTesting
internal constructor(
    private val delegate: HealthDataAsyncClient,
    override val features: HealthConnectFeatures
) : HealthConnectClient, PermissionController {

    @OptIn(ExperimentalFeatureAvailabilityApi::class)
    internal constructor(
        context: Context,
        providerPackageName: String
    ) : this(
        delegate = HealthDataService.getClient(context, providerPackageName),
        features =
            if (providerPackageName == HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME) {
                HealthConnectFeaturesApkImpl(context, providerPackageName)
            } else {
                HealthConnectFeaturesUnavailableImpl
            }
    )

    override suspend fun getGrantedPermissions(): Set<String> {
        val grantedPermissions = wrapRemoteException {
            delegate
                .filterGrantedPermissions(
                    ALL_PERMISSIONS.map {
                            PermissionProto.Permission.newBuilder().setPermission(it).build()
                        }
                        .toSet()
                )
                .await()
                .map { it.permission }
                .toSet()
        }
        Logger.debug(
            HEALTH_CONNECT_CLIENT_TAG,
            "Granted ${grantedPermissions.size} out of ${ALL_PERMISSIONS.size} permissions."
        )
        return grantedPermissions
    }

    override suspend fun revokeAllPermissions() {
        wrapRemoteException { delegate.revokeAllPermissions().await() }
        Logger.debug(HEALTH_CONNECT_CLIENT_TAG, "Revoked all permissions.")
    }

    override val permissionController: PermissionController
        get() = this

    override suspend fun insertRecords(records: List<Record>): InsertRecordsResponse {
        val uidList = wrapRemoteException {
            delegate.insertData(records.map { it.toProto() }).await()
        }
        Logger.debug(HEALTH_CONNECT_CLIENT_TAG, "${records.size} records inserted.")
        return InsertRecordsResponse(recordIdsList = uidList)
    }

    override suspend fun updateRecords(records: List<Record>) {
        wrapRemoteException { delegate.updateData(records.map { it.toProto() }).await() }
        Logger.debug(HEALTH_CONNECT_CLIENT_TAG, "${records.size} records updated.")
    }

    override suspend fun deleteRecords(
        recordType: KClass<out Record>,
        recordIdsList: List<String>,
        clientRecordIdsList: List<String>,
    ) {
        wrapRemoteException {
            delegate
                .deleteData(
                    toDataTypeIdPairProtoList(recordType, recordIdsList),
                    toDataTypeIdPairProtoList(recordType, clientRecordIdsList)
                )
                .await()
        }
        Logger.debug(
            HEALTH_CONNECT_CLIENT_TAG,
            "${recordIdsList.size + clientRecordIdsList.size} records deleted."
        )
    }

    override suspend fun deleteRecords(
        recordType: KClass<out Record>,
        timeRangeFilter: TimeRangeFilter,
    ) {
        wrapRemoteException {
            delegate
                .deleteDataRange(toDeleteDataRangeRequestProto(recordType, timeRangeFilter))
                .await()
        }
        Logger.debug(HEALTH_CONNECT_CLIENT_TAG, "Records deletion successful.")
    }

    @Suppress("UNCHECKED_CAST") // Safe to cast as the type should match
    override suspend fun <T : Record> readRecord(
        recordType: KClass<T>,
        recordId: String,
    ): ReadRecordResponse<T> {
        val proto = wrapRemoteException {
            delegate.readData(toReadDataRequestProto(recordType, recordId)).await()
        }
        val response = ReadRecordResponse(toRecord(proto) as T)
        Logger.debug(HEALTH_CONNECT_CLIENT_TAG, "Reading record of $recordId successful.")
        return response
    }

    override suspend fun getChangesToken(request: ChangesTokenRequest): String {
        val proto = wrapRemoteException {
            delegate
                .getChangesToken(
                    RequestProto.GetChangesTokenRequest.newBuilder()
                        .addAllDataType(request.recordTypes.map { it.toDataType() })
                        .addAllDataOriginFilters(
                            request.dataOriginFilters.map {
                                DataProto.DataOrigin.newBuilder()
                                    .setApplicationId(it.packageName)
                                    .build()
                            }
                        )
                        .build()
                )
                .await()
        }
        val changeToken = proto.changesToken
        Logger.debug(HEALTH_CONNECT_CLIENT_TAG, "Retrieved change token $changeToken.")
        return changeToken
    }

    override suspend fun getChanges(changesToken: String): ChangesResponse {
        val proto = wrapRemoteException {
            delegate
                .getChanges(
                    RequestProto.GetChangesRequest.newBuilder()
                        .setChangesToken(changesToken)
                        .build()
                )
                .await()
        }
        val nextToken = proto.nextChangesToken
        Logger.debug(
            HEALTH_CONNECT_CLIENT_TAG,
            "Retrieved changes successful with $changesToken, next token $nextToken."
        )
        return toChangesResponse(proto)
    }

    @OptIn(ExperimentalDeduplicationApi::class)
    override suspend fun <T : Record> readRecords(
        request: ReadRecordsRequest<T>,
    ): ReadRecordsResponse<T> {
        if (request.deduplicateStrategy != DEDUPLICATION_STRATEGY_DISABLED) {
            TODO("Not yet implemented")
        }
        val proto = wrapRemoteException {
            delegate.readDataRange(toReadDataRangeRequestProto(request)).await()
        }
        val response = toReadRecordsResponse<T>(proto)
        Logger.debug(HEALTH_CONNECT_CLIENT_TAG, "Retrieve records successful.")
        return response
    }

    override suspend fun aggregate(request: AggregateRequest): AggregationResult {
        val responseProto = wrapRemoteException { delegate.aggregate(request.toProto()).await() }
        val result = responseProto.rowsList.first().retrieveAggregateDataRow()
        val numberOfMetrics = result.longValues.size + result.doubleValues.size
        Logger.debug(HEALTH_CONNECT_CLIENT_TAG, "Retrieved $numberOfMetrics metrics.")
        return result
    }

    override suspend fun aggregateGroupByDuration(
        request: AggregateGroupByDurationRequest,
    ): List<AggregationResultGroupedByDuration> {
        val responseProto = wrapRemoteException { delegate.aggregate(request.toProto()).await() }
        val result = responseProto.rowsList.map { it.toAggregateDataRowGroupByDuration() }.toList()
        Logger.debug(
            HEALTH_CONNECT_CLIENT_TAG,
            "Retrieved ${result.size} duration aggregation buckets."
        )
        return result
    }

    override suspend fun aggregateGroupByPeriod(
        request: AggregateGroupByPeriodRequest
    ): List<AggregationResultGroupedByPeriod> {
        val responseProto = wrapRemoteException { delegate.aggregate(request.toProto()).await() }
        val result = responseProto.rowsList.map { it.toAggregateDataRowGroupByPeriod() }.toList()
        Logger.debug(
            HEALTH_CONNECT_CLIENT_TAG,
            "Retrieved ${result.size} period aggregation buckets."
        )
        return result
    }

    /**
     * Wraps any thrown [RemoteException] with a new instance, such that stack traces indicate which
     * API call failed.
     */
    private inline fun <T> wrapRemoteException(function: () -> T): T {
        try {
            return function()
        } catch (e: RemoteException) {
            val wrapper =
                when (e) {
                    is DeadObjectException -> DeadObjectException(e.message)
                    is TransactionTooLargeException -> TransactionTooLargeException(e.message)
                    else -> RemoteException(e.message)
                }
            wrapper.initCause(e)
            throw wrapper
        }
    }
}
