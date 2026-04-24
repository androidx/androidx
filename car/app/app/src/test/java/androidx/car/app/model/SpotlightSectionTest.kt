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
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class SpotlightSectionTest {
    @Test
    fun create_defaultValues() {
        val image = CarIcon.APP_ICON
        val item = CondensedItem.Builder().setTitle("Title").build()

        val section = SpotlightSection.Builder(image).addItem(item).build()

        assertThat(section.image).isEqualTo(image)
        assertThat(section.title).isNull()
        assertThat(section.noItemsMessage).isNull()
        assertThat(section.onItemVisibilityChangedDelegate).isNull()
    }

    @Test
    fun build_withAllFields() {
        val title = "Spotlight Title"
        val image = CarIcon.APP_ICON
        val item = CondensedItem.Builder().setTitle("Item Title").build()

        val section = SpotlightSection.Builder(image).setTitle(title).addItem(item).build()

        assertThat(section.title?.toString()).isEqualTo(title)
        assertThat(section.image).isEqualTo(image)
    }

    @Test
    fun build_withEmptyItems_throwsException() {
        assertThrows(IllegalStateException::class.java) {
            SpotlightSection.Builder(CarIcon.APP_ICON).build()
        }
    }

    @Test
    fun equals_andHashCode() {
        val image = CarIcon.APP_ICON
        val item = CondensedItem.Builder().setTitle("Item").build()

        val section1 = SpotlightSection.Builder(image).setTitle("Title").addItem(item).build()

        val section2 = SpotlightSection.Builder(image).setTitle("Title").addItem(item).build()

        assertThat(section1).isEqualTo(section2)
        assertThat(section1.hashCode()).isEqualTo(section2.hashCode())
    }

    @Test
    fun equals_differentImages_notEqual() {
        val image1 = CarIcon.APP_ICON
        val image2 = CarIcon.ERROR
        val item = CondensedItem.Builder().setTitle("Title").build()

        val section1 = SpotlightSection.Builder(image1).addItem(item).build()
        val section2 = SpotlightSection.Builder(image2).addItem(item).build()

        assertThat(section1).isNotEqualTo(section2)
    }

    @Test
    fun equals_differentTitles_notEqual() {
        val image = CarIcon.APP_ICON
        val item = CondensedItem.Builder().setTitle("Title").build()

        val section1 = SpotlightSection.Builder(image).setTitle("Title 1").addItem(item).build()
        val section2 = SpotlightSection.Builder(image).setTitle("Title 2").addItem(item).build()

        assertThat(section1).isNotEqualTo(section2)
    }

    @Test
    fun equals_differentItems_notEqual() {
        val image = CarIcon.APP_ICON
        val item1 = CondensedItem.Builder().setTitle("Title 1").build()
        val item2 = CondensedItem.Builder().setTitle("Title 2").build()

        val section1 = SpotlightSection.Builder(image).addItem(item1).build()
        val section2 = SpotlightSection.Builder(image).addItem(item2).build()

        assertThat(section1).isNotEqualTo(section2)
    }
}
