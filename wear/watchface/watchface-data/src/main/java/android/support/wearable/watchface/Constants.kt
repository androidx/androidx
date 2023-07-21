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

/** Shared constants between client and implementation. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Constants {
    // Not instantiable.
    private constructor()

    public companion object {
        // Keys for [ContentDescriptionLabel].
        public const val KEY_BOUNDS: String = "KEY_BOUNDS"
        public const val KEY_TEXT: String = "KEY_TEXT"

        @SuppressWarnings("IntentName") public const val KEY_TAP_ACTION: String = "KEY_TAP_ACTION"

        // Keys for [WatchFaceStyle].
        public const val KEY_COMPONENT: String = "component"
        public const val KEY_VIEW_PROTECTION_MODE: String = "viewProtectionMode"
        public const val KEY_STATUS_BAR_GRAVITY: String = "statusBarGravity"
        public const val KEY_ACCENT_COLOR: String = "accentColor"
        public const val KEY_SHOW_UNREAD_INDICATOR: String = "showUnreadIndicator"
        public const val KEY_HIDE_NOTIFICATION_INDICATOR: String = "hideNotificationIndicator"
        public const val KEY_ACCEPTS_TAPS: String = "acceptsTapEvents"

        /** Key used for top level complications item. */
        public const val KEY_COMPLICATIONS_SETTINGS: String = "key_complications_settings"

        /** Key used for top level background image item. */
        public const val KEY_BACKGROUND_IMAGE_SETTINGS: String = "key_background_image_settings"

        public const val KEY_BITMAP_WIDTH_PX: String = "KEY_BITMAP_WIDTH_PX"
        public const val KEY_BITMAP_HEIGHT_PX: String = "KEY_BITMAP_HEIGHT_PX"
        public const val KEY_BITMAP_CONFIG_ORDINAL: String = "KEY_BITMAP_CONFIG_ORDINAL"
        public const val KEY_SCREENSHOT: String = "KEY_SCREENSHOT"

        public const val PERMISSION_BIND_WATCH_FACE_CONTROL: String =
            "com.google.android.wearable.permission.BIND_WATCH_FACE_CONTROL"

        public const val ACTION_WATCH_FACE_REFRESH_A11Y_LABELS: String =
            "androidx.watchface.action.WATCH_FACE_A11Y_LABELS_REFRESH"

        @SuppressWarnings("ActionValue")
        public const val ACTION_REQUEST_STATE: String =
            "com.google.android.wearable.watchfaces.action.REQUEST_STATE"

        // Various wallpaper commands.
        @SuppressWarnings("IntentName")
        public const val COMMAND_AMBIENT_UPDATE: String =
            "com.google.android.wearable.action.AMBIENT_UPDATE"

        @SuppressWarnings("IntentName")
        public const val COMMAND_BACKGROUND_ACTION: String =
            "com.google.android.wearable.action.BACKGROUND_ACTION"

        @SuppressWarnings("IntentName")
        public const val COMMAND_COMPLICATION_DATA: String =
            "com.google.android.wearable.action.COMPLICATION_DATA"

        @SuppressWarnings("IntentName")
        public const val COMMAND_REQUEST_STYLE: String =
            "com.google.android.wearable.action.REQUEST_STYLE"

        // NB this is not currently deprecated.
        @SuppressWarnings("IntentName")
        public const val COMMAND_SET_BINDER: String =
            "com.google.android.wearable.action.SET_BINDER"

        @SuppressWarnings("IntentName")
        public const val COMMAND_SET_PROPERTIES: String =
            "com.google.android.wearable.action.SET_PROPERTIES"

        public const val COMMAND_TAP: String = WallpaperManager.COMMAND_TAP
        public const val COMMAND_TOUCH: String = "android.wallpaper.touch"
        public const val COMMAND_TOUCH_CANCEL: String = "android.wallpaper.touch_cancel"

        // Various binder extras.
        @SuppressWarnings("ActionValue") public const val EXTRA_BINDER: String = "binder"

        @SuppressWarnings("ActionValue")
        public const val EXTRA_AMBIENT_MODE: String = "ambient_mode"

        @SuppressWarnings("ActionValue")
        public const val EXTRA_CALENDAR_TIME_MS: String = "EXTRA_CALENDAR_TIME_MS"

        @SuppressWarnings("ActionValue")
        public const val EXTRA_COMPLICATION_ID: String = "complication_id"

        @SuppressWarnings("ActionValue")
        public const val EXTRA_COMPLICATION_DATA: String = "complication_data"

        @SuppressWarnings("ActionValue")
        public const val EXTRA_DRAW_MODE: String = "EXTRA_DRAW_MODE"

        @SuppressWarnings("ActionValue")
        public const val EXTRA_INDICATOR_STATUS: String = "indicator_status"

        @SuppressWarnings("ActionValue")
        public const val EXTRA_INTERRUPTION_FILTER: String = "interruption_filter"

        @SuppressWarnings("ActionValue")
        public const val EXTRA_NOTIFICATION_COUNT: String = "notification_count"

        @SuppressWarnings("ActionValue")
        public const val EXTRA_UNREAD_COUNT: String = "unread_count"

        @SuppressWarnings("ActionValue")
        public const val EXTRA_WATCH_FACE_COMPONENT: String =
            "android.support.wearable.watchface.extra.WATCH_FACE_COMPONENT"

        @SuppressWarnings("ActionValue")
        public const val EXTRA_WATCH_FACE_VISIBLE: String = "watch_face_visible"

        /**
         * Property in bundle passed to [Engine.onPropertiesChanged] to indicate whether burn-in
         * protection is required. When this property is set to true, views are shifted around
         * periodically in ambient mode. To ensure that content isn't shifted off the screen, watch
         * faces should avoid placing content within 10 pixels of the edge of the screen. Watch
         * faces should also avoid solid white areas to prevent pixel burn-in. Both of these
         * requirements only apply in ambient mode, and only when this property is set to true.
         */
        public const val PROPERTY_BURN_IN_PROTECTION: String = "burn_in_protection"

        /**
         * Property in bundle passed to [Engine.onPropertiesChanged] to indicate whether the device
         * has low-bit ambient mode. When this property is set to true, the screen supports fewer
         * bits for each color in ambient mode. In this case, watch faces should disable
         * anti-aliasing in ambient mode.
         */
        public const val PROPERTY_LOW_BIT_AMBIENT: String = "low_bit_ambient"

        /**
         * Key for a boolean value in the bundle passed to [Engine.onStatusChanged(Bundle)] that
         * indicates whether or not the keyguard is locked.
         */
        public const val STATUS_KEYGUARD_LOCKED: String = "keyguard_locked"

        /**
         * Metadata flag specifying that the watch face is OK for the system to have multiple
         * instances of the watch face. Without this by default the system will only allow a single
         * instance.
         */
        public const val META_DATA_MULTIPLE_INSTANCES_ALLOWED: String =
            "androidx.wear.watchface.MULTIPLE_INSTANCES_ALLOWED"

        /**
         * Metadata flag indicating the watch face service exposes flavors. The system will access
         * them only if this flag is present in manifest.
         */
        public const val META_DATA_FLAVORS_SUPPORTED: String =
            "androidx.wear.watchface.FLAVORS_SUPPORTED"
    }
}
