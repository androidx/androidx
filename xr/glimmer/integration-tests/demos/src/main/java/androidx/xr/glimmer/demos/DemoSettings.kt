/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.demos

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.xr.glimmer.ListItem
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.list.VerticalList

@Composable
internal fun DemoSettings() {
    VerticalList(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            val overlayOnBackground = OverlayOnBackgroundSetting.asState().value
            val context = LocalContext.current
            ListItem(
                modifier = Modifier.padding(16.dp),
                onClick = { OverlayOnBackgroundSetting.set(context, !overlayOnBackground) },
            ) {
                Text("${if (overlayOnBackground) "Disable" else "Enable"} overlay on background")
            }
        }
    }
}

internal object OverlayOnBackgroundSetting {
    fun set(context: Context, enabled: Boolean) {
        context.glimmerSharedPreferences.edit().putBoolean(KEY, enabled).apply()
    }

    @Composable fun asState() = preferenceAsState(KEY) { getBoolean(KEY, false) }

    private const val KEY = "overlay_on_background"
}

@Composable
private fun <T> preferenceAsState(key: String, readValue: SharedPreferences.() -> T): State<T> {
    val context = LocalContext.current
    val sharedPreferences = remember(context) { context.glimmerSharedPreferences }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // De-duplicate passing keys explicitly to remembers and effects below.
    return key(key, readValue, sharedPreferences) {
        val value = remember { mutableStateOf(sharedPreferences.readValue()) }

        // Update value when preference changes.
        DisposableEffect(Unit) {
            val listener = OnSharedPreferenceChangeListener { _, changedKey ->
                if (changedKey == key) {
                    value.value = sharedPreferences.readValue()
                }
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            onDispose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
        }

        // Also update the value when resumed.
        DisposableEffect(lifecycle) {
            val obs = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    value.value = sharedPreferences.readValue()
                }
            }
            lifecycle.addObserver(obs)
            onDispose { lifecycle.removeObserver(obs) }
        }

        return@key value
    }
}

private val Context.glimmerSharedPreferences
    get() = getSharedPreferences("glimmer", MODE_PRIVATE)
