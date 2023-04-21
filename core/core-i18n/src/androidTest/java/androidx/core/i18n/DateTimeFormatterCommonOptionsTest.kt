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

import android.icu.text.DateFormat
import android.os.Build
import androidx.core.os.BuildCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Must execute on an Android device. */
@RunWith(AndroidJUnit4::class)
class DateTimeFormatterCommonOptionsTest {
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val testCalendar = GregorianCalendar(
        2021, Calendar.SEPTEMBER, 19, // Date
        21, 42, 12 // Time
    )

    @Test @SmallTest
    fun test() {
        val commonFormats = mapOf(
            DateTimeFormatterCommonOptions.ABBR_MONTH_WEEKDAY_DAY to "Sun, Sep 19",
            DateTimeFormatterCommonOptions.ABBR_MONTH_DAY to "Sep 19",
            DateTimeFormatterCommonOptions.MINUTE_SECOND to "42:12",
            DateTimeFormatterCommonOptions.MONTH_DAY to "September 19",
            DateTimeFormatterCommonOptions.MONTH_WEEKDAY_DAY to "Sunday, September 19",
            DateTimeFormatterCommonOptions.NUM_MONTH_DAY to "9/19",
            DateTimeFormatterCommonOptions.NUM_MONTH_WEEKDAY_DAY to "Sun, 9/19",
            DateTimeFormatterCommonOptions.YEAR_ABBR_MONTH to "Sep 2021",
            DateTimeFormatterCommonOptions.YEAR_ABBR_MONTH_DAY to "Sep 19, 2021",
            DateTimeFormatterCommonOptions.YEAR_ABBR_MONTH_WEEKDAY_DAY to "Sun, Sep 19, 2021",
            DateTimeFormatterCommonOptions.YEAR_MONTH to "September 2021",
            DateTimeFormatterCommonOptions.YEAR_MONTH_DAY to "September 19, 2021",
            DateTimeFormatterCommonOptions.YEAR_MONTH_WEEKDAY_DAY to "Sunday, September 19, 2021",
            DateTimeFormatterCommonOptions.YEAR_NUM_MONTH to "9/2021",
            DateTimeFormatterCommonOptions.YEAR_NUM_MONTH_DAY to "9/19/2021",
            DateTimeFormatterCommonOptions.YEAR_NUM_MONTH_WEEKDAY_DAY to "Sun, 9/19/2021"
        )
        val commonFormatsVersionDependent = when {
            BuildCompat.isAtLeastU() -> mapOf(
                DateTimeFormatterCommonOptions.HOUR_MINUTE to "9:42\u202FPM",
                DateTimeFormatterCommonOptions.HOUR_MINUTE_SECOND to "9:42:12\u202FPM"
            )
            else -> mapOf(
                DateTimeFormatterCommonOptions.HOUR_MINUTE to "9:42 PM",
                DateTimeFormatterCommonOptions.HOUR_MINUTE_SECOND to "9:42:12 PM"
            )
        }
        commonFormats.forEach { entry ->
            assertEquals(entry.value,
                DateTimeFormatter(appContext, entry.key, Locale.US).format(testCalendar))
        }
        commonFormatsVersionDependent.forEach { entry ->
            assertEquals(entry.value,
                DateTimeFormatter(appContext, entry.key, Locale.US).format(testCalendar))
        }
    }

    @Test @SmallTest
    fun testProperDefinitions() {
        val commonFormats = mapOf(
            DateTimeFormatterCommonOptions.ABBR_MONTH_DAY to "MMMd",
            DateTimeFormatterCommonOptions.ABBR_MONTH_WEEKDAY_DAY to "MMMEd",
            DateTimeFormatterCommonOptions.HOUR24_MINUTE to "Hm",
            DateTimeFormatterCommonOptions.HOUR24_MINUTE_SECOND to "Hms",
            DateTimeFormatterCommonOptions.HOUR_MINUTE to "jm",
            DateTimeFormatterCommonOptions.HOUR_MINUTE_SECOND to "jms",
            DateTimeFormatterCommonOptions.MINUTE_SECOND to "ms",
            DateTimeFormatterCommonOptions.MONTH_DAY to "MMMMd",
            DateTimeFormatterCommonOptions.MONTH_WEEKDAY_DAY to "MMMMEEEEd",
            DateTimeFormatterCommonOptions.NUM_MONTH_DAY to "Md",
            DateTimeFormatterCommonOptions.NUM_MONTH_WEEKDAY_DAY to "MEd",
            DateTimeFormatterCommonOptions.YEAR_ABBR_MONTH to "yMMM",
            DateTimeFormatterCommonOptions.YEAR_ABBR_MONTH_DAY to "yMMMd",
            DateTimeFormatterCommonOptions.YEAR_ABBR_MONTH_WEEKDAY_DAY to "yMMMEd",
            DateTimeFormatterCommonOptions.YEAR_MONTH to "yMMMM",
            DateTimeFormatterCommonOptions.YEAR_MONTH_DAY to "yMMMMd",
            DateTimeFormatterCommonOptions.YEAR_MONTH_WEEKDAY_DAY to "yMMMMEEEEd",
            DateTimeFormatterCommonOptions.YEAR_NUM_MONTH to "yM",
            DateTimeFormatterCommonOptions.YEAR_NUM_MONTH_DAY to "yMd",
            DateTimeFormatterCommonOptions.YEAR_NUM_MONTH_WEEKDAY_DAY to "yMEd"
        )
        commonFormats.forEach { entry ->
            assertEquals(entry.value, entry.key.toString())
        }
    }

    @Test @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    fun testProperDefinitionsAgainstIcu() {
        // Check that our definitions are the same as the ones used by ICU.
        val commonFormats = mapOf(
            DateTimeFormatterCommonOptions.ABBR_MONTH_DAY
                    to DateFormat.ABBR_MONTH_DAY,
            DateTimeFormatterCommonOptions.ABBR_MONTH_WEEKDAY_DAY
                    to DateFormat.ABBR_MONTH_WEEKDAY_DAY,
            DateTimeFormatterCommonOptions.HOUR24_MINUTE
                    to DateFormat.HOUR24_MINUTE,
            DateTimeFormatterCommonOptions.HOUR24_MINUTE_SECOND
                    to DateFormat.HOUR24_MINUTE_SECOND,
            DateTimeFormatterCommonOptions.HOUR_MINUTE
                    to DateFormat.HOUR_MINUTE,
            DateTimeFormatterCommonOptions.HOUR_MINUTE_SECOND
                    to DateFormat.HOUR_MINUTE_SECOND,
            DateTimeFormatterCommonOptions.MINUTE_SECOND
                    to DateFormat.MINUTE_SECOND,
            DateTimeFormatterCommonOptions.MONTH_DAY
                    to DateFormat.MONTH_DAY,
            DateTimeFormatterCommonOptions.MONTH_WEEKDAY_DAY
                    to DateFormat.MONTH_WEEKDAY_DAY,
            DateTimeFormatterCommonOptions.NUM_MONTH_DAY
                    to DateFormat.NUM_MONTH_DAY,
            DateTimeFormatterCommonOptions.NUM_MONTH_WEEKDAY_DAY
                    to DateFormat.NUM_MONTH_WEEKDAY_DAY,
            DateTimeFormatterCommonOptions.YEAR_ABBR_MONTH
                    to DateFormat.YEAR_ABBR_MONTH,
            DateTimeFormatterCommonOptions.YEAR_ABBR_MONTH_DAY
                    to DateFormat.YEAR_ABBR_MONTH_DAY,
            DateTimeFormatterCommonOptions.YEAR_ABBR_MONTH_WEEKDAY_DAY
                    to DateFormat.YEAR_ABBR_MONTH_WEEKDAY_DAY,
            DateTimeFormatterCommonOptions.YEAR_MONTH
                    to DateFormat.YEAR_MONTH,
            DateTimeFormatterCommonOptions.YEAR_MONTH_DAY
                    to DateFormat.YEAR_MONTH_DAY,
            DateTimeFormatterCommonOptions.YEAR_MONTH_WEEKDAY_DAY
                    to DateFormat.YEAR_MONTH_WEEKDAY_DAY,
            DateTimeFormatterCommonOptions.YEAR_NUM_MONTH
                    to DateFormat.YEAR_NUM_MONTH,
            DateTimeFormatterCommonOptions.YEAR_NUM_MONTH_DAY
                    to DateFormat.YEAR_NUM_MONTH_DAY,
            DateTimeFormatterCommonOptions.YEAR_NUM_MONTH_WEEKDAY_DAY
                    to DateFormat.YEAR_NUM_MONTH_WEEKDAY_DAY
        )
        commonFormats.forEach { entry ->
            assertEquals(entry.value, entry.key.toString())
        }
    }
}