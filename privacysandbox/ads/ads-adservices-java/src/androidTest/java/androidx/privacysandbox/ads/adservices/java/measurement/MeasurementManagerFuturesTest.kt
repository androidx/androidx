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
import android.os.Looper
import android.os.OutcomeReceiver
import android.view.InputEvent
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.java.VersionCompatUtil
import androidx.privacysandbox.ads.adservices.java.measurement.MeasurementManagerFutures.Companion.from
import androidx.privacysandbox.ads.adservices.measurement.DeletionRequest
import androidx.privacysandbox.ads.adservices.measurement.SourceRegistrationRequest
import androidx.privacysandbox.ads.adservices.measurement.WebSourceParams
import androidx.privacysandbox.ads.adservices.measurement.WebSourceRegistrationRequest
import androidx.privacysandbox.ads.adservices.measurement.WebTriggerParams
import androidx.privacysandbox.ads.adservices.measurement.WebTriggerRegistrationRequest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.dx.mockito.inline.extended.StaticMockitoSession
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.atMost
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
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
class MeasurementManagerFuturesTest {

    private var mSession: StaticMockitoSession? = null
    private val mValidAdExtServicesSdkExtVersionS =
        VersionCompatUtil.isSWithMinExtServicesVersion(9)

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
        Assume.assumeFalse(
            "maxSdkVersion = API 33 ext 4 or API 31/32 ext 8",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion=*/ 5,
                /* minExtServicesVersionS=*/ 9,
            )
        )
        assertThat(from(mContext)).isEqualTo(null)
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testDeleteRegistrationsAsyncOnSPlus() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion= */ 5,
                /* minExtServicesVersion=*/ 9
            )
        )

        val mMeasurementManager =
            mockMeasurementManager(mContext, mValidAdExtServicesSdkExtVersionS)
        val managerCompat = from(mContext)

        // Set up the request.
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
            receiver.onResult(Object())
            assertNotEquals(Looper.myLooper(), Looper.getMainLooper())
            null
        }
        doAnswer(answer)
            .`when`(mMeasurementManager)
            .deleteRegistrations(
                any<android.adservices.measurement.DeletionRequest>(),
                any<Executor>(),
                any<OutcomeReceiver<Any, java.lang.Exception>>()
            )

        // Actually invoke the compat code.
        val request =
            DeletionRequest(
                DeletionRequest.DELETION_MODE_ALL,
                DeletionRequest.MATCH_BEHAVIOR_DELETE,
                Instant.now(),
                Instant.now(),
                listOf(uri1),
                listOf(uri1)
            )

        managerCompat!!.deleteRegistrationsAsync(request).get()

        // Verify that the compat code was invoked correctly.
        val captor =
            ArgumentCaptor.forClass(android.adservices.measurement.DeletionRequest::class.java)
        verify(mMeasurementManager)
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
    fun testRegisterSourceAsyncOnSPlus() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion= */ 5,
                /* minExtServicesVersion=*/ 9
            )
        )

        val mMeasurementManager =
            mockMeasurementManager(mContext, mValidAdExtServicesSdkExtVersionS)
        val inputEvent = mock(InputEvent::class.java)
        val managerCompat = from(mContext)

        val answer = { args: InvocationOnMock ->
            assertNotEquals(Looper.myLooper(), Looper.getMainLooper())
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(3)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer)
            .`when`(mMeasurementManager)
            .registerSource(
                any<Uri>(),
                any<InputEvent>(),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )

        // Actually invoke the compat code.
        managerCompat!!.registerSourceAsync(uri1, inputEvent).get()

        // Verify that the compat code was invoked correctly.
        val captor1 = ArgumentCaptor.forClass(Uri::class.java)
        val captor2 = ArgumentCaptor.forClass(InputEvent::class.java)
        verify(mMeasurementManager)
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
    fun testRegisterTriggerAsyncOnSPlus() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion= */ 5,
                /* minExtServicesVersion=*/ 9
            )
        )

        val mMeasurementManager =
            mockMeasurementManager(mContext, mValidAdExtServicesSdkExtVersionS)
        val managerCompat = from(mContext)

        val answer = { args: InvocationOnMock ->
            assertNotEquals(Looper.myLooper(), Looper.getMainLooper())
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer)
            .`when`(mMeasurementManager)
            .registerTrigger(any<Uri>(), any<Executor>(), any<OutcomeReceiver<Any, Exception>>())

        // Actually invoke the compat code.
        managerCompat!!.registerTriggerAsync(uri1).get()

        // Verify that the compat code was invoked correctly.
        val captor1 = ArgumentCaptor.forClass(Uri::class.java)
        verify(mMeasurementManager)
            .registerTrigger(
                captor1.capture(),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )

        // Verify that the request that the compat code makes to the platform is correct.
        assertThat(captor1.value == uri1)
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testRegisterWebSourceAsyncOnSPlus() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion= */ 5,
                /* minExtServicesVersion=*/ 9
            )
        )

        val mMeasurementManager =
            mockMeasurementManager(mContext, mValidAdExtServicesSdkExtVersionS)
        val managerCompat = from(mContext)

        val answer = { args: InvocationOnMock ->
            assertNotEquals(Looper.myLooper(), Looper.getMainLooper())
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer)
            .`when`(mMeasurementManager)
            .registerWebSource(
                any<android.adservices.measurement.WebSourceRegistrationRequest>(),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )

        val request =
            WebSourceRegistrationRequest.Builder(listOf(WebSourceParams(uri2, false)), uri1)
                .setAppDestination(appDestination)
                .build()

        // Actually invoke the compat code.
        managerCompat!!.registerWebSourceAsync(request).get()

        // Verify that the compat code was invoked correctly.
        val captor1 =
            ArgumentCaptor.forClass(
                android.adservices.measurement.WebSourceRegistrationRequest::class.java
            )
        verify(mMeasurementManager)
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
        assertThat(actualRequest.sourceParams[0].registrationUri == uri2)
        assertThat(!actualRequest.sourceParams[0].isDebugKeyAllowed)
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testRegisterWebTriggerAsyncOnSPlus() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion= */ 5,
                /* minExtServicesVersion=*/ 9
            )
        )

        val mMeasurementManager =
            mockMeasurementManager(mContext, mValidAdExtServicesSdkExtVersionS)
        val managerCompat = from(mContext)

        val answer = { args: InvocationOnMock ->
            assertNotEquals(Looper.myLooper(), Looper.getMainLooper())
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
            receiver.onResult(Object())
            null
        }
        doAnswer(answer)
            .`when`(mMeasurementManager)
            .registerWebTrigger(
                any<android.adservices.measurement.WebTriggerRegistrationRequest>(),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )

        val request = WebTriggerRegistrationRequest(listOf(WebTriggerParams(uri1, false)), uri2)

        // Actually invoke the compat code.
        managerCompat!!.registerWebTriggerAsync(request).get()

        // Verify that the compat code was invoked correctly.
        val captor1 =
            ArgumentCaptor.forClass(
                android.adservices.measurement.WebTriggerRegistrationRequest::class.java
            )
        verify(mMeasurementManager)
            .registerWebTrigger(
                captor1.capture(),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )

        // Verify that the request that the compat code makes to the platform is correct.
        val actualRequest = captor1.value
        assertThat(actualRequest.destination == uri2)
        assertThat(actualRequest.triggerParams.size == 1)
        assertThat(actualRequest.triggerParams[0].registrationUri == uri1)
        assertThat(!actualRequest.triggerParams[0].isDebugKeyAllowed)
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testMeasurementApiStatusAsyncOnSPlus() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion= */ 5,
                /* minExtServicesVersion=*/ 9
            )
        )

        val mMeasurementManager =
            mockMeasurementManager(mContext, mValidAdExtServicesSdkExtVersionS)
        val managerCompat = from(mContext)

        val state = MeasurementManager.MEASUREMENT_API_STATE_DISABLED
        val answer = { args: InvocationOnMock ->
            assertNotEquals(Looper.myLooper(), Looper.getMainLooper())
            val receiver = args.getArgument<OutcomeReceiver<Int, Exception>>(1)
            receiver.onResult(state)
            null
        }
        doAnswer(answer)
            .`when`(mMeasurementManager)
            .getMeasurementApiStatus(any<Executor>(), any<OutcomeReceiver<Int, Exception>>())

        // Actually invoke the compat code.
        val result = managerCompat!!.getMeasurementApiStatusAsync()
        result.get()

        // Verify that the compat code was invoked correctly.
        verify(mMeasurementManager)
            .getMeasurementApiStatus(any<Executor>(), any<OutcomeReceiver<Int, Exception>>())

        // Verify that the result.
        assertThat(result.get() == state)
    }

    @ExperimentalFeatures.RegisterSourceOptIn
    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testRegisterSourceAsync_allSuccessOnSPlus() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion= */ 5,
                /* minExtServicesVersion=*/ 9
            )
        )

        val mMeasurementManager =
            mockMeasurementManager(mContext, mValidAdExtServicesSdkExtVersionS)
        val inputEvent = mock(InputEvent::class.java)
        val managerCompat = from(mContext)

        val successCallback = { args: InvocationOnMock ->
            assertNotEquals(Looper.myLooper(), Looper.getMainLooper())
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(3)
            receiver.onResult(Object())
            null
        }
        doAnswer(successCallback)
            .`when`(mMeasurementManager)
            .registerSource(
                any<Uri>(),
                any<InputEvent>(),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )

        // Actually invoke the compat code.
        val request =
            SourceRegistrationRequest.Builder(listOf(uri1, uri2)).setInputEvent(inputEvent).build()
        managerCompat!!.registerSourceAsync(request).get()

        // Verify that the compat code was invoked correctly.
        verify(mMeasurementManager)
            .registerSource(
                eq(uri1),
                eq(inputEvent),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )
        verify(mMeasurementManager)
            .registerSource(
                eq(uri2),
                eq(inputEvent),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )
    }

    @ExperimentalFeatures.RegisterSourceOptIn
    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testRegisterSource_15thOf20Fails_atLeast15thExecutesOnSPlus() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 5 or API 31/32 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion= */ 5,
                /* minExtServicesVersion=*/ 9
            )
        )

        val mMeasurementManager =
            mockMeasurementManager(mContext, mValidAdExtServicesSdkExtVersionS)
        val mockInputEvent = mock(InputEvent::class.java)
        val managerCompat = from(mContext)

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
            (1..20)
                .map { i ->
                    val uri = Uri.parse("www.uri$i.com")
                    if (i == 15) {
                        doAnswer(errorCallback)
                            .`when`(mMeasurementManager)
                            .registerSource(
                                eq(uri),
                                any<InputEvent>(),
                                any<Executor>(),
                                any<OutcomeReceiver<Any, Exception>>()
                            )
                    } else {
                        doAnswer(successCallback)
                            .`when`(mMeasurementManager)
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
                withContext(Dispatchers.Main) { managerCompat!!.registerSourceAsync(request).get() }
                fail("Expected failure.")
            } catch (e: ExecutionException) {
                assertTrue(e.cause!! is IllegalArgumentException)
                assertThat(e.cause!!.message).isEqualTo(errorMessage)
            }
        }

        // Verify that the compat code was invoked correctly.
        // registerSource gets called 1-20 times. We cannot predict the exact number because
        // uri15 would crash asynchronously. Other uris may succeed and those threads on default
        // dispatcher won't crash.
        verify(mMeasurementManager, atLeastOnce())
            .registerSource(
                any<Uri>(),
                eq(mockInputEvent),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )
        verify(mMeasurementManager, atMost(20))
            .registerSource(
                any<Uri>(),
                eq(mockInputEvent),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
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
            // mock the .get() method if using extServices version, otherwise mock getSystemService
            if (isExtServices) {
                `when`(android.adservices.measurement.MeasurementManager.get(any()))
                    .thenReturn(measurementManager)
            } else {
                `when`(spyContext.getSystemService(MeasurementManager::class.java))
                    .thenReturn(measurementManager)
            }
            return measurementManager
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
