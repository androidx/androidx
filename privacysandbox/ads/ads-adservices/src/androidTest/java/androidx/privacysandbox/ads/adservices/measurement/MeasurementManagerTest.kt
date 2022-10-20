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

package androidx.privacysandbox.ads.adservices.measurement

import android.adservices.measurement.MeasurementManager
import android.content.Context
import android.net.Uri
import android.os.OutcomeReceiver
import android.view.InputEvent
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ads.adservices.measurement.MeasurementManager.Companion.obtain
import androidx.privacysandbox.ads.adservices.measurement.MeasurementManager.MeasurementApiState
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
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
class MeasurementManagerTest {
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
        assertThat(obtain(mContext)).isEqualTo(null)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testDeleteRegistrations() {
        val measurementManager = mockMeasurementManager(mContext)
        val managerCompat = obtain(mContext)

        // Set up the request.
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer).`when`(measurementManager).deleteRegistrations(any(), any(), any())

        // Actually invoke the compat code.
        runBlocking {
            val request = DeletionRequest.Builder()
                .setDomainUris(listOf(uri1))
                .setOriginUris(listOf(uri1))
                .build()

            managerCompat!!.deleteRegistrations(request)
        }

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
    fun testRegisterSource() {
        val inputEvent = mock(InputEvent::class.java)
        val measurementManager = mockMeasurementManager(mContext)
        val managerCompat = obtain(mContext)
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(3)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer).`when`(measurementManager).registerSource(any(), any(), any(), any())

        // Actually invoke the compat code.
        runBlocking {
            managerCompat!!.registerSource(uri1, inputEvent)
        }

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
    fun testRegisterTrigger() {
        val measurementManager = mockMeasurementManager(mContext)
        val managerCompat = obtain(mContext)
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer).`when`(measurementManager).registerTrigger(any(), any(), any())

        // Actually invoke the compat code.
        runBlocking {
            managerCompat!!.registerTrigger(uri1)
        }

        // Verify that the compat code was invoked correctly.
        val captor1 = ArgumentCaptor.forClass(Uri::class.java)
        verify(measurementManager).registerTrigger(
            captor1.capture(),
            any(),
            any())

        // Verify that the request that the compat code makes to the platform is correct.
        assertThat(captor1.value).isEqualTo(uri1)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testRegisterWebSource() {
        val measurementManager = mockMeasurementManager(mContext)
        val managerCompat = obtain(mContext)
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer).`when`(measurementManager).registerWebSource(any(), any(), any())

        val request = WebSourceRegistrationRequest.Builder(
            listOf(WebSourceParams(uri1, false)), uri1)
            .setAppDestination(uri1)
            .build()

        // Actually invoke the compat code.
        runBlocking {
            managerCompat!!.registerWebSource(request)
        }

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
        assertThat(actualRequest.sourceParams[0].registrationUri == uri1)
        assertThat(!actualRequest.sourceParams[0].isDebugKeyAllowed)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testRegisterWebTrigger() {
        val measurementManager = mockMeasurementManager(mContext)
        val managerCompat = obtain(mContext)
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer).`when`(measurementManager).registerWebTrigger(any(), any(), any())

        val request = WebTriggerRegistrationRequest(
            listOf(WebTriggerParams(uri1, false)), uri2)

        // Actually invoke the compat code.
        runBlocking {
            managerCompat!!.registerWebTrigger(request)
        }

        // Verify that the compat code was invoked correctly.
        val captor1 = ArgumentCaptor.forClass(
            android.adservices.measurement.WebTriggerRegistrationRequest::class.java)
        verify(measurementManager).registerWebTrigger(
            captor1.capture(),
            any(),
            any())

        // Verify that the request that the compat code makes to the platform is correct.
        val actualRequest = captor1.value
        assertThat(actualRequest.destination).isEqualTo(uri2)
        assertThat(actualRequest.triggerParams.size == 1)
        assertThat(actualRequest.triggerParams[0].registrationUri == uri1)
        assertThat(!actualRequest.triggerParams[0].isDebugKeyAllowed)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testMeasurementApiStatus() {
        val measurementManager = mockMeasurementManager(mContext)
        val managerCompat = obtain(mContext)
        val state = MeasurementApiState.MEASUREMENT_API_STATE_ENABLED
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Int, Exception>>(1)
            receiver.onResult(state.ordinal)
            null
        }
        doAnswer(answer).`when`(measurementManager).getMeasurementApiStatus(any(), any())

        // Actually invoke the compat code.
        val actualResult = runBlocking {
            managerCompat!!.getMeasurementApiStatus()
        }

        // Verify that the compat code was invoked correctly.
        verify(measurementManager).getMeasurementApiStatus(any(), any())

        // Verify that the request that the compat code makes to the platform is correct.
        assertThat(actualResult == state)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testMeasurementApiStatusUnknown() {
        val measurementManager = mockMeasurementManager(mContext)
        val managerCompat = obtain(mContext)
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Int, Exception>>(1)
            receiver.onResult(2 /* Greater than values returned in SdkExtensions.AD_SERVICES = 4 */)
            null
        }
        doAnswer(answer).`when`(measurementManager).getMeasurementApiStatus(any(), any())

        // Actually invoke the compat code.
        val actualResult = runBlocking {
            managerCompat!!.getMeasurementApiStatus()
        }

        // Verify that the compat code was invoked correctly.
        verify(measurementManager).getMeasurementApiStatus(any(), any())

        // Verify that the request that the compat code makes to the platform is correct.
        // Since the compat code does not know the returned state, it sets it to UNKNOWN.
        assertThat(actualResult == MeasurementApiState.UNKNOWN)
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