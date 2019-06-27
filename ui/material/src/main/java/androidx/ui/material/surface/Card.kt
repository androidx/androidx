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

package androidx.ui.material.surface

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.baseui.shape.RectangleShape
import androidx.ui.baseui.shape.Shape
import androidx.ui.baseui.shape.border.Border
import androidx.ui.core.Dp
import androidx.ui.core.dp
import androidx.ui.graphics.Color
import androidx.ui.material.MaterialColors
import androidx.ui.material.themeColor

/**
 * Cards are [Surface]s that display content and actions on a single topic.
 *
 * By default it uses the [MaterialColors.surface] as a background color.
 *
 * @param shape Defines the surface's shape as well its shadow. A shadow is only
 *  displayed if the [elevation] is greater than zero.
 * @param color The background color. [MaterialColors.surface] is used when null
 *  is provided. Use [TransparentSurface] to have no color.
 * @param border Optional border to draw on top of the shape.
 * @param elevation The z-coordinate at which to place this surface. This controls
 *  the size of the shadow below the surface.
 *
 */
@Composable
fun Card(
    shape: Shape = RectangleShape, // TODO (Andrey: Take the default shape from the theme)
    color: Color = +themeColor { surface },
    border: Border? = null,
    elevation: Dp = 0.dp,
    @Children children: @Composable() () -> Unit
) {
    // TODO(Andrey: This currently adds no logic on top of Surface, I just reserve the name
    // for now. We will see what will be the additional Card specific logic later.
    // It will add the default shape with rounded corners, default 1px elevation, elevate on hover.
    Surface(
        shape = shape,
        color = color,
        elevation = elevation,
        border = border,
        children = children
    )
}
