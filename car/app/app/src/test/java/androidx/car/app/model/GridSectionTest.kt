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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GridSectionTest {
    private val testItemList =
        listOf(
            GridItem.Builder().setImage(CarIcon.COMPOSE_MESSAGE).build(),
            GridItem.Builder().setText("Test").setImage(CarIcon.ERROR).build()
        )
    private val testHeader = CarText.Builder("Test header text").build()

    @Test
    fun getItemSize() {
        val section = GridSection.Builder().setItemSize(GridSection.ITEM_SIZE_LARGE).build()

        assertThat(section.itemSize).isEqualTo(GridSection.ITEM_SIZE_LARGE)
    }

    @Test
    fun getItemImageShape() {
        val section =
            GridSection.Builder().setItemImageShape(GridSection.ITEM_IMAGE_SHAPE_CIRCLE).build()

        assertThat(section.itemImageShape).isEqualTo(GridSection.ITEM_IMAGE_SHAPE_CIRCLE)
    }

    @Test
    fun equals_returnsFalse_whenPassedNull() {
        val section = GridSection.Builder().build()

        assertThat(section.equals(null)).isFalse()
    }

    @Test
    fun equals_isReflexive() {
        val section =
            GridSection.Builder()
                .setItemSize(GridSection.ITEM_SIZE_LARGE)
                .setItemImageShape(GridSection.ITEM_IMAGE_SHAPE_CIRCLE)
                .build()

        @Suppress("ReplaceCallWithBinaryOperator") assertThat(section.equals(section)).isTrue()
    }

    @Test
    fun equals_returnsTrue_whenSectionsHaveTheSameContent() {
        val section1 =
            GridSection.Builder()
                .setItemSize(GridSection.ITEM_SIZE_LARGE)
                .setItemImageShape(GridSection.ITEM_IMAGE_SHAPE_CIRCLE)
                .setItems(testItemList)
                .setTitle(testHeader)
                .setNoItemsMessage("Test no items message")
                .build()
        val section2 =
            GridSection.Builder()
                .setItemSize(GridSection.ITEM_SIZE_LARGE)
                .setItemImageShape(GridSection.ITEM_IMAGE_SHAPE_CIRCLE)
                .setItems(testItemList)
                .setTitle(testHeader)
                .setNoItemsMessage("Test no items message")
                .build()

        // Is symmetric
        assertThat(section1).isEqualTo(section2)
        assertThat(section2).isEqualTo(section1)
    }

    @Test
    fun equals_returnsFalse_whenNotEqual() {
        val sections =
            listOf(
                GridSection.Builder().setItemSize(GridSection.ITEM_SIZE_LARGE).build(),
                GridSection.Builder()
                    .setItemImageShape(GridSection.ITEM_IMAGE_SHAPE_CIRCLE)
                    .build(),
                GridSection.Builder().setItems(testItemList).build(),
                GridSection.Builder().setTitle(testHeader).build(),
                GridSection.Builder().setNoItemsMessage("Example").build()
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
