/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.wear.watchface.complications

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.wear.watchface.complications.data.ComplicationType

/**
 * Methods to retrieve the component names for system complication complication data sources. This
 * will allow these complication data sources to be used as defaults by watch faces.
 */
public class SystemDataSources private constructor() {
    public companion object {
        // NEXT AVAILABLE DATA SOURCE ID: 17

        /** Specifies that no complication data source should be used. */
        public const val NO_DATA_SOURCE: Int = -1

        /**
         * Id for the 'watch battery' complication complication data source.
         *
         * This is a safe complication data source, so if a watch face uses this as a default it
         * will be able to receive data from it even before the RECEIVE_COMPLICATION_DATA permission
         * has been granted.
         *
         * This complication data source supports the following types:
         * [ComplicationType.MONOCHROMATIC_IMAGE], [ComplicationType.SHORT_TEXT],
         * [ComplicationType.LONG_TEXT], [ComplicationType.RANGED_VALUE].
         */
        public const val DATA_SOURCE_WATCH_BATTERY: Int = 1

        /**
         * Id for the 'date' complication complication data source.
         *
         * This is a safe complication data source, so if a watch face uses this as a default it
         * will be able to receive data from it even before the RECEIVE_COMPLICATION_DATA permission
         * has been granted.
         *
         * This complication data source supports only [ComplicationType.SHORT_TEXT].
         */
        public const val DATA_SOURCE_DATE: Int = 2

        /**
         * Id for the 'time and date' complication complication data source.
         *
         * This is a safe complication data source, so if a watch face uses this as a default it
         * will be able to receive data from it even before the RECEIVE_COMPLICATION_DATA permission
         * has been granted.
         *
         * This complication data source supports only [ComplicationType.SHORT_TEXT].
         */
        public const val DATA_SOURCE_TIME_AND_DATE: Int = 3

        /**
         * Id for the 'step count' complication complication data source.
         *
         * This is a safe complication data source (because it only shows a daily total), so if a
         * watch face uses this as a default it will be able to receive data from it even before the
         * RECEIVE_COMPLICATION_DATA permission has been granted.
         *
         * This complication data source supports only [ComplicationType.SHORT_TEXT].
         */
        public const val DATA_SOURCE_STEP_COUNT: Int = 4

        /**
         * Id for the 'world clock' complication complication data source.
         *
         * This is a safe complication data source, so if a watch face uses this as a default it
         * will be able to receive data from it even before the RECEIVE_COMPLICATION_DATA permission
         * has been granted.
         *
         * This complication data source supports only [ComplicationType.SHORT_TEXT].
         */
        public const val DATA_SOURCE_WORLD_CLOCK: Int = 5

        /**
         * Id for the 'app shortcut' complication complication data source.
         *
         * This is a safe complication data source, so if a watch face uses this as a default it
         * will be able to receive data from it even before the RECEIVE_COMPLICATION_DATA permission
         * has been granted.
         *
         * This complication data source supports the following types:
         * [ComplicationType.SMALL_IMAGE], [ComplicationType.LONG_TEXT].
         */
        public const val DATA_SOURCE_APP_SHORTCUT: Int = 6

        /**
         * Id for the 'unread notification count' complication complication data source.
         *
         * This is a safe complication data source, so if a watch face uses this as a default it
         * will be able to receive data from it even before the RECEIVE_COMPLICATION_DATA permission
         * has been granted.
         *
         * This complication data source supports the following types:
         * [ComplicationType.MONOCHROMATIC_IMAGE], [ComplicationType.SHORT_TEXT].
         */
        public const val DATA_SOURCE_UNREAD_NOTIFICATION_COUNT: Int = 7

        /** Deprecated data source, no longer available. Was GOOGLE_PAY. */
        internal const val DATA_SOURCE_DEPRECATED8: Int = 8

        /**
         * Id for the 'next event' complication complication data source.
         *
         * This is not a safe complication data source, so if a watch face uses this as a default it
         * will receive data of TYPE_NO_PERMISSION until the user has granted the
         * RECEIVE_COMPLICATION_DATA.
         *
         * This complication data source supports the following types:
         * [ComplicationType.SHORT_TEXT], [ComplicationType.LONG_TEXT].
         */
        public const val DATA_SOURCE_NEXT_EVENT: Int = 9

        /** Deprecated data source, no longer available. Was RETAIL_STEP_COUNT. */
        internal const val DATA_SOURCE_DEPRECATED10: Int = 10

        /** Deprecated data source, no longer available. Was RETAIL_CHAT. */
        internal const val DATA_SOURCE_DEPRECATED11: Int = 11

        /**
         * Id for the 'sunrise sunset' complication complication data source.
         *
         * This complication data source shows next sunrise or sunset time according to current
         * timezone and location.
         *
         * This complication data source supports only [ComplicationType.SHORT_TEXT].
         */
        public const val DATA_SOURCE_SUNRISE_SUNSET: Int = 12

        /**
         * Id for the 'day of week' complication complication data source.
         *
         * This is a safe complication data source, so if a watch face uses this as a default it
         * will be able to receive data from it even before the RECEIVE_COMPLICATION_DATA permission
         * has been granted.
         *
         * This complication data source supports only [ComplicationType.SHORT_TEXT].
         */
        public const val DATA_SOURCE_DAY_OF_WEEK: Int = 13

        /**
         * Id for the 'favorite contact' complication complication data source.
         *
         * This is not a safe complication data source, so if a watch face uses this as a default it
         * will receive data of TYPE_NO_PERMISSION until the user has granted the
         * RECEIVE_COMPLICATION_DATA.
         *
         * This complication data source supports only [ComplicationType.SMALL_IMAGE].
         */
        public const val DATA_SOURCE_FAVORITE_CONTACT: Int = 14

        /** Deprecated data source, no longer available. Was MOST_RECENT_APP. */
        internal const val DATA_SOURCE_DEPRECATED15: Int = 15

        /**
         * Id for the 'day and date' complication complication data source.
         *
         * This is a safe complication data source, so if a watch face uses this as a default it
         * will be able to receive data from it even before the RECEIVE_COMPLICATION_DATA permission
         * has been granted.
         *
         * This complication data source supports only [ComplicationType.SHORT_TEXT].
         */
        public const val DATA_SOURCE_DAY_AND_DATE: Int = 16
    }

    /** System complication data source id as defined in [SystemDataSources]. */
    @IntDef(
        NO_DATA_SOURCE,
        DATA_SOURCE_WATCH_BATTERY,
        DATA_SOURCE_DATE,
        DATA_SOURCE_TIME_AND_DATE,
        DATA_SOURCE_STEP_COUNT,
        DATA_SOURCE_WORLD_CLOCK,
        DATA_SOURCE_APP_SHORTCUT,
        DATA_SOURCE_UNREAD_NOTIFICATION_COUNT,
        DATA_SOURCE_NEXT_EVENT,
        DATA_SOURCE_SUNRISE_SUNSET,
        DATA_SOURCE_DAY_OF_WEEK,
        DATA_SOURCE_FAVORITE_CONTACT,
        DATA_SOURCE_DAY_AND_DATE
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    public annotation class DataSourceId
}
