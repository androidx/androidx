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
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo
import androidx.privacysandbox.ads.adservices.measurement.MeasurementManager.Companion.obtain
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.fail
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.dx.mockito.inline.extended.StaticMockitoSession
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.util.concurrent.Executor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.quality.Strictness

@SmallTest
@SuppressWarnings("NewApi")
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 30)
class MeasurementManagerTest {

    private var mSession: StaticMockitoSession? = null
    private val mValidAdServicesSdkExtVersion = AdServicesInfo.adServicesVersion() >= 5
    private val mValidAdExtServicesSdkExtVersionS = AdServicesInfo.extServicesVersionS() >= 9

    @Before
    fun setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext<Context>())

        if (mValidAdExtServicesSdkExtVersionS) {
            // setup a mockitoSession to return the mocked manager
            // when the static method .get() is called
            mSession =
                ExtendedMockito.mockitoSession()
                    .mockStatic(android.adservices.measurement.MeasurementManager::class.java)
                    .strictness(Strictness.LENIENT)
                    .startMocking()
        }
    }

    @After
    fun tearDown() {
        mSession?.finishMocking()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33, minSdkVersion = 30)
    fun testMeasurementOlderVersions() {
        Assume.assumeTrue("maxSdkVersion = API 33 ext 4", !mValidAdServicesSdkExtVersion)
        Assume.assumeTrue("maxSdkVersion = API 31/32 ext 8", !mValidAdExtServicesSdkExtVersionS)
        assertThat(obtain(mContext)).isNull()
    }

    @Test
    fun testMeasurementManagerNoClassDefFoundError() {
        Assume.assumeTrue("minSdkVersion = API 31/32 ext 9", mValidAdExtServicesSdkExtVersionS)

        `when`(MeasurementManager.get(any())).thenThrow(NoClassDefFoundError())
        assertThat(obtain(mContext)).isNull()
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testDeleteRegistrationsOnSPlus() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            mValidAdServicesSdkExtVersion || mValidAdExtServicesSdkExtVersionS
        )

        val measurementManager = mockMeasurementManager(mContext, mValidAdExtServicesSdkExtVersionS)
        val managerCompat = obtain(mContext)

        // Set up the request.
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer)
            .`when`(measurementManager)
            .deleteRegistrations(
                any<android.adservices.measurement.DeletionRequest>(),
                any<Executor>(),
                any<OutcomeReceiver<Any, java.lang.Exception>>()
            )

        // Actually invoke the compat code.
        runBlocking {
            val request =
                DeletionRequest(
                    DeletionRequest.DELETION_MODE_ALL,
                    DeletionRequest.MATCH_BEHAVIOR_DELETE,
                    Instant.now(),
                    Instant.now(),
                    listOf(uri1),
                    listOf(uri1)
                )

            managerCompat!!.deleteRegistrations(request)
        }

        // Verify that the compat code was invoked correctly.
        val captor =
            ArgumentCaptor.forClass(android.adservices.measurement.DeletionRequest::class.java)
        verify(measurementManager)
            .deleteRegistrations(
                captor.capture(),
                any<Executor>(),
                any<OutcomeReceiver<Any, java.lang.Exception>>()
            )

        // Verify that the request that the compat code makes to the platform is correct.
        verifyDeletionRequest(captor.value)
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testRegisterSourceOnSPlus() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            mValidAdServicesSdkExtVersion || mValidAdExtServicesSdkExtVersionS
        )

        val measurementManager = mockMeasurementManager(mContext, mValidAdExtServicesSdkExtVersionS)
        val inputEvent = mock(InputEvent::class.java)
        val managerCompat = obtain(mContext)

        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(3)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer)
            .`when`(measurementManager)
            .registerSource(
                any<Uri>(),
                any<InputEvent>(),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )

        // Actually invoke the compat code.
        runBlocking { managerCompat!!.registerSource(uri1, inputEvent) }

        // Verify that the compat code was invoked correctly.
        val captor1 = ArgumentCaptor.forClass(Uri::class.java)
        val captor2 = ArgumentCaptor.forClass(InputEvent::class.java)
        verify(measurementManager)
            .registerSource(
                captor1.capture(),
                captor2.capture(),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )

        // Verify that the request that the compat code makes to the platform is correct.
        assertThat(captor1.value == uri1)
        assertThat(captor2.value == inputEvent)
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testRegisterTriggerOnSPlus() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            mValidAdServicesSdkExtVersion || mValidAdExtServicesSdkExtVersionS
        )

        val measurementManager = mockMeasurementManager(mContext, mValidAdExtServicesSdkExtVersionS)
        val managerCompat = obtain(mContext)
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer)
            .`when`(measurementManager)
            .registerTrigger(any<Uri>(), any<Executor>(), any<OutcomeReceiver<Any, Exception>>())

        // Actually invoke the compat code.
        runBlocking { managerCompat!!.registerTrigger(uri1) }

        // Verify that the compat code was invoked correctly.
        val captor1 = ArgumentCaptor.forClass(Uri::class.java)
        verify(measurementManager)
            .registerTrigger(
                captor1.capture(),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )

        // Verify that the request that the compat code makes to the platform is correct.
        assertThat(captor1.value).isEqualTo(uri1)
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testRegisterWebSourceOnSPlus() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            mValidAdServicesSdkExtVersion || mValidAdExtServicesSdkExtVersionS
        )

        val measurementManager = mockMeasurementManager(mContext, mValidAdExtServicesSdkExtVersionS)
        val managerCompat = obtain(mContext)
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer)
            .`when`(measurementManager)
            .registerWebSource(
                any<android.adservices.measurement.WebSourceRegistrationRequest>(),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )

        val request =
            WebSourceRegistrationRequest.Builder(listOf(WebSourceParams(uri1, false)), uri1)
                .setAppDestination(appDestination)
                .build()

        // Actually invoke the compat code.
        runBlocking { managerCompat!!.registerWebSource(request) }

        // Verify that the compat code was invoked correctly.
        val captor1 =
            ArgumentCaptor.forClass(
                android.adservices.measurement.WebSourceRegistrationRequest::class.java
            )
        verify(measurementManager)
            .registerWebSource(
                captor1.capture(),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )

        // Verify that the request that the compat code makes to the platform is correct.
        val actualRequest = captor1.value
        assertThat(actualRequest.topOriginUri == uri1)
        assertThat(actualRequest.sourceParams.size == 1)
        assertThat(actualRequest.appDestination == appDestination)
        assertThat(actualRequest.sourceParams[0].registrationUri == uri1)
        assertThat(!actualRequest.sourceParams[0].isDebugKeyAllowed)
    }

    @ExperimentalFeatures.RegisterSourceOptIn
    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testRegisterSource_allSuccessOnSPlus() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            mValidAdServicesSdkExtVersion || mValidAdExtServicesSdkExtVersionS
        )

        val measurementManager = mockMeasurementManager(mContext, mValidAdExtServicesSdkExtVersionS)
        val mockInputEvent = mock(InputEvent::class.java)
        val managerCompat = obtain(mContext)

        val successCallback = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(3)
            receiver.onResult(Object())
            null
        }
        doAnswer(successCallback)
            .`when`(measurementManager)
            .registerSource(
                any<Uri>(),
                any<InputEvent>(),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )

        val request = SourceRegistrationRequest(listOf(uri1, uri2), mockInputEvent)

        // Actually invoke the compat code.
        runBlocking { managerCompat!!.registerSource(request) }

        // Verify that the compat code was invoked correctly.
        verify(measurementManager, times(2))
            .registerSource(
                any(),
                eq(mockInputEvent),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )
    }

    @ExperimentalFeatures.RegisterSourceOptIn
    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testRegisterSource_15thOf20Fails_remaining5DoNotExecuteOnSPlus() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            mValidAdServicesSdkExtVersion || mValidAdExtServicesSdkExtVersionS
        )

        val measurementManager = mockMeasurementManager(mContext, mValidAdExtServicesSdkExtVersionS)
        val mockInputEvent = mock(InputEvent::class.java)
        val managerCompat = obtain(mContext)

        val successCallback = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(3)
            receiver.onResult(Object())
            null
        }

        val errorMessage = "some error occurred"
        val errorCallback = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(3)
            receiver.onError(IllegalArgumentException(errorMessage))
            null
        }
        val uris =
            (0..20)
                .map { i ->
                    val uri = Uri.parse("www.uri$i.com")
                    if (i == 15) {
                        doAnswer(errorCallback)
                            .`when`(measurementManager)
                            .registerSource(
                                eq(uri),
                                any<InputEvent>(),
                                any<Executor>(),
                                any<OutcomeReceiver<Any, Exception>>()
                            )
                    } else {
                        doAnswer(successCallback)
                            .`when`(measurementManager)
                            .registerSource(
                                eq(uri),
                                any<InputEvent>(),
                                any<Executor>(),
                                any<OutcomeReceiver<Any, Exception>>()
                            )
                    }
                    uri
                }
                .toList()

        val request = SourceRegistrationRequest(uris, mockInputEvent)

        // Actually invoke the compat code.
        runBlocking {
            try {
                managerCompat!!.registerSource(request)
                fail("Expected failure.")
            } catch (e: IllegalArgumentException) {
                assertThat(e.message).isEqualTo(errorMessage)
            }
        }

        // Verify that the compat code was invoked correctly.
        (0..15).forEach { i ->
            verify(measurementManager)
                .registerSource(
                    eq(Uri.parse("www.uri$i.com")),
                    eq(mockInputEvent),
                    any<Executor>(),
                    any<OutcomeReceiver<Any, Exception>>()
                )
        }
        (16..20).forEach { i ->
            verify(measurementManager, never())
                .registerSource(
                    eq(Uri.parse("www.uri$i.com")),
                    eq(mockInputEvent),
                    any<Executor>(),
                    any<OutcomeReceiver<Any, Exception>>()
                )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testRegisterWebTriggerOnSPlus() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            mValidAdServicesSdkExtVersion || mValidAdExtServicesSdkExtVersionS
        )

        val measurementManager = mockMeasurementManager(mContext, mValidAdExtServicesSdkExtVersionS)
        val managerCompat = obtain(mContext)
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer)
            .`when`(measurementManager)
            .registerWebTrigger(
                any<android.adservices.measurement.WebTriggerRegistrationRequest>(),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )

        val request = WebTriggerRegistrationRequest(listOf(WebTriggerParams(uri1, false)), uri2)

        // Actually invoke the compat code.
        runBlocking { managerCompat!!.registerWebTrigger(request) }

        // Verify that the compat code was invoked correctly.
        val captor1 =
            ArgumentCaptor.forClass(
                android.adservices.measurement.WebTriggerRegistrationRequest::class.java
            )
        verify(measurementManager)
            .registerWebTrigger(
                captor1.capture(),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )

        // Verify that the request that the compat code makes to the platform is correct.
        val actualRequest = captor1.value
        assertThat(actualRequest.destination).isEqualTo(uri2)
        assertThat(actualRequest.triggerParams.size == 1)
        assertThat(actualRequest.triggerParams[0].registrationUri == uri1)
        assertThat(!actualRequest.triggerParams[0].isDebugKeyAllowed)
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testMeasurementApiStatusOnSPlus() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            mValidAdServicesSdkExtVersion || mValidAdExtServicesSdkExtVersionS
        )

        val measurementManager = mockMeasurementManager(mContext, mValidAdExtServicesSdkExtVersionS)
        callAndVerifyGetMeasurementApiStatusOnSPlus(
            measurementManager,
            /* state= */ MeasurementManager.MEASUREMENT_API_STATE_ENABLED,
            /* expectedResult= */ MeasurementManager.MEASUREMENT_API_STATE_ENABLED
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testMeasurementApiStatusUnknownOnSPlus() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            mValidAdServicesSdkExtVersion || mValidAdExtServicesSdkExtVersionS
        )

        val measurementManager = mockMeasurementManager(mContext, mValidAdExtServicesSdkExtVersionS)

        // Call with a value greater than values returned in SdkExtensions.AD_SERVICES = 5
        // Since the compat code does not know the returned state, it sets it to UNKNOWN.
        callAndVerifyGetMeasurementApiStatusOnSPlus(
            measurementManager,
            /* state= */ 6,
            /* expectedResult= */ 5
        )
    }

    @SdkSuppress(minSdkVersion = 30)
    companion object {

        private val uri1: Uri = Uri.parse("https://www.abc.com")
        private val uri2: Uri = Uri.parse("https://www.xyz.com")
        private val appDestination: Uri = Uri.parse("android-app://com.app.package")

        private lateinit var mContext: Context

        private fun mockMeasurementManager(
            spyContext: Context,
            isExtServices: Boolean
        ): MeasurementManager {
            val measurementManager = mock(MeasurementManager::class.java)
            `when`(spyContext.getSystemService(MeasurementManager::class.java))
                .thenReturn(measurementManager)
            // only mock the .get() method if using the extServices version
            if (isExtServices) {
                `when`(MeasurementManager.get(any())).thenReturn(measurementManager)
            }
            return measurementManager
        }

        private fun callAndVerifyGetMeasurementApiStatusOnSPlus(
            measurementManager: android.adservices.measurement.MeasurementManager,
            state: Int,
            expectedResult: Int
        ) {
            val managerCompat = obtain(mContext)
            val answer = { args: InvocationOnMock ->
                val receiver = args.getArgument<OutcomeReceiver<Int, Exception>>(1)
                receiver.onResult(state)
                null
            }
            doAnswer(answer)
                .`when`(measurementManager)
                .getMeasurementApiStatus(any<Executor>(), any<OutcomeReceiver<Int, Exception>>())

            // Actually invoke the compat code.
            val actualResult = runBlocking { managerCompat!!.getMeasurementApiStatus() }

            // Verify that the compat code was invoked correctly.
            verify(measurementManager)
                .getMeasurementApiStatus(any<Executor>(), any<OutcomeReceiver<Int, Exception>>())

            // Verify that the request that the compat code makes to the platform is correct.
            assertThat(actualResult == expectedResult)
        }

        private fun verifyDeletionRequest(request: android.adservices.measurement.DeletionRequest) {
            // Set up the request that we expect the compat code to invoke.
            val expectedRequest =
                android.adservices.measurement.DeletionRequest.Builder()
                    .setDomainUris(listOf(uri1))
                    .setOriginUris(listOf(uri1))
                    .build()

            assertThat(HashSet(request.domainUris) == HashSet(expectedRequest.domainUris))
            assertThat(HashSet(request.originUris) == HashSet(expectedRequest.originUris))
        }
    }
}
