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
package androidx.health.platform.client

import androidx.health.platform.client.permission.Permission
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.proto.PermissionProto
import androidx.health.platform.client.proto.RequestProto
import androidx.health.platform.client.proto.ResponseProto
import com.google.common.util.concurrent.ListenableFuture

/**
 * Interface to access health and fitness records.
 *
 * Like [HealthDataClient] but expose ListenableFuture instead of kotlin coroutines.
 */
interface HealthDataAsyncClient {
    /**
     * Returns a set of [Permission] granted by the user to this app, out of the input [Permission]
     * set.
     */
    fun getGrantedPermissions(
        permissions: Set<PermissionProto.Permission>
    ): ListenableFuture<Set<PermissionProto.Permission>>

    /** Allows an app to relinquish app permissions granted to itself by calling this method. */
    fun revokeAllPermissions(): ListenableFuture<Unit>

    fun insertData(dataCollection: List<DataProto.DataPoint>): ListenableFuture<List<String>>

    fun updateData(dataCollection: List<DataProto.DataPoint>): ListenableFuture<Unit>

    fun deleteData(
        uidsCollection: List<RequestProto.DataTypeIdPair>,
        clientIdsCollection: List<RequestProto.DataTypeIdPair>
    ): ListenableFuture<Unit>

    fun deleteDataRange(dataCollection: RequestProto.DeleteDataRangeRequest): ListenableFuture<Unit>

    fun readData(
        dataCollection: RequestProto.ReadDataRequest
    ): ListenableFuture<DataProto.DataPoint>

    fun readDataRange(
        dataCollection: RequestProto.ReadDataRangeRequest
    ): ListenableFuture<ResponseProto.ReadDataRangeResponse>

    fun aggregate(
        request: RequestProto.AggregateDataRequest
    ): ListenableFuture<ResponseProto.AggregateDataResponse>

    fun getChangesToken(
        request: RequestProto.GetChangesTokenRequest
    ): ListenableFuture<ResponseProto.GetChangesTokenResponse>
    fun getChanges(
        request: RequestProto.GetChangesRequest
    ): ListenableFuture<ResponseProto.GetChangesResponse>
}
