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

package androidx.compose.ui.graphics.drawscope

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Default density value that is used as a stub to provide a non-null density parameter within
 * CanvasDrawScope. Density is provided as a parameter as part of the draw call to issue drawing
 * commands into a target canvas so this Density value is never consumed
 */
internal val DefaultDensity = Density(1.0f, 1.0f)

/**
 * Object that provides the dependencies to support a [DrawScope] drawing environment. Namely this
 * provides the drawing bounds represented as a size as well as the target [Canvas] to issue drawing
 * commands into. Additionally the [DrawContext] handles updating [Canvas] state during
 * transformations and updating the size of the drawing bounds that may occur during these
 * transformations.
 *
 * This exposes necessary internal state to the implementation of the [DrawScope] in order to
 * support inline scoped transformation calls without allowing consumers of [DrawScope] to modify
 * state directly thus maintaining the stateless API surface
 */
interface DrawContext {

    /** The current size of the drawing environment */
    var size: Size

    /** The target canvas to issue drawing commands */
    var canvas: Canvas
        get() = EmptyCanvas
        set(_) {}

    /** The controller for issuing transformations to the drawing environment */
    val transform: DrawTransform

    /** [LayoutDirection] of the layout being drawn in. */
    var layoutDirection: LayoutDirection
        get() = LayoutDirection.Ltr
        set(_) {}

    /**
     * [Density] used to assist in conversions of density independent pixels to raw pixels to draw
     */
    var density: Density
        get() = DefaultDensity
        set(_) {}

    /**
     * Current [GraphicsLayer] we are drawing into. Might be null if the [canvas] is not provided by
     * a [GraphicsLayer], for example in the case of a software-accelerated drawing.
     */
    var graphicsLayer: GraphicsLayer?
        get() = null
        set(_) {}
}
