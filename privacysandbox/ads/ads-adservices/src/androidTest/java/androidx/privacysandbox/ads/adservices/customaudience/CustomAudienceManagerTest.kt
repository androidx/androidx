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

package androidx.privacysandbox.ads.adservices.customaudience

import android.adservices.customaudience.CustomAudienceManager
import android.content.Context
import android.net.Uri
import android.os.OutcomeReceiver
import androidx.privacysandbox.ads.adservices.common.AdData
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudienceManager.Companion.obtain
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.dx.mockito.inline.extended.StaticMockitoSession
import com.google.common.truth.Truth
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
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
import org.mockito.quality.Strictness

@OptIn(ExperimentalFeatures.Ext8OptIn::class, ExperimentalFeatures.Ext10OptIn::class)
@SmallTest
@SuppressWarnings("NewApi")
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class CustomAudienceManagerTest {

    private var mSession: StaticMockitoSession? = null
    private val mValidAdServicesSdkExtVersion = AdServicesInfo.adServicesVersion() >= 4
    private val mValidAdExtServicesSdkExtVersion = AdServicesInfo.extServicesVersion() >= 9

    @Before
    fun setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext<Context>())

        if (mValidAdExtServicesSdkExtVersion) {
            // setup a mockitoSession to return the mocked manager
            // when the static method .get() is called
            mSession = ExtendedMockito.mockitoSession()
                .mockStatic(android.adservices.customaudience.CustomAudienceManager::class.java)
                .strictness(Strictness.LENIENT)
                .startMocking()
        }
    }

    @After
    fun tearDown() {
        mSession?.finishMocking()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33, minSdkVersion = 31)
    fun testOlderVersions() {
        Assume.assumeTrue("maxSdkVersion = API 33 ext 3", !mValidAdServicesSdkExtVersion)
        Assume.assumeTrue("maxSdkVersion = API 31/32 ext 8", !mValidAdExtServicesSdkExtVersion)
        Truth.assertThat(obtain(mContext)).isEqualTo(null)
    }

    @Test
    @SdkSuppress(maxSdkVersion = 34, minSdkVersion = 31)
    fun testFetchAndJoinCustomAudienceOlderVersions() {
        /* AdServices or ExtServices are present */
        Assume.assumeTrue("minSdkVersion = API 33 ext 4 or API 31/32 ext 9",
                          mValidAdServicesSdkExtVersion || mValidAdExtServicesSdkExtVersion)

        /* API is not available */
        Assume.assumeTrue("maxSdkVersion = API 31-34 ext 9",
            AdServicesInfo.adServicesVersion() < 10 && AdServicesInfo.extServicesVersion() < 10)
        mockCustomAudienceManager(mContext, mValidAdExtServicesSdkExtVersion)
        val managerCompat = obtain(mContext)

        // Verify that it throws an exception
        assertThrows(UnsupportedOperationException::class.java) {
            runBlocking {
                val request = FetchAndJoinCustomAudienceRequest(
                    uri,
                    name,
                    activationTime,
                    expirationTime,
                    userBiddingSignals
                )
                managerCompat!!.fetchAndJoinCustomAudience(request)
            }
        }.hasMessageThat().contains("API is not available. Min version is API 31 ext 10")
    }

    @Test
    fun testJoinCustomAudience() {
        Assume.assumeTrue("minSdkVersion = API 33 ext 4 or API 31/32 ext 9",
            mValidAdServicesSdkExtVersion || mValidAdExtServicesSdkExtVersion)

        val customAudienceManager =
            mockCustomAudienceManager(mContext, mValidAdExtServicesSdkExtVersion)
        setupResponse(customAudienceManager)
        val managerCompat = obtain(mContext)

        // Actually invoke the compat code.
        runBlocking {
            val customAudience = CustomAudience.Builder(buyer, name, uri, uri, ads)
                .setActivationTime(Instant.now())
                .setExpirationTime(Instant.now())
                .setUserBiddingSignals(userBiddingSignals)
                .setTrustedBiddingData(trustedBiddingSignals)
                .build()
            val request = JoinCustomAudienceRequest(customAudience)
            managerCompat!!.joinCustomAudience(request)
        }

        // Verify that the compat code was invoked correctly.
        val captor = ArgumentCaptor.forClass(
            android.adservices.customaudience.JoinCustomAudienceRequest::class.java
        )
        verify(customAudienceManager).joinCustomAudience(captor.capture(), any(), any())

        // Verify that the request that the compat code makes to the platform is correct.
        verifyJoinCustomAudienceRequest(captor.value)
    }

    @Test
    fun testFetchAndJoinCustomAudience() {
        Assume.assumeTrue("minSdkVersion = API 31 ext 10",
            AdServicesInfo.adServicesVersion() >= 10 ||
                AdServicesInfo.extServicesVersion() >= 10)

        val customAudienceManager =
            mockCustomAudienceManager(mContext, mValidAdExtServicesSdkExtVersion)
        setupFetchAndJoinResponse(customAudienceManager)
        val managerCompat = obtain(mContext)

        // Actually invoke the compat code.
        runBlocking {
            val request = FetchAndJoinCustomAudienceRequest(
                uri,
                name,
                activationTime,
                expirationTime,
                userBiddingSignals
            )
            managerCompat!!.fetchAndJoinCustomAudience(request)
        }

        // Verify that the compat code was invoked correctly.
        val captor = ArgumentCaptor.forClass(
            android.adservices.customaudience.FetchAndJoinCustomAudienceRequest::class.java
        )
        verify(customAudienceManager).fetchAndJoinCustomAudience(captor.capture(), any(), any())

        // Verify that the request that the compat code makes to the platform is correct.
        verifyFetchAndJoinCustomAudienceRequest(captor.value)
    }

    @Test
    fun testLeaveCustomAudience() {
        Assume.assumeTrue("minSdkVersion = API 33 ext 4 or API 31/32 ext 9",
            mValidAdServicesSdkExtVersion || mValidAdExtServicesSdkExtVersion)

        val customAudienceManager =
            mockCustomAudienceManager(mContext, mValidAdExtServicesSdkExtVersion)
        setupResponse(customAudienceManager)
        val managerCompat = obtain(mContext)

        // Actually invoke the compat code.
        runBlocking {
            val request = LeaveCustomAudienceRequest(buyer, name)
            managerCompat!!.leaveCustomAudience(request)
        }

        // Verify that the compat code was invoked correctly.
        val captor = ArgumentCaptor.forClass(
            android.adservices.customaudience.LeaveCustomAudienceRequest::class.java
        )
        verify(customAudienceManager).leaveCustomAudience(captor.capture(), any(), any())

        // Verify that the request that the compat code makes to the platform is correct.
        verifyLeaveCustomAudienceRequest(captor.value)
    }

    @SdkSuppress(minSdkVersion = 31)
    companion object {
        private lateinit var mContext: Context
        private val uri: Uri = Uri.parse("abc.com")
        private const val adtech = "1234"
        private val buyer: AdTechIdentifier = AdTechIdentifier(adtech)
        private const val name: String = "abc"
        private const val signals = "signals"
        private val userBiddingSignals: AdSelectionSignals = AdSelectionSignals(signals)
        private val keys: List<String> = listOf("key1", "key2")
        private val trustedBiddingSignals: TrustedBiddingData = TrustedBiddingData(uri, keys)
        private const val metadata = "metadata"
        private val ads: List<AdData> = listOf(AdData(uri, metadata))
        private val activationTime: Instant = Instant.ofEpochSecond(5)
        private val expirationTime: Instant = Instant.ofEpochSecond(10)

        private fun mockCustomAudienceManager(
            spyContext: Context,
            isExtServices: Boolean
        ): CustomAudienceManager {
            val customAudienceManager = mock(CustomAudienceManager::class.java)
            `when`(spyContext.getSystemService(CustomAudienceManager::class.java))
                .thenReturn(customAudienceManager)
            // only mock the .get() method if using extServices version
            if (isExtServices) {
                `when`(CustomAudienceManager.get(any())).thenReturn(customAudienceManager)
            }
            return customAudienceManager
        }

        private fun setupResponse(customAudienceManager: CustomAudienceManager) {
            val answer = { args: InvocationOnMock ->
                val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
                receiver.onResult(Object())
                null
            }
            doAnswer(answer).`when`(customAudienceManager).joinCustomAudience(any(), any(), any())
            doAnswer(answer).`when`(customAudienceManager).leaveCustomAudience(any(), any(), any())
        }

        private fun setupFetchAndJoinResponse(customAudienceManager: CustomAudienceManager) {
            val answer = { args: InvocationOnMock ->
                val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
                receiver.onResult(Object())
                null
            }
            doAnswer(answer).`when`(customAudienceManager)
                .fetchAndJoinCustomAudience(any(), any(), any())
        }

        private fun verifyJoinCustomAudienceRequest(
            joinCustomAudienceRequest: android.adservices.customaudience.JoinCustomAudienceRequest
        ) {
            // Set up the request that we expect the compat code to invoke.
            val adtechIdentifier = android.adservices.common.AdTechIdentifier.fromString(adtech)
            val userBiddingSignals =
                android.adservices.common.AdSelectionSignals.fromString(signals)
            val trustedBiddingSignals =
                android.adservices.customaudience.TrustedBiddingData.Builder()
                .setTrustedBiddingKeys(keys)
                .setTrustedBiddingUri(uri)
                .build()
            val customAudience = android.adservices.customaudience.CustomAudience.Builder()
                .setBuyer(adtechIdentifier)
                .setName(name)
                .setActivationTime(Instant.now())
                .setExpirationTime(Instant.now())
                .setBiddingLogicUri(uri)
                .setDailyUpdateUri(uri)
                .setUserBiddingSignals(userBiddingSignals)
                .setTrustedBiddingData(trustedBiddingSignals)
                .setAds(listOf(android.adservices.common.AdData.Builder()
                    .setRenderUri(uri)
                    .setMetadata(metadata)
                    .build()))
                .build()

            val expectedRequest =
                android.adservices.customaudience.JoinCustomAudienceRequest.Builder()
                    .setCustomAudience(customAudience)
                    .build()

            // Verify that the actual request matches the expected one.
            Truth.assertThat(expectedRequest.customAudience.ads.size ==
                joinCustomAudienceRequest.customAudience.ads.size).isTrue()
            Truth.assertThat(expectedRequest.customAudience.ads[0].renderUri ==
                joinCustomAudienceRequest.customAudience.ads[0].renderUri).isTrue()
            Truth.assertThat(expectedRequest.customAudience.ads[0].metadata ==
                joinCustomAudienceRequest.customAudience.ads[0].metadata).isTrue()
            Truth.assertThat(expectedRequest.customAudience.biddingLogicUri ==
                joinCustomAudienceRequest.customAudience.biddingLogicUri).isTrue()
            Truth.assertThat(expectedRequest.customAudience.buyer.toString() ==
                joinCustomAudienceRequest.customAudience.buyer.toString()).isTrue()
            Truth.assertThat(expectedRequest.customAudience.dailyUpdateUri ==
                joinCustomAudienceRequest.customAudience.dailyUpdateUri).isTrue()
            Truth.assertThat(expectedRequest.customAudience.name ==
                joinCustomAudienceRequest.customAudience.name).isTrue()
            Truth.assertThat(trustedBiddingSignals.trustedBiddingKeys ==
                joinCustomAudienceRequest.customAudience.trustedBiddingData!!.trustedBiddingKeys)
                .isTrue()
            Truth.assertThat(trustedBiddingSignals.trustedBiddingUri ==
                joinCustomAudienceRequest.customAudience.trustedBiddingData!!.trustedBiddingUri)
                .isTrue()
            Truth.assertThat(
                joinCustomAudienceRequest.customAudience.userBiddingSignals!!.toString() ==
                signals).isTrue()
        }

        private fun verifyFetchAndJoinCustomAudienceRequest(
            fetchAndJoinCustomAudienceRequest:
                android.adservices.customaudience.FetchAndJoinCustomAudienceRequest
        ) {
            // Set up the request that we expect the compat code to invoke.
            val expectedRequest = android.adservices.customaudience
                .FetchAndJoinCustomAudienceRequest
                .Builder(uri)
                .setName(name)
                .setActivationTime(activationTime)
                .setExpirationTime(expirationTime)
                .setUserBiddingSignals(userBiddingSignals.convertToAdServices())
                .build()

            // Verify that the actual request matches the expected one.
            Truth.assertThat(expectedRequest == fetchAndJoinCustomAudienceRequest).isTrue()
        }

        private fun verifyLeaveCustomAudienceRequest(
            leaveCustomAudienceRequest: android.adservices.customaudience.LeaveCustomAudienceRequest
        ) {
            // Set up the request that we expect the compat code to invoke.
            val adtechIdentifier = android.adservices.common.AdTechIdentifier.fromString(adtech)

            val expectedRequest = android.adservices.customaudience.LeaveCustomAudienceRequest
                .Builder()
                .setBuyer(adtechIdentifier)
                .setName(name)
                .build()

            // Verify that the actual request matches the expected one.
            Truth.assertThat(expectedRequest == leaveCustomAudienceRequest).isTrue()
        }
    }
}
