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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.text.DateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class DateTimeFormatJdkStylesTest {
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val testCalendar = GregorianCalendar(
        2021, Calendar.SEPTEMBER, 19, // Date
        23, 42, 12 // Time
    )

    init {
        testCalendar.set(Calendar.MILLISECOND, 345)
    }

    @Test @SmallTest
    fun testEmulateJavaStyles() {
        val locale = Locale.US

        checkDate(locale, DateFormat.FULL)
        checkDate(locale, DateFormat.LONG)
        checkDate(locale, DateFormat.MEDIUM)
        checkDate(locale, DateFormat.SHORT)
        checkDate(locale, DateFormat.DEFAULT)

        checkTime(locale, DateFormat.FULL)
        checkTime(locale, DateFormat.LONG)
        checkTime(locale, DateFormat.MEDIUM)
        checkTime(locale, DateFormat.SHORT)
        checkTime(locale, DateFormat.DEFAULT)

        checkDateTime(locale, DateFormat.FULL, DateFormat.FULL)
        checkDateTime(locale, DateFormat.FULL, DateFormat.LONG)
        checkDateTime(locale, DateFormat.FULL, DateFormat.MEDIUM)
        checkDateTime(locale, DateFormat.FULL, DateFormat.SHORT)
        checkDateTime(locale, DateFormat.LONG, DateFormat.FULL)
        checkDateTime(locale, DateFormat.LONG, DateFormat.LONG)
        checkDateTime(locale, DateFormat.LONG, DateFormat.MEDIUM)
        checkDateTime(locale, DateFormat.LONG, DateFormat.SHORT)
        checkDateTime(locale, DateFormat.MEDIUM, DateFormat.FULL)
        checkDateTime(locale, DateFormat.MEDIUM, DateFormat.LONG)
        checkDateTime(locale, DateFormat.MEDIUM, DateFormat.MEDIUM)
        checkDateTime(locale, DateFormat.MEDIUM, DateFormat.SHORT)
        checkDateTime(locale, DateFormat.SHORT, DateFormat.FULL)
        checkDateTime(locale, DateFormat.SHORT, DateFormat.LONG)
        checkDateTime(locale, DateFormat.SHORT, DateFormat.MEDIUM)
        checkDateTime(locale, DateFormat.SHORT, DateFormat.SHORT)
    }

    private fun checkDate(locale: Locale, javaStyle: @DateTimeStyle Int) {
        val jdkFormatter = DateFormat.getDateInstance(javaStyle, locale)
        val options = DateTimeFormatterJdkStyleOptions.createDateInstance(javaStyle)
        val compatFormatter = DateTimeFormatter(options, locale)
        assertEquals(jdkFormatter.format(testCalendar.time), compatFormatter.format(testCalendar))
    }

    private fun checkTime(locale: Locale, javaStyle: @DateTimeStyle Int) {
        val jdkFormatter = DateFormat.getTimeInstance(javaStyle, locale)
        val options = DateTimeFormatterJdkStyleOptions.createTimeInstance(javaStyle)
        val compatFormatter = DateTimeFormatter(options, locale)
        assertEquals(
            Helper.normalizeNnbsp(jdkFormatter.format(testCalendar.time)),
            Helper.normalizeNnbsp(compatFormatter.format(testCalendar)))
    }

    private fun checkDateTime(
        locale: Locale,
        javaDateStyle: @DateTimeStyle Int,
        javaTimeStyle: @DateTimeStyle Int
    ) {
        val jdkFormatter = DateFormat.getDateTimeInstance(javaDateStyle, javaTimeStyle, locale)
        val options =
            DateTimeFormatterJdkStyleOptions.createDateTimeInstance(javaDateStyle, javaTimeStyle)
        val compatFormatter = DateTimeFormatter(options, locale)
        assertEquals(
            Helper.normalizeNnbsp(jdkFormatter.format(testCalendar.time)),
            Helper.normalizeNnbsp(compatFormatter.format(testCalendar)))
    }

    @Test @SmallTest
    fun testPredefinedStyles() {
        val options = DateTimeFormatterCommonOptions.ABBR_MONTH_WEEKDAY_DAY
        val formatter = DateTimeFormatter(appContext, options, Locale.US)
        assertEquals("Sun, Sep 19", formatter.format(testCalendar))
    }
}