/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.work.impl.constraints.trackers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.annotation.RestrictTo
import androidx.work.Logger
import androidx.work.impl.utils.taskexecutor.TaskExecutor

/**
 * Tracks whether or not the device's battery level is low.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BatteryNotLowTracker(context: Context, taskExecutor: TaskExecutor) :
    BroadcastReceiverConstraintTracker<Boolean>(context, taskExecutor) {
    /**
     * Based on BatteryService#shouldSendBatteryLowLocked(), but this ignores the previous plugged
     * state - cannot guarantee the last plugged state because this isn't always tracking.
     *
     * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/services/core/java/com/android/server/BatteryService.java#268}
     */
    override fun readSystemState(): Boolean {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = appContext.registerReceiver(null, intentFilter)
            if (intent == null) {
                Logger.get().error(TAG, "getInitialState - null intent received")
                return false
            }
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPercentage = level / scale.toFloat()

            // BATTERY_STATUS_UNKNOWN typically refers to devices without a battery.
            // So those kinds of devices must be allowed.
            return status == BatteryManager.BATTERY_STATUS_UNKNOWN ||
                batteryPercentage > BATTERY_LOW_THRESHOLD
        }

    override val intentFilter: IntentFilter
        get() {
            val intentFilter = IntentFilter()
            intentFilter.addAction(Intent.ACTION_BATTERY_OKAY)
            intentFilter.addAction(Intent.ACTION_BATTERY_LOW)
            return intentFilter
        }

    override fun onBroadcastReceive(intent: Intent) {
        if (intent.action == null) {
            return
        }
        Logger.get().debug(TAG, "Received ${intent.action}")
        when (intent.action) {
            Intent.ACTION_BATTERY_OKAY -> state = true
            Intent.ACTION_BATTERY_LOW -> state = false
        }
    }
}

private val TAG = Logger.tagWithPrefix("BatteryNotLowTracker")

/**
 * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/core/res/res/values/config.xml#986}
 */
internal const val BATTERY_LOW_THRESHOLD = 0.15f
