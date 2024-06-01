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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Checkbox
import androidx.wear.compose.material.CheckboxDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.SplitToggleChip
import androidx.wear.compose.material.SplitToggleChipColors
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.SwitchDefaults
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults

@Composable
fun ToggleChips(
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    description: String = "Toggle Chips"
) {
    val scrollState: ScalingLazyListState = rememberScalingLazyListState()
    var enabled by remember { mutableStateOf(true) }

    var checkBoxIconChecked by remember { mutableStateOf(true) }
    var checkBoxIconCustomColorChecked by remember { mutableStateOf(true) }
    var switchIconChecked by remember { mutableStateOf(true) }
    var switchIconCustomColorChecked by remember { mutableStateOf(true) }
    var splitWithCheckboxIconChecked by remember { mutableStateOf(true) }
    var splitWithSwitchIconChecked by remember { mutableStateOf(true) }

    var switchIconWithSecondaryChecked by remember { mutableStateOf(true) }
    var switchIconWithIconChecked by remember { mutableStateOf(true) }

    ScalingLazyColumn(
        state = scrollState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        item {
            ListHeader {
                Text(
                    text = description,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption1,
                    color = Color.White
                )
            }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                ToggleChip(
                    label = { Text("Checkbox") },
                    checked = checkBoxIconChecked,
                    toggleControl = {
                        Checkbox(
                            checked = checkBoxIconChecked,
                            enabled = enabled,
                        )
                    },
                    onCheckedChange = { checkBoxIconChecked = it },
                    enabled = enabled,
                )
            }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                ToggleChip(
                    label = { Text("Checkbox") },
                    secondaryLabel = { Text("Custom color") },
                    checked = checkBoxIconCustomColorChecked,
                    toggleControl = {
                        Checkbox(
                            colors =
                                CheckboxDefaults.colors(
                                    checkedBoxColor = MaterialTheme.colors.primary,
                                    checkedCheckmarkColor = Color.Green,
                                    uncheckedBoxColor = Color.Magenta,
                                    uncheckedCheckmarkColor = Color.Red,
                                ),
                            checked = checkBoxIconCustomColorChecked,
                            enabled = enabled,
                        )
                    },
                    onCheckedChange = { checkBoxIconCustomColorChecked = it },
                    enabled = enabled,
                )
            }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                ToggleChip(
                    label = { Text("Switch") },
                    checked = switchIconChecked,
                    toggleControl = {
                        Switch(
                            checked = switchIconChecked,
                            enabled = enabled,
                        )
                    },
                    onCheckedChange = { switchIconChecked = it },
                    enabled = enabled,
                )
            }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                ToggleChip(
                    label = { Text("Switch") },
                    secondaryLabel = { Text("Custom color") },
                    checked = switchIconCustomColorChecked,
                    toggleControl = {
                        Switch(
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colors.primary,
                                    checkedTrackColor = Color.Green,
                                    uncheckedThumbColor = Color.Red,
                                    uncheckedTrackColor = Color.Magenta,
                                ),
                            checked = switchIconCustomColorChecked,
                            enabled = enabled,
                        )
                    },
                    onCheckedChange = { switchIconCustomColorChecked = it },
                    enabled = enabled,
                )
            }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                ToggleChip(
                    label = { Text("Switch", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    secondaryLabel = {
                        Text("With secondary label", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    checked = switchIconWithSecondaryChecked,
                    // For Switch toggle controls the Wear Material UX guidance is to set the
                    // unselected toggle control color to
                    // ToggleChipDefaults.switchUncheckedIconColor() rather than the default.
                    colors =
                        ToggleChipDefaults.toggleChipColors(
                            uncheckedToggleControlColor =
                                ToggleChipDefaults.SwitchUncheckedIconColor
                        ),
                    toggleControl = {
                        Switch(
                            checked = switchIconWithSecondaryChecked,
                            enabled = enabled,
                        )
                    },
                    onCheckedChange = { switchIconWithSecondaryChecked = it },
                    appIcon = { DemoIcon(R.drawable.ic_airplanemode_active_24px) },
                    enabled = enabled,
                )
            }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                ToggleChip(
                    label = { Text("Switch", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    secondaryLabel = {
                        Text("With switchable icon", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    checked = switchIconWithIconChecked,
                    // For Switch  toggle controls the Wear Material UX guidance is to set the
                    // unselected toggle control color to
                    // ToggleChipDefaults.switchUncheckedIconColor() rather than the default.
                    colors =
                        ToggleChipDefaults.toggleChipColors(
                            uncheckedToggleControlColor =
                                ToggleChipDefaults.SwitchUncheckedIconColor
                        ),
                    toggleControl = {
                        Switch(
                            checked = switchIconWithIconChecked,
                            enabled = enabled,
                        )
                    },
                    onCheckedChange = { switchIconWithIconChecked = it },
                    appIcon = {
                        if (switchIconWithIconChecked) DemoIcon(R.drawable.ic_volume_up_24px)
                        else DemoIcon(R.drawable.ic_volume_off_24px)
                    },
                    enabled = enabled,
                )
            }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                ToggleChip(
                    label = {
                        Text(
                            "Long primary label split across multiple lines of text",
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Text(
                            "Long secondary label split across multiple lines of text",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    checked = switchIconWithIconChecked,
                    // For Switch  toggle controls the Wear Material UX guidance is to set the
                    // unselected toggle control color to
                    // ToggleChipDefaults.switchUncheckedIconColor() rather than the default.
                    colors =
                        ToggleChipDefaults.toggleChipColors(
                            uncheckedToggleControlColor =
                                ToggleChipDefaults.SwitchUncheckedIconColor
                        ),
                    toggleControl = {
                        Switch(
                            checked = switchIconWithIconChecked,
                            enabled = enabled,
                        )
                    },
                    onCheckedChange = { switchIconWithIconChecked = it },
                    enabled = enabled,
                )
            }
        }
        item {
            ListHeader {
                Text(
                    text = "Split Toggle Chips",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption1,
                    color = Color.White
                )
            }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                DemoSplitToggleChip(
                    enabled = enabled,
                    checked = splitWithSwitchIconChecked,
                    onCheckedChange = { splitWithSwitchIconChecked = it },
                    primaryLabel = "Primary Label",
                    // For Switch  toggle controls the Wear Material UX guidance is to set the
                    // unselected toggle control color to
                    // ToggleChipDefaults.switchUncheckedIconColor() rather than the default.
                    colors =
                        ToggleChipDefaults.splitToggleChipColors(
                            uncheckedToggleControlColor =
                                ToggleChipDefaults.SwitchUncheckedIconColor
                        ),
                ) {
                    Switch(
                        modifier = Modifier.semantics { contentDescription = "Primary Label" },
                        checked = splitWithSwitchIconChecked,
                        enabled = enabled,
                    )
                }
            }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                DemoSplitToggleChip(
                    enabled = enabled,
                    checked = splitWithCheckboxIconChecked,
                    onCheckedChange = { splitWithCheckboxIconChecked = it },
                    primaryLabel = "Primary Label",
                ) {
                    Checkbox(
                        modifier = Modifier.semantics { contentDescription = "Primary Label" },
                        checked = splitWithCheckboxIconChecked,
                        enabled = enabled,
                    )
                }
            }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                DemoSplitToggleChip(
                    enabled = enabled,
                    checked = splitWithSwitchIconChecked,
                    onCheckedChange = { splitWithSwitchIconChecked = it },
                    primaryLabel = "Long primary label split across maximum three lines of text",
                    secondaryLabel = "Long secondary label split across maximum two lines of text",
                    // For Switch  toggle controls the Wear Material UX guidance is to set the
                    // unselected toggle control color to
                    // ToggleChipDefaults.switchUncheckedIconColor() rather than the default.
                    colors =
                        ToggleChipDefaults.splitToggleChipColors(
                            uncheckedToggleControlColor =
                                ToggleChipDefaults.SwitchUncheckedIconColor
                        ),
                ) {
                    Switch(
                        modifier = Modifier.semantics { contentDescription = "Primary Label" },
                        checked = splitWithSwitchIconChecked,
                        enabled = enabled,
                    )
                }
            }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                ToggleChip(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    label = { Text("Chips enabled") },
                    // For Switch  toggle controls the Wear Material UX guidance is to set the
                    // unselected toggle control color to
                    // ToggleChipDefaults.switchUncheckedIconColor() rather than the default.
                    colors =
                        ToggleChipDefaults.toggleChipColors(
                            uncheckedToggleControlColor =
                                ToggleChipDefaults.SwitchUncheckedIconColor
                        ),
                    toggleControl = {
                        Switch(
                            checked = enabled,
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun DemoSplitToggleChip(
    enabled: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    primaryLabel: String,
    secondaryLabel: String? = null,
    colors: SplitToggleChipColors = ToggleChipDefaults.splitToggleChipColors(),
    content: @Composable BoxScope.() -> Unit,
) {
    val context = LocalContext.current

    SplitToggleChip(
        label = { Text(primaryLabel, maxLines = 3, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = {
            secondaryLabel?.let { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        },
        checked = checked,
        toggleControl = content,
        onCheckedChange = onCheckedChange,
        onClick = { Toast.makeText(context, "Text was clicked", Toast.LENGTH_SHORT).show() },
        colors = colors,
        enabled = enabled,
    )
}
