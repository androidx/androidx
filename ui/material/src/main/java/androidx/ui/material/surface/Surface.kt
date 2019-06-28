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
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.baseui.shape.border.Border
import androidx.ui.baseui.shape.DrawShape
import androidx.ui.baseui.shape.RectangleShape
import androidx.ui.baseui.shape.Shape
import androidx.ui.baseui.shape.border.DrawBorder
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.Dp
import androidx.ui.core.Draw
import androidx.ui.core.Layout
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.engine.geometry.addOutline
import androidx.ui.graphics.Color
import androidx.ui.material.MaterialColors
import androidx.ui.material.ripple.RippleEffect
import androidx.ui.material.ripple.RippleSurface
import androidx.ui.material.ripple.RippleSurfaceOwner
import androidx.ui.material.ripple.ambientRippleSurface
import androidx.ui.material.textColorForBackground
import androidx.ui.material.themeColor
import androidx.ui.painting.Path
import androidx.ui.painting.TextStyle

/**
 * The [Surface] is responsible for:
 *
 * 1) Clipping: Surface clips its children to the shape specified by [shape]
 *
 * 2) Elevation: Surface elevates its children on the Z axis by [elevation] pixels,
 *   and draws the appropriate shadow.
 *
 * 3) Ripple effects: Surface shows [RippleEffect]s below its children.
 *
 * 4) Borders: If [shape] has a border, then it will also be drawn.
 *
 *
 * Material surface is the central metaphor in material design. Each surface
 * exists at a given elevation, which influences how that piece of surface
 * visually relates to other surfaces and how that surface casts shadows.
 *
 * Most user interface elements are either conceptually printed on a surface
 * or themselves made of surface. Surface reacts to user input using [RippleEffect] effects.
 * To trigger a reaction on the surface, use a [RippleSurfaceOwner] obtained via
 * [ambientRippleSurface].
 *
 * The text color for internal [Text] components will try to match the correlated color
 * for the background [color]. For example, on [MaterialColors.surface] background
 * [MaterialColors.onSurface] will be used for text. To modify these default style
 * values use [CurrentTextStyleProvider] or provide direct styling to your components.
 *
 * @param shape Defines the surface's shape as well its shadow. A shadow is only
 *  displayed if the [elevation] is greater than zero.
 * @param color The background color. [MaterialColors.surface] is used when null
 *  is provided. Use [TransparentSurface] to have no color.
 * @param border Optional border to draw on top of the shape.
 * @param elevation The z-coordinate at which to place this surface. This controls
 *  the size of the shadow below the surface.
 */
@Composable
fun Surface(
    shape: Shape = RectangleShape,
    color: Color = +themeColor { surface },
    border: Border? = null,
    elevation: Dp = 0.dp,
    @Children children: @Composable() () -> Unit
) {
    SurfaceLayout {
        DrawShadow(shape = shape, elevation = elevation)
        DrawShape(shape = shape, color = color)
        Clip(shape = shape) {
            RippleSurface(color = color) {
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
 * A simple layout which just reserves a space for a [Surface].
 * It positions the only child in the left top corner.
 *
 * TODO("Andrey: Should be replaced with some basic layout implementation when we have it")
 */
@Composable
private fun SurfaceLayout(@Children children: @Composable() () -> Unit) {
    Layout(children = children, layoutBlock = { measurables, constraints ->
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
    })
}

// Temporary.
// TODO: Replace with an implementation using RenderNode's Outlines instead to have
// clipping with antialiasing.
@Composable
private fun Clip(shape: Shape, @Children children: @Composable() () -> Unit) {
    val path = +memo { Path() }
    Draw(children = {
        // this layout is temporary while Draw doesn't accept multiple children
        SurfaceLayout(children)
    }) { canvas, parentSize ->
        path.reset()
        path.addOutline(shape.createOutline(parentSize, density))
        canvas.save()
        canvas.clipPath(path)
        drawChildren()
        canvas.restore()
    }
}
