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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.IconToggleButton
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.ToggleButton
import androidx.xr.glimmer.list.GlimmerLazyColumn
import androidx.xr.glimmer.samples.ButtonSampleUsage
import androidx.xr.glimmer.samples.IconButtonSample
import androidx.xr.glimmer.samples.IconToggleButtonSample
import androidx.xr.glimmer.samples.LargeToggleButtonSample
import androidx.xr.glimmer.samples.ToggleButtonSample
import androidx.xr.glimmer.samples.ToggleButtonWithLeadingIconSample
import androidx.xr.glimmer.samples.ToggleButtonWithTrailingIconSample

internal val ButtonDemos =
    listOf(
        ComposableDemo("Buttons") { ButtonSampleUsage() },
        ComposableDemo("IconButton") { IconButtonSample() },
        ComposableDemo("ToggleButtons") { ToggleButtonsDemo() },
        ComposableDemo("IconToggleButton") { IconToggleButtonsDemo() },
    )

@Composable
fun ToggleButtonsDemo() {
    GlimmerLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
    ) {
        item { ToggleButtonSample() }
        item { LargeToggleButtonSample() }
        item { ToggleButtonWithLeadingIconSample() }
        item { ToggleButtonWithTrailingIconSample() }
        item { DisabledToggleButtonDemo(checked = false) }
        item { DisabledToggleButtonDemo(checked = true) }
    }
}

@Composable
fun IconToggleButtonsDemo() {
    GlimmerLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
    ) {
        item { IconToggleButtonSample() }
        item { DisabledIconToggleButtonDemo(checked = false) }
        item { DisabledIconToggleButtonDemo(checked = true) }
    }
}

@Composable
private fun DisabledToggleButtonDemo(checked: Boolean) {
    val text = if (checked) "Disabled checked" else "Disabled unchecked"
    ToggleButton(checked = checked, enabled = false, onCheckedChange = {}) { Text(text) }
}

@Composable
private fun DisabledIconToggleButtonDemo(checked: Boolean) {
    IconToggleButton(checked = checked, enabled = false, onCheckedChange = {}) {
        Icon(
            imageVector = if (checked) Icons.FavoriteIcon else Icons.OutlinedFavoriteIcon,
            contentDescription = "Localized description",
        )
    }
}
