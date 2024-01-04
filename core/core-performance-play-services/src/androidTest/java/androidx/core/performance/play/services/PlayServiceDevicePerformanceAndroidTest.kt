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
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.common.truth.Truth
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** Android Unit tests for [PlayServicesDevicePerformance]. */
@RunWith(AndroidJUnit4::class)
class PlayServicesDevicePerformanceTest {
    class FakeDevicePerformanceClient() : DevicePerformanceClient {
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
    fun tearDown() = runBlocking {
        PlayServicesDevicePerformance.clearPerformanceClass(context)
    }

    @Test
    @MediumTest
    fun mediaPerformanceClass_EmptyStore_33Client() {
        val fakeDevicePerformanceClient = FakeDevicePerformanceClient()

        val playServicesDevicePerformance = PlayServicesDevicePerformance.create(
            context,
            fakeDevicePerformanceClient
        )
        fakeDevicePerformanceClient.taskSource.setResult(33)
        delayRead()
        val pcScore = playServicesDevicePerformance.mediaPerformanceClass
        Truth.assertThat(pcScore).isEqualTo(33)
    }

    @Test
    @MediumTest
    fun mediaPerformanceClass_EmptyStore() {
        val fakeDevicePerformanceClient = FakeDevicePerformanceClient()
        val playServicesDevicePerformance = PlayServicesDevicePerformance.create(
            context,
            fakeDevicePerformanceClient
        )
        val pcScore = playServicesDevicePerformance.mediaPerformanceClass
        Truth.assertThat(pcScore).isEqualTo(defaultMediaPerformanceClass)
    }

    @Test
    @MediumTest
    fun mediaPerformanceClass_EmptyStore_IllegalStateException() {
        val fakeDevicePerformanceClient = FakeDevicePerformanceClient()
        fakeDevicePerformanceClient.taskSource.setException(IllegalStateException())
        val playServicesDevicePerformance = PlayServicesDevicePerformance.create(
            context,
            fakeDevicePerformanceClient
        )
        // Since the gms service has crashed, the library should still return default value.
        Truth.assertThat(playServicesDevicePerformance.mediaPerformanceClass)
            .isEqualTo(defaultMediaPerformanceClass)
    }

    @Test
    @MediumTest
    fun mediaPerformanceClass_EmptyStore_TimeOut() {
        val fakeDevicePerformanceClient = FakeDevicePerformanceClient()
        fakeDevicePerformanceClient.taskSource.setException(ApiException(Status.RESULT_TIMEOUT))
        val playServicesDevicePerformance = PlayServicesDevicePerformance.create(
            context,
            fakeDevicePerformanceClient
        )
        // Since the gms service not started, the library should still return default value.
        Truth.assertThat(playServicesDevicePerformance.mediaPerformanceClass)
            .isEqualTo(defaultMediaPerformanceClass)
    }

    /* Add delay to make sure that value is written in Preference datastore before reading it */
    private fun delayRead() {
        val delayTime: Long = 200
        TimeUnit.MILLISECONDS.sleep(delayTime)
    }
}
