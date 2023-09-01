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
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/** Android Unit tests for [PlayServicesDevicePerformance]. */
@RunWith(AndroidJUnit4::class)
class PlayServicesDevicePerformanceTest {
    open class DevicePerformanceClientTest : DevicePerformanceClient {
        override fun getApiKey(): ApiKey<Api.ApiOptions.NoOptions> {
            // method for testing purpose
            return this.apiKey
        }

        override fun mediaPerformanceClass(): Task<Int> {
            return Tasks.forResult(0)
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext<Application>()
    private val defaultMediaPerformanceClass = DefaultDevicePerformance().mediaPerformanceClass

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
    fun mockPlayServiceDevicePerformanceClassTest() {
        val mockClient: DevicePerformanceClient = mock(DevicePerformanceClientTest::class.java)
        val mediaPerformanceClass = 33
        `when`(mockClient.mediaPerformanceClass()).thenAnswer {
            Tasks.forResult(mediaPerformanceClass)
        }
        val playServicesDevicePerformance = PlayServicesDevicePerformance(
            context,
            mockClient
        )
        delayRead()
        val pcScore = playServicesDevicePerformance.mediaPerformanceClass
        Truth.assertThat(pcScore).isEqualTo(mediaPerformanceClass)
    }

    @Test
    @MediumTest
    fun delayMockPlayServiceDevicePerformanceClassTest() {
        val mockClient: DevicePerformanceClient = mock(DevicePerformanceClientTest::class.java)

        // Delay the response from mockClient.mediaPerformanceClass() so
        // response will be different that provided.
        `when`(mockClient.mediaPerformanceClass()).thenAnswer {
            TimeUnit.SECONDS.sleep(5)
            Tasks.forResult(defaultMediaPerformanceClass + 100)
        }
        val playServicesDevicePerformance = PlayServicesDevicePerformance(
            context,
            mockClient
        )
        val pcScore = playServicesDevicePerformance.mediaPerformanceClass
        Truth.assertThat(pcScore).isEqualTo(defaultMediaPerformanceClass)
    }

    @Test
    @MediumTest
    fun playServiceCrashPerformanceClassTest() {
        val mockClient: DevicePerformanceClient = mock(DevicePerformanceClientTest::class.java)
        `when`(mockClient.mediaPerformanceClass()).thenReturn( // Throw an exception here.
            Tasks.forException(IllegalStateException())
        )
        val pc = PlayServicesDevicePerformance(
            context,
            mockClient
        )
        // Since the gms service has crashed, the library should still return default value.
        Truth.assertThat(pc.mediaPerformanceClass).isEqualTo(defaultMediaPerformanceClass)
    }

    @Test
    @MediumTest
    fun playServiceNotStartPerformanceClassTest() {
        val mockClient: DevicePerformanceClient = mock(DevicePerformanceClientTest::class.java)
        `when`(mockClient.mediaPerformanceClass()).thenReturn( // Throw an exception here.
            Tasks.forException(ApiException(Status.RESULT_TIMEOUT))
        )
        val pc = PlayServicesDevicePerformance(
            context,
            mockClient
        )
        // Since the gms service not started, the library should still return default value.
        Truth.assertThat(pc.mediaPerformanceClass).isEqualTo(defaultMediaPerformanceClass)
    }

    /* Add delay to make sure that value is written in Preference datastore before reading it */
    private fun delayRead() {
        val delayTime: Long = 200
        TimeUnit.MILLISECONDS.sleep(delayTime)
    }
}
