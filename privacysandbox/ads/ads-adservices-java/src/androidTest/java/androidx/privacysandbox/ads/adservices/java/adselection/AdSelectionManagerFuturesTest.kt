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
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionConfig
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionOutcome
import androidx.privacysandbox.ads.adservices.adselection.ReportImpressionRequest
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.java.adselection.AdSelectionManagerFutures.Companion.from
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import com.google.common.util.concurrent.ListenableFuture
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

@SmallTest
@SuppressWarnings("NewApi")
@RunWith(AndroidJUnit4::class)
class AdSelectionManagerFuturesTest {

    @Before
    fun setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    fun testAdSelectionOlderVersions() {
        val sdkExtVersion = SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)

        Assume.assumeTrue("maxSdkVersion = API 33 ext 3", sdkExtVersion < 4)
        Truth.assertThat(from(mContext)).isEqualTo(null)
    }

    @Test
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    fun testSelectAds() {
        val sdkExtVersion = SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)

        Assume.assumeTrue("minSdkVersion = API 33 ext 4", sdkExtVersion >= 4)
        val adSelectionManager = mockAdSelectionManager(mContext)
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
    @SuppressWarnings("NewApi")
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    fun testReportImpression() {
        val sdkExtVersion = SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)

        Assume.assumeTrue("minSdkVersion = API 33 ext 4", sdkExtVersion >= 4)
        val adSelectionManager = mockAdSelectionManager(mContext)
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

    @SuppressWarnings("NewApi")
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
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

        // Response.
        private val renderUri = Uri.parse("render-uri.com")

        private fun mockAdSelectionManager(
            spyContext: Context
        ): android.adservices.adselection.AdSelectionManager {
            val adSelectionManager =
                mock(android.adservices.adselection.AdSelectionManager::class.java)
            `when`(spyContext.getSystemService(
                android.adservices.adselection.AdSelectionManager::class.java))
                .thenReturn(adSelectionManager)
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
                    any(),
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

        private fun verifyRequest(request: android.adservices.adselection.AdSelectionConfig) {
            // Set up the request that we expect the compat code to invoke.
            val expectedRequest = getPlatformAdSelectionConfig()

            Assert.assertEquals(expectedRequest, request)
        }

        private fun verifyResponse(
            outcome: androidx.privacysandbox.ads.adservices.adselection.AdSelectionOutcome
        ) {
            val expectedOutcome =
                androidx.privacysandbox.ads.adservices.adselection.AdSelectionOutcome(
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

        private fun verifyReportImpressionRequest(
            request: android.adservices.adselection.ReportImpressionRequest
        ) {
            val expectedRequest = android.adservices.adselection.ReportImpressionRequest(
                adSelectionId,
                getPlatformAdSelectionConfig())
            Assert.assertEquals(expectedRequest.adSelectionId, request.adSelectionId)
            Assert.assertEquals(expectedRequest.adSelectionConfig, request.adSelectionConfig)
        }
    }
}