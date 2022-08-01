/*
 * Copyright 2021 The Android Open Source Project
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

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.Instant
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLog

@RunWith(SharedRobolectricTestRunner::class)
public class AsWireComplicationDataTest {
    val resources = ApplicationProvider.getApplicationContext<Context>().resources
    val dataSourceA = ComponentName("com.pkg_a", "com.a")
    val dataSourceB = ComponentName("com.pkg_a", "com.a")

    @Before
    fun setup() {
        ShadowLog.setLoggable("ComplicationData", Log.DEBUG)
    }

    @Test
    public fun noDataComplicationData() {
        val data = NoDataComplicationData()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(null)
                    .build()
            )
        testRoundTripConversions(data)
        assertThat(serializeAndDeserialize(data)).isInstanceOf(NoDataComplicationData::class.java)

        assertThat(data).isEqualTo(NoDataComplicationData())
        assertThat(data.hashCode()).isEqualTo(NoDataComplicationData().hashCode())
        assertThat(data.toString()).isEqualTo(
            "NoDataComplicationData(placeholder=null, " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=TimeRange(" +
                "startDateTimeMillis=-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z))"
        )
    }

    @Test
    public fun emptyComplicationData() {
        val data = EmptyComplicationData()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_EMPTY).build()
            )
        testRoundTripConversions(data)
        assertThat(serializeAndDeserialize(data)).isInstanceOf(EmptyComplicationData::class.java)

        assertThat(data).isEqualTo(EmptyComplicationData())
        assertThat(data.hashCode()).isEqualTo(EmptyComplicationData().hashCode())
        assertThat(data.toString()).isEqualTo("EmptyComplicationData()")
    }

    @Test
    public fun notConfiguredComplicationData() {
        val data = NotConfiguredComplicationData()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NOT_CONFIGURED).build()
            )
        testRoundTripConversions(data)
        assertThat(serializeAndDeserialize(data))
            .isInstanceOf(NotConfiguredComplicationData::class.java)

        assertThat(data).isEqualTo(NotConfiguredComplicationData())
        assertThat(data.hashCode()).isEqualTo(NotConfiguredComplicationData().hashCode())
        assertThat(data.toString()).isEqualTo("NotConfiguredComplicationData()")
    }

    @Test
    public fun shortTextComplicationData() {
        val data = ShortTextComplicationData.Builder(
            "text".complicationText,
            "content description".complicationText
        )
            .setTitle("title".complicationText)
            .setDataSource(dataSourceA)
            .build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_SHORT_TEXT)
                    .setShortText(WireComplicationText.plainText("text"))
                    .setShortTitle(WireComplicationText.plainText("title"))
                    .setContentDescription(WireComplicationText.plainText("content description"))
                    .setDataSource(dataSourceA)
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as ShortTextComplicationData
        assertThat(deserialized.text.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("text")
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")
        assertThat(deserialized.title!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("title")

        val data2 = ShortTextComplicationData.Builder(
            "text".complicationText,
            "content description".complicationText
        )
            .setTitle("title".complicationText)
            .setDataSource(dataSourceA)
            .build()
        val data3 = ShortTextComplicationData.Builder(
            "text3".complicationText,
            "content description3".complicationText
        )
            .setTitle("title3".complicationText)
            .setDataSource(dataSourceB)
            .build()

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "ShortTextComplicationData(text=ComplicationText{mSurroundingText=text, " +
                "mTimeDependentText=null}, title=ComplicationText{mSurroundingText=title, " +
                "mTimeDependentText=null}, monochromaticImage=null, contentDescription=" +
                "ComplicationText{mSurroundingText=content description, mTimeDependentText=null}," +
                " tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=" +
                "TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, " +
                "endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z), dataSource=" +
                "ComponentInfo{com.pkg_a/com.a})"
        )
    }

    @Test
    public fun longTextComplicationData() {
        val data = LongTextComplicationData.Builder(
            "text".complicationText,
            "content description".complicationText
        )
            .setTitle("title".complicationText)
            .setDataSource(dataSourceA)
            .build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_LONG_TEXT)
                    .setLongText(WireComplicationText.plainText("text"))
                    .setLongTitle(WireComplicationText.plainText("title"))
                    .setContentDescription(WireComplicationText.plainText("content description"))
                    .setDataSource(dataSourceA)
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as LongTextComplicationData
        assertThat(deserialized.text.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("text")
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")
        assertThat(deserialized.title!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("title")

        val data2 = LongTextComplicationData.Builder(
            "text".complicationText,
            "content description".complicationText
        )
            .setTitle("title".complicationText)
            .setDataSource(dataSourceA)
            .build()
        val data3 = LongTextComplicationData.Builder(
            "text3".complicationText,
            "content description3".complicationText
        )
            .setTitle("title3".complicationText)
            .setDataSource(dataSourceB)
            .build()

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "LongTextComplicationData(text=ComplicationText{mSurroundingText=text, " +
                "mTimeDependentText=null}, title=ComplicationText{mSurroundingText=title, " +
                "mTimeDependentText=null}, monochromaticImage=null, smallImage=null, " +
                "contentDescription=ComplicationText{mSurroundingText=content description, " +
                "mTimeDependentText=null}), tapActionLostDueToSerialization=false, " +
                "tapAction=null, validTimeRange=TimeRange(startDateTimeMillis=" +
                "-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z), dataSource=ComponentInfo{com.pkg_a/com.a})"
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun rangedValueComplicationData() {
        val data = RangedValueComplicationData.Builder(
            value = 95f, min = 0f, max = 100f,
            contentDescription = "content description".complicationText
        )
            .setTitle("battery".complicationText)
            .setDataSource(dataSourceA)
            .build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_RANGED_VALUE)
                    .setRangedValue(95f)
                    .setRangedMinValue(0f)
                    .setRangedMaxValue(100f)
                    .setShortTitle(WireComplicationText.plainText("battery"))
                    .setContentDescription(WireComplicationText.plainText("content description"))
                    .setDataSource(dataSourceA)
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as RangedValueComplicationData
        assertThat(deserialized.max).isEqualTo(100f)
        assertThat(deserialized.min).isEqualTo(0f)
        assertThat(deserialized.value).isEqualTo(95f)
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")
        assertThat(deserialized.title!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("battery")

        val data2 = RangedValueComplicationData.Builder(
            value = 95f, min = 0f, max = 100f,
            contentDescription = "content description".complicationText
        )
            .setTitle("battery".complicationText)
            .setDataSource(dataSourceA)
            .build()

        val data3 = RangedValueComplicationData.Builder(
            value = 95f, min = 0f, max = 100f,
            contentDescription = "content description2".complicationText
        )
            .setTitle("battery2".complicationText)
            .setDataSource(dataSourceB)
            .build()

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "RangedValueComplicationData(value=95.0, min=0.0, max=100.0, " +
                "monochromaticImage=null, title=ComplicationText{mSurroundingText=battery, " +
                "mTimeDependentText=null}, text=null, contentDescription=ComplicationText" +
                "{mSurroundingText=content description, mTimeDependentText=null}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=" +
                "TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z), dataSource=" +
                "ComponentInfo{com.pkg_a/com.a}, colorRamp=null)"
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun goalProgressComplicationData_with_ColorRamp() {
        val data = GoalProgressComplicationData.Builder(
            value = 1200f, targetValue = 10000f,
            contentDescription = "content description".complicationText
        )
            .setTitle("steps".complicationText)
            .setDataSource(dataSourceA)
            .setColorRamp(ColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE), true))
            .build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_GOAL_PROGRESS)
                    .setRangedValue(1200f)
                    .setTargetValue(10000f)
                    .setShortTitle(WireComplicationText.plainText("steps"))
                    .setContentDescription(WireComplicationText.plainText("content description"))
                    .setDataSource(dataSourceA)
                    .setColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE))
                    .setColorRampIsSmoothShaded(true)
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as GoalProgressComplicationData
        assertThat(deserialized.value).isEqualTo(1200f)
        assertThat(deserialized.targetValue).isEqualTo(10000f)
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")
        assertThat(deserialized.title!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("steps")

        val data2 = GoalProgressComplicationData.Builder(
            value = 1200f, targetValue = 10000f,
            contentDescription = "content description".complicationText
        )
            .setTitle("steps".complicationText)
            .setDataSource(dataSourceA)
            .setColorRamp(ColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE), true))
            .build()

        val data3 = GoalProgressComplicationData.Builder(
            value = 1200f, targetValue = 10000f,
            contentDescription = "content description".complicationText
        )
            .setTitle("steps".complicationText)
            .setDataSource(dataSourceB)
            .setColorRamp(ColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE), false))
            .build()

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "GoalProgressComplicationData(value=1200.0, targetValue=10000.0, " +
                "monochromaticImage=null, title=ComplicationText{mSurroundingText=steps, " +
                "mTimeDependentText=null}, text=null, contentDescription=ComplicationText" +
                "{mSurroundingText=content description, mTimeDependentText=null}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=" +
                "TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z), dataSource=" +
                "ComponentInfo{com.pkg_a/com.a}, colorRamp=ColorRamp(colors=[-65536, -16711936, " +
                "-16776961], interpolated=true))"
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun discreteRangedValueComplicationData() {
        val data = DiscreteRangedValueComplicationData.Builder(
            value = 95, min = 0, max = 100,
            contentDescription = "content description".complicationText
        )
            .setTitle("battery".complicationText)
            .setDataSource(dataSourceA)
            .build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_DISCRETE_RANGED_VALUE)
                    .setDiscreteRangedValue(95)
                    .setDiscreteRangedMinValue(0)
                    .setDiscreteRangedMaxValue(100)
                    .setShortTitle(WireComplicationText.plainText("battery"))
                    .setContentDescription(WireComplicationText.plainText("content description"))
                    .setDataSource(dataSourceA)
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as DiscreteRangedValueComplicationData
        assertThat(deserialized.max).isEqualTo(100)
        assertThat(deserialized.min).isEqualTo(0)
        assertThat(deserialized.value).isEqualTo(95)
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")
        assertThat(deserialized.title!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("battery")

        val data2 = DiscreteRangedValueComplicationData.Builder(
            value = 95, min = 0, max = 100,
            contentDescription = "content description".complicationText
        )
            .setTitle("battery".complicationText)
            .setDataSource(dataSourceA)
            .build()

        val data3 = DiscreteRangedValueComplicationData.Builder(
            value = 95, min = 0, max = 100,
            contentDescription = "content description2".complicationText
        )
            .setTitle("battery2".complicationText)
            .setDataSource(dataSourceB)
            .build()

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "DiscreteRangedValueComplicationData(value=95, min=0, max=100, " +
                "monochromaticImage=null, title=ComplicationText{mSurroundingText=battery, " +
                "mTimeDependentText=null}, text=null, contentDescription=ComplicationText" +
                "{mSurroundingText=content description, mTimeDependentText=null}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=" +
                "TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z), dataSource=ComponentInfo{com.pkg_a/com.a})"
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun rangedValueComplicationData_with_ColorRamp() {
        val data = RangedValueComplicationData.Builder(
            value = 95f, min = 0f, max = 100f,
            contentDescription = "content description".complicationText
        )
            .setTitle("battery".complicationText)
            .setDataSource(dataSourceA)
            .setColorRamp(ColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE), true))
            .build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_RANGED_VALUE)
                    .setRangedValue(95f)
                    .setRangedMinValue(0f)
                    .setRangedMaxValue(100f)
                    .setShortTitle(WireComplicationText.plainText("battery"))
                    .setContentDescription(WireComplicationText.plainText("content description"))
                    .setDataSource(dataSourceA)
                    .setColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE))
                    .setColorRampIsSmoothShaded(true)
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as RangedValueComplicationData
        assertThat(deserialized.max).isEqualTo(100f)
        assertThat(deserialized.min).isEqualTo(0f)
        assertThat(deserialized.value).isEqualTo(95f)
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")
        assertThat(deserialized.title!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("battery")

        val data2 = RangedValueComplicationData.Builder(
            value = 95f, min = 0f, max = 100f,
            contentDescription = "content description".complicationText
        )
            .setTitle("battery".complicationText)
            .setDataSource(dataSourceA)
            .setColorRamp(ColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE), true))
            .build()

        val data3 = RangedValueComplicationData.Builder(
            value = 95f, min = 0f, max = 100f,
            contentDescription = "content description2".complicationText
        )
            .setTitle("battery".complicationText)
            .setDataSource(dataSourceB)
            .setColorRamp(ColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.YELLOW), true))
            .build()

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "RangedValueComplicationData(value=95.0, min=0.0, max=100.0, " +
                "monochromaticImage=null, title=ComplicationText{mSurroundingText=battery, " +
                "mTimeDependentText=null}, text=null, contentDescription=ComplicationText" +
                "{mSurroundingText=content description, mTimeDependentText=null}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=" +
                "TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z), dataSource=" +
                "ComponentInfo{com.pkg_a/com.a}, colorRamp=ColorRamp(colors=[-65536, -16711936, " +
                "-16776961], interpolated=true))"
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun weightedElementsComplicationData() {
        val data = WeightedElementsComplicationData.Builder(
            listOf(
                WeightedElementsComplicationData.Element(0.5f, Color.RED),
                WeightedElementsComplicationData.Element(1f, Color.GREEN),
                WeightedElementsComplicationData.Element(2f, Color.BLUE),
            ),
            contentDescription = "content description".complicationText
        )
            .setTitle("calories".complicationText)
            .setDataSource(dataSourceA)
            .build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_WEIGHTED_ELEMENTS)
                    .setElementWeights(floatArrayOf(0.5f, 1f, 2f))
                    .setElementColors(intArrayOf(Color.RED, Color.GREEN, Color.BLUE))
                    .setShortTitle(WireComplicationText.plainText("calories"))
                    .setContentDescription(WireComplicationText.plainText("content description"))
                    .setDataSource(dataSourceA)
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as WeightedElementsComplicationData
        assertThat(deserialized.elements).isEqualTo(
            listOf(
                WeightedElementsComplicationData.Element(0.5f, Color.RED),
                WeightedElementsComplicationData.Element(1f, Color.GREEN),
                WeightedElementsComplicationData.Element(2f, Color.BLUE),
            )
        )
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")
        assertThat(deserialized.title!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("calories")

        val data2 = WeightedElementsComplicationData.Builder(
            listOf(
                WeightedElementsComplicationData.Element(0.5f, Color.RED),
                WeightedElementsComplicationData.Element(1f, Color.GREEN),
                WeightedElementsComplicationData.Element(2f, Color.BLUE),
            ),
            contentDescription = "content description".complicationText
        )
            .setTitle("calories".complicationText)
            .setDataSource(dataSourceA)
            .build()

        val data3 = WeightedElementsComplicationData.Builder(
            listOf(
                WeightedElementsComplicationData.Element(0.5f, Color.RED),
                WeightedElementsComplicationData.Element(10f, Color.GREEN),
                WeightedElementsComplicationData.Element(2f, Color.BLUE),
            ),
            contentDescription = "content description".complicationText
        )
            .setTitle("battery".complicationText)
            .setDataSource(dataSourceA)
            .build()

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "WeightedElementsComplicationData(elements=Element(color=-65536, weight=0.5)," +
                " Element(color=-16711936, weight=1.0), Element(color=-16776961, weight=2.0), " +
                "monochromaticImage=null, title=ComplicationText{mSurroundingText=calories, " +
                "mTimeDependentText=null}, text=null, contentDescription=ComplicationText{" +
                "mSurroundingText=content description, mTimeDependentText=null}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=TimeRange(" +
                "startDateTimeMillis=-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z), dataSource=ComponentInfo{com.pkg_a/com.a})"
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Test
    public fun monochromaticImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        val image = MonochromaticImage.Builder(icon).build()
        val data = MonochromaticImageComplicationData.Builder(
            image, "content description".complicationText
        ).setDataSource(dataSourceA).build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_ICON)
                    .setIcon(icon)
                    .setContentDescription(WireComplicationText.plainText("content description"))
                    .setDataSource(dataSourceA)
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as MonochromaticImageComplicationData
        assertThat(deserialized.monochromaticImage.image.uri.toString())
            .isEqualTo("someuri")
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")

        val icon2 = Icon.createWithContentUri("someuri")
        val image2 = MonochromaticImage.Builder(icon2).build()
        val data2 = MonochromaticImageComplicationData.Builder(
            image2, "content description".complicationText
        ).setDataSource(dataSourceA).build()

        val icon3 = Icon.createWithContentUri("someuri3")
        val image3 = MonochromaticImage.Builder(icon3).build()
        val data3 = MonochromaticImageComplicationData.Builder(
            image3, "content description".complicationText
        ).setDataSource(dataSourceB).build()

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "MonochromaticImageComplicationData(monochromaticImage=MonochromaticImage(image=" +
                "Icon(typ=URI uri=someuri), ambientImage=null), contentDescription=" +
                "ComplicationText{mSurroundingText=content description, mTimeDependentText=null})" +
                ", tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=" +
                "TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z), dataSource=ComponentInfo{com.pkg_a/com.a})"
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Test
    public fun smallImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        val image = SmallImage.Builder(icon, SmallImageType.PHOTO).build()
        val data = SmallImageComplicationData.Builder(
            image, "content description".complicationText
        ).setDataSource(dataSourceA).build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_SMALL_IMAGE)
                    .setSmallImage(icon)
                    .setSmallImageStyle(WireComplicationData.IMAGE_STYLE_PHOTO)
                    .setContentDescription(WireComplicationText.plainText("content description"))
                    .setDataSource(dataSourceA)
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as SmallImageComplicationData
        assertThat(deserialized.smallImage.image.uri.toString())
            .isEqualTo("someuri")
        assertThat(deserialized.smallImage.type).isEqualTo(SmallImageType.PHOTO)
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")

        val icon2 = Icon.createWithContentUri("someuri")
        val image2 = SmallImage.Builder(icon2, SmallImageType.PHOTO).build()
        val data2 = SmallImageComplicationData.Builder(
            image2, "content description".complicationText
        ).setDataSource(dataSourceA).build()

        val icon3 = Icon.createWithContentUri("someuri3")
        val image3 = SmallImage.Builder(icon3, SmallImageType.PHOTO).build()
        val data3 = SmallImageComplicationData.Builder(
            image3, "content description".complicationText
        ).setDataSource(dataSourceB).build()

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "SmallImageComplicationData(smallImage=SmallImage(image=Icon(typ=URI uri=someuri)" +
                ", type=PHOTO, ambientImage=null), contentDescription=ComplicationText{" +
                "mSurroundingText=content description, mTimeDependentText=null}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=" +
                "TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z), dataSource=ComponentInfo{com.pkg_a/com.a})"
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Test
    public fun backgroundImageComplicationData() {
        val photoImage = Icon.createWithContentUri("someuri")
        val data = PhotoImageComplicationData.Builder(
            photoImage, "content description".complicationText
        ).setDataSource(dataSourceA).build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_LARGE_IMAGE)
                    .setLargeImage(photoImage)
                    .setContentDescription(WireComplicationText.plainText("content description"))
                    .setDataSource(dataSourceA)
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as PhotoImageComplicationData
        assertThat(deserialized.photoImage.uri.toString())
            .isEqualTo("someuri")
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")

        val photoImage2 = Icon.createWithContentUri("someuri")
        val data2 = PhotoImageComplicationData.Builder(
            photoImage2, "content description".complicationText
        ).setDataSource(dataSourceA).build()

        val photoImage3 = Icon.createWithContentUri("someuri3")
        val data3 = PhotoImageComplicationData.Builder(
            photoImage3, "content description".complicationText
        ).setDataSource(dataSourceB).build()

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "PhotoImageComplicationData(photoImage=Icon(typ=URI uri=someuri), contentDescription=" +
                "ComplicationText{mSurroundingText=content description, mTimeDependentText=null})" +
                ", tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=" +
                "TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z), dataSource=ComponentInfo{com.pkg_a/com.a})"
        )
    }

    @Test
    public fun noPermissionComplicationData() {
        val data = NoPermissionComplicationData.Builder()
            .setText("needs location".complicationText)
            .setDataSource(dataSourceA)
            .build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_PERMISSION)
                    .setShortText(WireComplicationText.plainText("needs location"))
                    .setDataSource(dataSourceA)
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as NoPermissionComplicationData
        assertThat(deserialized.text!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("needs location")

        val data2 = NoPermissionComplicationData.Builder()
            .setText("needs location".complicationText)
            .setDataSource(dataSourceA)
            .build()

        val data3 = NoPermissionComplicationData.Builder()
            .setText("needs location3".complicationText)
            .setDataSource(dataSourceB)
            .build()

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "NoPermissionComplicationData(text=ComplicationText{mSurroundingText=needs location," +
                " mTimeDependentText=null}, title=null, monochromaticImage=null, " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=" +
                "TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z), dataSource=ComponentInfo{com.pkg_a/com.a})"
        )
    }

    @Test
    @OptIn(ComplicationExperimental::class)
    public fun listComplicationData() {
        val data = ListComplicationData.Builder(
            listOf(
                ShortTextComplicationData.Builder("text".complicationText, ComplicationText.EMPTY)
                    .build(),
                RangedValueComplicationData.Builder(
                    value = 95f,
                    min = 0f,
                    max = 100f,
                    ComplicationText.EMPTY
                )
                    .setText("battery".complicationText)
                    .build()
            ),
            ListComplicationData.StyleHint.RowOfColumns,
            ComplicationText.EMPTY
        )
            .setDataSource(dataSourceA).build()

        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_LIST)
                    .setListEntryCollection(
                        listOf(
                            WireComplicationDataBuilder(WireComplicationData.TYPE_SHORT_TEXT)
                                .setShortText(WireComplicationText.plainText("text"))
                                .build(),
                            WireComplicationDataBuilder(WireComplicationData.TYPE_RANGED_VALUE)
                                .setRangedValue(95f)
                                .setRangedMinValue(0f)
                                .setRangedMaxValue(100f)
                                .setShortText(WireComplicationText.plainText("battery"))
                                .build()
                        )
                    )
                    .setListStyleHint(ListComplicationData.StyleHint.RowOfColumns.ordinal)
                    .setDataSource(dataSourceA)
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as ListComplicationData
        assertThat(deserialized.complicationList).containsExactly(
            ShortTextComplicationData.Builder("text".complicationText, ComplicationText.EMPTY)
                .build(),
            RangedValueComplicationData.Builder(
                value = 95f,
                min = 0f,
                max = 100f,
                ComplicationText.EMPTY
            )
                .setText("battery".complicationText)
                .build()
        )
        assertThat(deserialized.styleHint).isEqualTo(ListComplicationData.StyleHint.RowOfColumns)

        val data2 = ListComplicationData.Builder(
            listOf(
                ShortTextComplicationData.Builder("text".complicationText, ComplicationText.EMPTY)
                    .build(),
                RangedValueComplicationData.Builder(
                    value = 95f,
                    min = 0f,
                    max = 100f,
                    ComplicationText.EMPTY
                )
                    .setText("battery".complicationText)
                    .build()
            ),
            ListComplicationData.StyleHint.RowOfColumns,
            ComplicationText.EMPTY
        ).setDataSource(dataSourceA).build()

        val data3 = ListComplicationData.Builder(
            listOf(
                ShortTextComplicationData.Builder("text2".complicationText, ComplicationText.EMPTY)
                    .build(),
                RangedValueComplicationData.Builder(
                    value = 95f,
                    min = 0f,
                    max = 100f,
                    ComplicationText.EMPTY
                )
                    .setText("battery".complicationText)
                    .build()
            ),
            ListComplicationData.StyleHint.RowOfColumns,
            ComplicationText.EMPTY
        ).setDataSource(dataSourceB).build()

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "ListComplicationData(complicationList=[ShortTextComplicationData(" +
                "text=ComplicationText{mSurroundingText=text, mTimeDependentText=null}, " +
                "title=null, monochromaticImage=null, contentDescription=ComplicationText{" +
                "mSurroundingText=, mTimeDependentText=null}, " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=TimeRange(" +
                "startDateTimeMillis=-1000000000-01-01T00:00:00Z, " +
                "endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z), dataSource=null), " +
                "RangedValueComplicationData(value=95.0, min=0.0, max=100.0, " +
                "monochromaticImage=null, title=null, text=ComplicationText{" +
                "mSurroundingText=battery, mTimeDependentText=null}, contentDescription=" +
                "ComplicationText{mSurroundingText=, mTimeDependentText=null}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=TimeRange(" +
                "startDateTimeMillis=-1000000000-01-01T00:00:00Z, " +
                "endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z), dataSource=null, " +
                "colorRamp=null)], contentDescription=ComplicationText{mSurroundingText=, " +
                "mTimeDependentText=null}, tapActionLostDueToSerialization=false, tapAction=null," +
                " validTimeRange=TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z," +
                " endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z), dataSource=" +
                "ComponentInfo{com.pkg_a/com.a}, " +
                "styleHint=ListComplicationLayoutStyleHint(wireType=1))"
        )
    }

    @Test
    public fun noDataComplicationData_shortText() {
        val data = NoDataComplicationData(
            ShortTextComplicationData.Builder(
                ComplicationText.PLACEHOLDER,
                "content description".complicationText
            )
                .setTitle(ComplicationText.PLACEHOLDER)
                .setMonochromaticImage(MonochromaticImage.PLACEHOLDER)
                .setDataSource(dataSourceA)
                .build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_SHORT_TEXT)
                            .setShortText(ComplicationText.PLACEHOLDER.toWireComplicationText())
                            .setShortTitle(ComplicationText.PLACEHOLDER.toWireComplicationText())
                            .setIcon(createPlaceholderIcon())
                            .setContentDescription(
                                WireComplicationText.plainText("content description")
                            )
                            .setDataSource(dataSourceA)
                            .build()
                    )
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as NoDataComplicationData
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")

        val data2 = NoDataComplicationData(
            ShortTextComplicationData.Builder(
                ComplicationText.PLACEHOLDER,
                "content description".complicationText
            )
                .setTitle(ComplicationText.PLACEHOLDER)
                .setMonochromaticImage(MonochromaticImage.PLACEHOLDER)
                .setDataSource(dataSourceA)
                .build()
        )
        val data3 = NoDataComplicationData(
            ShortTextComplicationData.Builder(
                ComplicationText.PLACEHOLDER,
                "content description".complicationText
            )
                .setDataSource(dataSourceB)
                .build()
        )
        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "NoDataComplicationData(placeholder=ShortTextComplicationData(text=" +
                "ComplicationText{mSurroundingText=__placeholder__, mTimeDependentText=null}, " +
                "title=ComplicationText{mSurroundingText=__placeholder__, mTimeDependentText" +
                "=null}, monochromaticImage=MonochromaticImage(image=Icon(typ=RESOURCE pkg= " +
                "id=0xffffffff), ambientImage=null), contentDescription=ComplicationText{" +
                "mSurroundingText=content description, mTimeDependentText=null}, " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=" +
                "TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, " +
                "endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z), " +
                "dataSource=ComponentInfo{com.pkg_a/com.a}), " +
                "tapActionLostDueToSerialization=false, tapAction=null," +
                " validTimeRange=TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, " +
                "endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z))"
        )
    }

    @Test
    public fun noDataComplicationData_longText() {
        val data = NoDataComplicationData(
            LongTextComplicationData.Builder(
                "text".complicationText,
                "content description".complicationText
            ).setDataSource(dataSourceA).build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_LONG_TEXT)
                            .setLongText(WireComplicationText.plainText("text"))
                            .setContentDescription(
                                WireComplicationText.plainText("content description")
                            )
                            .setDataSource(dataSourceA)
                            .build()
                    )
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as NoDataComplicationData
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")

        val data2 = NoDataComplicationData(
            LongTextComplicationData.Builder(
                "text".complicationText,
                "content description".complicationText
            ).setDataSource(dataSourceA).build()
        )
        val data3 = NoDataComplicationData(
            LongTextComplicationData.Builder(
                ComplicationText.PLACEHOLDER,
                "content description".complicationText
            ).setDataSource(dataSourceB).build()
        )

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "NoDataComplicationData(placeholder=LongTextComplicationData(text=" +
                "ComplicationText{mSurroundingText=text, mTimeDependentText=null}, title=null, " +
                "monochromaticImage=null, smallImage=null, contentDescription=ComplicationText{" +
                "mSurroundingText=content description, mTimeDependentText=null}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=TimeRange(" +
                "startDateTimeMillis=-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z), " +
                "dataSource=ComponentInfo{com.pkg_a/com.a}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=TimeRange(" +
                "startDateTimeMillis=-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z))"
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun noDataComplicationData_rangedValue() {
        val data = NoDataComplicationData(
            RangedValueComplicationData.Builder(
                value = RangedValueComplicationData.PLACEHOLDER,
                min = 0f,
                max = 100f,
                "content description".complicationText
            )
                .setText(ComplicationText.PLACEHOLDER)
                .setDataSource(dataSourceA)
                .build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_RANGED_VALUE)
                            .setRangedValue(RangedValueComplicationData.PLACEHOLDER)
                            .setRangedMinValue(0f)
                            .setRangedMaxValue(100f)
                            .setShortText(ComplicationText.PLACEHOLDER.toWireComplicationText())
                            .setContentDescription(
                                WireComplicationText.plainText("content description")
                            )
                            .setDataSource(dataSourceA)
                            .build()
                    )
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as NoDataComplicationData
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")

        val data2 = NoDataComplicationData(
            RangedValueComplicationData.Builder(
                value = RangedValueComplicationData.PLACEHOLDER,
                min = 0f,
                max = 100f,
                "content description".complicationText
            )
                .setText(ComplicationText.PLACEHOLDER)
                .setDataSource(dataSourceA)
                .build()
        )
        val data3 = NoDataComplicationData(
            RangedValueComplicationData.Builder(
                value = RangedValueComplicationData.PLACEHOLDER,
                min = 0f,
                max = 100f,
                "content description".complicationText
            )
                .setText(ComplicationText.PLACEHOLDER)
                .build()
        )

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "NoDataComplicationData(placeholder=RangedValueComplicationData(" +
                "value=3.4028235E38, min=0.0, max=100.0, monochromaticImage=null, title=null, " +
                "text=ComplicationText{mSurroundingText=__placeholder__, mTimeDependentText=null" +
                "}, contentDescription=ComplicationText{mSurroundingText=content description, " +
                "mTimeDependentText=null}), tapActionLostDueToSerialization=false, tapAction=" +
                "null, validTimeRange=TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, " +
                "endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z)," +
                " dataSource=ComponentInfo{com.pkg_a/com.a}, " +
                "colorRamp=null), tapActionLostDueToSerialization=false, tapAction=null, " +
                "validTimeRange=TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, " +
                "endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z))"
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun noDataComplicationData_goalProgress() {
        val data = NoDataComplicationData(
            GoalProgressComplicationData.Builder(
                value = GoalProgressComplicationData.PLACEHOLDER,
                targetValue = 10000f,
                contentDescription = "content description".complicationText
            )
                .setText(ComplicationText.PLACEHOLDER)
                .setDataSource(dataSourceA)
                .setColorRamp(ColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE), false))
                .build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_GOAL_PROGRESS)
                            .setRangedValue(GoalProgressComplicationData.PLACEHOLDER)
                            .setTargetValue(10000f)
                            .setShortText(ComplicationText.PLACEHOLDER.toWireComplicationText())
                            .setContentDescription(
                                WireComplicationText.plainText("content description")
                            )
                            .setDataSource(dataSourceA)
                            .setColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE))
                            .setColorRampIsSmoothShaded(false)
                            .build()
                    )
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as NoDataComplicationData
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")

        val data2 = NoDataComplicationData(
            GoalProgressComplicationData.Builder(
                value = GoalProgressComplicationData.PLACEHOLDER,
                targetValue = 10000f,
                contentDescription = "content description".complicationText
            )
                .setText(ComplicationText.PLACEHOLDER)
                .setDataSource(dataSourceA)
                .setColorRamp(ColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE), false))
                .build()
        )
        val data3 = NoDataComplicationData(
            GoalProgressComplicationData.Builder(
                value = GoalProgressComplicationData.PLACEHOLDER,
                targetValue = 10000f,
                contentDescription = "content description".complicationText
            )
                .setText(ComplicationText.PLACEHOLDER)
                .build()
        )

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "NoDataComplicationData(placeholder=GoalProgressComplicationData(" +
                "value=3.4028235E38, targetValue=10000.0, monochromaticImage=null, title=null, " +
                "text=ComplicationText{mSurroundingText=__placeholder__, " +
                "mTimeDependentText=null}, contentDescription=ComplicationText{" +
                "mSurroundingText=content description, mTimeDependentText=null}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=TimeRange(" +
                "startDateTimeMillis=-1000000000-01-01T00:00:00Z, " +
                "endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z), " +
                "dataSource=ComponentInfo{com.pkg_a/com.a}, " +
                "colorRamp=ColorRamp(colors=[-65536, -16711936, -16776961], interpolated=false))," +
                " tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=" +
                "TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, " +
                "endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z))"
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun noDataComplicationData_discreteRangedValue() {
        val data = NoDataComplicationData(
            DiscreteRangedValueComplicationData.Builder(
                value = DiscreteRangedValueComplicationData.PLACEHOLDER,
                min = 0,
                max = 100,
                "content description".complicationText
            )
                .setText(ComplicationText.PLACEHOLDER)
                .setDataSource(dataSourceA)
                .build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_DISCRETE_RANGED_VALUE)
                            .setDiscreteRangedValue(DiscreteRangedValueComplicationData.PLACEHOLDER)
                            .setDiscreteRangedMinValue(0)
                            .setDiscreteRangedMaxValue(100)
                            .setShortText(ComplicationText.PLACEHOLDER.toWireComplicationText())
                            .setContentDescription(
                                WireComplicationText.plainText("content description")
                            )
                            .setDataSource(dataSourceA)
                            .build()
                    )
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as NoDataComplicationData
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")

        val data2 = NoDataComplicationData(
            DiscreteRangedValueComplicationData.Builder(
                value = DiscreteRangedValueComplicationData.PLACEHOLDER,
                min = 0,
                max = 100,
                "content description".complicationText
            )
                .setText(ComplicationText.PLACEHOLDER)
                .setDataSource(dataSourceA)
                .build()
        )
        val data3 = NoDataComplicationData(
            DiscreteRangedValueComplicationData.Builder(
                value = DiscreteRangedValueComplicationData.PLACEHOLDER,
                min = 0,
                max = 100,
                "content description".complicationText
            )
                .setTitle(ComplicationText.PLACEHOLDER)
                .setText(ComplicationText.PLACEHOLDER)
                .build()
        )

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "NoDataComplicationData(placeholder=DiscreteRangedValueComplicationData(" +
                "value=2147483647, min=0, max=100, monochromaticImage=null, title=null, " +
                "text=ComplicationText{mSurroundingText=__placeholder__, mTimeDependentText=null" +
                "}, contentDescription=ComplicationText{mSurroundingText=content description, " +
                "mTimeDependentText=null}), tapActionLostDueToSerialization=false, tapAction=" +
                "null, validTimeRange=TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, " +
                "endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z)," +
                " dataSource=ComponentInfo{com.pkg_a/com.a}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, " +
                "validTimeRange=TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, " +
                "endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z))"
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun noDataComplicationData_weightedElements() {
        val data = NoDataComplicationData(
            WeightedElementsComplicationData.Builder(
                listOf(
                    WeightedElementsComplicationData.Element(0.5f, Color.RED),
                    WeightedElementsComplicationData.Element(1f, Color.GREEN),
                    WeightedElementsComplicationData.Element(2f, Color.BLUE),
                ),
                contentDescription = "content description".complicationText
            )
                .setTitle("calories".complicationText)
                .setDataSource(dataSourceA)
                .build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_WEIGHTED_ELEMENTS)
                            .setElementWeights(floatArrayOf(0.5f, 1f, 2f))
                            .setElementColors(intArrayOf(Color.RED, Color.GREEN, Color.BLUE))
                            .setShortTitle(WireComplicationText.plainText("calories"))
                            .setContentDescription(
                                WireComplicationText.plainText("content description")
                            )
                            .setDataSource(dataSourceA)
                            .build()
                    )
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as NoDataComplicationData
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")

        val data2 = NoDataComplicationData(
            WeightedElementsComplicationData.Builder(
                listOf(
                    WeightedElementsComplicationData.Element(0.5f, Color.RED),
                    WeightedElementsComplicationData.Element(1f, Color.GREEN),
                    WeightedElementsComplicationData.Element(2f, Color.BLUE),
                ),
                contentDescription = "content description".complicationText
            )
                .setTitle("calories".complicationText)
                .setDataSource(dataSourceA)
                .build()
        )
        val data3 = NoDataComplicationData(
            WeightedElementsComplicationData.Builder(
                listOf(
                    WeightedElementsComplicationData.Element(0.5f, Color.RED),
                    WeightedElementsComplicationData.Element(1f, Color.GREEN),
                ),
                contentDescription = "content description".complicationText
            )
                .setTitle("calories".complicationText)
                .setDataSource(dataSourceA)
                .build()
        )

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "NoDataComplicationData(placeholder=WeightedElementsComplicationData(" +
                "elements=Element(color=-65536, weight=0.5), Element(color=-16711936, " +
                "weight=1.0), Element(color=-16776961, weight=2.0), monochromaticImage=null, " +
                "title=ComplicationText{mSurroundingText=calories, mTimeDependentText=null}, " +
                "text=null, contentDescription=ComplicationText{mSurroundingText=content " +
                "description, mTimeDependentText=null}), tapActionLostDueToSerialization=false, " +
                "tapAction=null, validTimeRange=TimeRange(startDateTimeMillis=" +
                "-1000000000-01-01T00:00:00Z, " +
                "endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z), " +
                "dataSource=ComponentInfo{com.pkg_a/com.a}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, " +
                "validTimeRange=TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, " +
                "endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z))"
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun noDataComplicationData_rangedValue_with_ColorRange() {
        val data = NoDataComplicationData(
            RangedValueComplicationData.Builder(
                value = RangedValueComplicationData.PLACEHOLDER,
                min = 0f,
                max = 100f,
                "content description".complicationText
            )
                .setText(ComplicationText.PLACEHOLDER)
                .setDataSource(dataSourceA)
                .setColorRamp(ColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE), true))
                .build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_RANGED_VALUE)
                            .setRangedValue(RangedValueComplicationData.PLACEHOLDER)
                            .setRangedMinValue(0f)
                            .setRangedMaxValue(100f)
                            .setShortText(ComplicationText.PLACEHOLDER.toWireComplicationText())
                            .setContentDescription(
                                WireComplicationText.plainText("content description")
                            )
                            .setDataSource(dataSourceA)
                            .setColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE))
                            .setColorRampIsSmoothShaded(true)
                            .build()
                    )
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as NoDataComplicationData
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")

        val data2 = NoDataComplicationData(
            RangedValueComplicationData.Builder(
                value = RangedValueComplicationData.PLACEHOLDER,
                min = 0f,
                max = 100f,
                "content description".complicationText
            )
                .setText(ComplicationText.PLACEHOLDER)
                .setDataSource(dataSourceA)
                .setColorRamp(ColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE), true))
                .build()
        )
        val data3 = NoDataComplicationData(
            RangedValueComplicationData.Builder(
                value = RangedValueComplicationData.PLACEHOLDER,
                min = 0f,
                max = 100f,
                "content description".complicationText
            )
                .setText(ComplicationText.PLACEHOLDER)
                .build()
        )

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "NoDataComplicationData(placeholder=RangedValueComplicationData(" +
                "value=3.4028235E38, min=0.0, max=100.0, monochromaticImage=null, title=null, " +
                "text=ComplicationText{mSurroundingText=__placeholder__, mTimeDependentText=null" +
                "}, contentDescription=ComplicationText{mSurroundingText=content description, " +
                "mTimeDependentText=null}), tapActionLostDueToSerialization=false, tapAction=" +
                "null, validTimeRange=TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, " +
                "endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z), " +
                "dataSource=ComponentInfo{com.pkg_a/com.a}, " +
                "colorRamp=ColorRamp(colors=[-65536, -16711936, -16776961], interpolated=true)), " +
                "tapActionLostDueToSerialization=false, tapAction=null, " +
                "validTimeRange=TimeRange(startDateTimeMillis=-1000000000-01-01T00:00:00Z, " +
                "endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z))"
        )
    }

    @Test
    public fun noDataComplicationData_monochromaticImage() {
        val data = NoDataComplicationData(
            MonochromaticImageComplicationData.Builder(
                MonochromaticImage.PLACEHOLDER,
                "content description".complicationText
            ).setDataSource(dataSourceA).build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_ICON)
                            .setIcon(createPlaceholderIcon())
                            .setContentDescription(
                                WireComplicationText.plainText("content description")
                            )
                            .setDataSource(dataSourceA)
                            .build()
                    )
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as NoDataComplicationData
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")

        val data2 = NoDataComplicationData(
            MonochromaticImageComplicationData.Builder(
                MonochromaticImage.PLACEHOLDER,
                "content description".complicationText
            ).setDataSource(dataSourceA).build()
        )

        val icon = Icon.createWithContentUri("someuri")
        val image = MonochromaticImage.Builder(icon).build()
        val data3 = NoDataComplicationData(
            MonochromaticImageComplicationData.Builder(
                image,
                "content description".complicationText
            ).setDataSource(dataSourceB).build()
        )

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "NoDataComplicationData(placeholder=MonochromaticImageComplicationData(" +
                "monochromaticImage=MonochromaticImage(image=Icon(typ=RESOURCE pkg= " +
                "id=0xffffffff), ambientImage=null), contentDescription=ComplicationText{" +
                "mSurroundingText=content description, mTimeDependentText=null}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=TimeRange(" +
                "startDateTimeMillis=-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z)," +
                " dataSource=ComponentInfo{com.pkg_a/com.a}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=TimeRange(" +
                "startDateTimeMillis=-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z))"
        )
    }

    @Test
    public fun noDataComplicationData_smallImage() {
        val data = NoDataComplicationData(
            SmallImageComplicationData.Builder(
                SmallImage.PLACEHOLDER,
                "content description".complicationText
            ).setDataSource(dataSourceA).build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_SMALL_IMAGE)
                            .setSmallImage(createPlaceholderIcon())
                            .setSmallImageStyle(WireComplicationData.IMAGE_STYLE_ICON)
                            .setContentDescription(
                                WireComplicationText.plainText("content description")
                            )
                            .setDataSource(dataSourceA)
                            .build()
                    )
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as NoDataComplicationData
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")

        val data2 = NoDataComplicationData(
            SmallImageComplicationData.Builder(
                SmallImage.PLACEHOLDER,
                "content description".complicationText
            ).setDataSource(dataSourceA).build()
        )

        val icon = Icon.createWithContentUri("someuri")
        val image = SmallImage.Builder(icon, SmallImageType.PHOTO).build()
        val data3 = NoDataComplicationData(
            SmallImageComplicationData.Builder(
                image,
                "content description".complicationText
            ).setDataSource(dataSourceA).build()
        )

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "NoDataComplicationData(placeholder=SmallImageComplicationData(smallImage=" +
                "SmallImage(image=Icon(typ=RESOURCE pkg= id=0xffffffff), type=ICON, " +
                "ambientImage=null), contentDescription=ComplicationText{mSurroundingText=" +
                "content description, mTimeDependentText=null}), tapActionLostDueToSerialization=" +
                "false, tapAction=null, validTimeRange=TimeRange(startDateTimeMillis=" +
                "-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z)," +
                " dataSource=ComponentInfo{com.pkg_a/com.a}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=TimeRange(" +
                "startDateTimeMillis=-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z))"
        )
    }

    @Test
    public fun noDataComplicationData_photoImage() {
        val data = NoDataComplicationData(
            PhotoImageComplicationData.Builder(
                PhotoImageComplicationData.PLACEHOLDER,
                "content description".complicationText
            ).setDataSource(dataSourceA).build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_LARGE_IMAGE)
                            .setLargeImage(createPlaceholderIcon())
                            .setContentDescription(
                                WireComplicationText.plainText("content description")
                            )
                            .setDataSource(dataSourceA)
                            .build()
                    )
                    .build()
            )
        testRoundTripConversions(data)
        val deserialized = serializeAndDeserialize(data) as NoDataComplicationData
        assertThat(deserialized.contentDescription!!.getTextAt(resources, Instant.EPOCH))
            .isEqualTo("content description")

        val data2 = NoDataComplicationData(
            PhotoImageComplicationData.Builder(
                PhotoImageComplicationData.PLACEHOLDER,
                "content description".complicationText
            ).setDataSource(dataSourceA).build()
        )

        val icon = Icon.createWithContentUri("someuri")
        val data3 = NoDataComplicationData(
            PhotoImageComplicationData.Builder(
                icon,
                "content description".complicationText
            ).setDataSource(dataSourceA).build()
        )

        assertThat(data).isEqualTo(data2)
        assertThat(data).isNotEqualTo(data3)
        assertThat(data.hashCode()).isEqualTo(data2.hashCode())
        assertThat(data.hashCode()).isNotEqualTo(data3.hashCode())
        assertThat(data.toString()).isEqualTo(
            "NoDataComplicationData(placeholder=PhotoImageComplicationData(photoImage=" +
                "Icon(typ=RESOURCE pkg= id=0xffffffff), contentDescription=ComplicationText{" +
                "mSurroundingText=content description, mTimeDependentText=null}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=TimeRange(" +
                "startDateTimeMillis=-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z)," +
                " dataSource=ComponentInfo{com.pkg_a/com.a}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=TimeRange(" +
                "startDateTimeMillis=-1000000000-01-01T00:00:00Z, endDateTimeMillis=" +
                "+1000000000-12-31T23:59:59.999999999Z))"
        )
    }

    private fun testRoundTripConversions(data: ComplicationData) {
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                data.asWireComplicationData().toApiComplicationData().asWireComplicationData()
            )
    }

    private fun serializeAndDeserialize(data: ComplicationData): ComplicationData {
        val stream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(stream)
        objectOutputStream.writeObject(data.asWireComplicationData())
        objectOutputStream.close()
        val byteArray = stream.toByteArray()

        val objectInputStream = ObjectInputStream(ByteArrayInputStream(byteArray))
        val wireData = objectInputStream.readObject() as WireComplicationData
        objectInputStream.close()
        return wireData.toApiComplicationData()
    }
}

@RunWith(SharedRobolectricTestRunner::class)
public class FromWireComplicationDataTest {
    @Test
    public fun noDataComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                .setPlaceholder(null).build(),
            ComplicationType.NO_DATA
        )
    }

    @Test
    public fun emptyComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_EMPTY).build(),
            ComplicationType.EMPTY
        )
    }

    @Test
    public fun notConfiguredComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_NOT_CONFIGURED).build(),
            ComplicationType.NOT_CONFIGURED
        )
    }

    @Test
    public fun shortTextComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_SHORT_TEXT)
                .setShortText(WireComplicationText.plainText("text"))
                .setShortTitle(WireComplicationText.plainText("title"))
                .setContentDescription(WireComplicationText.plainText("content description"))
                .build(),
            ComplicationType.SHORT_TEXT
        )
    }

    @Test
    public fun longTextComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_LONG_TEXT)
                .setLongText(WireComplicationText.plainText("text"))
                .setLongTitle(WireComplicationText.plainText("title"))
                .setContentDescription(WireComplicationText.plainText("content description"))
                .build(),
            ComplicationType.LONG_TEXT
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun rangedValueComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_RANGED_VALUE)
                .setRangedValue(95f)
                .setRangedMinValue(0f)
                .setRangedMaxValue(100f)
                .setShortTitle(WireComplicationText.plainText("battery"))
                .setContentDescription(WireComplicationText.plainText("content description"))
                .build(),
            ComplicationType.RANGED_VALUE
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun rangedValueComplicationData_drawSegmented() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_RANGED_VALUE)
                .setRangedValue(95f)
                .setRangedMinValue(0f)
                .setRangedMaxValue(100f)
                .setShortTitle(WireComplicationText.plainText("battery"))
                .setContentDescription(WireComplicationText.plainText("content description"))
                .build(),
            ComplicationType.RANGED_VALUE
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun goalProgressComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_GOAL_PROGRESS)
                .setRangedValue(1200f)
                .setTargetValue(10000f)
                .setShortTitle(WireComplicationText.plainText("steps"))
                .setContentDescription(WireComplicationText.plainText("content description"))
                .setColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE))
                .setColorRampIsSmoothShaded(false)
                .build(),
            ComplicationType.GOAL_PROGRESS
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun discreteRangedValueComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_DISCRETE_RANGED_VALUE)
                .setDiscreteRangedValue(95)
                .setDiscreteRangedMinValue(0)
                .setDiscreteRangedMaxValue(100)
                .setShortTitle(WireComplicationText.plainText("battery"))
                .setContentDescription(WireComplicationText.plainText("content description"))
                .build(),
            ComplicationType.DISCRETE_RANGED_VALUE
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun weightedElementsComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_WEIGHTED_ELEMENTS)
                .setElementWeights(floatArrayOf(0.5f, 1f, 2f))
                .setElementColors(intArrayOf(Color.RED, Color.GREEN, Color.BLUE))
                .setShortTitle(WireComplicationText.plainText("calories"))
                .setContentDescription(WireComplicationText.plainText("content description"))
                .build(),
            ComplicationType.WEIGHTED_ELEMENTS
        )
    }

    @Test
    public fun monochromaticImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_ICON)
                .setIcon(icon)
                .setContentDescription(WireComplicationText.plainText("content description"))
                .build(),
            ComplicationType.MONOCHROMATIC_IMAGE
        )
    }

    @Test
    public fun smallImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_SMALL_IMAGE)
                .setSmallImage(icon)
                .setSmallImageStyle(WireComplicationData.IMAGE_STYLE_PHOTO)
                .setContentDescription(WireComplicationText.plainText("content description"))
                .build(),
            ComplicationType.SMALL_IMAGE
        )
    }

    @Test
    public fun photoImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_LARGE_IMAGE)
                .setLargeImage(icon)
                .setContentDescription(WireComplicationText.plainText("content description"))
                .build(),
            ComplicationType.PHOTO_IMAGE
        )
    }

    @Test
    public fun noPermissionComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_NO_PERMISSION)
                .setShortText(WireComplicationText.plainText("needs location"))
                .build(),
            ComplicationType.NO_PERMISSION
        )
    }

    @Test
    @OptIn(ComplicationExperimental::class)
    public fun protoLayoutComplicationData() {
        val ambientLayout = ByteArray(1)
        val interactiveLayout = ByteArray(2)
        val resources = ByteArray(3)

        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_PROTO_LAYOUT)
                .setAmbientLayout(ambientLayout)
                .setInteractiveLayout(interactiveLayout)
                .setLayoutResources(resources)
                .build(),
            ComplicationType.PROTO_LAYOUT
        )
    }

    @Test
    @OptIn(ComplicationExperimental::class)
    public fun listComplicationData() {
        val wireShortText = WireComplicationDataBuilder(WireComplicationData.TYPE_SHORT_TEXT)
            .setShortText(WireComplicationText.plainText("text"))
            .build()

        val wireRangedValue = WireComplicationDataBuilder(WireComplicationData.TYPE_RANGED_VALUE)
            .setRangedValue(95f)
            .setRangedMinValue(0f)
            .setRangedMaxValue(100f)
            .setShortText(WireComplicationText.plainText("battery"))
            .build()

        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_LIST)
                .setListEntryCollection(listOf(wireShortText, wireRangedValue))
                .setListStyleHint(
                    ListComplicationData.StyleHint.RowOfColumns.ordinal
                )
                .build(),
            ComplicationType.LIST
        )
    }

    @Test
    public fun noDataComplicationData_shortText() {
        val icon = Icon.createWithContentUri("someuri")
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                .setPlaceholder(
                    WireComplicationDataBuilder(WireComplicationData.TYPE_SHORT_TEXT)
                        .setContentDescription(
                            WireComplicationText.plainText("content description")
                        )
                        .setShortText(WireComplicationText.plainText("text"))
                        .setShortTitle(WireComplicationText.plainText("title"))
                        .setIcon(icon)
                        .build()
                )
                .build(),
            ComplicationType.NO_DATA
        )
    }

    @Test
    public fun noDataComplicationData_longText() {
        val icon = Icon.createWithContentUri("someuri")
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                .setPlaceholder(
                    WireComplicationDataBuilder(WireComplicationData.TYPE_LONG_TEXT)
                        .setContentDescription(
                            WireComplicationText.plainText("content description")
                        )
                        .setLongText(WireComplicationText.plainText("text"))
                        .setLongTitle(WireComplicationText.plainText("title"))
                        .setIcon(icon)
                        .build()
                )
                .build(),
            ComplicationType.NO_DATA
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun noDataComplicationData_rangedValue() {
        val icon = Icon.createWithContentUri("someuri")
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                .setPlaceholder(
                    WireComplicationDataBuilder(WireComplicationData.TYPE_RANGED_VALUE)
                        .setContentDescription(
                            WireComplicationText.plainText("content description")
                        )
                        .setRangedValue(75f)
                        .setRangedMinValue(0f)
                        .setRangedMaxValue(100f)
                        .setShortTitle(WireComplicationText.plainText("battery"))
                        .setIcon(icon)
                        .build()
                )
                .build(),
            ComplicationType.NO_DATA
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun noDataComplicationData_rangedValue_drawSegmented() {
        val icon = Icon.createWithContentUri("someuri")
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                .setPlaceholder(
                    WireComplicationDataBuilder(WireComplicationData.TYPE_RANGED_VALUE)
                        .setContentDescription(
                            WireComplicationText.plainText("content description")
                        )
                        .setRangedValue(75f)
                        .setRangedMinValue(0f)
                        .setRangedMaxValue(100f)
                        .setShortTitle(WireComplicationText.plainText("battery"))
                        .setIcon(icon)
                        .build()
                )
                .build(),
            ComplicationType.NO_DATA
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun noDataComplicationData_goalProgress() {
        val icon = Icon.createWithContentUri("someuri")
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                .setPlaceholder(
                    WireComplicationDataBuilder(WireComplicationData.TYPE_GOAL_PROGRESS)
                        .setRangedValue(1200f)
                        .setTargetValue(10000f)
                        .setShortTitle(WireComplicationText.plainText("steps"))
                        .setContentDescription(
                            WireComplicationText.plainText("content description")
                        )
                        .setColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE))
                        .setColorRampIsSmoothShaded(true)
                        .setIcon(icon)
                        .build()
                )
                .build(),
            ComplicationType.NO_DATA
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun noDataComplicationData_discreteRangedValue() {
        val icon = Icon.createWithContentUri("someuri")
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                .setPlaceholder(
                    WireComplicationDataBuilder(WireComplicationData.TYPE_DISCRETE_RANGED_VALUE)
                        .setContentDescription(
                            WireComplicationText.plainText("content description")
                        )
                        .setDiscreteRangedValue(75)
                        .setDiscreteRangedMinValue(0)
                        .setDiscreteRangedMaxValue(100)
                        .setShortTitle(WireComplicationText.plainText("battery"))
                        .setIcon(icon)
                        .build()
                )
                .build(),
            ComplicationType.NO_DATA
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun noDataComplicationData_weightedElementsComplicationData() {
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                .setPlaceholder(
                    WireComplicationDataBuilder(WireComplicationData.TYPE_WEIGHTED_ELEMENTS)
                        .setElementWeights(floatArrayOf(0.5f, 1f, 2f))
                        .setElementColors(intArrayOf(Color.RED, Color.GREEN, Color.BLUE))
                        .setShortTitle(WireComplicationText.plainText("calories"))
                        .setContentDescription(
                            WireComplicationText.plainText("content description")
                        )
                        .build()
                ).build(),
            ComplicationType.NO_DATA
        )
    }

    @Test
    public fun noDataComplicationData_smallImage() {
        val icon = Icon.createWithContentUri("someuri")
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                .setPlaceholder(
                    WireComplicationDataBuilder(WireComplicationData.TYPE_SMALL_IMAGE)
                        .setSmallImage(icon)
                        .setSmallImageStyle(WireComplicationData.IMAGE_STYLE_PHOTO)
                        .setContentDescription(
                            WireComplicationText.plainText("content description")
                        )
                        .build()
                )
                .build(),
            ComplicationType.NO_DATA
        )
    }

    @Test
    public fun noDataComplicationData_monochromaticImage() {
        val icon = Icon.createWithContentUri("someuri")
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                .setPlaceholder(
                    WireComplicationDataBuilder(WireComplicationData.TYPE_ICON)
                        .setIcon(icon)
                        .setContentDescription(
                            WireComplicationText.plainText("content description")
                        )
                        .build()
                )
                .build(),
            ComplicationType.NO_DATA
        )
    }

    @Test
    public fun noDataComplicationData_protoLayout() {
        val ambientLayout = ByteArray(1)
        val interactiveLayout = ByteArray(2)
        val resources = ByteArray(3)
        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                .setPlaceholder(
                    WireComplicationDataBuilder(WireComplicationData.TYPE_PROTO_LAYOUT)
                        .setAmbientLayout(ambientLayout)
                        .setInteractiveLayout(interactiveLayout)
                        .setLayoutResources(resources)
                        .build()
                )
                .build(),
            ComplicationType.NO_DATA
        )
    }

    @Test
    @OptIn(ComplicationExperimental::class)
    public fun noDataComplicationData_list() {
        val wireShortText = WireComplicationDataBuilder(WireComplicationData.TYPE_SHORT_TEXT)
            .setShortText(WireComplicationText.plainText("text"))
            .build()

        val wireRangedValue = WireComplicationDataBuilder(WireComplicationData.TYPE_RANGED_VALUE)
            .setRangedValue(95f)
            .setRangedMinValue(0f)
            .setRangedMaxValue(100f)
            .setShortTitle(WireComplicationText.plainText("battery"))
            .build()

        assertRoundtrip(
            WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                .setPlaceholder(
                    WireComplicationDataBuilder(WireComplicationData.TYPE_LIST)
                        .setListEntryCollection(listOf(wireShortText, wireRangedValue))
                        .setListStyleHint(
                            ListComplicationData.StyleHint.RowOfColumns.ordinal
                        )
                        .build(),
                )
                .build(),
            ComplicationType.NO_DATA
        )
    }

    private fun assertRoundtrip(wireData: WireComplicationData, type: ComplicationType) {
        val data = wireData.toApiComplicationData()
        assertThat(data.type).isEqualTo(type)
        ParcelableSubject.assertThat(data.asWireComplicationData()).hasSameSerializationAs(wireData)
    }
}

@RunWith(SharedRobolectricTestRunner::class)
public class TapActionTest {
    private val mPendingIntent = PendingIntent.getBroadcast(
        ApplicationProvider.getApplicationContext(),
        0,
        Intent(),
        0
    )

    @Test
    public fun shortTextComplicationData() {
        assertThat(
            ShortTextComplicationData.Builder("text".complicationText, ComplicationText.EMPTY)
                .setTapAction(mPendingIntent)
                .build().asWireComplicationData().tapAction
        ).isEqualTo(mPendingIntent)
    }

    @Test
    public fun longTextComplicationData() {
        assertThat(
            LongTextComplicationData.Builder("text".complicationText, ComplicationText.EMPTY)
                .setTapAction(mPendingIntent)
                .build().asWireComplicationData().tapAction
        ).isEqualTo(mPendingIntent)
    }

    @Test
    public fun rangedValueComplicationData() {
        assertThat(
            RangedValueComplicationData.Builder(
                value = 95f, min = 0f, max = 100f,
                contentDescription = ComplicationText.EMPTY
            )
                .setText("battery".complicationText)
                .setTapAction(mPendingIntent)
                .build().asWireComplicationData().tapAction
        ).isEqualTo(mPendingIntent)
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun goalProgressComplicationData() {
        assertThat(
            GoalProgressComplicationData.Builder(
                value = 1200f, targetValue = 10000f,
                contentDescription = "content description".complicationText
            )
                .setTitle("steps".complicationText)
                .setTapAction(mPendingIntent)
                .setColorRamp(ColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE), true))
                .build().asWireComplicationData().tapAction
        ).isEqualTo(mPendingIntent)
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun discreteRangedValueComplicationData() {
        assertThat(
            DiscreteRangedValueComplicationData.Builder(
                value = 95, min = 0, max = 100,
                contentDescription = ComplicationText.EMPTY
            )
                .setText("battery".complicationText)
                .setTapAction(mPendingIntent)
                .build().asWireComplicationData().tapAction
        ).isEqualTo(mPendingIntent)
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun weightedElementsComplicationData() {
        assertThat(
            WeightedElementsComplicationData.Builder(
                listOf(
                    WeightedElementsComplicationData.Element(0.5f, Color.RED),
                    WeightedElementsComplicationData.Element(1f, Color.GREEN),
                    WeightedElementsComplicationData.Element(2f, Color.BLUE),
                ),
                contentDescription = "content description".complicationText
            )
                .setTitle("calories".complicationText)
                .setTapAction(mPendingIntent)
                .build().asWireComplicationData().tapAction
        ).isEqualTo(mPendingIntent)
    }

    @Test
    public fun monochromaticImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        val image = MonochromaticImage.Builder(icon).build()
        assertThat(
            MonochromaticImageComplicationData.Builder(image, ComplicationText.EMPTY)
                .setTapAction(mPendingIntent)
                .build()
                .asWireComplicationData()
                .tapAction
        ).isEqualTo(mPendingIntent)
    }

    @Test
    public fun smallImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        val image = SmallImage.Builder(icon, SmallImageType.PHOTO).build()
        assertThat(
            SmallImageComplicationData.Builder(image, ComplicationText.EMPTY)
                .setTapAction(mPendingIntent).build()
                .asWireComplicationData()
                .tapAction
        ).isEqualTo(mPendingIntent)
    }

    @Test
    public fun photoImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        assertThat(
            PhotoImageComplicationData.Builder(icon, ComplicationText.EMPTY)
                .setTapAction(mPendingIntent).build()
                .asWireComplicationData()
                .tapAction
        ).isEqualTo(mPendingIntent)
    }

    @Test
    public fun noDataComplicationData() {
        assertThat(
            NoDataComplicationData(
                ShortTextComplicationData.Builder(
                    ComplicationText.PLACEHOLDER,
                    ComplicationText.EMPTY
                ).setTapAction(mPendingIntent).build()
            ).asWireComplicationData().placeholder?.tapAction
        ).isEqualTo(mPendingIntent)
    }

    @Test
    @OptIn(ComplicationExperimental::class)
    public fun protoLayoutComplicationData() {
        val ambientLayout = ByteArray(1)
        val interactiveLayout = ByteArray(2)
        val resources = ByteArray(3)

        assertThat(
            ProtoLayoutComplicationData.Builder(
                ambientLayout,
                interactiveLayout,
                resources,
                ComplicationText.EMPTY
            )
                .setTapAction(mPendingIntent)
                .build()
                .tapAction
        ).isEqualTo(mPendingIntent)
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun listComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        val image = SmallImage.Builder(icon, SmallImageType.PHOTO).build()
        assertThat(
            ListComplicationData.Builder(
                listOf(
                    SmallImageComplicationData.Builder(image, ComplicationText.EMPTY).build()
                ),
                ListComplicationData.StyleHint.RowOfColumns,
                ComplicationText.EMPTY
            )
                .setTapAction(mPendingIntent).build()
                .tapAction
        ).isEqualTo(mPendingIntent)
    }
}

@RunWith(SharedRobolectricTestRunner::class)
public class RoundtripTapActionTest {
    private val mPendingIntent = PendingIntent.getBroadcast(
        ApplicationProvider.getApplicationContext(),
        0,
        Intent(),
        0
    )

    @Test
    public fun shortTextComplicationData() {
        assertThat(
            ShortTextComplicationData.Builder("text".complicationText, ComplicationText.EMPTY)
                .setTapAction(mPendingIntent)
                .build().asWireComplicationData().toApiComplicationData().tapAction
        ).isEqualTo(mPendingIntent)
    }

    @Test
    public fun longTextComplicationData() {
        assertThat(
            LongTextComplicationData.Builder("text".complicationText, ComplicationText.EMPTY)
                .setTapAction(mPendingIntent)
                .build().asWireComplicationData().toApiComplicationData().tapAction
        ).isEqualTo(mPendingIntent)
    }

    @Test
    public fun rangedValueComplicationData() {
        assertThat(
            RangedValueComplicationData.Builder(
                value = 95f, min = 0f, max = 100f,
                contentDescription = ComplicationText.EMPTY
            )
                .setText("battery".complicationText)
                .setTapAction(mPendingIntent)
                .build().asWireComplicationData().toApiComplicationData().tapAction
        ).isEqualTo(mPendingIntent)
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun goalProgressComplicationData() {
        assertThat(
            GoalProgressComplicationData.Builder(
                value = 1200f, targetValue = 10000f,
                contentDescription = "content description".complicationText
            )
                .setTitle("steps".complicationText)
                .setTapAction(mPendingIntent)
                .setColorRamp(ColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE), false))
                .build().asWireComplicationData().toApiComplicationData().tapAction
        ).isEqualTo(mPendingIntent)
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun discreteRangedValueComplicationData() {
        assertThat(
            DiscreteRangedValueComplicationData.Builder(
                value = 95, min = 0, max = 100,
                contentDescription = ComplicationText.EMPTY
            )
                .setText("battery".complicationText)
                .setTapAction(mPendingIntent)
                .build().asWireComplicationData().toApiComplicationData().tapAction
        ).isEqualTo(mPendingIntent)
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun weightedElementsComplicationData() {
        assertThat(
            WeightedElementsComplicationData.Builder(
                listOf(
                    WeightedElementsComplicationData.Element(0.5f, Color.RED),
                    WeightedElementsComplicationData.Element(1f, Color.GREEN),
                    WeightedElementsComplicationData.Element(2f, Color.BLUE),
                ),
                contentDescription = "content description".complicationText
            )
                .setTitle("calories".complicationText)
                .setTapAction(mPendingIntent)
                .build().asWireComplicationData().toApiComplicationData().tapAction
        ).isEqualTo(mPendingIntent)
    }

    @Test
    public fun monochromaticImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        val image = MonochromaticImage.Builder(icon).build()
        assertThat(
            MonochromaticImageComplicationData.Builder(image, ComplicationText.EMPTY)
                .setTapAction(mPendingIntent)
                .build()
                .asWireComplicationData()
                .toApiComplicationData()
                .tapAction
        ).isEqualTo(mPendingIntent)
    }

    @Test
    public fun smallImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        val image = SmallImage.Builder(icon, SmallImageType.PHOTO).build()
        assertThat(
            SmallImageComplicationData.Builder(image, ComplicationText.EMPTY)
                .setTapAction(mPendingIntent).build()
                .asWireComplicationData()
                .toApiComplicationData()
                .tapAction
        ).isEqualTo(mPendingIntent)
    }

    @Test
    public fun photoImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        assertThat(
            PhotoImageComplicationData.Builder(icon, ComplicationText.EMPTY)
                .setTapAction(mPendingIntent).build()
                .asWireComplicationData()
                .toApiComplicationData()
                .tapAction
        ).isEqualTo(mPendingIntent)
    }

    @Test
    public fun noDataComplicationData() {
        assertThat(
            NoDataComplicationData(
                MonochromaticImageComplicationData.Builder(
                    MonochromaticImage.PLACEHOLDER,
                    ComplicationText.EMPTY
                ).setTapAction(mPendingIntent).build()
            ).asWireComplicationData().toApiComplicationData().tapAction
        ).isEqualTo(mPendingIntent)
    }

    @Test
    @OptIn(ComplicationExperimental::class)
    public fun protoLayoutComplicationData() {
        val ambientLayout = ByteArray(1)
        val interactiveLayout = ByteArray(2)
        val resources = ByteArray(3)

        assertThat(
            ProtoLayoutComplicationData.Builder(
                ambientLayout,
                interactiveLayout,
                resources,
                ComplicationText.EMPTY
            )
                .setTapAction(mPendingIntent)
                .build()
                .asWireComplicationData().toApiComplicationData().tapAction
        ).isEqualTo(mPendingIntent)
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun listComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        val image = SmallImage.Builder(icon, SmallImageType.PHOTO).build()
        assertThat(
            ListComplicationData.Builder(
                listOf(
                    SmallImageComplicationData.Builder(image, ComplicationText.EMPTY).build()
                ),
                ListComplicationData.StyleHint.RowOfColumns,
                ComplicationText.EMPTY
            )
                .setTapAction(mPendingIntent).build()
                .asWireComplicationData()
                .toApiComplicationData()
                .tapAction
        ).isEqualTo(mPendingIntent)
    }
}

@RunWith(SharedRobolectricTestRunner::class)
public class ValidTimeRangeTest {
    private val testStartInstant = Instant.ofEpochMilli(1000L)
    private val testEndDateInstant = Instant.ofEpochMilli(2000L)

    @Test
    public fun shortTextComplicationData() {
        val data = ShortTextComplicationData.Builder(
            "text".complicationText, ComplicationText.EMPTY
        )
            .setValidTimeRange(TimeRange.between(testStartInstant, testEndDateInstant))
            .build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_SHORT_TEXT)
                    .setShortText(WireComplicationText.plainText("text"))
                    .setStartDateTimeMillis(testStartInstant.toEpochMilli())
                    .setEndDateTimeMillis(testEndDateInstant.toEpochMilli())
                    .build()
            )
    }

    @Test
    public fun longTextComplicationData() {
        val data = LongTextComplicationData.Builder("text".complicationText, ComplicationText.EMPTY)
            .setValidTimeRange(TimeRange.between(testStartInstant, testEndDateInstant))
            .build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_LONG_TEXT)
                    .setLongText(WireComplicationText.plainText("text"))
                    .setStartDateTimeMillis(testStartInstant.toEpochMilli())
                    .setEndDateTimeMillis(testEndDateInstant.toEpochMilli())
                    .build()
            )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun rangedValueComplicationData() {
        val data = RangedValueComplicationData.Builder(
            value = 95f, min = 0f, max = 100f,
            contentDescription = ComplicationText.EMPTY
        )
            .setText("battery".complicationText)
            .setValidTimeRange(TimeRange.between(testStartInstant, testEndDateInstant))
            .build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_RANGED_VALUE)
                    .setRangedValue(95f)
                    .setRangedMinValue(0f)
                    .setRangedMaxValue(100f)
                    .setShortText(WireComplicationText.plainText("battery"))
                    .setStartDateTimeMillis(testStartInstant.toEpochMilli())
                    .setEndDateTimeMillis(testEndDateInstant.toEpochMilli())
                    .build()
            )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun goalProgressComplicationData() {
        val data = GoalProgressComplicationData.Builder(
            value = 1200f, targetValue = 10000f,
            contentDescription = "content description".complicationText
        )
            .setTitle("steps".complicationText)
            .setValidTimeRange(TimeRange.between(testStartInstant, testEndDateInstant))
            .setColorRamp(ColorRamp(intArrayOf(Color.YELLOW, Color.MAGENTA), true))
            .build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_GOAL_PROGRESS)
                    .setRangedValue(1200f)
                    .setTargetValue(10000f)
                    .setShortTitle(WireComplicationText.plainText("steps"))
                    .setContentDescription(WireComplicationText.plainText("content description"))
                    .setColorRamp(intArrayOf(Color.YELLOW, Color.MAGENTA))
                    .setColorRampIsSmoothShaded(true)
                    .setStartDateTimeMillis(testStartInstant.toEpochMilli())
                    .setEndDateTimeMillis(testEndDateInstant.toEpochMilli())
                    .build()
            )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun discreteRangedValueComplicationData() {
        val data = DiscreteRangedValueComplicationData.Builder(
            value = 95, min = 0, max = 100,
            contentDescription = ComplicationText.EMPTY
        )
            .setText("battery".complicationText)
            .setValidTimeRange(TimeRange.between(testStartInstant, testEndDateInstant))
            .build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_DISCRETE_RANGED_VALUE)
                    .setDiscreteRangedValue(95)
                    .setDiscreteRangedMinValue(0)
                    .setDiscreteRangedMaxValue(100)
                    .setShortText(WireComplicationText.plainText("battery"))
                    .setStartDateTimeMillis(testStartInstant.toEpochMilli())
                    .setEndDateTimeMillis(testEndDateInstant.toEpochMilli())
                    .build()
            )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun weightedElementsComplicationData() {
        val data = WeightedElementsComplicationData.Builder(
            listOf(
                WeightedElementsComplicationData.Element(0.5f, Color.RED),
                WeightedElementsComplicationData.Element(1f, Color.GREEN),
                WeightedElementsComplicationData.Element(2f, Color.BLUE),
            ),
            contentDescription = "content description".complicationText
        )
            .setTitle("calories".complicationText)
            .setValidTimeRange(TimeRange.between(testStartInstant, testEndDateInstant))
            .build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_WEIGHTED_ELEMENTS)
                    .setElementWeights(floatArrayOf(0.5f, 1f, 2f))
                    .setElementColors(intArrayOf(Color.RED, Color.GREEN, Color.BLUE))
                    .setShortTitle(WireComplicationText.plainText("calories"))
                    .setContentDescription(WireComplicationText.plainText("content description"))
                    .setStartDateTimeMillis(testStartInstant.toEpochMilli())
                    .setEndDateTimeMillis(testEndDateInstant.toEpochMilli())
                    .build()
            )
    }

    @Test
    public fun monochromaticImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        val image = MonochromaticImage.Builder(icon).build()
        val data = MonochromaticImageComplicationData.Builder(image, ComplicationText.EMPTY)
            .setValidTimeRange(TimeRange.between(testStartInstant, testEndDateInstant))
            .build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_ICON)
                    .setIcon(icon)
                    .setStartDateTimeMillis(testStartInstant.toEpochMilli())
                    .setEndDateTimeMillis(testEndDateInstant.toEpochMilli())
                    .build()
            )
    }

    @Test
    public fun smallImageComplicationData() {
        val icon = Icon.createWithContentUri("someuri")
        val image = SmallImage.Builder(icon, SmallImageType.PHOTO).build()
        val data = SmallImageComplicationData.Builder(image, ComplicationText.EMPTY)
            .setValidTimeRange(TimeRange.between(testStartInstant, testEndDateInstant))
            .build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_SMALL_IMAGE)
                    .setSmallImage(icon)
                    .setSmallImageStyle(WireComplicationData.IMAGE_STYLE_PHOTO)
                    .setStartDateTimeMillis(testStartInstant.toEpochMilli())
                    .setEndDateTimeMillis(testEndDateInstant.toEpochMilli())
                    .build()
            )
    }

    @Test
    public fun photoImageComplicationData() {
        val photoImage = Icon.createWithContentUri("someuri")
        val data = PhotoImageComplicationData.Builder(photoImage, ComplicationText.EMPTY)
            .setValidTimeRange(TimeRange.between(testStartInstant, testEndDateInstant))
            .build()
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_LARGE_IMAGE)
                    .setLargeImage(photoImage)
                    .setStartDateTimeMillis(testStartInstant.toEpochMilli())
                    .setEndDateTimeMillis(testEndDateInstant.toEpochMilli())
                    .build()
            )
    }

    @Test
    @OptIn(ComplicationExperimental::class)
    public fun protoLayout() {
        val ambientLayout = ByteArray(1)
        val interactiveLayout = ByteArray(2)
        val resources = ByteArray(3)

        val data = ProtoLayoutComplicationData.Builder(
            ambientLayout,
            interactiveLayout,
            resources,
            ComplicationText.EMPTY
        )
            .setValidTimeRange(TimeRange.between(testStartInstant, testEndDateInstant))
            .build()

        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_PROTO_LAYOUT)
                    .setAmbientLayout(ambientLayout)
                    .setInteractiveLayout(interactiveLayout)
                    .setLayoutResources(resources)
                    .setStartDateTimeMillis(testStartInstant.toEpochMilli())
                    .setEndDateTimeMillis(testEndDateInstant.toEpochMilli())
                    .build()
            )
    }

    @Test
    @OptIn(ComplicationExperimental::class)
    public fun list() {
        val data = ListComplicationData.Builder(
            listOf(
                ShortTextComplicationData.Builder(
                    "text".complicationText,
                    ComplicationText.EMPTY
                )
                    .build(),
                RangedValueComplicationData.Builder(
                    value = 95f,
                    min = 0f,
                    max = 100f,
                    ComplicationText.EMPTY
                )
                    .setText("battery".complicationText)
                    .build()
            ),
            ListComplicationData.StyleHint.RowOfColumns,
            ComplicationText.EMPTY
        )
            .setValidTimeRange(TimeRange.between(testStartInstant, testEndDateInstant))
            .build()

        val wireShortText = WireComplicationDataBuilder(WireComplicationData.TYPE_SHORT_TEXT)
            .setShortText(WireComplicationText.plainText("text"))
            .build()

        val wireRangedValue = WireComplicationDataBuilder(WireComplicationData.TYPE_RANGED_VALUE)
            .setRangedValue(95f)
            .setRangedMinValue(0f)
            .setRangedMaxValue(100f)
            .setShortText(WireComplicationText.plainText("battery"))
            .build()

        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_LIST)
                    .setListEntryCollection(listOf(wireShortText, wireRangedValue))
                    .setListStyleHint(
                        ListComplicationData.StyleHint.RowOfColumns.ordinal
                    )
                    .setStartDateTimeMillis(testStartInstant.toEpochMilli())
                    .setEndDateTimeMillis(testEndDateInstant.toEpochMilli())
                    .build()
            )
    }

    @Test
    public fun noDataComplicationData_shortText() {
        val data = NoDataComplicationData(
            ShortTextComplicationData.Builder("text".complicationText, ComplicationText.EMPTY)
                .build(),
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_SHORT_TEXT)
                            .setShortText(WireComplicationText.plainText("text"))
                            .build()
                    )
                    .build()
            )
    }

    @Test
    public fun noDataComplicationData_longText() {
        val data = NoDataComplicationData(
            LongTextComplicationData.Builder("text".complicationText, ComplicationText.EMPTY)
                .build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_LONG_TEXT)
                            .setLongText(WireComplicationText.plainText("text"))
                            .build()
                    )
                    .build()
            )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun noDataComplicationData_rangedValue() {
        val data = NoDataComplicationData(
            RangedValueComplicationData.Builder(
                value = 95f,
                min = 0f,
                max = 100f,
                ComplicationText.EMPTY
            )
                .setText("battery".complicationText)
                .build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_RANGED_VALUE)
                            .setRangedValue(95f)
                            .setRangedMinValue(0f)
                            .setRangedMaxValue(100f)
                            .setShortText(WireComplicationText.plainText("battery"))
                            .build()
                    )
                    .build()
            )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun noDataComplicationData_goalProgress() {
        val data = NoDataComplicationData(
            GoalProgressComplicationData.Builder(
                value = 1200f, targetValue = 10000f,
                contentDescription = "content description".complicationText
            )
                .setTitle("steps".complicationText)
                .setColorRamp(ColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE), false))
                .build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_GOAL_PROGRESS)
                            .setRangedValue(1200f)
                            .setTargetValue(10000f)
                            .setShortTitle(WireComplicationText.plainText("steps"))
                            .setContentDescription(
                                WireComplicationText.plainText("content description")
                            )
                            .setColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE))
                            .setColorRampIsSmoothShaded(false)
                            .build()
                    )
                    .build()
            )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun noDataComplicationData_discreteRangedValue() {
        val data = NoDataComplicationData(
            DiscreteRangedValueComplicationData.Builder(
                value = 95,
                min = 0,
                max = 100,
                ComplicationText.EMPTY
            )
                .setText("battery".complicationText)
                .build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_DISCRETE_RANGED_VALUE)
                            .setDiscreteRangedValue(95)
                            .setDiscreteRangedMinValue(0)
                            .setDiscreteRangedMaxValue(100)
                            .setShortText(WireComplicationText.plainText("battery"))
                            .build()
                    )
                    .build()
            )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    public fun noDataComplicationData_weightedElements() {
        val data = NoDataComplicationData(
            WeightedElementsComplicationData.Builder(
                listOf(
                    WeightedElementsComplicationData.Element(0.5f, Color.RED),
                    WeightedElementsComplicationData.Element(1f, Color.GREEN),
                    WeightedElementsComplicationData.Element(2f, Color.BLUE),
                ),
                contentDescription = "content description".complicationText
            )
                .setTitle("calories".complicationText)
                .build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_WEIGHTED_ELEMENTS)
                            .setElementWeights(floatArrayOf(0.5f, 1f, 2f))
                            .setElementColors(intArrayOf(Color.RED, Color.GREEN, Color.BLUE))
                            .setShortTitle(WireComplicationText.plainText("calories"))
                            .setContentDescription(
                                WireComplicationText.plainText("content description")
                            )
                            .build()
                    )
                    .build()
            )
    }

    @Test
    public fun noDataComplicationData_monochromaticImage() {
        val icon = Icon.createWithContentUri("someuri")
        val image = MonochromaticImage.Builder(icon).build()
        val data = NoDataComplicationData(
            MonochromaticImageComplicationData.Builder(image, ComplicationText.EMPTY).build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_ICON)
                            .setIcon(icon)
                            .build()
                    )
                    .build()
            )
    }

    @Test
    public fun noDataComplicationData_smallImage() {
        val icon = Icon.createWithContentUri("someuri")
        val image = SmallImage.Builder(icon, SmallImageType.PHOTO).build()
        val data = NoDataComplicationData(
            SmallImageComplicationData.Builder(image, ComplicationText.EMPTY).build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_SMALL_IMAGE)
                            .setSmallImage(icon)
                            .setSmallImageStyle(WireComplicationData.IMAGE_STYLE_PHOTO)
                            .build()
                    )
                    .build()
            )
    }

    @Test
    public fun noDataComplicationData_photoImage() {
        val icon = Icon.createWithContentUri("someuri")
        val data = NoDataComplicationData(
            PhotoImageComplicationData.Builder(icon, ComplicationText.EMPTY).build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_LARGE_IMAGE)
                            .setLargeImage(icon)
                            .build()
                    )
                    .build()
            )
    }

    @Test
    @OptIn(ComplicationExperimental::class)
    public fun noDataComplicationData_protoLayout() {
        val ambientLayout = ByteArray(1)
        val interactiveLayout = ByteArray(2)
        val resources = ByteArray(3)

        val data = NoDataComplicationData(
            ProtoLayoutComplicationData.Builder(
                ambientLayout,
                interactiveLayout,
                resources,
                ComplicationText.EMPTY
            ).build()
        )
        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_PROTO_LAYOUT)
                            .setAmbientLayout(ambientLayout)
                            .setInteractiveLayout(interactiveLayout)
                            .setLayoutResources(resources)
                            .build()
                    )
                    .build()
            )
    }

    @Test
    @OptIn(ComplicationExperimental::class)
    public fun noDataComplicationData_list() {
        val data = NoDataComplicationData(
            ListComplicationData.Builder(
                listOf(
                    ShortTextComplicationData.Builder(
                        "text".complicationText,
                        ComplicationText.EMPTY
                    )
                        .build(),
                    RangedValueComplicationData.Builder(
                        value = 95f,
                        min = 0f,
                        max = 100f,
                        ComplicationText.EMPTY
                    )
                        .setText("battery".complicationText)
                        .build()
                ),
                ListComplicationData.StyleHint.RowOfColumns,
                ComplicationText.EMPTY
            ).build()
        )

        val wireShortText = WireComplicationDataBuilder(WireComplicationData.TYPE_SHORT_TEXT)
            .setShortText(WireComplicationText.plainText("text"))
            .build()

        val wireRangedValue = WireComplicationDataBuilder(WireComplicationData.TYPE_RANGED_VALUE)
            .setRangedValue(95f)
            .setRangedMinValue(0f)
            .setRangedMaxValue(100f)
            .setShortText(WireComplicationText.plainText("battery"))
            .build()

        ParcelableSubject.assertThat(data.asWireComplicationData())
            .hasSameSerializationAs(
                WireComplicationDataBuilder(WireComplicationData.TYPE_NO_DATA)
                    .setPlaceholder(
                        WireComplicationDataBuilder(WireComplicationData.TYPE_LIST)
                            .setListEntryCollection(listOf(wireShortText, wireRangedValue))
                            .setListStyleHint(
                                ListComplicationData.StyleHint.RowOfColumns.ordinal
                            )
                            .build()
                    )
                    .build()
            )
    }
}

@RunWith(SharedRobolectricTestRunner::class)
public class RedactionTest {
    @Before
    fun setup() {
        ShadowLog.setLoggable("ComplicationData", Log.INFO)
    }

    @Test
    fun shouldRedact() {
        ShadowLog.setLoggable("ComplicationData", Log.DEBUG)
        assertThat(WireComplicationData.shouldRedact()).isFalse()

        ShadowLog.setLoggable("ComplicationData", Log.INFO)
        assertThat(WireComplicationData.shouldRedact()).isTrue()
    }

    @Test
    fun shortText() {
        val data = ShortTextComplicationData.Builder(
            "text".complicationText,
            "content description".complicationText
        )
            .setTitle("title".complicationText)
            .build()

        assertThat(data.toString()).isEqualTo(
            "ShortTextComplicationData(text=ComplicationText{mSurroundingText=REDACTED, " +
                "mTimeDependentText=null}, title=ComplicationText{mSurroundingText=REDACTED, " +
                "mTimeDependentText=null}, monochromaticImage=null, contentDescription=" +
                "ComplicationText{mSurroundingText=REDACTED, mTimeDependentText=null}, " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=" +
                "TimeRange(REDACTED), dataSource=null)"
        )
        assertThat(data.asWireComplicationData().toString()).isEqualTo(
            "ComplicationData{mType=3, mFields=REDACTED}"
        )
    }

    @Test
    fun longText() {
        val data = LongTextComplicationData.Builder(
            "text".complicationText,
            "content description".complicationText
        )
            .setTitle("title".complicationText)
            .build()

        assertThat(data.toString()).isEqualTo("LongTextComplicationData(text=" +
            "ComplicationText{mSurroundingText=REDACTED, mTimeDependentText=null}, title=" +
            "ComplicationText{mSurroundingText=REDACTED, mTimeDependentText=null}, " +
            "monochromaticImage=null, smallImage=null, contentDescription=ComplicationText" +
            "{mSurroundingText=REDACTED, mTimeDependentText=null}), " +
            "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=TimeRange" +
            "(REDACTED), dataSource=null)"
        )
        assertThat(data.asWireComplicationData().toString()).isEqualTo(
            "ComplicationData{mType=4, mFields=REDACTED}"
        )
    }

    @Test
    fun rangedValue() {
        val data = RangedValueComplicationData.Builder(
            50f,
            0f,
            100f,
            "content description".complicationText
        )
            .setText("text".complicationText)
            .setTitle("title".complicationText)
            .build()

        assertThat(data.toString()).isEqualTo("RangedValueComplicationData(value=REDACTED, " +
            "min=0.0, max=100.0, monochromaticImage=null, title=ComplicationText{mSurroundingText" +
            "=REDACTED, mTimeDependentText=null}, text=ComplicationText{mSurroundingText=REDACTED" +
            ", mTimeDependentText=null}, contentDescription=ComplicationText{mSurroundingText=" +
            "REDACTED, mTimeDependentText=null}), tapActionLostDueToSerialization=false, " +
            "tapAction=null, validTimeRange=TimeRange(REDACTED), dataSource=null, colorRamp=null)"
        )
        assertThat(data.asWireComplicationData().toString()).isEqualTo(
            "ComplicationData{mType=5, mFields=REDACTED}"
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    fun goalProgress() {
        val data = GoalProgressComplicationData.Builder(
            value = 1200f, targetValue = 10000f,
            contentDescription = "content description".complicationText
        )
            .setTitle("steps".complicationText)
            .setColorRamp(ColorRamp(intArrayOf(Color.RED, Color.GREEN, Color.BLUE), true))
            .build()

        assertThat(data.toString()).isEqualTo(
            "GoalProgressComplicationData(value=REDACTED, targetValue=10000.0, " +
                "monochromaticImage=null, title=ComplicationText{mSurroundingText=REDACTED, " +
                "mTimeDependentText=null}, text=null, contentDescription=ComplicationText{" +
                "mSurroundingText=REDACTED, mTimeDependentText=null}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, " +
                "validTimeRange=TimeRange(REDACTED), dataSource=null, " +
                "colorRamp=ColorRamp(colors=[-65536, -16711936, -16776961], interpolated=true))"
        )
        assertThat(data.asWireComplicationData().toString()).isEqualTo(
            "ComplicationData{mType=13, mFields=REDACTED}"
        )
    }

    @OptIn(ComplicationExperimental::class)
    @Test
    fun discreteRangedValue() {
        val data = DiscreteRangedValueComplicationData.Builder(
            50,
            0,
            100,
            "content description".complicationText
        )
            .setText("text".complicationText)
            .setTitle("title".complicationText)
            .build()

        assertThat(data.toString()).isEqualTo(
            "DiscreteRangedValueComplicationData(value=REDACTED, " +
            "min=0, max=100, monochromaticImage=null, title=ComplicationText{mSurroundingText" +
            "=REDACTED, mTimeDependentText=null}, text=ComplicationText{mSurroundingText=REDACTED" +
            ", mTimeDependentText=null}, contentDescription=ComplicationText{mSurroundingText=" +
            "REDACTED, mTimeDependentText=null}), tapActionLostDueToSerialization=false, " +
            "tapAction=null, validTimeRange=TimeRange(REDACTED), dataSource=null)"
        )
        assertThat(data.asWireComplicationData().toString()).isEqualTo(
            "ComplicationData{mType=14, mFields=REDACTED}"
        )
    }

    @Test
    fun placeholder() {
        val data = NoDataComplicationData(
            LongTextComplicationData.Builder(
                ComplicationText.PLACEHOLDER,
                ComplicationText.EMPTY
            ).build()
        )

        assertThat(data.toString()).isEqualTo(
            "NoDataComplicationData(placeholder=LongTextComplicationData(text=" +
                "ComplicationText{mSurroundingText=__placeholder__, mTimeDependentText=null}, " +
                "title=null, monochromaticImage=null, smallImage=null, contentDescription=" +
                "ComplicationText{mSurroundingText=REDACTED, mTimeDependentText=null}), " +
                "tapActionLostDueToSerialization=false, tapAction=null, validTimeRange=" +
                "TimeRange(REDACTED), dataSource=null), tapActionLostDueToSerialization=false, " +
                "tapAction=null, validTimeRange=TimeRange(REDACTED))"
        )
        assertThat(data.asWireComplicationData().toString()).isEqualTo(
            "ComplicationData{mType=10, mFields=REDACTED}"
        )
    }
}

val String.complicationText get() = PlainComplicationText.Builder(this).build()
