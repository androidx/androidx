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
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Tests for [Banner]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@DoNotInstrument
class BannerTest {
    @Test
    fun builder_defaults() {
        val banner = Banner.Builder().setTitle("Title").build()

        assertThat(banner.title!!.toString()).isEqualTo("Title")
        assertThat(banner.subtitle).isNull()
        assertThat(banner.onClickDelegate).isNull()
        assertThat(banner.background).isNull()
        assertThat(banner.leadingElement).isNull()
        assertThat(banner.trailingElements).isEmpty()
        assertThat(banner.belowActions).isEmpty()
    }

    @Test
    fun builder_populatedWithAllFields() {
        val title = "Title"
        val subtitle = "Subtitle"
        val background = Background.Builder().setColor(CarColor.BLUE).build()
        val leadingIcon = CarIcon.ALERT
        val trailingImage = CarIcon.APP_ICON
        val trailingAction = Action.Builder().setTitle("TrailingAction").build()
        val belowAction = Action.Builder().setTitle("BelowAction").build()

        val banner =
            Banner.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setOnClickListener {}
                .setBackground(background)
                .setLeadingIcon(leadingIcon)
                .addTrailingAction(trailingAction)
                .addTrailingImage(trailingImage)
                .addBelowAction(belowAction)
                .build()

        assertThat(banner.title!!.toString()).isEqualTo(title)
        assertThat(banner.subtitle!!.toString()).isEqualTo(subtitle)
        assertThat(banner.onClickDelegate).isNotNull()
        assertThat(banner.background).isEqualTo(background)
        assertThat(banner.leadingElement!!.type).isEqualTo(BannerElement.TYPE_ICON)
        assertThat(banner.leadingElement!!.icon).isEqualTo(leadingIcon)

        assertThat(banner.trailingElements).hasSize(2)
        assertThat(banner.trailingElements[0].type).isEqualTo(BannerElement.TYPE_ACTION)
        assertThat(banner.trailingElements[0].action).isEqualTo(trailingAction)
        assertThat(banner.trailingElements[1].type).isEqualTo(BannerElement.TYPE_IMAGE)
        assertThat(banner.trailingElements[1].icon).isEqualTo(trailingImage)

        assertThat(banner.belowActions).containsExactly(belowAction)
    }

    @Test
    fun builder_withNoTitle_throws() {
        assertThrows(IllegalArgumentException::class.java) { Banner.Builder().build() }
    }

    @Test
    fun builder_withTooManyTrailingElements_throws() {
        val action = Action.Builder().setTitle("Action").build()
        try {
            Banner.Builder()
                .setTitle("Title")
                .addTrailingAction(action)
                .addTrailingIcon(CarIcon.ALERT)
                .addTrailingImage(CarIcon.APP_ICON)
        } catch (e: IllegalStateException) {
            assertThat(e.message)
                .startsWith("Total number of trailing elements in a banner must not exceed")
        }
    }

    @Test
    fun builder_addBelowAction_with3ActionsThatHaveText_throws() {
        val action = Action.Builder().setTitle("Action").build()
        assertThrows(IllegalArgumentException::class.java) {
            Banner.Builder()
                .setTitle("Title")
                .addBelowAction(action)
                .addBelowAction(action)
                .addBelowAction(action)
        }
    }

    @Test
    fun title_unsupportedSpans_throws() {
        val title: CharSequence = "Title"
        val spannable = SpannableString(title)
        spannable.setSpan(ClickableSpan.create {}, 0, title.length, 0)
        assertThrows(IllegalArgumentException::class.java) { Banner.Builder().setTitle(spannable) }
    }

    @Test
    fun subtitle_unsupportedSpans_throws() {
        val subtitle: CharSequence = "Subtitle"
        val spannable = SpannableString(subtitle)
        spannable.setSpan(ClickableSpan.create {}, 0, subtitle.length, 0)
        assertThrows(IllegalArgumentException::class.java) {
            Banner.Builder().setSubtitle(spannable)
        }
    }

    @Test
    fun equals() {
        val title = "Title"
        val subtitle = "Subtitle"
        val background = Background.Builder().setColor(CarColor.BLUE).build()
        val leadingIcon = CarIcon.ALERT
        val trailingImage = CarIcon.APP_ICON
        val trailingAction = Action.Builder().setTitle("TrailingAction").build()
        val belowAction = Action.Builder().setTitle("BelowAction").build()

        val banner1 =
            Banner.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setOnClickListener {}
                .setBackground(background)
                .setLeadingIcon(leadingIcon)
                .addTrailingAction(trailingAction)
                .addTrailingImage(trailingImage)
                .addBelowAction(belowAction)
                .build()

        val banner2 =
            Banner.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setOnClickListener {}
                .setBackground(background)
                .setLeadingIcon(leadingIcon)
                .addTrailingAction(trailingAction)
                .addTrailingImage(trailingImage)
                .addBelowAction(belowAction)
                .build()

        assertThat(banner1).isEqualTo(banner2)
    }

    @Test
    fun equals_differentTitle_returnsFalse() {
        val banner1 = Banner.Builder().setTitle("Title1").build()
        val banner2 = Banner.Builder().setTitle("Title2").build()
        assertThat(banner1).isNotEqualTo(banner2)
    }

    @Test
    fun equals_differentSubtitle_returnsFalse() {
        val banner1 = Banner.Builder().setTitle("Title").setSubtitle("Subtitle1").build()
        val banner2 = Banner.Builder().setTitle("Title").setSubtitle("Subtitle2").build()
        assertThat(banner1).isNotEqualTo(banner2)
    }

    @Test
    fun equals_differentListener_returnsFalse() {
        val banner1 = Banner.Builder().setTitle("Title").setOnClickListener {}.build()
        val banner2 = Banner.Builder().setTitle("Title").build()
        assertThat(banner1).isNotEqualTo(banner2)
    }

    @Test
    fun equals_differentBackgroundColor_returnsFalse() {
        val banner1 =
            Banner.Builder()
                .setTitle("Title")
                .setBackground(Background.Builder().setColor(CarColor.BLUE).build())
                .build()
        val banner2 =
            Banner.Builder()
                .setTitle("Title")
                .setBackground(Background.Builder().setColor(CarColor.RED).build())
                .build()
        assertThat(banner1).isNotEqualTo(banner2)
    }

    @Test
    fun equals_differentLeadingElements_returnsFalse() {
        val banner1 = Banner.Builder().setTitle("Title").setLeadingIcon(CarIcon.ALERT).build()
        val banner2 = Banner.Builder().setTitle("Title").setLeadingIcon(CarIcon.APP_ICON).build()
        assertThat(banner1).isNotEqualTo(banner2)
    }

    @Test
    fun equals_differentTrailingElements_returnsFalse() {
        val banner1 = Banner.Builder().setTitle("Title").addTrailingIcon(CarIcon.ALERT).build()
        val banner2 = Banner.Builder().setTitle("Title").addTrailingIcon(CarIcon.APP_ICON).build()
        assertThat(banner1).isNotEqualTo(banner2)
    }
}
