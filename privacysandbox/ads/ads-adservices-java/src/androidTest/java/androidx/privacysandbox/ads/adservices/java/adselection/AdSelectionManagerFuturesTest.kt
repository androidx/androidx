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

package androidx.privacysandbox.ads.adservices.java.adselection

import android.content.Context
import android.net.Uri
import android.os.OutcomeReceiver
import android.view.InputEvent
import android.view.KeyEvent
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionConfig
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionFromOutcomesConfig
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionOutcome
import androidx.privacysandbox.ads.adservices.adselection.GetAdSelectionDataRequest
import androidx.privacysandbox.ads.adservices.adselection.PersistAdSelectionResultRequest
import androidx.privacysandbox.ads.adservices.adselection.ReportEventRequest
import androidx.privacysandbox.ads.adservices.adselection.ReportImpressionRequest
import androidx.privacysandbox.ads.adservices.adselection.UpdateAdCounterHistogramRequest
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.common.FrequencyCapFilters
import androidx.privacysandbox.ads.adservices.java.VersionCompatUtil
import androidx.privacysandbox.ads.adservices.java.adselection.AdSelectionManagerFutures.Companion.from
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.dx.mockito.inline.extended.StaticMockitoSession
import com.google.common.truth.Truth
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
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
@SdkSuppress(minSdkVersion = 30)
class AdSelectionManagerFuturesTest {

    private var mSession: StaticMockitoSession? = null
    private val mValidAdExtServicesSdkExtVersion = VersionCompatUtil.isSWithMinExtServicesVersion(9)

    @Before
    fun setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext<Context>())

        if (mValidAdExtServicesSdkExtVersion) {
            // setup a mockitoSession to return the mocked manager
            // when the static method .get() is called
            mSession = ExtendedMockito.mockitoSession()
                .mockStatic(android.adservices.adselection.AdSelectionManager::class.java)
                .strictness(Strictness.LENIENT)
                .startMocking()
        }
    }

    @After
    fun tearDown() {
        mSession?.finishMocking()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 34, minSdkVersion = 30)
    fun testAdSelectionOlderVersions() {
        Assume.assumeFalse("maxSdkVersion = API 33/34 ext 3 or API 31/32 ext 8",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion=*/ 4,
                /* minExtServicesVersion=*/ 9))
        Truth.assertThat(from(mContext)).isEqualTo(null)
    }

    @Test
    @SdkSuppress(maxSdkVersion = 34, minSdkVersion = 31)
    fun testUpdateAdCounterHistogramOlderVersions() {
        /* AdServices or ExtServices are present */
        Assume.assumeTrue("minSdkVersion = API 33 ext 4 or API 31/32 ext 9",
                          VersionCompatUtil.isTestableVersion(
                              /* minAdServicesVersion= */ 4,
                              /* minExtServicesVersion=*/ 9))

        /* API is not available */
        Assume.assumeFalse("maxSdkVersion = API 33/34 ext 7 or API 31/32 ext 8",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion=*/ 8,
                /* minExtServicesVersion=*/ 9))

        val managerCompat = from(mContext)
        val updateAdCounterHistogramRequest = UpdateAdCounterHistogramRequest(
            adSelectionId,
            adEventType,
            seller
        )

        // Verify that it throws an exception
        val exception = assertThrows(ExecutionException::class.java) {
            managerCompat!!.updateAdCounterHistogramAsync(updateAdCounterHistogramRequest).get()
        }.hasCauseThat()
        exception.isInstanceOf(UnsupportedOperationException::class.java)
        exception.hasMessageThat().contains("API is unsupported. Min version is API 33 ext 8 or " +
            "API 31/32 ext 9")
    }

    @Test
    @SdkSuppress(maxSdkVersion = 34, minSdkVersion = 31)
    fun testReportEventOlderVersions() {
        /* AdServices or ExtServices are present */
        Assume.assumeTrue("minSdkVersion = API 33 ext 4 or API 31/32 ext 9",
                          VersionCompatUtil.isTestableVersion(
                              /* minAdServicesVersion= */ 4,
                              /* minExtServicesVersion=*/ 9))

        /* API is not available */
        Assume.assumeFalse("maxSdkVersion = API 33/34 ext 7 or API 31/32 ext 8",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion=*/ 8,
                /* minExtServicesVersion=*/ 9))

        val managerCompat = from(mContext)
        val reportEventRequest = ReportEventRequest(
            adSelectionId,
            eventKey,
            eventData,
            reportingDestinations
        )

        // Verify that it throws an exception
        val exception = assertThrows(ExecutionException::class.java) {
            managerCompat!!.reportEventAsync(reportEventRequest).get()
        }.hasCauseThat()
        exception.isInstanceOf(UnsupportedOperationException::class.java)
        exception.hasMessageThat().contains("API is unsupported. Min version is API 33 ext 8 or " +
            "API 31/32 ext 9")
    }

    @Test
    @SdkSuppress(maxSdkVersion = 34, minSdkVersion = 31)
    fun testGetAdSelectionDataOlderVersions() {
        /* AdServices or ExtServices are present */
        Assume.assumeTrue("minSdkVersion = API 33 ext 4 or API 31/32 ext 9",
                          VersionCompatUtil.isTestableVersion(
                              /* minAdServicesVersion= */ 4,
                              /* minExtServicesVersion=*/ 9))

        /* API is not available */
        Assume.assumeFalse("maxSdkVersion = API 31-34 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion=*/ 10,
                /* minExtServicesVersion=*/ 10))

        mockAdSelectionManager(mContext, mValidAdExtServicesSdkExtVersion)
        val managerCompat = from(mContext)
        val getAdSelectionDataRequest = GetAdSelectionDataRequest(seller)

        // Verify that it throws an exception
        val exception = assertThrows(ExecutionException::class.java) {
            managerCompat!!.getAdSelectionDataAsync(getAdSelectionDataRequest).get()
        }.hasCauseThat()
        exception.isInstanceOf(UnsupportedOperationException::class.java)
        exception.hasMessageThat().contains("API is not available. Min version is API 31 ext 10")
    }

    @Test
    @SdkSuppress(maxSdkVersion = 34, minSdkVersion = 31)
    fun testPersistAdSelectionResultOlderVersions() {
        /* AdServices or ExtServices are present */
        Assume.assumeTrue("minSdkVersion = API 33 ext 4 or API 31/32 ext 9",
                          VersionCompatUtil.isTestableVersion(
                              /* minAdServicesVersion= */ 4,
                              /* minExtServicesVersion=*/ 9))

        /* API is not available */
        Assume.assumeFalse("maxSdkVersion = API 31-34 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion=*/ 10,
                /* minExtServicesVersion=*/ 10))

        mockAdSelectionManager(mContext, mValidAdExtServicesSdkExtVersion)
        val managerCompat = from(mContext)
        val persistAdSelectionResultRequest = PersistAdSelectionResultRequest(
            adSelectionId,
            seller,
            adSelectionData
        )
        // Verify that it throws an exception
        val exception = assertThrows(ExecutionException::class.java) {
            managerCompat!!.persistAdSelectionResultAsync(persistAdSelectionResultRequest).get()
        }.hasCauseThat()
        exception.isInstanceOf(UnsupportedOperationException::class.java)
        exception.hasMessageThat().contains("API is not available. Min version is API 31 ext 10")
    }

    @Test
    @SdkSuppress(maxSdkVersion = 34, minSdkVersion = 31)
    fun testReportImpressionOlderVersions() {
        /* AdServices or ExtServices are present */
        Assume.assumeTrue("minSdkVersion = API 33 ext 4 or API 31/32 ext 9",
                          VersionCompatUtil.isTestableVersion(
                              /* minAdServicesVersion= */ 4,
                              /* minExtServicesVersion=*/ 9))

        /* API is not available */
        Assume.assumeFalse("maxSdkVersion = API 31-34 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion=*/ 10,
                /* minExtServicesVersion=*/ 10))

        mockAdSelectionManager(mContext, mValidAdExtServicesSdkExtVersion)
        val managerCompat = from(mContext)
        val reportImpressionRequest = ReportImpressionRequest(adSelectionId)

        // Verify that it throws an exception
        val exception = assertThrows(ExecutionException::class.java) {
            managerCompat!!.reportImpressionAsync(reportImpressionRequest).get()
        }.hasCauseThat()
        exception.isInstanceOf(UnsupportedOperationException::class.java)
        exception.hasMessageThat().contains("adSelectionConfig is mandatory for" +
            "API versions lower than ext 10")
    }

    @Test
    @SdkSuppress(maxSdkVersion = 34, minSdkVersion = 31)
    fun testSelectAdsFromOutcomesOlderVersions() {
        /* AdServices or ExtServices are present */
        Assume.assumeTrue("minSdkVersion = API 33 ext 4 or API 31/32 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion= */ 4,
                /* minExtServicesVersion=*/ 9))

        /* API is not available */
        Assume.assumeFalse("maxSdkVersion = API 31-34 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion=*/ 10,
                /* minExtServicesVersion=*/ 10))

        mockAdSelectionManager(mContext, mValidAdExtServicesSdkExtVersion)
        val managerCompat = from(mContext)

        // Verify that it throws an exception
        val exception = assertThrows(ExecutionException::class.java) {
            managerCompat!!.selectAdsAsync(adSelectionFromOutcomesConfig).get()
        }.hasCauseThat()
        exception.isInstanceOf(UnsupportedOperationException::class.java)
        exception.hasMessageThat().contains("API is not available. Min version is API 31 ext 10")
    }

    @Test
    fun testSelectAds() {
        Assume.assumeTrue("minSdkVersion = API 33 ext 4 or API 31/32 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion= */ 4,
                /* minExtServicesVersion=*/ 9))

        val adSelectionManager = mockAdSelectionManager(mContext, mValidAdExtServicesSdkExtVersion)
        setupAdSelectionResponse(adSelectionManager)
        val managerCompat = from(mContext)

        // Actually invoke the compat code.
        val result: ListenableFuture<AdSelectionOutcome> =
            managerCompat!!.selectAdsAsync(adSelectionConfig)

        // Verify that the result of the compat call is correct.
        verifyResponse(result.get())

        // Verify that the compat code was invoked correctly.
        val captor = ArgumentCaptor.forClass(
            android.adservices.adselection.AdSelectionConfig::class.java)
        verify(adSelectionManager).selectAds(captor.capture(), any(), any())

        // Verify that the request that the compat code makes to the platform is correct.
        verifyRequest(captor.value)
    }

    @Test
    fun testSelectAdsFromOutcomes() {
        Assume.assumeTrue("minSdkVersion = API 31 ext 10",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion= */ 10,
                /* minExtServicesVersion=*/ 10))

        val adSelectionManager = mockAdSelectionManager(mContext, mValidAdExtServicesSdkExtVersion)
        setupAdSelectionFromOutcomesResponse(adSelectionManager)
        val managerCompat = from(mContext)

        // Actually invoke the compat code.
        val result: ListenableFuture<AdSelectionOutcome> =
            managerCompat!!.selectAdsAsync(adSelectionFromOutcomesConfig)

        // Verify that the result of the compat call is correct.
        verifyResponse(result.get())

        // Verify that the compat code was invoked correctly.
        val captor = ArgumentCaptor.forClass(
            android.adservices.adselection.AdSelectionFromOutcomesConfig::class.java)
        verify(adSelectionManager).selectAds(captor.capture(), any(), any())

        // Verify that the request that the compat code makes to the platform is correct.
        verifyRequest(captor.value)
    }

    @Test
    @SuppressWarnings("NewApi")
    fun testReportImpression() {
        Assume.assumeTrue("minSdkVersion = API 33 ext 4 or API 31/32 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion= */ 4,
                /* minExtServicesVersion=*/ 9))

        val adSelectionManager = mockAdSelectionManager(mContext, mValidAdExtServicesSdkExtVersion)
        setupAdSelectionResponse(adSelectionManager)
        val managerCompat = from(mContext)
        val reportImpressionRequest = ReportImpressionRequest(adSelectionId, adSelectionConfig)

        // Actually invoke the compat code.
        managerCompat!!.reportImpressionAsync(reportImpressionRequest).get()

        // Verify that the compat code was invoked correctly.
        val captor = ArgumentCaptor.forClass(
            android.adservices.adselection.ReportImpressionRequest::class.java)
        verify(adSelectionManager).reportImpression(captor.capture(), any(), any())

        // Verify that the request that the compat code makes to the platform is correct.
        verifyReportImpressionRequest(captor.value)
    }

    @Test
    fun testUpdateAdCounterHistogram() {
        Assume.assumeTrue("minSdkVersion = API 33 ext 8 or API 31/32 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion= */ 8,
                /* minExtServicesVersion=*/ 9))

        val adSelectionManager = mockAdSelectionManager(mContext, mValidAdExtServicesSdkExtVersion)
        setupUpdateAdCounterHistogramResponse(adSelectionManager)
        val managerCompat = from(mContext)
        val updateAdCounterHistogramRequest = UpdateAdCounterHistogramRequest(
            adSelectionId,
            adEventType,
            seller
        )

        // Actually invoke the compat code.
        runBlocking {
            managerCompat!!.updateAdCounterHistogramAsync(updateAdCounterHistogramRequest)
        }

        // Verify that the compat code was invoked correctly.
        val captor = ArgumentCaptor.forClass(
            android.adservices.adselection.UpdateAdCounterHistogramRequest::class.java)
        verify(adSelectionManager).updateAdCounterHistogram(captor.capture(), any(), any())

        // Verify that the request that the compat code makes to the platform is correct.
        verifyUpdateAdCounterHistogramRequest(captor.value)
    }

    @Test
    fun testReportEvent() {
        Assume.assumeTrue("minSdkVersion = API 33 ext 8 or API 31/32 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion= */ 8,
                /* minExtServicesVersion=*/ 9))

        val adSelectionManager = mockAdSelectionManager(mContext, mValidAdExtServicesSdkExtVersion)
        setupReportEventResponse(adSelectionManager)
        val managerCompat = from(mContext)
        val reportEventRequest = ReportEventRequest(
            adSelectionId,
            eventKey,
            eventData,
            reportingDestinations,
            inputEvent
        )

        // Actually invoke the compat code.
        runBlocking {
            managerCompat!!.reportEventAsync(reportEventRequest)
        }

        // Verify that the compat code was invoked correctly.
        val captor = ArgumentCaptor.forClass(
            android.adservices.adselection.ReportEventRequest::class.java)
        verify(adSelectionManager).reportEvent(captor.capture(), any(), any())

        // Verify that the request that the compat code makes to the platform is correct.
        verifyReportEventRequest(captor.value)
    }

    @Test
    fun testPersistAdSelectionResult() {
        Assume.assumeTrue("minSdkVersion = API 31 ext 10",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion= */ 10,
                /* minExtServicesVersion=*/ 10))

        val adSelectionManager = mockAdSelectionManager(mContext, mValidAdExtServicesSdkExtVersion)
        setupGetAdSelectionResponse(adSelectionManager)

        val managerCompat = from(mContext)
        val persistAdSelectionResultRequest = PersistAdSelectionResultRequest(
            adSelectionId,
            seller,
            adSelectionData
        )

        // Actually invoke the compat code.
        val result = runBlocking {
            managerCompat!!.persistAdSelectionResultAsync(persistAdSelectionResultRequest)
        }

        // Verify that the compat code was invoked correctly.
        val captor = ArgumentCaptor.forClass(
            android.adservices.adselection.PersistAdSelectionResultRequest::class.java)
        verify(adSelectionManager).persistAdSelectionResult(captor.capture(), any(), any())

        // Verify that the request that the compat code makes to the platform is correct.
        verifyPersistAdSelectionResultRequest(captor.value)

        verifyResponse(result.get())
    }

    @SuppressWarnings("NewApi")
    @SdkSuppress(minSdkVersion = 30)
    companion object {
        private lateinit var mContext: Context
        private const val adSelectionId = 1234L
        private const val adId = "1234"
        private val seller: AdTechIdentifier = AdTechIdentifier(adId)
        private val decisionLogicUri: Uri = Uri.parse("www.abc.com")
        private val customAudienceBuyers: List<AdTechIdentifier> = listOf(seller)
        private const val adSelectionSignalsStr = "adSelSignals"
        private val adSelectionSignals: AdSelectionSignals =
            AdSelectionSignals(adSelectionSignalsStr)
        private const val sellerSignalsStr = "sellerSignals"
        private val sellerSignals: AdSelectionSignals = AdSelectionSignals(sellerSignalsStr)
        private val perBuyerSignals: Map<AdTechIdentifier, AdSelectionSignals> =
            mutableMapOf(Pair(seller, sellerSignals))
        private val trustedScoringSignalsUri: Uri = Uri.parse("www.xyz.com")
        private val adSelectionConfig = AdSelectionConfig(
            seller,
            decisionLogicUri,
            customAudienceBuyers,
            adSelectionSignals,
            sellerSignals,
            perBuyerSignals,
            trustedScoringSignalsUri)
        private const val adEventType = FrequencyCapFilters.AD_EVENT_TYPE_VIEW
        private const val eventKey = "click"
        private const val eventData = "{\"key\":\"value\"}"
        private const val reportingDestinations =
            ReportEventRequest.FLAG_REPORTING_DESTINATION_BUYER
        private val adSelectionData = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        private val inputEvent: InputEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_1)
        private val adSelectionIds: List<Long> = listOf(10, 11, 12)
        private val selectionLogicUri: Uri = Uri.parse("www.abc.com")
        private val adSelectionFromOutcomesConfig = AdSelectionFromOutcomesConfig(
            seller,
            adSelectionIds,
            adSelectionSignals,
            selectionLogicUri
        )

        // Response.
        private val renderUri = Uri.parse("render-uri.com")

        private fun mockAdSelectionManager(
            spyContext: Context,
            isExtServices: Boolean
        ): android.adservices.adselection.AdSelectionManager {
            val adSelectionManager =
                mock(android.adservices.adselection.AdSelectionManager::class.java)
            // mock the .get() method if using extServices version, otherwise mock getSystemService
            if (isExtServices) {
                `when`(android.adservices.adselection.AdSelectionManager.get(any()))
                    .thenReturn(adSelectionManager)
            } else {
                `when`(spyContext.getSystemService(
                    android.adservices.adselection.AdSelectionManager::class.java))
                    .thenReturn(adSelectionManager)
            }
            return adSelectionManager
        }

        private fun setupAdSelectionResponse(
            adSelectionManager: android.adservices.adselection.AdSelectionManager
        ) {
            // Set up the response that AdSelectionManager will return when the compat code calls
            // it.
            val response = android.adservices.adselection.AdSelectionOutcome.Builder()
                .setAdSelectionId(adSelectionId)
                .setRenderUri(renderUri)
                .build()
            val answer = { args: InvocationOnMock ->
                val receiver = args.getArgument<OutcomeReceiver<
                    android.adservices.adselection.AdSelectionOutcome, Exception>>(2)
                receiver.onResult(response)
                null
            }
            doAnswer(answer)
                .`when`(adSelectionManager).selectAds(
                    any<android.adservices.adselection.AdSelectionConfig>(),
                    any(),
                    any()
                )

            val answer2 = { args: InvocationOnMock ->
                val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
                receiver.onResult(Object())
                null
            }
            doAnswer(answer2).`when`(adSelectionManager).reportImpression(any(), any(), any())
        }

        private fun setupAdSelectionFromOutcomesResponse(
            adSelectionManager: android.adservices.adselection.AdSelectionManager
        ) {
            // Set up the response that AdSelectionManager will return when the compat code calls
            // it.
            val response = android.adservices.adselection.AdSelectionOutcome.Builder()
                .setAdSelectionId(adSelectionId)
                .setRenderUri(renderUri)
                .build()
            val answer = { args: InvocationOnMock ->
                val receiver = args.getArgument<OutcomeReceiver<
                    android.adservices.adselection.AdSelectionOutcome, Exception>>(2)
                receiver.onResult(response)
                null
            }
            doAnswer(answer)
                .`when`(adSelectionManager).selectAds(
                    any<android.adservices.adselection.AdSelectionFromOutcomesConfig>(),
                    any(),
                    any()
                )
        }

        private fun setupUpdateAdCounterHistogramResponse(
            adSelectionManager: android.adservices.adselection.AdSelectionManager
        ) {
            // Set up the response that AdSelectionManager will return when the compat code calls
            // UpdateAdCounterHistogramResponse().
            val answer = { args: InvocationOnMock ->
                val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
                receiver.onResult(Object())
                null
            }
            doAnswer(answer)
                .`when`(adSelectionManager).updateAdCounterHistogram(
                    any(),
                    any(),
                    any()
                )
        }

        private fun setupReportEventResponse(
            adSelectionManager: android.adservices.adselection.AdSelectionManager
        ) {
            // Set up the response that AdSelectionManager will return when the compat code calls
            // ReportEvent().
            val answer = { args: InvocationOnMock ->
                val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
                receiver.onResult(Object())
                null
            }
            doAnswer(answer)
                .`when`(adSelectionManager).reportEvent(
                    any(),
                    any(),
                    any()
                )
        }

        private fun setupGetAdSelectionResponse(
            adSelectionManager: android.adservices.adselection.AdSelectionManager
        ) {
            // There is no way to create a GetAdSelectionDataOutcome instance outside of adservices

            val response2 = android.adservices.adselection.AdSelectionOutcome.Builder()
                .setAdSelectionId(adSelectionId)
                .setRenderUri(renderUri)
                .build()
            val answer2 = { args: InvocationOnMock ->
                val receiver = args.getArgument<OutcomeReceiver<
                    android.adservices.adselection.AdSelectionOutcome, Exception>>(2)
                receiver.onResult(response2)
                null
            }
            doAnswer(answer2)
                .`when`(adSelectionManager).persistAdSelectionResult(any(), any(), any())
        }

        private fun verifyRequest(request: android.adservices.adselection.AdSelectionConfig) {
            // Set up the request that we expect the compat code to invoke.
            val expectedRequest = getPlatformAdSelectionConfig()

            Assert.assertEquals(expectedRequest, request)
        }

        private fun verifyRequest(
            request: android.adservices.adselection.AdSelectionFromOutcomesConfig
        ) {
            // Set up the request that we expect the compat code to invoke.
            val expectedRequest = getPlatformAdSelectionFromOutcomesConfig()

            Assert.assertEquals(expectedRequest, request)
        }

        private fun verifyResponse(
            outcome: AdSelectionOutcome
        ) {
            val expectedOutcome =
                AdSelectionOutcome(
                    adSelectionId,
                    renderUri)
            Assert.assertEquals(expectedOutcome, outcome)
        }

        private fun getPlatformAdSelectionConfig():
            android.adservices.adselection.AdSelectionConfig {
            val adTechIdentifier = android.adservices.common.AdTechIdentifier.fromString(adId)
            return android.adservices.adselection.AdSelectionConfig.Builder()
                .setAdSelectionSignals(
                    android.adservices.common.AdSelectionSignals.fromString(adSelectionSignalsStr))
                .setCustomAudienceBuyers(listOf(adTechIdentifier))
                .setDecisionLogicUri(decisionLogicUri)
                .setPerBuyerSignals(mutableMapOf(Pair(
                    adTechIdentifier,
                    android.adservices.common.AdSelectionSignals.fromString(sellerSignalsStr))))
                .setSeller(adTechIdentifier)
                .setSellerSignals(
                    android.adservices.common.AdSelectionSignals.fromString(sellerSignalsStr))
                .setTrustedScoringSignalsUri(trustedScoringSignalsUri)
                .build()
        }

        private fun getPlatformAdSelectionFromOutcomesConfig():
            android.adservices.adselection.AdSelectionFromOutcomesConfig {
            val adTechIdentifier = android.adservices.common.AdTechIdentifier.fromString(adId)
            return android.adservices.adselection.AdSelectionFromOutcomesConfig.Builder()
                .setSelectionSignals(
                    android.adservices.common.AdSelectionSignals.fromString(adSelectionSignalsStr)
                )
                .setAdSelectionIds(adSelectionIds)
                .setSelectionLogicUri(selectionLogicUri)
                .setSeller(adTechIdentifier)
                .build()
        }

        private fun verifyReportImpressionRequest(
            request: android.adservices.adselection.ReportImpressionRequest
        ) {
            val expectedRequest = android.adservices.adselection.ReportImpressionRequest(
                adSelectionId,
                getPlatformAdSelectionConfig())
            Assert.assertEquals(expectedRequest.adSelectionId, request.adSelectionId)
            Assert.assertEquals(expectedRequest.adSelectionConfig, request.adSelectionConfig)
        }

        private fun verifyUpdateAdCounterHistogramRequest(
            request: android.adservices.adselection.UpdateAdCounterHistogramRequest
        ) {
            val adTechIdentifier = android.adservices.common.AdTechIdentifier.fromString(adId)
            val expectedRequest = android.adservices.adselection.UpdateAdCounterHistogramRequest
                .Builder(adSelectionId, adEventType, adTechIdentifier)
                .build()
            Assert.assertEquals(expectedRequest, request)
        }

        private fun verifyReportEventRequest(
            request: android.adservices.adselection.ReportEventRequest
        ) {
            val checkInputEvent = VersionCompatUtil.isTestableVersion(10, 10)
            val expectedRequestBuilder = android.adservices.adselection.ReportEventRequest.Builder(
                adSelectionId,
                eventKey,
                eventData,
                reportingDestinations)

            if (checkInputEvent)
                expectedRequestBuilder.setInputEvent(inputEvent)

            val expectedRequest = expectedRequestBuilder.build()
            Assert.assertEquals(expectedRequest.adSelectionId, request.adSelectionId)
            Assert.assertEquals(expectedRequest.key, request.key)
            Assert.assertEquals(expectedRequest.data, request.data)
            Assert.assertEquals(expectedRequest.reportingDestinations,
                request.reportingDestinations)
            if (checkInputEvent)
                Assert.assertEquals(expectedRequest.inputEvent,
                    request.inputEvent)
        }

        private fun verifyPersistAdSelectionResultRequest(
            request: android.adservices.adselection.PersistAdSelectionResultRequest
        ) {
            val adTechIdentifier = android.adservices.common.AdTechIdentifier.fromString(adId)
            val expectedRequest = android.adservices.adselection.PersistAdSelectionResultRequest
                .Builder()
                .setAdSelectionId(adSelectionId)
                .setSeller(adTechIdentifier)
                .setAdSelectionResult(adSelectionData)
                .build()
            Assert.assertEquals(expectedRequest.adSelectionId, request.adSelectionId)
            Assert.assertEquals(expectedRequest.seller, request.seller)
            Assert.assertTrue(expectedRequest.adSelectionResult
                .contentEquals(request.adSelectionResult))
        }
    }
}
