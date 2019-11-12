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

import androidx.compose.Ambient
import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.unaryPlus
import androidx.ui.core.Clip
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.Dp
import androidx.ui.core.DrawShadow
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.engine.geometry.Shape
import androidx.ui.foundation.shape.border.Border
import androidx.ui.foundation.shape.DrawShape
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.foundation.shape.border.DrawBorder
import androidx.ui.graphics.Color
import androidx.ui.material.ColorPalette
import androidx.ui.material.MaterialTheme
import androidx.ui.material.textColorForBackground
import androidx.ui.text.TextStyle

/**
 * The [Surface] is responsible for:
 *
 * 1) Clipping: Surface clips its children to the shape specified by [shape]
 *
 * 2) Elevation: Surface elevates its children on the Z axis by [elevation] pixels,
 *   and draws the appropriate shadow.
 *
 * 3) Borders: If [shape] has a border, then it will also be drawn.
 *
 *
 * Material surface is the central metaphor in material design. Each surface
 * exists at a given elevation, which influences how that piece of surface
 * visually relates to other surfaces and how that surface casts shadows.
 *
 * The text color for inner [Text] components will try to match the correlated color
 * for the background [color]. For example, on [ColorPalette.surface] background
 * [ColorPalette.onSurface] will be used for text. To modify these default style
 * values use [CurrentTextStyleProvider] or provide direct styling to your components.
 * @see textColorForBackground
 *
 * @param modifier Modifier to be applied to the layout corresponding to the surface
 * @param shape Defines the surface's shape as well its shadow. A shadow is only
 *  displayed if the [elevation] is greater than zero.
 * @param color The background color. Use [Color.Transparent] to have no color.
 * @param border Optional border to draw on top of the shape.
 * @param elevation The z-coordinate at which to place this surface. This controls
 *  the size of the shadow below the surface.
 */
@Composable
fun Surface(
    modifier: Modifier = Modifier.None,
    shape: Shape = RectangleShape,
    color: Color = (+MaterialTheme.colors()).surface,
    border: Border? = null,
    elevation: Dp = 0.dp,
    children: @Composable() () -> Unit
) {
    SurfaceLayout(modifier) {
        if (elevation > 0.dp) {
            DrawShadow(shape = shape, elevation = elevation)
        }
        DrawShape(shape = shape, color = color)
        Clip(shape = shape) {
            val background = if (color.alpha > 0f) color else +ambient(CurrentBackground)
            CurrentBackground.Provider(background) {
                CurrentTextStyleProvider(
                    value = TextStyle(color = +textColorForBackground(color)),
                    children = children
                )
            }
        }
        if (border != null) {
            DrawBorder(shape = shape, border = border)
        }
    }
}

/**
 * An ambient to store the current background. Usually provided by [Surface] component.
 *
 * Other components can read it to apply some logic to automatically change icons, text
 * or Ripple color based on the current background.
 */
val CurrentBackground = Ambient.of { Color.Transparent }

/**
 * A simple layout which just reserves a space for a [Surface].
 * It positions the only child in the left top corner.
 */
// TODO("Andrey: Should be replaced with some basic layout implementation when we have it")
@Composable
private fun SurfaceLayout(modifier: Modifier = Modifier.None, children: @Composable() () -> Unit) {
    Layout(children, modifier) { measurables, constraints ->
        if (measurables.size > 1) {
            throw IllegalStateException("Surface can have only one direct measurable child!")
        }
        val measurable = measurables.firstOrNull()
        if (measurable == null) {
            layout(constraints.minWidth, constraints.minHeight) {}
        } else {
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0.ipx, 0.ipx)
            }
        }
    }
}
