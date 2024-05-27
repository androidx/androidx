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

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.DateFormat
import java.text.FieldPosition
import java.util.Calendar
import java.util.Locale

internal interface IDateTimeFormatterImpl {
    fun format(calendar: Calendar): String
}

internal class DateTimeFormatterImplAndroid(skeleton: String, locale: Locale) :
    IDateTimeFormatterImpl {

    private val sdf =
        java.text.SimpleDateFormat(
            android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton),
            locale
        )

    override fun format(calendar: Calendar): String {
        return sdf.format(calendar.time)
    }
}

// ICU4J will give better results, so we use it if we can.
// And android.text.format.DateFormat.getBestDateTimePattern has a bug in API 26 and 27.
// For some skeletons it returns "b" instead of "a" in patterns (the am/pm).
// That is invalid, and throws when used to build the SimpleDateFormat
// Using ICU avoids that.
// Should also be faster, as the Android used JNI.
@RequiresApi(Build.VERSION_CODES.N)
internal class DateTimeFormatterImplIcu(skeleton: String, private var locale: Locale) :
    IDateTimeFormatterImpl {

    private val sdf = android.icu.text.DateFormat.getInstanceForSkeleton(skeleton, locale)

    override fun format(calendar: Calendar): String {
        val result = StringBuffer()
        val tz = android.icu.util.TimeZone.getTimeZone(calendar.timeZone.id)
        val ucal = android.icu.util.Calendar.getInstance(tz, locale)
        sdf.calendar = ucal
        val fp = FieldPosition(0)
        sdf.format(calendar.timeInMillis, result, fp)
        return result.toString()
    }
}

internal class DateTimeFormatterImplJdkStyle(dateStyle: Int, timeStyle: Int, locale: Locale) :
    IDateTimeFormatterImpl {

    private val sdf =
        if (dateStyle == -1) {
            if (timeStyle == -1) {
                DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, locale)
            } else {
                DateFormat.getTimeInstance(timeStyle, locale)
            }
        } else {
            if (timeStyle == -1) {
                DateFormat.getDateInstance(dateStyle, locale)
            } else {
                DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale)
            }
        }

    override fun format(calendar: Calendar): String {
        return sdf.format(calendar.time)
    }
}
