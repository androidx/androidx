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

package androidx.wear.watchface

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.wear.watchface.BroadcastsReceiver.BroadcastEventObserver

/**
 * This class decouples [BroadcastEventObserver]s from the actual broadcast event receivers to make
 * testing easier.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BroadcastsReceiver constructor(
    private val context: Context,
    private val observer: BroadcastEventObserver
) {

    public interface BroadcastEventObserver {
        /** Called when we receive [Intent.ACTION_TIME_TICK]. */
        @UiThread
        public fun onActionTimeTick()

        /** Called when we receive [Intent.ACTION_TIMEZONE_CHANGED]. */
        @UiThread
        public fun onActionTimeZoneChanged()

        /** Called when we receive [Intent.ACTION_TIME_CHANGED]. */
        @UiThread
        public fun onActionTimeChanged()

        /** Called when we receive [Intent.ACTION_BATTERY_LOW]. */
        @UiThread
        public fun onActionBatteryLow()

        /** Called when we receive [Intent.ACTION_BATTERY_OKAY]. */
        @UiThread
        public fun onActionBatteryOkay()

        /** Called when we receive [Intent.ACTION_POWER_CONNECTED]. */
        @UiThread
        public fun onActionPowerConnected()

        /** Called when we receive [Intent.ACTION_POWER_DISCONNECTED]. */
        @UiThread
        public fun onActionPowerDisconnected()

        /** Called when we receive [WatchFaceImpl.MOCK_TIME_INTENT]. */
        @UiThread
        public fun onMockTime(intent: Intent)
    }

    companion object {
        // The threshold used to judge whether the battery is low during initialization.  Ideally
        // we would use the threshold for Intent.ACTION_BATTERY_LOW but it's not documented or
        // available programmatically. The value below is the default but it could be overridden
        // by OEMs.
        internal const val INITIAL_LOW_BATTERY_THRESHOLD = 15f
    }

    internal val actionTimeTickReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressWarnings("SyntheticAccessor")
        override fun onReceive(context: Context, intent: Intent) {
            observer.onActionTimeTick()
        }
    }

    internal val actionTimeZoneReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            observer.onActionTimeZoneChanged()
        }
    }

    internal val actionTimeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            observer.onActionTimeChanged()
        }
    }

    internal val actionBatteryLowReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressWarnings("SyntheticAccessor")
        override fun onReceive(context: Context, intent: Intent) {
            observer.onActionBatteryLow()
        }
    }

    internal val actionBatteryOkayReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressWarnings("SyntheticAccessor")
        override fun onReceive(context: Context, intent: Intent) {
            observer.onActionBatteryOkay()
        }
    }

    internal val actionPowerConnectedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressWarnings("SyntheticAccessor")
        override fun onReceive(context: Context, intent: Intent) {
            observer.onActionPowerConnected()
        }
    }

    internal val actionPowerDisconnectedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressWarnings("SyntheticAccessor")
        override fun onReceive(context: Context, intent: Intent) {
            observer.onActionPowerDisconnected()
        }
    }

    internal val mockTimeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressWarnings("SyntheticAccessor")
        override fun onReceive(context: Context, intent: Intent) {
            observer.onMockTime(intent)
        }
    }

    init {
        context.registerReceiver(actionTimeTickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
        context.registerReceiver(
            actionTimeZoneReceiver,
            IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
        )
        context.registerReceiver(actionTimeReceiver, IntentFilter(Intent.ACTION_TIME_CHANGED))
        context.registerReceiver(actionBatteryLowReceiver, IntentFilter(Intent.ACTION_BATTERY_LOW))
        context.registerReceiver(
            actionBatteryOkayReceiver,
            IntentFilter(Intent.ACTION_BATTERY_OKAY)
        )
        context.registerReceiver(
            actionPowerConnectedReceiver,
            IntentFilter(Intent.ACTION_POWER_CONNECTED)
        )
        context.registerReceiver(
            actionPowerDisconnectedReceiver,
            IntentFilter(Intent.ACTION_POWER_DISCONNECTED)
        )
        context.registerReceiver(mockTimeReceiver, IntentFilter(WatchFaceImpl.MOCK_TIME_INTENT))
    }

    /** Called to send observers initial battery state in advance of receiving any broadcasts. */
    internal fun processBatteryStatus(batteryStatus: Intent?) {
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        if (status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL) {
            observer.onActionPowerConnected()
        } else {
            observer.onActionPowerDisconnected()
        }

        val batteryPercent: Float = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        } ?: 100.0f
        if (batteryPercent < INITIAL_LOW_BATTERY_THRESHOLD) {
            observer.onActionBatteryLow()
        } else {
            observer.onActionBatteryOkay()
        }
    }

    public fun onDestroy() {
        context.unregisterReceiver(actionTimeTickReceiver)
        context.unregisterReceiver(actionTimeZoneReceiver)
        context.unregisterReceiver(actionTimeReceiver)
        context.unregisterReceiver(actionBatteryLowReceiver)
        context.unregisterReceiver(actionBatteryOkayReceiver)
        context.unregisterReceiver(actionPowerConnectedReceiver)
        context.unregisterReceiver(mockTimeReceiver)
    }
}
