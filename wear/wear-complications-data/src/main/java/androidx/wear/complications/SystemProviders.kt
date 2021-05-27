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
package androidx.wear.complications

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.wear.complications.data.ComplicationType

/**
 * Methods to retrieve the component names for system complication providers. This will allow these
 * providers to be used as defaults by watch faces.
 */
public class SystemProviders private constructor() {
    public companion object {
        /** Specifies that no provider should be used.  */
        public const val NO_PROVIDER: Int = -1

        /**
         * Id for the 'watch battery' complication provider.
         *
         * This is a safe provider, so if a watch face uses this as a default it will be able to
         * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been
         * granted.
         *
         * This provider supports the following types: [ComplicationType.MONOCHROMATIC_IMAGE],
         * [ComplicationType.SHORT_TEXT], [ComplicationType.LONG_TEXT],
         * [ComplicationType.RANGED_VALUE].
         */
        public const val PROVIDER_WATCH_BATTERY: Int = 1

        /**
         * Id for the 'date' complication provider.
         *
         * This is a safe provider, so if a watch face uses this as a default it will be able to
         * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been
         * granted.
         *
         * This provider supports only [ComplicationType.SHORT_TEXT].
         */
        public const val PROVIDER_DATE: Int = 2

        /**
         * Id for the 'time and date' complication provider.
         *
         * This is a safe provider, so if a watch face uses this as a default it will be able to
         * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been
         * granted.
         *
         * This provider supports only [ComplicationType.SHORT_TEXT].
         */
        public const val PROVIDER_TIME_AND_DATE: Int = 3

        /**
         * Id for the 'step count' complication provider.
         *
         * This is a safe provider (because it only shows a daily total), so if a watch face uses
         * this as a default it will be able to receive data from it even before the
         * RECEIVE_COMPLICATION_DATA permission has been granted.
         *
         * This provider supports only [ComplicationType.SHORT_TEXT].
         */
        public const val PROVIDER_STEP_COUNT: Int = 4

        /**
         * Id for the 'world clock' complication provider.
         *
         * This is a safe provider, so if a watch face uses this as a default it will be able to
         * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been
         * granted.
         *
         * This provider supports only [ComplicationType.SHORT_TEXT].
         */
        public const val PROVIDER_WORLD_CLOCK: Int = 5

        /**
         * Id for the 'app shortcut' complication provider.
         *
         * This is a safe provider, so if a watch face uses this as a default it will be able to
         * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been
         * granted.
         *
         * This provider supports the following types: [ComplicationType.SMALL_IMAGE],
         * [ComplicationType.LONG_TEXT].
         */
        public const val PROVIDER_APP_SHORTCUT: Int = 6

        /**
         * Id for the 'unread notification count' complication provider.
         *
         * This is a safe provider, so if a watch face uses this as a default it will be able to
         * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been
         * granted.
         *
         * This provider supports the following types: [ComplicationType.MONOCHROMATIC_IMAGE],
         * [ComplicationType.SHORT_TEXT].
         */
        public const val PROVIDER_UNREAD_NOTIFICATION_COUNT: Int = 7

        /**
         * Id for the 'next event' complication provider.
         *
         * This is not a safe provider, so if a watch face uses this as a default it will receive
         * data of TYPE_NO_PERMISSION until the user has granted the RECEIVE_COMPLICATION_DATA.
         *
         * This provider supports the following types: [ComplicationType.SHORT_TEXT],
         * [ComplicationType.LONG_TEXT].
         */
        public const val PROVIDER_NEXT_EVENT: Int = 9

        /**
         * Id for the 'retail mode step count' complication provider.
         *
         * This provider shows fake step count data, and the tap action launches the retail mode
         * health app. This provider should only be set as a default if the device is in retail
         * mode.
         *
         * This provider supports only [ComplicationType.SHORT_TEXT].
         */
        public const val PROVIDER_RETAIL_STEP_COUNT: Int = 10

        /**
         * Id for the 'retail mode chat' complication provider.
         *
         * This provider shows fake 'unread chat messages' data, and the tap action launches the
         * retail mode chat app. This provider should only be set as a default if the device is in
         * retail mode.
         *
         * This provider supports only [ComplicationType.SHORT_TEXT].
         */
        public const val PROVIDER_RETAIL_CHAT: Int = 11

        /**
         * Id for the 'sunrise sunset' complication provider.
         *
         * This provider shows next sunrise or sunset time according to current timezone and
         * location.
         *
         * This provider supports only [ComplicationType.SHORT_TEXT].
         */
        public const val PROVIDER_SUNRISE_SUNSET: Int = 12

        /**
         * Id for the 'day of week' complication provider.
         *
         * This is a safe provider, so if a watch face uses this as a default it will be able to
         * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been
         * granted.
         *
         * This provider supports only [ComplicationType.SHORT_TEXT].
         */
        public const val PROVIDER_DAY_OF_WEEK: Int = 13

        /**
         * Id for the 'favorite contact' complication provider.
         *
         * This is not a safe provider, so if a watch face uses this as a default it will receive
         * data of TYPE_NO_PERMISSION until the user has granted the RECEIVE_COMPLICATION_DATA.
         *
         * This provider supports only [ComplicationType.SMALL_IMAGE].
         */
        public const val PROVIDER_FAVORITE_CONTACT: Int = 14

        /**
         * Id for the 'day and date' complication provider.
         *
         * This is a safe provider, so if a watch face uses this as a default it will be able to
         * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been
         * granted.
         *
         * This provider supports only [ComplicationType.SHORT_TEXT].
         */
        public const val PROVIDER_DAY_AND_DATE: Int = 16
    }

    /**
     * System provider id as defined in [SystemProviders].
     *
     * @hide
     */
    @IntDef(
        NO_PROVIDER,
        PROVIDER_WATCH_BATTERY,
        PROVIDER_DATE,
        PROVIDER_TIME_AND_DATE,
        PROVIDER_STEP_COUNT,
        PROVIDER_WORLD_CLOCK,
        PROVIDER_APP_SHORTCUT,
        PROVIDER_UNREAD_NOTIFICATION_COUNT,
        PROVIDER_NEXT_EVENT,
        PROVIDER_RETAIL_STEP_COUNT,
        PROVIDER_RETAIL_CHAT,
        PROVIDER_SUNRISE_SUNSET,
        PROVIDER_DAY_OF_WEEK,
        PROVIDER_FAVORITE_CONTACT,
        PROVIDER_DAY_AND_DATE
    )
    @RestrictTo(
        RestrictTo.Scope.LIBRARY_GROUP
    )
    @Retention(AnnotationRetention.SOURCE)
    public annotation class ProviderId
}