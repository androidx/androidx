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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class TimeFormatTextTest {

    private Resources mResources = ApplicationProvider.getApplicationContext().getResources();

    @Test
    public void testDefaultStyleFormat() {
        // GIVEN a TimeFormatText object using the default style
        TimeFormatText tft =
                new TimeFormatText("E 'in' LLL", ComplicationText.FORMAT_STYLE_DEFAULT, null);

        // WHEN getText is called for a given date
        CharSequence result =
                tft.getText(null, new GregorianCalendar(2016, 2, 4).getTimeInMillis());

        // THEN the results are in the default case.
        assertEquals("Fri in Mar", result);
    }

    @Test
    public void testUpperCaseStyleFormat() {
        // GIVEN a TimeFormatText object using the upper case style
        TimeFormatText tft =
                new TimeFormatText("E 'in' LLL", ComplicationText.FORMAT_STYLE_UPPER_CASE, null);

        // WHEN getText is called for a given date
        CharSequence result =
                tft.getText(null, new GregorianCalendar(2016, 2, 4).getTimeInMillis());

        // THEN the results are in upper case.
        assertEquals("FRI IN MAR", result);
    }

    @Test
    public void testLowerCaseStyleFormat() {
        // GIVEN a TimeFormatText object using the lower case style
        TimeFormatText tft =
                new TimeFormatText("E 'in' LLL", ComplicationText.FORMAT_STYLE_LOWER_CASE, null);

        // WHEN getText is called for a given date
        CharSequence result =
                tft.getText(null, new GregorianCalendar(2016, 2, 4).getTimeInMillis());

        // THEN the results are in lower case.
        assertEquals("fri in mar", result);
    }

    @Test
    public void testFormatWithTimeZone() {
        // GIVEN a TimeFormatText object using the default style and a time zone specified.
        TimeFormatText tft =
                new TimeFormatText(
                        "HH:mm",
                        ComplicationText.FORMAT_STYLE_DEFAULT,
                        TimeZone.getTimeZone("America/Los_Angeles"));
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT+0"));
        calendar.set(2016, 2, 4, 15, 12, 58);

        // WHEN getText is called for a given date
        CharSequence result = tft.getText(null, calendar.getTimeInMillis());

        // THEN the results are adjusted for the time zone.
        assertEquals("07:12", result);
    }

    @Test
    public void testReturnSameTime() {
        // GIVEN a TimeFormatText object using the lower case style
        TimeFormatText tft =
                new TimeFormatText("HH:mm", ComplicationText.FORMAT_STYLE_LOWER_CASE, null);

        // WHEN returnsSameText is called for two time in the different period.
        // THEN the result of getText should be different.
        long testTime1 = TimeUnit.HOURS.toMillis(23) + TimeUnit.MINUTES.toMillis(10);
        long testTime2 = testTime1 + TimeUnit.MINUTES.toMillis(30);
        assertFalse(tft.returnsSameText(testTime1, testTime2));

        // WHEN returnsSameText is called for two time in the same period.
        // THEN the result of getText should be the same.
        testTime1 = TimeUnit.HOURS.toMillis(23) + TimeUnit.MINUTES.toMillis(10);
        testTime2 = testTime1 + TimeUnit.SECONDS.toMillis(30);
        assertTrue(tft.returnsSameText(testTime1, testTime2));

        // GIVEN a TimeFormatText object using the lower case style and a specify time zone.
        TimeZone tz = TimeZone.getTimeZone("America/Anchorage");
        tft = new TimeFormatText("d", ComplicationText.FORMAT_STYLE_LOWER_CASE, tz);
        Calendar cal = Calendar.getInstance();

        // WHEN returnsSameText is called for two time in different date.
        // THEN the result of getText should be the same.
        cal.setTimeZone(tz);
        cal.set(2016, 7, 7, 23, 59, 59);
        long time1 = cal.getTimeInMillis();
        cal.set(2016, 7, 8, 0, 0, 0);
        long time2 = cal.getTimeInMillis();
        assertFalse(tft.returnsSameText(time1, time2));
    }

    @Test
    public void testTimeFormatWithTimeZone() {
        TimeFormatText complicationText =
                new TimeFormatText(
                        "HH:mm",
                        ComplicationText.FORMAT_STYLE_DEFAULT,
                        TimeZone.getTimeZone("Asia/Seoul"));

        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT+0"));
        calendar.set(2016, 2, 4, 18, 52, 58);
        CharSequence result = complicationText.getText(mResources, calendar.getTimeInMillis());
        assertEquals("03:52", result.toString());
    }

    @Test
    public void testParcelTimeFormatTextWithoutTimeZone() {
        // GIVEN TimeFormatText with no time zone
        TimeFormatText originalText =
                new TimeFormatText(
                        "EEE 'the' d LLL", ComplicationText.FORMAT_STYLE_LOWER_CASE, null);

        // WHEN the object is parcelled and unparcelled
        TimeFormatText newText = roundTripParcelable(originalText);

        // THEN the object behaves as expected.
        CharSequence result =
                newText.getText(mResources, new GregorianCalendar(2016, 2, 4).getTimeInMillis());
        assertEquals("fri the 4 mar", result.toString());
    }

    @Test
    public void testParcelTimeFormatTextWithTimeZone() {
        // GIVEN TimeFormatText with a time zone specified
        TimeFormatText originalText =
                new TimeFormatText(
                        "EEE 'the' d LLL HH:mm",
                        ComplicationText.FORMAT_STYLE_LOWER_CASE,
                        TimeZone.getTimeZone("GMT+5"));

        // WHEN the object is parcelled and unparcelled
        TimeFormatText newText = roundTripParcelable(originalText);

        // THEN the object behaves as expected.
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT+0"));
        calendar.set(2016, 2, 4, 18, 52, 58);
        CharSequence result = newText.getText(mResources, calendar.getTimeInMillis());
        assertEquals("fri the 4 mar 23:52", result.toString());
    }

    @Test
    public void nextChangeTimeSeconds() {
        TimeFormatText timeFormatText =
                new TimeFormatText(
                        "HH:mm:ss", ComplicationText.FORMAT_STYLE_DEFAULT, TimeZone.getDefault());

        // Next change is at the next second boundary.
        assertThat(timeFormatText.getNextChangeTime(1000035789L)).isEqualTo(1000036000L);
        assertThat(timeFormatText.getNextChangeTime(7000060000L)).isEqualTo(7000061000L);
        assertThat(timeFormatText.getNextChangeTime(1900000001L)).isEqualTo(1900001000L);
        assertThat(timeFormatText.getNextChangeTime(1111100999L)).isEqualTo(1111101000L);
    }

    @Test
    public void nextChangeTimeMinutely() {
        TimeFormatText timeFormatText =
                new TimeFormatText(
                        "HH:mm",
                        ComplicationText.FORMAT_STYLE_DEFAULT,
                        TimeZone.getTimeZone("Asia/Seoul"));

        // Next change is at the next minute boundary.
        assertThat(timeFormatText.getNextChangeTime(6000035789L)).isEqualTo(6000060000L);
        assertThat(timeFormatText.getNextChangeTime(6000060000L)).isEqualTo(6000120000L);
        assertThat(timeFormatText.getNextChangeTime(6000060001L)).isEqualTo(6000120000L);
        assertThat(timeFormatText.getNextChangeTime(6000059999L)).isEqualTo(6000060000L);
    }

    @Test
    public void nextChangeTimeHourly() {
        TimeFormatText timeFormatText =
                new TimeFormatText(
                        "d LLL HH", ComplicationText.FORMAT_STYLE_DEFAULT, TimeZone.getDefault());

        // Next change is at the next hour boundary.
        assertThat(timeFormatText.getNextChangeTime(36000035789L)).isEqualTo(36003600000L);
        assertThat(timeFormatText.getNextChangeTime(36000600000L)).isEqualTo(36003600000L);
        assertThat(timeFormatText.getNextChangeTime(36003600000L)).isEqualTo(36007200000L);
        assertThat(timeFormatText.getNextChangeTime(36007199999L)).isEqualTo(36007200000L);
    }

    @Test
    public void nextChangeTimeDailyUtc() {
        TimeFormatText timeFormatText =
                new TimeFormatText(
                        "d LLL",
                        ComplicationText.FORMAT_STYLE_DEFAULT,
                        TimeZone.getTimeZone("UTC"));

        // Next change is at the next day boundary.
        assertThat(timeFormatText.getNextChangeTime(8640000035789L)).isEqualTo(8640086400000L);
        assertThat(timeFormatText.getNextChangeTime(8640086399999L)).isEqualTo(8640086400000L);
        assertThat(timeFormatText.getNextChangeTime(8640086400000L)).isEqualTo(8640172800000L);
    }

    @Test
    public void nextChangeTimeDailyTimeZone() {
        TimeFormatText timeFormatText =
                new TimeFormatText(
                        "d LLL",
                        ComplicationText.FORMAT_STYLE_DEFAULT,
                        TimeZone.getTimeZone("GMT-5"));

        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.set(2018, Calendar.APRIL, 17, 8, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long inputTime = calendar.getTimeInMillis();

        // Next change is at midnight in the specified time zone.
        calendar.set(2018, Calendar.APRIL, 18, 5, 0, 0);
        long expectedTime = calendar.getTimeInMillis();

        assertThat(timeFormatText.getNextChangeTime(inputTime)).isEqualTo(expectedTime);
    }

    @Test
    public void nextChangeTimeDailyTimeZoneNextDay() {
        TimeFormatText timeFormatText =
                new TimeFormatText(
                        "d LLL",
                        ComplicationText.FORMAT_STYLE_DEFAULT,
                        TimeZone.getTimeZone("GMT+5"));

        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.set(2018, Calendar.APRIL, 17, 23, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long inputTime = calendar.getTimeInMillis();

        // Next change is at midnight in the specified time zone, the day after the start time
        // (which
        // is after midnight in GMT+5).
        calendar.set(2018, Calendar.APRIL, 18, 19, 0, 0);
        long expectedTime = calendar.getTimeInMillis();

        assertThat(timeFormatText.getNextChangeTime(inputTime)).isEqualTo(expectedTime);
    }

    @Test
    public void testTimeFormatTextGetPrecision() {
        String[] dateFormats = {
            "S",
            "s",
            "d",
            "m",
            "h",
            "D",
            "W",
            "y",
            "E",
            "w",
            "EEE 'the' d LLL HH:mm",
            "'now time is' HH 'o''clock'",
            "yyyy-MM-dd",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd HH:mmZ",
            "yyyy-MM-dd HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy ''' hello'",
            "yyyy ''''' hello' s",
            "LLL",
            "LLL a"
        };

        long[] dateFormatPrecision = {
            SECONDS.toMillis(1),
            SECONDS.toMillis(1),
            DAYS.toMillis(1),
            MINUTES.toMillis(1),
            HOURS.toMillis(1),
            DAYS.toMillis(1),
            DAYS.toMillis(1),
            DAYS.toMillis(1),
            DAYS.toMillis(1),
            DAYS.toMillis(1),
            MINUTES.toMillis(1),
            HOURS.toMillis(1),
            DAYS.toMillis(1),
            MINUTES.toMillis(1),
            MINUTES.toMillis(1),
            SECONDS.toMillis(1),
            SECONDS.toMillis(1),
            DAYS.toMillis(1),
            SECONDS.toMillis(1),
            DAYS.toMillis(1),
            HOURS.toMillis(12)
        };

        for (int i = 0; i < dateFormats.length; i++) {
            TimeFormatText text =
                    new TimeFormatText(dateFormats[i], ComplicationText.FORMAT_STYLE_DEFAULT, null);
            assertEquals(dateFormatPrecision[i], text.getPrecision());
        }
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
