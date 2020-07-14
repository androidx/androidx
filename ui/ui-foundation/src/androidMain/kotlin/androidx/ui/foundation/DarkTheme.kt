/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.compose.Composable
import androidx.compose.onActive
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.core.ConfigurationAmbient
import androidx.ui.core.ContextAmbient

/**
 * This effect should be used to help build responsive UIs that follow the system setting, to avoid
 * harsh contrast changes when switching between applications. The behaviour differs depending on
 * API:
 *
 * On [Build.VERSION_CODES.Q] and above: returns the system-wide dark theme setting.
 *
 * On [Build.VERSION_CODES.P] and below: returns whether the device is in power saving mode or not,
 * which can be considered analogous to dark theme for these devices.
 *
 * It is also recommended to provide user accessible overrides in your application, so users can
 * choose to force an always-light or always-dark theme. To do this, you should provide the current
 * theme value in an ambient or similar to components further down your hierarchy, only calling
 * this effect once at the top level if no user override has been set. This also helps avoid
 * multiple calls to this effect, which can be expensive as it queries system configuration.
 *
 * For example, to draw a white rectangle when in dark theme, and a black rectangle when in light
 * theme:
 *
 * @sample androidx.ui.foundation.samples.DarkThemeSample
 *
 * @return `true` if the system is considered to be in 'dark theme'.
 */
@Composable
fun isSystemInDarkTheme(): Boolean {
    return if (Build.VERSION.SDK_INT >= 29) {
        isSystemSetToDarkTheme()
    } else {
        isInPowerSaveMode()
    }
}

/**
 * On [Build.VERSION_CODES.P] and below there is no system-wide dark theme toggle, so we use
 * whether the device is in power save mode or not as an indicator here.
 *
 * @return `true` if the device is in power save mode
 */
@Composable
private fun isInPowerSaveMode(): Boolean {
    val context = ContextAmbient.current
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    val isPowerSaveMode = state { powerManager.isPowerSaveMode }

    val broadcastReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                isPowerSaveMode.value = powerManager.isPowerSaveMode
            }
        }
    }

    val intentFilter = remember {
        IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
    }

    onActive {
        try {
            context.registerReceiver(broadcastReceiver, intentFilter)
        } catch (e: Exception) {
            // already registered
        }
        onDispose {
            try {
                context.unregisterReceiver(broadcastReceiver)
            } catch (e: Exception) {
                // already unregistered
            }
        }
    }

    return isPowerSaveMode.value
}

/**
 * Check to see if [Configuration.UI_MODE_NIGHT_YES] bit is set. It is also possible for this bit to
 * be [Configuration.UI_MODE_NIGHT_UNDEFINED], in which case we treat light theme as the default,
 * so we return false.
 *
 * @return `true` if the system-wide dark theme toggle is enabled
 */
@RequiresApi(29)
@Composable
private fun isSystemSetToDarkTheme(): Boolean {
    val configuration = ConfigurationAmbient.current
    return (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration
        .UI_MODE_NIGHT_YES
}
