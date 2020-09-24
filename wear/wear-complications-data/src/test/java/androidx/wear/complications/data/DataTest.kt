/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.complications.data

import android.graphics.drawable.Icon
import androidx.test.filters.SmallTest
import androidx.wear.complications.data.ParcelableSubject.Companion.assertThat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RestrictedInstrumentationRobolectricTestRunner::class)
@DoNotInstrumentPackage("androidx.wear.complications.data")
@SmallTest
class AsWireComplicationDataTest {
    @Test
    fun noDataComplicationData() {
        val data = NoDataComplicationData()
        assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA).build()
            )
    }

    @Test
    fun emptyComplicationData() {
        val data = EmptyComplicationData()
        assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_EMPTY).build()
            )
    }

    @Test
    fun notConfiguredComplicationData() {
        val data = NotConfiguredComplicationData()
        assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NOT_CONFIGURED).build()
            )
    }

    @Test
    fun shortTextComplicationData() {
        val data = ShortTextComplicationData.Builder("text".complicationText)
            .setTitle("title".complicationText)
            .build()
        assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_SHORT_TEXT)
                    .setShortText(WireComplicationText.plainText("text"))
                    .setShortTitle(WireComplicationText.plainText("title"))
                    .build()
            )
    }

    @Test
    fun longTextComplicationData() {
        val data = LongTextComplicationData.Builder("text".complicationText)
            .setTitle("title".complicationText)
            .build()
        assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_LONG_TEXT)
                    .setLongText(WireComplicationText.plainText("text"))
                    .setLongTitle(WireComplicationText.plainText("title"))
                    .build()
            )
    }

    @Test
    fun rangedValueComplicationData() {
        val data = RangedValueComplicationData.Builder(value = 95f, min = 0f, max = 100f)
            .setTitle("battery".complicationText)
            .build()
        assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_RANGED_VALUE)
                    .setRangedValue(95f)
                    .setRangedMinValue(0f)
                    .setRangedMaxValue(100f)
                    .setShortTitle(WireComplicationText.plainText("battery"))
                    .build()
            )
    }

    @Test
    fun monochromaticImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        val image = MonochromaticImage.Builder(icon).build()
        val data = MonochromaticImageComplicationData.Builder(image).build()
        assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_ICON)
                    .setIcon(icon)
                    .build()
            )
    }

    @Test
    fun smallImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        val image = SmallImage.Builder(icon, SmallImageType.PHOTO).build()
        val data = SmallImageComplicationData.Builder(image).build()
        assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_SMALL_IMAGE)
                    .setSmallImage(icon)
                    .setSmallImageStyle(WireComplicationData.IMAGE_STYLE_PHOTO)
                    .build()
            )
    }

    @Test
    fun backgroundImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        val image = BackgroundImage.Builder(icon).build()
        val data = BackgroundImageComplicationData.Builder(image)
            .setContentDescription("content description".complicationText)
            .build()
        assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_LARGE_IMAGE)
                    .setLargeImage(icon)
                    .setContentDescription(WireComplicationText.plainText("content description"))
                    .build()
            )
    }

    @Test
    fun noPermissionComplicationData() {
        val data = NoPermissionComplicationData.Builder()
            .setText("needs location".complicationText)
            .build()
        assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_PERMISSION)
                    .setShortText(WireComplicationText.plainText("needs location"))
                    .build()
            )
    }
}

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@SmallTest
class FromWireComplicationDataTest {
    @Test
    fun noDataComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA).build(),
            ComplicationType.NO_DATA
        )
    }

    @Test
    fun emptyComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_EMPTY).build(),
            ComplicationType.EMPTY
        )
    }

    @Test
    fun notConfiguredComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_NOT_CONFIGURED).build(),
            ComplicationType.NOT_CONFIGURED
        )
    }

    @Test
    fun shortTextComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_SHORT_TEXT)
                .setShortText(WireComplicationText.plainText("text"))
                .setShortTitle(WireComplicationText.plainText("title"))
                .build(),
            ComplicationType.SHORT_TEXT
        )
    }

    @Test
    fun longTextComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_LONG_TEXT)
                .setLongText(WireComplicationText.plainText("text"))
                .setLongTitle(WireComplicationText.plainText("title"))
                .build(),
            ComplicationType.LONG_TEXT
        )
    }

    @Test
    fun rangedValueComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_RANGED_VALUE)
                .setRangedValue(95f)
                .setRangedMinValue(0f)
                .setRangedMaxValue(100f)
                .setShortTitle(WireComplicationText.plainText("battery"))
                .build(),
            ComplicationType.RANGED_VALUE
        )
    }

    @Test
    fun monochromaticImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_ICON)
                .setIcon(icon)
                .build(),
            ComplicationType.MONOCHROMATIC_IMAGE
        )
    }

    @Test
    fun smallImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_SMALL_IMAGE)
                .setSmallImage(icon)
                .setSmallImageStyle(WireComplicationData.IMAGE_STYLE_PHOTO)
                .build(),
            ComplicationType.SMALL_IMAGE
        )
    }

    @Test
    fun backgroundImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_LARGE_IMAGE)
                .setLargeImage(icon)
                .setContentDescription(WireComplicationText.plainText("content description"))
                .build(),
            ComplicationType.BACKGROUND_IMAGE
        )
    }

    @Test
    fun noPermissionComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_NO_PERMISSION)
                .setShortText(WireComplicationText.plainText("needs location"))
                .build(),
            ComplicationType.NO_PERMISSION
        )
    }

    private fun assertRoundtrip(wireData: WireComplicationData, type: ComplicationType) {
        val data = wireData.asApiComplicationData()
        assertThat(data.type).isEqualTo(type)
        assertThat(data.asWireComplicationData()).hasSameSerializationAs(wireData)
    }
}

private val String.complicationText get() = ComplicationText.plain(this)
