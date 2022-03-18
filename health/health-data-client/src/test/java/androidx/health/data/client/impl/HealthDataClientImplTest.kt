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
import androidx.health.data.client.records.ActiveEnergyBurned
import androidx.health.data.client.records.Nutrition
import androidx.health.data.client.records.Steps
import androidx.health.data.client.records.Weight
import androidx.health.data.client.request.ChangesTokenRequest
import androidx.health.data.client.request.ReadRecordsRequest
import androidx.health.data.client.time.TimeRangeFilter
import androidx.health.platform.client.impl.ServiceBackedHealthDataClient
import androidx.health.platform.client.impl.error.errorCodeExceptionMap
import androidx.health.platform.client.impl.ipc.ClientConfiguration
import androidx.health.platform.client.impl.ipc.internal.ConnectionManager
import androidx.health.platform.client.impl.testing.FakeHealthDataService
import androidx.health.platform.client.proto.DataProto
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
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
        { getChanges("token") },
        { getChangesToken(ChangesTokenRequest(recordTypes = setOf(Steps::class))) }
    )

@Suppress("GoodTime") // Safe to use in test setup
@RunWith(AndroidJUnit4::class)
class HealthDataClientImplTest {
    private lateinit var healthConnectClient: HealthDataClientImpl
    private lateinit var fakeAhpServiceStub: FakeHealthDataService

    @Before
    fun setup() {
        val clientConfig =
            ClientConfiguration("FakeAHPProvider", PROVIDER_PACKAGE_NAME, "FakeProvider")

        healthConnectClient =
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
    fun apiMethods_hasError_throwsException() {
        for (error in errorCodeExceptionMap) {
            fakeAhpServiceStub.setErrorCode(error.key)
            val responseList = mutableListOf<Deferred<Unit>>()
            for (method in API_METHOD_LIST) {
                responseList.add(
                    CoroutineScope(Dispatchers.Default).async { healthConnectClient.method() }
                )
            }

            // wait for the client to enqueue message and handle message
            Thread.sleep(Duration.ofMillis(1000).toMillis())
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1000))

            for (response in responseList) {
                assertThrows(error.value.java) { runBlocking { response.await() } }
            }
        }
    }

    @Test(timeout = 60000L)
    fun insertRecords_steps() {
        val deferred =
            CoroutineScope(Dispatchers.Default).async {
                healthConnectClient.insertRecords(
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

        // wait for the client to enqueue message and handle message
        Thread.sleep(Duration.ofMillis(1000).toMillis())
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1000))

        val response = runBlocking { deferred.await() }
        assertThat(response.recordUidsList).containsExactly("0")
        assertThat(fakeAhpServiceStub.dataStore.toList())
            .containsExactly(
                DataProto.DataPoint.newBuilder()
                    .setStartTimeMillis(1234L)
                    .setEndTimeMillis(5678L)
                    .putValues("count", DataProto.Value.newBuilder().setLongVal(100).build())
                    .setDataType(DataProto.DataType.newBuilder().setName("Steps"))
                    .build()
            )
    }

    @Test(timeout = 60000L)
    fun insertRecords_weight() {
        val deferred =
            CoroutineScope(Dispatchers.Default).async {
                healthConnectClient.insertRecords(
                    listOf(
                        Weight(
                            weightKg = 45.8,
                            time = Instant.ofEpochMilli(1234L),
                            zoneOffset = null,
                        )
                    )
                )
            }

        // wait for the client to enqueue message and handle message
        Thread.sleep(Duration.ofMillis(1000).toMillis())
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1000))

        val response = runBlocking { deferred.await() }
        assertThat(response.recordUidsList).containsExactly("0")
        assertThat(fakeAhpServiceStub.dataStore.toList())
            .containsExactly(
                DataProto.DataPoint.newBuilder()
                    .setInstantTimeMillis(1234L)
                    .putValues("weight", DataProto.Value.newBuilder().setDoubleVal(45.8).build())
                    .setDataType(DataProto.DataType.newBuilder().setName("Weight"))
                    .build()
            )
    }

    @Test(timeout = 60000L)
    fun insertRecords_nutrition() {
        val deferred =
            CoroutineScope(Dispatchers.Default).async {
                healthConnectClient.insertRecords(
                    listOf(
                        Nutrition(
                            vitaminE = 10.0,
                            vitaminC = 20.0,
                            startTime = Instant.ofEpochMilli(1234L),
                            startZoneOffset = null,
                            endTime = Instant.ofEpochMilli(5678L),
                            endZoneOffset = null
                        )
                    )
                )
            }

        // wait for the client to enqueue message and handle message
        Thread.sleep(Duration.ofMillis(1000).toMillis())
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1000))

        val response = runBlocking { deferred.await() }
        assertThat(response.recordUidsList).containsExactly("0")
        assertThat(fakeAhpServiceStub.dataStore.toList())
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

    private fun installPackage(context: Context, packageName: String, enabled: Boolean) {
        val packageInfo = PackageInfo()
        packageInfo.packageName = packageName
        packageInfo.applicationInfo = ApplicationInfo()
        packageInfo.applicationInfo.enabled = enabled
        val packageManager = context.packageManager
        Shadows.shadowOf(packageManager).installPackage(packageInfo)
    }
}
