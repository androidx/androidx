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
import android.os.BatteryManager.BATTERY_STATUS_CHARGING
import android.os.BatteryManager.BATTERY_STATUS_FULL
import androidx.annotation.RestrictTo
import androidx.work.Logger
import androidx.work.impl.utils.taskexecutor.TaskExecutor

/** Tracks whether or not the device's battery is charging. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BatteryChargingTracker(context: Context, taskExecutor: TaskExecutor) :
    BroadcastReceiverConstraintTracker<Boolean>(context, taskExecutor) {

    override fun readSystemState(): Boolean {
        // {@link ACTION_CHARGING} and {@link ACTION_DISCHARGING} are not sticky broadcasts, so
        // we use {@link ACTION_BATTERY_CHANGED} on all APIs to get the initial state.
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = appContext.registerReceiver(null, intentFilter)
        if (intent == null) {
            Logger.get().error(TAG, "getInitialState - null intent received")
            return false
        }
        return isBatteryChangedIntentCharging(intent)
    }

    override val intentFilter: IntentFilter
        get() {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BatteryManager.ACTION_CHARGING)
            intentFilter.addAction(BatteryManager.ACTION_DISCHARGING)
            return intentFilter
        }

    override fun onBroadcastReceive(intent: Intent) {
        val action = intent.action ?: return
        Logger.get().debug(TAG, "Received $action")
        when (action) {
            BatteryManager.ACTION_CHARGING -> state = true
            BatteryManager.ACTION_DISCHARGING -> state = false
            Intent.ACTION_POWER_CONNECTED -> state = true
            Intent.ACTION_POWER_DISCONNECTED -> state = false
        }
    }

    private fun isBatteryChangedIntentCharging(intent: Intent): Boolean {
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return (status == BATTERY_STATUS_CHARGING || status == BATTERY_STATUS_FULL)
    }
}

private val TAG = Logger.tagWithPrefix("BatteryChrgTracker")
