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

package androidx.compose.material3.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.IconButtonWidthOption.Companion.Narrow
import androidx.compose.material3.IconButtonDefaults.IconButtonWidthOption.Companion.Wide
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ButtonGroupDemos() {
    val checked = remember { mutableStateListOf(false, false, false, false, false, false, false) }
    val interactionSources = List(7) { MutableInteractionSource() }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            ButtonGroup(
                overflowIndicator = { menuState ->
                    FilledIconToggleButton(
                        checked = false,
                        onCheckedChange = {
                            if (menuState.isExpanded) {
                                menuState.dismiss()
                            } else {
                                menuState.show()
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Localized description",
                        )
                    }
                },
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                customItem(
                    buttonGroupContent = {
                        FilledIconToggleButton(
                            interactionSource = interactionSources[0],
                            checked = checked[0],
                            onCheckedChange = { checked[0] = it },
                            shapes = IconButtonDefaults.toggleableShapes(),
                            modifier = Modifier.animateWidth(interactionSources[0]),
                        ) {
                            if (checked[0]) {
                                Icon(
                                    Icons.Filled.Bluetooth,
                                    contentDescription = "Localized description",
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Bluetooth,
                                    contentDescription = "Localized description",
                                )
                            }
                        }
                    },
                    menuContent = { state ->
                        // Use expressive menu when available
                        DropdownMenuItem(
                            text = { Text("Bluetooth") },
                            modifier =
                                Modifier.background(
                                    if (checked[0]) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        Color.Unspecified
                                    }
                                ),
                            leadingIcon = {
                                if (checked[0]) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Localized description",
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Bluetooth,
                                        contentDescription = "Localized description",
                                    )
                                }
                            },
                            onClick = {
                                checked[0] = !checked[0]
                                state.dismiss()
                            },
                        )
                    },
                )
                customItem(
                    buttonGroupContent = {
                        FilledIconToggleButton(
                            interactionSource = interactionSources[1],
                            checked = checked[1],
                            onCheckedChange = { checked[1] = it },
                            shapes = IconButtonDefaults.toggleableShapes(),
                            modifier =
                                Modifier.width(IconButtonDefaults.smallContainerSize(Wide).width)
                                    .animateWidth(interactionSources[1]),
                        ) {
                            if (checked[1]) {
                                Icon(
                                    Icons.Filled.Alarm,
                                    contentDescription = "Localized description",
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Alarm,
                                    contentDescription = "Localized description",
                                )
                            }
                        }
                    },
                    menuContent = { state ->
                        // Use expressive menu when available
                        DropdownMenuItem(
                            text = { Text("Alarm") },
                            modifier =
                                Modifier.background(
                                    if (checked[1]) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        Color.Unspecified
                                    }
                                ),
                            leadingIcon = {
                                if (checked[0]) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Localized description",
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Alarm,
                                        contentDescription = "Localized description",
                                    )
                                }
                            },
                            onClick = {
                                checked[1] = !checked[1]
                                state.dismiss()
                            },
                        )
                    },
                )
                customItem(
                    buttonGroupContent = {
                        ToggleButton(
                            checked = checked[2],
                            onCheckedChange = { checked[2] = it },
                            modifier = Modifier.width(125.dp).animateWidth(interactionSources[2]),
                            interactionSource = interactionSources[2],
                        ) {
                            if (checked[2]) {
                                Icon(
                                    Icons.Filled.DoNotDisturbOn,
                                    contentDescription = "Localized description",
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.DoNotDisturbOn,
                                    contentDescription = "Localized description",
                                )
                            }
                            Spacer(
                                modifier =
                                    Modifier.size(
                                        ButtonDefaults.iconSpacingFor(ButtonDefaults.MinHeight)
                                    )
                            )
                            Text("focus")
                        }
                    },
                    menuContent = { state ->
                        // Use expressive menu when available
                        DropdownMenuItem(
                            text = { Text("Focus") },
                            modifier =
                                Modifier.background(
                                    if (checked[2]) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        Color.Unspecified
                                    }
                                ),
                            leadingIcon = {
                                if (checked[2]) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Localized description",
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.DoNotDisturbOn,
                                        contentDescription = "Localized description",
                                    )
                                }
                            },
                            onClick = {
                                checked[2] = !checked[2]
                                state.dismiss()
                            },
                        )
                    },
                )
                customItem(
                    buttonGroupContent = {
                        FilledIconToggleButton(
                            checked = checked[3],
                            onCheckedChange = { checked[3] = it },
                            interactionSource = interactionSources[3],
                            modifier =
                                Modifier.width(IconButtonDefaults.smallContainerSize(Narrow).width)
                                    .animateWidth(interactionSources[3]),
                        ) {
                            if (checked[3]) {
                                Icon(
                                    Icons.Filled.FlashlightOn,
                                    contentDescription = "Localized description",
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.FlashlightOn,
                                    contentDescription = "Localized description",
                                )
                            }
                        }
                    },
                    menuContent = { state ->
                        // Use expressive menu when available
                        DropdownMenuItem(
                            text = { Text("Flashlight") },
                            modifier =
                                Modifier.background(
                                    if (checked[3]) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        Color.Unspecified
                                    }
                                ),
                            leadingIcon = {
                                if (checked[3]) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Localized description",
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.FlashlightOn,
                                        contentDescription = "Localized description",
                                    )
                                }
                            },
                            onClick = {
                                checked[3] = !checked[3]
                                state.dismiss()
                            },
                        )
                    },
                )
                customItem(
                    buttonGroupContent = {
                        ToggleButton(
                            checked = checked[4],
                            shapes =
                                ToggleButtonDefaults.shapes(
                                    ToggleButtonDefaults.squareShape,
                                    ToggleButtonDefaults.pressedShape,
                                    ToggleButtonDefaults.checkedShape,
                                ),
                            onCheckedChange = { checked[4] = it },
                            interactionSource = interactionSources[4],
                            modifier = Modifier.animateWidth(interactionSources[4]),
                        ) {
                            if (checked[4]) {
                                Icon(
                                    Icons.Filled.Wifi,
                                    contentDescription = "Localized description",
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Wifi,
                                    contentDescription = "Localized description",
                                )
                            }
                        }
                    },
                    menuContent = { state ->
                        // Use expressive menu when available
                        DropdownMenuItem(
                            modifier =
                                Modifier.background(
                                    if (checked[4]) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        Color.Unspecified
                                    }
                                ),
                            text = { Text("Wifi") },
                            leadingIcon = {
                                if (checked[4]) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Localized description",
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Wifi,
                                        contentDescription = "Localized description",
                                    )
                                }
                            },
                            onClick = {
                                checked[4] = !checked[4]
                                state.dismiss()
                            },
                        )
                    },
                )
                customItem(
                    buttonGroupContent = {
                        ToggleButton(
                            checked = checked[5],
                            shapes =
                                ToggleButtonDefaults.shapes(
                                    ToggleButtonDefaults.squareShape,
                                    ToggleButtonDefaults.pressedShape,
                                    ToggleButtonDefaults.checkedShape,
                                ),
                            onCheckedChange = { checked[5] = it },
                            interactionSource = interactionSources[5],
                            modifier = Modifier.animateWidth(interactionSources[5]),
                        ) {
                            if (checked[5]) {
                                Icon(
                                    Icons.Filled.Wallet,
                                    contentDescription = "Localized description",
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Wallet,
                                    contentDescription = "Localized description",
                                )
                            }
                        }
                    },
                    menuContent = { state ->
                        // Use expressive menu when available
                        DropdownMenuItem(
                            text = { Text("Wallet") },
                            modifier =
                                Modifier.background(
                                    if (checked[5]) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        Color.Unspecified
                                    }
                                ),
                            leadingIcon = {
                                if (checked[5]) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Localized description",
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Wallet,
                                        contentDescription = "Localized description",
                                    )
                                }
                            },
                            onClick = {
                                checked[5] = !checked[5]
                                state.dismiss()
                            },
                        )
                    },
                )
                customItem(
                    buttonGroupContent = {
                        ToggleButton(
                            checked = checked[6],
                            shapes =
                                ToggleButtonDefaults.shapes(
                                    ToggleButtonDefaults.squareShape,
                                    ToggleButtonDefaults.pressedShape,
                                    ToggleButtonDefaults.checkedShape,
                                ),
                            onCheckedChange = { checked[6] = it },
                            interactionSource = interactionSources[6],
                            modifier = Modifier.animateWidth(interactionSources[6]),
                        ) {
                            if (checked[6]) {
                                Icon(
                                    Icons.Filled.Calculate,
                                    contentDescription = "Localized description",
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Calculate,
                                    contentDescription = "Localized description",
                                )
                            }
                        }
                    },
                    menuContent = { state ->
                        // Use expressive menu when available
                        DropdownMenuItem(
                            enabled = true,
                            text = { Text("Calculator") },
                            modifier =
                                Modifier.background(
                                    if (checked[6]) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        Color.Unspecified
                                    }
                                ),
                            leadingIcon = {
                                if (checked[6]) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Localized description",
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Calculate,
                                        contentDescription = "Localized description",
                                    )
                                }
                            },
                            onClick = {
                                checked[6] = !checked[6]
                                state.dismiss()
                            },
                        )
                    },
                )
            }
        }
    }
}
