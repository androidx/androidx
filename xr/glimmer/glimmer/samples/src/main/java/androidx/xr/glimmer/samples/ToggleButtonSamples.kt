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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.xr.glimmer.ButtonSize
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.ToggleButton

@Sampled
@Composable
fun ToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    val text = if (checked) "Toggle on" else "Toggle off"
    ToggleButton(checked = checked, onCheckedChange = { checked = it }) { Text(text) }
}

@Sampled
@Composable
fun LargeToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    val text = if (checked) "Toggle on" else "Toggle off"
    ToggleButton(
        checked = checked,
        buttonSize = ButtonSize.Large,
        onCheckedChange = { checked = it },
    ) {
        Text(text)
    }
}

@Sampled
@Composable
fun ToggleButtonWithLeadingIconSample() {
    var checked by remember { mutableStateOf(false) }
    val text = if (checked) "Toggle on" else "Toggle off"
    ToggleButton(
        checked = checked,
        leadingIcon = {
            Icon(if (checked) FavoriteIcon else OutlinedFavoriteIcon, "Localized description")
        },
        onCheckedChange = { checked = it },
    ) {
        Text(text)
    }
}

@Sampled
@Composable
fun ToggleButtonWithTrailingIconSample() {
    var checked by remember { mutableStateOf(false) }
    val text = if (checked) "Toggle on" else "Toggle off"
    ToggleButton(
        checked = checked,
        trailingIcon = {
            Icon(if (checked) FavoriteIcon else OutlinedFavoriteIcon, "Localized description")
        },
        onCheckedChange = { checked = it },
    ) {
        Text(text)
    }
}

@Preview
@Composable
private fun ToggleButtonPreview() {
    GlimmerTheme { ToggleButtonSample() }
}

@Preview
@Composable
private fun LargeToggleButtonPreview() {
    GlimmerTheme { LargeToggleButtonSample() }
}

@Preview
@Composable
private fun ToggleButtonWithLeadingIconPreview() {
    GlimmerTheme { ToggleButtonWithLeadingIconSample() }
}

@Preview
@Composable
private fun ToggleButtonWithTrailingIconPreview() {
    GlimmerTheme { ToggleButtonWithTrailingIconSample() }
}
