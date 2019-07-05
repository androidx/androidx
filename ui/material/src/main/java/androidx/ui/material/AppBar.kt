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
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.Dp
import androidx.ui.core.Semantics
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.foundation.ColoredRect
import androidx.ui.layout.Container
import androidx.ui.layout.FlexRow
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.Row
import androidx.ui.layout.WidthSpacer
import androidx.ui.material.surface.Surface
import androidx.ui.graphics.Color
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.MainAxisSize

/**
 * A Top App Bar displays information and actions relating to the current screen and is placed at
 * the top of the screen.
 * This version of the TopAppBar will produce a default bar with a navigation leading icon, an
 * optional title and a set of menu icons.
 *
 * Example usage:
 *     TopAppBar(
 *         title = "Title",
 *         color = +themeColor{ secondary }
 *     )
 *
 * @param title An optional title to display
 * @param color An optional color for the App Bar. By default [MaterialColors.primary] will be used.
 * @param icons An optional list of icons to display on the App Bar.
 */
@Composable
fun TopAppBar(
    title: String? = null,
    color: Color = +themeColor { primary },
    // TODO: work on menus
    icons: List<Dp> = emptyList()
) {
    TopAppBar(
        color = color,
        leadingIcon = { AppBarLeadingIcon() },
        titleTextLabel = {
            if (title != null) {
                TopAppBarTitleTextLabel(title)
            }
        },
        trailingIcons = { TopAppBarTrailingIcons(icons) }
    )
}

/**
 * A Top App Bar displays information and actions relating to the current screen and is placed at
 * the top of the screen.
 *
 * Example usage:
 *     TopAppBar(
 *         color = +themeColor{ secondary },
 *         leadingIcon = { MyNavIcon() },
 *         titleTextLabel = { Text(text = "Title") },
 *         trailingIcons = { TopAppBarTrailingIcons(icons) }
 *     )
 *
 * @param color An optional color for the App Bar. By default [MaterialColors.primary] will be used.
 * @param leadingIcon A composable lambda to be inserted in the Leading Icon space. This is usually
 * a navigation icon. A standard implementation is provided by [AppBarLeadingIcon].
 * @param titleTextLabel A composable lambda to be inserted in the title space. This is usually a
 * [Text] element. A standard implementation is provided by [TopAppBarTitleTextLabel]. Default text
 * styling [MaterialTypography.h6] will be used.
 * @param trailingIcons A composable lambda to be inserted at the end of the bar, usually containing
 * a collection of menu icons. A standard implementation is provided by [TopAppBarTrailingIcons].
 */
@Composable
fun TopAppBar(
    color: Color = +themeColor { primary },
    leadingIcon: @Composable() () -> Unit,
    titleTextLabel: @Composable() () -> Unit,
    trailingIcons: @Composable() () -> Unit
) {
    AppBar(color) {
        FlexRow(mainAxisAlignment = MainAxisAlignment.SpaceBetween) {
            inflexible {
                leadingIcon()
                WidthSpacer(width = 32.dp)
            }
            expanded(flex = 1f) {
                CurrentTextStyleProvider(value = +themeTextStyle { h6 }) {
                    titleTextLabel()
                }
            }
            inflexible {
                trailingIcons()
            }
        }
    }
}

/**
 * An empty App Bar that expands to the parent's width.
 *
 * For an App Bar that follows Material spec guidelines to be placed on the top of the screen, see
 * [TopAppBar].
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

/**
 * A component that displays a leading icon for an App Bar following Material spec guidelines.
 *
 * @see [AppBar]
 * @see [TopAppBar]
 */
@Composable
fun AppBarLeadingIcon() {
    // TODO: Replace with real icon button
    Semantics(testTag = "Leading icon") {
        FakeIcon(24.dp)
    }
}

/**
 * A component that displays a title as a [Text] element for placement within a Top App Bar
 * following Material spec guidelines.
 *
 * @see [TopAppBar]
 *
 * @param title A title String to display
 */
@Composable
fun TopAppBarTitleTextLabel(title: String) {
    Text(text = title)
}

/**
 * A component that displays a set of menu icons for placement within a Top App Bar following
 * Material spec guidelines.
 *
 * @see [TopAppBar]
 *
 * @param icons A list of icons to display
 */
@Composable
fun TopAppBarTrailingIcons(icons: List<Dp>) {
    TrailingIcons(
        numIcons = icons.size,
        maxIcons = MaxIconsInTopAppBar,
        icons = { index ->
            Semantics(testTag = "Trailing icon") {
                // TODO: Replace with real icon button
                FakeIcon(icons[index])
            }
        },
        overflowIcon = {
            Semantics(testTag = "Overflow icon") {
                FakeIcon(12.dp)
            }
        }
    )
}

// TODO: make public
@Composable
internal fun TrailingIcons(
    numIcons: Int,
    maxIcons: Int,
    icons: @Composable() (index: Int) -> Unit,
    overflowIcon: @Composable() () -> Unit
) {
    if (numIcons > 0) {
        Row(mainAxisSize = MainAxisSize.Min) {
            val needsOverflow = numIcons > maxIcons
            val iconsToDisplay = if (needsOverflow) maxIcons else numIcons
            for (index in 0 until iconsToDisplay) {
                WidthSpacer(width = 24.dp)
                icons(index)
            }
            if (needsOverflow) {
                WidthSpacer(width = 24.dp)
                overflowIcon()
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
// TODO: IR compiler bug avoids this being const
private val MaxIconsInTopAppBar = 2