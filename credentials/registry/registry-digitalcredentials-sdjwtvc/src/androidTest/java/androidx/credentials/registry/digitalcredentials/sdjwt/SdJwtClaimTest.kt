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

package androidx.credentials.registry.digitalcredentials.sdjwt

import androidx.credentials.registry.provider.digitalcredentials.VerificationFieldDisplayProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SdJwtClaimTest {
    companion object {
        val FIELD_DISPLAY_DATA = VerificationFieldDisplayProperties("displayName1")
    }

    @Test
    fun construction_success() {
        val claim =
            SdJwtClaim(
                path = listOf("age_equal_or_over", "18"),
                value = true,
                fieldDisplayPropertySet = setOf(FIELD_DISPLAY_DATA),
            )

        assertThat(claim.path).containsExactly("age_equal_or_over", "18").inOrder()
        assertThat((claim.value) as Boolean).isTrue()
        assertThat(claim.fieldDisplayPropertySet).containsExactly(FIELD_DISPLAY_DATA)
        assertThat(claim.isSelectivelyDisclosable).isTrue()
    }

    @Test
    fun construction_notSelectiveDisclosable_success() {
        val claim =
            SdJwtClaim(
                path = listOf("given_name"),
                value = "Elisa",
                fieldDisplayPropertySet = setOf(FIELD_DISPLAY_DATA),
                isSelectivelyDisclosable = false,
            )

        assertThat(claim.path).containsExactly("given_name").inOrder()
        assertThat((claim.value) as String).isEqualTo("Elisa")
        assertThat(claim.fieldDisplayPropertySet).containsExactly(FIELD_DISPLAY_DATA)
        assertThat(claim.isSelectivelyDisclosable).isFalse()
    }
}
