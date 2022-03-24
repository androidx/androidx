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
import androidx.health.platform.client.proto.ResponseProto
import androidx.health.platform.client.request.AggregateDataRequest
import androidx.health.platform.client.request.DeleteDataRangeRequest
import androidx.health.platform.client.request.DeleteDataRequest
import androidx.health.platform.client.request.GetChangesRequest
import androidx.health.platform.client.request.GetChangesTokenRequest
import androidx.health.platform.client.request.ReadDataRangeRequest
import androidx.health.platform.client.request.ReadDataRequest
import androidx.health.platform.client.request.RequestContext
import androidx.health.platform.client.request.UpsertDataRequest
import androidx.health.platform.client.response.AggregateDataResponse
import androidx.health.platform.client.response.GetChangesResponse
import androidx.health.platform.client.response.GetChangesTokenResponse
import androidx.health.platform.client.response.InsertDataResponse
import androidx.health.platform.client.response.ReadDataRangeResponse
import androidx.health.platform.client.response.ReadDataResponse
import androidx.health.platform.client.service.IAggregateDataCallback
import androidx.health.platform.client.service.IDeleteDataCallback
import androidx.health.platform.client.service.IDeleteDataRangeCallback
import androidx.health.platform.client.service.IGetChangesCallback
import androidx.health.platform.client.service.IGetChangesTokenCallback
import androidx.health.platform.client.service.IGetGrantedPermissionsCallback
import androidx.health.platform.client.service.IHealthDataService
import androidx.health.platform.client.service.IInsertDataCallback
import androidx.health.platform.client.service.IReadDataCallback
import androidx.health.platform.client.service.IReadDataRangeCallback
import androidx.health.platform.client.service.IRevokeAllPermissionsCallback
import androidx.health.platform.client.service.IUpdateDataCallback

/** Fake {@link IHealthDataService} implementation for unit testing. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class FakeHealthDataService : IHealthDataService.Stub() {
    /** Change this state to control permission responses. Not thread safe */
    val grantedPermissions: MutableSet<Permission> = mutableSetOf()

    /** State retaining last requested parameters. */
    var lastUpsertDataRequest: UpsertDataRequest? = null
    var lastReadDataRequest: ReadDataRequest? = null
    var lastReadDataRangeRequest: ReadDataRangeRequest? = null

    /** State for returned responses. */
    var insertDataResponse: InsertDataResponse? = null
    var readDataResponse: ReadDataResponse? = null
    var readDataRangeResponse: ReadDataRangeResponse? = null

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
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@getGrantedPermissions
        }

        val granted = permissions.filter { it in grantedPermissions }.toList()
        callback.onSuccess(granted)
    }

    override fun revokeAllPermissions(
        context: RequestContext,
        callback: IRevokeAllPermissionsCallback,
    ) {
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
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@aggregate
        }
        callback.onSuccess(
            AggregateDataResponse(ResponseProto.AggregateDataResponse.getDefaultInstance())
        )
    }

    override fun getChangesToken(
        context: RequestContext,
        request: GetChangesTokenRequest,
        callback: IGetChangesTokenCallback,
    ) {
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@getChangesToken
        }
        callback.onSuccess(
            GetChangesTokenResponse(ResponseProto.GetChangesTokenResponse.getDefaultInstance())
        )
    }

    override fun getChanges(
        context: RequestContext,
        request: GetChangesRequest,
        callback: IGetChangesCallback,
    ) {
        errorCode?.let {
            callback.onError(ErrorStatus.create(it, "" + it))
            return@getChanges
        }
        callback.onSuccess(
            GetChangesResponse(ResponseProto.GetChangesResponse.getDefaultInstance())
        )
    }
}
