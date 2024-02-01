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

package androidx.privacysandbox.ads.adservices.customaudience

import android.net.Uri
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFeatures.Ext10OptIn::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class FetchAndJoinCustomAudienceRequestTest {
    private val fetchUri: Uri = Uri.parse("abc.com")
    private val name: String = "abc"
    private val activationTime: Instant = Instant.ofEpochSecond(5)
    private val expirationTime: Instant = Instant.ofEpochSecond(10)
    private val userBiddingSignals: AdSelectionSignals = AdSelectionSignals("signals")

    @Test
    fun testToString() {
        val request = FetchAndJoinCustomAudienceRequest(
            fetchUri,
            name,
            activationTime,
            expirationTime,
            userBiddingSignals)
        val result = "FetchAndJoinCustomAudienceRequest: fetchUri=$fetchUri, " +
            "name=$name, activationTime=$activationTime, " +
            "expirationTime=$expirationTime, userBiddingSignals=$userBiddingSignals"
        Truth.assertThat(request.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        val fetchAndJoinCustomAudienceRequest1 = FetchAndJoinCustomAudienceRequest(
            fetchUri,
            name,
            activationTime,
            expirationTime,
            userBiddingSignals
        )
        var fetchAndJoinCustomAudienceRequest2 = FetchAndJoinCustomAudienceRequest(
            Uri.parse("abc.com"),
            "abc",
            Instant.ofEpochSecond(5),
            Instant.ofEpochSecond(10),
            AdSelectionSignals("signals")
        )
        Truth.assertThat(fetchAndJoinCustomAudienceRequest1 == fetchAndJoinCustomAudienceRequest2)
            .isTrue()
    }
}
