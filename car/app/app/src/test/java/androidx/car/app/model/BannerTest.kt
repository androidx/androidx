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
        assertThat(banner.style).isNull()
        assertThat(banner.leadingElement).isNull()
        assertThat(banner.trailingElements).isEmpty()
        assertThat(banner.belowActions).isEmpty()
    }

    @Test
    fun builder_populatedWithAllFields() {
        val title = "Title"
        val subtitle = "Subtitle"
        val style =
            BannerStyle.Builder()
                .setBackground(Background.Builder().setColor(CarColor.BLUE).build())
                .setShape(Shape.CORNER_MEDIUM)
                .build()
        val leadingIcon = CarIcon.ALERT
        val trailingImage = CarIcon.APP_ICON
        val trailingAction = Action.Builder().setTitle("TrailingAction").build()
        val belowAction = Action.Builder().setTitle("BelowAction").build()

        val banner =
            Banner.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setOnClickListener {}
                .setStyle(style)
                .setLeadingImage(leadingIcon, Banner.IMAGE_TYPE_ICON)
                .addTrailingAction(trailingAction)
                .addTrailingImage(trailingImage)
                .addBelowAction(belowAction)
                .build()

        assertThat(banner.title!!.toString()).isEqualTo(title)
        assertThat(banner.subtitle!!.toString()).isEqualTo(subtitle)
        assertThat(banner.onClickDelegate).isNotNull()
        assertThat(banner.style).isEqualTo(style)
        assertThat(banner.leadingElement!!.type).isEqualTo(BannerElement.TYPE_IMAGE)
        assertThat(banner.leadingElement!!.imageType).isEqualTo(Banner.IMAGE_TYPE_ICON)
        assertThat(banner.leadingElement!!.image).isEqualTo(leadingIcon)

        assertThat(banner.trailingElements).hasSize(2)
        assertThat(banner.trailingElements[0].type).isEqualTo(BannerElement.TYPE_ACTION)
        assertThat(banner.trailingElements[0].action).isEqualTo(trailingAction)
        assertThat(banner.trailingElements[1].type).isEqualTo(BannerElement.TYPE_IMAGE)
        assertThat(banner.trailingElements[1].imageType).isEqualTo(Banner.IMAGE_TYPE_SMALL)
        assertThat(banner.trailingElements[1].image).isEqualTo(trailingImage)

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
                .addTrailingImage(CarIcon.ALERT, Banner.IMAGE_TYPE_ICON)
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
        val style =
            BannerStyle.Builder()
                .setBackground(Background.Builder().setColor(CarColor.BLUE).build())
                .build()
        val leadingIcon = CarIcon.ALERT
        val trailingImage = CarIcon.APP_ICON
        val trailingAction = Action.Builder().setTitle("TrailingAction").build()
        val belowAction = Action.Builder().setTitle("BelowAction").build()

        val banner1 =
            Banner.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setOnClickListener {}
                .setStyle(style)
                .setLeadingImage(leadingIcon, Banner.IMAGE_TYPE_ICON)
                .addTrailingAction(trailingAction)
                .addTrailingImage(trailingImage)
                .addBelowAction(belowAction)
                .build()

        val banner2 =
            Banner.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setOnClickListener {}
                .setStyle(style)
                .setLeadingImage(leadingIcon, Banner.IMAGE_TYPE_ICON)
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
    fun equals_differentStyle_returnsFalse() {
        val style1 =
            BannerStyle.Builder()
                .setBackground(Background.Builder().setColor(CarColor.BLUE).build())
                .build()
        val style2 =
            BannerStyle.Builder()
                .setBackground(Background.Builder().setColor(CarColor.RED).build())
                .build()
        val banner1 = Banner.Builder().setTitle("Title").setStyle(style1).build()
        val banner2 = Banner.Builder().setTitle("Title").setStyle(style2).build()
        assertThat(banner1).isNotEqualTo(banner2)
    }

    @Test
    fun equals_differentLeadingElements_returnsFalse() {
        val banner1 =
            Banner.Builder()
                .setTitle("Title")
                .setLeadingImage(CarIcon.ALERT, Banner.IMAGE_TYPE_ICON)
                .build()
        val banner2 =
            Banner.Builder()
                .setTitle("Title")
                .setLeadingImage(CarIcon.APP_ICON, Banner.IMAGE_TYPE_ICON)
                .build()
        assertThat(banner1).isNotEqualTo(banner2)
    }

    @Test
    fun equals_differentTrailingElements_returnsFalse() {
        val banner1 =
            Banner.Builder()
                .setTitle("Title")
                .addTrailingImage(CarIcon.ALERT, Banner.IMAGE_TYPE_ICON)
                .build()
        val banner2 =
            Banner.Builder()
                .setTitle("Title")
                .addTrailingImage(CarIcon.APP_ICON, Banner.IMAGE_TYPE_ICON)
                .build()
        assertThat(banner1).isNotEqualTo(banner2)
    }

    @Test
    fun equals_differentStyleShape_returnsFalse() {
        val style1 = BannerStyle.Builder().setShape(Shape.CORNER_MEDIUM).build()
        val style2 = BannerStyle.Builder().setShape(Shape.CORNER_LARGE).build()
        val banner1 = Banner.Builder().setTitle("Title").setStyle(style1).build()
        val banner2 = Banner.Builder().setTitle("Title").setStyle(style2).build()
        assertThat(banner1).isNotEqualTo(banner2)
    }

    @Test
    fun builder_setLeadingImage_withVariants() {
        val bannerIcon =
            Banner.Builder()
                .setTitle("Title")
                .setLeadingImage(CarIcon.ALERT, Banner.IMAGE_TYPE_ICON)
                .build()
        assertThat(bannerIcon.leadingElement!!.type).isEqualTo(BannerElement.TYPE_IMAGE)
        assertThat(bannerIcon.leadingElement!!.imageType).isEqualTo(Banner.IMAGE_TYPE_ICON)

        val bannerSmall =
            Banner.Builder()
                .setTitle("Title")
                .setLeadingImage(CarIcon.ALERT, Banner.IMAGE_TYPE_SMALL)
                .build()
        assertThat(bannerSmall.leadingElement!!.type).isEqualTo(BannerElement.TYPE_IMAGE)
        assertThat(bannerSmall.leadingElement!!.imageType).isEqualTo(Banner.IMAGE_TYPE_SMALL)

        val bannerLarge =
            Banner.Builder()
                .setTitle("Title")
                .setLeadingImage(CarIcon.ALERT, Banner.IMAGE_TYPE_LARGE)
                .build()
        assertThat(bannerLarge.leadingElement!!.type).isEqualTo(BannerElement.TYPE_IMAGE)
        assertThat(bannerLarge.leadingElement!!.imageType).isEqualTo(Banner.IMAGE_TYPE_LARGE)
    }

    @Test
    fun builder_addTrailingImage_withVariants() {
        val banner =
            Banner.Builder()
                .setTitle("Title")
                .addTrailingImage(CarIcon.ALERT, Banner.IMAGE_TYPE_ICON)
                .addTrailingImage(CarIcon.APP_ICON, Banner.IMAGE_TYPE_LARGE)
                .build()
        assertThat(banner.trailingElements).hasSize(2)
        assertThat(banner.trailingElements[0].type).isEqualTo(BannerElement.TYPE_IMAGE)
        assertThat(banner.trailingElements[0].imageType).isEqualTo(Banner.IMAGE_TYPE_ICON)
        assertThat(banner.trailingElements[1].type).isEqualTo(BannerElement.TYPE_IMAGE)
        assertThat(banner.trailingElements[1].imageType).isEqualTo(Banner.IMAGE_TYPE_LARGE)
    }

    @Test
    fun equals_differentLeadingImageVariants_returnsFalse() {
        val banner1 =
            Banner.Builder()
                .setTitle("Title")
                .setLeadingImage(CarIcon.ALERT, Banner.IMAGE_TYPE_SMALL)
                .build()
        val banner2 =
            Banner.Builder()
                .setTitle("Title")
                .setLeadingImage(CarIcon.ALERT, Banner.IMAGE_TYPE_LARGE)
                .build()
        assertThat(banner1).isNotEqualTo(banner2)
        assertThat(banner1.hashCode()).isNotEqualTo(banner2.hashCode())
    }

    @Test
    fun equals_differentTrailingImageVariants_returnsFalse() {
        val banner1 =
            Banner.Builder()
                .setTitle("Title")
                .addTrailingImage(CarIcon.ALERT, Banner.IMAGE_TYPE_SMALL)
                .build()
        val banner2 =
            Banner.Builder()
                .setTitle("Title")
                .addTrailingImage(CarIcon.ALERT, Banner.IMAGE_TYPE_LARGE)
                .build()
        assertThat(banner1).isNotEqualTo(banner2)
        assertThat(banner1.hashCode()).isNotEqualTo(banner2.hashCode())
    }

    @Test
    fun builder_addMultipleTrailingLargeImages_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Banner.Builder()
                .setTitle("Title")
                .addTrailingImage(CarIcon.ALERT, Banner.IMAGE_TYPE_LARGE)
                .addTrailingImage(CarIcon.APP_ICON, Banner.IMAGE_TYPE_LARGE)
        }
    }

    @Test
    fun builder_addTrailingLargeImageWithSmallImage_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Banner.Builder()
                .setTitle("Title")
                .addTrailingImage(CarIcon.ALERT, Banner.IMAGE_TYPE_LARGE)
                .addTrailingImage(CarIcon.APP_ICON, Banner.IMAGE_TYPE_SMALL)
        }
    }

    @Test
    fun builder_addTrailingLargeImageWithAction_succeeds() {
        val action = Action.Builder().setTitle("Action").build()
        val banner =
            Banner.Builder()
                .setTitle("Title")
                .addTrailingImage(CarIcon.ALERT, Banner.IMAGE_TYPE_LARGE)
                .addTrailingAction(action)
                .build()

        assertThat(banner.trailingElements).hasSize(2)
        assertThat(banner.trailingElements[0].type).isEqualTo(BannerElement.TYPE_IMAGE)
        assertThat(banner.trailingElements[0].imageType).isEqualTo(Banner.IMAGE_TYPE_LARGE)
        assertThat(banner.trailingElements[1].type).isEqualTo(BannerElement.TYPE_ACTION)
        assertThat(banner.trailingElements[1].action).isEqualTo(action)
    }

    @Test
    fun builder_belowActionsWithLeadingLargeImage_throws() {
        val action = Action.Builder().setTitle("Action").build()
        assertThrows(IllegalArgumentException::class.java) {
            Banner.Builder()
                .setTitle("Title")
                .addBelowAction(action)
                .setLeadingImage(CarIcon.ALERT, Banner.IMAGE_TYPE_LARGE)
                .build()
        }
    }

    @Test
    fun builder_belowActionsWithTrailingLargeImage_throws() {
        val action = Action.Builder().setTitle("Action").build()
        assertThrows(IllegalArgumentException::class.java) {
            Banner.Builder()
                .setTitle("Title")
                .addBelowAction(action)
                .addTrailingImage(CarIcon.ALERT, Banner.IMAGE_TYPE_LARGE)
                .build()
        }
    }

    @Test
    fun builder_belowActionsWithSmallImageOrIcon_succeeds() {
        val action = Action.Builder().setTitle("Action").build()
        val banner =
            Banner.Builder()
                .setTitle("Title")
                .addBelowAction(action)
                .setLeadingImage(CarIcon.ALERT, Banner.IMAGE_TYPE_SMALL)
                .addTrailingImage(CarIcon.APP_ICON, Banner.IMAGE_TYPE_ICON)
                .build()

        assertThat(banner.belowActions).containsExactly(action)
        assertThat(banner.leadingElement!!.imageType).isEqualTo(Banner.IMAGE_TYPE_SMALL)
        assertThat(banner.trailingElements[0].imageType).isEqualTo(Banner.IMAGE_TYPE_ICON)
    }
}
