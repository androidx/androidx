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

import android.net.Uri
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(
    ExperimentalFeatures.Ext10OptIn::class,
    ExperimentalFeatures.Ext12OptIn::class,
    ExperimentalFeatures.Ext14OptIn::class,
)
@SmallTest
@RunWith(AndroidJUnit4::class)
class GetAdSelectionDataRequestTest {
    private val seller: AdTechIdentifier = AdTechIdentifier("1234")
    private val coordinatorOriginUri: Uri = Uri.parse("www.coordinator.com")
    val buyerTarget: Int = 1000
    val buyerTarget2: Int = 500
    val validBuyer: AdTechIdentifier = AdTechIdentifier("test.com")
    val validBuyer2: AdTechIdentifier = AdTechIdentifier("test2.com")
    val perBuyerConfiguration: PerBuyerConfiguration =
        PerBuyerConfiguration(buyerTarget, validBuyer)
    val perBuyerConfiguration2: PerBuyerConfiguration =
        PerBuyerConfiguration(buyerTarget2, validBuyer2)
    val sellerTargetSize: Int = 2000
    val perBuyerConfigurations = setOf(perBuyerConfiguration, perBuyerConfiguration2)
    val sellerConfiguration = SellerConfiguration(sellerTargetSize, perBuyerConfigurations)

    @Test
    fun testToString() {
        val result =
            "GetAdSelectionDataRequest: seller=$seller, coordinatorOriginUri=null, sellerConfiguration=null"
        val request = GetAdSelectionDataRequest(seller)

        Truth.assertThat(request.toString()).isEqualTo(result)
    }

    @Test
    fun testToStringWithCoordinatorUri() {
        val result =
            "GetAdSelectionDataRequest: seller=$seller, " +
                "coordinatorOriginUri=$coordinatorOriginUri, " +
                "sellerConfiguration=null"
        val request = GetAdSelectionDataRequest(seller, coordinatorOriginUri)

        Truth.assertThat(request.toString()).isEqualTo(result)
    }

    @Test
    fun testToStringWithSellerConfiguration() {
        val result =
            "GetAdSelectionDataRequest: seller=$seller, " +
                "coordinatorOriginUri=$coordinatorOriginUri, " +
                "sellerConfiguration=$sellerConfiguration"
        val request = GetAdSelectionDataRequest(seller, coordinatorOriginUri, sellerConfiguration)

        Truth.assertThat(request.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        val getAdSelectionDataRequest = GetAdSelectionDataRequest(seller)
        val getAdSelectionDataRequest2 = GetAdSelectionDataRequest(AdTechIdentifier("1234"))

        Truth.assertThat(getAdSelectionDataRequest == getAdSelectionDataRequest2).isTrue()
    }

    @Test
    fun testEqualsWithCoordinatorUri() {
        val getAdSelectionDataRequest = GetAdSelectionDataRequest(seller, coordinatorOriginUri)
        val getAdSelectionDataRequest2 =
            GetAdSelectionDataRequest(AdTechIdentifier("1234"), Uri.parse("www.coordinator.com"))

        Truth.assertThat(getAdSelectionDataRequest == getAdSelectionDataRequest2).isTrue()
    }

    @Test
    fun testEqualsWithSellerConfiguration() {
        val getAdSelectionDataRequest =
            GetAdSelectionDataRequest(seller, coordinatorOriginUri, sellerConfiguration)
        val reportEventRequestUnequal =
            GetAdSelectionDataRequest(AdTechIdentifier("1234"), Uri.parse("www.coordinator.com"))
        val reportEventRequestEqual =
            GetAdSelectionDataRequest(
                AdTechIdentifier("1234"),
                Uri.parse("www.coordinator.com"),
                sellerConfiguration,
            )

        Truth.assertThat(getAdSelectionDataRequest == reportEventRequestUnequal).isFalse()
        Truth.assertThat(getAdSelectionDataRequest == reportEventRequestEqual).isTrue()
    }
}
