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

import androidx.compose.Composable
import androidx.compose.unaryPlus
import androidx.ui.core.Dp
import androidx.ui.core.dp
import androidx.ui.engine.geometry.Shape
import androidx.ui.foundation.shape.border.Border
import androidx.ui.graphics.Color
import androidx.ui.material.MaterialTheme
import androidx.ui.material.themeShape

/**
 * Cards are [Surface]s that display content and actions on a single topic.
 *
 * @param shape Defines the surface's shape as well its shadow. A shadow is only
 *  displayed if the [elevation] is greater than zero.
 * @param color The background color.
 * @param border Optional border to draw on top of the shape.
 * @param elevation The z-coordinate at which to place this surface. This controls
 *  the size of the shadow below the surface.
 */
@Composable
fun Card(
    shape: Shape = +themeShape { card },
    color: Color = (+MaterialTheme.colors()).surface,
    border: Border? = null,
    elevation: Dp = 1.dp,
    children: @Composable() () -> Unit
) {
    Surface(
        shape = shape,
        color = color,
        elevation = elevation,
        border = border,
        children = children
    )
}
