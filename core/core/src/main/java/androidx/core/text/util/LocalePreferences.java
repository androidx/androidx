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
import android.os.Build;
import android.os.Build.VERSION_CODES;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
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
        private static final String U_EXTENSION_TAG = "hc";

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
     * bases on the {@code Locale#getDefault(Locale.Category)}. It is one of the strings defined in
     * {@see HourCycle}, e.g. {@code HourCycle#H11}.
     */
    @HourCycle.HourCycleTypes
    public static @NonNull String getHourCycle() {
        return getHourCycle(true);
    }

    /**
     * Return the hour cycle setting of the inputted {@link Locale}. The returned result is resolved
     * and based on the input {@code Locale}. It is one of the strings defined in
     * {@see HourCycle}, e.g. {@code HourCycle#H11}.
     */
    @HourCycle.HourCycleTypes
    public static @NonNull String getHourCycle(@NonNull Locale locale) {
        return getHourCycle(locale, true);
    }

    /**
     * Return the user's preference of the hour cycle which is from
     * {@link Locale#getDefault(Locale.Category)}, e.g. {@code HourCycle#H11}.
     *
     * @param resolved If the {@code Locale#getDefault(Locale.Category)} contains hour cycle subtag,
     *                 this argument is ignored. If the
     *                 {@code Locale#getDefault(Locale.Category)} doesn't contain hour cycle subtag
     *                 and the resolved argument is true, this function tries to find the default
     *                 hour cycle for the {@code Locale#getDefault(Locale.Category)}. If the
     *                 {@code Locale#getDefault(Locale.Category)} doesn't contain hour cycle subtag
     *                 and the resolved argument is false, this function returns empty string
     *                 , i.e. {@code HourCycle#DEFAULT}.
     * @return {@link HourCycle.HourCycleTypes} If the malformed hour cycle format was specified
     * in the hour cycle subtag, e.g. en-US-u-hc-h32, this function returns empty string, i.e.
     * {@code HourCycle#DEFAULT}.
     */
    @HourCycle.HourCycleTypes
    public static @NonNull String getHourCycle(
            boolean resolved) {
        Locale defaultLocale = (Build.VERSION.SDK_INT >= VERSION_CODES.N)
                ? Api24Impl.getDefaultLocale()
                : getDefaultLocale();
        return getHourCycle(defaultLocale, resolved);
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
     *                 return empty string, i.e. {@code HourCycle#DEFAULT}.
     * @return {@link HourCycle.HourCycleTypes} If the malformed hour cycle format was specified
     * in the hour cycle subtag, e.g. en-US-u-hc-h32, this function returns empty string, i.e.
     * {@code HourCycle#DEFAULT}.
     */
    @HourCycle.HourCycleTypes
    public static @NonNull String getHourCycle(@NonNull Locale locale, boolean resolved) {
        String result = getUnicodeLocaleType(HourCycle.U_EXTENSION_TAG,
                HourCycle.DEFAULT, locale, resolved);
        if (result != null) {
            return result;
        }
        if (Build.VERSION.SDK_INT >= 33) {
            return Api33Impl.getHourCycle(locale);
        } else {
            return getBaseHourCycle(locale);
        }
    }

    /** APIs to get the user's preference of Calendar. */
    public static class CalendarType {
        private static final String U_EXTENSION_TAG = "ca";
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
     * the {@code Locale#getDefault(Locale.Category)} settings. It is one of the strings defined in
     * {@see CalendarType}, e.g. {@code CalendarType#CHINESE}.
     */
    @CalendarType.CalendarTypes
    public static @NonNull String getCalendarType() {
        return getCalendarType(true);
    }

    /**
     * Return the calendar type of the inputted {@link Locale}. The returned result is resolved and
     * based on the input {@link Locale} settings. It is one of the strings defined in
     * {@see CalendarType}, e.g. {@code CalendarType#CHINESE}.
     */
    @CalendarType.CalendarTypes
    public static @NonNull String getCalendarType(@NonNull Locale locale) {
        return getCalendarType(locale, true);
    }

    /**
     * Return the user's preference of the calendar type which is from {@link
     * Locale#getDefault(Category)}, e.g. {@code CalendarType#CHINESE}.
     *
     * @param resolved If the {@code Locale#getDefault(Locale.Category)} contains calendar type
     *                 subtag, this argument is ignored. If the
     *                 {@code Locale#getDefault(Locale.Category)} doesn't contain calendar type
     *                 subtag and the resolved argument is true, this function tries to find
     *                 the default calendar type for the
     *                 {@code Locale#getDefault(Locale.Category)}. If the
     *                 {@code Locale#getDefault(Locale.Category)} doesn't contain calendar type
     *                 subtag and the resolved argument is false, this function returns empty string
     *                 , i.e. {@code CalendarType#DEFAULT}.
     * @return {@link CalendarType.CalendarTypes} If the malformed calendar type format was
     * specified in the calendar type subtag, e.g. en-US-u-ca-calendar, this function returns
     * empty string, i.e. {@code CalendarType#DEFAULT}.
     */
    @CalendarType.CalendarTypes
    public static @NonNull String getCalendarType(boolean resolved) {
        Locale defaultLocale = (Build.VERSION.SDK_INT >= VERSION_CODES.N)
                ? Api24Impl.getDefaultLocale()
                : getDefaultLocale();
        return getCalendarType(defaultLocale, resolved);
    }

    /**
     * Return the calendar type of the inputted {@link Locale}, e.g. {@code CalendarType#CHINESE}.
     *
     * @param locale   The {@link Locale} to get the calendar type.
     * @param resolved If the given {@code Locale} contains calendar type subtag, this argument is
     *                 ignored. If the given {@code Locale} doesn't contain calendar type subtag and
     *                 the resolved argument is true, this function tries to find the default
     *                 calendar type for the given {@code Locale}. If the given {@code Locale}
     *                 doesn't contain calendar type subtag and the resolved argument is false, this
     *                 function return empty string, i.e. {@code CalendarType#DEFAULT}.
     * @return {@link CalendarType.CalendarTypes} If the malformed calendar type format was
     * specified in the calendar type subtag, e.g. en-US-u-ca-calendar, this function returns
     * empty string, i.e. {@code CalendarType#DEFAULT}.
     */
    @CalendarType.CalendarTypes
    public static @NonNull String getCalendarType(@NonNull Locale locale, boolean resolved) {
        String result = getUnicodeLocaleType(CalendarType.U_EXTENSION_TAG,
                CalendarType.DEFAULT, locale, resolved);
        if (result != null) {
            return result;
        }
        if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
            return Api24Impl.getCalendarType(locale);
        } else {
            return resolved ? CalendarType.GREGORIAN : CalendarType.DEFAULT;
        }
    }

    /** APIs to get the user's preference of temperature unit. */
    public static class TemperatureUnit {
        private static final String U_EXTENSION_TAG = "mu";
        /** Celsius */
        public static final String CELSIUS = "celsius";
        /** Fahrenheit */
        public static final String FAHRENHEIT = "fahrenhe";
        /** Kelvin */
        public static final String KELVIN = "kelvin";
        /** Default Temperature for the locale */
        public static final String DEFAULT = "";

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
     * {@code Locale#getDefault(Locale.Category)} settings. It is one of the strings defined in
     * {@see TemperatureUnit}, e.g. {@code TemperatureUnit#FAHRENHEIT}.
     */
    @TemperatureUnit.TemperatureUnits
    public static @NonNull String getTemperatureUnit() {
        return getTemperatureUnit(true);
    }

    /**
     * Return the temperature unit of the inputted {@link Locale}. It is one of the strings
     * defined in {@see TemperatureUnit}, e.g. {@code TemperatureUnit#FAHRENHEIT}.
     */
    @TemperatureUnit.TemperatureUnits
    public static @NonNull String getTemperatureUnit(
            @NonNull Locale locale) {
        return getTemperatureUnit(locale, true);
    }

    /**
     * Return the user's preference of the temperature unit which is from {@link
     * Locale#getDefault(Locale.Category)}, e.g. {@code TemperatureUnit#FAHRENHEIT}.
     *
     * @param resolved If the {@code Locale#getDefault(Locale.Category)} contains temperature unit
     *                 subtag, this argument is ignored. If the
     *                 {@code Locale#getDefault(Locale.Category)} doesn't contain temperature unit
     *                 subtag and the resolved argument is true, this function tries to find
     *                 the default temperature unit for the
     *                 {@code Locale#getDefault(Locale.Category)}. If the
     *                 {@code Locale#getDefault(Locale.Category)} doesn't contain temperature unit
     *                 subtag and the resolved argument is false, this function returns empty string
     *                 , i.e. {@code TemperatureUnit#DEFAULT}.
     * @return {@link TemperatureUnit.TemperatureUnits} If the malformed temperature unit format was
     * specified in the temperature unit subtag, e.g. en-US-u-mu-temperature, this function returns
     * empty string, i.e. {@code TemperatureUnit#DEFAULT}.
     */
    @TemperatureUnit.TemperatureUnits
    public static @NonNull String getTemperatureUnit(boolean resolved) {
        Locale defaultLocale = (Build.VERSION.SDK_INT >= VERSION_CODES.N)
                ? Api24Impl.getDefaultLocale()
                : getDefaultLocale();
        return getTemperatureUnit(defaultLocale, resolved);
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
     *                 argument is false, this function return empty string, i.e.
     *                 {@code TemperatureUnit#DEFAULT}.
     * @return {@link TemperatureUnit.TemperatureUnits} If the malformed temperature unit format was
     * specified in the temperature unit subtag, e.g. en-US-u-mu-temperature, this function returns
     * empty string, i.e. {@code TemperatureUnit#DEFAULT}.
     */
    @TemperatureUnit.TemperatureUnits
    public static @NonNull String getTemperatureUnit(@NonNull Locale locale, boolean resolved) {
        String result = getUnicodeLocaleType(TemperatureUnit.U_EXTENSION_TAG,
                TemperatureUnit.DEFAULT, locale, resolved);
        if (result != null) {
            return result;
        }
        if (Build.VERSION.SDK_INT >= 33) {
            return Api33Impl.getResolvedTemperatureUnit(locale);
        } else {
            return getTemperatureHardCoded(locale);
        }
    }

    /** APIs to get the user's preference of the first day of week. */
    public static class FirstDayOfWeek {
        private static final String U_EXTENSION_TAG = "fw";
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
     * {@code Locale#getDefault(Locale.Category)} settings. It is one of the strings defined in
     * {@see FirstDayOfWeek}, e.g. {@code FirstDayOfWeek#SUNDAY}.
     */
    @FirstDayOfWeek.Days
    public static @NonNull String getFirstDayOfWeek() {
        return getFirstDayOfWeek(true);
    }

    /**
     * Return the first day of week of the inputted {@link Locale}. The returned result is resolved
     * and based on the input {@code Locale} settings. It is one of the strings defined in
     * {@see FirstDayOfWeek}, e.g. {@code FirstDayOfWeek#SUNDAY}.
     */
    @FirstDayOfWeek.Days
    public static @NonNull String getFirstDayOfWeek(@NonNull Locale locale) {
        return getFirstDayOfWeek(locale, true);
    }

    /**
     * Return the user's preference of the first day of week which is from {@link
     * Locale#getDefault(Locale.Category)}, e.g. {@code FirstDayOfWeek#SUNDAY}.
     *
     * @param resolved If the {@code Locale#getDefault(Locale.Category)} contains first day of week
     *                 subtag, this argument is ignored. If the
     *                 {@code Locale#getDefault(Locale.Category)} doesn't contain first day of week
     *                 subtag and the resolved argument is true, this function tries to find
     *                 the default first day of week for the
     *                 {@code Locale#getDefault(Locale.Category)}. If the
     *                 {@code Locale#getDefault(Locale.Category)} doesn't contain first day of week
     *                 subtag and the resolved argument is false, this function returns empty string
     *                 , i.e. {@code FirstDayOfWeek#DEFAULT}.
     * @return {@link FirstDayOfWeek.Days} If the malformed first day of week format was specified
     * in the first day of week subtag, e.g. en-US-u-fw-days, this function returns empty string,
     * i.e. {@code FirstDayOfWeek#DEFAULT}.
     */
    @FirstDayOfWeek.Days
    public static @NonNull String getFirstDayOfWeek(boolean resolved) {
        Locale defaultLocale = (Build.VERSION.SDK_INT >= VERSION_CODES.N)
                ? Api24Impl.getDefaultLocale()
                : getDefaultLocale();
        return getFirstDayOfWeek(defaultLocale, resolved);
    }

    /**
     * Return the first day of week of the inputted {@link Locale},
     * e.g. {@code FirstDayOfWeek#SUNDAY}.
     *
     * @param locale   The {@link Locale} to get the first day of week.
     * @param resolved If the given {@code Locale} contains first day of week subtag, this argument
     *                 is ignored. If the given {@code Locale} doesn't contain first day of week
     *                 subtag and the resolved argument is true, this function tries to find
     *                 the default first day of week for the given {@code Locale}. If the given
     *                 {@code Locale} doesn't contain first day of week subtag and the resolved
     *                 argument is false, this function return empty string, i.e.
     *                 {@code FirstDayOfWeek#DEFAULT}.
     * @return {@link FirstDayOfWeek.Days} If the malformed first day of week format was
     * specified in the first day of week subtag, e.g. en-US-u-fw-days, this function returns
     * empty string, i.e. {@code FirstDayOfWeek#DEFAULT}.
     */
    @FirstDayOfWeek.Days
    public static @NonNull String getFirstDayOfWeek(
            @NonNull Locale locale, boolean resolved) {
        String result = getUnicodeLocaleType(FirstDayOfWeek.U_EXTENSION_TAG,
                FirstDayOfWeek.DEFAULT, locale, resolved);
        return result != null ? result : getBaseFirstDayOfWeek(locale);
    }

    private static String getUnicodeLocaleType(String tag, String defaultValue, Locale locale,
            boolean resolved) {
        String ext = locale.getUnicodeLocaleType(tag);
        if (ext != null) {
            return ext;
        }
        if (!resolved) {
            return defaultValue;
        }
        return null;
    }


    // Warning: This list of country IDs must be in alphabetical order for binarySearch to
    // work correctly.
    private static final String[] WEATHER_FAHRENHEIT_COUNTRIES =
            {"BS", "BZ", "KY", "PR", "PW", "US"};

    @TemperatureUnit.TemperatureUnits
    private static String getTemperatureHardCoded(Locale locale) {
        return Arrays.binarySearch(WEATHER_FAHRENHEIT_COUNTRIES, locale.getCountry()) >= 0
                ? TemperatureUnit.FAHRENHEIT
                : TemperatureUnit.CELSIUS;
    }

    @HourCycle.HourCycleTypes
    private static String getBaseHourCycle(@NonNull Locale locale) {
        String pattern =
                android.text.format.DateFormat.getBestDateTimePattern(
                        locale, "jm");
        return pattern.contains("H") ? HourCycle.H23 : HourCycle.H12;
    }

    @FirstDayOfWeek.Days
    private static String getBaseFirstDayOfWeek(@NonNull Locale locale) {
        // A known bug affects both the {@code android.icu.util.Calendar} and
        // {@code java.util.Calendar}: they ignore the "fw" field in the -u- extension, even if
        // present. So please do not remove the explicit check on getUnicodeLocaleType,
        // which protects us from that bug.
        return getStringOfFirstDayOfWeek(
                java.util.Calendar.getInstance(locale).getFirstDayOfWeek());
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

    private static Locale getDefaultLocale() {
        return Locale.getDefault();
    }

    @RequiresApi(VERSION_CODES.N)
    private static class Api24Impl {
        @CalendarType.CalendarTypes
        static String getCalendarType(@NonNull Locale locale) {
            return android.icu.util.Calendar.getInstance(locale).getType();
        }

        static Locale getDefaultLocale() {
            return Locale.getDefault(Category.FORMAT);
        }

        private Api24Impl() {
        }
    }

    @RequiresApi(VERSION_CODES.TIRAMISU)
    private static class Api33Impl {
        @TemperatureUnit.TemperatureUnits
        static String getResolvedTemperatureUnit(@NonNull Locale locale) {
            LocalizedNumberFormatter nf = NumberFormatter.with()
                    .usage("weather")
                    .unit(MeasureUnit.CELSIUS)
                    .locale(locale);
            String unit = nf.format(1).getOutputUnit().getIdentifier();
            if (unit.startsWith(TemperatureUnit.FAHRENHEIT)) {
                return TemperatureUnit.FAHRENHEIT;
            }
            return unit;
        }

        @HourCycle.HourCycleTypes
        static String getHourCycle(@NonNull Locale locale) {
            return getHourCycleType(
                    DateTimePatternGenerator.getInstance(locale).getDefaultHourCycle());
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

        private Api33Impl() {
        }
    }

    private LocalePreferences() {
    }
}
