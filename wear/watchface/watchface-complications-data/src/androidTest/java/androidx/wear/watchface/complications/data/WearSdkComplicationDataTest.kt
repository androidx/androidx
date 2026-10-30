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

package androidx.wear.watchface.complications.data

import android.graphics.drawable.Icon
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.watchface.utility.iconEquals
import com.google.common.truth.Truth.assertThat
import com.google.wear.services.complications.ComplicationData as WearSdkComplicationData
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [ComplicationData.asWearSdkComplicationData].
 *
 * The Wear SDK is only available as a stub at compile time, so these conversions can only be
 * verified on a watch where the real Wear SDK implementation is present.
 */
@RunWith(AndroidJUnit4::class)
// The conversion is used by ComplicationDataSourceUpdateRequester on API 36+ watches.
@SdkSuppress(minSdkVersion = 36)
@SmallTest
class WearSdkComplicationDataTest {

    @Test
    @RequiresWatch
    fun shortText_copiesSmallImage() {
        val data =
            ShortTextComplicationData.Builder(TEXT, CONTENT_DESCRIPTION)
                .setSmallImage(SMALL_IMAGE_ICON)
                .build()

        val wearSdkData = data.asWearSdkComplicationData()

        assertThat(wearSdkData.type).isEqualTo(ComplicationType.SHORT_TEXT.toWireComplicationType())
        assertSmallImageIcon(wearSdkData)
    }

    @Test
    @RequiresWatch
    fun shortText_copiesSmallImage_alongsideMonochromaticImage() {
        val data =
            ShortTextComplicationData.Builder(TEXT, CONTENT_DESCRIPTION)
                .setMonochromaticImage(
                    MonochromaticImage.Builder(ICON).setAmbientImage(AMBIENT_ICON).build()
                )
                .setSmallImage(SMALL_IMAGE_ICON)
                .build()

        val wearSdkData = data.asWearSdkComplicationData()

        assertThat(wearSdkData.hasIcon()).isTrue()
        assertThat(wearSdkData.icon iconEquals ICON).isTrue()
        assertThat(wearSdkData.hasBurnInProtectionIcon()).isTrue()
        assertThat(wearSdkData.burnInProtectionIcon iconEquals AMBIENT_ICON).isTrue()
        assertSmallImageIcon(wearSdkData)
    }

    @Test
    @RequiresWatch
    fun shortText_withoutSmallImage_hasNoSmallImage() {
        val data = ShortTextComplicationData.Builder(TEXT, CONTENT_DESCRIPTION).build()

        val wearSdkData = data.asWearSdkComplicationData()

        assertThat(wearSdkData.hasSmallImage()).isFalse()
        assertThat(wearSdkData.hasBurnInProtectionSmallImage()).isFalse()
    }

    @Test
    @RequiresWatch
    fun longText_copiesSmallImage() {
        val data =
            LongTextComplicationData.Builder(TEXT, CONTENT_DESCRIPTION)
                .setSmallImage(SMALL_IMAGE_PHOTO)
                .build()

        val wearSdkData = data.asWearSdkComplicationData()

        assertThat(wearSdkData.type).isEqualTo(ComplicationType.LONG_TEXT.toWireComplicationType())
        assertSmallImagePhoto(wearSdkData)
    }

    @Test
    @RequiresWatch
    fun noPermission_copiesSmallImage() {
        val data =
            NoPermissionComplicationData.Builder()
                .setText(TEXT)
                .setSmallImage(SMALL_IMAGE_ICON)
                .build()

        val wearSdkData = data.asWearSdkComplicationData()

        assertThat(wearSdkData.type)
            .isEqualTo(ComplicationType.NO_PERMISSION.toWireComplicationType())
        assertSmallImageIcon(wearSdkData)
    }

    @Test
    @RequiresWatch
    fun noData_copiesSmallImageOfShortTextPlaceholder() {
        val data =
            NoDataComplicationData(
                ShortTextComplicationData.Builder(
                        ComplicationText.PLACEHOLDER,
                        ComplicationText.EMPTY,
                    )
                    .setSmallImage(SMALL_IMAGE_ICON)
                    .build()
            )

        val wearSdkData = data.asWearSdkComplicationData()

        assertThat(wearSdkData.type).isEqualTo(ComplicationType.NO_DATA.toWireComplicationType())
        assertThat(wearSdkData.hasPlaceholder()).isTrue()
        assertSmallImageIcon(wearSdkData.placeholder)
    }

    @Test
    @RequiresWatch
    fun rangedValue_copiesSmallImage() {
        val data =
            RangedValueComplicationData.Builder(5f, 0f, 10f, CONTENT_DESCRIPTION)
                .setSmallImage(SMALL_IMAGE_ICON)
                .build()

        assertSmallImageIcon(data.asWearSdkComplicationData())
    }

    @Test
    @RequiresWatch
    fun goalProgress_copiesSmallImage() {
        val data =
            GoalProgressComplicationData.Builder(5f, 10f, CONTENT_DESCRIPTION)
                .setSmallImage(SMALL_IMAGE_ICON)
                .build()

        assertSmallImageIcon(data.asWearSdkComplicationData())
    }

    @Test
    @RequiresWatch
    fun weightedElements_copiesSmallImage() {
        val data =
            WeightedElementsComplicationData.Builder(
                    listOf(WeightedElementsComplicationData.Element(1f, 0xFF0000FF.toInt())),
                    CONTENT_DESCRIPTION,
                )
                .setSmallImage(SMALL_IMAGE_ICON)
                .build()

        assertSmallImageIcon(data.asWearSdkComplicationData())
    }

    @Test
    @RequiresWatch
    fun smallImage_copiesSmallImage() {
        val data =
            SmallImageComplicationData.Builder(SMALL_IMAGE_PHOTO, CONTENT_DESCRIPTION).build()

        assertSmallImagePhoto(data.asWearSdkComplicationData())
    }

    @Test
    @RequiresWatch
    fun nonEmptyContentDescription_isCopied() {
        val data = ShortTextComplicationData.Builder(TEXT, CONTENT_DESCRIPTION).build()

        val wearSdkData = data.asWearSdkComplicationData()

        assertThat(wearSdkData.hasContentDescription()).isTrue()
    }

    @Test
    @RequiresWatch
    fun dynamicContentDescription_isCopied() {
        // A DynamicComplicationText without a fallback has an empty surrounding text, so it must
        // not be treated as an empty content description.
        val data =
            ShortTextComplicationData.Builder(
                    TEXT,
                    DynamicComplicationText(DynamicString.constant("description")),
                )
                .build()

        val wearSdkData = data.asWearSdkComplicationData()

        assertThat(wearSdkData.hasContentDescription()).isTrue()
    }

    // Helpers that mention Wear SDK types must stay in the companion object (and must not be
    // @JvmStatic). JUnit reflects over all methods of the test class before filters such as
    // @RequiresWatch are applied, and on devices without the Wear SDK any method signature that
    // mentions a Wear SDK type fails with NoClassDefFoundError, turning the whole class into an
    // initializationError.
    private companion object {
        private fun assertSmallImageIcon(wearSdkData: WearSdkComplicationData) {
            assertThat(wearSdkData.hasSmallImage()).isTrue()
            assertThat(wearSdkData.smallImage iconEquals SMALL_ICON).isTrue()
            assertThat(wearSdkData.smallImageStyle)
                .isEqualTo(WearSdkComplicationData.IMAGE_STYLE_ICON)
            assertThat(wearSdkData.hasBurnInProtectionSmallImage()).isTrue()
            assertThat(wearSdkData.burnInProtectionSmallImage iconEquals AMBIENT_ICON).isTrue()
        }

        private fun assertSmallImagePhoto(wearSdkData: WearSdkComplicationData) {
            assertThat(wearSdkData.hasSmallImage()).isTrue()
            assertThat(wearSdkData.smallImage iconEquals SMALL_ICON).isTrue()
            assertThat(wearSdkData.smallImageStyle)
                .isEqualTo(WearSdkComplicationData.IMAGE_STYLE_PHOTO)
            assertThat(wearSdkData.hasBurnInProtectionSmallImage()).isFalse()
        }

        val TEXT = PlainComplicationText.Builder("text").build()
        val CONTENT_DESCRIPTION = PlainComplicationText.Builder("description").build()
        val ICON: Icon = Icon.createWithContentUri(Uri.parse("content://test/icon"))
        val AMBIENT_ICON: Icon = Icon.createWithContentUri(Uri.parse("content://test/ambient"))
        val SMALL_ICON: Icon = Icon.createWithContentUri(Uri.parse("content://test/small"))
        val SMALL_IMAGE_ICON =
            SmallImage.Builder(SMALL_ICON, SmallImageType.ICON)
                .setAmbientImage(AMBIENT_ICON)
                .build()
        val SMALL_IMAGE_PHOTO = SmallImage.Builder(SMALL_ICON, SmallImageType.PHOTO).build()
    }
}
