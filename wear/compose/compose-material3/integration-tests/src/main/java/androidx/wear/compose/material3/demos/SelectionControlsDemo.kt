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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.integration.demos.common.DemoCategory
import androidx.wear.compose.material3.Checkbox
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.Switch
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.CheckboxSample
import androidx.wear.compose.material3.samples.RadioButtonSample
import androidx.wear.compose.material3.samples.RtlSwitchSample
import androidx.wear.compose.material3.samples.SwitchSample

val selectionControlsDemos = listOf(
    DemoCategory(
        "Samples",
        listOf(
            ComposableDemo("Checkbox sample") {
                Centralize(Modifier.padding(horizontal = 10.dp)) {
                    CheckboxSample()
                }
            },
            ComposableDemo("Switch sample") {
                Centralize(Modifier.padding(horizontal = 10.dp)) {
                    SwitchSample()
                }
            },
            ComposableDemo("Rtl Switch sample") {
                Centralize(Modifier.padding(horizontal = 10.dp)) {
                    RtlSwitchSample()
                }
            },
            ComposableDemo("RadioButton sample") {
                Centralize(Modifier.padding(horizontal = 10.dp)) {
                    RadioButtonSample()
                }
            },
        )
    ),
    DemoCategory("Demos", listOf(
        ComposableDemo("Checkbox demos") {
            CheckboxDemos()
        },
        ComposableDemo("Switch demos") {
            SwitchDemos()
        },
        ComposableDemo("RadioButton demos") {
            RadioButtonDemos()
        }
    ))
)

@Composable
private fun CheckboxDemos() {
    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            ListHeader { Text(text = "Checkbox") }
        }
        item {
            Row {
                var checked1 by remember { mutableStateOf(false) }
                Checkbox(checked = checked1, onCheckedChange = {
                    checked1 = it
                })
                Spacer(modifier = Modifier.width(10.dp))
                var checked2 by remember { mutableStateOf(true) }
                Checkbox(checked = checked2, onCheckedChange = {
                    checked2 = it
                })
            }
        }
        item {
            ListHeader { Text(text = "Disabled Checkbox") }
        }
        item {
            Row {
                Checkbox(
                    checked = false,
                    enabled = false
                )
                Spacer(modifier = Modifier.width(10.dp))
                Checkbox(
                    checked = true,
                    enabled = false
                )
            }
        }
    }
}

@Composable
private fun SwitchDemos() {
    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            ListHeader { Text(text = "Switch") }
        }
        item {
            Row {
                var checked1 by remember { mutableStateOf(false) }
                Switch(checked = checked1, onCheckedChange = {
                    checked1 = it
                })
                Spacer(modifier = Modifier.width(10.dp))
                var checked2 by remember { mutableStateOf(true) }
                Switch(checked = checked2, onCheckedChange = {
                    checked2 = it
                })
            }
        }
        item {
            ListHeader { Text(text = "RTL Switch") }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row {
                    var checked1 by remember { mutableStateOf(true) }
                    Switch(checked = checked1, onCheckedChange = {
                        checked1 = it
                    })
                    Spacer(modifier = Modifier.width(10.dp))
                    var checked2 by remember { mutableStateOf(false) }
                    Switch(checked = checked2, onCheckedChange = {
                        checked2 = it
                    })
                }
            }
        }
        item {
            ListHeader { Text(text = "Disabled Switch") }
        }
        item {
            Row {
                Switch(
                    checked = false,
                    enabled = false
                )
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    checked = true,
                    enabled = false
                )
            }
        }
    }
}

@Composable
private fun RadioButtonDemos() {
    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            ListHeader { Text(text = "RadioButton") }
        }
        item {
            Row {
                var selected1 by remember { mutableStateOf(false) }
                RadioButton(selected = selected1, onClick = {
                    selected1 = !selected1
                })
                Spacer(modifier = Modifier.width(10.dp))
                var selected2 by remember { mutableStateOf(true) }
                RadioButton(selected = selected2, onClick = {
                    selected2 = !selected2
                })
            }
        }
        item {
            ListHeader { Text(text = "Disabled Radio") }
        }
        item {
            Row {
                RadioButton(
                    selected = false,
                    enabled = false
                )
                Spacer(modifier = Modifier.width(10.dp))
                RadioButton(
                    selected = true,
                    enabled = false
                )
            }
        }
    }
}
