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
package androidx.wear.phone.interactions

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.IntDef

/** Provides helper methods for determining the type of phone we are paired to. */
public class PhoneTypeHelper private constructor() {
    public companion object {
        internal const val BLUETOOTH_MODE = "bluetooth_mode"
        internal const val PAIRED_DEVICE_OS_TYPE = "paired_device_os_type"
        internal const val SETTINGS_AUTHORITY = "com.google.android.wearable.settings"
        private val BLUETOOTH_MODE_URI =
            Uri.Builder()
                .scheme("content")
                .authority(SETTINGS_AUTHORITY)
                .path(BLUETOOTH_MODE)
                .build()

        /**
         * These values follow the values of platform constants defined in
         * [Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE].
         */
        internal const val UNKNOWN_MODE = 0
        internal const val ANDROID_MODE = 1
        internal const val IOS_MODE = 2
        internal const val NONE_PAIRED_MODE = 3

        /** Indicates an error returned retrieving the type of phone we are paired to. */
        public const val DEVICE_TYPE_ERROR: Int = 0

        /** Indicates that we are paired to an Android phone. */
        public const val DEVICE_TYPE_ANDROID: Int = 1

        /** Indicates that we are paired to an iOS phone. */
        public const val DEVICE_TYPE_IOS: Int = 2

        /** Indicates unknown type of phone we are paired to. */
        public const val DEVICE_TYPE_UNKNOWN: Int = 3

        /** Indicates that device is not paired to phone. */
        public const val DEVICE_TYPE_NONE: Int = 4

        /**
         * Returns the type of phone handset this Wear OS device has been paired with.
         *
         * @return one of `DEVICE_TYPE_ERROR`, `DEVICE_TYPE_ANDROID`, `DEVICE_TYPE_IOS`,
         *   `DEVICE_TYPE_UNKNOWN` or `DEVICE_TYPE_NONE` indicating we had an error while
         *   determining the phone type, we are paired to an Android phone, we are paired to an iOS
         *   phone, we could not determine the phone type respectively, or no phone is paired
         *   respectively.
         */
        @DeviceFamily
        @JvmStatic
        public fun getPhoneDeviceType(context: Context): Int {
            var pairedDeviceType = UNKNOWN_MODE
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                val cursor =
                    context.contentResolver.query(BLUETOOTH_MODE_URI, null, null, null, null)
                        ?: return DEVICE_TYPE_ERROR
                cursor.use {
                    while (it.moveToNext()) {
                        if (BLUETOOTH_MODE == it.getString(0)) {
                            pairedDeviceType = it.getInt(1)
                            break
                        }
                    }
                }
                return when (pairedDeviceType) {
                    ANDROID_MODE -> DEVICE_TYPE_ANDROID
                    IOS_MODE -> DEVICE_TYPE_IOS
                    else -> DEVICE_TYPE_UNKNOWN
                }
            } else if (
                Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    context.applicationInfo.targetSdkVersion > Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            ) {
                return DEVICE_TYPE_ANDROID
            }
            pairedDeviceType =
                Settings.Global.getInt(context.contentResolver, PAIRED_DEVICE_OS_TYPE, UNKNOWN_MODE)
            return when (pairedDeviceType) {
                ANDROID_MODE -> DEVICE_TYPE_ANDROID
                IOS_MODE -> DEVICE_TYPE_IOS
                NONE_PAIRED_MODE -> DEVICE_TYPE_NONE
                else -> DEVICE_TYPE_UNKNOWN
            }
        }

        /** Annotates a value of DeviceType. */
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(
            DEVICE_TYPE_ERROR,
            DEVICE_TYPE_ANDROID,
            DEVICE_TYPE_IOS,
            DEVICE_TYPE_UNKNOWN,
            DEVICE_TYPE_NONE,
        )
        internal annotation class DeviceFamily
    }
}
