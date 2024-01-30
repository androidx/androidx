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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.Checkbox
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.SplitToggleButton
import androidx.wear.compose.material3.Switch
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.ToggleButton

@Sampled
@Composable
fun ToggleButtonWithCheckbox() {
    var checked by remember { mutableStateOf(true) }
    ToggleButton(
        label = {
            Text("SwitchIcon", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        secondaryLabel = {
            Text("With secondary label", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        checked = checked,
        selectionControl = {
            Checkbox(
                checked = checked,
                enabled = true,
                modifier = Modifier.semantics {
                    this.contentDescription =
                        if (checked) "On" else "Off"
                }
            )
        },
        onCheckedChange = { checked = it },
        icon = {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Favorite icon"
            )
        },
        enabled = true,
    )
}

@Sampled
@Composable
fun ToggleButtonWithSwitch() {
    var checked by remember { mutableStateOf(true) }
    ToggleButton(
        label = {
            Text("SwitchIcon", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        secondaryLabel = {
            Text("With secondary label", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        checked = checked,
        selectionControl = {
            Switch(
                checked = checked,
                enabled = true,
                modifier = Modifier.semantics {
                    this.contentDescription =
                        if (checked) "On" else "Off"
                }
            )
        },
        onCheckedChange = { checked = it },
        icon = {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Favorite icon"
            )
        },
        enabled = true,
    )
}

@Sampled
@Composable
fun ToggleButtonWithRadioButton() {
    var selected by remember { mutableStateOf(true) }
    ToggleButton(
        label = {
            Text("RadioIcon", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        secondaryLabel = {
            Text("With secondary label", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        checked = selected,
        selectionControl = {
            RadioButton(
                selected = selected,
                enabled = true,
                modifier = Modifier.semantics {
                    this.contentDescription =
                        if (selected) "On" else "Off"
                }
            )
        },
        onCheckedChange = { selected = it },
        icon = {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Favorite icon"
            )
        },
        enabled = true,
    )
}

@Sampled
@Composable
fun SplitToggleButtonWithCheckbox() {
    var checked by remember { mutableStateOf(true) }
    SplitToggleButton(
        label = {
            Text("Split with CheckboxIcon", maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        checked = checked,
        selectionControl = {
            Checkbox(
                checked = checked,
                enabled = true,
                modifier = Modifier.semantics {
                    this.contentDescription =
                        if (checked) "Checked" else "Unchecked"
                }
            )
        },
        onCheckedChange = { checked = it },
        onClick = {
            /* Do something */
        },
        enabled = true,
    )
}

@Sampled
@Composable
fun SplitToggleButtonWithSwitch() {
    var checked by remember { mutableStateOf(true) }
    SplitToggleButton(
        label = {
            Text("Split with CheckboxIcon", maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        checked = checked,
        selectionControl = {
            Switch(
                checked = checked,
                enabled = true,
                modifier = Modifier.semantics {
                    this.contentDescription =
                        if (checked) "Checked" else "Unchecked"
                }
            )
        },
        onCheckedChange = { checked = it },
        onClick = {
            /* Do something */
        },
        enabled = true,
    )
}

@Sampled
@Composable
fun SplitToggleButtonWithRadioButton() {
    var selected by remember { mutableStateOf(true) }
    SplitToggleButton(
        label = {
            Text("Split with CheckboxIcon", maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        checked = selected,
        selectionControl = {
            Checkbox(
                checked = selected,
                enabled = true,
                modifier = Modifier.semantics {
                    this.contentDescription =
                        if (selected) "On" else "Off"
                }
            )
        },
        onCheckedChange = { selected = it },
        onClick = {
            /* Do something */
        },
        enabled = true,
    )
}
