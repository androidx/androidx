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
package androidx.wear.remote.interactions

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight

/**
 * Helper class for checking whether the phone paired to a given Wear OS device has the Play Store.
 */
@RequiresApi(Build.VERSION_CODES.N)
public class PlayStoreAvailability private constructor() {
    public companion object {
        /**
         * This value means that there was an error in checking for whether the Play Store is
         * available son the phone.
         */
        public const val PLAY_STORE_ERROR_UNKNOWN: Int = 0

        /** This value means that the Play Store is available on the phone.  */
        public const val PLAY_STORE_AVAILABLE: Int = 1

        /** This value means that the Play Store is not available on the phone.  */
        public const val PLAY_STORE_UNAVAILABLE: Int = 2

        private const val PLAY_STORE_AVAILABILITY_PATH = "play_store_availability"
        internal const val SETTINGS_AUTHORITY_URI = "com.google.android.wearable.settings"
        internal val PLAY_STORE_AVAILABILITY_URI = Uri.Builder()
            .scheme("content")
            .authority(SETTINGS_AUTHORITY_URI)
            .path(PLAY_STORE_AVAILABILITY_PATH)
            .build()

        // The name of the row which stores the play store availability setting in versions before
        // R.
        internal const val KEY_PLAY_STORE_AVAILABILITY = "play_store_availability"

        // The name of the settings value which stores the play store availability setting in
        // versions from R.
        private const val SETTINGS_PLAY_STORE_AVAILABILITY = "phone_play_store_availability"

        internal const val SYSTEM_FEATURE_WATCH: String = "android.hardware.type.watch"

        /**
         * Returns whether the Play Store is available on the Phone. If
         * [PLAY_STORE_ERROR_UNKNOWN] is returned, the caller should try again later. This
         * method should not be run on the main thread.
         *
         * @return One of three values: [PLAY_STORE_AVAILABLE],
         * [PLAY_STORE_UNAVAILABLE], or [PLAY_STORE_ERROR_UNKNOWN].
         */
        @JvmStatic
        @PlayStoreStatus
        public fun getPlayStoreAvailabilityOnPhone(context: Context): Int {
            val isCurrentDeviceAWatch = context.packageManager.hasSystemFeature(
                SYSTEM_FEATURE_WATCH
            )

            if (!isCurrentDeviceAWatch) {
                val isPlayServiceAvailable =
                    GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context)
                return if (isPlayServiceAvailable == ConnectionResult.SUCCESS) PLAY_STORE_AVAILABLE
                else PLAY_STORE_UNAVAILABLE
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                context.contentResolver.query(
                    PLAY_STORE_AVAILABILITY_URI, null, null, null,
                    null
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        if (KEY_PLAY_STORE_AVAILABILITY == cursor.getString(0)) {
                            return cursor.getInt(1)
                        }
                    }
                }
            } else {
                return Settings.Global.getInt(
                    context.contentResolver, SETTINGS_PLAY_STORE_AVAILABILITY,
                    PLAY_STORE_ERROR_UNKNOWN
                )
            }
            return PLAY_STORE_ERROR_UNKNOWN
        }

        /** @hide */
        @IntDef(
            PLAY_STORE_ERROR_UNKNOWN,
            PLAY_STORE_AVAILABLE,
            PLAY_STORE_UNAVAILABLE
        )
        @Retention(AnnotationRetention.SOURCE)
        public annotation class PlayStoreStatus
    }
}