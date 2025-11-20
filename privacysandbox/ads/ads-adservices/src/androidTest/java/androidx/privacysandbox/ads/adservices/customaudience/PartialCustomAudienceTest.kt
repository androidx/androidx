/*
 * Copyright 2024 The Android Open Source Project
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

import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import java.time.Instant
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFeatures.Ext14OptIn::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class PartialCustomAudienceTest {
    private val name: String = "abc"
    private val activationTime: Instant = Instant.ofEpochSecond(5)
    private val expirationTime: Instant = Instant.ofEpochSecond(10)
    private val userBiddingSignals: AdSelectionSignals = AdSelectionSignals("signals")

    @Test
    fun testToString() {
        val partialCustomAudience =
            PartialCustomAudience(name, activationTime, expirationTime, userBiddingSignals)
        val result =
            "PartialCustomAudience: name=$name, " +
                "activationTime=$activationTime, expirationTime=$expirationTime, " +
                "userBiddingSignals=$userBiddingSignals"
        Truth.assertThat(partialCustomAudience.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        val partialCustomAudience1 =
            PartialCustomAudience(name, activationTime, expirationTime, userBiddingSignals)
        val partialCustomAudience2 =
            PartialCustomAudience(name, activationTime, expirationTime, userBiddingSignals)
        Truth.assertThat(partialCustomAudience1 == partialCustomAudience2).isTrue()
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 14)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 14)
    @Test
    fun testConvertToAdServices() {
        /* API is not available */
        Assume.assumeTrue(
            "minSdkVersion = API 31 ext 14",
            AdServicesInfo.adServicesVersion() >= 14 || AdServicesInfo.extServicesVersionS() >= 14,
        )

        val partialCustomAudience =
            PartialCustomAudience(name, activationTime, expirationTime, userBiddingSignals)
        val result = partialCustomAudience.convertToAdServices()
        val expected =
            android.adservices.customaudience.PartialCustomAudience.Builder(name)
                .setActivationTime(activationTime)
                .setExpirationTime(expirationTime)
                .setUserBiddingSignals(userBiddingSignals.convertToAdServices())
                .build()
        Truth.assertThat(result).isEqualTo(expected)
    }
}
