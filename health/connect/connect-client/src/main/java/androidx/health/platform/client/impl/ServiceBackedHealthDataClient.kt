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

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.health.platform.client.HealthDataAsyncClient
import androidx.health.platform.client.SdkConfig
import androidx.health.platform.client.impl.internal.ProviderConnectionManager
import androidx.health.platform.client.impl.ipc.Client
import androidx.health.platform.client.impl.ipc.ClientConfiguration
import androidx.health.platform.client.impl.ipc.internal.ConnectionManager
import androidx.health.platform.client.impl.permission.foregroundstate.ForegroundStateChecker
import androidx.health.platform.client.impl.permission.token.PermissionTokenManager
import androidx.health.platform.client.permission.Permission
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.proto.PermissionProto
import androidx.health.platform.client.proto.RequestProto
import androidx.health.platform.client.proto.ResponseProto
import androidx.health.platform.client.request.AggregateDataRequest
import androidx.health.platform.client.request.DeleteDataRangeRequest
import androidx.health.platform.client.request.DeleteDataRequest
import androidx.health.platform.client.request.GetChangesRequest
import androidx.health.platform.client.request.GetChangesTokenRequest
import androidx.health.platform.client.request.ReadDataRangeRequest
import androidx.health.platform.client.request.ReadDataRequest
import androidx.health.platform.client.request.RegisterForDataNotificationsRequest
import androidx.health.platform.client.request.RequestContext
import androidx.health.platform.client.request.UnregisterFromDataNotificationsRequest
import androidx.health.platform.client.request.UpsertDataRequest
import androidx.health.platform.client.service.IHealthDataService
import androidx.health.platform.client.service.IHealthDataService.MIN_API_VERSION
import com.google.common.util.concurrent.ListenableFuture
import kotlin.math.min

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

    private fun getRequestContext(): RequestContext {
        return RequestContext(
            callingPackageName,
            SdkConfig.SDK_VERSION,
            PermissionTokenManager.getCurrentToken(context),
            ForegroundStateChecker.isInForeground()
        )
    }

    constructor(
        context: Context,
        clientConfiguration: ClientConfiguration,
    ) : this(context, clientConfiguration, ProviderConnectionManager.getInstance(context))

    override fun getGrantedPermissions(
        permissions: Set<PermissionProto.Permission>,
    ): ListenableFuture<Set<PermissionProto.Permission>> {
        return executeWithVersionCheck(MIN_API_VERSION) { service, resultFuture ->
            service.getGrantedPermissions(
                getRequestContext(),
                permissions.map { Permission(it) }.toList(),
                GetGrantedPermissionsCallback(resultFuture)
            )
        }
    }

    override fun filterGrantedPermissions(
        permissions: Set<PermissionProto.Permission>,
    ): ListenableFuture<Set<PermissionProto.Permission>> {
        return executeWithVersionCheck(min(MIN_API_VERSION, 5)) { service, resultFuture ->
            service.filterGrantedPermissions(
                getRequestContext(),
                permissions.map { Permission(it) }.toList(),
                FilterGrantedPermissionsCallback(resultFuture)
            )
        }
    }

    override fun revokeAllPermissions(): ListenableFuture<Unit> {
        return executeWithVersionCheck(MIN_API_VERSION) { service, resultFuture ->
            service.revokeAllPermissions(
                getRequestContext(),
                RevokeAllPermissionsCallback(resultFuture)
            )
        }
    }

    override fun insertData(
        dataCollection: List<DataProto.DataPoint>
    ): ListenableFuture<List<String>> {
        val request = UpsertDataRequest(dataCollection)
        return executeWithVersionCheck(MIN_API_VERSION) { service, resultFuture ->
            service.insertData(getRequestContext(), request, InsertDataCallback(resultFuture))
        }
    }

    override fun updateData(dataCollection: List<DataProto.DataPoint>): ListenableFuture<Unit> {
        val request = UpsertDataRequest(dataCollection)
        return executeWithVersionCheck(MIN_API_VERSION) { service, resultFuture ->
            service.updateData(getRequestContext(), request, UpdateDataCallback(resultFuture))
        }
    }

    override fun deleteData(
        uidsCollection: List<RequestProto.DataTypeIdPair>,
        clientIdsCollection: List<RequestProto.DataTypeIdPair>
    ): ListenableFuture<Unit> {
        val request = DeleteDataRequest(uidsCollection, clientIdsCollection)
        return executeWithVersionCheck(MIN_API_VERSION) { service, resultFuture ->
            service.deleteData(getRequestContext(), request, DeleteDataCallback(resultFuture))
        }
    }

    override fun deleteDataRange(
        dataCollection: RequestProto.DeleteDataRangeRequest
    ): ListenableFuture<Unit> {
        val request = DeleteDataRangeRequest(dataCollection)
        return executeWithVersionCheck(MIN_API_VERSION) { service, resultFuture ->
            service.deleteDataRange(
                getRequestContext(),
                request,
                DeleteDataRangeCallback(resultFuture)
            )
        }
    }

    override fun readData(
        dataCollection: RequestProto.ReadDataRequest
    ): ListenableFuture<DataProto.DataPoint> {
        val request = ReadDataRequest(dataCollection)
        return executeWithVersionCheck(MIN_API_VERSION) { service, resultFuture ->
            service.readData(getRequestContext(), request, ReadDataCallback(resultFuture))
        }
    }

    override fun readDataRange(
        dataCollection: RequestProto.ReadDataRangeRequest
    ): ListenableFuture<ResponseProto.ReadDataRangeResponse> {
        val request = ReadDataRangeRequest(dataCollection)
        return executeWithVersionCheck(MIN_API_VERSION) { service, resultFuture ->
            service.readDataRange(getRequestContext(), request, ReadDataRangeCallback(resultFuture))
        }
    }

    override fun aggregate(
        request: RequestProto.AggregateDataRequest
    ): ListenableFuture<ResponseProto.AggregateDataResponse> {
        return executeWithVersionCheck(MIN_API_VERSION) { service, resultFuture ->
            service.aggregate(
                getRequestContext(),
                AggregateDataRequest(request),
                AggregateDataCallback(resultFuture)
            )
        }
    }

    override fun getChangesToken(
        request: RequestProto.GetChangesTokenRequest
    ): ListenableFuture<ResponseProto.GetChangesTokenResponse> {
        return executeWithVersionCheck(MIN_API_VERSION) { service, resultFuture ->
            service.getChangesToken(
                getRequestContext(),
                GetChangesTokenRequest(request),
                GetChangesTokenCallback(resultFuture)
            )
        }
    }

    override fun getChanges(
        request: RequestProto.GetChangesRequest
    ): ListenableFuture<ResponseProto.GetChangesResponse> {
        return executeWithVersionCheck(MIN_API_VERSION) { service, resultFuture ->
            service.getChanges(
                getRequestContext(),
                GetChangesRequest(request),
                GetChangesCallback(resultFuture)
            )
        }
    }

    override fun registerForDataNotifications(
        request: RequestProto.RegisterForDataNotificationsRequest,
    ): ListenableFuture<Void> =
        executeWithVersionCheck(min(MIN_API_VERSION, 2)) { service, resultFuture ->
            service.registerForDataNotifications(
                getRequestContext(),
                RegisterForDataNotificationsRequest(request),
                RegisterForDataNotificationsCallback(resultFuture),
            )
        }

    override fun unregisterFromDataNotifications(
        request: RequestProto.UnregisterFromDataNotificationsRequest,
    ): ListenableFuture<Void> =
        executeWithVersionCheck(min(MIN_API_VERSION, 2)) { service, resultFuture ->
            service.unregisterFromDataNotifications(
                getRequestContext(),
                UnregisterFromDataNotificationsRequest(request),
                UnregisterFromDataNotificationsCallback(resultFuture),
            )
        }
}
