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

package androidx.xr.glimmer.demos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.tooling.preview.Preview
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.ListItem
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.list.VerticalList
import kotlinx.coroutines.delay

internal val FocusDemos =
    listOf(
        ComposableDemo("List") { FocusableListSample() },
        ComposableDemo("List + Initial Focus") { FocusableListInitialFocusSample() },
        ComposableDemo("VerticalList") { VerticalListFocusSample() },
        ComposableDemo("VerticalList + Initial Focus") { ListFocusInitialFocusSample() },
        ComposableDemo("Show/Hide + Focus Restoration") { ShowHideFocusRestorationSample() },
    )

@Composable
private fun FocusableListSample() {
    Column {
        ListItem { Text("Button 1") }
        ListItem { Text("Button 2") }
        ListItem { Text("Button 3") }
    }
}

@Composable
private fun FocusableListInitialFocusSample() {
    val initialFocus = remember { FocusRequester() }
    Column(
        Modifier.focusProperties {
                onEnter = {
                    initialFocus.requestFocus()
                    cancelFocusChange()
                }
            }
            .focusGroup()
    ) {
        Text("Initial Focus on Button 2")
        ListItem { Text("Button 1") }
        ListItem(Modifier.focusRequester(initialFocus)) { Text("Button 2") }
        ListItem { Text("Button 3") }
    }
}

@Composable
private fun VerticalListFocusSample() {
    VerticalList { items(20) { ListItem { Text("Button ${it + 1}") } } }
}

@Composable
private fun ListFocusInitialFocusSample() {
    Column {
        Text("Initial Focus on Button 3")
        val initialFocus = remember { FocusRequester() }
        VerticalList(
            Modifier.focusProperties {
                onEnter = {
                    initialFocus.requestFocus()
                    cancelFocusChange()
                }
            }
        ) {
            items(20) {
                ListItem(if (it == 2) Modifier.focusRequester(initialFocus) else Modifier) {
                    Text("Button ${it + 1}")
                }
            }
        }
    }
}

@Composable
private fun ShowHideFocusRestorationSample() {
    var visible by remember { mutableStateOf(true) }

    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Column(Modifier.focusRestorer().focusGroup()) {
            ListItem { Text("Button 1") }
            ListItem { Text("Button 2") }
            ListItem { Text("Button 3") }
        }
    }

    LaunchedEffect(visible) {
        delay(3000L)
        visible = !visible
    }
}

@Preview
@Composable
private fun FocusableList() {
    GlimmerTheme { FocusableListSample() }
}

@Preview
@Composable
private fun LazyList() {
    GlimmerTheme { VerticalListFocusSample() }
}
