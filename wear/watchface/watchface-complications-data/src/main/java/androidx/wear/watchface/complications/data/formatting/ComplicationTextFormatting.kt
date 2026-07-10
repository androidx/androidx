/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.watchface.complications.data.formatting

import android.icu.text.SimpleDateFormat
import android.icu.util.GregorianCalendar
import android.icu.util.TimeZone
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * A utility for finding optimal, locale-aware date and time format patterns that are guaranteed to
 * fit within the strict character limits of a `ComplicationType.SHORT_TEXT` field.
 *
 * The primary challenge in formatting text for complications is that the length of formatted
 * date/time strings can vary (e.g., "May" vs. "September"). This class solves this by
 * systematically testing a set of preferred format skeletons against a range of date and time
 * values. It determines the "worst-case" (longest) output for a given pattern and locale, ensuring
 * the chosen pattern is always safe to use.
 *
 * It also includes hardcoded overrides for specific locales where standard patterns are known to be
 * too long.
 */
public class ComplicationTextFormatting
internal constructor(private val mLocale: Locale, private val mDateFormat: WrappedDateFormat) {
    /**
     * Creates a new instance of [ComplicationTextFormatting] for the given locale.
     *
     * @param locale The [Locale] that defines the formatting conventions.
     */
    public constructor(locale: Locale) : this(locale, WrappedDateFormat.DEFAULT)

    /**
     * Returns a 12-hour time format pattern suitable for a short text field (e.g., "1:37"). This
     * version may shorten the AM/PM marker (e.g., from "a.m." to "am") to meet length constraints.
     */
    public val shortTextTimeFormat12Hour: String?
        get() = getShortTextTimeFormat(use24Hour = false, allowAmPmShortening = true)

    /** Returns a 24-hour time format pattern suitable for a short text field (e.g., "13:37"). */
    public val shortTextTimeFormat24Hour: String?
        get() = getShortTextTimeFormat(use24Hour = true, allowAmPmShortening = true)

    /**
     * Returns a 12-hour time format pattern suitable for a short text field (e.g., "1:37 a.m.").
     *
     * This version preserves the full, locale-specific AM/PM marker (e.g., "a.m." with periods) and
     * does not attempt to shorten it to save space.
     */
    public val shortTextTimeFormat12HourWithoutAmPmShortening: String?
        get() = getShortTextTimeFormat(use24Hour = false, allowAmPmShortening = false)

    /**
     * Returns a 24-hour time format pattern suitable for a short text field (e.g., "13:37").
     *
     * Note: This is functionally identical to [shortTextTimeFormat24Hour], as AM/PM shortening is
     * not applicable to 24-hour formats.
     */
    public val shortTextTimeFormat24HourWithoutAmPmShortening: String?
        get() = getShortTextTimeFormat(use24Hour = true, allowAmPmShortening = false)

    /**
     * Finds the best date format pattern from a prioritized list of skeletons that consistently
     * produces strings short enough for `SHORT_TEXT` fields.
     *
     * It iterates through the provided `skeletons` in order. The first skeleton that passes the
     * length-check across multiple date/time samples is selected. If no skeletons in the list
     * produce a sufficiently short result, the `fallback` pattern is returned.
     *
     * @param skeletons An array of date format skeletons to test, in order of preference. For
     *   example, `["MMMd", "MMd"]` would try a three-letter month first before a two-digit month.
     * @param fallback The non-validated pattern to return if no skeleton in `skeletons` is
     *   suitable.
     * @return The best-fitting date pattern string, or the [fallback].
     */
    public fun getBestShortTextDateFormat(skeletons: Array<String>, fallback: String?): String? {
        for (skeleton in skeletons) {
            var pattern: String? = null
            // Check for hardcoded overrides for known problematic locales.
            for (mapping in FORMAT_MAPPINGS) {
                if (mLocale.language == mapping.mLanguage && skeleton == mapping.mSkeleton) {
                    pattern = mapping.mPattern
                    break
                }
            }
            // If no override exists, get the best system-provided pattern for the skeleton.
            if (pattern == null) {
                pattern = mDateFormat.getBestDateTimePattern(mLocale, skeleton)
            }
            // Validate that the chosen pattern is short enough across various time samples.
            if (
                isShortEnough(mLocale, pattern, timeStepForSkeleton(skeleton), stripChars = false)
            ) {
                return pattern
            }
        }
        return fallback
    }

    /**
     * The internal implementation for finding a short time format.
     *
     * For a 12-hour clock, it attempts multiple strategies:
     * 1. Use the standard `hmm` skeleton.
     * 2. If that's too long, try again but without the AM/PM marker.
     * 3. If all else fails, use a simple fallback of `h:mm`.
     *
     * @param use24Hour `true` to request a 24-hour format, `false` for 12-hour.
     * @param allowAmPmShortening `true` to permit stripping characters (e.g., periods) from the
     *   AM/PM marker to save space.
     * @return The best pattern string that fits within the short text limit.
     */
    private fun getShortTextTimeFormat(use24Hour: Boolean, allowAmPmShortening: Boolean): String? {
        if (use24Hour) {
            return getBestShortTextDateFormat(arrayOf("HHmm"), "HH:mm")
        }

        val timeStep = TimeUnit.MINUTES.toMillis(97) // A prime step to test various times.
        val pattern: String =
            mDateFormat
                .getBestDateTimePattern(mLocale, "hmm")
                .replace("\\s".toRegex(), "")
                .replace(NARROW_NO_BREAK_SPACE, "")
        if (isShortEnough(mLocale, pattern, timeStep, allowAmPmShortening)) {
            return pattern
        }

        // Second attempt: remove the AM/PM marker entirely if it makes the string fit.
        val patternWithoutAmPm = pattern.replace("a", "").trim { it <= ' ' }
        if (
            pattern != patternWithoutAmPm &&
                isShortEnough(mLocale, patternWithoutAmPm, timeStep, allowAmPmShortening)
        ) {
            return patternWithoutAmPm
        }

        // Last resort fallback.
        return "h:mm"
    }

    /**
     * Returns a date format pattern for a short day-and-month representation (e.g., "Dec 25").
     * Tries skeletons in the order of `MMMd`, `MMd`, and `Md`. Falls back to `d/MM`.
     */
    public val shortTextDayMonthFormat: String?
        get() = getBestShortTextDateFormat(arrayOf("MMMd", "MMd", "Md"), "d/MM")

    /**
     * Returns a date format pattern for a short month representation (e.g., "Dec"). Tries skeletons
     * in the order of `MMM`, `MM`, and `M`. Falls back to `MM`.
     */
    public val shortTextMonthFormat: String?
        get() = getBestShortTextDateFormat(arrayOf("MMM", "MM", "M"), "MM")

    /**
     * Returns a date format pattern for the day of the month (e.g., "25"). Tries skeletons `dd` and
     * `d`. Falls back to `dd`.
     */
    public val shortTextDayOfMonthFormat: String?
        get() = getBestShortTextDateFormat(arrayOf("dd", "d"), "dd")

    /**
     * Returns a date format pattern for a short day-of-week representation (e.g., "Tue"). Tries
     * skeletons `EEE` (abbreviation), `EEEEEE` (2-letter), and `EEEEE` (1-letter). Falls back to
     * `EEEEE`.
     */
    public val shortTextDayOfWeekFormat: String?
        get() = getBestShortTextDateFormat(arrayOf("EEE", "EEEEEE", "EEEEE"), "EEEEE")

    /**
     * Returns a date format pattern for the full day-of-week name (e.g., "Tuesday"). This is
     * intended for `LONG_TEXT` fields and is not length-checked.
     */
    public val fullTextDayOfWeekFormat: String
        get() = mDateFormat.getBestDateTimePattern(mLocale, FULL_DAY_OF_WEEK_TEXT_SKELETON)

    /**
     * Applies the short text time formatting logic to a specific time.
     *
     * @param timeInMillis The UTC time to format, in milliseconds since the epoch.
     * @param timeZone The target [TimeZone] for the output string. `null` uses the system default.
     * @param use24Hour If `true`, formats in 24-hour time; otherwise, 12-hour.
     * @return The final, formatted time string, ready for display.
     */
    public fun getFormattedTimeForShortText(
        timeInMillis: Long,
        timeZone: TimeZone?,
        use24Hour: Boolean,
    ): String? {
        val pattern = getShortTextTimeFormat(use24Hour)
        val format = SimpleDateFormat(pattern, mLocale)
        format.timeZone = timeZone
        return formatTime(format, timeInMillis, stripChars = true)
    }

    /**
     * Applies the short day-of-week formatting logic to a specific time.
     *
     * @param timeInMillis The UTC time to format, in milliseconds since the epoch.
     * @param timeZone The target [TimeZone] for the output string. `null` uses the system default.
     * @return The final, formatted day-of-week string, ready for display.
     */
    public fun getFormattedDayOfWeekForShortText(timeInMillis: Long, timeZone: TimeZone?): String? {
        val pattern = shortTextDayOfWeekFormat
        val format = SimpleDateFormat(pattern, mLocale)
        format.timeZone = timeZone
        return formatTime(format, timeInMillis, stripChars = false)
    }

    /**
     * Returns a time format pattern suitable for a short text field. This version may shorten the
     * AM/PM marker (e.g., from "a.m." to "am") if necessary to meet length constraints.
     *
     * @param use24Hour If `true`, returns a 24-hour format (e.g., "13:37"). If `false`, returns a
     *   12-hour format (e.g., "1:37").
     * @return A time format pattern string guaranteed to produce short output.
     */
    private fun getShortTextTimeFormat(use24Hour: Boolean) =
        getShortTextTimeFormat(use24Hour, allowAmPmShortening = true)

    /** A data class for hardcoded skeleton-to-pattern mappings for specific languages. */
    private class FormatMapping(
        val mLanguage: String?,
        val mSkeleton: String?,
        val mPattern: String?,
    )

    /** Maps date/time symbols to a time duration for intelligent test-step selection. */
    private class TimeUnitMapping(val mTimeUnit: Long, vararg symbols: String) {
        val mStrings: Array<out String> = symbols
    }

    internal companion object {
        /**
         * A regex to remove spaces or periods adjacent to non-digit characters. This is primarily
         * used to shorten AM/PM markers, for example, converting "a.m." to "am".
         */
        private val REGEX_STRIP_CHARS: Pattern =
            Pattern.compile("(([^\\d.])([ \\u00A0.]+))|(([ \\u00A0.]+)([^\\d.]))")
        private const val REGEX_REPLACEMENT = "$2$6"
        private const val NARROW_NO_BREAK_SPACE = "\u202f"

        /** The number of samples to check across a time range to find the longest output. */
        private const val TEST_STEPS = 13

        /** The strict maximum character count for a `ComplicationType.SHORT_TEXT` field. */
        private const val SHORT_TEXT_MAX_LENGTH = 7

        /** The standard skeleton for a full, standalone day of the week name. */
        private const val FULL_DAY_OF_WEEK_TEXT_SKELETON = "EEEE"

        /** Maps date symbols to time units for selecting appropriate test time steps. */
        private val TIME_UNIT_MAPPINGS =
            arrayOf<TimeUnitMapping?>(
                TimeUnitMapping(TimeUnit.SECONDS.toMillis(47), "S", "s"),
                TimeUnitMapping(TimeUnit.MINUTES.toMillis(47), "m"),
                TimeUnitMapping(TimeUnit.HOURS.toMillis(5), "H", "K", "h", "k", "j", "J"),
                TimeUnitMapping(TimeUnit.DAYS.toMillis(1), "D", "E", "F", "c", "d", "g"),
                TimeUnitMapping(TimeUnit.DAYS.toMillis(27), "M", "L"),
            )

        /**
         * Empirically determined overrides where standard ICU patterns are too long. For example,
         * in Finnish ("fi"), day numbers require a trailing period.
         */
        private val FORMAT_MAPPINGS =
            arrayOf<FormatMapping>(
                FormatMapping("fi", "d", "d."),
                FormatMapping("fi", "dd", "dd."),
                FormatMapping("de", "d", "d."),
                FormatMapping("de", "dd", "dd."),
                FormatMapping("de", "MMM", "MMM"),
                FormatMapping("no", "MMM", "MMM"),
                FormatMapping("nb", "MMM", "MMM"),
                FormatMapping("no", "HHmm", "HH.mm"),
                FormatMapping("no", "hmm", "h.mma"),
                FormatMapping("nb", "HHmm", "HH.mm"),
                FormatMapping("nb", "hmm", "h.mma"),
                FormatMapping("ja", "EEE", "EEEE"),
                FormatMapping("de", "MMd", "dd.MM."),
            )

        /**
         * Verifies that a pattern consistently produces short strings.
         *
         * It iterates [TEST_STEPS] times, advancing a sample timestamp by `timeStep` on each
         * iteration. It formats the timestamp and returns `false` immediately if any result exceeds
         * [SHORT_TEXT_MAX_LENGTH].
         *
         * @param locale The [Locale] for formatting.
         * @param pattern The date/time pattern to validate.
         * @param timeStep The time increment between samples, chosen to be relevant to the pattern.
         * @param stripChars If `true`, shortens the result before length checking.
         * @return `true` if all generated samples are within the length limit.
         */
        private fun isShortEnough(
            locale: Locale?,
            pattern: String?,
            timeStep: Long,
            stripChars: Boolean,
        ): Boolean {
            val format = SimpleDateFormat(pattern, locale)
            var testTime = GregorianCalendar(2021, 1, 1, 1, 1, 1).getTimeInMillis()

            for (i in 0..<TEST_STEPS) {
                if (formatTime(format, testTime, stripChars)!!.length > SHORT_TEXT_MAX_LENGTH) {
                    return false
                }
                testTime += timeStep
            }
            return true
        }

        /**
         * Selects a representative time step duration based on the smallest time unit found in a
         * skeleton. For example, if a skeleton contains 'm' (minutes) but not 's' (seconds), the
         * step will be in minutes. This makes the testing in `isShortEnough` efficient and
         * relevant.
         *
         * @param skeleton The date format skeleton to analyze.
         * @return A relevant time step in milliseconds for testing the skeleton.
         */
        private fun timeStepForSkeleton(skeleton: String): Long {
            var timeStep: Long = 0
            for (timeMapping in TIME_UNIT_MAPPINGS) {
                for (symbol in timeMapping!!.mStrings) {
                    if (skeleton.contains(symbol)) {
                        timeStep += timeMapping.mTimeUnit
                        break
                    }
                }
            }
            return timeStep
        }

        /**
         * A helper to format a time value and optionally apply the character-stripping regex.
         *
         * @param format The [SimpleDateFormat] instance to use for formatting.
         * @param time The time value in milliseconds since the epoch.
         * @param stripChars If `true`, applies [REGEX_STRIP_CHARS] to the result.
         * @return The final formatted string.
         */
        private fun formatTime(format: SimpleDateFormat, time: Long, stripChars: Boolean): String? {
            val timeString = format.format(Date(time))
            return if (stripChars) {
                REGEX_STRIP_CHARS.matcher(timeString).replaceAll(REGEX_REPLACEMENT)
            } else {
                timeString
            }
        }
    }
}
