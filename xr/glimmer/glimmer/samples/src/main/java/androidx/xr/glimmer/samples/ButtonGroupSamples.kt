/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.glimmer.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.ButtonGroup
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.rememberButtonGroupState
import kotlinx.coroutines.launch

@Sampled
@Composable
fun ButtonGroupSample() {
    ButtonGroup(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = {}) { Text("Button 1") }
        Button(onClick = {}) { Text("Button 2") }
        Button(onClick = {}) { Text("Button 3") }
        Button(onClick = {}) { Text("Button 4") }
        Button(onClick = {}) { Text("Button 5") }
    }
}

@Sampled
@Composable
fun ButtonGroupControlCurrentItemSample() {
    val scope = rememberCoroutineScope()
    val state = rememberButtonGroupState()
    ButtonGroup(modifier = Modifier.fillMaxWidth(), state = state) {
        Button(onClick = { scope.launch { state.animateScrollToItem(1) } }) {
            Text("Select last item")
        }
        Button(onClick = { scope.launch { state.animateScrollToItem(0) } }) {
            Text("Select first item")
        }
    }
}

@Preview
@Composable
private fun ButtonGroupPreview() {
    GlimmerTheme { ButtonGroupSample() }
}
