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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ComplicationTextTest {

    private Resources mResources = ApplicationProvider.getApplicationContext().getResources();

    @Test
    public void testPlainText() {
        // GIVEN ComplicationText of the plain string type
        ComplicationText complicationText = ComplicationText.plainText("hello");

        // WHEN getText is called
        // THEN the plain string is returned.
        assertEquals("hello", complicationText.getText(mResources, 132456789).toString());
    }

    @Test
    public void testTimeDifferenceShortSingleUnitOnly() {
        // GIVEN ComplicationText with short single unit time difference text only
        long refTime = 10000000;
        ComplicationText complicationText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(refTime - 156561)
                        .setReferencePeriodEndMillis(refTime)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                        .build();

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "2h 35m" should be rounded to "3h".
        long testTime = refTime + MINUTES.toMillis(35) + HOURS.toMillis(2);
        assertEquals("3h", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "23h 59m 10s" should be rounded to "1d".
        testTime = refTime + SECONDS.toMillis(10) + MINUTES.toMillis(59) + HOURS.toMillis(23);
        assertEquals("1d", complicationText.getText(mResources, testTime).toString());

        // THEN the time difference text is returned, and "10m 10s" should be rounded to "11m".
        testTime = refTime + SECONDS.toMillis(10) + MINUTES.toMillis(10);
        assertEquals("11m", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "23h 15m" should be rounded to "1d".
        testTime = refTime + MINUTES.toMillis(15) + HOURS.toMillis(23);
        assertEquals("1d", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "23h 0m" should be round to "23h".
        testTime = refTime + HOURS.toMillis(23);
        assertEquals("23h", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "23h 10m" should be round to "1d".
        testTime = refTime + MINUTES.toMillis(10) + HOURS.toMillis(23);
        assertEquals("1d", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "22h 10m" should be round to "23h".
        testTime = refTime + MINUTES.toMillis(10) + HOURS.toMillis(22);
        assertEquals("23h", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "1d 10h" should be round to "2d".
        testTime = refTime + HOURS.toMillis(10) + DAYS.toMillis(1);
        assertEquals("2d", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "22h 10m" should be round to "23h".
        testTime = refTime + MINUTES.toMillis(10) + HOURS.toMillis(22);
        assertEquals("23h", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "59m 30s" should be round to "1h".
        testTime = refTime + SECONDS.toMillis(30) + MINUTES.toMillis(59);
        assertEquals("1h", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "59m 00s" should be displayed as "59m".
        testTime = refTime + MINUTES.toMillis(59);
        assertEquals("59m", complicationText.getText(mResources, testTime).toString());
    }

    @Test
    public void testTimeDifferenceShortDualUnitOnly() {
        // GIVEN ComplicationText with short dual unit time difference text only
        long refTime = 10000000;
        ComplicationText complicationText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(refTime - 156561)
                        .setReferencePeriodEndMillis(refTime)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT)
                        .build();

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "2h 35m 10s" should be rounded to "2h 36m".
        long testTime = refTime + SECONDS.toMillis(10) + MINUTES.toMillis(35) + HOURS.toMillis(2);
        assertEquals("2h 36m", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "2h 35m" should be rounded to "2h 35m".
        testTime = refTime + MINUTES.toMillis(35) + HOURS.toMillis(2);
        assertEquals("2h 35m", complicationText.getText(mResources, testTime).toString());

        // THEN the time difference text is returned
        // and "9d 23h 58m 10s" should be rounded to "10d".
        testTime =
                refTime
                        + SECONDS.toMillis(10)
                        + MINUTES.toMillis(58)
                        + HOURS.toMillis(23)
                        + DAYS.toMillis(9);
        assertEquals("10d", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "23h 59m 10s" should be rounded to "1d".
        testTime = refTime + SECONDS.toMillis(10) + MINUTES.toMillis(59) + HOURS.toMillis(23);
        assertEquals("1d", complicationText.getText(mResources, testTime).toString());

        // THEN the time difference text is returned
        // and "23h 58m 10s" should be rounded to "23h 59m".
        testTime = refTime + SECONDS.toMillis(10) + MINUTES.toMillis(58) + HOURS.toMillis(23);
        assertEquals("23h 59m", complicationText.getText(mResources, testTime).toString());
    }

    @Test
    public void testStopwatchTextUnitOnly() {
        // GIVEN ComplicationText with stop watch time difference text only
        long refTime = 10000000;
        ComplicationText complicationText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(refTime - 156561)
                        .setReferencePeriodEndMillis(refTime)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_STOPWATCH)
                        .build();

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "23h 59m 10s" should be rounded to "1d".
        long testTime = refTime + SECONDS.toMillis(10) + MINUTES.toMillis(59) + HOURS.toMillis(23);
        assertEquals("1d", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned in stopwatch style with no rounding.
        testTime = refTime + MINUTES.toMillis(59) + HOURS.toMillis(23);
        assertEquals("23:59", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for another time after the ref period
        // THEN the time difference text is returned in stopwatch style with no rounding.
        testTime = refTime + SECONDS.toMillis(59) + MINUTES.toMillis(1);
        assertEquals("01:59", complicationText.getText(mResources, testTime).toString());
    }

    @Test
    public void testWordsSingleUnitWithSurroundingString() {
        // GIVEN ComplicationText with stop watch time difference text only
        long refTime = 10000000;
        ComplicationText complicationText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(refTime - 156561)
                        .setReferencePeriodEndMillis(refTime)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT)
                        .setSurroundingText("just ^1 left")
                        .build();

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "23h 59m 10s" should be rounded to one day.
        long testTime = refTime + HOURS.toMillis(23) + MINUTES.toMillis(59) + SECONDS.toMillis(10);
        assertEquals("just 1 day left", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned in words, showing the bigger unit only, and
        // rounding up.
        testTime = refTime + HOURS.toMillis(12) + MINUTES.toMillis(35);
        assertEquals(
                "just 13 hours left",
                complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for another time after the ref period
        // THEN the time difference text is returned in words, showing the bigger unit only, and
        // rounding up.
        testTime = refTime + MINUTES.toMillis(35) + SECONDS.toMillis(59);
        assertEquals(
                "just 36 mins left",
                complicationText.getText(mResources, testTime).toString());
    }

    @Test
    public void testSurroundingTextWithIgnoredPlaceholders() {
        // WHEN ComplicationText with surrounding text that contains one of ^2..9 is created
        long refTime = 10000000;
        ComplicationText complicationText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(refTime - 156561)
                        .setReferencePeriodEndMillis(refTime)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT)
                        .setSurroundingText("^1 ^^ ^ ^^1 ^2 ^3 ^4 ^5 ^6 ^7 ^8 ^9")
                        .build();

        // THEN placeholders other than ^1 and ^^ are ignored
        assertEquals(
                "Now ^ ^ ^1 ^2 ^3 ^4 ^5 ^6 ^7 ^8 ^9",
                complicationText.getText(mResources, refTime).toString());
    }

    @Test
    public void testWordsSingleUnitWithSurroundingStringAndDayMinimumUnit() {
        // GIVEN ComplicationText with stop watch time difference text only
        long refTime = 10000000;
        ComplicationText complicationText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(refTime - 156561)
                        .setReferencePeriodEndMillis(refTime)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT)
                        .setSurroundingText("just ^1 left")
                        .setMinimumUnit(TimeUnit.DAYS)
                        .build();

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned with the time rounded up to the next day.
        long testTime = refTime + DAYS.toMillis(7) + HOURS.toMillis(23) + MINUTES.toMillis(59);
        assertEquals("just 8 days left", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned in words with the time rounded up to the next
        // day.
        testTime = refTime + HOURS.toMillis(12) + MINUTES.toMillis(35);
        assertEquals("just 1 day left", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for another time after the ref period
        // THEN the time difference text is returned in words with the time rounded up to the next
        // day.
        testTime = refTime + MINUTES.toMillis(35) + SECONDS.toMillis(59);
        assertEquals("just 1 day left", complicationText.getText(mResources, testTime).toString());
    }

    @Test
    public void testShortWordsSingleUnit() {
        // GIVEN ComplicationText with stop watch time difference text only
        long refTime = 10000000;
        ComplicationText complicationText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(refTime - 156561)
                        .setReferencePeriodEndMillis(refTime)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT)
                        .build();

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "23h 59m 10s" should be rounded to one day.
        long testTime = refTime + HOURS.toMillis(23) + MINUTES.toMillis(59) + SECONDS.toMillis(10);
        assertEquals("1 day", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned in words, showing the bigger unit only, and
        // rounding up.
        testTime = refTime + HOURS.toMillis(1) + MINUTES.toMillis(35);
        assertEquals("2 hours", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for a double-digit number of hours after the ref period
        // THEN the time difference text is returned using the short style.
        testTime = refTime + HOURS.toMillis(12) + MINUTES.toMillis(35);
        assertEquals("13h", complicationText.getText(mResources, testTime).toString());

        // WHEN getText is called for another time many days the ref period, such that more than 7
        // characters would be used if the unit was shown as a word
        // THEN the time difference text is returned using the short style.
        testTime = refTime + DAYS.toMillis(120);
        assertEquals("120d", complicationText.getText(mResources, testTime).toString());
    }

    @Test
    public void testStopwatchWithNowText() {
        // GIVEN ComplicationText with stop watch time difference text only
        long refTime = 10000000;
        ComplicationText complicationText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(refTime - 156561)
                        .setReferencePeriodEndMillis(refTime)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_STOPWATCH)
                        .setShowNowText(true)
                        .build();

        // WHEN getText is called for a time within the ref period
        // THEN the time difference text is returned and "Now" is shown.
        assertEquals("Now", complicationText.getText(mResources, refTime - 100).toString());

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and the time difference is shown in stopwatch
        // style.
        long testTime = refTime + SECONDS.toMillis(35);
        assertEquals("00:35", complicationText.getText(mResources, testTime).toString());
    }

    @Test
    public void testSingleUnitWithoutNowText() {
        // GIVEN ComplicationText with stop watch time difference text only
        long refTime = 10000000;
        ComplicationText complicationText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(refTime - 156561)
                        .setReferencePeriodEndMillis(refTime + 100)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                        .setShowNowText(false)
                        .build();

        // WHEN getText is called for a time within the ref period
        // THEN the time difference text is returned and "0m" is shown.
        assertEquals("0m", complicationText.getText(mResources, refTime).toString());
    }

    @Test
    public void testTimeDifferenceShortSingleUnitAndFormatString() {
        // GIVEN ComplicationText with short single unit time difference text and a format string
        long refTime = 10000000;
        ComplicationText complicationText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(refTime - 156561)
                        .setReferencePeriodEndMillis(refTime)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                        .setSurroundingText("hello ^1 time")
                        .build();

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned within the format string
        long testTime = refTime + MINUTES.toMillis(35) + HOURS.toMillis(2);
        assertEquals("hello 3h time", complicationText.getText(mResources, testTime).toString());
    }

    @Test
    public void testTimeDifferenceShortDualUnitAndFormatString() {
        // GIVEN ComplicationText with short dual unit time difference text and a format string
        long refTime = 10000000;
        ComplicationText complicationText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(refTime - 156561)
                        .setReferencePeriodEndMillis(refTime)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT)
                        .setSurroundingText("sometext^1somemoretext")
                        .build();

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned within the format string
        long testTime = refTime + MINUTES.toMillis(35) + HOURS.toMillis(2);
        assertEquals(
                "sometext2h 35msomemoretext",
                complicationText.getText(mResources, testTime).toString());
    }

    @Test
    public void testTimeFormatUpperCase() {
        ComplicationText complicationText =
                new ComplicationText.TimeFormatBuilder()
                        .setFormat("EEE 'the' d LLL")
                        .setStyle(ComplicationText.FORMAT_STYLE_UPPER_CASE)
                        .build();

        CharSequence result =
                complicationText.getText(
                        mResources, new GregorianCalendar(2016, 2, 4).getTimeInMillis());
        assertEquals("FRI THE 4 MAR", result.toString());
    }

    @Test
    public void testTimeFormatLowerCase() {
        ComplicationText complicationText =
                new ComplicationText.TimeFormatBuilder()
                        .setFormat("EEE 'the' d LLL")
                        .setStyle(ComplicationText.FORMAT_STYLE_LOWER_CASE)
                        .build();
        CharSequence result =
                complicationText.getText(
                        mResources, new GregorianCalendar(2016, 2, 4).getTimeInMillis());
        assertEquals("fri the 4 mar", result.toString());
    }

    @Test
    public void testTimeFormatNoStyle() {
        ComplicationText complicationText =
                new ComplicationText.TimeFormatBuilder().setFormat("EEE 'the' d LLL").build();

        CharSequence result =
                complicationText.getText(
                        mResources, new GregorianCalendar(2016, 2, 4).getTimeInMillis());
        assertEquals("Fri the 4 Mar", result.toString());
    }

    @Test
    public void testTimeFormatUpperCaseSurroundingString() {
        ComplicationText complicationText =
                new ComplicationText.TimeFormatBuilder()
                        .setFormat("EEE 'the' d LLL")
                        .setStyle(ComplicationText.FORMAT_STYLE_UPPER_CASE)
                        .setSurroundingText("sometext^1somemoretext")
                        .build();

        CharSequence result =
                complicationText.getText(
                        mResources, new GregorianCalendar(2016, 2, 4).getTimeInMillis());
        assertEquals("sometextFRI THE 4 MARsomemoretext", result.toString());
    }

    @Test
    public void testTimeFormatWithTimeZone() {
        ComplicationText complicationText =
                new ComplicationText.TimeFormatBuilder()
                        .setFormat("HH:mm")
                        .setTimeZone(TimeZone.getTimeZone("Asia/Seoul"))
                        .build();

        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT+0"));
        calendar.set(2016, 2, 4, 18, 52, 58);
        CharSequence result = complicationText.getText(mResources, calendar.getTimeInMillis());
        assertEquals("03:52", result.toString());
    }

    @Test
    public void testParcelPlainText() {
        // GIVEN ComplicationText containing plain text
        ComplicationText originalText = ComplicationText.plainText("hello how are you");

        // WHEN the object is parcelled and unparcelled
        ComplicationText newText = roundTripParcelable(originalText);

        // THEN the object behaves as expected.
        assertEquals("hello how are you", newText.getText(mResources, 100000).toString());
    }

    @Test
    public void testParcelTimeDifferenceTextWithoutMinUnit() {
        // GIVEN ComplicationText containing TimeDifferenceText
        long refTime = 10000000;
        ComplicationText originalText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(refTime - 156561)
                        .setReferencePeriodEndMillis(refTime)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                        .setSurroundingText("hello ^1 time")
                        .setShowNowText(false)
                        .build();

        // WHEN the object is parcelled and unparcelled
        ComplicationText newText = roundTripParcelable(originalText);

        // THEN the object behaves as expected.
        long testTime = refTime + HOURS.toMillis(2) + MINUTES.toMillis(35);
        assertEquals("hello 3h time", newText.getText(mResources, testTime).toString());
        assertEquals("hello 0m time", newText.getText(mResources, refTime).toString());
    }

    @Test
    public void testParcelTimeDifferenceTextWithMinUnit() {
        // GIVEN ComplicationText containing TimeDifferenceText with a minimum unit specified
        long refTime = 10000000;
        ComplicationText originalText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(refTime - 156561)
                        .setReferencePeriodEndMillis(refTime)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT)
                        .setMinimumUnit(HOURS)
                        .build();

        // WHEN the object is parcelled and unparcelled
        ComplicationText newText = roundTripParcelable(originalText);

        // THEN the object behaves as expected.
        long testTime = refTime + HOURS.toMillis(2) + MINUTES.toMillis(35);
        assertEquals("3h", newText.getText(mResources, testTime).toString());
    }

    @Test
    public void testParcelTimeDifferenceTextWithUnrecognizedMinUnit() {
        // GIVEN a parcelled ComplicationText object containing TimeDifferenceText with an unknown
        // minimum unit specified
        long refTime = 10000000;
        ComplicationText originalText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(refTime - 156561)
                        .setReferencePeriodEndMillis(refTime)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT)
                        .build();
        Parcel parcel = Parcel.obtain();
        originalText.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        Bundle bundle = parcel.readBundle(getClass().getClassLoader());
        bundle.putString("minimum_unit", "XYZ");
        Parcel parcelWithBadMinUnit = Parcel.obtain();
        parcelWithBadMinUnit.writeBundle(bundle);
        parcelWithBadMinUnit.setDataPosition(0);

        // WHEN the object is unparcelled
        ComplicationText newText = ComplicationText.CREATOR.createFromParcel(parcelWithBadMinUnit);

        // THEN the object is unparcelled successfully, and behaves as if no min unit was specified.
        long testTime = refTime + HOURS.toMillis(2) + MINUTES.toMillis(35);
        assertEquals("2h 35m", newText.getText(mResources, testTime).toString());
    }

    @Test
    public void testParcelTimeFormatTextWithoutTimeZone() {
        // GIVEN ComplicationText containing TimeFormatText with no time zone
        ComplicationText originalText =
                new ComplicationText.TimeFormatBuilder()
                        .setFormat("EEE 'the' d LLL")
                        .setStyle(ComplicationText.FORMAT_STYLE_LOWER_CASE)
                        .build();

        // WHEN the object is parcelled and unparcelled
        ComplicationText newText = roundTripParcelable(originalText);

        // THEN the object behaves as expected.
        CharSequence result =
                newText.getText(mResources, new GregorianCalendar(2016, 2, 4).getTimeInMillis());
        assertEquals("fri the 4 mar", result.toString());
    }

    @Test
    public void testParcelTimeFormatTextWithTimeZone() {
        // GIVEN ComplicationText containing TimeFormatText with a time zone specified
        ComplicationText originalText =
                new ComplicationText.TimeFormatBuilder()
                        .setFormat("EEE 'the' d LLL HH:mm")
                        .setStyle(ComplicationText.FORMAT_STYLE_LOWER_CASE)
                        .setTimeZone(TimeZone.getTimeZone("GMT+5"))
                        .build();

        // WHEN the object is parcelled and unparcelled
        ComplicationText newText = roundTripParcelable(originalText);

        // THEN the object behaves as expected.
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT+0"));
        calendar.set(2016, 2, 4, 18, 52, 58);
        CharSequence result = newText.getText(mResources, calendar.getTimeInMillis());
        assertEquals("fri the 4 mar 23:52", result.toString());
    }

    @Test
    public void zeroWorksWhenNoRefPeriodStartTime() {
        // GIVEN ComplicationText with time difference text with no ref period start time
        long refTime = 10000000;
        ComplicationText complicationText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(0)
                        .setReferencePeriodEndMillis(refTime + 100)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                        .build();

        // WHEN getText is called for a zero time
        // THEN the time difference text is returned and "Now" is shown, with no NPE
        assertEquals("Now", complicationText.getText(mResources, 0).toString());
    }

    @Test
    public void nextChangeTimeNotTimeDependent() {
        ComplicationText text = ComplicationText.plainText("hello");

        assertThat(text.getNextChangeTime(1000000)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void nextChangeTimeTimeDifference() {
        ComplicationText text =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(0)
                        .setReferencePeriodEndMillis(0)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                        .build();

        // Time difference rounds up, so the next change is 1ms after the next minute boundary.
        assertThat(text.getNextChangeTime(600000123)).isEqualTo(600060001);
    }

    @Test
    public void nextChangeTimeTimeFormat() {
        ComplicationText text =
                new ComplicationText.TimeFormatBuilder()
                        .setFormat("EEE 'the' d LLL HH:mm")
                        .setStyle(ComplicationText.FORMAT_STYLE_LOWER_CASE)
                        .build();

        // Time format rounds down, so the next change is at the next minute boundary.
        assertThat(text.getNextChangeTime(600000123)).isEqualTo(600060000);
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
