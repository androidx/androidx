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
package androidx.health.platform.client.impl

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.health.platform.client.HealthDataAsyncClient
import androidx.health.platform.client.SdkConfig
import androidx.health.platform.client.impl.internal.ProviderConnectionManager
import androidx.health.platform.client.impl.ipc.Client
import androidx.health.platform.client.impl.ipc.ClientConfiguration
import androidx.health.platform.client.impl.ipc.internal.ConnectionManager
import androidx.health.platform.client.impl.permission.token.PermissionTokenManager
import androidx.health.platform.client.permission.Permission
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.proto.PermissionProto
import androidx.health.platform.client.proto.RequestProto
import androidx.health.platform.client.proto.ResponseProto
import androidx.health.platform.client.request.AggregateDataRequest
import androidx.health.platform.client.request.DeleteDataRangeRequest
import androidx.health.platform.client.request.DeleteDataRequest
import androidx.health.platform.client.request.ReadDataRangeRequest
import androidx.health.platform.client.request.ReadDataRequest
import androidx.health.platform.client.request.RequestContext
import androidx.health.platform.client.request.UpsertDataRequest
import androidx.health.platform.client.service.IHealthDataService
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors

/** An IPC backed HealthDataClient implementation. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("RestrictedApi")
class ServiceBackedHealthDataClient(
    private val context: Context,
    clientConfiguration: ClientConfiguration,
    connectionManager: ConnectionManager,
) :
    Client<IHealthDataService>(
        clientConfiguration,
        connectionManager,
        IHealthDataService.Stub::asInterface,
        IHealthDataService::getApiVersion
    ),
    HealthDataAsyncClient {

    private val callingPackageName = context.packageName
    private val executorService = Executors.newSingleThreadScheduledExecutor()

    @get:SuppressLint("RestrictedApi") // Use permission token manager internal
    val backgroundRequestContext: RequestContext
        get() =
            RequestContext(
                callingPackageName,
                SdkConfig.SDK_VERSION,
                PermissionTokenManager.getCurrentToken(context),
                false
            )
    val requestContext: RequestContext
        get() =
            RequestContext(
                callingPackageName,
                SdkConfig.SDK_VERSION,
                PermissionTokenManager.getCurrentToken(context),
                true
            )

    constructor(
        context: Context,
        clientConfiguration: ClientConfiguration,
    ) : this(context, clientConfiguration, ProviderConnectionManager.getInstance(context))

    override fun getGrantedPermissions(
        permissions: Set<PermissionProto.Permission>,
    ): ListenableFuture<Set<PermissionProto.Permission>> {
        return executeWithVersionCheck(
            { service, resultFuture ->
                service.getGrantedPermissions(
                    requestContext,
                    permissions.map { Permission(it) }.toList(),
                    GetGrantedPermissionsCallback(resultFuture)
                )
            },
            IHealthDataService.MIN_API_VERSION
        )
    }

    override fun revokeAllPermissions(): ListenableFuture<Unit> {
        return executeWithVersionCheck(
            { service, resultFuture ->
                service.revokeAllPermissions(
                    requestContext,
                    RevokeAllPermissionsCallback(resultFuture)
                )
            },
            IHealthDataService.MIN_API_VERSION
        )
    }

    override fun insertData(
        dataCollection: List<DataProto.DataPoint>
    ): ListenableFuture<List<String>> {
        val request = UpsertDataRequest(dataCollection)
        return executeWithVersionCheck(
            { service, resultFuture ->
                service.insertData(requestContext, request, InsertDataCallback(resultFuture))
            },
            IHealthDataService.MIN_API_VERSION
        )
    }

    override fun updateData(dataCollection: List<DataProto.DataPoint>): ListenableFuture<Unit> {
        val request = UpsertDataRequest(dataCollection)
        return executeWithVersionCheck(
            { service, resultFuture ->
                service.updateData(requestContext, request, UpdateDataCallback(resultFuture))
            },
            IHealthDataService.MIN_API_VERSION
        )
    }

    override fun deleteData(
        uidsCollection: List<RequestProto.DataTypeIdPair>,
        clientIdsCollection: List<RequestProto.DataTypeIdPair>
    ): ListenableFuture<Unit> {
        val request = DeleteDataRequest(uidsCollection, clientIdsCollection)
        return executeWithVersionCheck(
            { service, resultFuture ->
                service.deleteData(requestContext, request, DeleteDataCallback(resultFuture))
            },
            IHealthDataService.MIN_API_VERSION
        )
    }

    override fun deleteDataRange(
        dataCollection: RequestProto.DeleteDataRangeRequest
    ): ListenableFuture<Unit> {
        val request = DeleteDataRangeRequest(dataCollection)
        return executeWithVersionCheck(
            { service, resultFuture ->
                service.deleteDataRange(
                    requestContext,
                    request,
                    DeleteDataRangeCallback(resultFuture)
                )
            },
            IHealthDataService.MIN_API_VERSION
        )
    }

    override fun readData(
        dataCollection: RequestProto.ReadDataRequest
    ): ListenableFuture<DataProto.DataPoint> {
        val request = ReadDataRequest(dataCollection)
        return executeWithVersionCheck(
            { service, resultFuture ->
                service.readData(requestContext, request, ReadDataCallback(resultFuture))
            },
            IHealthDataService.MIN_API_VERSION
        )
    }

    override fun readDataRange(
        dataCollection: RequestProto.ReadDataRangeRequest
    ): ListenableFuture<ResponseProto.ReadDataRangeResponse> {
        val request = ReadDataRangeRequest(dataCollection)
        return executeWithVersionCheck(
            { service, resultFuture ->
                service.readDataRange(requestContext, request, ReadDataRangeCallback(resultFuture))
            },
            IHealthDataService.MIN_API_VERSION
        )
    }

    override fun aggregate(
        request: RequestProto.AggregateDataRequest
    ): ListenableFuture<ResponseProto.AggregateDataResponse> {
        return executeWithVersionCheck(
            { service, resultFuture ->
                service.aggregate(
                    requestContext,
                    AggregateDataRequest(request),
                    AggregateDataCallback(resultFuture)
                )
            },
            IHealthDataService.MIN_API_VERSION
        )
    }
}
