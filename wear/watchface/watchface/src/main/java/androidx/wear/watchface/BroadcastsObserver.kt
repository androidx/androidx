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

package androidx.wear.watchface

import android.content.ContentResolver
import android.content.Intent
import android.provider.Settings
import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BroadcastsObserver(
    private val watchState: WatchState,
    private val watchFaceHostApi: WatchFaceHostApi,
    private val deferredWatchFaceImpl: Deferred<WatchFaceImpl>,
    private val uiThreadCoroutineScope: CoroutineScope,
    private val contentResolver: ContentResolver,
    private val ambientSettingAvailable: Boolean
) : BroadcastsReceiver.BroadcastEventObserver {
    private var batteryLow: Boolean? = null
    private var charging: Boolean? = null
    private var sysUiHasSentWatchUiState: Boolean = false

    internal companion object {
        internal const val AMBIENT_ENABLED_PATH = "ambient_enabled"
    }

    override fun onActionTimeTick() {
        // android.intent.action.TIME_TICK is sent by the system at the top of every minute
        watchFaceHostApi.onActionTimeTick()
    }

    override fun onActionTimeZoneChanged() {
        uiThreadCoroutineScope.launch {
            deferredWatchFaceImpl.await().onActionTimeZoneChanged()
        }
    }

    override fun onActionTimeChanged() {
        uiThreadCoroutineScope.launch {
            deferredWatchFaceImpl.await().onActionTimeChanged()
        }
    }

    override fun onActionBatteryLow() {
        batteryLow = true
        if (charging == false) {
            updateBatteryLowAndNotChargingStatus(true)
        }
    }

    override fun onActionBatteryOkay() {
        batteryLow = false
        updateBatteryLowAndNotChargingStatus(false)
    }

    override fun onActionPowerConnected() {
        charging = true
        updateBatteryLowAndNotChargingStatus(false)
    }

    override fun onActionPowerDisconnected() {
        charging = false
        if (batteryLow == true) {
            updateBatteryLowAndNotChargingStatus(true)
        }
    }

    override fun onMockTime(intent: Intent) {
        uiThreadCoroutineScope.launch {
            deferredWatchFaceImpl.await().onMockTime(intent)
        }
    }

    private fun updateBatteryLowAndNotChargingStatus(value: Boolean) {
        val isBatteryLowAndNotCharging =
            watchState.isBatteryLowAndNotCharging as MutableStateFlow
        if (!isBatteryLowAndNotCharging.hasValue() || value != isBatteryLowAndNotCharging.value) {
            isBatteryLowAndNotCharging.value = value
            watchFaceHostApi.invalidate()
        }
    }

    fun onSysUiHasSentWatchUiState() {
        sysUiHasSentWatchUiState = true
    }

    override fun onActionScreenOff() {
        // Before SysUI has connected, we use ActionScreenOn/ActionScreenOff as a trigger to query
        // AMBIENT_ENABLED_PATH in order to determine if the device os ambient or not.
        if (sysUiHasSentWatchUiState) {
            return
        }

        val isAmbient = watchState.isAmbient as MutableStateFlow

        // This is a backup signal for when SysUI is unable to deliver the ambient state (e.g. in
        // direct boot mode). We need to distinguish between ACTION_SCREEN_OFF for entering ambient
        // and the screen turning off. This is only possible from R.
        isAmbient.value = if (ambientSettingAvailable) {
            Settings.Global.getInt(contentResolver, AMBIENT_ENABLED_PATH, 0) == 1
        } else {
            // On P and below we just have to assume we're not ambient.
            false
        }
    }

    override fun onActionScreenOn() {
        // Before SysUI has connected, we use ActionScreenOn/ActionScreenOff as a trigger to query
        // AMBIENT_ENABLED_PATH in order to determine if the device os ambient or not.
        if (sysUiHasSentWatchUiState) {
            return
        }

        val isAmbient = watchState.isAmbient as MutableStateFlow
        isAmbient.value = false
    }
}