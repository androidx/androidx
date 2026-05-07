/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.integration.demos.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
internal fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val layoutDirection by
        remember {
                context.dataStore.data.map { it[LAYOUT_DIRECTION_PREF_KEY] ?: LAYOUT_DIRECTION_LTR }
            }
            .collectAsState(initial = LAYOUT_DIRECTION_LTR)

    val coroutineScope = rememberCoroutineScope()

    val isRtl = layoutDirection == LAYOUT_DIRECTION_RTL
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Layout Direction",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = !isRtl,
                onClick = {
                    coroutineScope.launch {
                        context.dataStore.edit { preferences ->
                            preferences[LAYOUT_DIRECTION_PREF_KEY] = LAYOUT_DIRECTION_LTR
                        }
                    }
                },
            )
            Text("LTR")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = isRtl,
                onClick = {
                    coroutineScope.launch {
                        context.dataStore.edit { preferences ->
                            preferences[LAYOUT_DIRECTION_PREF_KEY] = LAYOUT_DIRECTION_RTL
                        }
                    }
                },
            )
            Text("RTL")
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    SettingsScreen()
}
