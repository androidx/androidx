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

import androidx.core.i18n.DateTimeFormatterSkeletonOptions.Day
import androidx.core.i18n.DateTimeFormatterSkeletonOptions.Hour
import androidx.core.i18n.DateTimeFormatterSkeletonOptions.Minute
import androidx.core.i18n.DateTimeFormatterSkeletonOptions.Month
import androidx.core.i18n.DateTimeFormatterSkeletonOptions.Second
import androidx.core.i18n.DateTimeFormatterSkeletonOptions.WeekDay
import androidx.core.i18n.DateTimeFormatterSkeletonOptions.Year

/**
 * Used to support common styles, similar to the ones defined in Closure / Material.
 *
 * The names here are the same as the ones in ICU4J, as Closure / Material have three different
 * naming conventions, sometimes even internally inconsistent. Will add mapping to the Material
 * Design recommended date / time formats.
 *
 * @see android.icu.text.DateFormat
 */
public class DateTimeFormatterCommonOptions {
    public companion object {
        // TODO: Add a way to combine a predefined data + a predefined time,
        // for example `ABBR_MONTH_DAY` + `HOUR_MINUTE`

        /**
         * Constant for date skeleton with abbreviated month and day. E.g. "Mar 27".
         *
         * @see android.icu.text.DateFormat.ABBR_MONTH_DAY
         */
        @JvmField
        public val ABBR_MONTH_DAY: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setMonth(Month.ABBREVIATED)
                .setDay(Day.NUMERIC)
                .build()

        /**
         * Constant for date skeleton with abbreviated month, weekday, and day. E.g. "Sat, Mar 27".
         *
         * @see android.icu.text.DateFormat.ABBR_MONTH_WEEKDAY_DAY
         */
        @JvmField
        public val ABBR_MONTH_WEEKDAY_DAY: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setMonth(Month.ABBREVIATED)
                .setDay(Day.NUMERIC)
                .setWeekDay(WeekDay.ABBREVIATED)
                .build()

        /**
         * Constant for date skeleton with hour and minute in 24-hour presentation. E.g. "19:42".
         * WARNING: Not user friendly (ignores user preferences) and not i18n friendly (ignoring
         * locale customs)
         *
         * @see android.icu.text.DateFormat.HOUR24_MINUTE
         */
        @JvmField
        public val HOUR24_MINUTE: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setHour(Hour.FORCE_24H_NUMERIC)
                .setMinute(Minute.NUMERIC)
                .build()

        /**
         * Constant for date skeleton with hour, minute, and second in 24-hour presentation. E.g.
         * "19:42:57". WARNING: Not user friendly (ignores user preferences) and not i18n friendly
         * (ignoring locale customs)
         *
         * @see android.icu.text.DateFormat.HOUR24_MINUTE_SECOND
         */
        @JvmField
        public val HOUR24_MINUTE_SECOND: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setHour(Hour.FORCE_24H_NUMERIC)
                .setMinute(Minute.NUMERIC)
                .setSecond(Second.NUMERIC)
                .build()

        /**
         * Constant for date skeleton with hour and minute, with the locale's preferred hour format
         * (12 or 24). E.g. "7:42 PM".
         *
         * @see android.icu.text.DateFormat.HOUR_MINUTE
         */
        @JvmField
        public val HOUR_MINUTE: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setHour(Hour.NUMERIC)
                .setMinute(Minute.NUMERIC)
                .build()

        /**
         * Constant for date skeleton with hour, minute, and second, with the locale's preferred
         * hour format (12 or 24). E.g. "7:42:57 PM".
         *
         * @see android.icu.text.DateFormat.HOUR_MINUTE_SECOND
         */
        @JvmField
        public val HOUR_MINUTE_SECOND: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setHour(Hour.NUMERIC)
                .setMinute(Minute.NUMERIC)
                .setSecond(Second.NUMERIC)
                .build()

        /**
         * Constant for date skeleton with minute and second. E.g. "42:57".
         *
         * @see android.icu.text.DateFormat.MINUTE_SECOND
         */
        @JvmField
        public val MINUTE_SECOND: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setMinute(Minute.NUMERIC)
                .setSecond(Second.NUMERIC)
                .build()

        /**
         * Constant for date skeleton with long month and day. E.g. "March 27".
         *
         * @see android.icu.text.DateFormat.MONTH_DAY
         */
        @JvmField
        public val MONTH_DAY: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setMonth(Month.WIDE)
                .setDay(Day.NUMERIC)
                .build()

        /**
         * Constant for date skeleton with month, weekday, and day. E.g. "Saturday, March 27".
         *
         * @see android.icu.text.DateFormat.MONTH_WEEKDAY_DAY
         */
        @JvmField
        public val MONTH_WEEKDAY_DAY: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setMonth(Month.WIDE)
                .setDay(Day.NUMERIC)
                .setWeekDay(WeekDay.WIDE)
                .build()

        /**
         * Constant for date skeleton with numeric month and day. E.g. "3/27". WARNING, I18N: this
         * can be confusing for an end user. It is unclear if it is month-day, day-month or even
         * month year.
         *
         * @see android.icu.text.DateFormat.NUM_MONTH_DAY
         */
        @JvmField
        public val NUM_MONTH_DAY: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setMonth(Month.NUMERIC)
                .setDay(Day.NUMERIC)
                .build()

        /**
         * Constant for date skeleton with numeric month, weekday, and day. E.g. "Sat, 3/27".
         * WARNING, I18N: this can be confusing for an end user. It is unclear if it is month-day,
         * day-month or even month year.
         *
         * @see android.icu.text.DateFormat.NUM_MONTH_WEEKDAY_DAY
         */
        @JvmField
        public val NUM_MONTH_WEEKDAY_DAY: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setMonth(Month.NUMERIC)
                .setDay(Day.NUMERIC)
                .setWeekDay(WeekDay.ABBREVIATED)
                .build()

        /**
         * Constant for date skeleton with year and abbreviated month. E.g. "Mar 2021".
         *
         * @see android.icu.text.DateFormat.YEAR_ABBR_MONTH
         */
        @JvmField
        public val YEAR_ABBR_MONTH: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setYear(Year.NUMERIC)
                .setMonth(Month.ABBREVIATED)
                .build()

        /**
         * Constant for date skeleton with year, abbreviated month, and day. E.g. "Mar 27, 2021".
         *
         * @see android.icu.text.DateFormat.YEAR_ABBR_MONTH_DAY
         */
        @JvmField
        public val YEAR_ABBR_MONTH_DAY: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setYear(Year.NUMERIC)
                .setMonth(Month.ABBREVIATED)
                .setDay(Day.NUMERIC)
                .build()

        /**
         * Constant for date skeleton with year, abbreviated month, weekday, and day. E.g. "Sat, Mar
         * 27, 2021".
         *
         * @see android.icu.text.DateFormat.YEAR_ABBR_MONTH_WEEKDAY_DAY
         */
        @JvmField
        public val YEAR_ABBR_MONTH_WEEKDAY_DAY: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setYear(Year.NUMERIC)
                .setMonth(Month.ABBREVIATED)
                .setDay(Day.NUMERIC)
                .setWeekDay(WeekDay.ABBREVIATED)
                .build()

        /**
         * Constant for date skeleton with year and month. E.g. "March 2021".
         *
         * @see android.icu.text.DateFormat.YEAR_MONTH
         */
        @JvmField
        public val YEAR_MONTH: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setYear(Year.NUMERIC)
                .setMonth(Month.WIDE)
                .build()

        /**
         * Constant for date skeleton with year, month, and day. E.g. "March 27, 2021".
         *
         * @see android.icu.text.DateFormat.YEAR_MONTH_DAY
         */
        @JvmField
        public val YEAR_MONTH_DAY: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setYear(Year.NUMERIC)
                .setMonth(Month.WIDE)
                .setDay(Day.NUMERIC)
                .build()

        /**
         * Constant for date skeleton with year, month, weekday, and day. E.g. "Saturday, March 27,
         * 2021".
         *
         * @see android.icu.text.DateFormat.YEAR_MONTH_WEEKDAY_DAY
         */
        @JvmField
        public val YEAR_MONTH_WEEKDAY_DAY: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setYear(Year.NUMERIC)
                .setMonth(Month.WIDE)
                .setDay(Day.NUMERIC)
                .setWeekDay(WeekDay.WIDE)
                .build()

        /**
         * Constant for date skeleton with year and numeric month. E.g. "3/2021".
         *
         * @see android.icu.text.DateFormat.YEAR_NUM_MONTH
         */
        @JvmField
        public val YEAR_NUM_MONTH: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setYear(Year.NUMERIC)
                .setMonth(Month.NUMERIC)
                .build()

        /**
         * Constant for date skeleton with year, numeric month, and day. E.g. "3/27/2021". WARNING,
         * I18N: this can be confusing for an end user. It is unclear if it is month-day or
         * day-month (think 3/4/2021 vs 4/3/2021.).
         *
         * @see android.icu.text.DateFormat.YEAR_NUM_MONTH_DAY
         */
        @JvmField
        public val YEAR_NUM_MONTH_DAY: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setYear(Year.NUMERIC)
                .setMonth(Month.NUMERIC)
                .setDay(Day.NUMERIC)
                .build()

        /**
         * Constant for date skeleton with year, numeric month, weekday, and day. E.g. "Sat,
         * 3/27/2021". WARNING, I18N: this can be confusing for an end user. It is unclear if it is
         * month-day or day-month (think 3/4/2021 vs 4/3/2021.).
         *
         * @see android.icu.text.DateFormat.YEAR_NUM_MONTH_WEEKDAY_DAY
         */
        @JvmField
        public val YEAR_NUM_MONTH_WEEKDAY_DAY: DateTimeFormatterSkeletonOptions =
            DateTimeFormatterSkeletonOptions.Builder()
                .setYear(Year.NUMERIC)
                .setMonth(Month.NUMERIC)
                .setDay(Day.NUMERIC)
                .setWeekDay(WeekDay.ABBREVIATED)
                .build()
    }
}
