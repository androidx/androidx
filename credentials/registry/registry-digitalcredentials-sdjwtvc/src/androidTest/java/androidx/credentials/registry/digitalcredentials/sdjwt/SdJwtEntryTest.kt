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

import android.graphics.Bitmap
import androidx.credentials.registry.provider.digitalcredentials.VerificationEntryDisplayProperties
import androidx.credentials.registry.provider.digitalcredentials.VerificationFieldDisplayProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SdJwtEntryTest {
    companion object {
        val ENTRY_DISPLAY_DATA =
            VerificationEntryDisplayProperties(
                title = "test-title",
                subtitle = "test-subtitle",
                icon = Bitmap.createBitmap(4, 4, Bitmap.Config.ALPHA_8),
            )
    }

    @Test
    fun construction_longId_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            SdJwtEntry(
                verifiableCredentialType = "https://example.com/vct",
                claims = listOf(),
                entryDisplayPropertySet = setOf(ENTRY_DISPLAY_DATA),
                id = "a".repeat(65),
            )
        }
    }

    @Test
    fun construction_success() {
        val claim1 =
            SdJwtClaim(
                path = listOf("age_equal_or_over", "18"),
                value = "claimVal1",
                fieldDisplayPropertySet = setOf(VerificationFieldDisplayProperties("displayName1")),
            )
        val claim2 =
            SdJwtClaim(
                path = listOf("given_name"),
                value = null,
                fieldDisplayPropertySet = setOf(VerificationFieldDisplayProperties("displayName1")),
            )

        val entry =
            SdJwtEntry(
                verifiableCredentialType = "https://example.com/vct",
                claims = listOf(claim1, claim2),
                entryDisplayPropertySet = setOf(ENTRY_DISPLAY_DATA),
                id = "id",
            )

        assertThat(entry.verifiableCredentialType).isEqualTo("https://example.com/vct")
        assertThat(entry.claims).containsExactly(claim1, claim2).inOrder()
        assertThat(entry.entryDisplayPropertySet).containsExactly(ENTRY_DISPLAY_DATA)
        assertThat(entry.id).isEqualTo("id")
    }
}
