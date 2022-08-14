/*
 * Copyright 2022 The Android Open Source Project
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

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationData.IMAGE_STYLE_ICON
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(SharedRobolectricTestRunner::class)
class PlaceholderTest {
    val text = "text".complicationText
    val title = "title".complicationText
    val contentDescription = "description".complicationText
    val icon = Icon.createWithContentUri("someuri")
    val monochromaticImage = MonochromaticImage.Builder(icon).build()
    val smallImage = SmallImage.Builder(icon, SmallImageType.ICON).build()
    val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun placeholder_shortText() {
        val placeholderShortText = NoDataComplicationData(
            ShortTextComplicationData.Builder(ComplicationText.PLACEHOLDER, contentDescription)
                .setTitle(ComplicationText.PLACEHOLDER)
                .setMonochromaticImage(MonochromaticImage.PLACEHOLDER)
                .build()
        ).toWireFormatRoundTrip().placeholder as ShortTextComplicationData

        assertThat(placeholderShortText.text).isEqualTo(ComplicationText.PLACEHOLDER)
        assertThat(placeholderShortText.title).isEqualTo(ComplicationText.PLACEHOLDER)
        assertThat(placeholderShortText.monochromaticImage)
            .isEqualTo(MonochromaticImage.PLACEHOLDER)
        assertThat(placeholderShortText.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("description")
        assertThat(placeholderShortText.hasPlaceholderFields()).isTrue()
    }

    @Test
    fun normal_shortText() {
        val placeholderShortText = NoDataComplicationData(
            ShortTextComplicationData.Builder(text, contentDescription)
                .setTitle(title)
                .setMonochromaticImage(monochromaticImage)
                .build()
        ).toWireFormatRoundTrip().placeholder as ShortTextComplicationData

        assertThat(placeholderShortText.text.getTextAt(resources, Instant.EPOCH)).isEqualTo(
            text.getTextAt(resources, Instant.EPOCH)
        )
        assertThat(placeholderShortText.title!!.getTextAt(resources, Instant.EPOCH)).isEqualTo(
            title.getTextAt(resources, Instant.EPOCH)
        )
        assertThat(placeholderShortText.monochromaticImage).isEqualTo(monochromaticImage)
        assertThat(placeholderShortText.hasPlaceholderFields()).isFalse()
    }

    @Test
    fun absent_shortText() {
        val placeholderShortText = NoDataComplicationData(
            ShortTextComplicationData.Builder(ComplicationText.PLACEHOLDER, contentDescription)
                .build()
        ).toWireFormatRoundTrip().placeholder as ShortTextComplicationData

        assertThat(placeholderShortText.title).isNull()
        assertThat(placeholderShortText.monochromaticImage).isNull()
    }

    @Test
    fun placeholder_longText() {
        val placeholderLongText = NoDataComplicationData(
            LongTextComplicationData.Builder(ComplicationText.PLACEHOLDER, contentDescription)
                .setTitle(ComplicationText.PLACEHOLDER)
                .setMonochromaticImage(MonochromaticImage.PLACEHOLDER)
                .setSmallImage(SmallImage.PLACEHOLDER)
                .build()
        ).toWireFormatRoundTrip().placeholder as LongTextComplicationData

        assertThat(placeholderLongText.text).isEqualTo(ComplicationText.PLACEHOLDER)
        assertThat(placeholderLongText.title).isEqualTo(ComplicationText.PLACEHOLDER)
        assertThat(placeholderLongText.monochromaticImage)
            .isEqualTo(MonochromaticImage.PLACEHOLDER)
        assertThat(placeholderLongText.smallImage).isEqualTo(SmallImage.PLACEHOLDER)
        assertThat(placeholderLongText.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("description")
        assertThat(placeholderLongText.hasPlaceholderFields()).isTrue()
    }

    @Test
    fun normal_longText() {
        val placeholderLongText = NoDataComplicationData(
            LongTextComplicationData.Builder(text, contentDescription)
                .setTitle(title)
                .setMonochromaticImage(monochromaticImage)
                .setSmallImage(smallImage)
                .build()
        ).toWireFormatRoundTrip().placeholder as LongTextComplicationData

        assertThat(placeholderLongText.text.getTextAt(resources, Instant.EPOCH)).isEqualTo(
            text.getTextAt(resources, Instant.EPOCH)
        )
        assertThat(placeholderLongText.title!!.getTextAt(resources, Instant.EPOCH)).isEqualTo(
            title.getTextAt(resources, Instant.EPOCH)
        )
        assertThat(placeholderLongText.monochromaticImage).isEqualTo(monochromaticImage)
        assertThat(placeholderLongText.smallImage).isEqualTo(smallImage)
        assertThat(placeholderLongText.hasPlaceholderFields()).isFalse()
    }

    @Test
    fun absent_longText() {
        val placeholderLongText = NoDataComplicationData(
            LongTextComplicationData.Builder(ComplicationText.PLACEHOLDER, contentDescription)
                .build()
        ).toWireFormatRoundTrip().placeholder as LongTextComplicationData

        assertThat(placeholderLongText.title).isNull()
        assertThat(placeholderLongText.monochromaticImage).isNull()
        assertThat(placeholderLongText.smallImage).isNull()
    }

    @Test
    fun placeholder_rangedValue() {
        val placeholderRangedValue = NoDataComplicationData(
            RangedValueComplicationData.Builder(
                value = RangedValueComplicationData.PLACEHOLDER,
                min = 1f,
                max = 10f,
                contentDescription
            )
                .setText(ComplicationText.PLACEHOLDER)
                .setTitle(ComplicationText.PLACEHOLDER)
                .setMonochromaticImage(MonochromaticImage.PLACEHOLDER)
                .build()
        ).toWireFormatRoundTrip().placeholder as RangedValueComplicationData

        assertThat(placeholderRangedValue.value).isEqualTo(RangedValueComplicationData.PLACEHOLDER)
        assertThat(placeholderRangedValue.text).isEqualTo(ComplicationText.PLACEHOLDER)
        assertThat(placeholderRangedValue.title).isEqualTo(ComplicationText.PLACEHOLDER)
        assertThat(placeholderRangedValue.monochromaticImage)
            .isEqualTo(MonochromaticImage.PLACEHOLDER)
        assertThat(placeholderRangedValue.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("description")
        assertThat(placeholderRangedValue.hasPlaceholderFields()).isTrue()
    }

    @Test
    fun normal_rangedValue() {
        val placeholderRangedValue = NoDataComplicationData(
            RangedValueComplicationData.Builder(value = 7f, min = 1f, max = 10f, contentDescription)
                .setText(text)
                .setTitle(title)
                .setMonochromaticImage(monochromaticImage)
                .build()
        ).toWireFormatRoundTrip().placeholder as RangedValueComplicationData

        assertThat(placeholderRangedValue.text!!.getTextAt(resources, Instant.EPOCH)).isEqualTo(
            text.getTextAt(resources, Instant.EPOCH)
        )
        assertThat(placeholderRangedValue.title!!.getTextAt(resources, Instant.EPOCH)).isEqualTo(
            title.getTextAt(resources, Instant.EPOCH)
        )
        assertThat(placeholderRangedValue.monochromaticImage).isEqualTo(monochromaticImage)
        assertThat(placeholderRangedValue.value).isEqualTo(7f)
        assertThat(placeholderRangedValue.min).isEqualTo(1f)
        assertThat(placeholderRangedValue.max).isEqualTo(10f)
        assertThat(placeholderRangedValue.hasPlaceholderFields()).isFalse()
    }

    @Test
    fun titleAbsent_rangedValue() {
        val placeholderRangedValue = NoDataComplicationData(
            RangedValueComplicationData.Builder(
                value = RangedValueComplicationData.PLACEHOLDER,
                min = 1f,
                max = 10f,
                contentDescription
            )
                .setText(ComplicationText.PLACEHOLDER)
                .build()
        ).toWireFormatRoundTrip().placeholder as RangedValueComplicationData

        assertThat(placeholderRangedValue.text).isEqualTo(ComplicationText.PLACEHOLDER)
        assertThat(placeholderRangedValue.title).isNull()
        assertThat(placeholderRangedValue.monochromaticImage).isNull()
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    fun placeholder_goalProgress() {
        val placeholderGoalProgress = NoDataComplicationData(
            GoalProgressComplicationData.Builder(
                value = GoalProgressComplicationData.PLACEHOLDER,
                targetValue = 10000f,
                contentDescription
            )
                .setText(ComplicationText.PLACEHOLDER)
                .setTitle(ComplicationText.PLACEHOLDER)
                .setMonochromaticImage(MonochromaticImage.PLACEHOLDER)
                .build()
        ).toWireFormatRoundTrip().placeholder as GoalProgressComplicationData

        assertThat(placeholderGoalProgress.value).isEqualTo(RangedValueComplicationData.PLACEHOLDER)
        assertThat(placeholderGoalProgress.text).isEqualTo(ComplicationText.PLACEHOLDER)
        assertThat(placeholderGoalProgress.title).isEqualTo(ComplicationText.PLACEHOLDER)
        assertThat(placeholderGoalProgress.monochromaticImage)
            .isEqualTo(MonochromaticImage.PLACEHOLDER)
        assertThat(placeholderGoalProgress.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("description")
        assertThat(placeholderGoalProgress.hasPlaceholderFields()).isTrue()
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    fun normal_goalProgress() {
        val placeholderGoalProgress = NoDataComplicationData(
            GoalProgressComplicationData.Builder(
                value = 1200f,
                targetValue = 10000f,
                contentDescription
            )
                .setText(text)
                .setTitle(title)
                .setMonochromaticImage(monochromaticImage)
                .build()
        ).toWireFormatRoundTrip().placeholder as GoalProgressComplicationData

        assertThat(placeholderGoalProgress.text!!.getTextAt(resources, Instant.EPOCH)).isEqualTo(
            text.getTextAt(resources, Instant.EPOCH)
        )
        assertThat(placeholderGoalProgress.title!!.getTextAt(resources, Instant.EPOCH)).isEqualTo(
            title.getTextAt(resources, Instant.EPOCH)
        )
        assertThat(placeholderGoalProgress.monochromaticImage).isEqualTo(monochromaticImage)
        assertThat(placeholderGoalProgress.value).isEqualTo(1200f)
        assertThat(placeholderGoalProgress.targetValue).isEqualTo(10000f)
        assertThat(placeholderGoalProgress.hasPlaceholderFields()).isFalse()
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    fun placeholder_discreteRangedValue() {
        val placeholderRangedValue = NoDataComplicationData(
            DiscreteRangedValueComplicationData.Builder(
                value = DiscreteRangedValueComplicationData.PLACEHOLDER,
                min = 1,
                max = 10,
                contentDescription
            )
                .setText(ComplicationText.PLACEHOLDER)
                .setTitle(ComplicationText.PLACEHOLDER)
                .setMonochromaticImage(MonochromaticImage.PLACEHOLDER)
                .build()
        ).toWireFormatRoundTrip().placeholder as DiscreteRangedValueComplicationData

        assertThat(placeholderRangedValue.value)
            .isEqualTo(DiscreteRangedValueComplicationData.PLACEHOLDER)
        assertThat(placeholderRangedValue.text).isEqualTo(ComplicationText.PLACEHOLDER)
        assertThat(placeholderRangedValue.title).isEqualTo(ComplicationText.PLACEHOLDER)
        assertThat(placeholderRangedValue.monochromaticImage)
            .isEqualTo(MonochromaticImage.PLACEHOLDER)
        assertThat(placeholderRangedValue.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("description")
        assertThat(placeholderRangedValue.hasPlaceholderFields()).isTrue()
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    fun normal_discreteRangedValue() {
        val placeholderRangedValue = NoDataComplicationData(
            DiscreteRangedValueComplicationData.Builder(
                value = 7,
                min = 1,
                max = 10,
                contentDescription
            )
                .setText(text)
                .setTitle(title)
                .setMonochromaticImage(monochromaticImage)
                .build()
        ).toWireFormatRoundTrip().placeholder as DiscreteRangedValueComplicationData

        assertThat(placeholderRangedValue.text!!.getTextAt(resources, Instant.EPOCH)).isEqualTo(
            text.getTextAt(resources, Instant.EPOCH)
        )
        assertThat(placeholderRangedValue.title!!.getTextAt(resources, Instant.EPOCH)).isEqualTo(
            title.getTextAt(resources, Instant.EPOCH)
        )
        assertThat(placeholderRangedValue.monochromaticImage).isEqualTo(monochromaticImage)
        assertThat(placeholderRangedValue.value).isEqualTo(7)
        assertThat(placeholderRangedValue.min).isEqualTo(1)
        assertThat(placeholderRangedValue.max).isEqualTo(10)
        assertThat(placeholderRangedValue.hasPlaceholderFields()).isFalse()
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    fun titleAbsent_discreteRangedValue() {
        val placeholderRangedValue = NoDataComplicationData(
            DiscreteRangedValueComplicationData.Builder(
                value = DiscreteRangedValueComplicationData.PLACEHOLDER,
                min = 1,
                max = 10,
                contentDescription
            )
                .setText(ComplicationText.PLACEHOLDER)
                .build()
        ).toWireFormatRoundTrip().placeholder as DiscreteRangedValueComplicationData

        assertThat(placeholderRangedValue.text).isEqualTo(ComplicationText.PLACEHOLDER)
        assertThat(placeholderRangedValue.title).isNull()
        assertThat(placeholderRangedValue.monochromaticImage).isNull()
    }

    @Test
    fun placeholder_monochromaticImage() {
        val placeholderMonochromaticImage = NoDataComplicationData(
            MonochromaticImageComplicationData.Builder(
                MonochromaticImage.PLACEHOLDER,
                contentDescription
            ).build()
        ).toWireFormatRoundTrip().placeholder as MonochromaticImageComplicationData

        assertThat(placeholderMonochromaticImage.monochromaticImage)
            .isEqualTo(MonochromaticImage.PLACEHOLDER)
        assertThat(
            placeholderMonochromaticImage.contentDescription!!.getTextAt(resources, Instant.EPOCH)
        ).isEqualTo("description")
        assertThat(placeholderMonochromaticImage.hasPlaceholderFields()).isTrue()
    }

    @Test
    fun normal_monochromaticImage() {
        val placeholderMonochromaticImage = NoDataComplicationData(
            MonochromaticImageComplicationData.Builder(
                monochromaticImage,
                contentDescription
            ).build()
        ).toWireFormatRoundTrip().placeholder as MonochromaticImageComplicationData

        assertThat(placeholderMonochromaticImage.monochromaticImage).isEqualTo(monochromaticImage)
        assertThat(placeholderMonochromaticImage.hasPlaceholderFields()).isFalse()
    }

    @Test
    fun placeholder_smallImage() {
        val placeholderSmallImage = NoDataComplicationData(
            SmallImageComplicationData.Builder(SmallImage.PLACEHOLDER, contentDescription).build()
        ).toWireFormatRoundTrip().placeholder as SmallImageComplicationData

        assertThat(placeholderSmallImage.smallImage).isEqualTo(SmallImage.PLACEHOLDER)
        assertThat(placeholderSmallImage.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("description")
        assertThat(placeholderSmallImage.hasPlaceholderFields()).isTrue()
    }

    @Test
    fun normal_smallImage() {
        val placeholderSmallImage = NoDataComplicationData(
            SmallImageComplicationData.Builder(smallImage, contentDescription).build()
        ).toWireFormatRoundTrip().placeholder as SmallImageComplicationData

        assertThat(placeholderSmallImage.smallImage).isEqualTo(smallImage)
        assertThat(placeholderSmallImage.hasPlaceholderFields()).isFalse()
    }

    @Test
    fun placeholder_photoImage() {
        val placeholderPhotoImage = NoDataComplicationData(
            PhotoImageComplicationData.Builder(
                PhotoImageComplicationData.PLACEHOLDER,
                contentDescription
            ).build()
        ).toWireFormatRoundTrip().placeholder as PhotoImageComplicationData

        assertThat(placeholderPhotoImage.photoImage)
            .isEqualTo(PhotoImageComplicationData.PLACEHOLDER)
        assertThat(placeholderPhotoImage.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("description")
        assertThat(placeholderPhotoImage.hasPlaceholderFields()).isTrue()
    }

    @Test
    fun normal_photoImage() {
        val placeholderPhotoImage = NoDataComplicationData(
            PhotoImageComplicationData.Builder(icon, contentDescription).build()
        ).toWireFormatRoundTrip().placeholder as PhotoImageComplicationData

        assertThat(placeholderPhotoImage.photoImage).isEqualTo(icon)
        assertThat(placeholderPhotoImage.hasPlaceholderFields()).isFalse()
    }

    @Test
    fun wireLongTextWithPlaceholder_toApi() {
        val timelineEntry =
            ComplicationData.Builder(ComplicationData.TYPE_NO_DATA)
                .setPlaceholder(
                    ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                        .setLongText(ComplicationText.PLACEHOLDER.toWireComplicationText())
                        .build()
                )
                .build()
        timelineEntry.timelineStartEpochSecond = 100
        timelineEntry.timelineEndEpochSecond = 1000

        val wireLongTextComplication = WireComplicationDataBuilder(
            ComplicationType.LONG_TEXT.toWireComplicationType()
        )
            .setEndDateTimeMillis(1650988800000)
            .setDataSource(ComponentName("a", "b"))
            .setLongText(
                android.support.wearable.complications.ComplicationText.plainText("longText")
            )
            .setIcon(icon)
            .setSmallImageStyle(IMAGE_STYLE_ICON)
            .setContentDescription(
                android.support.wearable.complications.ComplicationText.plainText("test")
            )
            .build()
        wireLongTextComplication.setTimelineEntryCollection(listOf(timelineEntry))

        val apiLongTextComplicationData = wireLongTextComplication.toApiComplicationData()

        assertThat(apiLongTextComplicationData.type).isEqualTo(ComplicationType.LONG_TEXT)
        apiLongTextComplicationData as LongTextComplicationData
        assertThat(apiLongTextComplicationData.text.isPlaceholder()).isFalse()

        val noDataComplicationData =
            apiLongTextComplicationData.asWireComplicationData().timelineEntries!!.first()
                .toApiComplicationData()

        assertThat(noDataComplicationData.type).isEqualTo(ComplicationType.NO_DATA)
        noDataComplicationData as NoDataComplicationData

        val placeholder = noDataComplicationData.placeholder!!
        assertThat(placeholder.type).isEqualTo(ComplicationType.LONG_TEXT)

        placeholder as LongTextComplicationData
        assertThat(placeholder.text.isPlaceholder()).isTrue()
    }
}

fun NoDataComplicationData.toWireFormatRoundTrip() =
    asWireComplicationData().toApiComplicationData() as NoDataComplicationData