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

import android.text.SpannableString
import android.text.Spanned
import androidx.core.graphics.drawable.IconCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Tests for [CondensedItem]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@DoNotInstrument
class CondensedItemTest {
    @Test
    fun setTitle_invalidText_throws() {
        val title = SpannableString("Title")
        title.setSpan(ClickableSpan.create {}, 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        val textWithClickableSpan = CarText.create(title)
        assertThrows(IllegalArgumentException::class.java) {
            CondensedItem.Builder().setTitle(textWithClickableSpan)
        }
    }

    @Test
    fun setText_invalidText_throws() {
        val text = SpannableString("Text")
        text.setSpan(ClickableSpan.create {}, 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        val textWithClickableSpan = CarText.create(text)
        assertThrows(IllegalArgumentException::class.java) {
            CondensedItem.Builder().setText(textWithClickableSpan)
        }
    }

    @Test
    fun setLeadingImage_invalidIcon_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            val icon = CarIcon.Builder(IconCompat.createWithData(ByteArray(0), 0, 0)).build()
            CondensedItem.Builder().setLeadingImage(icon, CondensedItem.IMAGE_TYPE_ICON)
        }
    }

    @Test
    fun build_validItem_titleOnly() {
        val item = CondensedItem.Builder().setTitle("Title").build()
        assertThat(item.title.toString()).isEqualTo("Title")
    }

    @Test
    fun build_noRequiredFields_throws() {
        assertThrows(IllegalStateException::class.java) { CondensedItem.Builder().build() }
    }

    @Test
    fun setLeadingImage_defaultStyle() {
        val icon = CarIcon.BACK
        val item = CondensedItem.Builder().setTitle("Title").setLeadingImage(icon).build()
        assertThat(item.leadingImage).isEqualTo(icon)
        assertThat(item.leadingImageType).isEqualTo(CondensedItem.IMAGE_TYPE_SMALL)
    }

    @Test
    fun setTrailingImage_defaultStyle() {
        val icon = CarIcon.BACK
        val item = CondensedItem.Builder().setTitle("Title").setTrailingImage(icon).build()
        assertThat(item.trailingImage).isEqualTo(icon)
        assertThat(item.trailingImageType).isEqualTo(CondensedItem.IMAGE_TYPE_SMALL)
    }

    @Test
    fun setLeadingImage() {
        val icon = CarIcon.BACK
        val item =
            CondensedItem.Builder()
                .setTitle("Title")
                .setLeadingImage(icon, CondensedItem.IMAGE_TYPE_ICON)
                .build()
        assertThat(item.leadingImage).isEqualTo(icon)
        assertThat(item.leadingImageType).isEqualTo(CondensedItem.IMAGE_TYPE_ICON)
    }

    @Test
    fun setTrailingImage() {
        val icon = CarIcon.BACK
        val item =
            CondensedItem.Builder()
                .setTitle("Title")
                .setTrailingImage(icon, CondensedItem.IMAGE_TYPE_SMALL)
                .build()
        assertThat(item.trailingImage).isEqualTo(icon)
        assertThat(item.trailingImageType).isEqualTo(CondensedItem.IMAGE_TYPE_SMALL)
    }

    @Test
    fun setOnClickListener() {
        val item = CondensedItem.Builder().setTitle("Title").setOnClickListener {}.build()
        assertThat(item.onClickDelegate).isNotNull()
    }

    @Test
    fun setProgressBar() {
        val progressBar = CarProgressBar.Builder(0.5f).build()
        val item = CondensedItem.Builder().setTitle("Title").setProgressBar(progressBar).build()
        assertThat(item.progressBar).isEqualTo(progressBar)
    }

    @Test
    fun textAndProgressBarSet_throws() {
        val bar = CarProgressBar.Builder(0.5f).build()
        assertThrows(IllegalStateException::class.java) {
            CondensedItem.Builder().setTitle("Title").setText("Text").setProgressBar(bar).build()
        }
    }

    @Test
    fun setIndexable() {
        val item = CondensedItem.Builder().setTitle("Title").setIndexable(false).build()
        assertThat(item.isIndexable).isFalse()
    }

    @Test
    fun isIndexable_defaultIsTrue() {
        val item = CondensedItem.Builder().setTitle("Title").build()
        assertThat(item.isIndexable).isTrue()
    }

    @Test
    fun equals() {
        val icon = CarIcon.BACK
        val itemStyle = CondensedItemStyle.Builder().setShape(Shape.NONE).build()

        val item1 =
            CondensedItem.Builder()
                .setTitle("Title")
                .setText("Text")
                .setLeadingImage(icon, CondensedItem.IMAGE_TYPE_ICON)
                .setStyle(itemStyle)
                .build()
        val item2 =
            CondensedItem.Builder()
                .setTitle("Title")
                .setText("Text")
                .setLeadingImage(icon, CondensedItem.IMAGE_TYPE_ICON)
                .setStyle(itemStyle)
                .build()
        val item3 = CondensedItem.Builder().setTitle("Title").setText("Other Text").build()

        assertThat(item1).isEqualTo(item2)
        assertThat(item1).isNotEqualTo(item3)
    }

    @Test
    fun hashCode_match() {
        val item1 = CondensedItem.Builder().setTitle("Title").build()
        val item2 = CondensedItem.Builder().setTitle("Title").build()

        assertThat(item1.hashCode()).isEqualTo(item2.hashCode())
    }
}
