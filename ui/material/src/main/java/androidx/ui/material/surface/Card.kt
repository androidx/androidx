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

import androidx.ui.core.Dp
import androidx.ui.core.dp
import androidx.ui.material.MaterialColors
import androidx.ui.material.borders.RoundedRectangleBorder
import androidx.ui.material.borders.ShapeBorder
import androidx.ui.graphics.Color
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer

/**
 * Cards are [Surface]s that display content and actions on a single topic.
 *
 * By default it uses the [MaterialColors.surface] as a background color.
 *
 * @param shape Defines the surface's shape as well its shadow. A shadow is only
 *  displayed if the [elevation] is greater than zero.
 * @param color The background color. [MaterialColors.surface] is used when null
 *  is provided. Use [TransparentSurface] to have no color.
 * @param elevation The z-coordinate at which to place this surface. This controls
 *  the size of the shadow below the surface.
 *
 */
@Composable
fun Card(
    shape: ShapeBorder = RoundedRectangleBorder(),
    color: Color? = null,
    elevation: Dp = 0.dp,
    @Children children: @Composable() () -> Unit
) {
    // TODO(Andrey: This currently adds no logic on top of Surface, I just reserve the name
    // for now. We will see what will be the additional Card specific logic later.
    // It will add the default shape with rounded corners, default 1px elevation, elevate on hover.
    Surface(shape = shape, elevation = elevation, color = color, children = children)
}
