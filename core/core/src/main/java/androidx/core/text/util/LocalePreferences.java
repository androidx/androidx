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

package androidx.core.text.util;

import android.icu.number.LocalizedNumberFormatter;
import android.icu.number.NumberFormatter;
import android.icu.text.DateFormat;
import android.icu.text.DateTimePatternGenerator;
import android.icu.util.MeasureUnit;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.core.os.BuildCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;
import java.util.Locale.Category;

/**
 * Provides friendly APIs to get the user's locale preferences. The data can refer to
 * external/cldr/common/main/en.xml.
 */
public final class LocalePreferences {
    private static final String TAG = LocalePreferences.class.getSimpleName();

    /** APIs to get the user's preference of the hour cycle. */
    public static class HourCycle {
        private static final String U_EXTENSION_OF_HOUR_CYCLE = "hc";

        /** 12 Hour System (0-11) */
        public static final String H11 = "h11";
        /** 12 Hour System (1-12) */
        public static final String H12 = "h12";
        /** 24 Hour System (0-23) */
        public static final String H23 = "h23";
        /** 24 Hour System (1-24) */
        public static final String H24 = "h24";
        /** Default hour cycle for the locale */
        public static final String DEFAULT = "";

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @StringDef({
                H11,
                H12,
                H23,
                H24,
                DEFAULT
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface HourCycleTypes {
        }

        private HourCycle() {
        }
    }

    /**
     * Return the user's preference of the hour cycle which is from
     * {@link Locale#getDefault(Locale.Category)}. The returned result is resolved and
     * bases on the {@code Locale#getDefault(Locale.Category)}. E.g. "h23"
     */
    @NonNull
    @RequiresApi(VERSION_CODES.TIRAMISU)
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @HourCycle.HourCycleTypes
    public static String getHourCycle() {
        return getHourCycle(true);
    }

    /**
     * Return the hour cycle setting of the inputted {@link Locale}. The returned result is resolved
     * and bases on the inputted {@code Locale}.
     * E.g. "h23"
     */
    @NonNull
    @RequiresApi(VERSION_CODES.TIRAMISU)
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @HourCycle.HourCycleTypes
    public static String getHourCycle(@NonNull Locale locale) {
        return getHourCycle(locale, true);
    }

    /**
     * Return the user's preference of the hour cycle which is from
     * {@link Locale#getDefault(Locale.Category)}. E.g. "h23"
     *
     * @param resolved If the {@code Locale#getDefault(Locale.Category)} contains hour cycle subtag,
     *                 this argument is ignored. If the
     *                 {@code Locale#getDefault(Locale.Category)} doesn't contain hour cycle subtag
     *                 and the resolved argument is true, this function tries to find the default
     *                 hour cycle for the {@code Locale#getDefault(Locale.Category)}. If the
     *                 {@code Locale#getDefault(Locale.Category)} doesn't contain hour cycle subtag
     *                 and the resolved argument is false, this function returns empty string
     *                 i.e. HourCycle.Default.
     * @return {@link HourCycle.HourCycleTypes} If the malformed hour cycle format was specified
     * in the hour cycle subtag, e.g. en-US-u-hc-h32, this function returns empty string
     * i.e. HourCycle.Default.
     */
    @NonNull
    @RequiresApi(VERSION_CODES.TIRAMISU)
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @HourCycle.HourCycleTypes
    public static String getHourCycle(
            boolean resolved) {
        return getHourCycle(Api33Impl.getDefaultLocale(), resolved);
    }

    /**
     * Return the hour cycle setting of the inputted {@link Locale}. E.g. "en-US-u-hc-h23".
     *
     * @param locale   The {@code Locale} to get the hour cycle.
     * @param resolved If the given {@code Locale} contains hour cycle subtag, this argument is
     *                 ignored. If the given {@code Locale} doesn't contain hour cycle subtag and
     *                 the resolved argument is true, this function tries to find the default
     *                 hour cycle for the given {@code Locale}. If the given {@code Locale} doesn't
     *                 contain hour cycle subtag and the resolved argument is false, this function
     *                 return empty string i.e. HourCycle.Default.
     * @return {@link HourCycle.HourCycleTypes} If the malformed hour cycle format was specified
     * in the hour cycle subtag, e.g. en-US-u-hc-h32, this function returns empty string
     * i.e. HourCycle.Default.
     */
    @NonNull
    @RequiresApi(VERSION_CODES.TIRAMISU)
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @HourCycle.HourCycleTypes
    public static String getHourCycle(@NonNull Locale locale, boolean resolved) {
        if (!BuildCompat.isAtLeastT()) {
            throw new IllegalArgumentException("not a valid extension: " + VERSION.SDK_INT);
        }
        return Api33Impl.getHourCycle(locale, resolved);
    }

    /** APIs to get the user's preference of Calendar. */
    public static class CalendarType {
        private static final String U_EXTENSION_OF_CALENDAR = "ca";
        /** Chinese Calendar */
        public static final String CHINESE = "chinese";
        /** Dangi Calendar (Korea Calendar) */
        public static final String DANGI = "dangi";
        /** Gregorian Calendar */
        public static final String GREGORIAN = "gregorian";
        /** Hebrew Calendar */
        public static final String HEBREW = "hebrew";
        /** Indian National Calendar */
        public static final String INDIAN = "indian";
        /** Islamic Calendar */
        public static final String ISLAMIC = "islamic";
        /** Islamic Calendar (tabular, civil epoch) */
        public static final String ISLAMIC_CIVIL = "islamic-civil";
        /** Islamic Calendar (Saudi Arabia, sighting) */
        public static final String ISLAMIC_RGSA = "islamic-rgsa";
        /** Islamic Calendar (tabular, astronomical epoch) */
        public static final String ISLAMIC_TBLA = "islamic-tbla";
        /** Islamic Calendar (Umm al-Qura) */
        public static final String ISLAMIC_UMALQURA = "islamic-umalqura";
        /** Persian Calendar */
        public static final String PERSIAN = "persian";
        /** Default calendar for the locale */
        public static final String DEFAULT = "";

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @StringDef({
                CHINESE,
                DANGI,
                GREGORIAN,
                HEBREW,
                INDIAN,
                ISLAMIC,
                ISLAMIC_CIVIL,
                ISLAMIC_RGSA,
                ISLAMIC_TBLA,
                ISLAMIC_UMALQURA,
                PERSIAN,
                DEFAULT
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface CalendarTypes {
        }

        private CalendarType() {
        }
    }

    /**
     * Return the user's preference of the calendar type which is from {@link
     * Locale#getDefault(Locale.Category)}. The returned result is resolved and bases on
     * the {@code Locale#getDefault(Locale.Category)} settings. E.g. "chinese"
     */
    @NonNull
    @RequiresApi(VERSION_CODES.TIRAMISU)
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @CalendarType.CalendarTypes
    public static String getCalendarType() {
        return getCalendarType(true);
    }

    /**
     * Return the calendar type of the inputted {@link Locale}. The returned result is resolved and
     * bases on the inputted {@link Locale} settings.
     * E.g. "chinese"
     */
    @NonNull
    @RequiresApi(VERSION_CODES.TIRAMISU)
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @CalendarType.CalendarTypes
    public static String getCalendarType(@NonNull Locale locale) {
        return getCalendarType(locale, true);
    }

    /**
     * Return the user's preference of the calendar type which is from {@link
     * Locale#getDefault(Locale.Category)}. E.g. "chinese"
     *
     * @param resolved If the {@code Locale#getDefault(Locale.Category)} contains calendar type
     *                 subtag, this argument is ignored. If the
     *                 {@code Locale#getDefault(Locale.Category)} doesn't contain calendar type
     *                 subtag and the resolved argument is true, this function tries to find
     *                 the default calendar type for the
     *                 {@code Locale#getDefault(Locale.Category)}. If the
     *                 {@code Locale#getDefault(Locale.Category)} doesn't contain calendar type
     *                 subtag and the resolved argument is false, this function returns empty string
     *                 i.e. CalendarTypes.Default.
     * @return {@link CalendarType.CalendarTypes} If the malformed calendar type format was
     * specified in the calendar type subtag, e.g. en-US-u-ca-calendar, this function returns
     * empty string i.e. CalendarTypes.Default.
     */
    @NonNull
    @RequiresApi(VERSION_CODES.TIRAMISU)
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @CalendarType.CalendarTypes
    public static String getCalendarType(boolean resolved) {
        return getCalendarType(Api33Impl.getDefaultLocale(), resolved);
    }

    /**
     * Return the calendar type of the inputted {@link Locale}. E.g. "chinese"
     *
     * @param locale   The {@link Locale} to get the calendar type.
     * @param resolved If the given {@code Locale} contains calendar type subtag, this argument is
     *                 ignored. If the given {@code Locale} doesn't contain calendar type subtag and
     *                 the resolved argument is true, this function tries to find the default
     *                 calendar type for the given {@code Locale}. If the given {@code Locale}
     *                 doesn't contain calendar type subtag and the resolved argument is false, this
     *                 function return empty string i.e. CalendarTypes.Default.
     * @return {@link CalendarType.CalendarTypes} If the malformed calendar type format was
     * specified in the calendar type subtag, e.g. en-US-u-ca-calendar, this function returns
     * empty string i.e. CalendarTypes.Default.
     */
    @NonNull
    @RequiresApi(VERSION_CODES.TIRAMISU)
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @CalendarType.CalendarTypes
    public static String getCalendarType(@NonNull Locale locale, boolean resolved) {
        if (!BuildCompat.isAtLeastT()) {
            throw new IllegalArgumentException("not a valid extension: " + VERSION.SDK_INT);
        }
        return Api33Impl.getCalendarType(locale, resolved);
    }

    /** APIs to get the user's preference of temperature unit. */
    public static class TemperatureUnit {
        private static final String U_EXTENSION_OF_TEMPERATURE_UNIT = "mu";
        /** Celsius */
        public static final String CELSIUS = "celsius";
        /** Fahrenheit */
        public static final String FAHRENHEIT = "fahrenheit";
        /** Kelvin */
        public static final String KELVIN = "kelvin";
        /** Default Temperature for the locale */
        public static final String DEFAULT = "";

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @StringDef({
                CELSIUS,
                FAHRENHEIT,
                KELVIN,
                DEFAULT
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface TemperatureUnits {
        }

        private TemperatureUnit() {
        }
    }

    /**
     * Return the user's preference of the temperature unit which is from {@link
     * Locale#getDefault(Locale.Category)}. The returned result is resolved and bases on the
     * {@code Locale#getDefault(Locale.Category)} settings. E.g. "fahrenheit"
     */
    @NonNull
    @RequiresApi(VERSION_CODES.TIRAMISU)
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @TemperatureUnit.TemperatureUnits
    public static String getTemperatureUnit() {
        return getTemperatureUnit(true);
    }

    /**
     * Return the temperature unit of the inputted {@link Locale}. The returned result is resolved
     * and bases on the inputted {@code Locale} settings. E.g. "fahrenheit"
     */
    @NonNull
    @RequiresApi(VERSION_CODES.TIRAMISU)
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @TemperatureUnit.TemperatureUnits
    public static String getTemperatureUnit(
            @NonNull Locale locale) {
        return getTemperatureUnit(locale, true);
    }

    /**
     * Return the user's preference of the temperature unit which is from {@link
     * Locale#getDefault(Locale.Category)}. E.g. "fahrenheit"
     *
     * @param resolved If the {@code Locale#getDefault(Locale.Category)} contains temperature unit
     *                 subtag, this argument is ignored. If the
     *                 {@code Locale#getDefault(Locale.Category)} doesn't contain temperature unit
     *                 subtag and the resolved argument is true, this function tries to find
     *                 the default temperature unit for the
     *                 {@code Locale#getDefault(Locale.Category)}. If the
     *                 {@code Locale#getDefault(Locale.Category)} doesn't contain temperature unit
     *                 subtag and the resolved argument is false, this function returns empty string
     *                 i.e. TemperatureUnits.Default.
     * @return {@link TemperatureUnit.TemperatureUnits} If the malformed temperature unit format was
     * specified in the temperature unit subtag, e.g. en-US-u-mu-temperature, this function returns
     * empty string i.e. TemperatureUnits.Default.
     */
    @NonNull
    @RequiresApi(VERSION_CODES.TIRAMISU)
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @TemperatureUnit.TemperatureUnits
    public static String getTemperatureUnit(boolean resolved) {
        return getTemperatureUnit(Api33Impl.getDefaultLocale(), resolved);
    }

    /**
     * Return the temperature unit of the inputted {@link Locale}. E.g. "fahrenheit"
     *
     * @param locale   The {@link Locale} to get the temperature unit.
     * @param resolved If the given {@code Locale} contains temperature unit subtag, this argument
     *                 is ignored. If the given {@code Locale} doesn't contain temperature unit
     *                 subtag and the resolved argument is true, this function tries to find
     *                 the default temperature unit for the given {@code Locale}. If the given
     *                 {@code Locale} doesn't contain temperature unit subtag and the resolved
     *                 argument is false, this function return empty string
     *                 i.e. TemperatureUnits.Default.
     * @return {@link TemperatureUnit.TemperatureUnits} If the malformed temperature unit format was
     * specified in the temperature unit subtag, e.g. en-US-u-mu-temperature, this function returns
     * empty string i.e. TemperatureUnits.Default.
     */
    @NonNull
    @RequiresApi(VERSION_CODES.TIRAMISU)
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @TemperatureUnit.TemperatureUnits
    public static String getTemperatureUnit(@NonNull Locale locale, boolean resolved) {
        if (!BuildCompat.isAtLeastT()) {
            throw new IllegalArgumentException("not a valid extension: " + VERSION.SDK_INT);
        }
        return Api33Impl.getTemperatureUnit(locale, resolved);
    }

    /** APIs to get the user's preference of the first day of week. */
    public static class FirstDayOfWeek {
        private static final String U_EXTENSION_OF_FIRST_DAY_OF_WEEK = "fw";
        /** Sunday */
        public static final String SUNDAY = "sun";
        /** Monday */
        public static final String MONDAY = "mon";
        /** Tuesday */
        public static final String TUESDAY = "tue";
        /** Wednesday */
        public static final String WEDNESDAY = "wed";
        /** Thursday */
        public static final String THURSDAY = "thu";
        /** Friday */
        public static final String FRIDAY = "fri";
        /** Saturday */
        public static final String SATURDAY = "sat";
        /** Default first day of week for the locale */
        public static final String DEFAULT = "";

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @StringDef({
                SUNDAY,
                MONDAY,
                TUESDAY,
                WEDNESDAY,
                THURSDAY,
                FRIDAY,
                SATURDAY,
                DEFAULT
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface Days {
        }

        private FirstDayOfWeek() {
        }
    }

    /**
     * Return the user's preference of the first day of week which is from
     * {@link Locale#getDefault(Locale.Category)}. The returned result is resolved and bases on the
     * {@code Locale#getDefault(Locale.Category)} settings. E.g. "sun"
     */
    @NonNull
    @RequiresApi(VERSION_CODES.TIRAMISU)
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @FirstDayOfWeek.Days
    public static String getFirstDayOfWeek() {
        return getFirstDayOfWeek(true);
    }

    /**
     * Return the first day of week of the inputted {@link Locale}. The returned result is resolved
     * and bases on the inputted {@code Locale} settings.
     * E.g. "sun"
     */
    @NonNull
    @RequiresApi(VERSION_CODES.TIRAMISU)
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    public static @FirstDayOfWeek.Days String getFirstDayOfWeek(@NonNull Locale locale) {
        return getFirstDayOfWeek(locale, true);
    }

    /**
     * Return the user's preference of the first day of week which is from {@link
     * Locale#getDefault(Locale.Category)}. E.g. "sun"
     *
     * @param resolved If the {@code Locale#getDefault(Locale.Category)} contains first day of week
     *                 subtag, this argument is ignored. If the
     *                 {@code Locale#getDefault(Locale.Category)} doesn't contain first day of week
     *                 subtag and the resolved argument is true, this function tries to find
     *                 the default first day of week for the
     *                 {@code Locale#getDefault(Locale.Category)}. If the
     *                 {@code Locale#getDefault(Locale.Category)} doesn't contain first day of week
     *                 subtag and the resolved argument is false, this function returns empty string
     *                 i.e. Days.Default.
     * @return {@link FirstDayOfWeek.Days} If the malformed first day of week format was specified
     * in the first day of week subtag, e.g. en-US-u-fw-days, this function returns empty string
     * i.e. Days.Default.
     */
    @NonNull
    @RequiresApi(VERSION_CODES.TIRAMISU)
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @FirstDayOfWeek.Days
    public static String getFirstDayOfWeek(boolean resolved) {
        return getFirstDayOfWeek(Api33Impl.getDefaultLocale(), resolved);
    }

    /**
     * Return the first day of week of the inputted {@link Locale}. E.g. "sun"
     *
     * @param locale   The {@link Locale} to get the first day of week.
     * @param resolved If the given {@code Locale} contains first day of week subtag, this argument
     *                 is ignored. If the given {@code Locale} doesn't contain first day of week
     *                 subtag and the resolved argument is true, this function tries to find
     *                 the default first day of week for the given {@code Locale}. If the given
     *                 {@code Locale} doesn't contain first day of week subtag and the resolved
     *                 argument is false, this function return empty string i.e. Days.Default.
     * @return {@link FirstDayOfWeek.Days} If the malformed first day of week format was
     * specified in the first day of week subtag, e.g. en-US-u-fw-days, this function returns
     * empty string i.e. Days.Default.
     */
    @NonNull
    @RequiresApi(VERSION_CODES.TIRAMISU)
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @FirstDayOfWeek.Days
    public static String getFirstDayOfWeek(
            @NonNull Locale locale, boolean resolved) {
        if (!BuildCompat.isAtLeastT()) {
            throw new IllegalArgumentException("not a valid extension: " + VERSION.SDK_INT);
        }

        return Api33Impl.getFirstDayOfWeek(locale, resolved);
    }

    @RequiresApi(VERSION_CODES.TIRAMISU)
    private static class Api33Impl {
        @DoNotInline
        @HourCycle.HourCycleTypes
        static String getHourCycle(@NonNull Locale locale,
                boolean resolved) {
            String hc = locale.getUnicodeLocaleType(HourCycle.U_EXTENSION_OF_HOUR_CYCLE);
            if (hc != null) {
                return hc;
            }
            if (!resolved) {
                return HourCycle.DEFAULT;
            }

            return getHourCycleType(
                    DateTimePatternGenerator.getInstance(locale).getDefaultHourCycle());

        }

        @DoNotInline
        @CalendarType.CalendarTypes
        static String getCalendarType(@NonNull Locale locale, boolean resolved) {
            String ca = locale.getUnicodeLocaleType(CalendarType.U_EXTENSION_OF_CALENDAR);
            if (ca != null) {
                return ca;
            }
            if (!resolved) {
                return CalendarType.DEFAULT;
            }

            return android.icu.util.Calendar.getInstance(locale).getType();
        }

        @DoNotInline
        @TemperatureUnit.TemperatureUnits
        static String getTemperatureUnit(@NonNull Locale locale, boolean resolved) {
            String mu =
                    locale.getUnicodeLocaleType(TemperatureUnit.U_EXTENSION_OF_TEMPERATURE_UNIT);
            if (mu != null) {
                if (mu.contains("fahrenhe")) {
                    mu = TemperatureUnit.FAHRENHEIT;
                }
                return mu;
            }
            if (!resolved) {
                return TemperatureUnit.DEFAULT;
            }

            return getResolvedTemperatureUnit(locale);
        }

        @DoNotInline
        @FirstDayOfWeek.Days
        static String getFirstDayOfWeek(@NonNull Locale locale, boolean resolved) {
            String mu =
                    locale.getUnicodeLocaleType(FirstDayOfWeek.U_EXTENSION_OF_FIRST_DAY_OF_WEEK);
            if (mu != null) {
                return mu;
            }
            if (!resolved) {
                return FirstDayOfWeek.DEFAULT;
            }
            // TODO(b/262294472) Use {@code android.icu.util.Calendar} instead of
            //  {@code java.util.Calendar}.
            return getStringOfFirstDayOfWeek(
                    java.util.Calendar.getInstance(locale).getFirstDayOfWeek());
        }

        @DoNotInline
        static Locale getDefaultLocale() {
            return Locale.getDefault(Category.FORMAT);
        }

        private static String getStringOfFirstDayOfWeek(int fw) {
            String[] arrDays = {
                    FirstDayOfWeek.SUNDAY,
                    FirstDayOfWeek.MONDAY,
                    FirstDayOfWeek.TUESDAY,
                    FirstDayOfWeek.WEDNESDAY,
                    FirstDayOfWeek.THURSDAY,
                    FirstDayOfWeek.FRIDAY,
                    FirstDayOfWeek.SATURDAY};

            return fw >= 1 && fw <= 7 ? arrDays[fw - 1] : FirstDayOfWeek.DEFAULT;
        }

        @HourCycle.HourCycleTypes
        private static String getHourCycleType(
                DateFormat.HourCycle hourCycle) {
            switch (hourCycle) {
                case HOUR_CYCLE_11:
                    return HourCycle.H11;
                case HOUR_CYCLE_12:
                    return HourCycle.H12;
                case HOUR_CYCLE_23:
                    return HourCycle.H23;
                case HOUR_CYCLE_24:
                    return HourCycle.H24;
                default:
                    return HourCycle.DEFAULT;
            }
        }

        @TemperatureUnit.TemperatureUnits
        private static String getResolvedTemperatureUnit(@NonNull Locale locale) {
            LocalizedNumberFormatter nf = NumberFormatter.with()
                    .usage("temperature")
                    .unit(MeasureUnit.CELSIUS)
                    .locale(locale);
            return nf.format(1).getOutputUnit().getIdentifier();
        }

        private Api33Impl() {
        }
    }

    private LocalePreferences() {
    }
}
