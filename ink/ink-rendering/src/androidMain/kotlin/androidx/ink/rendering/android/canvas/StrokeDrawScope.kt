/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.rendering.android.canvas

import android.graphics.Canvas
import android.graphics.Matrix
import androidx.annotation.RestrictTo
import androidx.ink.strokes.Stroke

/**
 * A utility to simplify usage of [CanvasStrokeRenderer] by automatically calculating the
 * `strokeToCanvasTransform` parameter of [CanvasStrokeRenderer.draw]. Obtain an instance of this
 * class using [androidx.ink.rendering.android.view.ViewStrokeRenderer], if using
 * [android.view.View]. Use this scope by calling its [drawStroke] function.
 */
// TODO: b/353561141 - Reference ComposeStrokeRenderer above once implemented.
public class StrokeDrawScope
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(private val renderer: CanvasStrokeRenderer) {

    /**
     * Pre-allocated value updated in [onDrawStart] holding a transform from the
     * implementation-defined "initial" transform state of the canvas to screen coordinates as
     * described below.
     *
     * We want to be able to calculate the complete (Local -> Screen) transform inside [drawStroke].
     * The only options to track changes made by client code to the [Canvas] transform state are:
     * 1. Require the user to explicitly pass the transform as in [CanvasStrokeRenderer],
     * 2. Create and require clients to use a complete [Canvas] wrapper type, or
     * 3. Make use of the deprecated [Canvas.getMatrix] method.
     *
     * Option 1 is provided, but cumbersome for clients. We are avoiding option 2, because it would
     * also be cumbersome client code, and we would need to track every API adding or breaking
     * change in [Canvas].
     *
     * Part of the reason for the deprecation documented on [Canvas.getMatrix] is that hardware
     * accelerated canvases have an implementation-defined matrix value when passed to a `View`,
     * because they may be anywhere in the `View` hierarchy. However, we can use the delta between
     * the values returned by two calls to [Canvas.getMatrix] to find the relative change in
     * transformations.
     *
     * We assume that any non-identity value of the matrix at the start of [onDrawStart] is already
     * part of the `canvasToScreenTransform` passed to [onDrawStart] as shown in the following
     * diagram:
     *
     *                     |-       [canvasToScreenTransform]         -|
     *                     |                                           |
     *                     |                   |-  canvas.getMatrix() -|
     *                     |                   |    in [onDrawStart]   |
     *
     * (Local -> Screen) = (Initial -> Screen) * (Canvas ---> Initial) * (Local -> Canvas)
     *
     *                                         |           canvas.getMatrix()            |
     *                                         |-           in [drawStroke]             -|
     */
    private val initialCanvasToScreenTransform = Matrix()
    private lateinit var canvas: Canvas

    /**
     * Pre-allocated total transform from drawn object local coordinates to screen coordinates
     * calculated once per call to [drawStroke].
     */
    private val localToScreenTransform = Matrix()

    /** Overwrite this object for reuse. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun onDrawStart(canvasToScreenTransform: Matrix, newCanvas: Canvas) {
        canvas = newCanvas
        with(initialCanvasToScreenTransform) {
            // (Canvas -> Initial)
            @Suppress("DEPRECATION") canvas.getMatrix(this)
            // (Initial -> Canvas)
            invert(this)
            // (Initial -> Screen) = (Canvas -> Screen) * (Initial -> Canvas)
            postConcat(canvasToScreenTransform)
        }
    }

    /** Draw the given [Stroke] to the [Canvas] represented by this scope. */
    public fun drawStroke(stroke: Stroke) {
        // First, calculate (Local -> Screen). That is the transform that the renderer needs.
        with(localToScreenTransform) {
            // (Local -> Initial)
            @Suppress("DEPRECATION") canvas.getMatrix(this)
            // (Local -> Screen) = (Initial -> Screen) * (Local -> Initial)
            postConcat(initialCanvasToScreenTransform)
        }
        renderer.draw(canvas, stroke, localToScreenTransform)
    }
}
