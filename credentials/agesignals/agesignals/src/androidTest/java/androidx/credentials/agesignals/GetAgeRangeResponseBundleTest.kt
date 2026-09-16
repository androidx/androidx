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

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class GetAgeRangeResponseBundleTest {

    @Test
    fun asBundle_openEndedRange_omitsUpperBoundKey() {
        val bundle =
            GetAgeRangeResponse.asBundle(
                GetAgeRangeResponse(18, null, GetAgeRangeResponse.ASSURANCE_TIER_A)
            )

        assertThat(bundle.containsKey(GetAgeRangeResponse.EXTRA_AGE_UPPER_BOUND)).isFalse()
        assertThat(GetAgeRangeResponse.fromBundle(bundle)!!.upperAgeBound).isNull()
    }

    @Test
    fun fromBundle_emptyBundle_returnsNull() {
        assertThat(GetAgeRangeResponse.fromBundle(Bundle())).isNull()
    }

    @Test
    fun fromBundle_missingLowerBound_returnsNull() {
        val bundle =
            Bundle().apply {
                putInt(GetAgeRangeResponse.EXTRA_AGE_UPPER_BOUND, 15)
                putInt(
                    GetAgeRangeResponse.EXTRA_AGE_ASSURANCE_TIER,
                    GetAgeRangeResponse.ASSURANCE_TIER_A,
                )
            }

        assertThat(GetAgeRangeResponse.fromBundle(bundle)).isNull()
    }

    @Test
    fun fromBundle_missingAssuranceTier_returnsNull() {
        val bundle =
            Bundle().apply {
                putInt(GetAgeRangeResponse.EXTRA_AGE_LOWER_BOUND, 13)
                putInt(GetAgeRangeResponse.EXTRA_AGE_UPPER_BOUND, 15)
            }

        assertThat(GetAgeRangeResponse.fromBundle(bundle)).isNull()
    }

    /** A tier introduced after this version of the library must not invalidate the response. */
    @Test
    fun fromBundle_unrecognizedAssuranceTier_isPreserved() {
        for (tier in listOf(GetAgeRangeResponse.ASSURANCE_TIER_D + 1, 99)) {
            val bundle =
                Bundle().apply {
                    putInt(GetAgeRangeResponse.EXTRA_AGE_LOWER_BOUND, 13)
                    putInt(GetAgeRangeResponse.EXTRA_AGE_UPPER_BOUND, 15)
                    putInt(GetAgeRangeResponse.EXTRA_AGE_ASSURANCE_TIER, tier)
                }

            val response = GetAgeRangeResponse.fromBundle(bundle)

            assertThat(response).isNotNull()
            assertThat(response!!.assuranceTier).isEqualTo(tier)
        }
    }

    @Test
    fun fromBundle_negativeLowerBound_returnsNull() {
        val bundle =
            Bundle().apply {
                putInt(GetAgeRangeResponse.EXTRA_AGE_LOWER_BOUND, -1)
                putInt(
                    GetAgeRangeResponse.EXTRA_AGE_ASSURANCE_TIER,
                    GetAgeRangeResponse.ASSURANCE_TIER_A,
                )
            }

        assertThat(GetAgeRangeResponse.fromBundle(bundle)).isNull()
    }

    @Test
    fun fromBundle_upperBoundLessThanLowerBound_returnsNull() {
        val bundle =
            Bundle().apply {
                putInt(GetAgeRangeResponse.EXTRA_AGE_LOWER_BOUND, 20)
                putInt(GetAgeRangeResponse.EXTRA_AGE_UPPER_BOUND, 15)
                putInt(
                    GetAgeRangeResponse.EXTRA_AGE_ASSURANCE_TIER,
                    GetAgeRangeResponse.ASSURANCE_TIER_A,
                )
            }

        assertThat(GetAgeRangeResponse.fromBundle(bundle)).isNull()
    }

    @Test
    fun asBundleAndFromBundle_boundedRange_preservesAllValues() {
        val original = GetAgeRangeResponse(13, 15, GetAgeRangeResponse.ASSURANCE_TIER_B)
        val bundle = GetAgeRangeResponse.asBundle(original)

        val restored = GetAgeRangeResponse.fromBundle(bundle)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun fromBundle_zeroLowerBound_succeeds() {
        val bundle =
            Bundle().apply {
                putInt(GetAgeRangeResponse.EXTRA_AGE_LOWER_BOUND, 0)
                putInt(GetAgeRangeResponse.EXTRA_AGE_UPPER_BOUND, 12)
                putInt(
                    GetAgeRangeResponse.EXTRA_AGE_ASSURANCE_TIER,
                    GetAgeRangeResponse.ASSURANCE_TIER_A,
                )
            }

        val response = GetAgeRangeResponse.fromBundle(bundle)

        assertThat(response).isNotNull()
        assertThat(response!!.lowerAgeBound).isEqualTo(0)
        assertThat(response.upperAgeBound).isEqualTo(12)
        assertThat(response.assuranceTier).isEqualTo(GetAgeRangeResponse.ASSURANCE_TIER_A)
    }

    @Test
    fun fromBundle_equalBounds_succeeds() {
        val bundle =
            Bundle().apply {
                putInt(GetAgeRangeResponse.EXTRA_AGE_LOWER_BOUND, 18)
                putInt(GetAgeRangeResponse.EXTRA_AGE_UPPER_BOUND, 18)
                putInt(
                    GetAgeRangeResponse.EXTRA_AGE_ASSURANCE_TIER,
                    GetAgeRangeResponse.ASSURANCE_TIER_D,
                )
            }

        val response = GetAgeRangeResponse.fromBundle(bundle)

        assertThat(response).isNotNull()
        assertThat(response!!.lowerAgeBound).isEqualTo(18)
        assertThat(response.upperAgeBound).isEqualTo(18)
        assertThat(response.assuranceTier).isEqualTo(GetAgeRangeResponse.ASSURANCE_TIER_D)
    }
}
