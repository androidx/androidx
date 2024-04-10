/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.integration.demos.common.ScalingLazyColumnWithRSB
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.RadioButton
import androidx.wear.compose.material.RadioButtonDefaults
import androidx.wear.compose.material.SelectableChip
import androidx.wear.compose.material.SelectableChipDefaults
import androidx.wear.compose.material.SplitSelectableChip
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults

@Composable
fun SelectableChips(
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    description: String = "Selectable Chips"
) {
    val applicationContext = LocalContext.current
    val scrollState: ScalingLazyListState = rememberScalingLazyListState()
    var enabled by remember { mutableStateOf(true) }

    var radioIconSelected by remember { mutableStateOf(true) }
    var radioIconWithSecondarySelected by remember { mutableStateOf(true) }
    var splitWithRadioIconSelected by remember { mutableStateOf(true) }

    ScalingLazyColumnWithRSB(
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
                // Call the selectionControl variation, with default selectionControl = RadioButton
                SelectableChip(
                    selected = radioIconSelected,
                    onClick = { radioIconSelected = it },
                    label = {
                        Text("Radio", maxLines = 2, overflow = TextOverflow.Ellipsis)
                    },
                    enabled = enabled,
                )
            }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                SelectableChip(
                    selected = radioIconWithSecondarySelected,
                    onClick = { radioIconWithSecondarySelected = it },
                    label = {
                        Text(
                            "RadioIcon",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Text("CustomColor", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    selectionControl = {
                        RadioButton(
                            selected = radioIconWithSecondarySelected,
                            enabled = enabled,
                            colors = RadioButtonDefaults.colors(
                                selectedRingColor = MaterialTheme.colors.primary,
                                selectedDotColor = Color.Green,
                                unselectedRingColor = Color.Magenta,
                                unselectedDotColor = Color.Red,
                            ),
                        )
                    },
                    enabled = enabled,
                    colors = SelectableChipDefaults.selectableChipColors(
                        selectedSelectionControlColor = AlternatePrimaryColor3,
                        selectedEndBackgroundColor = AlternatePrimaryColor3.copy(alpha = 0.325f)
                    )
                )
            }
        }
        item {
            ListHeader {
                Text(
                    text = "Split Selectable Chips",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption1,
                    color = Color.White
                )
            }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                // Call the selectionControl variation, with default selectionControl = RadioButton
                SplitSelectableChip(
                    selected = splitWithRadioIconSelected,
                    onClick = { splitWithRadioIconSelected = it },
                    label = { Text("Split with Radio") },
                    onBodyClick = {
                        Toast.makeText(
                            applicationContext, "Body was clicked",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    enabled = enabled,
                )
            }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                ToggleChip(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    label = {
                        Text("Chips enabled")
                    },
                    // For Switch  toggle controls the Wear Material UX guidance is to set the
                    // unselected toggle control color to
                    // ToggleChipDefaults.switchUncheckedIconColor() rather than the default.
                    colors = ToggleChipDefaults.toggleChipColors(
                        uncheckedToggleControlColor = ToggleChipDefaults
                            .SwitchUncheckedIconColor
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
