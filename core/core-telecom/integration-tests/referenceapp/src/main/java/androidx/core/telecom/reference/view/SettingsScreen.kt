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

package androidx.core.telecom.reference.view

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit

// Constants for SharedPreferences
private const val PREFS_NAME = "MyAppSettings"
// Phone Prefix Keys
private const val KEY_PHONE_PREFIX = "phone_number_prefix"
private const val DEFAULT_PREFIX = "CustomScheme"
// Extension Keys
private const val KEY_PARTICIPANT_EXTENSION_ENABLED = "participant_extension_enabled"
private const val KEY_LOCAL_CALL_SILENCE_EXTENSION_ENABLED = "local_call_silence_extension_enabled"
private const val KEY_CALL_ICON_EXTENSION_ENABLED = "call_icon_extension_enabled"

/**
 * Loads the saved phone number prefix from SharedPreferences.
 *
 * If no prefix is found, it returns the [DEFAULT_PREFIX].
 *
 * @param context The application context to access SharedPreferences.
 * @return The saved prefix string or the default prefix.
 */
fun loadPhoneNumberPrefix(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // Get the saved prefix, defaulting to DEFAULT_PREFIX if not found
    return prefs.getString(KEY_PHONE_PREFIX, DEFAULT_PREFIX) ?: DEFAULT_PREFIX
}

/**
 * Saves the given phone number prefix to SharedPreferences.
 *
 * @param context The application context to access SharedPreferences.
 * @param prefix The prefix string to save.
 */
fun savePhoneNumberPrefix(context: Context, prefix: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit { putString(KEY_PHONE_PREFIX, prefix) }
}

/**
 * Loads the enabled state for a specific extension from SharedPreferences. Defaults to `true` if
 * the setting is not found.
 *
 * @param context The application context.
 * @param key The SharedPreferences key for the extension setting.
 * @return `true` if the extension is enabled (or not set), `false` otherwise.
 */
fun loadExtensionEnabled(context: Context, key: String): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // Default to true (enabled) if the key doesn't exist
    return prefs.getBoolean(key, true)
}

/**
 * Saves the enabled state for a specific extension to SharedPreferences.
 *
 * @param context The application context.
 * @param key The SharedPreferences key for the extension setting.
 * @param isEnabled The new enabled state to save.
 */
fun saveExtensionEnabled(context: Context, key: String, isEnabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit { putBoolean(key, isEnabled) }
}

data class ExtensionSettings(
    val participantEnabled: Boolean,
    val localCallSilenceEnabled: Boolean,
    val callIconEnabled: Boolean,
)

fun loadAllExtensionSettings(context: Context): ExtensionSettings {
    return ExtensionSettings(
        participantEnabled = loadExtensionEnabled(context, KEY_PARTICIPANT_EXTENSION_ENABLED),
        localCallSilenceEnabled =
            loadExtensionEnabled(context, KEY_LOCAL_CALL_SILENCE_EXTENSION_ENABLED),
        callIconEnabled = loadExtensionEnabled(context, KEY_CALL_ICON_EXTENSION_ENABLED),
    )
}

/**
 * Composable function that displays the Settings screen. Allows configuration of the phone number
 * prefix and enabling/disabling call extensions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current

    // State for the phone number prefix
    var prefix by remember { mutableStateOf(loadPhoneNumberPrefix(context)) }

    // State for each extension setting
    var participantEnabled by remember {
        mutableStateOf(loadExtensionEnabled(context, KEY_PARTICIPANT_EXTENSION_ENABLED))
    }
    var localCallSilenceEnabled by remember {
        mutableStateOf(loadExtensionEnabled(context, KEY_LOCAL_CALL_SILENCE_EXTENSION_ENABLED))
    }
    var callIconEnabled by remember {
        mutableStateOf(loadExtensionEnabled(context, KEY_CALL_ICON_EXTENSION_ENABLED))
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            // --- Phone Prefix Section ---
            Text(
                text = "Configure the prefix added before dialing numbers.",
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value = prefix,
                onValueChange = { newValue ->
                    prefix = newValue
                    savePhoneNumberPrefix(context, newValue)
                },
                label = { Text("Phone Number Prefix") },
                placeholder = { Text(DEFAULT_PREFIX) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Current Prefix: $prefix", modifier = Modifier.padding(bottom = 16.dp))

            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // --- Call Extensions Section ---
            Text(
                text = "Call Extensions",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // Participant Extension Toggle
            SettingSwitchRow(
                text = "Participant Extension",
                checked = participantEnabled,
                onCheckedChange = { isChecked ->
                    participantEnabled = isChecked
                    saveExtensionEnabled(context, KEY_PARTICIPANT_EXTENSION_ENABLED, isChecked)
                },
            )

            // Local Call Silence Extension Toggle
            SettingSwitchRow(
                text = "Local Call Silence",
                checked = localCallSilenceEnabled,
                onCheckedChange = { isChecked ->
                    localCallSilenceEnabled = isChecked
                    saveExtensionEnabled(
                        context,
                        KEY_LOCAL_CALL_SILENCE_EXTENSION_ENABLED,
                        isChecked,
                    )
                },
            )

            // Call Icon Extension Toggle
            SettingSwitchRow(
                text = "Call Icon",
                checked = callIconEnabled,
                onCheckedChange = { isChecked ->
                    callIconEnabled = isChecked
                    saveExtensionEnabled(context, KEY_CALL_ICON_EXTENSION_ENABLED, isChecked)
                },
            )
        }
    }
}

/** Reusable Composable for a setting row with text and a switch. */
@Composable
fun SettingSwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = text, modifier = Modifier.weight(1f).padding(end = 16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Provides a design-time preview of the [SettingsScreen] composable in Android Studio. */
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SettingsScreen()
}
