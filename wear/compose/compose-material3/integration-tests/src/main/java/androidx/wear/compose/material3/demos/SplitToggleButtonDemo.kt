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

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Checkbox
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.SplitToggleButton
import androidx.wear.compose.material3.Switch
import androidx.wear.compose.material3.Text

@Composable
fun SplitToggleButtonDemo() {
    var selectedRadioIndex by remember { mutableIntStateOf(0) }
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader { Text("Checkbox") }
        }
        item {
            DemoSplitToggleCheckbox(enabled = true, initiallyChecked = true)
        }
        item {
            DemoSplitToggleCheckbox(enabled = true, initiallyChecked = false)
        }
        item {
            ListHeader { Text("Disabled Checkbox") }
        }
        item {
            DemoSplitToggleCheckbox(enabled = false, initiallyChecked = true)
        }
        item {
            DemoSplitToggleCheckbox(enabled = false, initiallyChecked = false)
        }
        item {
            ListHeader { Text("Switch") }
        }
        item {
            DemoSplitToggleSwitch(enabled = true, initiallyChecked = true)
        }
        item {
            DemoSplitToggleSwitch(enabled = true, initiallyChecked = false)
        }
        item {
            ListHeader { Text("Disabled Switch") }
        }
        item {
            DemoSplitToggleSwitch(enabled = false, initiallyChecked = true)
        }
        item {
            DemoSplitToggleSwitch(enabled = false, initiallyChecked = false)
        }
        item {
            ListHeader { Text("Radio Button") }
        }
        item {
            DemoSplitToggleRadioButton(
                enabled = true,
                (selectedRadioIndex == 0)
            ) { selectedRadioIndex = 0 }
        }
        item {
            DemoSplitToggleRadioButton(
                enabled = true,
                (selectedRadioIndex == 1)
            ) { selectedRadioIndex = 1 }
        }
        item {
            ListHeader { Text("Disabled Radio Button") }
        }
        item {
            DemoSplitToggleRadioButton(enabled = false, selected = true)
        }
        item {
            DemoSplitToggleRadioButton(enabled = false, selected = false)
        }
        item {
            ListHeader { Text("Multi-line") }
        }
        item {
            DemoSplitToggleCheckbox(
                enabled = true,
                initiallyChecked = true,
                primary = "8:15AM",
                secondary = "Mon, Tue, Wed"
            )
        }
        item {
            DemoSplitToggleCheckbox(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary Label with 3 lines of content max"
            )
        }
        item {
            DemoSplitToggleCheckbox(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary Label with 3 lines of content max",
                secondary = "Secondary label with 2 lines"
            )
        }
    }
}

@Composable
private fun DemoSplitToggleCheckbox(
    enabled: Boolean,
    initiallyChecked: Boolean,
    primary: String = "Primary label",
    secondary: String = ""
) {
    var checked by remember { mutableStateOf(initiallyChecked) }
    val context = LocalContext.current
    SplitToggleButton(
        label = {
            Text(primary, maxLines = 3, overflow = TextOverflow.Ellipsis)
        },
        checked = checked,
        selectionControl = {
            Checkbox(
                checked = checked,
                enabled = enabled,
            )
        },
        onCheckedChange = { checked = it },
        onClick = {
            Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show()
        },
        enabled = enabled,
        secondaryLabel = {
            if (secondary.isNotEmpty()) {
                Text(secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    )
}

@Composable
private fun DemoSplitToggleSwitch(enabled: Boolean, initiallyChecked: Boolean) {
    var checked by remember { mutableStateOf(initiallyChecked) }
    val context = LocalContext.current
    SplitToggleButton(
        label = {
            Text("Primary label", maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        checked = checked,
        selectionControl = {
            Switch(
                checked = checked,
                enabled = enabled,
            )
        },
        onCheckedChange = { checked = it },
        onClick = {
            Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show()
        },
        enabled = enabled,
    )
}

@Composable
private fun DemoSplitToggleRadioButton(
    enabled: Boolean,
    selected: Boolean,
    onSelectedChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    SplitToggleButton(
        label = {
            Text("Primary label", maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        checked = selected,
        selectionControl = {
            RadioButton(
                selected = selected,
                enabled = enabled,
            )
        },
        onCheckedChange = onSelectedChanged,
        onClick = {
            Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show()
        },
        enabled = enabled,
    )
}
