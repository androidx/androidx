/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.agesignals

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class GetAgeRangeResponseTest {

    @Test
    fun construct_withValidBoundedRange_succeeds() {
        val response =
            GetAgeRangeResponse(
                lowerAgeBound = 13,
                upperAgeBound = 17,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_C,
            )
        assertThat(response.lowerAgeBound).isEqualTo(13)
        assertThat(response.upperAgeBound).isEqualTo(17)
        assertThat(response.assuranceTier).isEqualTo(GetAgeRangeResponse.ASSURANCE_TIER_C)
    }

    @Test
    fun construct_withOpenEndedRange_succeeds() {
        val response =
            GetAgeRangeResponse(
                lowerAgeBound = 18,
                upperAgeBound = null,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_A,
            )
        assertThat(response.lowerAgeBound).isEqualTo(18)
        assertThat(response.upperAgeBound).isNull()
        assertThat(response.assuranceTier).isEqualTo(GetAgeRangeResponse.ASSURANCE_TIER_A)
    }

    @Test
    fun construct_equalBounds_succeeds() {
        val response =
            GetAgeRangeResponse(
                lowerAgeBound = 18,
                upperAgeBound = 18,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_D,
            )
        assertThat(response.lowerAgeBound).isEqualTo(18)
        assertThat(response.upperAgeBound).isEqualTo(18)
        assertThat(response.assuranceTier).isEqualTo(GetAgeRangeResponse.ASSURANCE_TIER_D)
    }

    @Test
    fun construct_zeroLowerBound_succeeds() {
        val response =
            GetAgeRangeResponse(
                lowerAgeBound = 0,
                upperAgeBound = 10,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_A,
            )
        assertThat(response.lowerAgeBound).isEqualTo(0)
        assertThat(response.upperAgeBound).isEqualTo(10)
        assertThat(response.assuranceTier).isEqualTo(GetAgeRangeResponse.ASSURANCE_TIER_A)
    }

    @Test
    fun construct_negativeLowerBound_throwsIllegalArgumentException() {
        val e =
            assertThrows(IllegalArgumentException::class.java) {
                GetAgeRangeResponse(
                    lowerAgeBound = -1,
                    upperAgeBound = 10,
                    assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_A,
                )
            }
        assertThat(e).hasMessageThat().contains("lowerAgeBound must be non-negative")
    }

    @Test
    fun construct_upperBoundLessThanLowerBound_throwsIllegalArgumentException() {
        val e =
            assertThrows(IllegalArgumentException::class.java) {
                GetAgeRangeResponse(
                    lowerAgeBound = 18,
                    upperAgeBound = 17,
                    assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_B,
                )
            }
        assertThat(e).hasMessageThat().contains("greater than or equal to lowerAgeBound")
    }

    @Test
    fun construct_validAssuranceTiers_succeeds() {
        val validTiers =
            listOf(
                GetAgeRangeResponse.ASSURANCE_TIER_A,
                GetAgeRangeResponse.ASSURANCE_TIER_B,
                GetAgeRangeResponse.ASSURANCE_TIER_C,
                GetAgeRangeResponse.ASSURANCE_TIER_D,
            )
        for (tier in validTiers) {
            val response =
                GetAgeRangeResponse(lowerAgeBound = 13, upperAgeBound = 17, assuranceTier = tier)
            assertThat(response.assuranceTier).isEqualTo(tier)
        }
    }

    @Test
    fun equals_sameValues_returnsTrue() {
        val response1 =
            GetAgeRangeResponse(
                lowerAgeBound = 13,
                upperAgeBound = 17,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_C,
            )
        val response2 =
            GetAgeRangeResponse(
                lowerAgeBound = 13,
                upperAgeBound = 17,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_C,
            )
        assertThat(response1).isEqualTo(response2)
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode())
    }

    @Test
    fun equals_differentValues_returnsFalse() {
        val base =
            GetAgeRangeResponse(
                lowerAgeBound = 13,
                upperAgeBound = 17,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_C,
            )
        val diffLower =
            GetAgeRangeResponse(
                lowerAgeBound = 14,
                upperAgeBound = 17,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_C,
            )
        val diffUpper =
            GetAgeRangeResponse(
                lowerAgeBound = 13,
                upperAgeBound = 18,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_C,
            )
        val diffTier =
            GetAgeRangeResponse(
                lowerAgeBound = 13,
                upperAgeBound = 17,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_D,
            )
        val openEnded =
            GetAgeRangeResponse(
                lowerAgeBound = 13,
                upperAgeBound = null,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_C,
            )

        assertThat(base).isNotEqualTo(diffLower)
        assertThat(base).isNotEqualTo(diffUpper)
        assertThat(base).isNotEqualTo(diffTier)
        assertThat(base).isNotEqualTo(openEnded)
    }

    @Test
    fun toString_containsExpectedValues() {
        val response =
            GetAgeRangeResponse(
                lowerAgeBound = 13,
                upperAgeBound = 17,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_C,
            )
        val string = response.toString()
        assertThat(string).contains("lowerAgeBound=13")
        assertThat(string).contains("upperAgeBound=17")
        assertThat(string).contains("assuranceTier=${GetAgeRangeResponse.ASSURANCE_TIER_C}")
    }

    @Test
    fun tierConstants_matchSpecification() {
        assertThat(GetAgeRangeResponse.ASSURANCE_TIER_A).isEqualTo(1)
        assertThat(GetAgeRangeResponse.ASSURANCE_TIER_B).isEqualTo(2)
        assertThat(GetAgeRangeResponse.ASSURANCE_TIER_C).isEqualTo(3)
        assertThat(GetAgeRangeResponse.ASSURANCE_TIER_D).isEqualTo(4)
    }

    @Test
    fun tierOrdering_escalatesFromLowestToHighestAssurance() {
        assertThat(GetAgeRangeResponse.ASSURANCE_TIER_A)
            .isLessThan(GetAgeRangeResponse.ASSURANCE_TIER_B)
        assertThat(GetAgeRangeResponse.ASSURANCE_TIER_B)
            .isLessThan(GetAgeRangeResponse.ASSURANCE_TIER_C)
        assertThat(GetAgeRangeResponse.ASSURANCE_TIER_C)
            .isLessThan(GetAgeRangeResponse.ASSURANCE_TIER_D)
    }
}
