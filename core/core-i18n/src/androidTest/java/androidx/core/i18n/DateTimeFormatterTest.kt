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

package androidx.core.i18n

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.core.os.BuildCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import kotlin.test.assertFailsWith
import androidx.core.i18n.DateTimeFormatterSkeletonOptions as SkeletonOptions

/** Must execute on an Android device. */
@RunWith(AndroidJUnit4::class)
@SuppressLint("ClassVerificationFailure")
class DateTimeFormatterTest {
    companion object {
        // Lollipop introduced Locale.toLanguageTag and Locale.forLanguageTag
        private const val AVAILABLE_LANGUAGE_TAG = Build.VERSION_CODES.LOLLIPOP

        // From this version we can access ICU4J public APIs (android.icu.*)
        private const val AVAILABLE_ICU4J = Build.VERSION_CODES.N
        private const val AVAILABLE_HC_U_EXT = Build.VERSION_CODES.S
        private const val AVAILABLE_PERIOD_B = Build.VERSION_CODES.Q

        private var sSavedTimeZone: TimeZone? = null

        @BeforeClass
        fun beforeClass() {
            sSavedTimeZone = TimeZone.getDefault()
        }
    }

    /** Starting with Android N ICU4J is public API. */
    private val isIcuAvailable = Build.VERSION.SDK_INT >= AVAILABLE_ICU4J
    /** Starting with Android S ICU honors the "-u-hc-" extension in locale id. */
    private val isHcExtensionHonored = Build.VERSION.SDK_INT >= AVAILABLE_HC_U_EXT
    /** Starting with Android Q ICU supports "b" and "B". */
    private val isFlexiblePeriodAvailable = Build.VERSION.SDK_INT >= AVAILABLE_PERIOD_B

    private val logTag = this::class.qualifiedName
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    private val defaultTimeZone = TimeZone.getTimeZone("America/Los_Angeles")
    private val testCalendar = GregorianCalendar(defaultTimeZone)

    init {
        // Sept 19, 2021, 21:42:12.345
        testCalendar.timeInMillis = 1632112932345L
    }

    private val testDate = testCalendar.time
    private val testMillis = testCalendar.timeInMillis

    @Before
    fun beforeTest() {
        // Some of the test check that the functionality honors the default timezone.
        // So we make sure it is set to something we control.
        TimeZone.setDefault(defaultTimeZone)
    }

    @After
    fun afterTest() {
        if (sSavedTimeZone != null) {
            TimeZone.setDefault(sSavedTimeZone)
        }
    }

    @Test @SmallTest
    fun test() {
        val locale = Locale.US
        val options = SkeletonOptions.fromString("yMMMdjms")
        val expected = when {
            BuildCompat.isAtLeastU() -> "Sep 19, 2021, 9:42:12\u202FPM"
            else -> "Sep 19, 2021, 9:42:12 PM"
        }

        // Test Calendar
        assertEquals(expected, DateTimeFormatter(appContext, options, locale).format(testCalendar))
        // Test Date
        assertEquals(expected, DateTimeFormatter(appContext, options, locale).format(testDate))
        // Test milliseconds
        assertEquals(expected, DateTimeFormatter(appContext, options, locale).format(testMillis))
    }

    @Test @SmallTest
    fun testApi() {
        val builder = SkeletonOptions.Builder()
            .setYear(SkeletonOptions.Year.NUMERIC)
            .setMonth(SkeletonOptions.Month.ABBREVIATED)
            .setDay(SkeletonOptions.Day.NUMERIC)
            .setHour(SkeletonOptions.Hour.NUMERIC)
            .setMinute(SkeletonOptions.Minute.NUMERIC)
            .setSecond(SkeletonOptions.Second.NUMERIC)

        val localeFr = Locale.FRANCE
        val localeUs = Locale.US

        val expectedUs12 = when {
            BuildCompat.isAtLeastU() -> "Sep 19, 2021, 9:42:12\u202FPM"
            else -> "Sep 19, 2021, 9:42:12 PM"
        }
        val expectedUs24 = "Sep 19, 2021, 21:42:12"
        val expectedUs12Milli = when {
            BuildCompat.isAtLeastU() -> "Sep 19, 2021, 9:42:12.345\u202FPM"
            else -> "Sep 19, 2021, 9:42:12.345 PM"
        }

        val expectedFr12: String
        val expectedFr24: String
        when {
             BuildCompat.isAtLeastU() -> { // >= 31
                 expectedFr12 = "19 sept. 2021, 9:42:12\u202FPM"
                 expectedFr24 = "19 sept. 2021, 21:42:12"
             }
             Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { // >= 31
                expectedFr12 = "19 sept. 2021, 9:42:12 PM"
                expectedFr24 = "19 sept. 2021, 21:42:12"
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> { // [24, 31)
                expectedFr12 = "19 sept. 2021 à 9:42:12 PM"
                expectedFr24 = "19 sept. 2021 à 21:42:12"
            }
            else -> { // < 24
                expectedFr12 = "19 sept. 2021 9:42:12 PM"
                expectedFr24 = "19 sept. 2021 21:42:12"
            }
        }

        var options = builder.build()

        var formatter = DateTimeFormatter(appContext, options, localeFr)
        assertEquals(expectedFr24, formatter.format(testDate))
        assertEquals(expectedFr24, formatter.format(testCalendar))

        options = builder.setHour(SkeletonOptions.Hour.NUMERIC).build()
        formatter = DateTimeFormatter(appContext, options, localeFr)
        assertEquals(expectedFr24, formatter.format(testDate)) // fr-FR default is h24

        options = builder.setHour(SkeletonOptions.Hour.FORCE_12H_NUMERIC).build()
        formatter = DateTimeFormatter(appContext, options, localeFr)
        assertEquals(expectedFr12, formatter.format(testDate)) // force to h12

        options = builder.setHour(SkeletonOptions.Hour.FORCE_24H_NUMERIC).build()
        formatter = DateTimeFormatter(appContext, options, localeFr)
        assertEquals(expectedFr24, formatter.format(testDate)) // force to h24

        options = builder.setHour(SkeletonOptions.Hour.NUMERIC).build()
        formatter = DateTimeFormatter(appContext, options, localeUs)
        assertEquals(expectedUs12, formatter.format(testDate)) // en-US default is h12

        options = builder.setHour(SkeletonOptions.Hour.FORCE_12H_NUMERIC).build()
        formatter = DateTimeFormatter(appContext, options, localeUs)
        assertEquals(expectedUs12, formatter.format(testDate)) // force to h12

        options = builder.setHour(SkeletonOptions.Hour.FORCE_24H_NUMERIC).build()
        formatter = DateTimeFormatter(appContext, options, localeUs)
        assertEquals(expectedUs24, formatter.format(testDate)) // force to h12

        // Make sure that the milliseconds are formatted
        options = builder
            .setHour(SkeletonOptions.Hour.NUMERIC)
            .setFractionalSecond(SkeletonOptions.FractionalSecond.NUMERIC_3_DIGITS)
            .build()
        formatter = DateTimeFormatter(appContext, options, localeUs)
        assertEquals(expectedUs12Milli, formatter.format(testDate))
        assertEquals(expectedUs12Milli, formatter.format(testCalendar))
    }

    @Test @SmallTest
    @SdkSuppress(minSdkVersion = AVAILABLE_LANGUAGE_TAG)
    // Without `Locale.forLanguageTag` we can't even build a locale with `-u-` extension.
    fun testSystemSupportForExtensionU() {
        val enUsForceH11 = Locale.forLanguageTag("en-US-u-hc-h11")
        val enUsForceH12 = Locale.forLanguageTag("en-US-u-hc-h12")
        val enUsForceH23 = Locale.forLanguageTag("en-US-u-hc-h23")
        val enUsForceH24 = Locale.forLanguageTag("en-US-u-hc-h24")

        val expectedUs: String = "9:42:12 PM"
        val expectedUs11: String = expectedUs
        val expectedUs12: String = expectedUs
        // TODO: check this. Is `-u-hc-` not honored at all?
        // Official bug: https://unicode-org.atlassian.net/browse/ICU-11870
        // It only manifests for the predefined formats (`DateFormat.MEDIUM` and so on),
        // not for patterns generated from skeletons.
        val expectedUs23: String = expectedUs
        val expectedUs24: String = expectedUs

        var formatter: java.text.DateFormat

        // Formatting with style does not honor the uc overrides
        formatter = java.text.DateFormat.getTimeInstance(java.text.DateFormat.MEDIUM, Locale.US)
        assertEquals(expectedUs, Helper.normalizeNnbsp(formatter.format(testMillis)))
        formatter = java.text.DateFormat.getTimeInstance(java.text.DateFormat.MEDIUM, enUsForceH11)
        assertEquals(expectedUs11, Helper.normalizeNnbsp(formatter.format(testMillis)))
        formatter = java.text.DateFormat.getTimeInstance(java.text.DateFormat.MEDIUM, enUsForceH12)
        assertEquals(expectedUs12, Helper.normalizeNnbsp(formatter.format(testMillis)))
        formatter = java.text.DateFormat.getTimeInstance(java.text.DateFormat.MEDIUM, enUsForceH23)
        assertEquals(expectedUs23, Helper.normalizeNnbsp(formatter.format(testMillis)))
        formatter = java.text.DateFormat.getTimeInstance(java.text.DateFormat.MEDIUM, enUsForceH24)
        assertEquals(expectedUs24, Helper.normalizeNnbsp(formatter.format(testMillis)))
    }

    @Test @SmallTest
    @SdkSuppress(minSdkVersion = AVAILABLE_LANGUAGE_TAG)
    fun testHourCycleOverrides() {
        val expectedUs12 = when {
            BuildCompat.isAtLeastU() -> "Sep 19, 2021, 9:42:12\u202FPM"
            else -> "Sep 19, 2021, 9:42:12 PM"
        }
        val expectedUs24 = "Sep 19, 2021, 21:42:12"
        val builder = SkeletonOptions.Builder()
            .setYear(SkeletonOptions.Year.NUMERIC)
            .setMonth(SkeletonOptions.Month.ABBREVIATED)
            .setDay(SkeletonOptions.Day.NUMERIC)
            .setHour(SkeletonOptions.Hour.NUMERIC)
            .setMinute(SkeletonOptions.Minute.NUMERIC)
            .setSecond(SkeletonOptions.Second.NUMERIC)
        val locale = Locale.forLanguageTag("en-US-u-hc-h23")

        var formatter: DateTimeFormatter
        if (isHcExtensionHonored) {
            formatter = DateTimeFormatter(
                appContext,
                builder.setHour(SkeletonOptions.Hour.NUMERIC).build(),
                locale
            )
            // en-US default is h12, but hc forces it to 24
            assertEquals(expectedUs24, formatter.format(testDate))
        } else {
            formatter = DateTimeFormatter(
                appContext,
                builder.setHour(SkeletonOptions.Hour.NUMERIC).build(),
                locale
            )
            assertEquals(expectedUs12, formatter.format(testDate)) // hc is ignored
        }

        formatter = DateTimeFormatter(
            appContext,
            builder.setHour(SkeletonOptions.Hour.FORCE_12H_NUMERIC).build(),
            locale
        )
        assertEquals(expectedUs12, formatter.format(testDate)) // force to h12

        formatter = DateTimeFormatter(
            appContext,
            builder.setHour(SkeletonOptions.Hour.FORCE_24H_NUMERIC).build(),
            locale
        )
        assertEquals(expectedUs24, formatter.format(testDate)) // force to h12
    }

    @Test @LargeTest
    fun testApi26And27PatternHasB() {
        val options = SkeletonOptions.fromString("yMMMdjmsSSS")
        val now = Date()

        // TODO: move to a different test class and try to abuse
        //  all skeleton combinations on all API levels
        for (locale in Locale.getAvailableLocales()) {
            try {
                DateTimeFormatter(appContext, options, locale).format(now)
            } catch (e: java.lang.IllegalArgumentException) {
                Log.e(logTag, "Failed for '" + locale + "': " + e.message)
            }
        }
    }

    @Test @SmallTest
    fun testBbb() {
        val builder = SkeletonOptions.Builder()
            .setHour(SkeletonOptions.Hour.NUMERIC)
            .setPeriod(SkeletonOptions.Period.FLEXIBLE)
            .setMinute(SkeletonOptions.Minute.NUMERIC)

        val formatterUs = DateTimeFormatter(appContext, builder.build(), Locale.US)
        val formatterZh = DateTimeFormatter(appContext, builder.build(), Locale.CHINA)
        val formatterFr = DateTimeFormatter(appContext, builder.build(), Locale.FRANCE)

        val expectedUs = if (isFlexiblePeriodAvailable) {
            "12:43 at night || 4:43 at night || 8:43 in the morning || " +
                    "12:43 in the afternoon || 4:43 in the afternoon || 8:43 in the evening"
        } else {
            "12:43 AM || 4:43 AM || 8:43 AM || 12:43 PM || 4:43 PM || 8:43 PM"
        }
        val expectedZh = when {
            // Chinese changed to 24h from ICU 70.1
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                "00:43 || 04:43 || 08:43 || 12:43 || 16:43 || 20:43"
            isFlexiblePeriodAvailable ->
                "凌晨12:43 || 凌晨4:43 || 上午8:43 || 中午12:43 || 下午4:43 || 晚上8:43"
            else ->
                "上午12:43 || 上午4:43 || 上午8:43 || 下午12:43 || 下午4:43 || 下午8:43"
        }
        val expectedFr = "00:43 || 04:43 || 08:43 || 12:43 || 16:43 || 20:43"

        val calendar = Calendar.getInstance()
        val separator = " || "

        // StringJoiner would be nicer, but it is only available from Android 24
        val resultUs = StringBuilder()
        val resultZh = StringBuilder()
        val resultFr = StringBuilder()

        for (hour in 0..23 step 4) {
            calendar.set(2022, Calendar.JANUARY, 15, hour, 43)
            if (hour != 0) {
                resultUs.append(separator)
                resultZh.append(separator)
                resultFr.append(separator)
            }
            resultUs.append(formatterUs.format(calendar))
            resultZh.append(formatterZh.format(calendar))
            resultFr.append(formatterFr.format(calendar))
        }

        assertEquals(expectedUs, resultUs.toString())
        assertEquals(expectedZh, resultZh.toString())
        assertEquals(expectedFr, resultFr.toString())
    }

    @Test @SmallTest
    fun testEra() {
        val builder = SkeletonOptions.Builder()
            .setYear(SkeletonOptions.Year.NUMERIC)
            .setMonth(SkeletonOptions.Month.ABBREVIATED)
            .setEra(SkeletonOptions.Era.ABBREVIATED)

        val dateBc = Calendar.getInstance()
        dateBc.set(-42, Calendar.SEPTEMBER, 21)
        assertEquals(
            "Sep 43 BC", // There is no year 0, so -42 means 43 BC
            DateTimeFormatter(appContext, builder.build(), Locale.US).format(dateBc)
        )

        assertEquals(
            "Sep 2021 AD",
            DateTimeFormatter(appContext, builder.build(), Locale.US).format(testDate)
        )

        assertEquals(
            if (isIcuAvailable) "Sep 2021 Anno Domini" else "Sep 2021 AD",
            DateTimeFormatter(
                appContext,
                builder.setEra(SkeletonOptions.Era.WIDE).build(),
                Locale.US
            ).format(testDate)
        )
    }

    @Test @SmallTest
    fun testWeekDay() {
        val builder = SkeletonOptions.Builder()
            .setYear(SkeletonOptions.Year.NUMERIC)
            .setMonth(SkeletonOptions.Month.WIDE)
            .setDay(SkeletonOptions.Day.NUMERIC)
            .setWeekDay(SkeletonOptions.WeekDay.ABBREVIATED)

        assertEquals(
            "Sun, September 19, 2021",
            DateTimeFormatter(appContext, builder.build(), Locale.US).format(testDate)
        )
    }

    @Test @SmallTest
    fun testTimeZone() {
        val builder = SkeletonOptions.Builder()
            .setHour(SkeletonOptions.Hour.NUMERIC)
            .setMinute(SkeletonOptions.Minute.NUMERIC)
            .setTimezone(SkeletonOptions.Timezone.LONG)
        val locale = Locale.US

        val timeZone = TimeZone.getTimeZone("America/Denver")
        val coloradoTime = Calendar.getInstance(timeZone, locale)
        coloradoTime.set(
            2021, Calendar.AUGUST, 19, // Date
            21, 42, 12
        ) // Time

        var options = builder.build()
        assertEquals(
            when {
                BuildCompat.isAtLeastU() -> "9:42\u202FPM Mountain Daylight Time"
                isIcuAvailable -> "9:42 PM Mountain Daylight Time"
                else -> "8:42 PM Pacific Daylight Time"
            },
            DateTimeFormatter(appContext, options, locale).format(coloradoTime)
        )

        options = builder.setTimezone(SkeletonOptions.Timezone.SHORT).build()
        assertEquals(
            when {
                BuildCompat.isAtLeastU() -> "9:42\u202FPM MDT"
                isIcuAvailable -> "9:42 PM MDT"
                else -> "8:42 PM PDT"
             },
            DateTimeFormatter(appContext, options, locale).format(coloradoTime)
        )

        options = builder.setTimezone(SkeletonOptions.Timezone.SHORT_GENERIC).build()
        assertEquals(
            when {
                BuildCompat.isAtLeastU() -> "9:42\u202FPM MT"
                isIcuAvailable -> "9:42 PM MT"
                else -> "8:42 PM PDT"
             },
            DateTimeFormatter(appContext, options, locale).format(coloradoTime)
        )

        options = builder.setTimezone(SkeletonOptions.Timezone.SHORT_OFFSET).build()
        assertEquals(
            when {
                BuildCompat.isAtLeastU() -> "9:42\u202FPM GMT-6"
                isIcuAvailable -> "9:42 PM GMT-6"
                else -> "8:42 PM PDT"
             },
            DateTimeFormatter(appContext, options, locale).format(coloradoTime)
        )
    }

    @Test @SmallTest
    // Making sure the APIs honor the default timezone
    fun testDefaultTimeZone() {
        val options = SkeletonOptions.Builder()
            .setHour(SkeletonOptions.Hour.NUMERIC)
            .setMinute(SkeletonOptions.Minute.NUMERIC)
            .setTimezone(SkeletonOptions.Timezone.LONG)
            .build()
        val locale = Locale.US

        // Honor the current default timezone
        val expPDT = when {
            BuildCompat.isAtLeastU() -> "9:42\u202FPM Pacific Daylight Time"
            else -> "9:42 PM Pacific Daylight Time"
        }
        // Test Calendar, Date, and milliseconds
        assertEquals(expPDT, DateTimeFormatter(appContext, options, locale).format(testCalendar))
        assertEquals(expPDT, DateTimeFormatter(appContext, options, locale).format(testDate))
        assertEquals(expPDT, DateTimeFormatter(appContext, options, locale).format(testMillis))

        // Change the default timezone.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Denver"))
        val expMDT = when {
            BuildCompat.isAtLeastU() -> "10:42\u202FPM Mountain Daylight Time"
            else -> "10:42 PM Mountain Daylight Time"
        }
        // The calendar object already has a time zone of its own, captured at creation time.
        // So not matching the default changed after is the expected behavior.
        // BUT!
        // Below N there is no ICU, so we fallback to `java.text.DateFormat`.
        // Which can't format a Calendar, only Date and Number (millisecond, epoch time)
        // So the time zone info is lost below N. It is the best we can do.
        val expCal = if (isIcuAvailable) expPDT else expMDT
        assertEquals(expCal, DateTimeFormatter(appContext, options, locale).format(testCalendar))
        assertEquals(expMDT, DateTimeFormatter(appContext, options, locale).format(testDate))
        assertEquals(expMDT, DateTimeFormatter(appContext, options, locale).format(testMillis))
        // The default timezone is restored in @Before, no need to change it back
    }

    @Test @SmallTest
    fun testEmptySkeleton() {
        val options = SkeletonOptions.fromString("")
        assertEquals("",
            DateTimeFormatter(appContext, options, Locale.US).format(testDate)
        )
    }

    @Test @SmallTest
    fun testInvalidSkeletonField_throwsIAE() {
        assertFailsWith<IllegalArgumentException> {
            SkeletonOptions.fromString("fiInNopPRtT")
        }
    }
}