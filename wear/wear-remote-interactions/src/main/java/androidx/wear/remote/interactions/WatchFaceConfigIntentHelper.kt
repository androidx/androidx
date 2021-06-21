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
package androidx.wear.remote.interactions

import android.content.ComponentName
import android.content.Intent
import androidx.annotation.NonNull
import androidx.annotation.Nullable

/**
 * Helper functions for use by watch face configuration activities. In general, there are two
 * distinct users:
 * * ones creating Intents
 * * ones receiving and responding to those Intents.
 *
 *
 * To register a configuration activity for a watch face, add a `<meta-data>` entry to the
 * watch face component in its Android Manifest file with an intent action to be fired to start the
 * activity. The following meta-data will register the `com.example.watchface.CONFIG_DIGITAL`
 * action to be started when configuring a watch face on the wearable device:
 * ```
 * <meta-data
 * android:name="com.google.android.wearable.watchface.wearableConfigurationAction"
 * android:value="com.example.watchface.CONFIG_DIGITAL" />
 * ```
 *
 *
 * To register a configuration activity to be started on a companion phone, add the following
 * alternative meta-data entry to the watch face component:
 * ```
 * <meta-data
 * android:name="com.google.android.wearable.watchface.companionConfigurationAction"
 * android:value="com.example.watchface.CONFIG_DIGITAL" />
 * ```
 *
 *
 * The activity should have an intent filter which lists the action specified in the meta-data
 * block above, in addition to the two categories present in the following example:
 * ```
 * <activity android:name=".MyWatchFaceConfigActivity">
 * <intent-filter>
 * <action android:name="com.example.watchface.CONFIG_DIGITAL" />
 * <category android:name=
 * "com.google.android.wearable.watchface.category.WEARABLE_CONFIGURATION" />
 * <category android:name="android.intent.category.DEFAULT" />
 * </intent-filter>
 * </activity>
 * ```
 *
 *
 * For phone side configuration activities, substitute the category
 * `com.google.android.wearable.watchface.category.COMPANION_CONFIGURATION` for
 * `com.google.android.wearable.watchface.category.WEARABLE_CONFIGURATION`.
 */
public class WatchFaceConfigIntentHelper private constructor() {
    public companion object {
        // The key for extra specifying a android.content.ComponentName of the watch face.
        private const val EXTRA_WATCH_FACE_COMPONENT: String =
            "android.support.wearable.watchface.extra.WATCH_FACE_COMPONENT"

        // The key for extra specifying the ID of the currently connected device in the phone-side
        // config.
        private const val EXTRA_PEER_ID: String = "android.support.wearable.watchface.extra.PEER_ID"

        /**
         * This method is for retrieving the extended data of watch face [ComponentName] from the
         * given [Intent]. [ComponentName] is being used to identify the APK and the class of the
         * watch face service.
         *
         * @param watchFaceIntent  The intent holding config activity launch.
         * @return the value of an item previously added with [putWatchFaceComponentExtra], or
         * null if no value was found.
         */
        @JvmStatic
        @Nullable
        public fun getWatchFaceComponentExtra(watchFaceIntent: Intent): ComponentName? =
            watchFaceIntent.getParcelableExtra(EXTRA_WATCH_FACE_COMPONENT)

        /**
         * This method is for adding the extended data of watch face [ComponentName] to the given
         * [Intent].
         *
         * @param watchFaceIntent The intent holding config activity launch.
         * @param componentName The component name of the watch face to be added as an extra.
         */
        @JvmStatic
        @NonNull
        public fun putWatchFaceComponentExtra(
            watchFaceIntent: Intent,
            componentName: ComponentName
        ): Intent = watchFaceIntent.putExtra(EXTRA_WATCH_FACE_COMPONENT, componentName)

        /**
         * This method is for retrieving the ID of the currently connected device from the given
         * [Intent].
         *
         * @param watchFaceIntent The intent holding config activity launch.
         * @return the value of an item previously added with [putPeerIdExtra], or null if no value
         *         was found.
         */
        @JvmStatic
        @Nullable
        public fun getPeerIdExtra(watchFaceIntent: Intent): String? =
            watchFaceIntent.getStringExtra(EXTRA_PEER_ID)

        /**
         * This method is adding the ID of the currently connected device to the given [Intent].
         *
         * @param watchFaceIntent The intent holding config activity launch.
         * @param peerId The string representing peer ID to be added as an extra.
         */
        @JvmStatic
        @NonNull
        public fun putPeerIdExtra(watchFaceIntent: Intent, peerId: String): Intent =
            watchFaceIntent.putExtra(EXTRA_PEER_ID, peerId)
    }
}