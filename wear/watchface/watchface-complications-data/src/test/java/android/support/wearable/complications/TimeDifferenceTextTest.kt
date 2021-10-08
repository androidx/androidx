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
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.complications.data.SharedRobolectricTestRunner
import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(SharedRobolectricTestRunner::class)
public class TimeDifferenceTextTest {
    private val mResources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    public fun testShortSingleUnitAfterRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_SINGLE_UNIT
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            true,
            null
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style.
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1m", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("13m", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.HOURS.toMillis(4)
        Assert.assertEquals("5h", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.HOURS.toMillis(10)
        Assert.assertEquals("15h", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.DAYS.toMillis(3)
        Assert.assertEquals("4d", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.DAYS.toMillis(10)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testShortDualUnitAfterRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_DUAL_UNIT
        val refTime: Long = 10000000
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            true,
            null
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style.
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1m", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("13m", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.HOURS.toMillis(4)
        Assert.assertEquals("4h 13m", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.HOURS.toMillis(10)
        Assert.assertEquals("14h 13m", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.DAYS.toMillis(3)
        Assert.assertEquals("3d 15h", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.DAYS.toMillis(10)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testStopwatchAfterRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_STOPWATCH
        val refTime = 70000000000L
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_STOPWATCH,
            true,
            null
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style.
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("00:35", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("12:35", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.HOURS.toMillis(4)
        Assert.assertEquals("4:13", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.HOURS.toMillis(10)
        Assert.assertEquals("14:13", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.DAYS.toMillis(3)
        Assert.assertEquals("3d 15h", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.DAYS.toMillis(10)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testWordsSingleUnitAfterRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_WORDS_SINGLE_UNIT
        val refTime = 990000000000L
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT,
            true,
            null
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style.
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1 min", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.MINUTES.toMillis(1)
        Assert.assertEquals("2 mins", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.MINUTES.toMillis(11)
        Assert.assertEquals("13 mins", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.HOURS.toMillis(1)
        Assert.assertEquals("2 hours", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.HOURS.toMillis(4)
        Assert.assertEquals("6 hours", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.HOURS.toMillis(18)
        Assert.assertEquals("1 day", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.DAYS.toMillis(1)
        Assert.assertEquals("2 days", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.DAYS.toMillis(10)
        Assert.assertEquals("12 days", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testShortWordsSingleUnitAfterRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_WORDS_SINGLE_UNIT
        val refTime = 990000000000L
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT,
            true,
            null
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style.
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1 min", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(1) + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("2 mins", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(12) + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("13 mins", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(1) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("2 hours", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(9) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("10h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(23) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(
                35
            )
        Assert.assertEquals("1 day", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(1) +
            TimeUnit.HOURS.toMillis(23) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("2 days", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(11) +
            TimeUnit.HOURS.toMillis(23) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("12 days", tdt.getTextAt(mResources, testTime))
        testTime = refTime + TimeUnit.DAYS.toMillis(125)
        Assert.assertEquals("125d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testShortSingleUnitBeforeRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_SINGLE_UNIT
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime,
            123456789,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            true,
            null
        )

        // WHEN getText is called for times before the end of the ref period...
        // THEN the result is formatted according to the style.
        var testTime = refTime - TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1m", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("13m", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.HOURS.toMillis(4)
        Assert.assertEquals("5h", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.HOURS.toMillis(10)
        Assert.assertEquals("15h", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.DAYS.toMillis(3)
        Assert.assertEquals("4d", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.DAYS.toMillis(10)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testShortDualUnitBeforeRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_DUAL_UNIT
        val refTime: Long = 10000000
        val tdt = TimeDifferenceText(
            refTime,
            refTime + 1000,
            ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            true,
            null
        )

        // WHEN getText is called for times before the end of the ref period...
        // THEN the result is formatted according to the style.
        var testTime = refTime - TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1m", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("13m", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.HOURS.toMillis(4)
        Assert.assertEquals("4h 13m", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.HOURS.toMillis(10)
        Assert.assertEquals("14h 13m", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.DAYS.toMillis(3)
        Assert.assertEquals("3d 15h", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.DAYS.toMillis(10)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testStopwatchBeforeRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_STOPWATCH
        val refTime = 70000000000L
        val tdt = TimeDifferenceText(
            refTime,
            refTime + 1569456,
            ComplicationText.DIFFERENCE_STYLE_STOPWATCH,
            true,
            null
        )

        // WHEN getText is called for times before the end of the ref period...
        // THEN the result is formatted according to the style.
        var testTime = refTime - TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("00:35", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("12:35", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.HOURS.toMillis(4)
        Assert.assertEquals("4:13", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.HOURS.toMillis(10)
        Assert.assertEquals("14:13", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.DAYS.toMillis(3)
        Assert.assertEquals("3d 15h", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.DAYS.toMillis(10)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testWordsSingleUnitBeforeRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_WORDS_SINGLE_UNIT
        val refTime = 990000000000L
        val tdt = TimeDifferenceText(
            refTime,
            refTime + 654654,
            ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT,
            true,
            null
        )

        // WHEN getText is called for times before the end of the ref period...
        // THEN the result is formatted according to the style.
        var testTime = refTime - TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1 min", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.MINUTES.toMillis(1)
        Assert.assertEquals("2 mins", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.MINUTES.toMillis(11)
        Assert.assertEquals("13 mins", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.HOURS.toMillis(1)
        Assert.assertEquals("2 hours", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.HOURS.toMillis(4)
        Assert.assertEquals("6 hours", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.HOURS.toMillis(18)
        Assert.assertEquals("1 day", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.DAYS.toMillis(1)
        Assert.assertEquals("2 days", tdt.getTextAt(mResources, testTime))
        testTime -= TimeUnit.DAYS.toMillis(10)
        Assert.assertEquals("12 days", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testDuringRefPeriodShowingNowText() {
        // GIVEN TimeDifferenceText
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime,
            refTime + 100000,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            true,
            null
        )

        // WHEN getText is called for a time within the ref period
        // THEN "Now" is returned.
        Assert.assertEquals("Now", tdt.getTextAt(mResources, refTime + 1000))
    }

    @Test
    public fun testDuringRefPeriodNotShowingNowTextShortSingle() {
        // GIVEN TimeDifferenceText
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime,
            refTime + 100000,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            false,
            null
        )

        // WHEN getText is called for a time within the ref period
        // THEN a zero value is returned.
        Assert.assertEquals("0m", tdt.getTextAt(mResources, refTime + 1000))
    }

    @Test
    public fun testDuringRefPeriodNotShowingNowTextShortDual() {
        // GIVEN TimeDifferenceText
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime,
            refTime + 100000,
            ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            false,
            null
        )

        // WHEN getText is called for a time within the ref period
        // THEN a zero value is returned.
        Assert.assertEquals("0m", tdt.getTextAt(mResources, refTime + 1000))
    }

    @Test
    public fun testDuringRefPeriodNotShowingNowTextWord() {
        // GIVEN TimeDifferenceText
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime,
            refTime + 100000,
            ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT,
            false,
            null
        )

        // WHEN getText is called for a time within the ref period
        // THEN a zero value is returned.
        Assert.assertEquals("0 mins", tdt.getTextAt(mResources, refTime + 1000))
    }

    @Test
    public fun testDuringRefPeriodNotShowingNowTextStopwatch() {
        // GIVEN TimeDifferenceText
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime,
            refTime + 100000,
            ComplicationText.DIFFERENCE_STYLE_STOPWATCH,
            false,
            null
        )

        // WHEN getText is called for a time within the ref period
        // THEN a zero value is returned.
        Assert.assertEquals("00:00", tdt.getTextAt(mResources, refTime + 1000))
    }

    @Test
    public fun testDuringRefPeriodShowingNowTextStopwatch() {
        // GIVEN TimeDifferenceText
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime,
            refTime + 100000,
            ComplicationText.DIFFERENCE_STYLE_STOPWATCH,
            true,
            null
        )

        // WHEN getText is called for a time within the ref period
        // THEN "Now" is returned.
        Assert.assertEquals("Now", tdt.getTextAt(mResources, refTime + 1000))
    }

    @Test
    public fun testAtRefPeriodStartTime() {
        // GIVEN TimeDifferenceText
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime,
            refTime + 100000,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            true,
            null
        )

        // WHEN getText is called for the ref period start time
        // THEN "Now" is returned.
        Assert.assertEquals("Now", tdt.getTextAt(mResources, refTime))
    }

    @Test
    public fun testAtRefPeriodEndTime() {
        // GIVEN TimeDifferenceText
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime - 100000,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            true,
            null
        )

        // WHEN getText is called for the ref period end time
        // THEN "Now" is returned.
        Assert.assertEquals("Now", tdt.getTextAt(mResources, refTime))
    }

    @Test
    public fun testReturnsSameTextWithStopwatchStyle() {
        // GIVEN TimeDifferenceText
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime - 100000,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_STOPWATCH,
            true,
            null
        )

        // WHEN we consider two times for which the text would differ.
        // THEN returnsSameText returns false.
        var testTime1 = refTime + TimeUnit.SECONDS.toMillis(10)
        var testTime2 = refTime + TimeUnit.SECONDS.toMillis(15)
        Assert.assertFalse(tdt.returnsSameText(testTime1, testTime2))

        // WHEN we consider two times for which the text would be the same.
        // THEN returnsSameText returns true.
        testTime1 = refTime + TimeUnit.SECONDS.toMillis(10) + 100
        testTime2 = refTime + TimeUnit.SECONDS.toMillis(10) + 600
        Assert.assertTrue(tdt.returnsSameText(testTime1, testTime2))
    }

    @Test
    public fun testReturnsSameTextWithShortSingleStyle() {
        // GIVEN TimeDifferenceText
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime - 100000,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            true,
            null
        )

        // WHEN we consider two times for which the text would differ.
        // THEN returnsSameText returns false.
        var testTime1 = refTime + TimeUnit.MINUTES.toMillis(10)
        var testTime2 = refTime + TimeUnit.MINUTES.toMillis(12)
        Assert.assertFalse(tdt.returnsSameText(testTime1, testTime2))

        // WHEN we consider two times for which the text would be the same.
        // THEN returnsSameText returns true.
        testTime1 = refTime +
            TimeUnit.MINUTES.toMillis(10) + TimeUnit.SECONDS.toMillis(10)
        testTime2 = refTime +
            TimeUnit.MINUTES.toMillis(10) + TimeUnit.SECONDS.toMillis(15)
        Assert.assertTrue(tdt.returnsSameText(testTime1, testTime2))
    }

    @Test
    public fun testReturnsSameTextShortDualStyle() {
        // GIVEN TimeDifferenceText
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime - 100000,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            true,
            null
        )

        // WHEN we consider two times for which the text would differ.
        // THEN returnsSameText returns false.
        var testTime1 = refTime + TimeUnit.MINUTES.toMillis(10)
        var testTime2 = refTime + TimeUnit.MINUTES.toMillis(12)
        Assert.assertFalse(tdt.returnsSameText(testTime1, testTime2))

        // WHEN we consider two times for which the text would be the same.
        // THEN returnsSameText returns true.
        testTime1 = refTime +
            TimeUnit.MINUTES.toMillis(10) + TimeUnit.SECONDS.toMillis(10)
        testTime2 = refTime +
            TimeUnit.MINUTES.toMillis(10) + TimeUnit.SECONDS.toMillis(15)
        Assert.assertTrue(tdt.returnsSameText(testTime1, testTime2))
    }

    @Test
    public fun testReturnsSameTextWordsSingleStyle() {
        // GIVEN TimeDifferenceText
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime - 100000,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT,
            true,
            null
        )

        // WHEN we consider two times for which the text would differ.
        // THEN returnsSameText returns false.
        var testTime1 = refTime + TimeUnit.MINUTES.toMillis(10)
        var testTime2 = refTime + TimeUnit.MINUTES.toMillis(12)
        Assert.assertFalse(tdt.returnsSameText(testTime1, testTime2))

        // WHEN we consider two times for which the text would be the same.
        // THEN returnsSameText returns true.
        testTime1 = refTime + TimeUnit.MINUTES.toMillis(10) + TimeUnit.SECONDS.toMillis(10)
        testTime2 = refTime + TimeUnit.MINUTES.toMillis(10) + TimeUnit.SECONDS.toMillis(15)
        Assert.assertTrue(tdt.returnsSameText(testTime1, testTime2))
    }

    @Test
    public fun testReturnsSameTextWithStopwatchStyleMinuteMinimumUnit() {
        // GIVEN TimeDifferenceText
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime - 100000,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_STOPWATCH,
            true,
            TimeUnit.MINUTES
        )

        // WHEN we consider two times for which the text would differ without the minimum unit,
        // but now will not differ
        // THEN returnsSameText returns true.
        var testTime1 = refTime + TimeUnit.SECONDS.toMillis(10)
        var testTime2 = refTime + TimeUnit.SECONDS.toMillis(15)
        Assert.assertTrue(tdt.returnsSameText(testTime1, testTime2))

        // WHEN we consider two times for which the text would differ.
        // THEN returnsSameText returns false.
        testTime1 = refTime + TimeUnit.MINUTES.toMillis(10)
        testTime2 = refTime + TimeUnit.MINUTES.toMillis(12)
        Assert.assertFalse(tdt.returnsSameText(testTime1, testTime2))
    }

    @Test
    public fun testReturnsSameTextWithShortSingleStyleHourMinimumUnit() {
        // GIVEN TimeDifferenceText
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime - 100000,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            true,
            TimeUnit.HOURS
        )

        // WHEN we consider two times for which the text would differ without the minimum unit,
        // but now will not differ
        // THEN returnsSameText returns true.
        var testTime1 = refTime + TimeUnit.MINUTES.toMillis(10)
        var testTime2 = refTime + TimeUnit.MINUTES.toMillis(12)
        Assert.assertTrue(tdt.returnsSameText(testTime1, testTime2))

        // WHEN we consider two times for which the text would differ.
        // THEN returnsSameText returns false.
        testTime1 = refTime + TimeUnit.HOURS.toMillis(10)
        testTime2 = refTime + TimeUnit.HOURS.toMillis(12)
        Assert.assertFalse(tdt.returnsSameText(testTime1, testTime2))
    }

    @Test
    public fun testReturnsSameTextWithShortDualStyleHourMinimumUnit() {
        // GIVEN TimeDifferenceText
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime - 100000,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            true,
            TimeUnit.HOURS
        )

        // WHEN we consider two times for which the text would differ without the minimum unit,
        // but now will not differ
        // THEN returnsSameText returns true.
        var testTime1 = refTime + TimeUnit.MINUTES.toMillis(10)
        var testTime2 = refTime + TimeUnit.MINUTES.toMillis(12)
        Assert.assertTrue(tdt.returnsSameText(testTime1, testTime2))

        // WHEN we consider two times for which the text would differ.
        // THEN returnsSameText returns false.
        testTime1 = refTime + TimeUnit.HOURS.toMillis(10)
        testTime2 = refTime + TimeUnit.HOURS.toMillis(12)
        Assert.assertFalse(tdt.returnsSameText(testTime1, testTime2))
    }

    @Test
    public fun testReturnsSameTextWithShortSingleStyleDayMinimumUnit() {
        // GIVEN TimeDifferenceText
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime - 100000,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            true,
            TimeUnit.DAYS
        )

        // WHEN we consider two times for which the text differs by a number of minutes
        // THEN returnsSameText returns true.
        var testTime1 = refTime + TimeUnit.MINUTES.toMillis(10)
        var testTime2 = refTime + TimeUnit.MINUTES.toMillis(12)
        Assert.assertTrue(tdt.returnsSameText(testTime1, testTime2))

        // WHEN we consider two times for which the text differs by a number of hours
        // THEN returnsSameText returns true.
        testTime1 = refTime + TimeUnit.HOURS.toMillis(10)
        testTime2 = refTime + TimeUnit.HOURS.toMillis(12)
        Assert.assertTrue(tdt.returnsSameText(testTime1, testTime2))

        // WHEN we consider two times for which the text differs by a number of days
        // THEN returnsSameText returns false.
        testTime1 = refTime + TimeUnit.DAYS.toMillis(10)
        testTime2 = refTime + TimeUnit.DAYS.toMillis(12)
        Assert.assertFalse(tdt.returnsSameText(testTime1, testTime2))
    }

    @Test
    public fun testReturnsSameTextWithWordStyleDayMinimumUnit() {
        // GIVEN TimeDifferenceText
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            refTime - 100000,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT,
            true,
            TimeUnit.DAYS
        )

        // WHEN we consider two times for which the text differs by a number of minutes
        // THEN returnsSameText returns true.
        var testTime1 = refTime + TimeUnit.MINUTES.toMillis(10)
        var testTime2 = refTime + TimeUnit.MINUTES.toMillis(12)
        Assert.assertTrue(tdt.returnsSameText(testTime1, testTime2))

        // WHEN we consider two times for which the text differs by a number of hours
        // THEN returnsSameText returns true.
        testTime1 = refTime + TimeUnit.HOURS.toMillis(10)
        testTime2 = refTime + TimeUnit.HOURS.toMillis(12)
        Assert.assertTrue(tdt.returnsSameText(testTime1, testTime2))

        // WHEN we consider two times for which the text differs by a number of days
        // THEN returnsSameText returns false.
        testTime1 = refTime + TimeUnit.DAYS.toMillis(10)
        testTime2 = refTime + TimeUnit.DAYS.toMillis(12)
        Assert.assertFalse(tdt.returnsSameText(testTime1, testTime2))
    }

    @Test
    public fun testShortSingleUnitRoundingFromSmallUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_SINGLE_UNIT
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            true,
            null
        )

        // WHEN getText is called for a time a few seconds more than a whole number of hours after
        // the
        // ref time
        var testTime = refTime +
            TimeUnit.HOURS.toMillis(4) + TimeUnit.SECONDS.toMillis(35)
        // THEN the time is rounded up to the next hour
        Assert.assertEquals("5h", tdt.getTextAt(mResources, testTime))

        // WHEN getText is called for a time a few seconds more than a whole number of days after
        // the
        // ref time
        testTime = refTime +
            TimeUnit.DAYS.toMillis(12) + TimeUnit.SECONDS.toMillis(35)
        // THEN the text is rounded up to the next day
        Assert.assertEquals("13d", tdt.getTextAt(mResources, testTime))

        // WHEN getText is called for a time a few minutes more than a whole number of days after
        // the
        // ref time
        testTime = refTime +
            TimeUnit.DAYS.toMillis(12) + TimeUnit.MINUTES.toMillis(35)
        // THEN the text is rounded up to the next day
        Assert.assertEquals("13d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testShortSingleUnitExactNumberOfUnits() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_SINGLE_UNIT
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            true,
            null
        )

        // WHEN getText is called for a time a whole number of minutes after the ref time
        var testTime = refTime + TimeUnit.MINUTES.toMillis(35)
        // THEN the output shows that number of minutes.
        Assert.assertEquals("35m", tdt.getTextAt(mResources, testTime))

        // WHEN getText is called for a time a whole number of hours after the ref time
        testTime = refTime + TimeUnit.HOURS.toMillis(12)
        // THEN the output shows that number of hours.
        Assert.assertEquals("12h", tdt.getTextAt(mResources, testTime))

        // WHEN getText is called for a time a whole number of days after the ref time
        testTime = refTime + TimeUnit.DAYS.toMillis(3)
        // THEN the output shows that number of days.
        Assert.assertEquals("3d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testDefaultStyle() {
        // GIVEN TimeDifferenceText with an unrecognised style
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(0, refTime, 123456, true, null)

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the default style.
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1m", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("13m", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.HOURS.toMillis(4)
        Assert.assertEquals("5h", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.HOURS.toMillis(10)
        Assert.assertEquals("15h", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.DAYS.toMillis(3)
        Assert.assertEquals("4d", tdt.getTextAt(mResources, testTime))
        testTime += TimeUnit.DAYS.toMillis(10)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testShortDualUnitDoesntShowZeroInSmallerUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_DUAL_UNIT
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            true,
            null
        )

        // WHEN getText is called for a time a whole number of hours after the ref time
        var testTime = refTime + TimeUnit.HOURS.toMillis(4)
        // THEN the text shows hours but not minutes.
        Assert.assertEquals("4h", tdt.getTextAt(mResources, testTime))

        // WHEN getText is called for a time a whole number of days after the ref time
        testTime = refTime + TimeUnit.DAYS.toMillis(35)
        // THEN the text shows days but not hours.
        Assert.assertEquals("35d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testShortSingleUnitWithHourMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_SINGLE_UNIT
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            true,
            TimeUnit.HOURS
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // an hour
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(4) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("5h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("15h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(3) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("4d", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(13) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testShortSingleUnitWithDayMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_SINGLE_UNIT
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            true,
            TimeUnit.DAYS
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // an hour
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1d", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1d", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(4) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1d", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1d", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(3) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("4d", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(13) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testShortSingleUnitWithMinuteMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_SINGLE_UNIT
        val refTime: Long = 1000000
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            true,
            TimeUnit.MINUTES
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, with no change due to the minimum
        // unit.
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1m", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("13m", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(4) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("5h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("15h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(3) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("4d", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(13) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testShortDualUnitWithHourMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_DUAL_UNIT
        val refTime: Long = 10000000
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            true,
            TimeUnit.HOURS
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // an hour
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(4) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("5h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("15h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(3) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("3d 15h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(13) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testShortDualUnitWithDayMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_DUAL_UNIT
        val refTime: Long = 10000000
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            true,
            TimeUnit.DAYS
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // a day
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1d", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1d", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(4) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1d", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1d", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(3) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("4d", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(13) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testShortDualUnitWithMinuteMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_DUAL_UNIT
        val refTime: Long = 10000000
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            true,
            TimeUnit.MINUTES
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, with no change due to the minimum
        // unit.
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1m", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("13m", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(4) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("4h 13m", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("14h 13m", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(3) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("3d 15h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(13) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testShortDualUnitWithSecondMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_DUAL_UNIT
        val refTime: Long = 10000000
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            true,
            TimeUnit.SECONDS
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, with no change due to the minimum
        // unit.
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1m", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("13m", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(4) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("4h 13m", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("14h 13m", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(3) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("3d 15h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(13) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testStopwatchWithMinuteMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_STOPWATCH
        val refTime = 70000000000L
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_STOPWATCH,
            true,
            TimeUnit.MINUTES
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // a minute.
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("0:01", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("0:13", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(4) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("4:13", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("14:13", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(3) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("3d 15h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(13) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testStopwatchWithHourMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_STOPWATCH
        val refTime = 70000000000L
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_STOPWATCH,
            true,
            TimeUnit.HOURS
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // an hour (which means that the stopwatch-style format hh:mm cannot be used).
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(4) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("5h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("15h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(3) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("3d 15h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(13) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testStopwatchWithDayMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_STOPWATCH
        val refTime = 70000000000L
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_STOPWATCH,
            true,
            TimeUnit.DAYS
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // a day..
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1d", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1d", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(4) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1d", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1d", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(3) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("4d", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(13) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testStopwatchWithSecondMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_STOPWATCH
        val refTime = 70000000000L
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_STOPWATCH,
            true,
            TimeUnit.SECONDS
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, with no change due to the minimum
        // unit.
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("00:35", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("12:35", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(4) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("4:13", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("14:13", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(3) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("3d 15h", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(13) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("14d", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testWordsSingleUnitWithHourMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_WORDS_SINGLE_UNIT
        val refTime = 990000000000L
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT,
            true,
            TimeUnit.HOURS
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // an hour
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1 hour", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1 hour", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(4) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("5 hours", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("15 hours", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(3) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("4 days", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(13) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("14 days", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testWordsSingleUnitWithDayMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_WORDS_SINGLE_UNIT
        val refTime = 990000000000L
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT,
            true,
            TimeUnit.DAYS
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // an hour
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1 day", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1 day", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(4) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1 day", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1 day", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(3) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("4 days", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.DAYS.toMillis(13) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("14 days", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testWordsSingleUnitWithMinuteMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_WORDS_SINGLE_UNIT
        val refTime = 990000000000L
        val tdt = TimeDifferenceText(
            0,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT,
            true,
            TimeUnit.MINUTES
        )

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, with no change due to the minimum
        // unit.
        var testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("1 min", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("13 mins", tdt.getTextAt(mResources, testTime))
        testTime = refTime +
            TimeUnit.HOURS.toMillis(4) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("5 hours", tdt.getTextAt(mResources, testTime))
        testTime = refTime + TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12) +
            TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals("15 hours", tdt.getTextAt(mResources, testTime))
        testTime = refTime + TimeUnit.DAYS.toMillis(3) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("4 days", tdt.getTextAt(mResources, testTime))
        testTime = refTime + TimeUnit.DAYS.toMillis(13) +
            TimeUnit.HOURS.toMillis(14) +
            TimeUnit.MINUTES.toMillis(12)
        Assert.assertEquals("14 days", tdt.getTextAt(mResources, testTime))
    }

    @Test
    public fun testTimeDifferenceGetNextChangeStopWatchNoMinimum() {
        val text = TimeDifferenceText(
            0,
            1,
            ComplicationText.DIFFERENCE_STYLE_STOPWATCH,
            true,
            null
        )
        Assert.assertEquals(
            TimeUnit.SECONDS.toMillis(1),
            text.precision
        )

        // Time difference is rounded up, so the next change is 1ms after the next second boundary.
        Truth.assertThat(text.getNextChangeTime(1000000000L))
            .isEqualTo(1000000001L)
        Truth.assertThat(text.getNextChangeTime(1000000001L))
            .isEqualTo(1000001001L)
        Truth.assertThat(text.getNextChangeTime(1000001234L))
            .isEqualTo(1000002001L)
    }

    @Test
    public fun testTimeDifferenceGetNextChangeNoMinimum() {
        val styles = intArrayOf(
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT,
            ComplicationText.DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT
        )
        for (style in styles) {
            val text = TimeDifferenceText(0, 1, style, true, null)
            Assert.assertEquals(
                TimeUnit.MINUTES.toMillis(1),
                text.precision
            )

            // Time difference is rounded up, and precision for all of these is 1 minute, so the
            // next
            // change is 1ms after the next minute boundary.
            Truth.assertThat(text.getNextChangeTime(60000000000L))
                .isEqualTo(60000000001L)
            Truth.assertThat(text.getNextChangeTime(60000000001L))
                .isEqualTo(60000060001L)
            Truth.assertThat(text.getNextChangeTime(60000060000L))
                .isEqualTo(60000060001L)
        }
    }

    @Test
    public fun testTimeDifferenceGetNextChangeMinuteMinimum() {
        val styles = intArrayOf(
            ComplicationText.DIFFERENCE_STYLE_STOPWATCH,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT,
            ComplicationText.DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT
        )
        for (style in styles) {
            val text =
                TimeDifferenceText(0, 1, style, true, TimeUnit.MINUTES)
            Assert.assertEquals(
                TimeUnit.MINUTES.toMillis(1),
                text.precision
            )

            // Next change is 1ms after the next minute boundary.
            Truth.assertThat(text.getNextChangeTime(60000000000L))
                .isEqualTo(60000000001L)
            Truth.assertThat(text.getNextChangeTime(60000000001L))
                .isEqualTo(60000060001L)
            Truth.assertThat(text.getNextChangeTime(60000060000L))
                .isEqualTo(60000060001L)
        }
    }

    @Test
    public fun testTimeDifferenceGetNextChangeHourMinimum() {
        val styles = intArrayOf(
            ComplicationText.DIFFERENCE_STYLE_STOPWATCH,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT,
            ComplicationText.DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT
        )
        for (style in styles) {
            val text =
                TimeDifferenceText(0, 1, style, true, TimeUnit.HOURS)
            Assert.assertEquals(
                TimeUnit.HOURS.toMillis(1),
                text.precision
            )

            // Next change is 1ms after the next hour boundary.
            Truth.assertThat(text.getNextChangeTime(36000035789L))
                .isEqualTo(36003600001L)
            Truth.assertThat(text.getNextChangeTime(36003600000L))
                .isEqualTo(36003600001L)
            Truth.assertThat(text.getNextChangeTime(36003600001L))
                .isEqualTo(36007200001L)
            Truth.assertThat(text.getNextChangeTime(36007199999L))
                .isEqualTo(36007200001L)
        }
    }

    @Test
    public fun testTimeDifferenceGetPrecisionDayMinimum() {
        val styles = intArrayOf(
            ComplicationText.DIFFERENCE_STYLE_STOPWATCH,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT,
            ComplicationText.DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT
        )
        for (style in styles) {
            val text =
                TimeDifferenceText(0, 1, style, true, TimeUnit.DAYS)
            Assert.assertEquals(
                TimeUnit.DAYS.toMillis(1),
                text.precision
            )

            // Next change is 1ms after the next day boundary.
            Truth.assertThat(text.getNextChangeTime(8640000035789L))
                .isEqualTo(8640086400001L)
            Truth.assertThat(text.getNextChangeTime(8640086400000L))
                .isEqualTo(8640086400001L)
            Truth.assertThat(text.getNextChangeTime(8640086400001L))
                .isEqualTo(8640172800001L)
        }
    }

    @Test
    public fun testParcelTimeDifferenceTextWithoutMinUnit() {
        // GIVEN TimeDifferenceText without min unit
        val refTime: Long = 10000000
        val originalText = TimeDifferenceText(
            refTime - 156561,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            false,
            null
        )

        // WHEN the object is parcelled and unparcelled
        val newText = originalText.roundTripParcelable()!!

        // THEN the object behaves as expected.
        val testTime = refTime +
            TimeUnit.HOURS.toMillis(2) +
            TimeUnit.MINUTES.toMillis(35)
        Assert.assertEquals("3h", newText.getTextAt(mResources, testTime).toString())
        Assert.assertEquals("0m", newText.getTextAt(mResources, refTime).toString())
    }

    @Test
    public fun testParcelTimeDifferenceTextWithMinUnit() {
        // GIVEN TimeDifferenceText with a minimum unit specified
        val refTime: Long = 10000000
        val originalText = TimeDifferenceText(
            refTime - 156561,
            refTime,
            ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            true,
            TimeUnit.HOURS
        )

        // WHEN the object is parcelled and unparcelled
        val newText = originalText.roundTripParcelable()!!

        // THEN the object behaves as expected.
        val testTime = refTime + TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(35)
        Assert.assertEquals("3h", newText.getTextAt(mResources, testTime).toString())
    }
}