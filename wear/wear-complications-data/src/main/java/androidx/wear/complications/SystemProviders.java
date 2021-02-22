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

import android.support.wearable.complications.ComplicationData;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

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
        WATCH_BATTERY,
        DATE,
        TIME_AND_DATE,
        STEP_COUNT,
        WORLD_CLOCK,
        APP_SHORTCUT,
        UNREAD_NOTIFICATION_COUNT,
        GOOGLE_PAY,
        NEXT_EVENT,
        RETAIL_STEP_COUNT,
        RETAIL_CHAT,
        SUNRISE_SUNSET,
        DAY_OF_WEEK,
        FAVORITE_CONTACT,
        MOST_RECENT_APP,
        DAY_AND_DATE
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
     * <p>This provider supports the following types: {@link ComplicationData#TYPE_ICON TYPE_ICON},
     * {@link ComplicationData#TYPE_SHORT_TEXT TYPE_SHORT_TEXT}, {@link
     * ComplicationData#TYPE_LONG_TEXT TYPE_LONG_TEXT}, {@link ComplicationData#TYPE_RANGED_VALUE
     * TYPE_RANGED_VALUE}.
     */
    public static final int WATCH_BATTERY = 1;

    /**
     * Id for the 'date' complication provider.
     *
     * <p>This is a safe provider, so if a watch face uses this as a default it will be able to
     * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports only {@link ComplicationData#TYPE_SHORT_TEXT TYPE_SHORT_TEXT}.
     */
    public static final int DATE = 2;

    /**
     * Id for the 'time and date' complication provider.
     *
     * <p>This is a safe provider, so if a watch face uses this as a default it will be able to
     * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports only {@link ComplicationData#TYPE_SHORT_TEXT TYPE_SHORT_TEXT}.
     */
    public static final int TIME_AND_DATE = 3;

    /**
     * Id for the 'step count' complication provider.
     *
     * <p>This is a safe provider (because it only shows a daily total), so if a watch face uses
     * this as a default it will be able to receive data from it even before the
     * RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports only {@link ComplicationData#TYPE_SHORT_TEXT TYPE_SHORT_TEXT}.
     */
    public static final int STEP_COUNT = 4;

    /**
     * Id for the 'world clock' complication provider.
     *
     * <p>This is a safe provider, so if a watch face uses this as a default it will be able to
     * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports only {@link ComplicationData#TYPE_SHORT_TEXT TYPE_SHORT_TEXT}.
     */
    public static final int WORLD_CLOCK = 5;

    /**
     * Id for the 'app shortcut' complication provider.
     *
     * <p>This is a safe provider, so if a watch face uses this as a default it will be able to
     * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports the following types: {@link ComplicationData#TYPE_SMALL_IMAGE
     * TYPE_SMALL_IMAGE}, {@link ComplicationData#TYPE_LONG_TEXT TYPE_LONG_TEXT}.
     */
    public static final int APP_SHORTCUT = 6;

    /**
     * Id for the 'unread notification count' complication provider.
     *
     * <p>This is a safe provider, so if a watch face uses this as a default it will be able to
     * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports the following types: {@link ComplicationData#TYPE_ICON TYPE_ICON},
     * {@link ComplicationData#TYPE_SHORT_TEXT TYPE_SHORT_TEXT}.
     */
    public static final int UNREAD_NOTIFICATION_COUNT = 7;

    /**
     * Id for the Google Pay complication provider.
     *
     * <p>This is a safe provider (because it only launches the Google Pay app), so if a watch face
     * uses this as a default it will be able to receive data from it even before the
     * RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports only {@link ComplicationData#TYPE_SMALL_IMAGE TYPE_SMALL_IMAGE}.
     */
    @SuppressWarnings("MentionsGoogle") // This is not an api service.
    public static final int GOOGLE_PAY = 8;

    /**
     * Id for the 'next event' complication provider.
     *
     * <p>This is not a safe provider, so if a watch face uses this as a default it will receive
     * data of TYPE_NO_PERMISSION until the user has granted the RECEIVE_COMPLICATION_DATA.
     *
     * <p>This provider supports the following types: {@link ComplicationData#TYPE_SHORT_TEXT
     * TYPE_SHORT_TEXT}, {@link ComplicationData#TYPE_LONG_TEXT TYPE_LONG_TEXT}.
     */
    public static final int NEXT_EVENT = 9;

    /**
     * Id for the 'retail mode step count' complication provider.
     *
     * <p>This provider shows fake step count data, and the tap action launches the retail mode
     * health app. This provider should only be set as a default if the device is in retail mode.
     *
     * <p>This provider supports only {@link ComplicationData#TYPE_SHORT_TEXT TYPE_SHORT_TEXT}.
     */
    public static final int RETAIL_STEP_COUNT = 10;

    /**
     * Id for the 'retail mode chat' complication provider.
     *
     * <p>This provider shows fake 'unread chat messages' data, and the tap action launches the
     * retail mode chat app. This provider should only be set as a default if the device is in
     * retail mode.
     *
     * <p>This provider supports only {@link ComplicationData#TYPE_SHORT_TEXT TYPE_SHORT_TEXT}.
     */
    public static final int RETAIL_CHAT = 11;

    /**
     * Id for the 'sunrise sunset' complication provider.
     *
     * <p>This provider shows next sunrise or sunset time according to current timezone and
     * location.
     *
     * <p>This provider supports only {@link ComplicationData#TYPE_SHORT_TEXT TYPE_SHORT_TEXT}.
     */
    public static final int SUNRISE_SUNSET = 12;

    /**
     * Id for the 'day of week' complication provider.
     *
     * <p>This is a safe provider, so if a watch face uses this as a default it will be able to
     * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports only {@link ComplicationData#TYPE_SHORT_TEXT TYPE_SHORT_TEXT}.
     */
    public static final int DAY_OF_WEEK = 13;

    /**
     * Id for the 'favorite contact' complication provider.
     *
     * <p>This is not a safe provider, so if a watch face uses this as a default it will receive
     * data of TYPE_NO_PERMISSION until the user has granted the RECEIVE_COMPLICATION_DATA.
     *
     * <p>This provider supports only {@link ComplicationData#TYPE_SMALL_IMAGE TYPE_SMALL_IMAGE}.
     */
    public static final int FAVORITE_CONTACT = 14;

    /**
     * Id for the 'most recent app' complication provider.
     *
     * <p>This is a safe provider, so if a watch face uses this as a default it will be able to
     * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports {@link ComplicationData#TYPE_SMALL_IMAGE TYPE_SMALL_IMAGE}, {@link
     * ComplicationData#TYPE_LONG_TEXT TYPE_LONG_TEXT}.
     */
    public static final int MOST_RECENT_APP = 15;

    /**
     * Id for the 'day and date' complication provider.
     *
     * <p>This is a safe provider, so if a watch face uses this as a default it will be able to
     * receive data from it even before the RECEIVE_COMPLICATION_DATA permission has been granted.
     *
     * <p>This provider supports only {@link ComplicationData#TYPE_SHORT_TEXT TYPE_SHORT_TEXT}.
     */
    public static final int DAY_AND_DATE = 16;
}
