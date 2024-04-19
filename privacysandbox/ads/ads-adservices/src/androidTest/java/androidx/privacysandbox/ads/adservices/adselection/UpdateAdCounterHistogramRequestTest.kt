/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.common.FrequencyCapFilters
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFeatures.Ext8OptIn::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class UpdateAdCounterHistogramRequestTest {
    private val adSelectionId: Long = 1234L
    private val adEventType: Int = FrequencyCapFilters.AD_EVENT_TYPE_CLICK
    private val callerAdTech: AdTechIdentifier = AdTechIdentifier("1234")

    @Test
    fun testToString() {
        val result = "UpdateAdCounterHistogramRequest: adSelectionId=$adSelectionId, " +
            "adEventType=AD_EVENT_TYPE_CLICK, callerAdTech=$callerAdTech"
        val request = UpdateAdCounterHistogramRequest(adSelectionId, adEventType, callerAdTech)
        Truth.assertThat(request.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        val updateAdCounterHistogramRequest1 = UpdateAdCounterHistogramRequest(
            adSelectionId,
            adEventType,
            callerAdTech
        )
        var updateAdCounterHistogramRequest2 = UpdateAdCounterHistogramRequest(
            1234L,
            FrequencyCapFilters.AD_EVENT_TYPE_CLICK,
            AdTechIdentifier("1234")
        )
        Truth.assertThat(updateAdCounterHistogramRequest1 == updateAdCounterHistogramRequest2)
            .isTrue()

        var updateAdCounterHistogramRequest3 = UpdateAdCounterHistogramRequest(
            1234L,
            FrequencyCapFilters.AD_EVENT_TYPE_VIEW,
            AdTechIdentifier("1234")
        )
        Truth.assertThat(updateAdCounterHistogramRequest1 == updateAdCounterHistogramRequest3)
            .isFalse()
    }

    @Test
    fun testInvalidAdEventType() {
        assertThrows<IllegalArgumentException> {
            UpdateAdCounterHistogramRequest(
                1234L,
                -1 /* Invalid adEventType */,
                AdTechIdentifier("1234")
            )
        }.hasMessageThat().contains(
            "Ad event type must be one of AD_EVENT_TYPE_IMPRESSION, " +
                "AD_EVENT_TYPE_VIEW, or AD_EVENT_TYPE_CLICK"
        )
    }

    @Test
    fun testExceptionWinAdEventType() {
        assertThrows<IllegalArgumentException> {
            UpdateAdCounterHistogramRequest(
                1234L,
                FrequencyCapFilters.AD_EVENT_TYPE_WIN,
                AdTechIdentifier("1234")
            )
        }.hasMessageThat().contains("Win event types cannot be manually updated.")
    }
}
