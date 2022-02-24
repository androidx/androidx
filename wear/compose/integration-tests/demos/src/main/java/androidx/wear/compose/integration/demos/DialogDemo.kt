/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyListState
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Confirmation
import androidx.wear.compose.material.dialog.Dialog
import androidx.wear.compose.material.rememberScalingLazyListState

@Composable
fun DialogPowerOff(navigateBack: () -> Unit) {
    val scrollState = rememberScalingLazyListState()
    DialogDemo(scrollState) {
        Alert(
            title = {
                Text(
                    text = "Power off",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onBackground
                )
            },
            negativeButton = {
                Button(
                    onClick = navigateBack,
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    DemoIcon(resourceId = R.drawable.ic_clear_24px, contentDescription = "No")
                }
            },
            positiveButton = {
                Button(
                    onClick = navigateBack,
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    DemoIcon(resourceId = R.drawable.ic_check_24px, contentDescription = "Yes")
                }
            },
        ) {
            Text(
                text = "Are you sure?",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground,
            )
        }
    }
}

@Composable
fun DialogAccessLocation(navigateBack: () -> Unit) {
    val scrollState = rememberScalingLazyListState()
    DialogDemo(scrollState) {
        Alert(
            scrollState = scrollState,
            icon = {
                DemoIcon(
                    resourceId = R.drawable.ic_baseline_location_on_24,
                    contentDescription = "Location"
                )
            },
            title = {
                Text(
                    text = "Allow Bikemap to access this device's location?",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onBackground
                )
            },
            negativeButton = {
                Button(
                    onClick = navigateBack,
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    DemoIcon(resourceId = R.drawable.ic_clear_24px, contentDescription = "Cross")
                }
            },
            positiveButton = {
                Button(
                    onClick = navigateBack,
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    DemoIcon(resourceId = R.drawable.ic_check_24px, contentDescription = "Tick")
                }
            },
        )
    }
}

@Composable
fun DialogGrantPermission(
    navigateBack: () -> Unit
) {
    val scrollState = rememberScalingLazyListState()
    DialogDemo(scrollState) {
        Alert(
            scrollState = scrollState,
            icon = {
                DemoIcon(resourceId = R.drawable.ic_baseline_error_24, contentDescription = "Error")
            },
            title = { Text(
                text = "You need to grant location permission to use this app",
                textAlign = TextAlign.Center
            ) },
            contentPadding =
                PaddingValues(start = 10.dp, end = 10.dp, top = 0.dp, bottom = 16.dp),
        ) {
            item {
                Chip(
                    icon = {
                        DemoIcon(
                            resourceId = R.drawable.ic_baseline_settings_24,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    onClick = navigateBack,
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.width(124.dp),
                )
            }
        }
    }
}

@Composable
fun DialogLongChips(navigateBack: () -> Unit) {
    val scrollState = rememberScalingLazyListState()
    DialogDemo(scrollState) {
        Alert(
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 36.dp, bottom = 52.dp),
            scrollState = scrollState,
            title = {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center) {
                    Text(text = "Title that is quite long", textAlign = TextAlign.Center)
                }
            },
            message = {
                Text(
                    text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2
                )
            },
        ) {
            item {
                Chip(
                    icon = {
                        DemoIcon(resourceId = R.drawable.ic_check_24px, contentDescription = "Tick")
                    },
                    label = { Text("Allow access") },
                    onClick = navigateBack,
                    colors = ChipDefaults.primaryChipColors(),
                )
            }
            item {
                Chip(
                    icon = {
                        DemoIcon(
                            resourceId = R.drawable.ic_clear_24px,
                            contentDescription = "Cross")
                    },
                    label = { Text("Keep \"while app is in use\"") },
                    onClick = navigateBack,
                    colors = ChipDefaults.secondaryChipColors(),
                )
            }
        }
    }
}

@Composable
fun DialogSuccessConfirmation(navigateBack: () -> Unit) {
    DialogDemo {
        Confirmation(
            onTimeout = navigateBack,
            icon = {
                DemoIcon(
                    resourceId = R.drawable.ic_check_48px,
                    size = 48.dp,
                    contentDescription = "Tick")
            },
        ) {
            Text(
                text = "Success",
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DialogDemo(
    scrollState: ScalingLazyListState? = null,
    content: @Composable () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    // Launches the Alert Dialog by setting the value of showDialog.
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Chip(
            onClick = { showDialog = true },
            label = { Text("Show dialog") },
        )
    }
    if (showDialog) {
        Dialog(
            onDismissRequest = { showDialog = false },
            scrollState = scrollState,
            content = content
        )
    }
}
