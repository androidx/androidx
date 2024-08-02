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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import java.text.DateFormat

/** Date/time formatting styles, compatible to the ones in [java.text.DateFormat] */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    /** SHORT is completely numeric, such as 12.13.52 or 3:30pm. */
    DateFormat.SHORT,
    /** MEDIUM is longer, such as Jan 12, 1952. */
    DateFormat.MEDIUM,
    /** LONG is longer, such as January 12, 1952 or 3:30:32pm. */
    DateFormat.LONG,
    /** FULL is pretty completely specified, such as Tuesday, April 12, 1952 AD or 3:30:42pm PST. */
    DateFormat.FULL
)
public annotation class DateTimeStyle

/**
 * This class provides functionality similar to the one in [java.text.DateFormat].
 *
 * Although not very flexible, it makes migration easier.
 */
public class DateTimeFormatterJdkStyleOptions
private constructor(
    public val dateStyle: @DateTimeStyle Int,
    public val timeStyle: @DateTimeStyle Int
) {
    public companion object {
        /**
         * Gets the date formatter with the given formatting style for the default locale.
         *
         * @param style the given date formatting style. For example, SHORT for "M/d/yy" in the US
         *   locale.
         * @return the formatting options to use with [androidx.core.i18n.DateTimeFormatter].
         */
        @JvmStatic
        public fun createDateInstance(style: @DateTimeStyle Int): DateTimeFormatterJdkStyleOptions {
            return DateTimeFormatterJdkStyleOptions(style, -1)
        }

        /**
         * Gets the time formatter with the given formatting style for the default locale.
         *
         * @param style the given time formatting style. For example, SHORT for "h:mm a" in the US
         *   locale.
         * @return the formatting options to use with [androidx.core.i18n.DateTimeFormatter].
         */
        @JvmStatic
        public fun createTimeInstance(style: @DateTimeStyle Int): DateTimeFormatterJdkStyleOptions {
            return DateTimeFormatterJdkStyleOptions(-1, style)
        }

        /**
         * Gets the date / time formatter with the given formatting styles for the default locale.
         *
         * @param dateStyle the given date formatting style. For example, SHORT for "M/d/yy" in the
         *   US locale.
         * @param timeStyle the given time formatting style. For example, SHORT for "h:mm a" in the
         *   US locale.
         * @return the formatting options to use with [androidx.core.i18n.DateTimeFormatter].
         */
        @JvmStatic
        public fun createDateTimeInstance(
            dateStyle: @DateTimeStyle Int,
            timeStyle: @DateTimeStyle Int
        ): DateTimeFormatterJdkStyleOptions {
            return DateTimeFormatterJdkStyleOptions(dateStyle, timeStyle)
        }
    }
}
