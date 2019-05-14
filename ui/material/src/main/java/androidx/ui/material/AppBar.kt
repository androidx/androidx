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

package androidx.ui.material

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.baseui.ColoredRect
import androidx.ui.core.Dp
import androidx.ui.core.Semantics
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.layout.Container
import androidx.ui.layout.FlexRow
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.Row
import androidx.ui.layout.WidthSpacer
import androidx.ui.material.surface.Surface
import androidx.ui.graphics.Color
import androidx.ui.layout.EdgeInsets

/**
 * A Top App Bar displays information and actions relating to the current screen and is placed at
 * the top of the screen.
 *
 * @param title An optional title to display
 * @param color An optional color for the App Bar. By default
 * [androidx.ui.material.MaterialColors.primary] will be used.
 * @param icons An optional list of icons to display on the App Bar.
 */
@Composable
fun TopAppBar(
    title: String? = null,
    color: Color = +themeColor { primary },
    // TODO: replace with icons?
    icons: List<Dp> = emptyList()
) {
    AppBar(color) {
        FlexRow(mainAxisAlignment = MainAxisAlignment.SpaceBetween) {
            inflexible {
                LeadingIcon()
                WidthSpacer(width = 32.dp)
            }
            expanded(flex = 1f) {
                TitleTextLabel(title)
            }
            inflexible {
                TrailingIcons(icons)
            }
        }
    }
}

/**
 * An empty App Bar that expands to the parent's width.
 */
@Composable
fun AppBar(color: Color, @Children children: @Composable() () -> Unit) {
    Semantics(
        container = true
    ) {
        Surface(color = color) {
            Container(height = RegularHeight, expanded = true, padding = EdgeInsets(Padding)) {
                children()
            }
        }
    }
}

// TODO: Expose as a public API piece?
@Composable
internal fun LeadingIcon() {
    // TODO: Replace with real icon button
    Semantics(testTag = "Leading icon") {
        FakeIcon(24.dp)
    }
}

@Composable
internal fun TitleTextLabel(title: String?) {
    if (title != null) {
        val style = +themeTextStyle { h6 }
        Semantics(testTag = "Title text label") {
            Text(text = title, style = style)
        }
    }
}

@Composable
internal fun TrailingIcons(icons: List<Dp>) {
    if (icons.isNotEmpty()) {
        Row {
            icons.forEachIndexed { index, dp ->
                if (index < MaxIconsInTopAppBar) {
                    if (index > 0) {
                        WidthSpacer(width = 24.dp)
                    }
                    Semantics(testTag = "Trailing icon") {
                        FakeIcon(dp)
                    }
                } else if (index == MaxIconsInTopAppBar) {
                    // Overflow menu
                    WidthSpacer(width = 24.dp)
                    Semantics(testTag = "Overflow icon") {
                        FakeIcon(12.dp)
                    }
                }
            }
        }
    }
}

// TODO: remove
@Composable
internal fun FakeIcon(size: Dp) {
    ColoredRect(color = Color(0xFFFFFFFF.toInt()), width = size, height = 24.dp)
}

private val RegularHeight = 56.dp
private val Padding = 16.dp
private const val MaxIconsInTopAppBar = 2