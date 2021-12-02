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

import android.content.Context
import android.icu.util.TimeZone
import android.support.wearable.complications.ComplicationText
import android.support.wearable.complications.TimeFormatText
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.concurrent.TimeUnit

private typealias WireTimeDifferenceBuilder =
    android.support.wearable.complications.ComplicationText.TimeDifferenceBuilder

private typealias WireTimeFormatBuilder =
    android.support.wearable.complications.ComplicationText.TimeFormatBuilder

@RunWith(SharedRobolectricTestRunner::class)
public class AsWireComplicationTextTest {
    @Test
    public fun plainText() {
        val text = PlainComplicationText.Builder("abc").build()
        ParcelableSubject.assertThat(text.toWireComplicationText())
            .hasSameSerializationAs(WireComplicationText.plainText("abc"))
        ParcelableSubject.assertThat(text.toWireComplicationText())
            .hasDifferentSerializationAs(WireComplicationText.plainText("abc1"))
    }

    @Test
    public fun timeDifferenceText_CountUpTimeReference() {
        val referenceInstant = Instant.parse("2020-12-30T10:15:30.001Z")
        val text = TimeDifferenceComplicationText.Builder(
            TimeDifferenceStyle.STOPWATCH,
            CountUpTimeReference(referenceInstant)
        )
            .setText("^1 after lunch")
            .setDisplayAsNow(false)
            .setMinimumTimeUnit(TimeUnit.SECONDS)
            .build()

        ParcelableSubject.assertThat(text.toWireComplicationText())
            .hasSameSerializationAs(
                WireTimeDifferenceBuilder()
                    .setStyle(WireComplicationText.DIFFERENCE_STYLE_STOPWATCH)
                    .setSurroundingText("^1 after lunch")
                    .setShowNowText(false)
                    .setMinimumUnit(TimeUnit.SECONDS)
                    .setReferencePeriodEndMillis(referenceInstant.toEpochMilli())
                    .build()
            )

        val twoMinutesThreeSecondAfter =
            Instant.ofEpochMilli(referenceInstant.toEpochMilli() + 2.minutes + 3.seconds)
        assertThat(
            text.getTextAt(getResource(), twoMinutesThreeSecondAfter).toString()
        ).isEqualTo("02:03 after lunch")
    }

    @Test
    public fun timeDifferenceText_CountDownTimeReference() {
        val referenceInstant = Instant.parse("2020-12-30T10:15:30.001Z")
        val text = TimeDifferenceComplicationText.Builder(
            TimeDifferenceStyle.STOPWATCH,
            CountDownTimeReference(referenceInstant)
        )
            .setText("^1 before lunch")
            .setDisplayAsNow(false)
            .setMinimumTimeUnit(TimeUnit.SECONDS)
            .build()

        ParcelableSubject.assertThat(text.toWireComplicationText())
            .hasSameSerializationAs(
                WireTimeDifferenceBuilder()
                    .setStyle(WireComplicationText.DIFFERENCE_STYLE_STOPWATCH)
                    .setSurroundingText("^1 before lunch")
                    .setShowNowText(false)
                    .setMinimumUnit(TimeUnit.SECONDS)
                    .setReferencePeriodStartMillis(referenceInstant.toEpochMilli())
                    .build()
            )

        val twoMinutesThreeSecondBefore =
            Instant.ofEpochMilli(referenceInstant.toEpochMilli() - 2.minutes - 3.seconds)
        assertThat(
            text.getTextAt(getResource(), twoMinutesThreeSecondBefore).toString()
        ).isEqualTo("02:03 before lunch")
    }

    @Test
    public fun timeFormatText() {
        val text = TimeFormatComplicationText.Builder("h:m")
            .setText("^1 in London")
            .setStyle(TimeFormatStyle.UPPER_CASE)
            .setTimeZone(TimeZone.getTimeZone("Europe/London"))
            .build()

        ParcelableSubject.assertThat(text.toWireComplicationText())
            .hasSameSerializationAs(
                WireTimeFormatBuilder()
                    .setFormat("h:m")
                    .setSurroundingText("^1 in London")
                    .setStyle(WireComplicationText.FORMAT_STYLE_UPPER_CASE)
                    .setTimeZone(java.util.TimeZone.getTimeZone("Europe/London"))
                    .build()
            )
    }

    private fun getResource() = ApplicationProvider.getApplicationContext<Context>().resources
}

@RunWith(SharedRobolectricTestRunner::class)
public class FromWireComplicationTextTest {
    @Test
    public fun plainText() {
        val wireText = WireComplicationText.plainText("abc")
        val text = wireText.toApiComplicationText()

        assertThat(text.getTextAt(getResource(), Instant.EPOCH))
            .isEqualTo("abc")
        assertThat(text.getNextChangeTime(Instant.EPOCH))
            .isEqualTo(Instant.ofEpochMilli(Long.MAX_VALUE))
        assertThat(text.isAlwaysEmpty()).isFalse()
        assertThat(
            text.returnsSameText(
                Instant.EPOCH,
                Instant.ofEpochMilli(Long.MAX_VALUE)
            )
        ).isTrue()
    }

    @Test
    public fun timeDifferenceText() {
        val startPoint = Instant.parse("2020-12-30T10:15:30.001Z")
        val wireText = WireTimeDifferenceBuilder()
            .setStyle(WireComplicationText.DIFFERENCE_STYLE_STOPWATCH)
            .setSurroundingText("^1 before lunch")
            .setShowNowText(false)
            .setMinimumUnit(TimeUnit.SECONDS)
            .setReferencePeriodEndMillis(startPoint.toEpochMilli())
            .build()

        val text = wireText.toApiComplicationText()

        val twoMinutesThreeSecondAfter =
            Instant.ofEpochMilli(startPoint.toEpochMilli() + 2.minutes + 3.seconds)
        assertThat(
            text.getTextAt(
                getResource(),
                twoMinutesThreeSecondAfter
            ).toString()
        ).isEqualTo("02:03 before lunch")
        assertThat(text.getNextChangeTime(twoMinutesThreeSecondAfter))
            .isEqualTo(
                Instant.ofEpochMilli(twoMinutesThreeSecondAfter.toEpochMilli() + 1.seconds)
            )
        assertThat(text.isAlwaysEmpty()).isFalse()
        assertThat(text.returnsSameText(twoMinutesThreeSecondAfter, startPoint)).isFalse()
    }

    @Test
    public fun timeFormatText() {
        val dateTime = Instant.parse("2020-12-30T10:15:20.00Z")
        val wireText = WireTimeFormatBuilder()
            .setFormat("h:m")
            .setStyle(WireComplicationText.FORMAT_STYLE_UPPER_CASE)
            .setSurroundingText("^1 in London")
            .setTimeZone(java.util.TimeZone.getTimeZone("Europe/London"))
            .build()

        val text = wireText.toApiComplicationText()

        assertThat(text.getTextAt(getResource(), dateTime).toString())
            .isEqualTo("10:15 in London")
        assertThat(text.getNextChangeTime(dateTime))
            .isEqualTo(Instant.ofEpochMilli(dateTime.toEpochMilli() + 40.seconds))
        assertThat(text.isAlwaysEmpty()).isFalse()
        assertThat(
            text.returnsSameText(
                dateTime,
                Instant.ofEpochMilli(dateTime.toEpochMilli() + 20.seconds)
            )
        ).isTrue()
        assertThat(
            text.returnsSameText(
                dateTime,
                Instant.ofEpochMilli(dateTime.toEpochMilli() + 60.seconds)
            )
        ).isFalse()
    }

    @Test
    public fun testGetMinimumTimeUnit_WithValidTimeDependentTextObject() {
        val minimumTimeUnit = TimeUnit.SECONDS

        val referenceInstant = Instant.parse("2020-12-30T10:15:30.001Z")
        val text = TimeDifferenceComplicationText.Builder(
            TimeDifferenceStyle.STOPWATCH,
            CountUpTimeReference(referenceInstant)
        )
            .setMinimumTimeUnit(minimumTimeUnit)
            .build()

        assertThat(minimumTimeUnit).isEqualTo(text.getMinimumTimeUnit())
    }

    @Test
    public fun testGetMinimumTimeUnit_WithoutTimeDependentTextObject() {
        val referenceInstant = Instant.parse("2020-12-30T10:15:30.001Z")
        val text = TimeDifferenceComplicationText.Builder(
            TimeDifferenceStyle.STOPWATCH,
            CountUpTimeReference(referenceInstant)
        ).build()

        assertNull(text.getMinimumTimeUnit())
    }

    @Test
    public fun testGetMinimumTimeUnit_WithWrongTimeDependentTextObject() {
        val tft = TimeFormatText(
            "E 'in' LLL",
            ComplicationText.FORMAT_STYLE_DEFAULT,
            null
        )
        val text = TimeDifferenceComplicationText(ComplicationText("test", tft))

        assertNull(text.getMinimumTimeUnit())
    }

    private fun getResource() = ApplicationProvider.getApplicationContext<Context>().resources
}

private val Int.minutes get() = TimeUnit.MINUTES.toMillis(this.toLong())
private val Int.seconds get() = TimeUnit.SECONDS.toMillis(this.toLong())
