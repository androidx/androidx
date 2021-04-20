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


import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.wear.complications.data.ComplicationType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Methods to retrieve the component names for system complication providers. This will allow these
 * providers to be used as defaults by watch faces.
 */
public class SystemProviders {

    private SystemProviders() {}

    /**
     * System provider id as defined in {@link SystemProviders}.
     *
     * @hide
     */
    @IntDef({
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
            PROVIDER_MOST_RECENT_APP,
            PROVIDER_DAY_AND_DATE
    })
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProviderId {}

    /** Specifies that no provider should be used. */
    public static final int NO_PROVIDER = -1;

    /**
     * Id for the 'watch battery' complication provider.
     *
     * <p>This is a safe provider, so if a watch face uses this as a default it will be able to
     * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports the following types: {@link ComplicationType#MONOCHROMATIC_IMAGE},
     * {@link ComplicationType#SHORT_TEXT}, {@link ComplicationType#LONG_TEXT},
     * {@link ComplicationType#RANGED_VALUE}.
     */
    public static final int PROVIDER_WATCH_BATTERY = 1;

    /**
     * Id for the 'date' complication provider.
     *
     * <p>This is a safe provider, so if a watch face uses this as a default it will be able to
     * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports only {@link ComplicationType#SHORT_TEXT}.
     */
    public static final int PROVIDER_DATE = 2;

    /**
     * Id for the 'time and date' complication provider.
     *
     * <p>This is a safe provider, so if a watch face uses this as a default it will be able to
     * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports only {@link ComplicationType#SHORT_TEXT}.
     */
    public static final int PROVIDER_TIME_AND_DATE = 3;

    /**
     * Id for the 'step count' complication provider.
     *
     * <p>This is a safe provider (because it only shows a daily total), so if a watch face uses
     * this as a default it will be able to receive data from it even before the
     * RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports only {@link ComplicationType#SHORT_TEXT}.
     */
    public static final int PROVIDER_STEP_COUNT = 4;

    /**
     * Id for the 'world clock' complication provider.
     *
     * <p>This is a safe provider, so if a watch face uses this as a default it will be able to
     * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports only {@link ComplicationType#SHORT_TEXT}.
     */
    public static final int PROVIDER_WORLD_CLOCK = 5;

    /**
     * Id for the 'app shortcut' complication provider.
     *
     * <p>This is a safe provider, so if a watch face uses this as a default it will be able to
     * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports the following types: {@link ComplicationType#SMALL_IMAGE},
     * {@link ComplicationType#LONG_TEXT}.
     */
    public static final int PROVIDER_APP_SHORTCUT = 6;

    /**
     * Id for the 'unread notification count' complication provider.
     *
     * <p>This is a safe provider, so if a watch face uses this as a default it will be able to
     * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports the following types: {@link ComplicationType#MONOCHROMATIC_IMAGE},
     * {@link ComplicationType#SHORT_TEXT}.
     */
    public static final int PROVIDER_UNREAD_NOTIFICATION_COUNT = 7;

    /**
     * Id for the 'next event' complication provider.
     *
     * <p>This is not a safe provider, so if a watch face uses this as a default it will receive
     * data of TYPE_NO_PERMISSION until the user has granted the RECEIVE_COMPLICATION_DATA.
     *
     * <p>This provider supports the following types: {@link ComplicationType#SHORT_TEXT},
     * {@link ComplicationType#LONG_TEXT}.
     */
    public static final int PROVIDER_NEXT_EVENT = 9;

    /**
     * Id for the 'retail mode step count' complication provider.
     *
     * <p>This provider shows fake step count data, and the tap action launches the retail mode
     * health app. This provider should only be set as a default if the device is in retail mode.
     *
     * <p>This provider supports only {@link ComplicationType#SHORT_TEXT}.
     */
    public static final int PROVIDER_RETAIL_STEP_COUNT = 10;

    /**
     * Id for the 'retail mode chat' complication provider.
     *
     * <p>This provider shows fake 'unread chat messages' data, and the tap action launches the
     * retail mode chat app. This provider should only be set as a default if the device is in
     * retail mode.
     *
     * <p>This provider supports only {@link ComplicationType#SHORT_TEXT}.
     */
    public static final int PROVIDER_RETAIL_CHAT = 11;

    /**
     * Id for the 'sunrise sunset' complication provider.
     *
     * <p>This provider shows next sunrise or sunset time according to current timezone and
     * location.
     *
     * <p>This provider supports only {@link ComplicationType#SHORT_TEXT}.
     */
    public static final int PROVIDER_SUNRISE_SUNSET = 12;

    /**
     * Id for the 'day of week' complication provider.
     *
     * <p>This is a safe provider, so if a watch face uses this as a default it will be able to
     * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports only {@link ComplicationType#SHORT_TEXT}.
     */
    public static final int PROVIDER_DAY_OF_WEEK = 13;

    /**
     * Id for the 'favorite contact' complication provider.
     *
     * <p>This is not a safe provider, so if a watch face uses this as a default it will receive
     * data of TYPE_NO_PERMISSION until the user has granted the RECEIVE_COMPLICATION_DATA.
     *
     * <p>This provider supports only {@link ComplicationType#SMALL_IMAGE}.
     */
    public static final int PROVIDER_FAVORITE_CONTACT = 14;

    /**
     * Id for the 'most recent app' complication provider.
     *
     * <p>This is a safe provider, so if a watch face uses this as a default it will be able to
     * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports {@link ComplicationType#SMALL_IMAGE}, {@link
     * ComplicationType#LONG_TEXT}.
     */
    public static final int PROVIDER_MOST_RECENT_APP = 15;

    /**
     * Id for the 'day and date' complication provider.
     *
     * <p>This is a safe provider, so if a watch face uses this as a default it will be able to
     * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports only {@link ComplicationType#SHORT_TEXT}.
     */
    public static final int PROVIDER_DAY_AND_DATE = 16;
}
