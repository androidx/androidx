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

package android.support.wearable.complications;

import static android.support.wearable.complications.ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT;
import static android.support.wearable.complications.ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT;
import static android.support.wearable.complications.ComplicationText.DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT;
import static android.support.wearable.complications.ComplicationText.DIFFERENCE_STYLE_STOPWATCH;
import static android.support.wearable.complications.ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class TimeDifferenceTextTest {

    private Resources mResources = ApplicationProvider.getApplicationContext().getResources();

    @Test
    public void testShortSingleUnitAfterRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_SINGLE_UNIT
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_SHORT_SINGLE_UNIT, true, null);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style.
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("1m", tdt.getText(mResources, testTime));

        testTime += MINUTES.toMillis(12);
        assertEquals("13m", tdt.getText(mResources, testTime));

        testTime += HOURS.toMillis(4);
        assertEquals("5h", tdt.getText(mResources, testTime));

        testTime += HOURS.toMillis(10);
        assertEquals("15h", tdt.getText(mResources, testTime));

        testTime += DAYS.toMillis(3);
        assertEquals("4d", tdt.getText(mResources, testTime));

        testTime += DAYS.toMillis(10);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testShortDualUnitAfterRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_DUAL_UNIT
        long refTime = 10000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_SHORT_DUAL_UNIT, true, null);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style.
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("1m", tdt.getText(mResources, testTime));

        testTime += MINUTES.toMillis(12);
        assertEquals("13m", tdt.getText(mResources, testTime));

        testTime += HOURS.toMillis(4);
        assertEquals("4h 13m", tdt.getText(mResources, testTime));

        testTime += HOURS.toMillis(10);
        assertEquals("14h 13m", tdt.getText(mResources, testTime));

        testTime += DAYS.toMillis(3);
        assertEquals("3d 15h", tdt.getText(mResources, testTime));

        testTime += DAYS.toMillis(10);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testStopwatchAfterRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_STOPWATCH
        long refTime = 70000000000L;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_STOPWATCH, true, null);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style.
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("00:35", tdt.getText(mResources, testTime));

        testTime += MINUTES.toMillis(12);
        assertEquals("12:35", tdt.getText(mResources, testTime));

        testTime += HOURS.toMillis(4);
        assertEquals("4:13", tdt.getText(mResources, testTime));

        testTime += HOURS.toMillis(10);
        assertEquals("14:13", tdt.getText(mResources, testTime));

        testTime += DAYS.toMillis(3);
        assertEquals("3d 15h", tdt.getText(mResources, testTime));

        testTime += DAYS.toMillis(10);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testWordsSingleUnitAfterRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_WORDS_SINGLE_UNIT
        long refTime = 990000000000L;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_WORDS_SINGLE_UNIT, true, null);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style.
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("1 min", tdt.getText(mResources, testTime));

        testTime += MINUTES.toMillis(1);
        assertEquals("2 mins", tdt.getText(mResources, testTime));

        testTime += MINUTES.toMillis(11);
        assertEquals("13 mins", tdt.getText(mResources, testTime));

        testTime += HOURS.toMillis(1);
        assertEquals("2 hours", tdt.getText(mResources, testTime));

        testTime += HOURS.toMillis(4);
        assertEquals("6 hours", tdt.getText(mResources, testTime));

        testTime += HOURS.toMillis(18);
        assertEquals("1 day", tdt.getText(mResources, testTime));

        testTime += DAYS.toMillis(1);
        assertEquals("2 days", tdt.getText(mResources, testTime));

        testTime += DAYS.toMillis(10);
        assertEquals("12 days", tdt.getText(mResources, testTime));
    }

    @Test
    public void testShortWordsSingleUnitAfterRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_WORDS_SINGLE_UNIT
        long refTime = 990000000000L;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        0, refTime, DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT, true, null);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style.
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("1 min", tdt.getText(mResources, testTime));

        testTime = refTime + MINUTES.toMillis(1) + SECONDS.toMillis(35);
        assertEquals("2 mins", tdt.getText(mResources, testTime));

        testTime = refTime + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("13 mins", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(1) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("2 hours", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(9) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("10h", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(23) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("1 day", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(1) + HOURS.toMillis(23) + MINUTES.toMillis(12);
        assertEquals("2 days", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(11) + HOURS.toMillis(23) + MINUTES.toMillis(12);
        assertEquals("12 days", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(125);
        assertEquals("125d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testShortSingleUnitBeforeRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_SINGLE_UNIT
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime, 123456789, DIFFERENCE_STYLE_SHORT_SINGLE_UNIT, true, null);

        // WHEN getText is called for times before the end of the ref period...
        // THEN the result is formatted according to the style.
        long testTime = refTime - SECONDS.toMillis(35);
        assertEquals("1m", tdt.getText(mResources, testTime));

        testTime -= MINUTES.toMillis(12);
        assertEquals("13m", tdt.getText(mResources, testTime));

        testTime -= HOURS.toMillis(4);
        assertEquals("5h", tdt.getText(mResources, testTime));

        testTime -= HOURS.toMillis(10);
        assertEquals("15h", tdt.getText(mResources, testTime));

        testTime -= DAYS.toMillis(3);
        assertEquals("4d", tdt.getText(mResources, testTime));

        testTime -= DAYS.toMillis(10);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testShortDualUnitBeforeRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_DUAL_UNIT
        long refTime = 10000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime, refTime + 1000, DIFFERENCE_STYLE_SHORT_DUAL_UNIT, true, null);

        // WHEN getText is called for times before the end of the ref period...
        // THEN the result is formatted according to the style.
        long testTime = refTime - SECONDS.toMillis(35);
        assertEquals("1m", tdt.getText(mResources, testTime));

        testTime -= MINUTES.toMillis(12);
        assertEquals("13m", tdt.getText(mResources, testTime));

        testTime -= HOURS.toMillis(4);
        assertEquals("4h 13m", tdt.getText(mResources, testTime));

        testTime -= HOURS.toMillis(10);
        assertEquals("14h 13m", tdt.getText(mResources, testTime));

        testTime -= DAYS.toMillis(3);
        assertEquals("3d 15h", tdt.getText(mResources, testTime));

        testTime -= DAYS.toMillis(10);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testStopwatchBeforeRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_STOPWATCH
        long refTime = 70000000000L;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime, refTime + 1569456, DIFFERENCE_STYLE_STOPWATCH, true, null);

        // WHEN getText is called for times before the end of the ref period...
        // THEN the result is formatted according to the style.
        long testTime = refTime - SECONDS.toMillis(35);
        assertEquals("00:35", tdt.getText(mResources, testTime));

        testTime -= MINUTES.toMillis(12);
        assertEquals("12:35", tdt.getText(mResources, testTime));

        testTime -= HOURS.toMillis(4);
        assertEquals("4:13", tdt.getText(mResources, testTime));

        testTime -= HOURS.toMillis(10);
        assertEquals("14:13", tdt.getText(mResources, testTime));

        testTime -= DAYS.toMillis(3);
        assertEquals("3d 15h", tdt.getText(mResources, testTime));

        testTime -= DAYS.toMillis(10);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testWordsSingleUnitBeforeRefPeriod() {
        // GIVEN TimeDifferenceText of STYLE_WORDS_SINGLE_UNIT
        long refTime = 990000000000L;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime, refTime + 654654, DIFFERENCE_STYLE_WORDS_SINGLE_UNIT, true, null);

        // WHEN getText is called for times before the end of the ref period...
        // THEN the result is formatted according to the style.
        long testTime = refTime - SECONDS.toMillis(35);
        assertEquals("1 min", tdt.getText(mResources, testTime));

        testTime -= MINUTES.toMillis(1);
        assertEquals("2 mins", tdt.getText(mResources, testTime));

        testTime -= MINUTES.toMillis(11);
        assertEquals("13 mins", tdt.getText(mResources, testTime));

        testTime -= HOURS.toMillis(1);
        assertEquals("2 hours", tdt.getText(mResources, testTime));

        testTime -= HOURS.toMillis(4);
        assertEquals("6 hours", tdt.getText(mResources, testTime));

        testTime -= HOURS.toMillis(18);
        assertEquals("1 day", tdt.getText(mResources, testTime));

        testTime -= DAYS.toMillis(1);
        assertEquals("2 days", tdt.getText(mResources, testTime));

        testTime -= DAYS.toMillis(10);
        assertEquals("12 days", tdt.getText(mResources, testTime));
    }

    @Test
    public void testDuringRefPeriodShowingNowText() {
        // GIVEN TimeDifferenceText
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime, refTime + 100000, DIFFERENCE_STYLE_SHORT_SINGLE_UNIT, true, null);

        // WHEN getText is called for a time within the ref period
        // THEN "Now" is returned.
        assertEquals("Now", tdt.getText(mResources, refTime + 1000));
    }

    @Test
    public void testDuringRefPeriodNotShowingNowTextShortSingle() {
        // GIVEN TimeDifferenceText
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime, refTime + 100000, DIFFERENCE_STYLE_SHORT_SINGLE_UNIT, false, null);

        // WHEN getText is called for a time within the ref period
        // THEN a zero value is returned.
        assertEquals("0m", tdt.getText(mResources, refTime + 1000));
    }

    @Test
    public void testDuringRefPeriodNotShowingNowTextShortDual() {
        // GIVEN TimeDifferenceText
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime, refTime + 100000, DIFFERENCE_STYLE_SHORT_DUAL_UNIT, false, null);

        // WHEN getText is called for a time within the ref period
        // THEN a zero value is returned.
        assertEquals("0m", tdt.getText(mResources, refTime + 1000));
    }

    @Test
    public void testDuringRefPeriodNotShowingNowTextWord() {
        // GIVEN TimeDifferenceText
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime, refTime + 100000, DIFFERENCE_STYLE_WORDS_SINGLE_UNIT, false, null);

        // WHEN getText is called for a time within the ref period
        // THEN a zero value is returned.
        assertEquals("0 mins", tdt.getText(mResources, refTime + 1000));
    }

    @Test
    public void testDuringRefPeriodNotShowingNowTextStopwatch() {
        // GIVEN TimeDifferenceText
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime, refTime + 100000, DIFFERENCE_STYLE_STOPWATCH, false, null);

        // WHEN getText is called for a time within the ref period
        // THEN a zero value is returned.
        assertEquals("00:00", tdt.getText(mResources, refTime + 1000));
    }

    @Test
    public void testDuringRefPeriodShowingNowTextStopwatch() {
        // GIVEN TimeDifferenceText
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime, refTime + 100000, DIFFERENCE_STYLE_STOPWATCH, true, null);

        // WHEN getText is called for a time within the ref period
        // THEN "Now" is returned.
        assertEquals("Now", tdt.getText(mResources, refTime + 1000));
    }

    @Test
    public void testAtRefPeriodStartTime() {
        // GIVEN TimeDifferenceText
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime, refTime + 100000, DIFFERENCE_STYLE_SHORT_SINGLE_UNIT, true, null);

        // WHEN getText is called for the ref period start time
        // THEN "Now" is returned.
        assertEquals("Now", tdt.getText(mResources, refTime));
    }

    @Test
    public void testAtRefPeriodEndTime() {
        // GIVEN TimeDifferenceText
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime - 100000, refTime, DIFFERENCE_STYLE_SHORT_SINGLE_UNIT, true, null);

        // WHEN getText is called for the ref period end time
        // THEN "Now" is returned.
        assertEquals("Now", tdt.getText(mResources, refTime));
    }

    @Test
    public void testReturnsSameTextWithStopwatchStyle() {
        // GIVEN TimeDifferenceText
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime - 100000, refTime, DIFFERENCE_STYLE_STOPWATCH, true, null);

        // WHEN we consider two times for which the text would differ.
        // THEN returnsSameText returns false.
        long testTime1 = refTime + SECONDS.toMillis(10);
        long testTime2 = refTime + SECONDS.toMillis(15);
        assertFalse(tdt.returnsSameText(testTime1, testTime2));

        // WHEN we consider two times for which the text would be the same.
        // THEN returnsSameText returns true.
        testTime1 = refTime + SECONDS.toMillis(10) + 100;
        testTime2 = refTime + SECONDS.toMillis(10) + 600;
        assertTrue(tdt.returnsSameText(testTime1, testTime2));
    }

    @Test
    public void testReturnsSameTextWithShortSingleStyle() {
        // GIVEN TimeDifferenceText
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime - 100000, refTime, DIFFERENCE_STYLE_SHORT_SINGLE_UNIT, true, null);

        // WHEN we consider two times for which the text would differ.
        // THEN returnsSameText returns false.
        long testTime1 = refTime + MINUTES.toMillis(10);
        long testTime2 = refTime + MINUTES.toMillis(12);
        assertFalse(tdt.returnsSameText(testTime1, testTime2));

        // WHEN we consider two times for which the text would be the same.
        // THEN returnsSameText returns true.
        testTime1 = refTime + MINUTES.toMillis(10) + SECONDS.toMillis(10);
        testTime2 = refTime + MINUTES.toMillis(10) + SECONDS.toMillis(15);
        assertTrue(tdt.returnsSameText(testTime1, testTime2));
    }

    @Test
    public void testReturnsSameTextShortDualStyle() {
        // GIVEN TimeDifferenceText
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime - 100000, refTime, DIFFERENCE_STYLE_SHORT_DUAL_UNIT, true, null);

        // WHEN we consider two times for which the text would differ.
        // THEN returnsSameText returns false.
        long testTime1 = refTime + MINUTES.toMillis(10);
        long testTime2 = refTime + MINUTES.toMillis(12);
        assertFalse(tdt.returnsSameText(testTime1, testTime2));

        // WHEN we consider two times for which the text would be the same.
        // THEN returnsSameText returns true.
        testTime1 = refTime + MINUTES.toMillis(10) + SECONDS.toMillis(10);
        testTime2 = refTime + MINUTES.toMillis(10) + SECONDS.toMillis(15);
        assertTrue(tdt.returnsSameText(testTime1, testTime2));
    }

    @Test
    public void testReturnsSameTextWordsSingleStyle() {
        // GIVEN TimeDifferenceText
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime - 100000, refTime, DIFFERENCE_STYLE_WORDS_SINGLE_UNIT, true, null);

        // WHEN we consider two times for which the text would differ.
        // THEN returnsSameText returns false.
        long testTime1 = refTime + MINUTES.toMillis(10);
        long testTime2 = refTime + MINUTES.toMillis(12);
        assertFalse(tdt.returnsSameText(testTime1, testTime2));

        // WHEN we consider two times for which the text would be the same.
        // THEN returnsSameText returns true.
        testTime1 = refTime + MINUTES.toMillis(10) + SECONDS.toMillis(10);
        testTime2 = refTime + MINUTES.toMillis(10) + SECONDS.toMillis(15);
        assertTrue(tdt.returnsSameText(testTime1, testTime2));
    }

    @Test
    public void testReturnsSameTextWithStopwatchStyleMinuteMinimumUnit() {
        // GIVEN TimeDifferenceText
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime - 100000, refTime, DIFFERENCE_STYLE_STOPWATCH, true, MINUTES);

        // WHEN we consider two times for which the text would differ without the minimum unit,
        // but now will not differ
        // THEN returnsSameText returns true.
        long testTime1 = refTime + SECONDS.toMillis(10);
        long testTime2 = refTime + SECONDS.toMillis(15);
        assertTrue(tdt.returnsSameText(testTime1, testTime2));

        // WHEN we consider two times for which the text would differ.
        // THEN returnsSameText returns false.
        testTime1 = refTime + MINUTES.toMillis(10);
        testTime2 = refTime + MINUTES.toMillis(12);
        assertFalse(tdt.returnsSameText(testTime1, testTime2));
    }

    @Test
    public void testReturnsSameTextWithShortSingleStyleHourMinimumUnit() {
        // GIVEN TimeDifferenceText
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime - 100000, refTime, DIFFERENCE_STYLE_SHORT_SINGLE_UNIT, true, HOURS);

        // WHEN we consider two times for which the text would differ without the minimum unit,
        // but now will not differ
        // THEN returnsSameText returns true.
        long testTime1 = refTime + MINUTES.toMillis(10);
        long testTime2 = refTime + MINUTES.toMillis(12);
        assertTrue(tdt.returnsSameText(testTime1, testTime2));

        // WHEN we consider two times for which the text would differ.
        // THEN returnsSameText returns false.
        testTime1 = refTime + HOURS.toMillis(10);
        testTime2 = refTime + HOURS.toMillis(12);
        assertFalse(tdt.returnsSameText(testTime1, testTime2));
    }

    @Test
    public void testReturnsSameTextWithShortDualStyleHourMinimumUnit() {
        // GIVEN TimeDifferenceText
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime - 100000, refTime, DIFFERENCE_STYLE_SHORT_DUAL_UNIT, true, HOURS);

        // WHEN we consider two times for which the text would differ without the minimum unit,
        // but now will not differ
        // THEN returnsSameText returns true.
        long testTime1 = refTime + MINUTES.toMillis(10);
        long testTime2 = refTime + MINUTES.toMillis(12);
        assertTrue(tdt.returnsSameText(testTime1, testTime2));

        // WHEN we consider two times for which the text would differ.
        // THEN returnsSameText returns false.
        testTime1 = refTime + HOURS.toMillis(10);
        testTime2 = refTime + HOURS.toMillis(12);
        assertFalse(tdt.returnsSameText(testTime1, testTime2));
    }

    @Test
    public void testReturnsSameTextWithShortSingleStyleDayMinimumUnit() {
        // GIVEN TimeDifferenceText
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime - 100000, refTime, DIFFERENCE_STYLE_SHORT_SINGLE_UNIT, true, DAYS);

        // WHEN we consider two times for which the text differs by a number of minutes
        // THEN returnsSameText returns true.
        long testTime1 = refTime + MINUTES.toMillis(10);
        long testTime2 = refTime + MINUTES.toMillis(12);
        assertTrue(tdt.returnsSameText(testTime1, testTime2));

        // WHEN we consider two times for which the text differs by a number of hours
        // THEN returnsSameText returns true.
        testTime1 = refTime + HOURS.toMillis(10);
        testTime2 = refTime + HOURS.toMillis(12);
        assertTrue(tdt.returnsSameText(testTime1, testTime2));

        // WHEN we consider two times for which the text differs by a number of days
        // THEN returnsSameText returns false.
        testTime1 = refTime + DAYS.toMillis(10);
        testTime2 = refTime + DAYS.toMillis(12);
        assertFalse(tdt.returnsSameText(testTime1, testTime2));
    }

    @Test
    public void testReturnsSameTextWithWordStyleDayMinimumUnit() {
        // GIVEN TimeDifferenceText
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        refTime - 100000, refTime, DIFFERENCE_STYLE_WORDS_SINGLE_UNIT, true, DAYS);

        // WHEN we consider two times for which the text differs by a number of minutes
        // THEN returnsSameText returns true.
        long testTime1 = refTime + MINUTES.toMillis(10);
        long testTime2 = refTime + MINUTES.toMillis(12);
        assertTrue(tdt.returnsSameText(testTime1, testTime2));

        // WHEN we consider two times for which the text differs by a number of hours
        // THEN returnsSameText returns true.
        testTime1 = refTime + HOURS.toMillis(10);
        testTime2 = refTime + HOURS.toMillis(12);
        assertTrue(tdt.returnsSameText(testTime1, testTime2));

        // WHEN we consider two times for which the text differs by a number of days
        // THEN returnsSameText returns false.
        testTime1 = refTime + DAYS.toMillis(10);
        testTime2 = refTime + DAYS.toMillis(12);
        assertFalse(tdt.returnsSameText(testTime1, testTime2));
    }

    @Test
    public void testShortSingleUnitRoundingFromSmallUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_SINGLE_UNIT
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_SHORT_SINGLE_UNIT, true, null);

        // WHEN getText is called for a time a few seconds more than a whole number of hours after
        // the
        // ref time
        long testTime = refTime + HOURS.toMillis(4) + SECONDS.toMillis(35);
        // THEN the time is rounded up to the next hour
        assertEquals("5h", tdt.getText(mResources, testTime));

        // WHEN getText is called for a time a few seconds more than a whole number of days after
        // the
        // ref time
        testTime = refTime + DAYS.toMillis(12) + SECONDS.toMillis(35);
        // THEN the text is rounded up to the next day
        assertEquals("13d", tdt.getText(mResources, testTime));

        // WHEN getText is called for a time a few minutes more than a whole number of days after
        // the
        // ref time
        testTime = refTime + DAYS.toMillis(12) + MINUTES.toMillis(35);
        // THEN the text is rounded up to the next day
        assertEquals("13d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testShortSingleUnitExactNumberOfUnits() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_SINGLE_UNIT
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_SHORT_SINGLE_UNIT, true, null);

        // WHEN getText is called for a time a whole number of minutes after the ref time
        long testTime = refTime + MINUTES.toMillis(35);
        // THEN the output shows that number of minutes.
        assertEquals("35m", tdt.getText(mResources, testTime));

        // WHEN getText is called for a time a whole number of hours after the ref time
        testTime = refTime + HOURS.toMillis(12);
        // THEN the output shows that number of hours.
        assertEquals("12h", tdt.getText(mResources, testTime));

        // WHEN getText is called for a time a whole number of days after the ref time
        testTime = refTime + DAYS.toMillis(3);
        // THEN the output shows that number of days.
        assertEquals("3d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testDefaultStyle() {
        // GIVEN TimeDifferenceText with an unrecognised style
        long refTime = 1000000;
        TimeDifferenceText tdt = new TimeDifferenceText(0, refTime, 123456, true, null);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the default style.
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("1m", tdt.getText(mResources, testTime));

        testTime += MINUTES.toMillis(12);
        assertEquals("13m", tdt.getText(mResources, testTime));

        testTime += HOURS.toMillis(4);
        assertEquals("5h", tdt.getText(mResources, testTime));

        testTime += HOURS.toMillis(10);
        assertEquals("15h", tdt.getText(mResources, testTime));

        testTime += DAYS.toMillis(3);
        assertEquals("4d", tdt.getText(mResources, testTime));

        testTime += DAYS.toMillis(10);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testShortDualUnitDoesntShowZeroInSmallerUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_DUAL_UNIT
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_SHORT_DUAL_UNIT, true, null);

        // WHEN getText is called for a time a whole number of hours after the ref time
        long testTime = refTime + HOURS.toMillis(4);
        // THEN the text shows hours but not minutes.
        assertEquals("4h", tdt.getText(mResources, testTime));

        // WHEN getText is called for a time a whole number of days after the ref time
        testTime = refTime + DAYS.toMillis(35);
        // THEN the text shows days but not hours.
        assertEquals("35d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testShortSingleUnitWithHourMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_SINGLE_UNIT
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_SHORT_SINGLE_UNIT, true, HOURS);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // an hour
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("1h", tdt.getText(mResources, testTime));

        testTime = refTime + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("1h", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(4) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("5h", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(14) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("15h", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(3) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("4d", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(13) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testShortSingleUnitWithDayMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_SINGLE_UNIT
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_SHORT_SINGLE_UNIT, true, DAYS);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // an hour
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("1d", tdt.getText(mResources, testTime));

        testTime = refTime + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("1d", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(4) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("1d", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(14) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("1d", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(3) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("4d", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(13) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testShortSingleUnitWithMinuteMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_SINGLE_UNIT
        long refTime = 1000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        0, refTime, DIFFERENCE_STYLE_SHORT_SINGLE_UNIT, true, MINUTES);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, with no change due to the minimum
        // unit.
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("1m", tdt.getText(mResources, testTime));

        testTime = refTime + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("13m", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(4) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("5h", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(14) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("15h", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(3) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("4d", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(13) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testShortDualUnitWithHourMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_DUAL_UNIT
        long refTime = 10000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_SHORT_DUAL_UNIT, true, HOURS);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // an hour
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("1h", tdt.getText(mResources, testTime));

        testTime = refTime + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("1h", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(4) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("5h", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(14) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("15h", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(3) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("3d 15h", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(13) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testShortDualUnitWithDayMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_DUAL_UNIT
        long refTime = 10000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_SHORT_DUAL_UNIT, true, DAYS);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // a day
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("1d", tdt.getText(mResources, testTime));

        testTime = refTime + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("1d", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(4) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("1d", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(14) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("1d", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(3) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("4d", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(13) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testShortDualUnitWithMinuteMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_DUAL_UNIT
        long refTime = 10000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_SHORT_DUAL_UNIT, true, MINUTES);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, with no change due to the minimum
        // unit.
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("1m", tdt.getText(mResources, testTime));

        testTime = refTime + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("13m", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(4) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("4h 13m", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(14) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("14h 13m", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(3) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("3d 15h", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(13) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testShortDualUnitWithSecondMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_SHORT_DUAL_UNIT
        long refTime = 10000000;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_SHORT_DUAL_UNIT, true, SECONDS);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, with no change due to the minimum
        // unit.
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("1m", tdt.getText(mResources, testTime));

        testTime = refTime + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("13m", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(4) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("4h 13m", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(14) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("14h 13m", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(3) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("3d 15h", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(13) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testStopwatchWithMinuteMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_STOPWATCH
        long refTime = 70000000000L;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_STOPWATCH, true, MINUTES);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // a minute.
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("0:01", tdt.getText(mResources, testTime));

        testTime = refTime + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("0:13", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(4) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("4:13", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(14) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("14:13", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(3) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("3d 15h", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(13) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testStopwatchWithHourMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_STOPWATCH
        long refTime = 70000000000L;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_STOPWATCH, true, HOURS);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // an hour (which means that the stopwatch-style format hh:mm cannot be used).
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("1h", tdt.getText(mResources, testTime));

        testTime = refTime + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("1h", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(4) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("5h", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(14) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("15h", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(3) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("3d 15h", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(13) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testStopwatchWithDayMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_STOPWATCH
        long refTime = 70000000000L;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_STOPWATCH, true, DAYS);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // a day..
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("1d", tdt.getText(mResources, testTime));

        testTime = refTime + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("1d", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(4) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("1d", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(14) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("1d", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(3) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("4d", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(13) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testStopwatchWithSecondMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_STOPWATCH
        long refTime = 70000000000L;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_STOPWATCH, true, SECONDS);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, with no change due to the minimum
        // unit.
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("00:35", tdt.getText(mResources, testTime));

        testTime = refTime + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("12:35", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(4) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("4:13", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(14) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("14:13", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(3) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("3d 15h", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(13) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("14d", tdt.getText(mResources, testTime));
    }

    @Test
    public void testWordsSingleUnitWithHourMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_WORDS_SINGLE_UNIT
        long refTime = 990000000000L;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_WORDS_SINGLE_UNIT, true, HOURS);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // an hour
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("1 hour", tdt.getText(mResources, testTime));

        testTime = refTime + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("1 hour", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(4) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("5 hours", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(14) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("15 hours", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(3) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("4 days", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(13) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("14 days", tdt.getText(mResources, testTime));
    }

    @Test
    public void testWordsSingleUnitWithDayMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_WORDS_SINGLE_UNIT
        long refTime = 990000000000L;
        TimeDifferenceText tdt =
                new TimeDifferenceText(0, refTime, DIFFERENCE_STYLE_WORDS_SINGLE_UNIT, true, DAYS);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, not including any units smaller than
        // an hour
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("1 day", tdt.getText(mResources, testTime));

        testTime = refTime + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("1 day", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(4) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("1 day", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(14) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("1 day", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(3) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("4 days", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(13) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("14 days", tdt.getText(mResources, testTime));
    }

    @Test
    public void testWordsSingleUnitWithMinuteMinimumUnit() {
        // GIVEN TimeDifferenceText of STYLE_WORDS_SINGLE_UNIT
        long refTime = 990000000000L;
        TimeDifferenceText tdt =
                new TimeDifferenceText(
                        0, refTime, DIFFERENCE_STYLE_WORDS_SINGLE_UNIT, true, MINUTES);

        // WHEN getText is called for times after the end of the ref period...
        // THEN the result is formatted according to the style, with no change due to the minimum
        // unit.
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("1 min", tdt.getText(mResources, testTime));

        testTime = refTime + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("13 mins", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(4) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("5 hours", tdt.getText(mResources, testTime));

        testTime = refTime + HOURS.toMillis(14) + MINUTES.toMillis(12) + SECONDS.toMillis(35);
        assertEquals("15 hours", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(3) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("4 days", tdt.getText(mResources, testTime));

        testTime = refTime + DAYS.toMillis(13) + HOURS.toMillis(14) + MINUTES.toMillis(12);
        assertEquals("14 days", tdt.getText(mResources, testTime));
    }

    @Test
    public void testTimeDifferenceGetNextChangeStopWatchNoMinimum() {
        TimeDifferenceText text =
                new TimeDifferenceText(0, 1, DIFFERENCE_STYLE_STOPWATCH, true, null);

        assertEquals(SECONDS.toMillis(1), text.getPrecision());

        // Time difference is rounded up, so the next change is 1ms after the next second boundary.
        assertThat(text.getNextChangeTime(1000000000L)).isEqualTo(1000000001L);
        assertThat(text.getNextChangeTime(1000000001L)).isEqualTo(1000001001L);
        assertThat(text.getNextChangeTime(1000001234L)).isEqualTo(1000002001L);
    }

    @Test
    public void testTimeDifferenceGetNextChangeNoMinimum() {
        int[] styles = {
            DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            DIFFERENCE_STYLE_WORDS_SINGLE_UNIT,
            DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT,
        };

        for (int style : styles) {
            TimeDifferenceText text = new TimeDifferenceText(0, 1, style, true, null);
            assertEquals(MINUTES.toMillis(1), text.getPrecision());

            // Time difference is rounded up, and precision for all of these is 1 minute, so the
            // next
            // change is 1ms after the next minute boundary.
            assertThat(text.getNextChangeTime(60000000000L)).isEqualTo(60000000001L);
            assertThat(text.getNextChangeTime(60000000001L)).isEqualTo(60000060001L);
            assertThat(text.getNextChangeTime(60000060000L)).isEqualTo(60000060001L);
        }
    }

    @Test
    public void testTimeDifferenceGetNextChangeMinuteMinimum() {
        int[] styles = {
            DIFFERENCE_STYLE_STOPWATCH,
            DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            DIFFERENCE_STYLE_WORDS_SINGLE_UNIT,
            DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT,
        };

        for (int style : styles) {
            TimeDifferenceText text = new TimeDifferenceText(0, 1, style, true, MINUTES);
            assertEquals(MINUTES.toMillis(1), text.getPrecision());

            // Next change is 1ms after the next minute boundary.
            assertThat(text.getNextChangeTime(60000000000L)).isEqualTo(60000000001L);
            assertThat(text.getNextChangeTime(60000000001L)).isEqualTo(60000060001L);
            assertThat(text.getNextChangeTime(60000060000L)).isEqualTo(60000060001L);
        }
    }

    @Test
    public void testTimeDifferenceGetNextChangeHourMinimum() {
        int[] styles = {
            DIFFERENCE_STYLE_STOPWATCH,
            DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            DIFFERENCE_STYLE_WORDS_SINGLE_UNIT,
            DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT,
        };

        for (int style : styles) {
            TimeDifferenceText text = new TimeDifferenceText(0, 1, style, true, HOURS);
            assertEquals(HOURS.toMillis(1), text.getPrecision());

            // Next change is 1ms after the next hour boundary.
            assertThat(text.getNextChangeTime(36000035789L)).isEqualTo(36003600001L);
            assertThat(text.getNextChangeTime(36003600000L)).isEqualTo(36003600001L);
            assertThat(text.getNextChangeTime(36003600001L)).isEqualTo(36007200001L);
            assertThat(text.getNextChangeTime(36007199999L)).isEqualTo(36007200001L);
        }
    }

    @Test
    public void testTimeDifferenceGetPrecisionDayMinimum() {
        int[] styles = {
            DIFFERENCE_STYLE_STOPWATCH,
            DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            DIFFERENCE_STYLE_WORDS_SINGLE_UNIT,
            DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT,
        };

        for (int style : styles) {
            TimeDifferenceText text = new TimeDifferenceText(0, 1, style, true, DAYS);
            assertEquals(DAYS.toMillis(1), text.getPrecision());

            // Next change is 1ms after the next day boundary.
            assertThat(text.getNextChangeTime(8640000035789L)).isEqualTo(8640086400001L);
            assertThat(text.getNextChangeTime(8640086400000L)).isEqualTo(8640086400001L);
            assertThat(text.getNextChangeTime(8640086400001L)).isEqualTo(8640172800001L);
        }
    }

    @Test
    public void testParcelTimeDifferenceTextWithoutMinUnit() {
        // GIVEN TimeDifferenceText without min unit
        long refTime = 10000000;
        TimeDifferenceText originalText =
                new TimeDifferenceText(
                        refTime - 156561, refTime, DIFFERENCE_STYLE_SHORT_SINGLE_UNIT, false, null);

        // WHEN the object is parcelled and unparcelled
        TimeDifferenceText newText = roundTripParcelable(originalText);

        // THEN the object behaves as expected.
        long testTime = refTime + HOURS.toMillis(2) + MINUTES.toMillis(35);
        assertEquals("3h", newText.getText(mResources, testTime).toString());
        assertEquals("0m", newText.getText(mResources, refTime).toString());
    }

    @Test
    public void testParcelTimeDifferenceTextWithMinUnit() {
        // GIVEN TimeDifferenceText with a minimum unit specified
        long refTime = 10000000;
        TimeDifferenceText originalText =
                new TimeDifferenceText(
                        refTime - 156561, refTime, DIFFERENCE_STYLE_SHORT_DUAL_UNIT, true, HOURS);

        // WHEN the object is parcelled and unparcelled
        TimeDifferenceText newText = roundTripParcelable(originalText);

        // THEN the object behaves as expected.
        long testTime = refTime + HOURS.toMillis(2) + MINUTES.toMillis(35);
        assertEquals("3h", newText.getText(mResources, testTime).toString());
    }

    /** Writes {@code in} to a {@link Parcel} and reads it back, returning the result. */
    @SuppressWarnings("unchecked")
    private static <T extends Parcelable> T roundTripParcelable(T in) {
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeValue(in);
            parcel.setDataPosition(0);
            return (T) parcel.readValue(in.getClass().getClassLoader());
        } finally {
            parcel.recycle();
        }
    }
}
