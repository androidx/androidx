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
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.dp
import androidx.ui.core.hasBoundedHeight
import androidx.ui.core.hasBoundedWidth
import androidx.ui.material.borders.RoundedRectangleBorder
import androidx.ui.material.borders.ShapeBorder
import androidx.ui.material.clip.ClipPath
import androidx.ui.material.clip.ShapeBorderClipper
import androidx.ui.material.clip.cache.CachingClipper
import androidx.ui.material.ripple.RippleEffect
import androidx.ui.material.ripple.RippleSurface
import androidx.ui.material.ripple.RippleSurfaceOwner
import androidx.ui.material.ripple.ambientRippleSurface
import androidx.ui.painting.Color
import com.google.r4a.Ambient
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer

/**
 * The ambient to configure the color of the shadow below the [Surface].
 *
 * Defaults to fully opaque black.
 */
val CurrentSurfaceShadowColor = Ambient.of { Color(0xFF000000.toInt()) }

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
 */
@Composable
fun Surface(
    /**
     * Defines the surface's shape as well its shadow.
     *
     * A shadow is only displayed if the [elevation] is greater than
     * zero.
     */
    shape: ShapeBorder = RoundedRectangleBorder(),
    /**
     * The color to paint the surface.
     *
     * By default it has no color.
     */
    color: Color? = null,
    /**
     * The z-coordinate at which to place this surface. This controls the size
     * of the shadow below the surface.
     */
    elevation: Dp = 0.dp,
    @Children children: () -> Unit
) {
    <CurrentSurfaceShadowColor.Consumer> shadowColor ->
        <SurfaceMeasureBox>
            <CachingClipper
                clipper=ShapeBorderClipper(shape)> clipper ->
                <DrawShadow elevation clipper shadowColor />
                <ClipPath clipper>
                    <DrawColor color />
                    <RippleSurface color>
                        <children />
                    </RippleSurface>
                </ClipPath>
            </CachingClipper>
            <DrawBorder shape />
        </SurfaceMeasureBox>
    </CurrentSurfaceShadowColor.Consumer>
}

/**
 * A simple MeasureBox which just reserves a space for a [Surface].
 * It position the children in the left top corner and takes all the available space.
 *
 * TODO("Andrey: Should be replaced with some basic layout implementation when we have it")
 */
@Composable
internal fun SurfaceMeasureBox(@Children children: () -> Unit) {
    <Layout layoutBlock = { measurables, constraints ->
        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            constraints.minWidth
        }

        val height = if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            constraints.minHeight
        }

        layout(width, height) {
            measurables.forEach { it.measure(constraints).place(IntPx.Zero, IntPx.Zero) }
        }
    } children />
}