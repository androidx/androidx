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

package androidx.glance.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal class IdleEventBroadcastReceiver(val onIdle: () -> Unit) : BroadcastReceiver() {
    companion object {
        val events = listOf(
            PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED,
            PowerManager.ACTION_DEVICE_LIGHT_IDLE_MODE_CHANGED,
            PowerManager.ACTION_LOW_POWER_STANDBY_ENABLED_CHANGED
        )
        val filter = IntentFilter().apply {
            events.forEach { addAction(it) }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in events)
            checkIdleStatus(context)
    }

    internal fun checkIdleStatus(context: Context) {
        // Idle status is not available before Android M
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        var isIdle = Api23Impl.isIdle(pm)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isIdle = isIdle || Api33Impl.isLightIdleOrLowPowerStandby(pm)
        }
        if (isIdle) onIdle()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object Api33Impl {
    @DoNotInline
    fun isLightIdleOrLowPowerStandby(pm: PowerManager): Boolean {
        return pm.isLowPowerStandbyEnabled || pm.isDeviceLightIdleMode
    }
}

@RequiresApi(Build.VERSION_CODES.M)
private object Api23Impl {
    @DoNotInline
    fun isIdle(pm: PowerManager): Boolean {
        return pm.isDeviceIdleMode
    }
}

/**
 * Observe idle events while running [block]. If the device enters idle mode, run [onIdle].
 */
internal suspend fun <T> observeIdleEvents(
    context: Context,
    onIdle: suspend () -> Unit,
    block: suspend () -> T,
): T = coroutineScope {
    val idleReceiver = IdleEventBroadcastReceiver { launch { onIdle() } }
    context.registerReceiver(idleReceiver, IdleEventBroadcastReceiver.filter)
    try {
        idleReceiver.checkIdleStatus(context)
        return@coroutineScope block()
    } finally {
        context.unregisterReceiver(idleReceiver)
    }
}