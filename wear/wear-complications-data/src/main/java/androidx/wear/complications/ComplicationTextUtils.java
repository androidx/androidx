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

package androidx.wear.complications;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utilities to obtain the best date or time format for a given locale.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ComplicationTextUtils {

    private ComplicationTextUtils() {}

    private static final int TEST_STEPS = 13;
    private static final int SHORT_TEXT_MAX_LENGTH = 7;

    /**
     * The amount of time to advance at each test step if a given symbol appears in a format
     * skeleton.
     */
    private static final TimeUnitMapping[] TIME_UNIT_MAPPINGS = {
        new TimeUnitMapping(SECONDS.toMillis(47), "S", "s"),
        new TimeUnitMapping(MINUTES.toMillis(47), "m"),
        new TimeUnitMapping(HOURS.toMillis(5), "H", "K", "h", "k", "j", "J"),
        new TimeUnitMapping(DAYS.toMillis(1), "D", "E", "F", "c", "d", "g"),
        new TimeUnitMapping(DAYS.toMillis(27), "M", "L")
    };

    /** Formats to use instead of the return value of getBestDateTimePattern. */
    private static final FormatMapping[] FORMAT_MAPPINGS = {
        // In Finnish and German, date should be followed by "." but the best format does not
        // include this when date is on its own.
        new FormatMapping("fi", "d", "d."),
        new FormatMapping("fi", "dd", "dd."),
        new FormatMapping("de", "d", "d."),
        new FormatMapping("de", "dd", "dd."),

        // German MMM gives LLL as best format, which returns e.g. "Jan", whereas MMM returns "Jan."
        // Same for Norwegian.
        new FormatMapping("de", "MMM", "MMM"),
        new FormatMapping("no", "MMM", "MMM"),
        new FormatMapping("nb", "MMM", "MMM"),

        // Norwegian time formats should use a dot instead of a colon
        new FormatMapping("no", "HHmm", "HH.mm"),
        new FormatMapping("no", "hmm", "h.mm a"),
        new FormatMapping("nb", "HHmm", "HH.mm"),
        new FormatMapping("nb", "hmm", "h.mm a")
    };

    /**
     * Returns a pattern suitable for use with {@link SimpleDateFormat} to represent a time value in
     * the given locale. The resulting text should fit within a short text field for any input time
     * value. This may be achieved by showing a 12h time without an am/pm indicator in languages in
     * which that indicator would be too long.
     */
    @NonNull
    public static String shortTextTimeFormat(@NonNull Locale locale, boolean use24Hour) {
        if (use24Hour) {
            return bestShortTextDateFormat(locale, new String[] {"HHmm"}, "HH:mm");
        }

        // For 12h clock, the dayPeriod (am/pm) part is often problematic. Try bestDateTimePattern
        // first...
        long timeStep = MINUTES.toMillis(97);
        String pattern = DateFormat.getBestDateTimePattern(locale, "hmm");
        if (isShortEnough(locale, pattern, timeStep)) {
            return pattern;
        }

        // Too long - try removing the space before am/pm, if there is one.
        String patternWithoutSpaceBeforeAmPm = pattern.replace(" a", "a");
        if (!pattern.equals(patternWithoutSpaceBeforeAmPm)
                && isShortEnough(locale, patternWithoutSpaceBeforeAmPm, timeStep)) {
            return patternWithoutSpaceBeforeAmPm;
        }

        // Still too long - try stripping the am/pm part entirely.
        String patternWithoutAmPm = pattern.replace("a", "").trim();
        if (!pattern.equals(patternWithoutAmPm)
                && isShortEnough(locale, patternWithoutAmPm, timeStep)) {
            return patternWithoutAmPm;
        }

        // Still too long. Fall back.
        return "h:mm";
    }

    /**
     * Returns a pattern suitable for use with {@link SimpleDateFormat} to represent a day and
     * month, in the given locale, e.g. "25 Jan" in en-GB. The resulting text should fit within a
     * short text field for any input time value.
     */
    @NonNull
    public static String shortTextDayMonthFormat(@NonNull Locale locale) {
        return bestShortTextDateFormat(locale, new String[] {"MMMd", "MMd", "Md"}, "d/MM");
    }

    /**
     * Returns a pattern suitable for use with {@link SimpleDateFormat} to represent a month, in the
     * given locale, e.g. "Jan" in en-GB. The resulting text should fit within a short text field
     * for any input time value.
     */
    @NonNull
    public static String shortTextMonthFormat(@NonNull Locale locale) {
        return bestShortTextDateFormat(locale, new String[] {"MMM", "MM", "M"}, "MM");
    }

    /**
     * Returns a pattern suitable for use with {@link SimpleDateFormat} to represent the day part of
     * a given date, in the given locale, e.g. "25" in en-GB, or "25." in de. The resulting text
     * should fit within a short text field for any input time value.
     */
    @NonNull
    public static String shortTextDayOfMonthFormat(@NonNull Locale locale) {
        return bestShortTextDateFormat(locale, new String[] {"dd", "d"}, "dd");
    }

    /**
     * Returns a pattern suitable for use with {@link SimpleDateFormat} to represent the day of the
     * week, in the given locale, e.g. "Thu" in en-GB. The resulting text should fit within a short
     * text field for any input time value.
     */
    @NonNull
    public static String shortTextDayOfWeekFormat(@NonNull Locale locale) {
        return bestShortTextDateFormat(locale, new String[] {"EEE", "EEEEEE", "EEEEE"}, "EEEEE");
    }

    /**
     * Returns a date format pattern that will produce results that fit into a short text field when
     * used with the given locale. The skeletons will be passed to {@link
     * DateFormat#getBestDateTimePattern}, and the resulting pattern will be tested for a number of
     * times to see if the text will fit. The first result that fits for all the test times will be
     * returned. If none of the skeletons produces a suitable result, the fallback will be returned
     * instead.
     */
    @NonNull
    private static String bestShortTextDateFormat(
            @NonNull Locale locale, @NonNull String[] skeletons, @NonNull String fallback) {
        for (String skeleton : skeletons) {
            String pattern = null;
            for (FormatMapping mapping : FORMAT_MAPPINGS) {
                if (locale.getLanguage().equals(mapping.mLanguage)
                        && skeleton.equals(mapping.mSkeleton)) {
                    pattern = mapping.mPattern;
                    break;
                }
            }
            if (pattern == null) {
                pattern = DateFormat.getBestDateTimePattern(locale, skeleton);
            }
            if (isShortEnough(locale, pattern, timeStepForSkeleton(skeleton))) {
                return pattern;
            }
        }
        return fallback;
    }

    private static boolean isShortEnough(
            @NonNull Locale locale, @NonNull String pattern, long timeStep) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, locale);
        long testTime = System.currentTimeMillis();

        for (int i = 0; i < TEST_STEPS; i++) {
            if (format.format(new Date(testTime)).length() > SHORT_TEXT_MAX_LENGTH) {
                return false;
            }
            testTime += timeStep;
        }
        return true;
    }

    private static long timeStepForSkeleton(@NonNull String skeleton) {
        long timeStep = 0;
        for (TimeUnitMapping timeMapping : TIME_UNIT_MAPPINGS) {
            for (String symbol : timeMapping.mStrings) {
                if (skeleton.contains(symbol)) {
                    timeStep += timeMapping.mTimeUnit;
                    break;
                }
            }
        }
        return timeStep;
    }

    private static class FormatMapping {
        final String mLanguage;
        final String mSkeleton;
        final String mPattern;

        FormatMapping(String language, String skeleton, String pattern) {
            this.mLanguage = language;
            this.mSkeleton = skeleton;
            this.mPattern = pattern;
        }
    }

    private static class TimeUnitMapping {
        final long mTimeUnit;
        final String[] mStrings;

        TimeUnitMapping(long timeUnit, String... symbols) {
            this.mTimeUnit = timeUnit;
            this.mStrings = symbols;
        }
    }
}
