/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.foundation.Text
import androidx.ui.foundation.selection.Toggleable
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.foundation.selection.TriStateToggleable

@Sampled
@Composable
fun ToggleableSample() {
    var checked by state { false }
    Toggleable(value = checked, onValueChange = { checked = it }) {
        // content that you want to make toggleable
        Text(text = checked.toString())
    }
}

@Sampled
@Composable
fun TriStateToggleableSample() {
    var checked by state { ToggleableState.Indeterminate }
    TriStateToggleable(
        state = checked,
        onClick = {
            checked =
                if (checked == ToggleableState.On) {
                    ToggleableState.Off
                } else {
                    ToggleableState.On
                }
        }) {
        // content that you want to make toggleable
        Text(text = checked.toString())
    }
}