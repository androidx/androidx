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

package androidx.credentials.agesignals.exceptions

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class GetAgeRangeExceptionBundleTest {

    @Test
    fun asBundleAndFromBundle_preservesTypeAndMessage() {
        val original = GetAgeRangeInterruptedException("network dropped")

        val restored = GetAgeRangeException.fromBundle(GetAgeRangeException.asBundle(original))

        assertThat(restored).isInstanceOf(GetAgeRangeInterruptedException::class.java)
        assertThat(restored.type).isEqualTo(original.type)
        assertThat(restored.errorMessage.toString()).isEqualTo("network dropped")
    }

    @Test
    fun asBundleAndFromBundle_nonStringCharSequenceMessage_preservesText() {
        val message: CharSequence = StringBuilder("built message")

        val restored =
            GetAgeRangeException.fromBundle(
                GetAgeRangeException.asBundle(GetAgeRangeUnknownException(message))
            )

        assertThat(restored.errorMessage.toString()).isEqualTo("built message")
    }

    @Test
    fun fromBundle_missingType_returnsUnknownException() {
        val restored = GetAgeRangeException.fromBundle(Bundle())

        assertThat(restored).isInstanceOf(GetAgeRangeUnknownException::class.java)
        assertThat(restored.errorMessage).isNull()
    }

    @Test
    fun fromBundle_unrecognizedType_returnsUnknownException() {
        val bundle = GetAgeRangeException.asBundle(GetAgeRangeUnknownException("boom"))
        bundle.putString(
            GetAgeRangeException.EXTRA_GET_AGE_RANGE_EXCEPTION_TYPE,
            "com.example.SOME_FUTURE_EXCEPTION_TYPE",
        )

        val restored = GetAgeRangeException.fromBundle(bundle)

        assertThat(restored).isInstanceOf(GetAgeRangeUnknownException::class.java)
        assertThat(restored.errorMessage.toString()).isEqualTo("boom")
    }

    @Test
    fun asBundle_nullMessage_omitsMessageKey() {
        val bundle = GetAgeRangeException.asBundle(GetAgeRangeUnavailableException(null))

        assertThat(bundle.containsKey(GetAgeRangeException.EXTRA_GET_AGE_RANGE_EXCEPTION_MESSAGE))
            .isFalse()

        val restored = GetAgeRangeException.fromBundle(bundle)

        assertThat(restored).isInstanceOf(GetAgeRangeUnavailableException::class.java)
        assertThat(restored.errorMessage).isNull()
    }

    @Test
    fun asBundleAndFromBundle_allSubclasses_preservesType() {
        val exceptions =
            listOf(
                GetAgeRangeUnavailableException("unavailable"),
                GetAgeRangeInterruptedException("interrupted"),
                GetAgeRangeProviderConfigurationException("misconfigured"),
                GetAgeRangeUnknownException("unknown"),
            )
        for (original in exceptions) {
            val restored = GetAgeRangeException.fromBundle(GetAgeRangeException.asBundle(original))

            assertThat(restored.javaClass).isEqualTo(original.javaClass)
            assertThat(restored.type).isEqualTo(original.type)
            assertThat(restored.errorMessage.toString()).isEqualTo(original.errorMessage.toString())
        }
    }
}
