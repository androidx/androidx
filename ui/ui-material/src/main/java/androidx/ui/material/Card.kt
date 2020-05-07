/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.foundation.Border
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shape
import androidx.ui.unit.Dp
import androidx.ui.unit.dp

/**
 * Cards are [Surface]s that display content and actions on a single topic.
 *
 * @sample androidx.ui.material.samples.CardSample
 *
 * @param modifier Modifier to be applied to the layout of the card.
 * @param shape Defines the surface's shape as well its shadow. A shadow is only
 *  displayed if the [elevation] is greater than zero.
 * @param color The background color.
 * @param contentColor The preferred content color provided by this Surface to its children.
 * Defaults to either the matching `onFoo` color for [color], or if [color] is not a color from
 * the theme, this will keep the same value set above this Surface.
 * @param border Optional border to draw on top of the card
 * @param elevation The z-coordinate at which to place this surface. This controls
 *  the size of the shadow below the surface.
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    color: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(color),
    border: Border? = null,
    elevation: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        elevation = elevation,
        border = border,
        content = content
    )
}
