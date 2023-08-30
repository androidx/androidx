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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.ApiKey
import com.google.android.gms.deviceperformance.DevicePerformanceClient
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** Android Unit tests for [PlayServicesDevicePerformance]. */
@RunWith(AndroidJUnit4::class)
class PlayServicesDevicePerformanceTest {
    enum class TestType {
        NONE, DELAY, API_EXCEPTION, IS_EXCEPTION
    }

    companion object {
        const val fakeMPC = 100
    }

    class FakeDevicePerformanceClient : DevicePerformanceClient {

        var testType = TestType.NONE
        override fun getApiKey(): ApiKey<Api.ApiOptions.NoOptions> {
            // method for testing purpose
            return this.apiKey
        }

        override fun mediaPerformanceClass(): Task<Int> {
            when (testType) {
                TestType.DELAY -> {
                    TimeUnit.SECONDS.sleep(5)
                    return Tasks.forResult(fakeMPC)
                }

                TestType.API_EXCEPTION -> {
                    return Tasks.forException(ApiException(Status.RESULT_TIMEOUT))
                }

                TestType.IS_EXCEPTION -> {
                    return Tasks.forException(IllegalStateException())
                }

                else -> {
                    return Tasks.forResult(fakeMPC)
                }
            }
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext<Application>()
    private val defaultMediaPerformanceClass = DefaultDevicePerformance().mediaPerformanceClass
    private val fakeClient: FakeDevicePerformanceClient = FakeDevicePerformanceClient()

    @After
    fun tearDown() = runBlocking {
        PlayServicesDevicePerformance.clearPerformanceClass(context)
    }

    @Test
    @MediumTest
    fun basePlayServiceDevicePerformanceClassTest() {
        val playServicesDevicePerformance = PlayServicesDevicePerformance(
            context
        )
        val pcScore = playServicesDevicePerformance.mediaPerformanceClass
        Truth.assertThat(pcScore).isEqualTo(defaultMediaPerformanceClass)
    }

    @Test
    @MediumTest
    fun fakePlayServiceDevicePerformanceClassTest() {
        fakeClient.testType = TestType.NONE
        val playServicesDevicePerformance = PlayServicesDevicePerformance(
            context,
            fakeClient
        )
        val pcScore = playServicesDevicePerformance.mediaPerformanceClass
        Truth.assertThat(pcScore).isEqualTo(fakeMPC)
    }

    @Test
    @MediumTest
    fun delayFakePlayServiceDevicePerformanceClassTest() {
        fakeClient.testType = TestType.DELAY
        val playServicesDevicePerformance = PlayServicesDevicePerformance(
            context,
            fakeClient,
            true
        )
        val pcScore = playServicesDevicePerformance.mediaPerformanceClass
        Truth.assertThat(pcScore).isEqualTo(defaultMediaPerformanceClass)
    }

    @Test
    @MediumTest
    fun playServiceCrashPerformanceClassTest() {
        fakeClient.testType = TestType.IS_EXCEPTION
        val playServicesDevicePerformance = PlayServicesDevicePerformance(
            context,
            fakeClient
        )
        // Since the gms service has crashed, the library should still return default value.
        Truth.assertThat(playServicesDevicePerformance.mediaPerformanceClass)
            .isEqualTo(defaultMediaPerformanceClass)
    }

    @Test
    @MediumTest
    fun playServiceNotStartPerformanceClassTest() {
        fakeClient.testType = TestType.API_EXCEPTION
        val playServicesDevicePerformance = PlayServicesDevicePerformance(
            context,
            fakeClient
        )
        // Since the gms service not started, the library should still return default value.
        Truth.assertThat(playServicesDevicePerformance.mediaPerformanceClass)
            .isEqualTo(defaultMediaPerformanceClass)
    }
}
