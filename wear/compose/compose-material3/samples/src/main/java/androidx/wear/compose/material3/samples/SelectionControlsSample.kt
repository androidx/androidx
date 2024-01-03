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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.wear.compose.material3.Checkbox
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.Switch

@Sampled
@Composable
fun CheckboxSample() {
    var checked by remember { mutableStateOf(false) }
    Checkbox(checked = checked, onCheckedChange = { checked = it })
}

@Sampled
@Composable
fun SwitchSample() {
    var checked by remember { mutableStateOf(false) }
    Switch(checked = checked, onCheckedChange = { checked = it })
}

@Sampled
@Composable
fun RtlSwitchSample() {
    var checked by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Switch(checked = checked, onCheckedChange = { checked = it })
    }
}

@Sampled
@Composable
fun RadioButtonSample() {
    var selected by remember { mutableStateOf(false) }
    RadioButton(selected = selected, onClick = { selected = !selected })
}
