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

package androidx.credentials.registry.digitalcredentials.mdoc

import androidx.credentials.registry.provider.digitalcredentials.InlineIssuanceEntry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MdocInlineIssuanceEntryTest {
    @Test
    fun constructor_validInputs_succeeds() {
        val issuanceDisplay =
            InlineIssuanceEntry.InlineIssuanceDisplayProperties(subtitle = "subtitle")
        val supportedMdoc = MdocInlineIssuanceEntry.SupportedMdoc("docType")
        val entry =
            MdocInlineIssuanceEntry(
                id = "id",
                display = issuanceDisplay,
                supportedMdocs = setOf(supportedMdoc),
            )

        assertThat(entry.id).isEqualTo("id")
        assertThat(entry.display).isEqualTo(issuanceDisplay)
        assertThat(entry.supportedMdocs).containsExactly(supportedMdoc)
    }

    @Test
    fun constructor_longId_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            MdocInlineIssuanceEntry(
                id = "a".repeat(65),
                display =
                    InlineIssuanceEntry.InlineIssuanceDisplayProperties(subtitle = "subtitle"),
                supportedMdocs = setOf(MdocInlineIssuanceEntry.SupportedMdoc("docType")),
            )
        }
    }

    @Test
    fun constructor_emptySupportedMdocs_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            MdocInlineIssuanceEntry(
                id = "id",
                display =
                    InlineIssuanceEntry.InlineIssuanceDisplayProperties(subtitle = "subtitle"),
                supportedMdocs = emptySet(),
            )
        }
    }

    @Test
    fun supportedMdoc_emptyDocType_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            MdocInlineIssuanceEntry.SupportedMdoc("")
        }
    }

    @Test
    fun equals_and_hashCode_areEqual() {
        val issuanceDisplay1 =
            InlineIssuanceEntry.InlineIssuanceDisplayProperties(subtitle = "subtitle")
        val supportedMdoc1 = MdocInlineIssuanceEntry.SupportedMdoc("docType")
        val entry1 =
            MdocInlineIssuanceEntry(
                id = "id",
                display = issuanceDisplay1,
                supportedMdocs = setOf(supportedMdoc1),
            )

        val issuanceDisplay2 =
            InlineIssuanceEntry.InlineIssuanceDisplayProperties(subtitle = "subtitle")
        val supportedMdoc2 = MdocInlineIssuanceEntry.SupportedMdoc("docType")
        val entry2 =
            MdocInlineIssuanceEntry(
                id = "id",
                display = issuanceDisplay2,
                supportedMdocs = setOf(supportedMdoc2),
            )

        assertThat(entry1).isEqualTo(entry2)
        assertThat(entry1.hashCode()).isEqualTo(entry2.hashCode())
    }

    @Test
    fun equals_and_hashCode_areNotEqual() {
        val issuanceDisplay1 =
            InlineIssuanceEntry.InlineIssuanceDisplayProperties(subtitle = "subtitle")
        val supportedMdoc1 = MdocInlineIssuanceEntry.SupportedMdoc("docType")
        val entry1 =
            MdocInlineIssuanceEntry(
                id = "id",
                display = issuanceDisplay1,
                supportedMdocs = setOf(supportedMdoc1),
            )

        val issuanceDisplay2 =
            InlineIssuanceEntry.InlineIssuanceDisplayProperties(subtitle = "subtitle2")
        val supportedMdoc2 = MdocInlineIssuanceEntry.SupportedMdoc("docType2")
        val entry2 =
            MdocInlineIssuanceEntry(
                id = "id2",
                display = issuanceDisplay2,
                supportedMdocs = setOf(supportedMdoc2),
            )

        assertThat(entry1).isNotEqualTo(entry2)
        assertThat(entry1.hashCode()).isNotEqualTo(entry2.hashCode())
    }
}
