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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFeatures.Ext10OptIn::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class PersistAdSelectionResultRequestTest {
    private val adSelectionId = 1234L
    private val seller: AdTechIdentifier = AdTechIdentifier("1234")
    private val adSelectionResult = byteArrayOf(0x01, 0x02, 0x03, 0x04)
    @Test
    fun testToString() {
        val result = "PersistAdSelectionResultRequest: adSelectionId=$adSelectionId, " +
            "seller=$seller, adSelectionResult=$adSelectionResult"
        val request = PersistAdSelectionResultRequest(adSelectionId, seller, adSelectionResult)

        Truth.assertThat(request.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        val persistAdSelectionResultRequest = PersistAdSelectionResultRequest(
            adSelectionId,
            seller,
            adSelectionResult
        )
        var persistAdSelectionResultRequest2 = PersistAdSelectionResultRequest(
            1234L,
            AdTechIdentifier("1234"),
            byteArrayOf(0x01, 0x02, 0x03, 0x04)
        )

        Truth.assertThat(persistAdSelectionResultRequest == persistAdSelectionResultRequest2)
            .isTrue()
    }
}
