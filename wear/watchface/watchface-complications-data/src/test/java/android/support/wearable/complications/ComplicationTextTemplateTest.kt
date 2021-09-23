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

package android.support.wearable.complications

import android.content.Context
import android.support.wearable.complications.ComplicationText.TimeDifferenceBuilder
import android.support.wearable.complications.ComplicationText.TimeFormatBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.complications.data.SharedRobolectricTestRunner
import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.GregorianCalendar
import java.util.concurrent.TimeUnit

@RunWith(SharedRobolectricTestRunner::class)
public class ComplicationTextTemplateTest {
    private val mResources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    public fun testPlainTextConcatenation() {
        val hello = ComplicationText.plainText(TEST_TEXT1)
        val there = ComplicationText.plainText(TEST_TEXT2)
        val friend = ComplicationText.plainText(TEST_TEXT3)
        val complicationTextTemplate = ComplicationTextTemplate.Builder()
            .addComplicationText(hello)
            .addComplicationText(there)
            .addComplicationText(friend)
            .build()
        Assert.assertEquals(
            "$TEST_TEXT1 $TEST_TEXT2 $TEST_TEXT3",
            complicationTextTemplate.getTextAt(mResources, 132456789).toString()
        )
    }

    @Test
    public fun testPlainTextTemplate() {
        val hello = ComplicationText.plainText(TEST_TEXT1)
        val there = ComplicationText.plainText(TEST_TEXT2)
        val friend = ComplicationText.plainText(TEST_TEXT3)
        val complicationTextTemplate = ComplicationTextTemplate.Builder()
            .addComplicationText(hello)
            .addComplicationText(there)
            .addComplicationText(friend)
            .setSurroundingText("^1, ^2 my ^3.")
            .build()
        Assert.assertEquals(
            "$TEST_TEXT1, $TEST_TEXT2 my $TEST_TEXT3.",
            complicationTextTemplate.getTextAt(mResources, 132456789).toString()
        )
    }

    @Test
    public fun testTemplateEscapeCharacter() {
        val hello = ComplicationText.plainText(TEST_TEXT1)
        val there = ComplicationText.plainText(TEST_TEXT2)
        val complicationTextTemplate = ComplicationTextTemplate.Builder()
            .addComplicationText(hello)
            .addComplicationText(there)
            .setSurroundingText("^1 ^^2 ^2")
            .build()
        Assert.assertEquals(
            "$TEST_TEXT1 ^2 $TEST_TEXT2",
            complicationTextTemplate.getTextAt(mResources, 132456789).toString()
        )
    }

    @Test
    public fun testTimeDifferenceTemplate() {
        val refTime: Long = 10000000
        val complicationText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(refTime - 156561)
                .setReferencePeriodEndMillis(refTime)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                .build()
        val complicationText2 =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(refTime - 156561)
                .setReferencePeriodEndMillis(refTime)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT)
                .build()
        val template = ComplicationTextTemplate.Builder()
            .addComplicationText(complicationText)
            .addComplicationText(complicationText2)
            .setSurroundingText("^1 : ^2")
            .build()

        // "2h 35m" should be rounded to "3h".
        var testTime =
            refTime + TimeUnit.MINUTES.toMillis(35) + TimeUnit.HOURS.toMillis(2)
        Assert.assertEquals(
            "3h : 2h 35m",
            template.getTextAt(mResources, testTime).toString()
        )

        // "23h 59m" should be rounded to "1d".
        testTime =
            refTime + TimeUnit.MINUTES.toMillis(59) + TimeUnit.HOURS.toMillis(23)
        Assert.assertEquals(
            "1d : 23h 59m",
            template.getTextAt(mResources, testTime).toString()
        )

        // "10m 10s" should be rounded to "11m".
        testTime =
            refTime + TimeUnit.SECONDS.toMillis(10) + TimeUnit.MINUTES.toMillis(10)
        Assert.assertEquals(
            "11m : 11m",
            template.getTextAt(mResources, testTime).toString()
        )

        // "23h 15m" should be rounded to "1d".
        testTime =
            refTime + TimeUnit.MINUTES.toMillis(15) + TimeUnit.HOURS.toMillis(23)
        Assert.assertEquals(
            "1d : 23h 15m",
            template.getTextAt(mResources, testTime).toString()
        )
    }

    @Test
    public fun testTimeFormatUpperCase() {
        val complicationText =
            TimeFormatBuilder()
                .setFormat("EEE 'the' d LLL")
                .setStyle(ComplicationText.FORMAT_STYLE_UPPER_CASE)
                .build()
        val longText = ComplicationText.plainText(TEST_TEXT_LONG)
        val template = ComplicationTextTemplate.Builder()
            .addComplicationText(complicationText)
            .addComplicationText(longText)
            .setSurroundingText("^1 *** ^2")
            .build()
        val result = template.getTextAt(
            mResources,
            GregorianCalendar(2016, 2, 4).timeInMillis
        )
        Assert.assertEquals(
            "FRI THE 4 MAR *** $TEST_TEXT_LONG",
            result.toString()
        )
    }

    @Test
    public fun testParcelPlainText() {
        val text1 = ComplicationText.plainText(TEST_TEXT1)
        val longText = ComplicationText.plainText(TEST_TEXT_LONG)
        val template = ComplicationTextTemplate.Builder()
            .addComplicationText(text1)
            .addComplicationText(longText)
            .setSurroundingText("^1 : ^2")
            .build()
        val newText = template.roundTripParcelable()!!

        Assert.assertEquals(
            "$TEST_TEXT1 : $TEST_TEXT_LONG",
            newText.getTextAt(mResources, 100000).toString()
        )
    }

    @Test
    public fun testParcelTimeDifferenceTextWithPlainTextAndTemplate() {
        val refTime: Long = 10000000
        val originalText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(refTime - 156561)
                .setReferencePeriodEndMillis(refTime)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                .setSurroundingText("hello ^1 time")
                .setShowNowText(false)
                .build()
        val longText = ComplicationText.plainText(TEST_TEXT_LONG)
        val template = ComplicationTextTemplate.Builder()
            .addComplicationText(originalText)
            .addComplicationText(longText)
            .setSurroundingText("^1 : ^2")
            .build()
        val newText = template.roundTripParcelable()!!
        val testTime =
            refTime + TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(35)
        Assert.assertEquals(
            "hello 3h time : $TEST_TEXT_LONG",
            newText.getTextAt(mResources, testTime).toString()
        )
        Assert.assertEquals(
            "hello 0m time : $TEST_TEXT_LONG",
            newText.getTextAt(mResources, refTime).toString()
        )
    }

    @Test
    public fun nextChangeTimeDifferenceAndFormat() {
        val diffText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(0)
                .setReferencePeriodEndMillis(0)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                .build()
        val formatText =
            TimeFormatBuilder()
                .setFormat("EEE 'the' d LLL HH")
                .setStyle(ComplicationText.FORMAT_STYLE_LOWER_CASE)
                .build()
        val template = ComplicationTextTemplate.Builder()
            .addComplicationText(diffText)
            .addComplicationText(formatText)
            .setSurroundingText("^1 : ^2")
            .build()

        // Next change comes from the time difference, so is next minute boundary + 1ms.
        Truth.assertThat(template.getNextChangeTime(60000000123L))
            .isEqualTo(60000060001L)
    }

    @Test
    public fun nextChangeTimeDifferenceAndPlain() {
        val diffText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(0)
                .setReferencePeriodEndMillis(0)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                .build()
        val plainText =
            ComplicationText.plainText("hello")
        val template = ComplicationTextTemplate.Builder()
            .addComplicationText(diffText)
            .addComplicationText(plainText)
            .setSurroundingText("^1 : ^2")
            .build()
        Truth.assertThat(template.getNextChangeTime(60000000123L))
            .isEqualTo(60000060001L)
    }

    @Test
    public fun nextChangeTimeFormatAndPlain() {
        val formatText =
            TimeFormatBuilder()
                .setFormat("EEE 'the' d LLL HH:mm")
                .setStyle(ComplicationText.FORMAT_STYLE_LOWER_CASE)
                .build()
        val plainText =
            ComplicationText.plainText("hello")
        val template = ComplicationTextTemplate.Builder()
            .addComplicationText(plainText)
            .addComplicationText(formatText)
            .setSurroundingText("^1 : ^2")
            .build()
        Truth.assertThat(template.getNextChangeTime(60000000123L))
            .isEqualTo(60000060000L)
    }

    private companion object {
        private const val TEST_TEXT1 = "Hello"
        private const val TEST_TEXT2 = "darkness"
        private const val TEST_TEXT3 = "old friend"
        private const val TEST_TEXT_LONG = "Lovely weather we're having :)"
    }
}
