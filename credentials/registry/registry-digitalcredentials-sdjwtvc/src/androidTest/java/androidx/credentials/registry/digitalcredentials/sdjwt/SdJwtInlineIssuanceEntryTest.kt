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

import androidx.credentials.registry.provider.digitalcredentials.InlineIssuanceEntry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SdJwtInlineIssuanceEntryTest {
    @Test
    fun constructor_validInputs_succeeds() {
        val issuanceDisplay =
            InlineIssuanceEntry.InlineIssuanceDisplayProperties(subtitle = "subtitle")
        val supportedSdJwt = SdJwtInlineIssuanceEntry.SupportedSdJwt("vct")
        val entry =
            SdJwtInlineIssuanceEntry(
                id = "id",
                display = issuanceDisplay,
                supportedSdJwts = setOf(supportedSdJwt),
            )

        assertThat(entry.id).isEqualTo("id")
        assertThat(entry.display).isEqualTo(issuanceDisplay)
        assertThat(entry.supportedSdJwts).containsExactly(supportedSdJwt)
    }

    @Test
    fun constructor_longId_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            SdJwtInlineIssuanceEntry(
                id = "a".repeat(65),
                display =
                    InlineIssuanceEntry.InlineIssuanceDisplayProperties(subtitle = "subtitle"),
                supportedSdJwts = setOf(SdJwtInlineIssuanceEntry.SupportedSdJwt("vct")),
            )
        }
    }

    @Test
    fun constructor_emptySupportedSdJwts_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            SdJwtInlineIssuanceEntry(
                id = "id",
                display =
                    InlineIssuanceEntry.InlineIssuanceDisplayProperties(subtitle = "subtitle"),
                supportedSdJwts = emptySet(),
            )
        }
    }

    @Test
    fun supportedSdJwt_emptyVerifiableCredentialType_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            SdJwtInlineIssuanceEntry.SupportedSdJwt("")
        }
    }

    @Test
    fun equals_and_hashCode_areEqual() {
        val issuanceDisplay1 =
            InlineIssuanceEntry.InlineIssuanceDisplayProperties(subtitle = "subtitle")
        val supportedSdJwt1 = SdJwtInlineIssuanceEntry.SupportedSdJwt("vct")
        val entry1 =
            SdJwtInlineIssuanceEntry(
                id = "id",
                display = issuanceDisplay1,
                supportedSdJwts = setOf(supportedSdJwt1),
            )

        val issuanceDisplay2 =
            InlineIssuanceEntry.InlineIssuanceDisplayProperties(subtitle = "subtitle")
        val supportedSdJwt2 = SdJwtInlineIssuanceEntry.SupportedSdJwt("vct")
        val entry2 =
            SdJwtInlineIssuanceEntry(
                id = "id",
                display = issuanceDisplay2,
                supportedSdJwts = setOf(supportedSdJwt2),
            )

        assertThat(entry1).isEqualTo(entry2)
        assertThat(entry1.hashCode()).isEqualTo(entry2.hashCode())
    }

    @Test
    fun equals_and_hashCode_areNotEqual() {
        val issuanceDisplay1 =
            InlineIssuanceEntry.InlineIssuanceDisplayProperties(subtitle = "subtitle")
        val supportedSdJwt1 = SdJwtInlineIssuanceEntry.SupportedSdJwt("vct")
        val entry1 =
            SdJwtInlineIssuanceEntry(
                id = "id",
                display = issuanceDisplay1,
                supportedSdJwts = setOf(supportedSdJwt1),
            )

        val issuanceDisplay2 =
            InlineIssuanceEntry.InlineIssuanceDisplayProperties(subtitle = "subtitle2")
        val supportedSdJwt2 = SdJwtInlineIssuanceEntry.SupportedSdJwt("vct2")
        val entry2 =
            SdJwtInlineIssuanceEntry(
                id = "id2",
                display = issuanceDisplay2,
                supportedSdJwts = setOf(supportedSdJwt2),
            )

        assertThat(entry1).isNotEqualTo(entry2)
        assertThat(entry1.hashCode()).isNotEqualTo(entry2.hashCode())
    }
}
