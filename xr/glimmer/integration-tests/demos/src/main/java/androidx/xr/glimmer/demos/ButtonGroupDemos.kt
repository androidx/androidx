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

package androidx.xr.glimmer.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.ButtonGroup
import androidx.xr.glimmer.ButtonGroupState
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.IconButton
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.demos.Icons.FavoriteIcon
import androidx.xr.glimmer.rememberButtonGroupState
import kotlinx.coroutines.launch

val ButtonGroupDemos =
    listOf(
        ComposableDemo("ButtonGroup: Text Buttons") { ButtonGroupTextItems() },
        ComposableDemo("ButtonGroup: Icon Buttons") { ButtonGroupIconItems() },
        ComposableDemo("ButtonGroup: Text and Icon Buttons") { ButtonGroupMixedItemTypes() },
    )

@Composable
fun ButtonGroupTextItems() {
    val state = rememberButtonGroupState()
    val buttonCount = remember { mutableIntStateOf(5) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ButtonGroup(Modifier.fillMaxWidth(), state) {
            repeat(buttonCount.intValue) { TextButton() }
        }
        ButtonGroupScrollControls(state)
        ButtonGroupAddRemoveControls(buttonCount)
    }
}

@Composable
fun ButtonGroupIconItems() {
    val state = rememberButtonGroupState()
    val buttonCount = remember { mutableIntStateOf(5) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ButtonGroup(Modifier.fillMaxWidth(), state) {
            repeat(buttonCount.intValue) { IconButton() }
        }
        ButtonGroupScrollControls(state)
        ButtonGroupAddRemoveControls(buttonCount)
    }
}

@Composable
fun ButtonGroupMixedItemTypes() {
    val state = rememberButtonGroupState(initialItemIndex = 5)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ButtonGroup(Modifier.fillMaxWidth(), state) {
            IconButton()
            TextButton()
            TextButton()
            IconButton()
            IconButton()
            TextButton()
            IconButton()
            IconButton()
            IconButton()
            TextButton()
            IconButton()
        }
        ButtonGroupScrollControls(state)
    }
}

@Composable
private fun TextButton() {
    Button(onClick = {}) { Text("Text") }
}

@Composable
private fun IconButton() {
    IconButton(onClick = {}) { Icon(FavoriteIcon, "Localized Description") }
}

/** A row of buttons for programmatically scrolling through the Buttons in this [state]. */
@Composable
private fun ButtonGroupScrollControls(state: ButtonGroupState) {
    val scope = rememberCoroutineScope()
    val canScrollBackward by remember { derivedStateOf { state.currentItemIndex > 0 } }
    val canScrollForward by remember {
        derivedStateOf { state.currentItemIndex < state.itemCount - 1 }
    }
    ButtonGroupSampleControls {
        Button(
            enabled = canScrollBackward,
            onClick = { scope.launch { state.animateScrollToItem(0) } },
        ) {
            Text("«")
        }
        Button(
            enabled = canScrollBackward,
            onClick = { scope.launch { state.animateScrollToItem(state.currentItemIndex - 1) } },
        ) {
            Text("‹1")
        }
        Text("Current index: ${state.currentItemIndex}")
        Button(
            enabled = canScrollForward,
            onClick = { scope.launch { state.animateScrollToItem(state.currentItemIndex + 1) } },
        ) {
            Text("›1")
        }
        Button(
            enabled = canScrollForward,
            onClick = { scope.launch { state.animateScrollToItem(state.itemCount - 1) } },
        ) {
            Text("»")
        }
    }
}

/** A row of buttons that allows adding/removing Buttons from the ButtonGroup. */
@Composable
private fun ButtonGroupAddRemoveControls(buttonCount: MutableIntState) {
    ButtonGroupSampleControls {
        Button(onClick = { buttonCount.intValue-- }, enabled = buttonCount.intValue > 0) {
            Text("-")
        }
        Text("${buttonCount.intValue} buttons")
        Button(onClick = { buttonCount.intValue++ }) { Text("+") }
    }
}

@Composable
private fun ButtonGroupSampleControls(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
