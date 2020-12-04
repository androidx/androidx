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
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting

/**
 * All watchface instances share the same [Context] which is a problem for broadcast receivers
 * because the OS will mistakenly believe we're leaking them if there's more than one instance. So
 * we need to use this class to share them.
 */
internal class BroadcastReceivers private constructor(private val context: Context) {

    interface BroadcastEventObserver {
        /** Called when we receive Intent.ACTION_TIME_TICK. */
        @UiThread
        fun onActionTimeTick()

        /** Called when we receive Intent.ACTION_TIMEZONE_CHANGED. */
        @UiThread
        fun onActionTimeZoneChanged()

        /** Called when we receive Intent.ACTION_TIME_CHANGED. */
        @UiThread
        fun onActionTimeChanged()

        /** Called when we receive Intent.ACTION_BATTERY_CHANGED. */
        @UiThread
        fun onActionBatteryChanged(intent: Intent)

        /** Called when we receive Intent.MOCK_TIME_INTENT. */
        @UiThread
        fun onMockTime(intent: Intent)
    }

    companion object {
        val broadcastEventObservers = HashSet<BroadcastEventObserver>()

        /* We don't leak due to balanced calls to[addBroadcastEventObserver] and
        [removeBroadcastEventObserver] which sets this back to null.
         */
        @SuppressWarnings("StaticFieldLeak")
        var broadcastReceivers: BroadcastReceivers? = null

        @UiThread
        fun addBroadcastEventObserver(context: Context, observer: BroadcastEventObserver) {
            broadcastEventObservers.add(observer)
            if (broadcastReceivers == null) {
                broadcastReceivers = BroadcastReceivers(context)
            }
        }

        @UiThread
        fun removeBroadcastEventObserver(observer: BroadcastEventObserver) {
            broadcastEventObservers.remove(observer)
            if (broadcastEventObservers.isEmpty()) {
                broadcastReceivers!!.onDestroy()
                broadcastReceivers = null
            }
        }

        @VisibleForTesting
        fun sendOnActionBatteryChangedForTesting(intent: Intent) {
            require(broadcastEventObservers.isNotEmpty())
            for (observer in broadcastEventObservers) {
                observer.onActionBatteryChanged(intent)
            }
        }

        @VisibleForTesting
        fun sendOnMockTimeForTesting(intent: Intent) {
            require(broadcastEventObservers.isNotEmpty())
            for (observer in broadcastEventObservers) {
                observer.onMockTime(intent)
            }
        }
    }

    private val actionTimeTickReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressWarnings("SyntheticAccessor")
        override fun onReceive(context: Context, intent: Intent) {
            for (observer in broadcastEventObservers) {
                observer.onActionTimeTick()
            }
        }
    }

    private val actionTimeZoneReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            for (observer in broadcastEventObservers) {
                observer.onActionTimeZoneChanged()
            }
        }
    }

    private val actionTimeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            for (observer in broadcastEventObservers) {
                observer.onActionTimeChanged()
            }
        }
    }

    private val actionBatteryLevelReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressWarnings("SyntheticAccessor")
        override fun onReceive(context: Context, intent: Intent) {
            for (observer in broadcastEventObservers) {
                observer.onActionBatteryChanged(intent)
            }
        }
    }

    private val mockTimeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressWarnings("SyntheticAccessor")
        override fun onReceive(context: Context, intent: Intent) {
            for (observer in broadcastEventObservers) {
                observer.onMockTime(intent)
            }
        }
    }

    init {
        context.registerReceiver(actionTimeTickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
        context.registerReceiver(
            actionTimeZoneReceiver,
            IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
        )
        context.registerReceiver(actionTimeReceiver, IntentFilter(Intent.ACTION_TIME_CHANGED))
        context.registerReceiver(
            actionBatteryLevelReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        context.registerReceiver(mockTimeReceiver, IntentFilter(WatchFaceImpl.MOCK_TIME_INTENT))
    }

    fun onDestroy() {
        context.unregisterReceiver(actionTimeTickReceiver)
        context.unregisterReceiver(actionTimeZoneReceiver)
        context.unregisterReceiver(actionTimeReceiver)
        context.unregisterReceiver(actionBatteryLevelReceiver)
        context.unregisterReceiver(mockTimeReceiver)
    }
}