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

package androidx.car.app.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Tests for [CondensedSection]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@DoNotInstrument
class CondensedSectionTest {
    @Test
    fun addItems() {
        val item1 = CondensedItem.Builder().setTitle("Title 1").build()
        val item2 = CondensedItem.Builder().setTitle("Title 2").build()
        val section = CondensedSection.Builder().addItem(item1).addItem(item2).build()
        assertThat(section.itemsDelegate.size).isEqualTo(2)
    }

    @Test
    fun setTitleAndNoItemsMessage() {
        val section =
            CondensedSection.Builder().setTitle("Title").setNoItemsMessage("No items").build()
        assertThat(section.title.toString()).isEqualTo("Title")
        assertThat(section.noItemsMessage.toString()).isEqualTo("No items")
    }

    @Test
    fun equals() {
        val item = CondensedItem.Builder().setTitle("Title").build()
        val section1 = CondensedSection.Builder().setTitle("Title").addItem(item).build()
        val section2 = CondensedSection.Builder().setTitle("Title").addItem(item).build()
        val section3 = CondensedSection.Builder().setTitle("Title").build()

        assertThat(section1).isEqualTo(section2)
        assertThat(section1).isNotEqualTo(section3)
    }

    @Test
    fun hashCode_match() {
        val item = CondensedItem.Builder().setTitle("Title").build()
        val section1 = CondensedSection.Builder().setTitle("Title").addItem(item).build()
        val section2 = CondensedSection.Builder().setTitle("Title").addItem(item).build()

        assertThat(section1.hashCode()).isEqualTo(section2.hashCode())
    }
}
