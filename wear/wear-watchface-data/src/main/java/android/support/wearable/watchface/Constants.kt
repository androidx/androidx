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

package android.support.wearable.watchface

import android.app.WallpaperManager
import androidx.annotation.RestrictTo

/**
 * Shared constants between client and implementation.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class Constants {
    // Not instantiable.
    private constructor()

    companion object {
        // Keys for [ContentDescriptionLabel].
        const val KEY_BOUNDS = "KEY_BOUNDS"
        const val KEY_TEXT = "KEY_TEXT"

        @SuppressWarnings("IntentName")
        const val KEY_TAP_ACTION = "KEY_TAP_ACTION"

        // Keys for [WatchFaceStyle].
        const val KEY_COMPONENT = "component"
        const val KEY_VIEW_PROTECTION_MODE = "viewProtectionMode"
        const val KEY_STATUS_BAR_GRAVITY = "statusBarGravity"
        const val KEY_ACCENT_COLOR = "accentColor"
        const val KEY_SHOW_UNREAD_INDICATOR = "showUnreadIndicator"
        const val KEY_HIDE_NOTIFICATION_INDICATOR = "hideNotificationIndicator"
        const val KEY_ACCEPTS_TAPS = "acceptsTapEvents"

        /** Key used for top level complications item. */
        const val KEY_COMPLICATIONS_SETTINGS = "key_complications_settings"

        /** Key used for top level background image item. */
        const val KEY_BACKGROUND_IMAGE_SETTINGS = "key_background_image_settings"

        const val KEY_SCREENSHOT = "KEY_SCREENSHOT"

        /** Used to identify our provider chooser requests. */
        const val PROVIDER_CHOOSER_REQUEST_CODE = 1

        @SuppressWarnings("ActionValue")
        const val ACTION_REQUEST_STATE =
            "com.google.android.wearable.watchfaces.action.REQUEST_STATE"

        // Various wallpaper commands.
        @SuppressWarnings("IntentName")
        const val COMMAND_AMBIENT_UPDATE = "com.google.android.wearable.action.AMBIENT_UPDATE"

        @SuppressWarnings("IntentName")
        const val COMMAND_BACKGROUND_ACTION = "com.google.android.wearable.action.BACKGROUND_ACTION"

        @SuppressWarnings("IntentName")
        const val COMMAND_COMPLICATION_DATA = "com.google.android.wearable.action.COMPLICATION_DATA"

        @SuppressWarnings("IntentName")
        const val COMMAND_REQUEST_STYLE = "com.google.android.wearable.action.REQUEST_STYLE"

        // NB this is not currently deprecated.
        @SuppressWarnings("IntentName")
        const val COMMAND_SET_BINDER = "com.google.android.wearable.action.SET_BINDER"

        @SuppressWarnings("IntentName")
        const val COMMAND_SET_PROPERTIES = "com.google.android.wearable.action.SET_PROPERTIES"

        const val COMMAND_TAP = WallpaperManager.COMMAND_TAP
        const val COMMAND_TOUCH = "android.wallpaper.touch"
        const val COMMAND_TOUCH_CANCEL = "android.wallpaper.touch_cancel"

        // Various binder extras.
        @SuppressWarnings("ActionValue")
        const val EXTRA_BINDER = "binder"

        @SuppressWarnings("ActionValue")
        const val EXTRA_AMBIENT_MODE = "ambient_mode"

        @SuppressWarnings("ActionValue")
        const val EXTRA_CALENDAR_TIME_MS = "EXTRA_CALENDAR_TIME_MS"

        @SuppressWarnings("ActionValue")
        const val EXTRA_COMPLICATION_ID = "complication_id"

        @SuppressWarnings("ActionValue")
        const val EXTRA_COMPLICATION_DATA = "complication_data"

        @SuppressWarnings("ActionValue")
        const val EXTRA_DRAW_MODE = "EXTRA_DRAW_MODE"

        @SuppressWarnings("ActionValue")
        const val EXTRA_INDICATOR_STATUS = "indicator_status"

        @SuppressWarnings("ActionValue")
        const val EXTRA_INTERRUPTION_FILTER = "interruption_filter"

        @SuppressWarnings("ActionValue")
        const val EXTRA_NOTIFICATION_COUNT = "notification_count"

        @SuppressWarnings("ActionValue")
        const val EXTRA_UNREAD_COUNT = "unread_count"

        @SuppressWarnings("ActionValue")
        const val EXTRA_WATCH_FACE_COMPONENT =
            "android.support.wearable.watchface.extra.WATCH_FACE_COMPONENT"

        @SuppressWarnings("ActionValue")
        const val EXTRA_WATCH_FACE_VISIBLE = "watch_face_visible"

        @SuppressWarnings("ActionValue")
        const val EXTRA_WATCH_FACE_COMMAND_BINDER = "watch_face_command_binder"

        /**
         * Property in bundle passed to [Engine.onPropertiesChanged] to indicate whether burn-in
         * protection is required. When this property is set to true, views are shifted around
         * periodically in ambient mode. To ensure that content isn't shifted off the screen, watch
         * faces should avoid placing content within 10 pixels of the edge of the screen. Watch faces
         * should also avoid solid white areas to prevent pixel burn-in. Both of these requirements only
         * apply in ambient mode, and only when this property is set to true.
         */
        const val PROPERTY_BURN_IN_PROTECTION = "burn_in_protection"

        /**
         * Property in bundle passed to [Engine.onPropertiesChanged] to indicate whether the
         * device has low-bit ambient mode. When this property is set to true, the screen supports fewer
         * bits for each color in ambient mode. In this case, watch faces should disable anti-aliasing
         * in ambient mode.
         */
        const val PROPERTY_LOW_BIT_AMBIENT = "low_bit_ambient"

        /**
         * Key for a boolean value in the bundle passed to [Engine.onStatusChanged(Bundle)] that
         * indicates whether or not the device is charging. This will be true if the current battery
         * status is either [BatteryManager.BATTERY_STATUS_CHARGING] or
         * [BatteryManager.BATTERY_STATUS_FULL].
         */
        const val STATUS_CHARGING = "charging"

        /**
         * Key for a boolean value in the bundle passed to [Engine.onStatusChanged(Bundle)] that
         * indicates whether or not the device is in airplane mode.
         */
        const val STATUS_AIRPLANE_MODE = "airplane_mode"

        /**
         * Key for a boolean value in the bundle passed to [Engine.onStatusChanged(Bundle)] that
         * indicates whether or not the device is connected to the phone.
         */
        const val STATUS_CONNECTED = "connected"

        /**
         * Key for a boolean value in the bundle passed to [Engine.onStatusChanged(Bundle)] that
         * indicates whether or not the device is in theater mode.
         */
        const val STATUS_THEATER_MODE = "theater_mode"

        /**
         * Key for a boolean value in the bundle passed to [Engine.onStatusChanged(Bundle)] that
         * indicates whether or not GPS is enabled.
         */
        const val STATUS_GPS_ACTIVE = "gps_active"

        /**
         * Key for a boolean value in the bundle passed to [Engine.onStatusChanged(Bundle)] that
         * indicates whether or not the keyguard is locked.
         */
        const val STATUS_KEYGUARD_LOCKED = "keyguard_locked"
    }
}
