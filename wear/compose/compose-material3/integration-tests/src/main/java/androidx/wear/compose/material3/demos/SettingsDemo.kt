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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.integration.demos.common.ScalingLazyColumnWithRSB
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text

@Composable
fun SettingsDemo() {
    // TODO: Add Scaffold and TimeText when available
    val scalingLazyListState = rememberScalingLazyListState()
    ScalingLazyColumnWithRSB(
        state = scalingLazyListState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ListHeader(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Settings")
            }
        }
       // Connectivity
        item {
            SettingsChip(
                painterResourceId = R.drawable.ic_settings_connectivity,
                text = "Connectivity"
            )
        }
        // Display
        item {
            SettingsChip(
                painterResourceId = R.drawable.ic_settings_brightness,
                text = "Display"
            )
        }
        // Gestures
        item {
            SettingsChip(
                painterResourceId = R.drawable.ic_settings_gestures,
                text = "Gestures"
            )
        }
        // Apps & Notifications
        item {
            SettingsChip(
                painterResourceId = R.drawable.ic_settings_apps,
                text = "Apps & Notifications"
            )
        }
        // Google
        item {
            SettingsChip(
                painterResourceId = R.drawable.ic_settings_googleg,
                text = "Google"
            )
        }
        // Sound
        item {
            SettingsChip(
                painterResourceId = R.drawable.ic_settings_sound,
                text = "Sound"
            )
        }
        // Vibration
        item {
            SettingsChip(
                painterResourceId = R.drawable.ic_settings_vibration,
                text = "Vibration"
            )
        }
        // Battery
        item {
            SettingsChip(
                painterResourceId = R.drawable.ic_settings_battery,
                text = "Battery"
            )
        }
        // General
        item {
            SettingsChip(
                painterResourceId = R.drawable.ic_settings_watch_device,
                text = "General"
            )
        }
        // Health Profile
        item {
            SettingsChip(
                painterResourceId = R.drawable.ic_settings_health_profile,
                text = "Health Profile"
            )
        }
        // Location
        item {
            SettingsChip(
                painterResourceId = R.drawable.ic_settings_location,
                text = "Location"
            )
        }
        // Safety and Emergency
        item {
            SettingsChip(
                painterResourceId = R.drawable.ic_settings_emergency,
                text = "Safety and Emergency"
            )
        }
        // Accessibility
        item {
            SettingsChip(
                painterResourceId = R.drawable.ic_settings_accessibility,
                text = "Accessibility"
            )
        }
        // Security
        item {
            SettingsChip(
                painterResourceId = R.drawable.ic_settings_security,
                text = "Security"
            )
        }
        // System
        item {
            SettingsChip(
                painterResourceId = R.drawable.ic_settings_system,
                text = "System"
            )
        }
    }
}

@Composable
private fun SettingsChip(
    painterResourceId: Int,
    text: String
) {
    Button(
        onClick = { /* */ },
        modifier = Modifier.fillMaxSize(),
        colors = ButtonDefaults.filledTonalButtonColors(),
        icon = {
            Icon(
                painter = painterResource(painterResourceId),
                contentDescription = text
            )
        },
        label = { Text(text) }
    )
}
