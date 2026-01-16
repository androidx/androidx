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

package androidx.car.app.model

import androidx.car.app.testing.TestDelegateInvoker.requestAllItemsForTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [FilterChipSection]. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FilterChipSectionTest {

    @Test
    fun create_defaultValues() {
        val chip = FilterChip.Builder().setTitle("Title").setOnClickListener {}.build()
        val section = FilterChipSection.Builder().addItem(chip).build()

        assertThat(section.itemsDelegate.requestAllItemsForTest()).containsExactly(chip)
        assertThat(section.title).isNull()
        assertThat(section.noItemsMessage).isNull()
        assertThat(section.style).isNull()
    }

    @Test
    fun build_throws_ifNoItems() {
        try {
            FilterChipSection.Builder().build()
            assertWithMessage("Expected builder to throw an exception, but it didn't").fail()
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("must contain at least one item")
        }
    }

    @Test
    fun setStyle() {
        val style = FilterChipStyle.Builder().setBackgroundColor(CarColor.RED).build()
        val chip = FilterChip.Builder().setTitle("Title").setOnClickListener {}.build()
        val section = FilterChipSection.Builder().addItem(chip).setStyle(style).build()

        assertThat(section.style).isEqualTo(style)
    }

    @Test
    fun equals() {
        val chip = FilterChip.Builder().setTitle("Title").setOnClickListener {}.build()
        val style = FilterChipStyle.Builder().setBackgroundColor(CarColor.RED).build()
        val section =
            FilterChipSection.Builder().addItem(chip).setStyle(style).setTitle("Title").build()

        assertThat(
                FilterChipSection.Builder().addItem(chip).setStyle(style).setTitle("Title").build()
            )
            .isEqualTo(section)
    }

    @Test
    fun notEquals_differentItems() {
        val chip1 = FilterChip.Builder().setTitle("Title1").setOnClickListener {}.build()
        val chip2 = FilterChip.Builder().setTitle("Title2").setOnClickListener {}.build()
        val section = FilterChipSection.Builder().addItem(chip1).build()

        assertThat(FilterChipSection.Builder().addItem(chip2).build()).isNotEqualTo(section)
    }

    @Test
    fun notEquals_differentStyle() {
        val chip = FilterChip.Builder().setTitle("Title").setOnClickListener {}.build()
        val style1 = FilterChipStyle.Builder().setBackgroundColor(CarColor.RED).build()
        val style2 = FilterChipStyle.Builder().setBackgroundColor(CarColor.BLUE).build()
        val section = FilterChipSection.Builder().addItem(chip).setStyle(style1).build()

        assertThat(FilterChipSection.Builder().addItem(chip).setStyle(style2).build())
            .isNotEqualTo(section)
    }
}
