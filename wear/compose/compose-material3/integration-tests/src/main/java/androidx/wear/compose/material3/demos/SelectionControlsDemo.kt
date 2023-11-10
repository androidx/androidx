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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Checkbox
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.Switch
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.ToggleButton

@Composable
fun CheckboxDemos() {
    ScalingLazyColumn(
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
    ScalingLazyColumn(
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
fun RadioButtonDemos() {
    var selected by remember { mutableStateOf(false) }
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        item {
            ListHeader { Text(text = "Radio Button") }
        }
        item {
            RadioButtonDemo(
                selected = selected,
                onSelectedChanged = { if (!selected) selected = true },
                enabled = true
            )
        }
        item {
            RadioButtonDemo(
                selected = !selected,
                onSelectedChanged = { if (selected) selected = false },
                enabled = true
            )
        }
        item {
            ListHeader { Text(text = "Disabled Radio Button", textAlign = TextAlign.Center) }
        }
        item {
            RadioButtonDemo(selected = false, enabled = false)
        }
        item {
            RadioButtonDemo(selected = true, enabled = false)
        }
    }
}

@Composable
fun CheckboxDemo(initialChecked: Boolean, enabled: Boolean) {
    var checked by remember { mutableStateOf(initialChecked) }
    ToggleButton(
        label = {
            Text("Checkbox", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        checked = checked,
        selectionControl = {
            Checkbox(
                checked = checked,
                enabled = enabled,
                modifier = Modifier.semantics {
                    this.contentDescription =
                        if (checked) "On" else "Off"
                }
            )
        },
        onCheckedChange = { checked = it },
        enabled = enabled,
    )
}

@Composable
fun SwitchDemo(initialChecked: Boolean, enabled: Boolean) {
    var checked by remember { mutableStateOf(initialChecked) }
    ToggleButton(
        label = {
            Text("Switch", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        checked = checked,
        selectionControl = {
            Switch(
                checked = checked,
                enabled = enabled,
                modifier = Modifier.semantics {
                    this.contentDescription =
                        if (checked) "On" else "Off"
                }
            )
        },
        onCheckedChange = { checked = it },
        enabled = enabled,
    )
}

@Composable
fun RadioButtonDemo(
    selected: Boolean,
    enabled: Boolean,
    onSelectedChanged: (Boolean) -> Unit = {}
) {
    ToggleButton(
        label = {
            Text("Radio button", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        checked = selected,
        selectionControl = {
            RadioButton(
                selected = selected,
                enabled = enabled,
                modifier = Modifier.semantics {
                    this.contentDescription =
                        if (selected) "On" else "Off"
                }
            )
        },
        onCheckedChange = onSelectedChanged,
        enabled = enabled,
    )
}
