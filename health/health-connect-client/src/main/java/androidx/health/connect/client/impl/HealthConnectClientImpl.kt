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

import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.impl.converters.aggregate.retrieveAggregateDataRow
import androidx.health.connect.client.impl.converters.aggregate.toAggregateDataRowGroupByDuration
import androidx.health.connect.client.impl.converters.aggregate.toAggregateDataRowGroupByPeriod
import androidx.health.connect.client.impl.converters.datatype.toDataTypeIdPairProtoList
import androidx.health.connect.client.impl.converters.datatype.toDataTypeName
import androidx.health.connect.client.impl.converters.permission.toJetpackPermission
import androidx.health.connect.client.impl.converters.permission.toProtoPermission
import androidx.health.connect.client.impl.converters.records.toProto
import androidx.health.connect.client.impl.converters.records.toRecord
import androidx.health.connect.client.impl.converters.request.toDeleteDataRangeRequestProto
import androidx.health.connect.client.impl.converters.request.toProto
import androidx.health.connect.client.impl.converters.request.toReadDataRangeRequestProto
import androidx.health.connect.client.impl.converters.request.toReadDataRequestProto
import androidx.health.connect.client.impl.converters.response.toChangesResponse
import androidx.health.connect.client.impl.converters.response.toReadRecordsResponse
import androidx.health.connect.client.permission.Permission
import androidx.health.connect.client.permission.createHealthDataRequestPermissions
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
import androidx.health.platform.client.HealthDataAsyncClient
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.proto.RequestProto
import kotlin.reflect.KClass
import kotlinx.coroutines.guava.await

/**
 * Kotlin extension implementation that exposes kotlin coroutines rather than guava
 * ListenableFutures.
 *
 * @suppress
 */
class HealthConnectClientImpl
internal constructor(
    private val providerPackageName: String,
    private val delegate: HealthDataAsyncClient,
) : HealthConnectClient, PermissionController {

    override fun createRequestPermissionActivityContract():
        ActivityResultContract<Set<Permission>, Set<Permission>> =
        createHealthDataRequestPermissions(providerPackageName = providerPackageName)

    override suspend fun getGrantedPermissions(permissions: Set<Permission>): Set<Permission> {
        return delegate
            .getGrantedPermissions(permissions.map { it.toProtoPermission() }.toSet())
            .await()
            .map { it.toJetpackPermission() }
            .toSet()
    }

    override suspend fun revokeAllPermissions() {
        return delegate.revokeAllPermissions().await()
    }

    override val permissionController: PermissionController
        get() = this

    override suspend fun insertRecords(records: List<Record>): InsertRecordsResponse {
        val uidList = delegate.insertData(records.map { it.toProto() }).await()
        return InsertRecordsResponse(recordUidsList = uidList)
    }

    override suspend fun updateRecords(records: List<Record>) {
        delegate.updateData(records.map { it.toProto() }).await()
    }

    override suspend fun deleteRecords(
        recordType: KClass<out Record>,
        uidsList: List<String>,
        clientIdsList: List<String>,
    ) {
        delegate
            .deleteData(
                toDataTypeIdPairProtoList(recordType, uidsList),
                toDataTypeIdPairProtoList(recordType, clientIdsList)
            )
            .await()
    }

    override suspend fun deleteRecords(
        recordType: KClass<out Record>,
        timeRangeFilter: TimeRangeFilter,
    ) {
        delegate.deleteDataRange(toDeleteDataRangeRequestProto(recordType, timeRangeFilter)).await()
    }

    @Suppress("UNCHECKED_CAST") // Safe to cast as the type should match
    override suspend fun <T : Record> readRecord(
        recordType: KClass<T>,
        uid: String,
    ): ReadRecordResponse<T> {
        val proto = delegate.readData(toReadDataRequestProto(recordType, uid)).await()
        return ReadRecordResponse(toRecord(proto) as T)
    }

    override suspend fun getChangesToken(request: ChangesTokenRequest): String {
        val proto =
            delegate
                .getChangesToken(
                    RequestProto.GetChangesTokenRequest.newBuilder()
                        .addAllDataType(
                            request.recordTypes.map {
                                DataProto.DataType.newBuilder().setName(it.toDataTypeName()).build()
                            }
                        )
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
        return proto.changesToken
    }

    override suspend fun getChanges(changesToken: String): ChangesResponse {
        val proto =
            delegate
                .getChanges(
                    RequestProto.GetChangesRequest.newBuilder()
                        .setChangesToken(changesToken)
                        .build()
                )
                .await()
        return toChangesResponse(proto)
    }

    override suspend fun <T : Record> readRecords(
        request: ReadRecordsRequest<T>,
    ): ReadRecordsResponse<T> {
        val proto = delegate.readDataRange(toReadDataRangeRequestProto(request)).await()
        return toReadRecordsResponse(proto)
    }

    override suspend fun aggregate(request: AggregateRequest): AggregationResult {
        val responseProto = delegate.aggregate(request.toProto()).await()
        return responseProto.rowsList.first().retrieveAggregateDataRow()
    }

    override suspend fun aggregateGroupByDuration(
        request: AggregateGroupByDurationRequest,
    ): List<AggregationResultGroupedByDuration> {
        val responseProto = delegate.aggregate(request.toProto()).await()
        return responseProto.rowsList.map { it.toAggregateDataRowGroupByDuration() }.toList()
    }

    override suspend fun aggregateGroupByPeriod(
        request: AggregateGroupByPeriodRequest
    ): List<AggregationResultGroupedByPeriod> {
        val responseProto = delegate.aggregate(request.toProto()).await()
        return responseProto.rowsList.map { it.toAggregateDataRowGroupByPeriod() }.toList()
    }
}
