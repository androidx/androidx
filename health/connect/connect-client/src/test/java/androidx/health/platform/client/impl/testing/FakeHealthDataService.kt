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
package androidx.health.platform.client.impl.testing

import androidx.annotation.RestrictTo
import androidx.health.platform.client.error.ErrorCode
import androidx.health.platform.client.error.ErrorStatus
import androidx.health.platform.client.permission.Permission
import androidx.health.platform.client.request.AggregateDataRequest
import androidx.health.platform.client.request.DeleteDataRangeRequest
import androidx.health.platform.client.request.DeleteDataRequest
import androidx.health.platform.client.request.GetChangesRequest
import androidx.health.platform.client.request.GetChangesTokenRequest
import androidx.health.platform.client.request.ReadDataRangeRequest
import androidx.health.platform.client.request.ReadDataRequest
import androidx.health.platform.client.request.ReadExerciseRouteRequest
import androidx.health.platform.client.request.RegisterForDataNotificationsRequest
import androidx.health.platform.client.request.RequestContext
import androidx.health.platform.client.request.UnregisterFromDataNotificationsRequest
import androidx.health.platform.client.request.UpsertDataRequest
import androidx.health.platform.client.request.UpsertExerciseRouteRequest
import androidx.health.platform.client.response.AggregateDataResponse
import androidx.health.platform.client.response.GetChangesResponse
import androidx.health.platform.client.response.GetChangesTokenResponse
import androidx.health.platform.client.response.InsertDataResponse
import androidx.health.platform.client.response.ReadDataRangeResponse
import androidx.health.platform.client.response.ReadDataResponse
import androidx.health.platform.client.response.ReadExerciseRouteResponse
import androidx.health.platform.client.service.IAggregateDataCallback
import androidx.health.platform.client.service.IDeleteDataCallback
import androidx.health.platform.client.service.IDeleteDataRangeCallback
import androidx.health.platform.client.service.IFilterGrantedPermissionsCallback
import androidx.health.platform.client.service.IGetChangesCallback
import androidx.health.platform.client.service.IGetChangesTokenCallback
import androidx.health.platform.client.service.IGetGrantedPermissionsCallback
import androidx.health.platform.client.service.IHealthDataService
import androidx.health.platform.client.service.IInsertDataCallback
import androidx.health.platform.client.service.IReadDataCallback
import androidx.health.platform.client.service.IReadDataRangeCallback
import androidx.health.platform.client.service.IReadExerciseRouteCallback
import androidx.health.platform.client.service.IRegisterForDataNotificationsCallback
import androidx.health.platform.client.service.IRevokeAllPermissionsCallback
import androidx.health.platform.client.service.IUnregisterFromDataNotificationsCallback
import androidx.health.platform.client.service.IUpdateDataCallback
import androidx.health.platform.client.service.IUpsertExerciseRouteCallback

/** Fake {@link IHealthDataService} implementation for unit testing. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class FakeHealthDataService : IHealthDataService.Stub() {
    /** Change this state to control permission responses. Not thread safe */
    private val grantedPermissions: MutableSet<Permission> = mutableSetOf()

    /** State retaining last requested parameters. */
    var lastRequestContext: RequestContext? = null
    var lastUpsertDataRequest: UpsertDataRequest? = null
    var lastUpsertExerciseRouteRequest: UpsertExerciseRouteRequest? = null
    var lastReadDataRequest: ReadDataRequest? = null
    var lastReadDataRangeRequest: ReadDataRangeRequest? = null
    var lastReadExerciseRouteRequest: ReadExerciseRouteRequest? = null
    var lastDeleteDataRequest: DeleteDataRequest? = null
    var lastDeleteDataRangeRequest: DeleteDataRangeRequest? = null
    var lastAggregateRequest: AggregateDataRequest? = null
    var lastGetChangesTokenRequest: GetChangesTokenRequest? = null
    var lastGetChangesRequest: GetChangesRequest? = null
    var lastRegisterForDataNotificationsRequest: RegisterForDataNotificationsRequest? = null
    var lastUnregisterFromDataNotificationsRequest: UnregisterFromDataNotificationsRequest? = null

    /** State for returned responses. */
    var insertDataResponse: InsertDataResponse? = null
    var readDataResponse: ReadDataResponse? = null
    var readDataRangeResponse: ReadDataRangeResponse? = null
    var readExerciseRouteResponse: ReadExerciseRouteResponse? = null
    var aggregateDataResponse: AggregateDataResponse? = null
    var changesTokenResponse: GetChangesTokenResponse? = null
    var changesResponse: GetChangesResponse? = null

    /** Set this to control error responses. Not thread safe. */
    @ErrorCode var errorCode: Int? = null

    override fun getApiVersion(): Int {
        return 42
    }

    override fun getGrantedPermissions(
        context: RequestContext,
        permissions: List<Permission>,
        callback: IGetGrantedPermissionsCallback,
    ) {
        lastRequestContext = context
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@getGrantedPermissions
        }

        val granted = permissions.filter { it in grantedPermissions }.toList()
        callback.onSuccess(granted)
    }

    override fun filterGrantedPermissions(
        context: RequestContext,
        permissions: List<Permission>,
        callback: IFilterGrantedPermissionsCallback,
    ) {
        lastRequestContext = context
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@filterGrantedPermissions
        }

        val granted = permissions.filter { it in grantedPermissions }.toList()
        callback.onSuccess(granted)
    }

    override fun revokeAllPermissions(
        context: RequestContext,
        callback: IRevokeAllPermissionsCallback,
    ) {
        lastRequestContext = context
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@revokeAllPermissions
        }
        grantedPermissions.clear()
        callback.onSuccess()
    }

    fun addGrantedPermission(permission: Permission) {
        grantedPermissions.add(permission)
    }

    override fun insertData(
        context: RequestContext,
        request: UpsertDataRequest,
        callback: IInsertDataCallback,
    ) {
        lastRequestContext = context
        lastUpsertDataRequest = request
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@insertData
        }
        callback.onSuccess(insertDataResponse)
    }

    override fun updateData(
        context: RequestContext,
        request: UpsertDataRequest,
        callback: IUpdateDataCallback,
    ) {
        lastRequestContext = context
        lastUpsertDataRequest = request
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@updateData
        }
        callback.onSuccess()
    }

    override fun deleteData(
        context: RequestContext,
        request: DeleteDataRequest,
        callback: IDeleteDataCallback,
    ) {
        lastRequestContext = context
        lastDeleteDataRequest = request
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@deleteData
        }
        callback.onSuccess()
    }

    override fun deleteDataRange(
        context: RequestContext,
        request: DeleteDataRangeRequest,
        callback: IDeleteDataRangeCallback,
    ) {
        lastRequestContext = context
        lastDeleteDataRangeRequest = request
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@deleteDataRange
        }
        callback.onSuccess()
    }

    override fun readData(
        context: RequestContext,
        request: ReadDataRequest,
        callback: IReadDataCallback,
    ) {
        lastRequestContext = context
        lastReadDataRequest = request
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@readData
        }
        callback.onSuccess(checkNotNull(readDataResponse))
    }

    override fun readDataRange(
        context: RequestContext,
        request: ReadDataRangeRequest,
        callback: IReadDataRangeCallback,
    ) {
        lastRequestContext = context
        lastReadDataRangeRequest = request
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@readDataRange
        }
        callback.onSuccess(checkNotNull(readDataRangeResponse))
    }

    override fun aggregate(
        context: RequestContext,
        request: AggregateDataRequest,
        callback: IAggregateDataCallback,
    ) {
        lastRequestContext = context
        lastAggregateRequest = request
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@aggregate
        }
        callback.onSuccess(aggregateDataResponse)
    }

    override fun getChangesToken(
        context: RequestContext,
        request: GetChangesTokenRequest,
        callback: IGetChangesTokenCallback,
    ) {
        lastRequestContext = context
        lastGetChangesTokenRequest = request
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@getChangesToken
        }
        callback.onSuccess(changesTokenResponse)
    }

    override fun getChanges(
        context: RequestContext,
        request: GetChangesRequest,
        callback: IGetChangesCallback,
    ) {
        lastRequestContext = context
        lastGetChangesRequest = request
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@getChanges
        }
        callback.onSuccess(changesResponse)
    }

    override fun registerForDataNotifications(
        context: RequestContext,
        request: RegisterForDataNotificationsRequest,
        callback: IRegisterForDataNotificationsCallback,
    ) {
        lastRequestContext = context
        lastRegisterForDataNotificationsRequest = request
        errorCode?.also {
            callback.onError(ErrorStatus.create(errorCode = it))
            return@registerForDataNotifications
        }
        callback.onSuccess()
    }

    override fun unregisterFromDataNotifications(
        context: RequestContext,
        request: UnregisterFromDataNotificationsRequest,
        callback: IUnregisterFromDataNotificationsCallback,
    ) {
        lastRequestContext = context
        lastUnregisterFromDataNotificationsRequest = request
        errorCode?.also {
            callback.onError(ErrorStatus.create(errorCode = it))
            return@unregisterFromDataNotifications
        }
        callback.onSuccess()
    }

    override fun upsertExerciseRoute(
        context: RequestContext,
        request: UpsertExerciseRouteRequest,
        callback: IUpsertExerciseRouteCallback,
    ) {
        lastUpsertExerciseRouteRequest = request
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@upsertExerciseRoute
        }
        callback.onSuccess()
    }

    override fun readExerciseRoute(
        context: RequestContext,
        request: ReadExerciseRouteRequest,
        callback: IReadExerciseRouteCallback,
    ) {
        lastReadExerciseRouteRequest = request
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@readExerciseRoute
        }
        callback.onSuccess(checkNotNull(readExerciseRouteResponse))
    }
}
