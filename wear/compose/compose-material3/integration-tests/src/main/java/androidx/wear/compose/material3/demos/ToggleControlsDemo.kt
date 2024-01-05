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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.wear.compose.integration.demos.common.ScalingLazyColumnWithRSB
import androidx.wear.compose.material3.Checkbox
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Switch
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.ToggleButton

@Composable
fun CheckboxDemos() {
    ScalingLazyColumnWithRSB(
        modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader { Text(text = "Checkbox") }
        }
        item {
            CheckboxDemo(initialChecked = false, enabled = true)
        }
        item {
            CheckboxDemo(initialChecked = true, enabled = true)
        }
        item {
            ListHeader { Text(text = "Disabled Checkbox") }
        }
        item {
            CheckboxDemo(initialChecked = false, enabled = false)
        }
        item {
            CheckboxDemo(initialChecked = true, enabled = false)
        }
    }
}

@Composable
fun SwitchDemos() {
    ScalingLazyColumnWithRSB(
        modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader { Text(text = "Switch") }
        }
        item {
            SwitchDemo(initialChecked = false, enabled = true)
        }
        item {
            SwitchDemo(initialChecked = true, enabled = true)
        }
        item {
            ListHeader { Text(text = "Disabled Switch") }
        }
        item {
            SwitchDemo(initialChecked = false, enabled = false)
        }
        item {
            SwitchDemo(initialChecked = true, enabled = false)
        }
        item {
            ListHeader { Text(text = "RTL Switch") }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SwitchDemo(initialChecked = false, enabled = true)
            }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SwitchDemo(initialChecked = true, enabled = true)
            }
        }
    }
}

@Composable
private fun CheckboxDemo(initialChecked: Boolean, enabled: Boolean) {
    var checked by remember { mutableStateOf(initialChecked) }
    ToggleButton(
        label = {
            Text("Primary label", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        checked = checked,
        toggleControl = { Checkbox() },
        onCheckedChange = { checked = it },
        enabled = enabled,
    )
}

@Composable
private fun SwitchDemo(initialChecked: Boolean, enabled: Boolean) {
    var checked by remember { mutableStateOf(initialChecked) }
    ToggleButton(
        label = {
            Text("Primary label", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        checked = checked,
        toggleControl = { Switch() },
        onCheckedChange = { checked = it },
        enabled = enabled,
    )
}
