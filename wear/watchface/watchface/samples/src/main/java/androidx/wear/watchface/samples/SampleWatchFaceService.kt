/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.watchface.samples

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.wear.watchface.WatchFaceService

/**
 * Common base class for the androidx sample watch faces.
 *
 * You can trigger these to quit with: `adb shell am broadcast -a
 * androidx.wear.watchface.samples.QUIT`
 */
abstract class SampleWatchFaceService : WatchFaceService() {
    override fun onCreate() {
        super.onCreate()

        registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    Log.d(TAG, "androidx.wear.watchface.samples.QUIT received, quitting.")
                    System.exit(0)
                }
            },
            IntentFilter("androidx.wear.watchface.samples.QUIT"),
            Context.RECEIVER_EXPORTED
        )
    }

    private companion object {
        const val TAG = "SampleWatchFaceService"
    }
}
