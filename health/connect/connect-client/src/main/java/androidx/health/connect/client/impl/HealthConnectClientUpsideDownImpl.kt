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
import android.healthconnect.HealthConnectException
import android.healthconnect.HealthConnectManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.os.asOutcomeReceiver
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.impl.platform.records.toPlatformRecord
import androidx.health.connect.client.impl.platform.response.toKtResponse
import androidx.health.connect.client.impl.platform.toKtException
import androidx.health.connect.client.permission.HealthPermission
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
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Implements the [HealthConnectClient] with APIs in UpsideDownCake.
 *
 * @suppress
 */
@RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class HealthConnectClientUpsideDownImpl(private val context: Context) :
    HealthConnectClient, PermissionController {

    private val healthConnectManager: HealthConnectManager =
        context.getSystemService(Context.HEALTHCONNECT_SERVICE) as HealthConnectManager

    override val permissionController: PermissionController
        get() = this

    override suspend fun insertRecords(records: List<Record>): InsertRecordsResponse {
        val response = wrapPlatformException {
            suspendCancellableCoroutine<android.healthconnect.InsertRecordsResponse> { continuation
                ->
                healthConnectManager.insertRecords(
                    records.map { it.toPlatformRecord() },
                    Runnable::run,
                    continuation.asOutcomeReceiver()
                )
            }
        }
        return response.toKtResponse()
    }

    override suspend fun updateRecords(records: List<Record>) {
        throw UnsupportedOperationException("Method not supported yet")
    }

    override suspend fun deleteRecords(
        recordType: KClass<out Record>,
        recordIdsList: List<String>,
        clientRecordIdsList: List<String>
    ) {
        throw UnsupportedOperationException("Method not supported yet")
    }

    override suspend fun deleteRecords(
        recordType: KClass<out Record>,
        timeRangeFilter: TimeRangeFilter
    ) {
        throw UnsupportedOperationException("Method not supported yet")
    }

    override suspend fun <T : Record> readRecord(
        recordType: KClass<T>,
        recordId: String
    ): ReadRecordResponse<T> {
        throw UnsupportedOperationException("Method not supported yet")
    }

    override suspend fun <T : Record> readRecords(
        request: ReadRecordsRequest<T>
    ): ReadRecordsResponse<T> {
        throw UnsupportedOperationException("Method not supported yet")
    }

    override suspend fun aggregate(request: AggregateRequest): AggregationResult {
        throw UnsupportedOperationException("Method not supported yet")
    }

    override suspend fun aggregateGroupByDuration(
        request: AggregateGroupByDurationRequest
    ): List<AggregationResultGroupedByDuration> {
        throw UnsupportedOperationException("Method not supported yet")
    }

    override suspend fun aggregateGroupByPeriod(
        request: AggregateGroupByPeriodRequest
    ): List<AggregationResultGroupedByPeriod> {
        throw UnsupportedOperationException("Method not supported yet")
    }

    override suspend fun getChangesToken(request: ChangesTokenRequest): String {
        throw UnsupportedOperationException("Method not supported yet")
    }

    override suspend fun registerForDataNotifications(
        notificationIntentAction: String,
        recordTypes: Iterable<KClass<out Record>>
    ) {
        throw UnsupportedOperationException("Method not supported yet")
    }

    override suspend fun unregisterFromDataNotifications(notificationIntentAction: String) {
        throw UnsupportedOperationException("Method not supported yet")
    }

    override suspend fun getChanges(changesToken: String): ChangesResponse {
        throw UnsupportedOperationException("Method not supported yet")
    }

    override suspend fun getGrantedPermissions(
        permissions: Set<HealthPermission>
    ): Set<HealthPermission> {
        throw UnsupportedOperationException("Method not supported yet")
    }

    override suspend fun filterGrantedPermissions(permissions: Set<String>): Set<String> {
        throw UnsupportedOperationException("Method not supported yet")
    }

    override suspend fun revokeAllPermissions() {
        throw UnsupportedOperationException("Method not supported yet")
    }

    internal suspend fun <T> wrapPlatformException(function: suspend () -> T): T {
        return try {
            function()
        } catch (e: HealthConnectException) {
            throw e.toKtException()
        }
    }
}
