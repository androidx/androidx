/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.performance.play.services

import android.app.Application
import android.content.Context
import androidx.core.performance.DefaultDevicePerformance
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.ApiKey
import com.google.android.gms.deviceperformance.DevicePerformanceClient
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** Android Unit tests for [PlayServicesDevicePerformance]. */
@RunWith(AndroidJUnit4::class)
class PlayServicesDevicePerformanceTest {

    private val mpcKey = intPreferencesKey("mpc_value")

    companion object {
        private val testContext: Context =
            InstrumentationRegistry.getInstrumentation().targetContext
        private val testDataStore: DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                produceFile = { testContext.preferencesDataStoreFile("test_mpc_datastore") }
            )
    }

    class FakeDevicePerformanceClient : DevicePerformanceClient {
        val taskSource: TaskCompletionSource<Int> = TaskCompletionSource()

        override fun getApiKey(): ApiKey<Api.ApiOptions.NoOptions> {
            // method for testing purpose
            return this.apiKey
        }

        override fun mediaPerformanceClass(): Task<Int> {
            return taskSource.task
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext<Application>()
    private val defaultMediaPerformanceClass = DefaultDevicePerformance().mediaPerformanceClass

    @After
    fun tearDown() {
        runBlocking { testDataStore.edit { it.clear() } }
    }

    @Test
    @MediumTest
    fun mediaPerformanceClass_EmptyStore() {
        val fakeDevicePerformanceClient = FakeDevicePerformanceClient()
        val playServicesDevicePerformance =
            PlayServicesDevicePerformance(context, fakeDevicePerformanceClient, testDataStore)
        val pcScore = playServicesDevicePerformance.mediaPerformanceClass
        Truth.assertThat(pcScore).isEqualTo(defaultMediaPerformanceClass)
    }

    @Test
    @MediumTest
    fun mediaPerformanceClass_EmptyStore_LesserClient() = runTest {
        val clientMpc = defaultMediaPerformanceClass - 1
        val fakeDevicePerformanceClient = FakeDevicePerformanceClient()
        fakeDevicePerformanceClient.taskSource.setResult(clientMpc)
        val playServicesDevicePerformance =
            PlayServicesDevicePerformance(context, fakeDevicePerformanceClient, testDataStore)
        // Waits until the DataStore is populated with at least one non-null value.
        testDataStore.data
            .mapNotNull { values ->
                // No type safety.
                values[mpcKey]
            }
            .first()

        val pcScore = playServicesDevicePerformance.mediaPerformanceClass
        Truth.assertThat(pcScore).isEqualTo(defaultMediaPerformanceClass)
    }

    @Test
    @MediumTest
    fun mediaPerformanceClass_EmptyStore_HigherClient() = runTest {
        val clientMpc = defaultMediaPerformanceClass + 33
        val fakeDevicePerformanceClient = FakeDevicePerformanceClient()
        fakeDevicePerformanceClient.taskSource.setResult(clientMpc)
        val playServicesDevicePerformance =
            PlayServicesDevicePerformance(context, fakeDevicePerformanceClient, testDataStore)
        val mpcKey = intPreferencesKey("mpc_value")
        // Waits until the DataStore is populated with at least one non-null value.
        testDataStore.data
            .mapNotNull { values ->
                // No type safety.
                values[mpcKey]
            }
            .first()

        val pcScore = playServicesDevicePerformance.mediaPerformanceClass
        Truth.assertThat(pcScore).isEqualTo(clientMpc)
    }

    @Test
    @MediumTest
    fun mediaPerformanceClass_EmptyStore_IllegalStateException() {
        val fakeDevicePerformanceClient = FakeDevicePerformanceClient()
        fakeDevicePerformanceClient.taskSource.setException(IllegalStateException())
        val playServicesDevicePerformance =
            PlayServicesDevicePerformance(context, fakeDevicePerformanceClient, testDataStore)
        // Since the gms service has crashed, the library should still return default value.
        Truth.assertThat(playServicesDevicePerformance.mediaPerformanceClass)
            .isEqualTo(defaultMediaPerformanceClass)
    }

    @Test
    @MediumTest
    fun mediaPerformanceClass_EmptyStore_TimeOut() {
        val fakeDevicePerformanceClient = FakeDevicePerformanceClient()
        fakeDevicePerformanceClient.taskSource.setException(ApiException(Status.RESULT_TIMEOUT))
        val playServicesDevicePerformance =
            PlayServicesDevicePerformance(context, fakeDevicePerformanceClient, testDataStore)
        // Since the gms service not started, the library should still return default value.
        Truth.assertThat(playServicesDevicePerformance.mediaPerformanceClass)
            .isEqualTo(defaultMediaPerformanceClass)
    }

    @Test
    @MediumTest
    fun mediaPerformanceClass_NonEmptyStore_LesserClient() = runTest {
        val datastoreMpc = defaultMediaPerformanceClass + 30
        val clientMpc = defaultMediaPerformanceClass - 10
        testDataStore.edit { values -> values[mpcKey] = datastoreMpc }
        val fakeDevicePerformanceClient = FakeDevicePerformanceClient()
        fakeDevicePerformanceClient.taskSource.setResult(clientMpc)
        val playServicesDevicePerformance =
            PlayServicesDevicePerformance(context, fakeDevicePerformanceClient, testDataStore)
        val pcScore = playServicesDevicePerformance.mediaPerformanceClass
        Truth.assertThat(pcScore).isEqualTo(datastoreMpc)
    }

    @Test
    @MediumTest
    fun mediaPerformanceClass_NonEmptyStore_HigherClient() = runTest {
        val datastoreMpc = defaultMediaPerformanceClass - 5
        val clientMpc = defaultMediaPerformanceClass + 10
        testDataStore.edit { values -> values[mpcKey] = datastoreMpc }
        val fakeDevicePerformanceClient = FakeDevicePerformanceClient()
        fakeDevicePerformanceClient.taskSource.setResult(clientMpc)
        val playServicesDevicePerformance =
            PlayServicesDevicePerformance(context, fakeDevicePerformanceClient, testDataStore)
        // As the client has returned a value higher than the datastore, we expect the datastore
        // to be updated by the library within 200 ms.
        runCatching {
            withTimeout(200) {
                // Waits until the DataStore is populated with new MPC value
                do {
                    val mpc =
                        testDataStore.data
                            .mapNotNull { values ->
                                // No type safety.
                                values[mpcKey]
                            }
                            .first()
                } while (mpc != clientMpc)
            }
        }
        val pcScore = playServicesDevicePerformance.mediaPerformanceClass
        Truth.assertThat(pcScore).isEqualTo(clientMpc)
    }

    @Test
    @MediumTest
    fun mediaPerformanceClass_NonEmptyStore_IllegalStateException() = runTest {
        val datastoreMpc = defaultMediaPerformanceClass + 30
        testDataStore.edit { values -> values[mpcKey] = datastoreMpc }
        val fakeDevicePerformanceClient = FakeDevicePerformanceClient()
        fakeDevicePerformanceClient.taskSource.setException(IllegalStateException())
        val playServicesDevicePerformance =
            PlayServicesDevicePerformance(context, fakeDevicePerformanceClient, testDataStore)
        // Since the gms service has crashed, the library should still return stored value
        // in datastore.
        Truth.assertThat(playServicesDevicePerformance.mediaPerformanceClass)
            .isEqualTo(datastoreMpc)
    }

    @Test
    @MediumTest
    fun mediaPerformanceClass_NonEmptyStore_TimeOut() = runTest {
        val datastoreMpc = defaultMediaPerformanceClass + 30
        testDataStore.edit { values -> values[mpcKey] = datastoreMpc }
        val fakeDevicePerformanceClient = FakeDevicePerformanceClient()
        fakeDevicePerformanceClient.taskSource.setException(ApiException(Status.RESULT_TIMEOUT))
        val playServicesDevicePerformance =
            PlayServicesDevicePerformance(context, fakeDevicePerformanceClient, testDataStore)
        // Since the gms service not started, the library should still return stored value
        // in datastore
        Truth.assertThat(playServicesDevicePerformance.mediaPerformanceClass)
            .isEqualTo(datastoreMpc)
    }
}
