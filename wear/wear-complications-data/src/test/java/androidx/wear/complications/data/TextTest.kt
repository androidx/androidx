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

import android.content.Context
import android.icu.util.TimeZone
import androidx.test.core.app.ApplicationProvider
import androidx.wear.complications.ParcelableSubject
import androidx.wear.complications.SharedRobolectricTestRunner
import com.google.common.truth.Truth.assertThat
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
        val text = ComplicationText.plain("abc")
        ParcelableSubject.assertThat(text.asWireComplicationText())
            .hasSameSerializationAs(WireComplicationText.plainText("abc"))
        ParcelableSubject.assertThat(text.asWireComplicationText())
            .hasDifferentSerializationAs(WireComplicationText.plainText("abc1"))
    }

    @Test
    public fun timeDifferenceText() {
        val text = ComplicationText.timeDifferenceBuilder(
            TimeDifferenceStyle.STOPWATCH,
            TimeReference.starting(10000L)
        )
            .setText("^1 before lunch")
            .setDisplayAsNow(false)
            .setMinimumUnit(TimeUnit.SECONDS)
            .build()

        ParcelableSubject.assertThat(text.asWireComplicationText())
            .hasSameSerializationAs(
                WireTimeDifferenceBuilder()
                    .setStyle(WireComplicationText.DIFFERENCE_STYLE_STOPWATCH)
                    .setSurroundingText("^1 before lunch")
                    .setShowNowText(false)
                    .setMinimumUnit(TimeUnit.SECONDS)
                    .setReferencePeriodStartMillis(10000L)
                    .build()
            )
    }

    @Test
    public fun timeFormatText() {
        val text = ComplicationText.timeFormatBuilder("h:m")
            .setText("^1 in London")
            .setStyle(TimeFormatStyle.UPPER_CASE)
            .setTimeZone(TimeZone.getTimeZone("Europe/London"))
            .build()

        ParcelableSubject.assertThat(text.asWireComplicationText())
            .hasSameSerializationAs(
                WireTimeFormatBuilder()
                    .setFormat("h:m")
                    .setSurroundingText("^1 in London")
                    .setStyle(WireComplicationText.FORMAT_STYLE_UPPER_CASE)
                    .setTimeZone(java.util.TimeZone.getTimeZone("Europe/London"))
                    .build()
            )
    }
}

@RunWith(SharedRobolectricTestRunner::class)
public class FromWireComplicationText {
    @Test
    public fun plainText() {
        val wireText = WireComplicationText.plainText("abc")
        val text = wireText.asApiComplicationText()

        assertThat(text.getTextAt(getResource(), 0)).isEqualTo("abc")
        assertThat(text.getNextChangeTime(0)).isEqualTo(Long.MAX_VALUE)
        assertThat(text.isAlwaysEmpty()).isFalse()
        assertThat(text.returnsSameText(0, Long.MAX_VALUE)).isTrue()
    }

    @Test
    public fun timeDifferenceText() {
        val startPointMillis = Instant.parse("2020-12-30T10:15:30.001Z").toEpochMilli()
        val wireText = WireTimeDifferenceBuilder()
            .setStyle(WireComplicationText.DIFFERENCE_STYLE_STOPWATCH)
            .setSurroundingText("^1 before lunch")
            .setShowNowText(false)
            .setMinimumUnit(TimeUnit.SECONDS)
            .setReferencePeriodEndMillis(startPointMillis)
            .build()

        val text = wireText.asApiComplicationText()

        val twoMinutesThreeSecondAfter = startPointMillis + 2.minutes + 3.seconds
        assertThat(
            text.getTextAt(
                getResource(),
                twoMinutesThreeSecondAfter
            ).toString()
        ).isEqualTo("02:03 before lunch")
        assertThat(text.getNextChangeTime(twoMinutesThreeSecondAfter))
            .isEqualTo(twoMinutesThreeSecondAfter + 1.seconds)
        assertThat(text.isAlwaysEmpty()).isFalse()
        assertThat(text.returnsSameText(twoMinutesThreeSecondAfter, startPointMillis)).isFalse()
    }

    @Test
    public fun timeFormatText() {
        val dateTimeMillis = Instant.parse("2020-12-30T10:15:20.00Z").toEpochMilli()
        val wireText = WireTimeFormatBuilder()
            .setFormat("h:m")
            .setStyle(WireComplicationText.FORMAT_STYLE_UPPER_CASE)
            .setSurroundingText("^1 in London")
            .setTimeZone(java.util.TimeZone.getTimeZone("Europe/London"))
            .build()

        val text = wireText.asApiComplicationText()

        assertThat(text.getTextAt(getResource(), dateTimeMillis).toString())
            .isEqualTo("10:15 in London")
        assertThat(text.getNextChangeTime(dateTimeMillis))
            .isEqualTo(dateTimeMillis + 40.seconds)
        assertThat(text.isAlwaysEmpty()).isFalse()
        assertThat(text.returnsSameText(dateTimeMillis, dateTimeMillis + 20.seconds)).isTrue()
        assertThat(text.returnsSameText(dateTimeMillis, dateTimeMillis + 60.seconds)).isFalse()
    }

    private fun getResource() = ApplicationProvider.getApplicationContext<Context>().resources
}

private val Int.minutes get() = TimeUnit.MINUTES.toMillis(this.toLong())
private val Int.seconds get() = TimeUnit.SECONDS.toMillis(this.toLong())
