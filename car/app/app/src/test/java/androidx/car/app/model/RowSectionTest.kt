/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.car.app.model

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RowSectionTest {
    private val testHeader = CarText.Builder("Test header text").build()
    private val testNoItemsMessage = CarText.create("No items")
    private val testItemList =
        listOf(
            Row.Builder().setTitle("Test title").build(),
            Row.Builder().setTitle("Test another title").build()
        )

    @Test
    fun getInitialSelectedIndex() {
        val section = RowSection.Builder().setItems(testItemList).setAsSelectionGroup(1).build()

        assertThat(section.initialSelectedIndex).isEqualTo(1)
    }

    @Test
    fun isSelectionGroup_returnsTrue_whenInitialSelectedIndexIsSet() {
        val section = RowSection.Builder().setItems(testItemList).setAsSelectionGroup(1).build()

        assertThat(section.isSelectionGroup).isTrue()
    }

    @Test
    fun isSelectionGroup_returnsFalse_whenInitialSelectedIndexIsNotSet() {
        val section = RowSection.Builder().setItems(testItemList).build()

        assertThat(section.isSelectionGroup).isFalse()
    }

    @Test
    fun build_throwsException_whenInitialSelectedIndexIsOutOfBounds() {
        try {
            RowSection.Builder()
                .setItems(testItemList)
                .setAsSelectionGroup(testItemList.size)
                .build()
            assertWithMessage("Expected the build to fail with an IllegalArgumentException").fail()
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("cannot be larger than the size of the list")
        }
    }

    @Test
    fun build_throwsException_whenSetAsSelectionGroupAndRowContainsToggle() {
        try {
            RowSection.Builder()
                .addItem(
                    Row.Builder().setTitle("Test").setToggle(Toggle.Builder {}.build()).build()
                )
                .setAsSelectionGroup(0)
                .build()
            assertWithMessage("Expected the build to fail with an IllegalArgumentException").fail()
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("A row that has a toggle")
        }
    }

    @Test
    fun build_throwsException_whenRowIsNotFullListConstrained() {
        try {
            // Full list row cannot have more than 2 text lines
            RowSection.Builder()
                .addItem(
                    Row.Builder()
                        .setTitle("Test")
                        .addText("line 1")
                        .addText("line 2")
                        .addText("line 3")
                        .build()
                )
                .build()
            assertWithMessage("Expected the build to fail with an IllegalArgumentException").fail()
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("number of lines of texts")
        }
    }

    @Test
    fun equals_returnsFalse_whenPassedNull() {
        val section = RowSection.Builder().build()

        assertThat(section.equals(null)).isFalse()
    }

    @Test
    fun equals_isReflexive() {
        val section =
            RowSection.Builder()
                .setItems(testItemList)
                .setAsSelectionGroup(1)
                .setTitle(testHeader)
                .setNoItemsMessage(testNoItemsMessage)
                .build()

        @Suppress("ReplaceCallWithBinaryOperator") assertThat(section.equals(section)).isTrue()
    }

    @Test
    fun equals_returnsTrue_whenSectionsHaveTheSameContent() {
        val section1 =
            RowSection.Builder()
                .setItems(testItemList)
                .setAsSelectionGroup(1)
                .setTitle(testHeader)
                .setNoItemsMessage(testNoItemsMessage)
                .build()
        val section2 =
            RowSection.Builder()
                .setItems(testItemList)
                .setAsSelectionGroup(1)
                .setTitle(testHeader)
                .setNoItemsMessage(testNoItemsMessage)
                .build()

        // Is symmetric
        assertThat(section1).isEqualTo(section2)
        assertThat(section2).isEqualTo(section1)
    }

    @Test
    fun equals_returnsFalse_whenNotEqual() {
        val sections =
            listOf(
                RowSection.Builder().setItems(testItemList).build(),
                RowSection.Builder().setItems(testItemList).setAsSelectionGroup(1).build(),
                RowSection.Builder().setTitle(testHeader).build(),
                RowSection.Builder().setNoItemsMessage(testNoItemsMessage)
            )

        // Test all different sections against each other
        for (i in sections.indices) {
            for (j in sections.indices) {
                if (i == j) {
                    continue
                }
                assertThat(sections[i]).isNotEqualTo(sections[j])
            }
        }
    }
}
