/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.integration.demos

import android.widget.Toast
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.SplitToggleChip
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults

@Composable
fun ToggleChips() {
    val applicationContext = LocalContext.current
    val scrollState: ScrollState = rememberScrollState()
    var enabled by remember { mutableStateOf(true) }

    var checkBoxIconChecked by remember { mutableStateOf(true) }
    var switchIconChecked by remember { mutableStateOf(true) }
    var radioIconChecked by remember { mutableStateOf(true) }
    var radioIconWithSecondaryChecked by remember { mutableStateOf(true) }
    var splitWithCheckboxIconChecked by remember { mutableStateOf(true) }
    var splitWithSwitchIconChecked by remember { mutableStateOf(true) }
    var splitWithRadioIconChecked by remember { mutableStateOf(true) }

    var switchIconWithSecondaryChecked by remember { mutableStateOf(true) }
    var switchIconWithIconChecked by remember { mutableStateOf(true) }
    var splitWithCustomColorChecked by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.verticalScroll(scrollState)
            .padding(
                PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 15.dp,
                    bottom = 50.dp
                )
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Toggle Chips",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption1,
            color = Color.White
        )
        ToggleChip(
            label = { Text("CheckboxIcon") },
            checked = checkBoxIconChecked,
            toggleIcon = {
                ToggleChipDefaults.CheckboxIcon(checked = checkBoxIconChecked)
            },
            onCheckedChange = { checkBoxIconChecked = it },
            enabled = enabled,
        )
        ToggleChip(
            label = { Text("SwitchIcon") },
            checked = switchIconChecked,
            toggleIcon = {
                ToggleChipDefaults.SwitchIcon(checked = switchIconChecked)
            },
            onCheckedChange = { switchIconChecked = it },
            enabled = enabled,
        )
        ToggleChip(
            label = {
                Text("RadioIcon", maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            checked = radioIconChecked,
            toggleIcon = {
                ToggleChipDefaults.RadioIcon(checked = radioIconChecked)
            },
            onCheckedChange = { radioIconChecked = it },
            enabled = enabled,
        )
        ToggleChip(
            label = {
                Text(
                    "RadioIcon",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            secondaryLabel = {
                Text("With secondary label", maxLines = 1, overflow = TextOverflow.Ellipsis)
            },

            checked = radioIconWithSecondaryChecked,
            toggleIcon = { ToggleChipDefaults.RadioIcon(checked = radioIconWithSecondaryChecked) },
            onCheckedChange = { radioIconWithSecondaryChecked = it },
            enabled = enabled,
        )
        ToggleChip(
            label = {
                Text("SwitchIcon", maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            secondaryLabel = {
                Text("With secondary label", maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            checked = switchIconWithSecondaryChecked,
            toggleIcon = {
                ToggleChipDefaults.SwitchIcon(checked = switchIconWithSecondaryChecked)
            },
            onCheckedChange = { switchIconWithSecondaryChecked = it },
            appIcon = { DemoIcon(R.drawable.ic_airplanemode_active_24px) },
            enabled = enabled,
        )
        ToggleChip(
            label = { Text("SwitchIcon", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            secondaryLabel = {
                Text("With switchable icon", maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            checked = switchIconWithIconChecked,
            toggleIcon = { ToggleChipDefaults.SwitchIcon(checked = switchIconWithIconChecked) },
            onCheckedChange = { switchIconWithIconChecked = it },
            appIcon = {
                if (switchIconWithIconChecked) DemoIcon(R.drawable.ic_volume_up_24px) else
                    DemoIcon(R.drawable.ic_volume_off_24px)
            },
            enabled = enabled,
        )
        Text(
            text = "Split Toggle Chips",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption1,
            color = Color.White
        )
        SplitToggleChip(
            label = { Text("Split with CheckboxIcon") },
            checked = splitWithCheckboxIconChecked,
            toggleIcon = {
                ToggleChipDefaults.CheckboxIcon(checked = splitWithCheckboxIconChecked)
            },
            onCheckedChange = { splitWithCheckboxIconChecked = it },
            onClick = {
                Toast.makeText(
                    applicationContext, "Text was clicked",
                    Toast.LENGTH_SHORT
                ).show()
            },
            enabled = enabled,
        )
        SplitToggleChip(
            label = { Text("Split with SwitchIcon") },
            checked = splitWithSwitchIconChecked,
            toggleIcon = {
                ToggleChipDefaults.SwitchIcon(checked = splitWithSwitchIconChecked)
            },
            onCheckedChange = { splitWithSwitchIconChecked = it },
            onClick = {
                Toast.makeText(
                    applicationContext, "Text was clicked",
                    Toast.LENGTH_SHORT
                ).show()
            },
            enabled = enabled,
        )
        SplitToggleChip(
            label = { Text("Split with RadioIcon") },
            checked = splitWithRadioIconChecked,
            toggleIcon = {
                ToggleChipDefaults.RadioIcon(checked = splitWithRadioIconChecked)
            },
            onCheckedChange = { splitWithRadioIconChecked = it },
            onClick = {
                Toast.makeText(
                    applicationContext, "Text was clicked",
                    Toast.LENGTH_SHORT
                ).show()
            },
            enabled = enabled,
        )
        SplitToggleChip(
            label = {
                Text(
                    "Split with CheckboxIcon", maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            secondaryLabel = {
                Text(
                    "and custom background color", maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            checked = splitWithCustomColorChecked,
            toggleIcon = { ToggleChipDefaults.CheckboxIcon(checked = splitWithCustomColorChecked) },
            onCheckedChange = { splitWithCustomColorChecked = it },
            onClick = {
                Toast.makeText(
                    applicationContext,
                    "Text was clicked", Toast.LENGTH_SHORT
                ).show()
            },
            colors = ToggleChipDefaults.toggleChipColors(
                checkedStartBackgroundColor = Color.Yellow.copy(alpha = 0.5f)
            ),
            enabled = enabled,
        )
        ToggleChip(
            checked = enabled,
            onCheckedChange = { enabled = it },
            label = {
                Text("Chips enabled")
            },
            toggleIcon = {
                ToggleChipDefaults.SwitchIcon(checked = enabled)
            },
        )
    }
}

@Composable
fun RtlToggleChips() {
    val applicationContext = LocalContext.current
    val scrollState: ScrollState = rememberScrollState()

    var switchIconChecked by remember { mutableStateOf(true) }
    var radioIconChecked by remember { mutableStateOf(true) }
    var switchIconWithIconChecked by remember { mutableStateOf(true) }
    var splitWithCheckboxIconChecked by remember { mutableStateOf(true) }
    var splitWithSwitchIconChecked by remember { mutableStateOf(true) }
    var splitWithRadioIconChecked by remember { mutableStateOf(true) }

    var splitWithCustomColorChecked by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.verticalScroll(scrollState)
            .padding(
                PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 15.dp,
                    bottom = 50.dp
                )
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "RTL ToggleChips",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption1,
            color = Color.White
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ToggleChip(
                label = { Text("SwitchIcon") },
                checked = switchIconChecked,
                toggleIcon = {
                    ToggleChipDefaults.SwitchIcon(checked = switchIconChecked)
                },
                onCheckedChange = {
                    switchIconChecked = it
                },
            )
            ToggleChip(
                label = {
                    Text(
                        "RadioIcon",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                checked = radioIconChecked,
                toggleIcon = {
                    ToggleChipDefaults.RadioIcon(checked = radioIconChecked)
                },
                onCheckedChange = {
                    radioIconChecked = it
                },
            )
            ToggleChip(
                label = { Text("SwitchIcon", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                secondaryLabel = {
                    Text("With switchable icon", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                checked = switchIconWithIconChecked,
                toggleIcon = {
                    ToggleChipDefaults.SwitchIcon(checked = switchIconWithIconChecked)
                },
                onCheckedChange = {
                    switchIconWithIconChecked = it
                },
                appIcon = {
                    if (switchIconWithIconChecked) DemoIcon(R.drawable.ic_volume_up_24px) else
                        DemoIcon(R.drawable.ic_volume_off_24px)
                },
            )
        }

        Text(
            text = "SplitToggleChip",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption1,
            color = Color.White
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            SplitToggleChip(
                label = { Text("Split with CheckboxIcon") },
                checked = splitWithCheckboxIconChecked,
                toggleIcon = {
                    ToggleChipDefaults.CheckboxIcon(checked = splitWithCheckboxIconChecked)
                },
                onCheckedChange = { splitWithCheckboxIconChecked = it },
                onClick = {
                    Toast.makeText(applicationContext, "Text was clicked", Toast.LENGTH_SHORT)
                        .show()
                },
            )
            SplitToggleChip(
                label = { Text("Split with SwitchIcon") },
                checked = splitWithSwitchIconChecked,
                toggleIcon = {
                    ToggleChipDefaults.SwitchIcon(checked = splitWithSwitchIconChecked)
                },
                onCheckedChange = { splitWithSwitchIconChecked = it },
                onClick = {
                    Toast.makeText(
                        applicationContext, "Text was clicked",
                        Toast.LENGTH_SHORT
                    ).show()
                },
            )
            SplitToggleChip(
                label = { Text("Split with RadioIcon") },
                checked = splitWithRadioIconChecked,
                toggleIcon = {
                    ToggleChipDefaults.RadioIcon(checked = splitWithRadioIconChecked)
                },
                onCheckedChange = { splitWithRadioIconChecked = it },
                onClick = {
                    Toast.makeText(
                        applicationContext, "Text was clicked",
                        Toast.LENGTH_SHORT
                    ).show()
                },
            )
            SplitToggleChip(
                label = {
                    Text(
                        "Split with CheckboxIcon", maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                secondaryLabel = {
                    Text(
                        "and custom background color", maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                checked = splitWithCustomColorChecked,
                toggleIcon = {
                    ToggleChipDefaults.CheckboxIcon(checked = splitWithCustomColorChecked)
                },
                onCheckedChange = { splitWithCustomColorChecked = it },
                onClick = {
                    Toast.makeText(
                        applicationContext,
                        "Text was clicked", Toast.LENGTH_SHORT
                    ).show()
                },
                colors = ToggleChipDefaults.toggleChipColors(
                    checkedStartBackgroundColor = Color.Yellow.copy(alpha = 0.5f)
                ),
            )
        }
    }
}