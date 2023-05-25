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
import android.os.ext.SdkExtensions
import android.view.InputEvent
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.ads.adservices.measurement.MeasurementManager.Companion.obtain
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assume
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
@SuppressWarnings("NewApi")
@RunWith(AndroidJUnit4::class)
class MeasurementManagerTest {

    @Before
    fun setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    fun testMeasurementOlderVersions() {
        val sdkExtVersion = SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)

        Assume.assumeTrue("maxSdkVersion = API 33 ext 4", sdkExtVersion < 5)
        assertThat(obtain(mContext)).isEqualTo(null)
    }

    @Test
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    fun testDeleteRegistrations() {
        val sdkExtVersion = SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)

        Assume.assumeTrue("minSdkVersion = API 33 ext 5", sdkExtVersion >= 5)
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
            val request = DeletionRequest(
                DeletionRequest.DELETION_MODE_ALL,
                DeletionRequest.MATCH_BEHAVIOR_DELETE,
                Instant.now(),
                Instant.now(),
                listOf(uri1),
                listOf(uri1))

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
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    fun testRegisterSource() {
        val sdkExtVersion = SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)

        Assume.assumeTrue("minSdkVersion = API 33 ext 5", sdkExtVersion >= 5)
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
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    fun testRegisterTrigger() {
        val sdkExtVersion = SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)

        Assume.assumeTrue("minSdkVersion = API 33 ext 5", sdkExtVersion >= 5)
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
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    fun testRegisterWebSource() {
        val sdkExtVersion = SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)

        Assume.assumeTrue("minSdkVersion = API 33 ext 5", sdkExtVersion >= 5)
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
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    fun testRegisterWebTrigger() {
        val sdkExtVersion = SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)

        Assume.assumeTrue("minSdkVersion = API 33 ext 5", sdkExtVersion >= 5)
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
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    fun testMeasurementApiStatus() {
        val sdkExtVersion = SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)

        Assume.assumeTrue("minSdkVersion = API 33 ext 5", sdkExtVersion >= 5)
        val measurementManager = mockMeasurementManager(mContext)
        val managerCompat = obtain(mContext)
        val state = MeasurementManager.MEASUREMENT_API_STATE_ENABLED
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Int, Exception>>(1)
            receiver.onResult(state)
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
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    fun testMeasurementApiStatusUnknown() {
        val sdkExtVersion = SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)

        Assume.assumeTrue("minSdkVersion = API 33 ext 5", sdkExtVersion >= 5)
        val measurementManager = mockMeasurementManager(mContext)
        val managerCompat = obtain(mContext)
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Int, Exception>>(1)
            receiver.onResult(6 /* Greater than values returned in SdkExtensions.AD_SERVICES = 5 */)
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
        assertThat(actualResult == 5)
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    companion object {

        private val uri1: Uri = Uri.parse("www.abc.com")
        private val uri2: Uri = Uri.parse("http://www.xyz.com")

        private lateinit var mContext: Context

        private fun mockMeasurementManager(spyContext: Context): MeasurementManager {
            val measurementManager = mock(MeasurementManager::class.java)
            `when`(spyContext.getSystemService(MeasurementManager::class.java))
                .thenReturn(measurementManager)
            return measurementManager
        }

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
}