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

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.annotation.RequiresApi
import androidx.core.i18n.LocaleCompatUtils.getDefaultFormattingLocale
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * DateTimeFormatter is a class for international-aware date/time formatting.
 *
 * It is designed to encourage best i18n practices, and work correctly on old / new Android
 * versions, without having to test the API level everywhere.
 *
 * @param context the application context.
 * @param options various options for the formatter (what fields should be rendered, length, etc.).
 * @param locale the locale used for formatting. If missing then the application locale will be
 *   used.
 */
public class DateTimeFormatter {

    private val dateFormatter: IDateTimeFormatterImpl

    @JvmOverloads
    public constructor(
        context: Context,
        options: DateTimeFormatterSkeletonOptions,
        locale: Locale = getDefaultFormattingLocale()
    ) {
        val resolvedSkeleton = skeletonRespectingPrefs(context, locale, options.toString())
        dateFormatter =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                DateTimeFormatterImplIcu(resolvedSkeleton, locale)
            } else {
                DateTimeFormatterImplAndroid(resolvedSkeleton, locale)
            }
    }

    @JvmOverloads
    public constructor(
        options: DateTimeFormatterJdkStyleOptions,
        locale: Locale = getDefaultFormattingLocale()
    ) {
        dateFormatter = DateTimeFormatterImplJdkStyle(options.dateStyle, options.timeStyle, locale)
    }

    private fun skeletonRespectingPrefs(context: Context, locale: Locale, options: String): String {
        var strSkeleton =
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                options
            } else {
                // "B" is not supported on older Android
                // Mapping skeleton to pattern will add an `a` for period, if needed
                options.replace("B", "")
            }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // "v" is not supported on older Android
            // If the string comes from Skeleton it is not possible to have
            // both 'v' and 'z'. But just in case.
            if (strSkeleton.contains('z')) {
                // Keep the "z", remove the "v"
                strSkeleton = strSkeleton.replace("v", "")
            } else {
                strSkeleton = strSkeleton.replace('v', 'z')
                strSkeleton = strSkeleton.replace('O', 'z')
            }
        }

        if (!strSkeleton.contains('j')) {
            // No hour, the caller forced the hour to 12h or 24h ("h" or "H")
            return strSkeleton
        }

        // The caller does not force 12h/24h, look at the Android user prefs.
        val deviceHour =
            Settings.System.getString(context.contentResolver, Settings.System.TIME_12_24)

        return when (deviceHour) {
            "12" -> strSkeleton.replace('j', 'h')
            "24" -> strSkeleton.replace('j', 'H')
            else ->
                if (is24HourLocale(locale)) {
                    // The locale is a 24h locale, the time period ( `a` or `B` ) does not matter
                    strSkeleton
                } else {
                    strSkeleton.replace("a", "")
                    strSkeleton.replace('j', 'h')
                }
        }
    }

    private fun is24HourLocale(locale: Locale): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Api24Utils.is24HourLocale(locale)
        }
        val tempPattern = DateFormat.getBestDateTimePattern(locale, "jm")
        return tempPattern.contains('H')
    }

    /**
     * Formats an epoch time into a user friendly, locale aware, date/time string.
     *
     * @param milliseconds the date / time to format expressed in milliseconds since January 1,
     *   1970, 00:00:00 GMT.
     * @return the formatted date / time string.
     */
    public fun format(milliseconds: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliseconds
        return format(calendar)
    }

    /**
     * Formats a Date object into a user friendly locale aware date/time string.
     *
     * @param date the date / time to format.
     * @return the formatted date / time string.
     */
    public fun format(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return format(calendar)
    }

    /**
     * Formats a Calendar object into a user friendly locale aware date/time string.
     *
     * @param calendar the date / time to format.
     * @return the formatted date / time string.
     */
    public fun format(calendar: Calendar): String {
        return dateFormatter.format(calendar)
    }

    private companion object {
        // To avoid ClassVerificationFailure
        @RequiresApi(Build.VERSION_CODES.N)
        private class Api24Utils {
            companion object {
                fun is24HourLocale(locale: Locale): Boolean {
                    val tmpDf = android.icu.text.DateFormat.getInstanceForSkeleton("jm", locale)
                    val tmpPattern =
                        // This is true for all ICU implementation until now, but just in case
                        if (tmpDf is SimpleDateFormat) {
                            tmpDf.toPattern()
                        } else {
                            DateFormat.getBestDateTimePattern(locale, "jm")
                        }
                    return tmpPattern.contains('H')
                }
            }
        }
    }
}
