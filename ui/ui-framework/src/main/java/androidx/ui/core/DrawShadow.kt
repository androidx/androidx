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

package androidx.ui.core

import androidx.annotation.FloatRange
import androidx.compose.Composable
import androidx.ui.graphics.RectangleShape
import androidx.ui.graphics.Shape
import androidx.ui.unit.Dp
import androidx.ui.unit.dp

/**
 * Creates a [DrawLayerModifier] that draws the shadow. The [elevation] defines the visual depth of
 * the physical object. The physical object has a shape specified by [shape].
 *
 * Example usage:
 *
 * @sample androidx.ui.framework.samples.DrawShadowSample
 *
 * @param elevation The z-coordinate at which to place this physical object.
 * @param shape Defines a shape of the physical object
 * @param clipToOutline When active, the content drawing clips to the outline.
 * @param opacity The opacity of the layer, including the shadow.
 */
@Deprecated(
    "Use Modifier.drawShadow",
    replaceWith = ReplaceWith(
        "Modifier.drawShadow(shape, elevation, clipToOutline, opacity)",
        "androidx.ui.core.Modifier",
        "androidx.ui.core.drawShadow"
    )
)
@Composable
fun drawShadow(
    shape: Shape,
    elevation: Dp,
    clipToOutline: Boolean = true,
    @FloatRange(from = 0.0, to = 1.0) opacity: Float = 1f
) = Modifier.drawShadow(elevation, shape, clipToOutline, opacity)

/**
 * Creates a [DrawLayerModifier] that draws the shadow. The [elevation] defines the visual
 * depth of the physical object. The physical object has a shape specified by [shape].
 *
 * Note that [elevation] is only affecting the shadow size and doesn't change the drawing order.
 * Use [zIndex] modifier if you want to draw the elements with larger [elevation] after all the
 * elements with a smaller one.
 *
 * Example usage:
 *
 * @sample androidx.ui.framework.samples.DrawShadowSample
 *
 * @param elevation The elevation for the shadow in pixels
 * @param shape Defines a shape of the physical object
 * @param clipToOutline When active, the content drawing clips to the outline.
 * @param opacity The opacity of the layer, including the shadow.
 */
fun Modifier.drawShadow(
    elevation: Dp,
    shape: Shape = RectangleShape,
    clipToOutline: Boolean = elevation > 0.dp,
    @FloatRange(from = 0.0, to = 1.0) opacity: Float = 1f
) = if (elevation > 0.dp || clipToOutline || opacity != 1f) {
    composed {
        drawLayer(
            alpha = opacity,
            shadowElevation = with(DensityAmbient.current) { elevation.toPx().value },
            outlineShape = shape,
            clipToBounds = false,
            clipToOutline = clipToOutline
        )
    }
} else {
    this
}
