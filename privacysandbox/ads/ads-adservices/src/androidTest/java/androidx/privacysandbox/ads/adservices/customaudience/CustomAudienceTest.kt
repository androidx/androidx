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

import android.net.Uri
import androidx.privacysandbox.ads.adservices.common.AdData
import androidx.privacysandbox.ads.adservices.common.AdFilters
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.common.FrequencyCapFilters
import androidx.privacysandbox.ads.adservices.common.KeyedFrequencyCap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import java.time.Duration
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFeatures.Ext8OptIn::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class CustomAudienceTest {
    private val uri: Uri = Uri.parse("abc.com")
    private val buyer: AdTechIdentifier = AdTechIdentifier("1234")
    private val name: String = "abc"
    private val activationTime: Instant = Instant.now()
    private val expirationTime: Instant = Instant.now()
    private val userBiddingSignals: AdSelectionSignals = AdSelectionSignals("signals")
    private val keys: List<String> = listOf("key1", "key2")
    private val trustedBiddingSignals: TrustedBiddingData = TrustedBiddingData(uri, keys)
    private val adCounterKeys: Set<Int> = setOf<Int>(1, 2, 3)
    private val interval = Duration.ofSeconds(1)
    private val adFilters: AdFilters = AdFilters(
        FrequencyCapFilters(
            keyedFrequencyCapsForViewEvents =
            listOf(KeyedFrequencyCap(1, 3, interval)),
        )
    )
    private val ads: List<AdData> = listOf(
        AdData(uri, "metadata", adCounterKeys, adFilters)
    )

    @Test
    fun testToStringAndEquals() {
        val result = "CustomAudience: buyer=abc.com, name=abc, activationTime=$activationTime, " +
            "expirationTime=$expirationTime, dailyUpdateUri=abc.com, " +
            "userBiddingSignals=AdSelectionSignals: signals, " +
            "trustedBiddingSignals=TrustedBiddingData: trustedBiddingUri=abc.com " +
            "trustedBiddingKeys=[key1, key2], biddingLogicUri=abc.com, " +
            "ads=[AdData: renderUri=abc.com, metadata='metadata', adCounterKeys=[1, 2, 3], " +
            "adFilters=AdFilters: frequencyCapFilters=FrequencyCapFilters: " +
            "keyedFrequencyCapsForWinEvents=[], " +
            "keyedFrequencyCapsForImpressionEvents=[], " +
            "keyedFrequencyCapsForViewEvents=" +
            "[KeyedFrequencyCap: adCounterKey=1, maxCount=3, interval=$interval], " +
            "keyedFrequencyCapsForClickEvents=[], adRenderId=null]"

        val customAudience = CustomAudience(
            buyer,
            name,
            uri,
            uri,
            ads,
            activationTime,
            expirationTime,
            userBiddingSignals,
            trustedBiddingSignals
        )
        Truth.assertThat(customAudience.toString()).isEqualTo(result)

        // Verify Builder.
        val customAudienceBuilder2 = CustomAudience.Builder(buyer, name, uri, uri, ads)
            .setActivationTime(activationTime)
            .setExpirationTime(expirationTime)
            .setUserBiddingSignals(userBiddingSignals)
            .setTrustedBiddingData(trustedBiddingSignals)
        Truth.assertThat(customAudienceBuilder2.build().toString()).isEqualTo(result)

        // Test equality.
        Truth.assertThat(customAudience == customAudienceBuilder2.build()).isTrue()

        // Reset values of Builder.
        customAudienceBuilder2.setName("newName")
        Truth.assertThat(customAudience == customAudienceBuilder2.build()).isFalse()
    }
}
