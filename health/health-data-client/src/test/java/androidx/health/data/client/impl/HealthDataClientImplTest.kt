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
package androidx.health.data.client.impl

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Looper
import androidx.health.data.client.HealthDataClient
import androidx.health.data.client.aggregate.AggregateDataRow
import androidx.health.data.client.changes.DeletionChange
import androidx.health.data.client.changes.UpsertionChange
import androidx.health.data.client.metadata.DataOrigin
import androidx.health.data.client.metadata.Device
import androidx.health.data.client.metadata.Metadata
import androidx.health.data.client.records.ActiveEnergyBurned
import androidx.health.data.client.records.Nutrition
import androidx.health.data.client.records.Steps
import androidx.health.data.client.records.Weight
import androidx.health.data.client.request.AggregateRequest
import androidx.health.data.client.request.ChangesTokenRequest
import androidx.health.data.client.request.ReadRecordsRequest
import androidx.health.data.client.response.ReadRecordResponse
import androidx.health.data.client.response.ReadRecordsResponse
import androidx.health.data.client.time.TimeRangeFilter
import androidx.health.platform.client.impl.ServiceBackedHealthDataClient
import androidx.health.platform.client.impl.error.errorCodeExceptionMap
import androidx.health.platform.client.impl.ipc.ClientConfiguration
import androidx.health.platform.client.impl.ipc.internal.ConnectionManager
import androidx.health.platform.client.impl.testing.FakeHealthDataService
import androidx.health.platform.client.proto.ChangeProto
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.proto.RequestProto
import androidx.health.platform.client.proto.ResponseProto
import androidx.health.platform.client.proto.TimeProto
import androidx.health.platform.client.response.AggregateDataResponse
import androidx.health.platform.client.response.GetChangesResponse
import androidx.health.platform.client.response.GetChangesTokenResponse
import androidx.health.platform.client.response.InsertDataResponse
import androidx.health.platform.client.response.ReadDataRangeResponse
import androidx.health.platform.client.response.ReadDataResponse
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows

private const val PROVIDER_PACKAGE_NAME = "com.google.fake.provider"

private val API_METHOD_LIST =
    listOf<suspend HealthDataClient.() -> Unit>(
        { getGrantedPermissions(setOf()) },
        { insertRecords(listOf()) },
        { updateRecords(listOf()) },
        { deleteRecords(ActiveEnergyBurned::class, listOf(), listOf()) },
        { deleteRecords(ActiveEnergyBurned::class, TimeRangeFilter.empty()) },
        { readRecord(Steps::class, "uid") },
        {
            readRecords(
                ReadRecordsRequest(
                    Steps::class,
                    TimeRangeFilter.exact(Instant.ofEpochMilli(1234L), Instant.ofEpochMilli(1235L))
                )
            )
        },
        { aggregate(AggregateRequest(setOf(), TimeRangeFilter.empty())) },
        { getChanges("token") },
        { getChangesToken(ChangesTokenRequest(recordTypes = setOf(Steps::class))) }
    )

@Suppress("GoodTime") // Safe to use in test setup
@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HealthDataClientImplTest {

    private lateinit var healthDataClient: HealthDataClientImpl
    private lateinit var fakeAhpServiceStub: FakeHealthDataService

    @Before
    fun setup() {
        val clientConfig =
            ClientConfiguration("FakeAHPProvider", PROVIDER_PACKAGE_NAME, "FakeProvider")

        healthDataClient =
            HealthDataClientImpl(
                ServiceBackedHealthDataClient(
                    ApplicationProvider.getApplicationContext(),
                    clientConfig,
                    ConnectionManager(
                        ApplicationProvider.getApplicationContext(),
                        Looper.getMainLooper()
                    )
                )
            )
        fakeAhpServiceStub = FakeHealthDataService()

        Shadows.shadowOf(ApplicationProvider.getApplicationContext<Context>() as Application)
            .setComponentNameAndServiceForBindServiceForIntent(
                Intent()
                    .setPackage(clientConfig.servicePackageName)
                    .setAction(clientConfig.bindAction),
                ComponentName(clientConfig.servicePackageName, clientConfig.bindAction),
                fakeAhpServiceStub
            )
        installPackage(ApplicationProvider.getApplicationContext(), PROVIDER_PACKAGE_NAME, true)
        Intents.init()
    }

    @After
    fun teardown() {
        Intents.release()
    }

    @Test(timeout = 60000L)
    fun apiMethods_hasError_throwsException() = runTest {
        for (error in errorCodeExceptionMap) {
            fakeAhpServiceStub.errorCode = error.key
            val responseList = mutableListOf<Deferred<Any>>()
            for (method in API_METHOD_LIST) {
                responseList.add(
                    async { assertFailsWith(error.value) { healthDataClient.method() } }
                )
            }
            advanceUntilIdle()
            waitForMainLooperIdle()
            for (response in responseList) {
                response.await()
            }
        }
    }

    @Test(timeout = 10000L)
    fun insertRecords_steps() = runTest {
        fakeAhpServiceStub.insertDataResponse = InsertDataResponse(listOf("0"))
        val deferred = async {
            healthDataClient.insertRecords(
                listOf(
                    Steps(
                        count = 100,
                        startTime = Instant.ofEpochMilli(1234L),
                        startZoneOffset = null,
                        endTime = Instant.ofEpochMilli(5678L),
                        endZoneOffset = null
                    )
                )
            )
        }

        advanceUntilIdle()
        waitForMainLooperIdle()

        val response = deferred.await()
        assertThat(response.recordUidsList).containsExactly("0")
        assertThat(fakeAhpServiceStub.lastUpsertDataRequest?.dataPoints)
            .containsExactly(
                DataProto.DataPoint.newBuilder()
                    .setStartTimeMillis(1234L)
                    .setEndTimeMillis(5678L)
                    .putValues("count", DataProto.Value.newBuilder().setLongVal(100).build())
                    .setDataType(DataProto.DataType.newBuilder().setName("Steps"))
                    .build()
            )
    }

    @Test(timeout = 10000L)
    fun insertRecords_weight() = runTest {
        fakeAhpServiceStub.insertDataResponse = InsertDataResponse(listOf("0"))
        val deferred = async {
            healthDataClient.insertRecords(
                listOf(
                    Weight(
                        weightKg = 45.8,
                        time = Instant.ofEpochMilli(1234L),
                        zoneOffset = null,
                    )
                )
            )
        }

        advanceUntilIdle()
        waitForMainLooperIdle()

        val response = deferred.await()
        assertThat(response.recordUidsList).containsExactly("0")
        assertThat(fakeAhpServiceStub.lastUpsertDataRequest?.dataPoints)
            .containsExactly(
                DataProto.DataPoint.newBuilder()
                    .setInstantTimeMillis(1234L)
                    .putValues("weight", DataProto.Value.newBuilder().setDoubleVal(45.8).build())
                    .setDataType(DataProto.DataType.newBuilder().setName("Weight"))
                    .build()
            )
    }

    @Test(timeout = 10000L)
    fun insertRecords_nutrition() = runTest {
        fakeAhpServiceStub.insertDataResponse = InsertDataResponse(listOf("0"))
        val deferred = async {
            healthDataClient.insertRecords(
                listOf(
                    Nutrition(
                        vitaminEGrams = 10.0,
                        vitaminCGrams = 20.0,
                        startTime = Instant.ofEpochMilli(1234L),
                        startZoneOffset = null,
                        endTime = Instant.ofEpochMilli(5678L),
                        endZoneOffset = null
                    )
                )
            )
        }

        advanceUntilIdle()
        waitForMainLooperIdle()

        val response = deferred.await()
        assertThat(response.recordUidsList).containsExactly("0")
        assertThat(fakeAhpServiceStub.lastUpsertDataRequest?.dataPoints)
            .containsExactly(
                DataProto.DataPoint.newBuilder()
                    .setStartTimeMillis(1234L)
                    .setEndTimeMillis(5678L)
                    .putValues("vitaminC", DataProto.Value.newBuilder().setDoubleVal(20.0).build())
                    .putValues("vitaminE", DataProto.Value.newBuilder().setDoubleVal(10.0).build())
                    .setDataType(DataProto.DataType.newBuilder().setName("Nutrition"))
                    .build()
            )
    }

    @Test(timeout = 10000L)
    fun readRecordById_steps() = runTest {
        fakeAhpServiceStub.readDataResponse =
            ReadDataResponse(
                ResponseProto.ReadDataResponse.newBuilder()
                    .setDataPoint(
                        DataProto.DataPoint.newBuilder()
                            .setUid("testUid")
                            .setStartTimeMillis(1234L)
                            .setEndTimeMillis(5678L)
                            .putValues(
                                "count",
                                DataProto.Value.newBuilder().setLongVal(100).build()
                            )
                            .setDataType(DataProto.DataType.newBuilder().setName("Steps"))
                    )
                    .build()
            )
        val deferred = async {
            healthDataClient.readRecord(
                Steps::class,
                uid = "testUid",
            )
        }

        advanceUntilIdle()
        waitForMainLooperIdle()

        val response: ReadRecordResponse<Steps> = deferred.await()
        assertThat(fakeAhpServiceStub.lastReadDataRequest?.proto)
            .isEqualTo(
                RequestProto.ReadDataRequest.newBuilder()
                    .setDataTypeIdPair(
                        RequestProto.DataTypeIdPair.newBuilder()
                            .setDataType(DataProto.DataType.newBuilder().setName("Steps"))
                            .setId("testUid")
                    )
                    .build()
            )
        assertThat(response.record)
            .isEqualTo(
                Steps(
                    count = 100,
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(5678L),
                    endZoneOffset = null,
                    metadata =
                    Metadata(
                        uid = "testUid",
                        device = Device(),
                    )
                )
            )
    }

    @Test(timeout = 10000L)
    fun readRecords_steps() = runTest {
        fakeAhpServiceStub.readDataRangeResponse =
            ReadDataRangeResponse(
                ResponseProto.ReadDataRangeResponse.newBuilder()
                    .addDataPoint(
                        DataProto.DataPoint.newBuilder()
                            .setUid("testUid")
                            .setStartTimeMillis(1234L)
                            .setEndTimeMillis(5678L)
                            .putValues(
                                "count",
                                DataProto.Value.newBuilder().setLongVal(100).build()
                            )
                            .setDataType(DataProto.DataType.newBuilder().setName("Steps"))
                    )
                    .setPageToken("nextPageToken")
                    .build()
            )
        val deferred = async {
            healthDataClient.readRecords(
                ReadRecordsRequest(
                    Steps::class,
                    timeRangeFilter = TimeRangeFilter.exact(endTime = Instant.ofEpochMilli(7890L)),
                    limit = 10
                )
            )
        }

        advanceUntilIdle()
        waitForMainLooperIdle()

        val response: ReadRecordsResponse<Steps> = deferred.await()
        assertThat(fakeAhpServiceStub.lastReadDataRangeRequest?.proto)
            .isEqualTo(
                RequestProto.ReadDataRangeRequest.newBuilder()
                    .setTimeSpec(TimeProto.TimeSpec.newBuilder().setEndTimeEpochMs(7890L))
                    .setDataType(DataProto.DataType.newBuilder().setName("Steps"))
                    .setAscOrdering(true)
                    .setLimit(10)
                    .build()
            )
        assertThat(response.pageToken).isEqualTo("nextPageToken")
        assertThat(response.records)
            .containsExactly(
                Steps(
                    count = 100,
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(5678L),
                    endZoneOffset = null,
                    metadata =
                    Metadata(
                        uid = "testUid",
                        device = Device(),
                    )
                )
            )
    }

    @Test(timeout = 10000L)
    fun deleteRecords_steps() = runTest {
        val deferred = async {
            healthDataClient.deleteRecords(
                Steps::class,
                timeRangeFilter = TimeRangeFilter.exact(endTime = Instant.ofEpochMilli(7890L)),
            )
        }
        advanceUntilIdle()
        waitForMainLooperIdle()
        deferred.await()
        assertThat(fakeAhpServiceStub.lastDeleteDataRangeRequest?.proto)
            .isEqualTo(
                RequestProto.DeleteDataRangeRequest.newBuilder()
                    .setTimeSpec(TimeProto.TimeSpec.newBuilder().setEndTimeEpochMs(7890L))
                    .addDataType(DataProto.DataType.newBuilder().setName("Steps"))
                    .build()
            )
    }

    @Test(timeout = 10000L)
    fun updateRecords_steps() = runTest {
        val deferred = async {
            healthDataClient.updateRecords(
                listOf(
                    Steps(
                        count = 100,
                        startTime = Instant.ofEpochMilli(1234L),
                        startZoneOffset = null,
                        endTime = Instant.ofEpochMilli(5678L),
                        endZoneOffset = null,
                        metadata = Metadata(uid = "testUid")
                    )
                )
            )
        }

        advanceUntilIdle()
        waitForMainLooperIdle()

        deferred.await()
        assertThat(fakeAhpServiceStub.lastUpsertDataRequest?.dataPoints)
            .containsExactly(
                DataProto.DataPoint.newBuilder()
                    .setUid("testUid")
                    .setStartTimeMillis(1234L)
                    .setEndTimeMillis(5678L)
                    .putValues("count", DataProto.Value.newBuilder().setLongVal(100).build())
                    .setDataType(DataProto.DataType.newBuilder().setName("Steps"))
                    .build()
            )
    }

    @Test(timeout = 10000L)
    fun aggregate_totalSteps(): Unit = runTest {
        val dataOrigin = DataProto.DataOrigin.newBuilder().setApplicationId("id").build()
        val aggregateDataRow =
            DataProto.AggregateDataRow.newBuilder()
                .setStartTimeEpochMs(1234)
                .setEndTimeEpochMs(4567)
                .setZoneOffsetSeconds(999)
                .addDataOrigins(dataOrigin)
                .build()
        fakeAhpServiceStub.aggregateDataResponse =
            AggregateDataResponse(
                ResponseProto.AggregateDataResponse.newBuilder().addRows(aggregateDataRow).build()
            )
        val deferred = async {
            val startTime = Instant.ofEpochMilli(1234)
            val endTime = Instant.ofEpochMilli(4567)
            healthDataClient.aggregate(
                AggregateRequest(
                    setOf(Steps.STEPS_COUNT_TOTAL),
                    TimeRangeFilter.exact(startTime, endTime)
                )
            )
        }

        advanceUntilIdle()
        waitForMainLooperIdle()

        val response: AggregateDataRow = deferred.await()
        // This is currently impossible to test for 3p devs, we'll need to override equals()
        assertThat(response.longValues).isEmpty()
        assertThat(response.doubleValues).isEmpty()
        assertThat(response.dataOrigins).contains(DataOrigin("id"))
        assertThat(fakeAhpServiceStub.lastAggregateRequest?.proto)
            .isEqualTo(
                RequestProto.AggregateDataRequest.newBuilder()
                    .setTimeSpec(
                        TimeProto.TimeSpec.newBuilder()
                            .setStartTimeEpochMs(1234)
                            .setEndTimeEpochMs(4567)
                            .build()
                    )
                    .addMetricSpec(
                        RequestProto.AggregateMetricSpec.newBuilder()
                            .setDataTypeName("Steps")
                            .setAggregationType("total")
                            .setFieldName("count")
                            .build()
                    )
                    .build()
            )
    }

    @Test(timeout = 10000L)
    fun getChangesToken() = runTest {
        fakeAhpServiceStub.changesTokenResponse =
            GetChangesTokenResponse(
                ResponseProto.GetChangesTokenResponse.newBuilder()
                    .setChangesToken("changesToken")
                    .build()
            )
        val deferred = async {
            healthDataClient.getChangesToken(ChangesTokenRequest(setOf(Steps::class)))
        }

        advanceUntilIdle()
        waitForMainLooperIdle()

        val response = deferred.await()
        assertThat(response).isEqualTo("changesToken")
        assertThat(fakeAhpServiceStub.lastGetChangesTokenRequest?.proto)
            .isEqualTo(
                RequestProto.GetChangesTokenRequest.newBuilder()
                    .addDataType(DataProto.DataType.newBuilder().setName("Steps"))
                    .build()
            )
    }

    @Test(timeout = 10000L)
    fun getChanges_steps() = runTest {
        fakeAhpServiceStub.changesResponse =
            GetChangesResponse(
                ResponseProto.GetChangesResponse.newBuilder()
                    .addChanges(ChangeProto.DataChange.newBuilder().setDeleteUid("deleteUid"))
                    .addChanges(
                        ChangeProto.DataChange.newBuilder()
                            .setUpsertDataPoint(
                                DataProto.DataPoint.newBuilder()
                                    .setUid("testUid")
                                    .setStartTimeMillis(1234L)
                                    .setEndTimeMillis(5678L)
                                    .putValues(
                                        "count",
                                        DataProto.Value.newBuilder().setLongVal(100).build()
                                    )
                                    .setDataType(DataProto.DataType.newBuilder().setName("Steps"))
                                    .build()
                            )
                    )
                    .setHasMore(true)
                    .setChangesTokenExpired(false)
                    .build()
            )
        val deferred = async { healthDataClient.getChanges("steps_changes_token") }

        advanceUntilIdle()
        waitForMainLooperIdle()

        val response = deferred.await()
        assertThat(response.changes).hasSize(2)
        assertThat(response.changes.get(0)).isInstanceOf(DeletionChange::class.java)
        assertThat(response.changes.get(1)).isInstanceOf(UpsertionChange::class.java)
        assertThat(response.hasMore).isTrue()
        assertThat(response.changesTokenExpired).isFalse()
        assertThat(fakeAhpServiceStub.lastGetChangesRequest?.proto)
            .isEqualTo(
                RequestProto.GetChangesRequest.newBuilder()
                    .setChangesToken("steps_changes_token")
                    .build()
            )
    }

    @Test(timeout = 10000L)
    fun deleteRecordsById_steps() = runTest {
        val deferred = async {
            healthDataClient.deleteRecords(Steps::class, listOf("myUid"), listOf("myClientId"))
        }

        advanceUntilIdle()
        waitForMainLooperIdle()
        deferred.await()

        val stepsTypeProto = DataProto.DataType.newBuilder().setName("Steps")
        assertThat(fakeAhpServiceStub.lastDeleteDataRequest?.clientIds).containsExactly(
            RequestProto.DataTypeIdPair.newBuilder()
                .setDataType(stepsTypeProto).setId("myClientId").build()
        )
        assertThat(fakeAhpServiceStub.lastDeleteDataRequest?.uids).containsExactly(
            RequestProto.DataTypeIdPair.newBuilder()
                .setDataType(stepsTypeProto).setId("myUid").build()
        )
    }

    private fun waitForMainLooperIdle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun installPackage(context: Context, packageName: String, enabled: Boolean) {
        val packageInfo = PackageInfo()
        packageInfo.packageName = packageName
        packageInfo.applicationInfo = ApplicationInfo()
        packageInfo.applicationInfo.enabled = enabled
        val packageManager = context.packageManager
        Shadows.shadowOf(packageManager).installPackage(packageInfo)
    }
}
