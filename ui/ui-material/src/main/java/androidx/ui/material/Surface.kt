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
import androidx.compose.Providers
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.clip
import androidx.ui.core.drawShadow
import androidx.ui.core.zIndex
import androidx.ui.foundation.Border
import androidx.ui.foundation.ContentColorAmbient
import androidx.ui.foundation.ProvideTextStyle
import androidx.ui.foundation.Text
import androidx.ui.foundation.drawBackground
import androidx.ui.foundation.drawBorder
import androidx.ui.graphics.Color
import androidx.ui.graphics.RectangleShape
import androidx.ui.graphics.Shape
import androidx.ui.graphics.compositeOver
import androidx.ui.text.TextStyle
import androidx.ui.unit.Dp
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import kotlin.math.ln

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
 * Material surface is the central metaphor in material design. Each surface
 * exists at a given elevation, which influences how that piece of surface
 * visually relates to other surfaces and how that surface casts shadows.
 *
 * [contentColor] is the preferred color for any children inside this surface - any [Text] inside
 * this Surface will use this color by default.
 *
 * If no [contentColor] is set, this surface will try and match its background color to a color
 * defined in the theme [ColorPalette], and return the corresponding `onFoo` color. For example,
 * if the [color] of this surface is [ColorPalette.surface], [contentColor] will be set to
 * [ColorPalette.onSurface]. If [color] is not part of the theme palette, [contentColor] will keep
 * the same value set above this Surface.
 *
 * To modify these default style values used by text, use [ProvideTextStyle] or explicitly
 * pass a new [TextStyle] to your text.
 *
 * To manually retrieve the content color inside a surface, use [contentColor].
 *
 * @param modifier Modifier to be applied to the layout corresponding to the surface
 * @param shape Defines the surface's shape as well its shadow. A shadow is only
 *  displayed if the [elevation] is greater than zero.
 * @param color The background color. Use [Color.Transparent] to have no color.
 * @param contentColor The preferred content color provided by this Surface to its children.
 * Defaults to either the matching `onFoo` color for [color], or if [color] is not a color from
 * the theme, this will keep the same value set above this Surface.
 * @param border Optional border to draw on top of the surface
 * @param elevation The z-coordinate at which to place this surface. This controls
 * the size of the shadow below the surface.
 */
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(color),
    border: Border? = null,
    elevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    SurfaceLayout(
        modifier.drawShadow(elevation = elevation, shape = shape, clip = false)
            .zIndex(elevation.value)
            .plus(if (border != null) Modifier.drawBorder(border, shape) else Modifier)
            .drawBackground(
                color = getBackgroundColorForElevation(color, elevation),
                shape = shape
            )
            .clip(shape)
    ) {
        Providers(ContentColorAmbient provides contentColor, children = content)
    }
}

/**
 * primarySurface represents the background color of components that are [ColorPalette.primary]
 * in light theme, and [ColorPalette.surface] in dark theme, such as [androidx.ui.material.TabRow]
 * and [androidx.ui.material.TopAppBar]. This is to reduce brightness of large surfaces in dark
 * theme, aiding contrast and readability. See
 * [Dark Theme](https://material.io/design/color/dark-theme.html#custom-application).
 *
 * @return [ColorPalette.primary] if in light theme, else [ColorPalette.surface]
 */
val ColorPalette.primarySurface: Color get() = if (isLight) primary else surface

/**
 * A simple layout which just reserves a space for a [Surface].
 * It positions the only child in the left top corner.
 */
// TODO("Andrey: Should be replaced with some basic layout implementation when we have it")
@Composable
private fun SurfaceLayout(modifier: Modifier = Modifier, children: @Composable () -> Unit) {
    Layout(children, modifier) { measurables, constraints, _ ->
        if (measurables.size > 1) {
            throw IllegalStateException("Surface can have only one direct measurable child!")
        }
        val measurable = measurables.firstOrNull()
        if (measurable == null) {
            layout(constraints.minWidth, constraints.minHeight) {}
        } else {
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.placeAbsolute(0.ipx, 0.ipx)
            }
        }
    }
}

/**
 * If in a light theme, returns [color]. If in dark theme, applies an elevation overlay if [color]
 * is equal to ColorPalette.surface, else returns [color]
 */
@Composable
private fun getBackgroundColorForElevation(color: Color, elevation: Dp): Color {
    val colors = MaterialTheme.colors
    return if (elevation > 0.dp && color == colors.surface && !colors.isLight) {
        color.withElevation(elevation)
    } else {
        color
    }
}

/**
 * Applies a [Color.White] overlay to this color based on the [elevation]. This increases visibility
 * of elevation for surfaces in a dark theme.
 */
private fun Color.withElevation(elevation: Dp): Color {
    val foreground = calculateForeground(elevation)
    return foreground.compositeOver(this)
}

// TODO: b/145802792 - clarify this algorithm
/**
 * @return the alpha-modified [Color.White] to overlay on top of the surface color to produce
 * the resultant color.
 */
private fun calculateForeground(elevation: Dp): Color {
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    return Color.White.copy(alpha = alpha)
}
