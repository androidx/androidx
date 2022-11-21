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

package androidx.privacysandbox.ads.adservices.java.measurement

import android.adservices.measurement.MeasurementManager
import android.content.Context
import android.net.Uri
import android.os.OutcomeReceiver
import android.view.InputEvent
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ads.adservices.java.measurement.MeasurementManagerFutures.Companion.from
import androidx.privacysandbox.ads.adservices.measurement.DeletionRequest
import androidx.privacysandbox.ads.adservices.measurement.WebSourceParams
import androidx.privacysandbox.ads.adservices.measurement.WebSourceRegistrationRequest
import androidx.privacysandbox.ads.adservices.measurement.WebTriggerParams
import androidx.privacysandbox.ads.adservices.measurement.WebTriggerRegistrationRequest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.lang.SuppressWarnings
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.invocation.InvocationOnMock

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 34) // b/259092025
class MeasurementManagerFuturesTest {
    private val uri1: Uri = Uri.parse("www.abc.com")
    private val uri2: Uri = Uri.parse("http://www.xyz.com")
    private lateinit var mContext: Context

    @Before
    fun setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    fun testMeasurementOlderVersions() {
        assertThat(from(mContext)).isEqualTo(null)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    @SuppressWarnings("NewApi")
    fun testDeleteRegistrationsAsync() {
        val measurementManager = mockMeasurementManager(mContext)
        val managerCompat = from(mContext)

        // Set up the request.
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer).`when`(measurementManager).deleteRegistrations(any(), any(), any())

        // Actually invoke the compat code.
        val request = DeletionRequest.Builder()
            .setDomainUris(listOf(uri1))
            .setOriginUris(listOf(uri1))
            .build()

        managerCompat!!.deleteRegistrationsAsync(request).get()

        // Verify that the compat code was invoked correctly.
        val captor = ArgumentCaptor.forClass(
            android.adservices.measurement.DeletionRequest::class.java
        )
        verify(measurementManager).deleteRegistrations(captor.capture(), any(), any())

        // Verify that the request that the compat code makes to the platform is correct.
        verifyDeletionRequest(captor.value)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    @SuppressWarnings("NewApi")
    fun testRegisterSourceAsync() {
        val inputEvent = mock(InputEvent::class.java)
        val measurementManager = mockMeasurementManager(mContext)
        val managerCompat = from(mContext)
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(3)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer).`when`(measurementManager).registerSource(any(), any(), any(), any())

        // Actually invoke the compat code.
        managerCompat!!.registerSourceAsync(uri1, inputEvent).get()

        // Verify that the compat code was invoked correctly.
        val captor1 = ArgumentCaptor.forClass(Uri::class.java)
        val captor2 = ArgumentCaptor.forClass(InputEvent::class.java)
        verify(measurementManager).registerSource(
            captor1.capture(),
            captor2.capture(),
            any(),
            any())

        // Verify that the request that the compat code makes to the platform is correct.
        assertThat(captor1.value == uri1)
        assertThat(captor2.value == inputEvent)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    @SuppressWarnings("NewApi")
    fun testRegisterTriggerAsync() {
        val measurementManager = mockMeasurementManager(mContext)
        val managerCompat = from(mContext)
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer).`when`(measurementManager).registerTrigger(any(), any(), any())

        // Actually invoke the compat code.
        managerCompat!!.registerTriggerAsync(uri1).get()

        // Verify that the compat code was invoked correctly.
        val captor1 = ArgumentCaptor.forClass(Uri::class.java)
        verify(measurementManager).registerTrigger(
            captor1.capture(),
            any(),
            any())

        // Verify that the request that the compat code makes to the platform is correct.
        assertThat(captor1.value == uri1)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    @SuppressWarnings("NewApi")
    fun testRegisterWebSourceAsync() {
        val measurementManager = mockMeasurementManager(mContext)
        val managerCompat = from(mContext)
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer).`when`(measurementManager).registerWebSource(any(), any(), any())

        val request = WebSourceRegistrationRequest.Builder(
            listOf(WebSourceParams(uri2, false)), uri1)
            .setAppDestination(uri1)
            .build()

        // Actually invoke the compat code.
        managerCompat!!.registerWebSourceAsync(request).get()

        // Verify that the compat code was invoked correctly.
        val captor1 = ArgumentCaptor.forClass(
            android.adservices.measurement.WebSourceRegistrationRequest::class.java)
        verify(measurementManager).registerWebSource(
            captor1.capture(),
            any(),
            any())

        // Verify that the request that the compat code makes to the platform is correct.
        val actualRequest = captor1.value
        assertThat(actualRequest.topOriginUri == uri1)
        assertThat(actualRequest.sourceParams.size == 1)
        assertThat(actualRequest.sourceParams[0].registrationUri == uri2)
        assertThat(!actualRequest.sourceParams[0].isDebugKeyAllowed)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    @SuppressWarnings("NewApi")
    fun testRegisterWebTriggerAsync() {
        val measurementManager = mockMeasurementManager(mContext)
        val managerCompat = from(mContext)
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer).`when`(measurementManager).registerWebTrigger(any(), any(), any())

        val request = WebTriggerRegistrationRequest(listOf(WebTriggerParams(uri1, false)), uri2)

        // Actually invoke the compat code.
        managerCompat!!.registerWebTriggerAsync(request).get()

        // Verify that the compat code was invoked correctly.
        val captor1 = ArgumentCaptor.forClass(
            android.adservices.measurement.WebTriggerRegistrationRequest::class.java)
        verify(measurementManager).registerWebTrigger(
            captor1.capture(),
            any(),
            any())

        // Verify that the request that the compat code makes to the platform is correct.
        val actualRequest = captor1.value
        assertThat(actualRequest.destination == uri2)
        assertThat(actualRequest.triggerParams.size == 1)
        assertThat(actualRequest.triggerParams[0].registrationUri == uri1)
        assertThat(!actualRequest.triggerParams[0].isDebugKeyAllowed)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    @SuppressWarnings("NewApi")
    fun testMeasurementApiStatusAsync() {
        val measurementManager = mockMeasurementManager(mContext)
        val managerCompat = from(mContext)
        val state = MeasurementManager.MEASUREMENT_API_STATE_DISABLED
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Int, Exception>>(1)
            receiver.onResult(state)
            null
        }
        doAnswer(answer).`when`(measurementManager).getMeasurementApiStatus(any(), any())

        // Actually invoke the compat code.
        val result = managerCompat!!.getMeasurementApiStatusAsync()
        result.get()

        // Verify that the compat code was invoked correctly.
        verify(measurementManager).getMeasurementApiStatus(any(), any())

        // Verify that the result.
        assertThat(result.get() == state)
    }

    @RequiresApi(34)
    private fun mockMeasurementManager(spyContext: Context): MeasurementManager {
        val measurementManager = mock(MeasurementManager::class.java)
        `when`(spyContext.getSystemService(MeasurementManager::class.java))
            .thenReturn(measurementManager)
        return measurementManager
    }

    @RequiresApi(34)
    private fun verifyDeletionRequest(request: android.adservices.measurement.DeletionRequest) {
        // Set up the request that we expect the compat code to invoke.
        val expectedRequest = android.adservices.measurement.DeletionRequest.Builder()
            .setDomainUris(listOf(uri1))
            .setOriginUris(listOf(uri1))
            .build()

        assertThat(HashSet(request.domainUris) == HashSet(expectedRequest.domainUris))
        assertThat(HashSet(request.originUris) == HashSet(expectedRequest.originUris))
    }
}