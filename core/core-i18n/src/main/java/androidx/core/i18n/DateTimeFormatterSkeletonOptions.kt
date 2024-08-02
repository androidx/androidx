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

import java.util.regex.Pattern

/**
 * This class helps one create skeleton options for [DateTimeFormatter] in a safer and more
 * discoverable manner than using raw strings.
 *
 * Skeletons are a flexible way to specify (in a locale independent manner) how to format of a date
 * / time.
 *
 * It can be used for example to specify that a formatted date should contain a day-of-month, an
 * abbreviated month name, and a year.
 *
 * It does not specify the order of the fields, or the separators, those will depend on the locale.
 *
 * The result will be locale dependent: "Aug 17, 2022" for English U.S., "17 Aug 2022" for English -
 * Great Britain, "2022年8月17日" for Japanese.
 *
 * Skeletons are based on the
 * [Unicode Technical Standard #35](https://unicode.org/reports/tr35/tr35-dates.html#Date_Field_Symbol_Table),
 * but uses a builder to make things safer an more discoverable.
 *
 * You can still build these options from a string by using the
 * [DateTimeFormatterSkeletonOptions.fromString] method.
 */
public class DateTimeFormatterSkeletonOptions
internal constructor(
    private val era: Era,
    private val year: Year,
    private val month: Month,
    private val day: Day,
    private val weekDay: WeekDay,
    private val period: Period,
    private val hour: Hour,
    private val minute: Minute,
    private val second: Second,
    private val fractionalSecond: FractionalSecond,
    private val timezone: Timezone
) {

    // Date fields

    /** Era name. Era string for the date. */
    public class Era private constructor(public val value: String) {
        public companion object {
            /** E.g. "Anno Domini". */
            @JvmField public val WIDE: Era = Era("GGGG")

            /** E.g. "AD". */
            @JvmField public val ABBREVIATED: Era = Era("G")

            /** E.g. "A". */
            @JvmField public val NARROW: Era = Era("GGGGG")

            /** Produces no output. */
            @JvmField public val NONE: Era = Era("")

            @JvmStatic
            public fun fromString(value: String): Era {
                return when (value) {
                    "G",
                    "GG",
                    "GGG" -> ABBREVIATED
                    "GGGG" -> WIDE
                    "GGGGG" -> NARROW
                    else -> NONE
                }
            }
        }
    }

    /** Calendar year (numeric). */
    public class Year private constructor(public val value: String) {
        public companion object {
            /** As many digits as needed to show the full value. E.g. "2021" or "2009". */
            @JvmField public val NUMERIC: Year = Year("y")

            /**
             * The two low-order digits of the year, zero-padded as necessary. E.g. "21" or "09".
             */
            @JvmField public val TWO_DIGITS: Year = Year("yy")

            /** Produces no output. */
            @JvmField public val NONE: Year = Year("")

            @JvmStatic
            public fun fromString(value: String): Year {
                return when (value) {
                    "y" -> NUMERIC
                    "yy" -> TWO_DIGITS
                    else -> NONE
                }
            }
        }
    }

    /** Month number/name. */
    public class Month private constructor(public val value: String) {
        public companion object {
            /** e.g. "September". */
            @JvmField public val WIDE: Month = Month("MMMM")

            /** e.g. "Sep". */
            @JvmField public val ABBREVIATED: Month = Month("MMM")

            /** Might be soo short that it is confusing. E.g. "S". */
            @JvmField public val NARROW: Month = Month("MMMMM")

            /** E.g. "9". */
            @JvmField public val NUMERIC: Month = Month("M")

            /** Numeric: 2 digits, zero pad if needed. May not be good i18n. E.g. "09". */
            @JvmField public val TWO_DIGITS: Month = Month("MM")

            /** Produces no output. */
            @JvmField public val NONE: Month = Month("")

            @JvmStatic
            public fun fromString(value: String): Month {
                return when (value) {
                    "M" -> NUMERIC
                    "MM" -> TWO_DIGITS
                    "MMM" -> ABBREVIATED
                    "MMMM" -> WIDE
                    "MMMMM" -> NARROW
                    else -> NONE
                }
            }
        }
    }

    /** Day of month (numeric). */
    public class Day private constructor(public val value: String) {
        public companion object {
            /** As many digits as needed to show the full value. E.g. "1" or "17". */
            @JvmField public val NUMERIC: Day = Day("d")

            /** Two digits, zero pad if needed. E.g. "01" or "17". */
            @JvmField public val TWO_DIGITS: Day = Day("dd")

            /** Produces no output. */
            @JvmField public val NONE: Day = Day("")

            @JvmStatic
            public fun fromString(value: String): Day {
                return when (value) {
                    "d" -> NUMERIC
                    "dd" -> TWO_DIGITS
                    else -> NONE
                }
            }
        }
    }

    /** Day of week name. */
    public class WeekDay private constructor(public val value: String) {
        public companion object {
            /** E.g. "Tuesday". */
            @JvmField public val WIDE: WeekDay = WeekDay("EEEE")

            /** E.g. "Tue". */
            @JvmField public val ABBREVIATED: WeekDay = WeekDay("E")

            /** E.g. "Tu". */
            @JvmField public val SHORT: WeekDay = WeekDay("EEEEEE")

            /**
             * E.g. "T". Two weekdays may have the same narrow style for some locales. E.g. the
             * narrow style for both "Tuesday" and "Thursday" is "T".
             */
            @JvmField public val NARROW: WeekDay = WeekDay("EEEEE")

            /** Produces no output. */
            @JvmField public val NONE: WeekDay = WeekDay("")

            @JvmStatic
            public fun fromString(value: String): WeekDay {
                return when (value) {
                    "E",
                    "EE",
                    "EEE" -> ABBREVIATED
                    "EEEE" -> WIDE
                    "EEEEE" -> NARROW
                    "EEEEEE" -> SHORT
                    else -> NONE
                }
            }
        }
    }

    // Time fields

    /** The period of the day, if the hour is not 23h or 24h style. */
    public class Period private constructor(public val value: String) {
        public companion object {
            /** E.g. "12 a.m.". */
            @JvmField public val WIDE: Period = Period("aaaa")

            /** E.g. "12 a.m.". */
            @JvmField public val ABBREVIATED: Period = Period("a")

            /** E.g. "12 a". */
            @JvmField public val NARROW: Period = Period("aaaaa")

            /**
             * Flexible day periods. May be upper or lowercase depending on the locale and other
             * options. Often there is only one width that is customarily used. E.g. "3:00 at night"
             */
            @JvmField public val FLEXIBLE: Period = Period("B")

            /** Produces no output. */
            @JvmField public val NONE: Period = Period("")

            @JvmStatic
            public fun fromString(value: String): Period {
                return when (value) {
                    "a",
                    "aa",
                    "aaa" -> ABBREVIATED
                    "aaaa" -> WIDE
                    "aaaaa" -> NARROW
                    "B" -> FLEXIBLE
                    else -> NONE
                }
            }
        }
    }

    /** Hour (numeric). */
    public class Hour private constructor(public val value: String) {
        public companion object {
            /**
             * As many digits as needed to show the full value. Day period if used. E.g. "8", "8
             * AM", "13", "1 PM".
             */
            @JvmField public val NUMERIC: Hour = Hour("j")

            /**
             * Two digits, zero pad if needed. DayPeriod if used. Might be bad i18n. E.g. "08", "08
             * AM", "13", "01 PM".
             */
            @JvmField public val TWO_DIGITS: Hour = Hour("jj")

            /**
             * Bad i18n. As many digits as needed to show the full value. Day period added
             * automatically. E.g. "8 AM", "1 PM".
             */
            @JvmField public val FORCE_12H_NUMERIC: Hour = Hour("h")

            /**
             * Bad i18n. Two digits, zero pad if needed. Day period added automatically. E.g. "08
             * AM", "01 PM".
             */
            @JvmField public val FORCE_12H_TWO_DIGITS: Hour = Hour("hh")

            /**
             * Bad i18n. As many digits as needed to show the full value. No day period. E.g. "8",
             * "13".
             */
            @JvmField public val FORCE_24H_NUMERIC: Hour = Hour("H")

            /** Bad i18n. Two digits, zero pad if needed. No day period. E.g. "08", "13". */
            @JvmField public val FORCE_24H_TWO_DIGITS: Hour = Hour("HH")

            /** Produces no output. */
            @JvmField public val NONE: Hour = Hour("")

            @JvmStatic
            public fun fromString(value: String): Hour {
                return when (value) {
                    "j" -> NUMERIC
                    "jj" -> TWO_DIGITS
                    "h" -> FORCE_12H_NUMERIC
                    "hh" -> FORCE_12H_TWO_DIGITS
                    "H" -> FORCE_24H_NUMERIC
                    "HH" -> FORCE_24H_TWO_DIGITS
                    else -> NONE
                }
            }
        }
    }

    /** Minute (numeric). Truncated, not rounded. */
    public class Minute private constructor(public val value: String) {
        public companion object {
            /** As many digits as needed to show the full value. E.g. "8", "59" */
            @JvmField public val NUMERIC: Minute = Minute("m")

            /** Two digits, zero pad if needed. E.g. "08", "59" */
            @JvmField public val TWO_DIGITS: Minute = Minute("mm")

            /** Produces no output. */
            @JvmField public val NONE: Minute = Minute("")

            @JvmStatic
            public fun fromString(value: String): Minute {
                return when (value) {
                    "m" -> NUMERIC
                    "mm" -> TWO_DIGITS
                    else -> NONE
                }
            }
        }
    }

    /** Second (numeric). Truncated, not rounded. */
    public class Second private constructor(public val value: String) {
        public companion object {
            /** As many digits as needed to show the full value. E.g. "8", "59". */
            @JvmField public val NUMERIC: Second = Second("s")

            /** Two digits, zero pad if needed. E.g. "08", "59". */
            @JvmField public val TWO_DIGITS: Second = Second("ss")

            /** Produces no output. */
            @JvmField public val NONE: Second = Second("")

            @JvmStatic
            public fun fromString(value: String): Second {
                return when (value) {
                    "s" -> NUMERIC
                    "ss" -> TWO_DIGITS
                    else -> NONE
                }
            }
        }
    }

    /**
     * Fractional Second (numeric). Truncates, like other numeric time fields, but in this case to
     * the number of digits specified by the field length.
     */
    public class FractionalSecond private constructor(public val value: String) {
        public companion object {
            /** Fractional part represented as 3 digits. E.g. "12.345". */
            @JvmField public val NUMERIC_3_DIGITS: FractionalSecond = FractionalSecond("SSS")

            /** Fractional part represented as 2 digits. E.g. "12.34". */
            @JvmField public val NUMERIC_2_DIGITS: FractionalSecond = FractionalSecond("SS")

            /** Fractional part represented as 1 digit. E.g. "12.3". */
            @JvmField public val NUMERIC_1_DIGIT: FractionalSecond = FractionalSecond("S")

            /**
             * Fractional part dropped. Produces no output. E.g. "12" (seconds, without fractions).
             */
            @JvmField public val NONE: FractionalSecond = FractionalSecond("")

            @JvmStatic
            public fun fromString(value: String): FractionalSecond {

                return when (value) {
                    "S" -> NUMERIC_1_DIGIT
                    "SS" -> NUMERIC_2_DIGITS
                    "SSS" -> NUMERIC_3_DIGITS
                    else -> NONE
                }
            }
        }
    }

    /** The localized representation of the time zone name. */
    public class Timezone private constructor(public val value: String) {
        public companion object {
            /** Short localized form. E.g. "PST", "GMT-8". */
            @JvmField public val SHORT: Timezone = Timezone("z")

            /**
             * Long localized form. E.g. "Pacific Standard Time", "Nordamerikanische
             * Westküsten-Normalzeit".
             */
            @JvmField public val LONG: Timezone = Timezone("zzzz")

            /** Short localized GMT format. E.g. "GMT-8". */
            @JvmField public val SHORT_OFFSET: Timezone = Timezone("O")

            /** Long localized GMT format. E.g. "GMT-0800". */
            @JvmField public val LONG_OFFSET: Timezone = Timezone("OOOO")

            /** Short generic non-location format. E.g. "PT", "Los Angeles Zeit". */
            @JvmField public val SHORT_GENERIC: Timezone = Timezone("v")

            /**
             * Long generic non-location format. E.g. "Pacific Time", "Nordamerikanische
             * Westküstenzeit".
             */
            @JvmField public val LONG_GENERIC: Timezone = Timezone("vvvv")

            /** Produces no output. */
            @JvmField public val NONE: Timezone = Timezone("")

            @JvmStatic
            public fun fromString(value: String): Timezone {
                return when (value) {
                    "z",
                    "zz",
                    "zzz" -> SHORT
                    "zzzz" -> LONG
                    "O" -> SHORT_OFFSET
                    "OOOO" -> LONG_OFFSET
                    "v" -> SHORT_GENERIC
                    "vvvv" -> LONG_GENERIC
                    else -> NONE
                }
            }
        }
    }

    /** Returns the era option. */
    public fun getEra(): Era = era

    /** Returns the year option. */
    public fun getYear(): Year = year

    /** Returns the month option. */
    public fun getMonth(): Month = month

    /** Returns the day option. */
    public fun getDay(): Day = day

    /** Returns the day of week option. */
    public fun getWeekDay(): WeekDay = weekDay

    /** Returns the day period option. */
    public fun getPeriod(): Period = period

    /** Returns the hour option. */
    public fun getHour(): Hour = hour

    /** Returns the minutes option. */
    public fun getMinute(): Minute = minute

    /** Returns the seconds option. */
    public fun getSecond(): Second = second

    /** Returns the fractional second option. */
    public fun getFractionalSecond(): FractionalSecond = fractionalSecond

    /** Returns the timezone option. */
    public fun getTimezone(): Timezone = timezone

    override fun toString(): String {
        return era.value +
            year.value +
            month.value +
            weekDay.value +
            day.value +
            period.value +
            hour.value +
            minute.value +
            second.value +
            fractionalSecond.value +
            timezone.value
    }

    public companion object {
        // WARNING: if you change this regexp also update the switch in [fromString]
        private val pattern =
            Pattern.compile(
                "(G+)|(y+)|(M+)|(d+)|(E+)|" +
                    "(a+)|(B+)|(j+)|(h+)|(H+)|(m+)|(s+)|(S+)|(z+)|(O+)|(v+)"
            )
        private val TAG = this::class.qualifiedName

        /**
         * Creates the a [DateTimeFormatterSkeletonOptions] object from a string.
         *
         * Although less discoverable than using the `Builder`, it is useful for serialization, and
         * to implement the MessageFormat functionality.
         *
         * @param value the skeleton that specifies the fields to be formatted and their length.
         * @return the formatting options to use with [androidx.core.i18n.DateTimeFormatter].
         * @throws IllegalArgumentException if the [value] contains an unknown skeleton field.
         * @throws RuntimeException library error (unknown skeleton field encountered).
         */
        @JvmStatic
        public fun fromString(value: String): DateTimeFormatterSkeletonOptions {
            val result = Builder()
            if (value.isEmpty()) {
                return result.build()
            }

            var validFields = false
            val matcher = pattern.matcher(value)
            while (matcher.find()) {
                validFields = true
                val skeletonField = matcher.group()
                when (skeletonField.firstOrNull()) {
                    'G' -> result.setEra(Era.fromString(skeletonField))
                    'y' -> result.setYear(Year.fromString(skeletonField))
                    'M' -> result.setMonth(Month.fromString(skeletonField))
                    'd' -> result.setDay(Day.fromString(skeletonField))
                    'E' -> result.setWeekDay(WeekDay.fromString(skeletonField))
                    'a',
                    'B' -> result.setPeriod(Period.fromString(skeletonField))
                    'j',
                    'h',
                    'H' -> result.setHour(Hour.fromString(skeletonField))
                    'm' -> result.setMinute(Minute.fromString(skeletonField))
                    's' -> result.setSecond(Second.fromString(skeletonField))
                    'S' -> result.setFractionalSecond(FractionalSecond.fromString(skeletonField))
                    'z',
                    'O',
                    'v' -> result.setTimezone(Timezone.fromString(skeletonField))
                    else ->
                        // This should not happen, the regexp should protect us.
                        throw RuntimeException(
                            "Unrecognized skeleton field '$skeletonField' in \"${value}\"."
                        )
                }
            }
            if (!validFields) {
                throw IllegalArgumentException("Unrecognized skeleton field found in \"${value}\".")
            }
            return result.build()
        }
    }

    /**
     * The `Builder` class used to construct a [DateTimeFormatterSkeletonOptions] in a way that is
     * safe and discoverable.
     */
    public class Builder(
        private var era: Era = Era.NONE,
        private var year: Year = Year.NONE,
        private var month: Month = Month.NONE,
        private var day: Day = Day.NONE,
        private var weekDay: WeekDay = WeekDay.NONE,
        private var period: Period = Period.NONE,
        private var hour: Hour = Hour.NONE,
        private var minute: Minute = Minute.NONE,
        private var second: Second = Second.NONE,
        private var fractionalSecond: FractionalSecond = FractionalSecond.NONE,
        private var timezone: Timezone = Timezone.NONE
    ) {

        /**
         * Set the era presence and length to use for formatting.
         *
         * @param era the era style to use.
         */
        public fun setEra(era: Era): Builder {
            this.era = era
            return this
        }

        /**
         * Set the year presence and length to use for formatting.
         *
         * @param year the era style to use.
         */
        public fun setYear(year: Year): Builder {
            this.year = year
            return this
        }

        /**
         * Set the month presence and length to use for formatting.
         *
         * @param month the era style to use.
         */
        public fun setMonth(month: Month): Builder {
            this.month = month
            return this
        }

        /**
         * Set the day presence and length to use for formatting.
         *
         * @param day the era style to use.
         */
        public fun setDay(day: Day): Builder {
            this.day = day
            return this
        }

        /**
         * Set the day of week presence and length to use for formatting.
         *
         * @param weekDay the era style to use.
         */
        public fun setWeekDay(weekDay: WeekDay): Builder {
            this.weekDay = weekDay
            return this
        }

        /**
         * Set the day period presence and length to use for formatting.
         *
         * @param period the era style to use.
         */
        public fun setPeriod(period: Period): Builder {
            this.period = period
            return this
        }

        /**
         * Set the hour presence and length to use for formatting.
         *
         * @param hour the era style to use.
         */
        public fun setHour(hour: Hour): Builder {
            this.hour = hour
            return this
        }

        /**
         * Set the minute presence and length to use for formatting.
         *
         * @param minute the era style to use.
         */
        public fun setMinute(minute: Minute): Builder {
            this.minute = minute
            return this
        }

        /**
         * Set the second presence and length to use for formatting.
         *
         * @param second the era style to use.
         */
        public fun setSecond(second: Second): Builder {
            this.second = second
            return this
        }

        /**
         * Set the fractional second presence and length to use for formatting.
         *
         * @param fractionalSecond the era style to use.
         */
        public fun setFractionalSecond(fractionalSecond: FractionalSecond): Builder {
            this.fractionalSecond = fractionalSecond
            return this
        }

        /**
         * Set the timezone presence and length to use for formatting.
         *
         * @param timezone the era style to use.
         */
        public fun setTimezone(timezone: Timezone): Builder {
            this.timezone = timezone
            return this
        }

        /**
         * Builds the immutable [DateTimeFormatterSkeletonOptions] to use with [DateTimeFormatter].
         *
         * return the [DateTimeFormatterSkeletonOptions] options.
         */
        public fun build(): DateTimeFormatterSkeletonOptions {
            return DateTimeFormatterSkeletonOptions(
                era,
                year,
                month,
                day,
                weekDay,
                period,
                hour,
                minute,
                second,
                fractionalSecond,
                timezone
            )
        }
    } // end of Builder class
}
