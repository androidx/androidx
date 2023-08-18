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
import androidx.wear.compose.material3.Checkbox
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.Switch
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.CheckboxSample
import androidx.wear.compose.material3.samples.RadioButtonSample
import androidx.wear.compose.material3.samples.RtlSwitchSample
import androidx.wear.compose.material3.samples.SwitchSample

@Composable
fun CheckboxDemos() {
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
                CheckboxSample()
                Spacer(modifier = Modifier.width(10.dp))
                var checked by remember { mutableStateOf(true) }
                Checkbox(checked = checked, onCheckedChange = {
                    checked = it
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
fun SwitchDemos() {
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
                SwitchSample()
                Spacer(modifier = Modifier.width(10.dp))
                var checked by remember { mutableStateOf(true) }
                Switch(checked = checked, onCheckedChange = {
                    checked = it
                })
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
        item {
            ListHeader { Text(text = "RTL Switch") }
        }
        item {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row {
                    var checked by remember { mutableStateOf(true) }
                    Switch(checked = checked, onCheckedChange = {
                        checked = it
                    })
                    Spacer(modifier = Modifier.width(10.dp))
                    RtlSwitchSample()
                }
            }
        }
    }
}

@Composable
fun RadioButtonDemos() {
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
                RadioButtonSample()
                Spacer(modifier = Modifier.width(10.dp))
                var selected by remember { mutableStateOf(true) }
                RadioButton(selected = selected, onClick = {
                    selected = !selected
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
