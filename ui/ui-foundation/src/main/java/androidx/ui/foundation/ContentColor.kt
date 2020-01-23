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

package androidx.ui.foundation

import androidx.compose.Composable
import androidx.compose.Providers
import androidx.compose.ambientOf
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.graphics.Color
import androidx.ui.text.TextStyle

/**
 * Returns the preferred content color at the call site's position in the hierarchy.
 *
 * This color should be used for any typography / iconography, to ensure that the color of these
 * adjusts when the background color changes. For example, on a dark background, text should be
 * light, and on a light background, text should be dark.
 *
 * @return the preferred content color specified by a parent, defaulting to [Color.Black] if
 * unspecified.
 */
@Composable
fun contentColor() = ContentColorAmbient.current

/**
 * Sets [color] as the preferred content color for [children].
 * This color can then be retrieved inside children by using [contentColor]. Typography and
 * iconography should use this value as their default color.
 */
@Composable
fun ProvideContentColor(color: Color, children: @Composable() () -> Unit) {
    Providers(ContentColorAmbient provides color) {
        // TODO: we probably want to instead provide a Text component that queries contentColor()
        // instead of needing to manually merge this style.
        CurrentTextStyleProvider(value = TextStyle(color = color), children = children)
    }
}

private val ContentColorAmbient = ambientOf { Color.Black }
