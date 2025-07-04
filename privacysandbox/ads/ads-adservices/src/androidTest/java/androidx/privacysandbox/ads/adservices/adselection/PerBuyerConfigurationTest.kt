/*
 * Copyright 2025 The Android Open Source Project
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
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.Test

@OptIn(ExperimentalFeatures.Ext14OptIn::class)
class PerBuyerConfigurationTest {
    val buyerTarget: Int = 1000
    val buyerTarget2: Int = 500
    val validBuyer: AdTechIdentifier = AdTechIdentifier("test.com")
    val validBuyer2: AdTechIdentifier = AdTechIdentifier("test2.com")

    @Test
    fun testPerBuyerConfigurationConstructor() {
        val perBuyerConfiguration = PerBuyerConfiguration(buyerTarget, validBuyer)

        assertWithMessage("Buyer").that(perBuyerConfiguration.buyer).isEqualTo(validBuyer)
        assertWithMessage("Target input size bytes")
            .that(perBuyerConfiguration.targetInputSizeBytes)
            .isEqualTo(buyerTarget)
    }

    @Test
    fun testToString() {
        val perBuyerConfiguration = PerBuyerConfiguration(buyerTarget, validBuyer)
        val expectedString =
            "PerBuyerConfiguration: targetInputSizeBytes=$buyerTarget, " + "buyer=$validBuyer"

        assertWithMessage("toString")
            .that(perBuyerConfiguration.toString())
            .isEqualTo(expectedString)
    }

    @Test
    fun testEquals() {
        val perBuyerConfiguration = PerBuyerConfiguration(buyerTarget, validBuyer)
        val perBuyerConfigurationEquals = PerBuyerConfiguration(buyerTarget, validBuyer)
        val perBuyerConfigurationNotEqual = PerBuyerConfiguration(buyerTarget2, validBuyer2)

        assertWithMessage("Equal perBuyerConfiguration")
            .that(perBuyerConfiguration)
            .isEqualTo(perBuyerConfigurationEquals)
        assertWithMessage("Unequal perBuyerConfiguration")
            .that(perBuyerConfiguration)
            .isNotEqualTo(perBuyerConfigurationNotEqual)
    }
}
