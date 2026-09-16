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

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import androidx.credentials.agesignals.exceptions.GetAgeRangeInterruptedException
import androidx.credentials.agesignals.exceptions.GetAgeRangeProviderConfigurationException
import androidx.credentials.agesignals.exceptions.GetAgeRangeUnavailableException
import androidx.credentials.agesignals.exceptions.GetAgeRangeUnknownException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AgeSignalResultHandlerTest {

    @Test
    fun responseBundle_roundTrip_boundedRange() {
        val expected = GetAgeRangeResponse(13, 15, GetAgeRangeResponse.ASSURANCE_TIER_A)

        val actual =
            AgeSignalResultHandler.retrieveGetAgeRangeResponse(
                AgeSignalResultHandler.toResponseBundle(expected)
            )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun responseBundle_roundTrip_openEndedRange() {
        val expected = GetAgeRangeResponse(18, null, GetAgeRangeResponse.ASSURANCE_TIER_D)

        val actual =
            AgeSignalResultHandler.retrieveGetAgeRangeResponse(
                AgeSignalResultHandler.toResponseBundle(expected)
            )

        assertThat(actual).isEqualTo(expected)
        assertThat(actual!!.upperAgeBound).isNull()
    }

    @Test
    fun responseBundle_roundTrip_allAssuranceTiers() {
        val tiers =
            listOf(
                GetAgeRangeResponse.ASSURANCE_TIER_A,
                GetAgeRangeResponse.ASSURANCE_TIER_B,
                GetAgeRangeResponse.ASSURANCE_TIER_C,
                GetAgeRangeResponse.ASSURANCE_TIER_D,
            )
        for (tier in tiers) {
            val expected = GetAgeRangeResponse(16, 17, tier)

            val actual =
                AgeSignalResultHandler.retrieveGetAgeRangeResponse(
                    AgeSignalResultHandler.toResponseBundle(expected)
                )

            assertThat(actual).isEqualTo(expected)
        }
    }

    /** Mirrors the interactive transport: provider activity result intent -> provider bridge. */
    @Test
    fun responseBundle_roundTripThroughIntentExtras() {
        val expected = GetAgeRangeResponse(13, 15, GetAgeRangeResponse.ASSURANCE_TIER_C)
        val intent = Intent().putExtras(AgeSignalResultHandler.toResponseBundle(expected))

        val actual = AgeSignalResultHandler.retrieveGetAgeRangeResponse(intent.extras!!)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun exceptionBundle_roundTrip_allSubclasses() {
        val exceptions =
            listOf(
                GetAgeRangeUnavailableException("unavailable"),
                GetAgeRangeInterruptedException("interrupted"),
                GetAgeRangeProviderConfigurationException("misconfigured"),
                GetAgeRangeUnknownException("unknown"),
            )
        for (expected in exceptions) {
            val actual =
                AgeSignalResultHandler.retrieveGetAgeRangeException(
                    AgeSignalResultHandler.toExceptionBundle(expected)
                )

            assertThat(actual).isNotNull()
            assertThat(actual!!.javaClass).isEqualTo(expected.javaClass)
            assertThat(actual.errorMessage.toString()).isEqualTo(expected.errorMessage.toString())
        }
    }

    @Test
    fun exceptionBundle_roundTrip_nullMessage() {
        val actual =
            AgeSignalResultHandler.retrieveGetAgeRangeException(
                AgeSignalResultHandler.toExceptionBundle(GetAgeRangeUnavailableException())
            )

        assertThat(actual).isInstanceOf(GetAgeRangeUnavailableException::class.java)
        assertThat(actual!!.errorMessage).isNull()
    }

    @Test
    fun exceptionBundle_roundTripThroughIntentExtras() {
        val intent =
            Intent()
                .putExtras(
                    AgeSignalResultHandler.toExceptionBundle(
                        GetAgeRangeInterruptedException("network dropped")
                    )
                )

        val actual = AgeSignalResultHandler.retrieveGetAgeRangeException(intent.extras!!)

        assertThat(actual).isInstanceOf(GetAgeRangeInterruptedException::class.java)
        assertThat(actual!!.errorMessage.toString()).isEqualTo("network dropped")
    }

    @Test
    fun retrieveGetAgeRangeResponse_emptyBundle_returnsNull() {
        assertThat(AgeSignalResultHandler.retrieveGetAgeRangeResponse(Bundle())).isNull()
    }

    @Test
    fun retrieveGetAgeRangeException_emptyBundle_returnsNull() {
        assertThat(AgeSignalResultHandler.retrieveGetAgeRangeException(Bundle())).isNull()
    }

    @Test
    fun retrieveGetAgeRangeException_responseOnlyBundle_returnsNull() {
        val bundle =
            AgeSignalResultHandler.toResponseBundle(
                GetAgeRangeResponse(13, 15, GetAgeRangeResponse.ASSURANCE_TIER_A)
            )

        assertThat(AgeSignalResultHandler.retrieveGetAgeRangeException(bundle)).isNull()
    }

    @Test
    fun retrieveGetAgeRangeResponse_exceptionOnlyBundle_returnsNull() {
        val bundle =
            AgeSignalResultHandler.toExceptionBundle(GetAgeRangeUnavailableException("cancelled"))

        assertThat(AgeSignalResultHandler.retrieveGetAgeRangeResponse(bundle)).isNull()
    }

    @Test
    fun bothResponseAndException_bothRetrievable() {
        val response = GetAgeRangeResponse(13, 15, GetAgeRangeResponse.ASSURANCE_TIER_A)
        val exception = GetAgeRangeUnavailableException("cancelled")
        val bundle =
            Bundle().apply {
                putAll(AgeSignalResultHandler.toResponseBundle(response))
                putAll(AgeSignalResultHandler.toExceptionBundle(exception))
            }

        assertThat(AgeSignalResultHandler.retrieveGetAgeRangeResponse(bundle)).isEqualTo(response)
        assertThat(AgeSignalResultHandler.retrieveGetAgeRangeException(bundle))
            .isInstanceOf(GetAgeRangeUnavailableException::class.java)
    }

    @Test
    fun retrieveGetAgeRangeResponse_malformedNestedBundle_returnsNull() {
        val bundle =
            Bundle().apply {
                putBundle(AgeSignalResultHandler.EXTRA_GET_AGE_RANGE_RESPONSE, Bundle())
            }

        assertThat(AgeSignalResultHandler.retrieveGetAgeRangeResponse(bundle)).isNull()
    }

    @Test
    fun retrieveGetAgeRangeResponse_invalidValuesInNestedBundle_returnsNull() {
        val malformedResponseBundle =
            Bundle().apply {
                putInt(GetAgeRangeResponse.EXTRA_AGE_LOWER_BOUND, -1)
                putInt(
                    GetAgeRangeResponse.EXTRA_AGE_ASSURANCE_TIER,
                    GetAgeRangeResponse.ASSURANCE_TIER_A,
                )
            }
        val bundle =
            Bundle().apply {
                putBundle(
                    AgeSignalResultHandler.EXTRA_GET_AGE_RANGE_RESPONSE,
                    malformedResponseBundle,
                )
            }

        assertThat(AgeSignalResultHandler.retrieveGetAgeRangeResponse(bundle)).isNull()
    }

    @Test
    fun retrieveGetAgeRangeException_malformedNestedBundle_returnsUnknownException() {
        val bundle =
            Bundle().apply {
                putBundle(AgeSignalResultHandler.EXTRA_GET_AGE_RANGE_EXCEPTION, Bundle())
            }

        assertThat(AgeSignalResultHandler.retrieveGetAgeRangeException(bundle))
            .isInstanceOf(GetAgeRangeUnknownException::class.java)
    }

    // The tests below marshal through a Parcel, which is what actually happens when a bundle
    // crosses a process boundary. The tests above only exercise Bundle's in-memory map.

    @Test
    fun responseBundle_survivesParcelMarshalling() {
        val expected = GetAgeRangeResponse(13, 15, GetAgeRangeResponse.ASSURANCE_TIER_C)

        val actual =
            AgeSignalResultHandler.retrieveGetAgeRangeResponse(
                AgeSignalResultHandler.toResponseBundle(expected).marshall()
            )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun responseBundle_openEndedRange_survivesParcelMarshalling() {
        val expected = GetAgeRangeResponse(18, null, GetAgeRangeResponse.ASSURANCE_TIER_D)

        val actual =
            AgeSignalResultHandler.retrieveGetAgeRangeResponse(
                AgeSignalResultHandler.toResponseBundle(expected).marshall()
            )

        assertThat(actual).isEqualTo(expected)
        assertThat(actual!!.upperAgeBound).isNull()
    }

    /** A tier added after this version of the library must survive the trip across a process. */
    @Test
    fun responseBundle_unrecognizedAssuranceTier_survivesParcelMarshalling() {
        val futureTier = GetAgeRangeResponse.ASSURANCE_TIER_D + 1
        val responseBundle =
            Bundle().apply {
                putInt(GetAgeRangeResponse.EXTRA_AGE_LOWER_BOUND, 18)
                putInt(GetAgeRangeResponse.EXTRA_AGE_ASSURANCE_TIER, futureTier)
            }
        val bundle =
            Bundle().apply {
                putBundle(AgeSignalResultHandler.EXTRA_GET_AGE_RANGE_RESPONSE, responseBundle)
            }

        val actual = AgeSignalResultHandler.retrieveGetAgeRangeResponse(bundle.marshall())

        assertThat(actual).isNotNull()
        assertThat(actual!!.assuranceTier).isEqualTo(futureTier)
    }

    @Test
    fun exceptionBundle_survivesParcelMarshalling() {
        val actual =
            AgeSignalResultHandler.retrieveGetAgeRangeException(
                AgeSignalResultHandler.toExceptionBundle(
                        GetAgeRangeInterruptedException("network dropped")
                    )
                    .marshall()
            )

        assertThat(actual).isInstanceOf(GetAgeRangeInterruptedException::class.java)
        assertThat(actual!!.errorMessage.toString()).isEqualTo("network dropped")
    }

    /**
     * A non-String [CharSequence] is flattened by [Parcel], so only the text is guaranteed to
     * survive a cross-process hand-off, not the concrete type.
     */
    @Test
    fun exceptionBundle_nonStringCharSequenceMessage_survivesParcelMarshallingAsText() {
        val message: CharSequence = StringBuilder("built message")

        val actual =
            AgeSignalResultHandler.retrieveGetAgeRangeException(
                AgeSignalResultHandler.toExceptionBundle(GetAgeRangeUnknownException(message))
                    .marshall()
            )

        assertThat(actual!!.errorMessage.toString()).isEqualTo("built message")
        // Proves the value genuinely crossed a Parcel rather than being handed back by reference.
        assertThat(actual.errorMessage).isNotSameInstanceAs(message)
    }

    /** Writes the bundle to a [Parcel] and reads it back, mimicking a cross-process hand-off. */
    private fun Bundle.marshall(): Bundle {
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(this)
            parcel.setDataPosition(0)
            return parcel.readBundle(AgeSignalResultHandler::class.java.classLoader)!!
        } finally {
            parcel.recycle()
        }
    }
}
