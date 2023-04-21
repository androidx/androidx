/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.core.i18n;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.os.Build;

import androidx.core.i18n.DateTimeFormatterSkeletonOptions.Day;
import androidx.core.i18n.DateTimeFormatterSkeletonOptions.Era;
import androidx.core.i18n.DateTimeFormatterSkeletonOptions.FractionalSecond;
import androidx.core.i18n.DateTimeFormatterSkeletonOptions.Hour;
import androidx.core.i18n.DateTimeFormatterSkeletonOptions.Minute;
import androidx.core.i18n.DateTimeFormatterSkeletonOptions.Month;
import androidx.core.i18n.DateTimeFormatterSkeletonOptions.Period;
import androidx.core.i18n.DateTimeFormatterSkeletonOptions.Second;
import androidx.core.i18n.DateTimeFormatterSkeletonOptions.Timezone;
import androidx.core.i18n.DateTimeFormatterSkeletonOptions.WeekDay;
import androidx.core.i18n.DateTimeFormatterSkeletonOptions.Year;
import androidx.core.os.BuildCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This is less about testing and more to give us a feeling on how it is to use the APIs.
 */
@RunWith(AndroidJUnit4.class)
public class CheckTheJavaApisTest {
    private static TimeZone sSavedTimeZone;
    private final Context mAppContext =
            InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final Locale mLocale = Locale.GERMANY;
    private final Calendar mCal =
            Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"), mLocale);
    {
        mCal.set(2021, Calendar.AUGUST, 23, 19, 53, 47);
        mCal.set(Calendar.MILLISECOND, 123);
    }

    @BeforeClass
    public static void beforeClass() {
        sSavedTimeZone = TimeZone.getDefault();
    }

    @Before
    public void beforeTest() {
        // Some of the test check that the functionality honors the default timezone.
        // So we make sure it is set to something we control.
        TimeZone tzLosAngeles = TimeZone.getTimeZone("America/Los_Angeles");
        TimeZone.setDefault(tzLosAngeles);
    }

    @After
    public void afterTest() {
        if (sSavedTimeZone != null) {
            TimeZone.setDefault(sSavedTimeZone);
        }
    }

    @Test @SmallTest
    public void testSkeletonOptions() {
        final DateTimeFormatterSkeletonOptions.Builder builder =
                new DateTimeFormatterSkeletonOptions.Builder()
                        .setYear(Year.NUMERIC)
                        .setMonth(Month.WIDE)
                        .setDay(Day.NUMERIC)
                        .setWeekDay(WeekDay.ABBREVIATED)
                        .setHour(Hour.NUMERIC)
                        .setMinute(Minute.NUMERIC)
                        .setSecond(Second.NUMERIC)
                        .setFractionalSecond(FractionalSecond.NUMERIC_3_DIGITS)
                        .setEra(Era.ABBREVIATED)
                        .setTimezone(Timezone.SHORT_GENERIC)
                        .setPeriod(Period.FLEXIBLE);

        DateTimeFormatterSkeletonOptions dateTimeFormatterOptions = builder.build();
        String expected;
        if (BuildCompat.isAtLeastU()) {
            expected = "Mo., 23. August 2021 n. Chr. um 19:53:47,123 MEZ";
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            expected = "Mo., 23. August 2021 n. Chr., 19:53:47,123 MEZ";
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            expected = "Mo., 23. August 2021 n. Chr., 10:53:47,123 GMT-07:00";
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            expected = "Mo., 23. August 2021 n. Chr. 10:53:47,123 GMT-07:00";
        } else {
            expected = "Mo., 23. August 2021 n. Chr. 10:53:47,123 PDT";
        }
        // TODO: Bug? The hour is very different below M, as if the timezone is not supported.
        // 19 vs 10 is Paris vs California time

        DateTimeFormatter df =
                new DateTimeFormatter(mAppContext, dateTimeFormatterOptions, mLocale);
        assertEquals(expected, df.format(mCal));
        assertEquals(
                expected,
                new DateTimeFormatter(mAppContext, dateTimeFormatterOptions, mLocale).format(mCal));

        // Verify DateTimeFormatterSkeletonOptions.fromString
        dateTimeFormatterOptions = DateTimeFormatterSkeletonOptions.fromString("yMMMdjms");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            expected = "23. Aug. 2021, 19:53:47";
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            expected = "23. Aug. 2021, 10:53:47";
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            expected = "23. Aug. 2021 10:53:47";
        } else {
            expected = "23. Aug. 2021 10:53:47";
        }
        df = new DateTimeFormatter(mAppContext, dateTimeFormatterOptions, mLocale);
        assertEquals(expected, df.format(mCal));
        assertEquals(
                expected,
                new DateTimeFormatter(mAppContext, dateTimeFormatterOptions, mLocale).format(mCal));

        // Verify DateTimeFormatterSkeletonOptions.fromString
        dateTimeFormatterOptions = DateTimeFormatterSkeletonOptions.fromString("yMMMMdjmsEEEE");
        if (BuildCompat.isAtLeastU()) {
            expected = "Montag, 23. August 2021 um 19:53:47";
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            expected = "Montag, 23. August 2021, 19:53:47";
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            expected = "Montag, 23. August 2021, 10:53:47";
        } else {
            expected = "Montag, 23. August 2021 10:53:47";
        }
        df = new DateTimeFormatter(mAppContext, dateTimeFormatterOptions, mLocale);
        assertEquals(expected, df.format(mCal));
        assertEquals(
                expected,
                new DateTimeFormatter(mAppContext, dateTimeFormatterOptions, mLocale).format(mCal));
    }

    @Test @SmallTest
    public void testJdkOptions() {
        DateTimeFormatterJdkStyleOptions options =
                DateTimeFormatterJdkStyleOptions.createDateInstance(DateFormat.LONG);
        assertEquals("23. August 2021",
                new DateTimeFormatter(options, mLocale).format(mCal));
    }
}
