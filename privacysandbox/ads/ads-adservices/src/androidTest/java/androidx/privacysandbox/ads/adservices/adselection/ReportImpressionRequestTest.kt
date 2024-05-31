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

package androidx.privacysandbox.ads.adservices.adselection

import android.net.Uri
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ReportImpressionRequestTest {
    private val adSelectionId = 1234L
    private val seller: AdTechIdentifier = AdTechIdentifier("1234")
    private val decisionLogicUri: Uri = Uri.parse("www.abc.com")
    private val customAudienceBuyers: List<AdTechIdentifier> = listOf(seller)
    private val adSelectionSignals: AdSelectionSignals = AdSelectionSignals("adSelSignals")
    private val sellerSignals: AdSelectionSignals = AdSelectionSignals("sellerSignals")
    private val perBuyerSignals: Map<AdTechIdentifier, AdSelectionSignals> =
        mutableMapOf(Pair(seller, sellerSignals))
    private val trustedScoringSignalsUri: Uri = Uri.parse("www.xyz.com")
    private val adSelectionConfig =
        AdSelectionConfig(
            seller,
            decisionLogicUri,
            customAudienceBuyers,
            adSelectionSignals,
            sellerSignals,
            perBuyerSignals,
            trustedScoringSignalsUri
        )

    @Test
    fun testToString() {
        val result =
            "ReportImpressionRequest: adSelectionId=$adSelectionId, " +
                "adSelectionConfig=$adSelectionConfig"
        val request = ReportImpressionRequest(adSelectionId, adSelectionConfig)
        Truth.assertThat(request.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        val reportImpressionRequest = ReportImpressionRequest(adSelectionId, adSelectionConfig)
        var adSelectionConfig2 =
            AdSelectionConfig(
                AdTechIdentifier("1234"),
                Uri.parse("www.abc.com"),
                customAudienceBuyers,
                adSelectionSignals,
                sellerSignals,
                perBuyerSignals,
                trustedScoringSignalsUri
            )
        var reportImpressionRequest2 = ReportImpressionRequest(adSelectionId, adSelectionConfig2)
        Truth.assertThat(reportImpressionRequest == reportImpressionRequest2).isTrue()
    }
}
