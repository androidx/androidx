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

package androidx.compose.material3.a2ui.catalog

import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiAccessibilityTest {

    @Test
    fun toContentDescription_bothLabelAndDescription_returnsCombined() {
        val attributes =
            A2uiBasicCatalogV1.AccessibilityAttributes(label = "Label", description = "Description")
        assertThat(attributes.toContentDescription()).isEqualTo("Label - Description")
    }

    @Test
    fun toContentDescription_labelOnly_returnsLabel() {
        val attributes = A2uiBasicCatalogV1.AccessibilityAttributes(label = "Label")
        assertThat(attributes.toContentDescription()).isEqualTo("Label")
    }

    @Test
    fun toContentDescription_descriptionOnly_returnsDescription() {
        val attributes = A2uiBasicCatalogV1.AccessibilityAttributes(description = "Description")
        assertThat(attributes.toContentDescription()).isEqualTo("Description")
    }

    @Test
    fun toContentDescription_neitherPresent_returnsNull() {
        val attributes = A2uiBasicCatalogV1.AccessibilityAttributes()
        assertThat(attributes.toContentDescription()).isNull()
    }

    @Test
    fun toContentDescription_clickable_bothLabelAndDescription_returnsLabelOnly() {
        val attributes =
            A2uiBasicCatalogV1.AccessibilityAttributes(label = "Label", description = "Description")
        assertThat(attributes.toContentDescription(isClickable = true)).isEqualTo("Label")
    }

    @Test
    fun toContentDescription_clickable_descriptionOnly_returnsNull() {
        val attributes = A2uiBasicCatalogV1.AccessibilityAttributes(description = "Description")
        assertThat(attributes.toContentDescription(isClickable = true)).isNull()
    }

    @Test
    fun toContentDescription_blankLabel_returnsNull() {
        val attributes = A2uiBasicCatalogV1.AccessibilityAttributes(label = "   ")
        assertThat(attributes.toContentDescription()).isNull()
    }

    @Test
    fun toContentDescription_blankDescription_returnsNull() {
        val attributes = A2uiBasicCatalogV1.AccessibilityAttributes(description = "   ")
        assertThat(attributes.toContentDescription()).isNull()
    }

    @Test
    fun toContentDescription_validLabelWithBlankDescription_returnsLabelOnly() {
        val attributes =
            A2uiBasicCatalogV1.AccessibilityAttributes(label = "Label", description = "   ")
        assertThat(attributes.toContentDescription()).isEqualTo("Label")
    }

    @Test
    fun toContentDescription_blankLabelWithValidDescription_returnsDescriptionOnly() {
        val attributes =
            A2uiBasicCatalogV1.AccessibilityAttributes(label = "", description = "Description")
        assertThat(attributes.toContentDescription()).isEqualTo("Description")
    }

    @Test
    fun toContentDescription_bothBlank_returnsNull() {
        val attributes = A2uiBasicCatalogV1.AccessibilityAttributes(label = "  ", description = "")
        assertThat(attributes.toContentDescription()).isNull()
    }

    @Test
    fun toContentDescription_clickable_blankLabel_returnsNull() {
        val attributes =
            A2uiBasicCatalogV1.AccessibilityAttributes(label = "  ", description = "Action")
        assertThat(attributes.toContentDescription(isClickable = true)).isNull()
    }

    @Test
    fun buildContentDescription_bothNullOrBlank_returnsNull() {
        assertThat(buildContentDescription(label = null, description = null)).isNull()
        assertThat(buildContentDescription(label = "   ", description = "")).isNull()
    }

    @Test
    fun buildContentDescription_labelOnly_returnsLabel() {
        assertThat(buildContentDescription(label = "Volume", description = null))
            .isEqualTo("Volume")
        assertThat(buildContentDescription(label = "Volume", description = "   "))
            .isEqualTo("Volume")
    }

    @Test
    fun buildContentDescription_descriptionOnly_returnsDescription() {
        assertThat(buildContentDescription(label = null, description = "Adjusts sound"))
            .isEqualTo("Adjusts sound")
        assertThat(buildContentDescription(label = "  ", description = "Adjusts sound"))
            .isEqualTo("Adjusts sound")
    }

    @Test
    fun buildContentDescription_bothPresent_combinesWithDash() {
        assertThat(buildContentDescription(label = "Volume", description = "Adjusts sound"))
            .isEqualTo("Volume - Adjusts sound")
    }
}
