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

package androidx.ink.rendering.android.view

import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Build
import android.view.View
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.rendering.android.canvas.StrokeDrawScope

/**
 * Helps developers using Android Views to draw [androidx.ink.strokes.Stroke] objects in their UI,
 * in an easier way than using [CanvasStrokeRenderer] directly. Construct this once for your [View]
 * and reuse it during each [View.onDraw] call.
 *
 * This utility is valid as long as [View.onDraw]
 * 1. Does not call [Canvas.setMatrix].
 * 2. Does not modify [Canvas] transform state prior to calling [drawWithStrokes].
 * 3. Does not use [android.graphics.RenderEffect], either setting it on this [View] or a subview
 *    using [View.setRenderEffect], or by calling [Canvas.drawRenderNode] using a
 *    [android.graphics.RenderNode] that has been configured with
 *    [android.graphics.RenderNode.setRenderEffect]. Developers who want to use
 *    [android.graphics.RenderEffect] in conjunction with [androidx.ink.strokes.Stroke] rendering
 *    must use [CanvasStrokeRenderer.draw] directly.
 *
 * Example:
 * ```
 * class MyView(context: Context) : View(context) {
 *   private val viewStrokeRenderer = ViewStrokeRenderer(myCanvasStrokeRenderer, this)
 *
 *   override fun onDraw(canvas: Canvas) {
 *     viewStrokeRenderer.drawWithStrokes(canvas) { scope ->
 *       canvas.scale(myZoomLevel)
 *       canvas.rotate(myRotation)
 *       canvas.translate(myPanX, myPanY)
 *       scope.drawStroke(myStroke)
 *       // Draw other objects including more strokes, apply more transformations, etc.
 *     }
 *   }
 * }
 * ```
 */
public class ViewStrokeRenderer(
    private val canvasStrokeRenderer: CanvasStrokeRenderer,
    private val view: View,
) {

    private val scratchMatrix = Matrix()
    private val recycledDrawScopes = mutableListOf<StrokeDrawScope>()

    /**
     * Kotlin developers should call this at the beginning of [View.onDraw] and perform their
     * [Canvas] manipulations within its scope.
     *
     * For example:
     * ```
     * viewStrokeRenderer.drawWithStrokes(canvas) { scope ->
     *   canvas.scale(...) // or concat, or translate, or rotate, etc.
     *   scope.drawStroke(stroke)
     *   // Repeat with other strokes, draw other things to the canvas, etc.
     * }
     * ```
     *
     * Java callers should prefer to use the non-inline overload of [drawWithStrokes] with a
     * non-capturing lambda or an object that is allocated once and reused.
     */
    public inline fun drawWithStrokes(canvas: Canvas, block: (StrokeDrawScope) -> Unit) {
        val scope = obtainDrawScope(canvas)
        block(scope)
        recycleDrawScope(scope)
    }

    /**
     * Java developers should call this at the beginning of [View.onDraw] and perform their [Canvas]
     * manipulations within its scope.
     *
     * The structure of the callback in this non-inline version makes it easier for Java callers to
     * write performant code, since forwarding the value of [canvas] allows a lambda to be
     * non-capturing, thereby avoiding an allocation of the lambda on every frame.
     *
     * For example:
     * ```java
     * viewStrokeRenderer.drawWithStrokes(canvas, (scopedCanvas, scope) -> {
     *   // Make sure to use `scopedCanvas` rather than `canvas` in this lambda!
     *   scopedCanvas.scale(...); // or concat, or translate, or rotate, etc.
     *   scope.drawStroke(stroke);
     *   // Repeat with other strokes, draw other things to the canvas, etc.
     * });
     * ```
     *
     * Alternatively, the callback could be an object that is allocated once and reused.
     *
     * Kotlin callers should prefer to use the inline overload of [drawWithStrokes], as it better
     * guarantees that the lambda argument will not cause an allocation.
     */
    public fun drawWithStrokes(
        canvas: Canvas,
        block: (scopedCanvas: Canvas, StrokeDrawScope) -> Unit
    ) {
        val scope = obtainDrawScope(canvas)
        block(canvas, scope)
        recycleDrawScope(scope)
    }

    /**
     * Manually obtain a scope to draw into the given [canvas]. Make sure to call [recycleDrawScope]
     * when finished drawing. Prefer to use the public [drawWithStrokes] function instead.
     */
    @PublishedApi
    internal fun obtainDrawScope(canvas: Canvas): StrokeDrawScope {
        val viewToScreenTransform =
            scratchMatrix.also {
                it.reset()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    view.transformMatrixToGlobal(it)
                } else {
                    transformMatrixToGlobalFallback(view, it)
                }
            }
        require(viewToScreenTransform.isAffine) { "View to screen transform must be affine." }
        val scope = recycledDrawScopes.removeFirstOrNull() ?: StrokeDrawScope(canvasStrokeRenderer)
        scope.onDrawStart(viewToScreenTransform, canvas)
        return scope
    }

    /**
     * Recycle a [scope] for future use, that was previously obtained with [obtainDrawScope]. Prefer
     * to use the public [drawWithStrokes] function instead.
     */
    @PublishedApi
    internal fun recycleDrawScope(scope: StrokeDrawScope) {
        recycledDrawScopes.add(scope)
    }
}

/**
 * Modify [matrix] such that it maps from view-local to on-screen coordinates when
 * [View.transformMatrixToGlobal] is not available.
 */
private fun transformMatrixToGlobalFallback(view: View, matrix: Matrix) {
    (view.parent as? View)?.let {
        transformMatrixToGlobalFallback(it, matrix)
        matrix.preTranslate(-it.scrollX.toFloat(), -it.scrollY.toFloat())
    }
    matrix.preTranslate(view.left.toFloat(), view.top.toFloat())
    matrix.preConcat(view.matrix)
}
