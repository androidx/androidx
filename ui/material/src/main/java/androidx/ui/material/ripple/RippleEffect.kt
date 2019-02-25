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

package androidx.ui.material.ripple

import androidx.annotation.CallSuper
import androidx.ui.core.LayoutCoordinates
import androidx.ui.material.surface.Surface
import androidx.ui.painting.Canvas
import androidx.ui.painting.Color
import androidx.ui.vectormath64.Matrix4

/**
 * A visual reaction on a [RippleSurface].
 *
 * To add an [RippleEffect] to a piece of [Surface], obtain the [RippleSurfaceOwner] via
 * [RippleSurfaceConsumer] and call [RippleSurfaceOwner.addEffect].
 */
abstract class RippleEffect(
    /**
     * The [RippleSurfaceOwner] associated with this [RippleEffect].
     *
     * Typically used by subclasses to call
     * [RippleSurfaceOwner.markNeedsRedraw] when they need to redraw.
     */
    val rippleSurface: RippleSurfaceOwner,
    /** The layout coordinates of the parent for this ripple. */
    val coordinates: LayoutCoordinates,
    color: Color,
    /** Called when the ripple is no longer visible on the surface. */
    val onRemoved: (() -> Unit)? = null
) {

    internal var debugDisposed = false

    /** Free up the resources associated with this ripple. */
    @CallSuper
    open fun dispose() {
        assert(!debugDisposed)
        debugDisposed = true
        rippleSurface.removeEffect(this)
        onRemoved?.invoke()
    }

    internal fun draw(canvas: Canvas) {
        assert(!debugDisposed)
        // TODO("Migration|Andrey: Calculate transformation matrix using parents")
        // TODO("Migration|Andrey: Currently we don't have such a logic")
//        // find the chain of renderers from us to the feature's target layout
//        val descendants = mutableListOf(referenceBox)
//        var node = referenceBox
//        while (node != _controller) {
//            node = node.parent as RenderBox
//            descendants.add(node)
//        }
//         determine the transform that gets our coordinate system to be like theirs
        val transform = Matrix4.identity()
//        assert(descendants.size >= 2)
//
//        for (index in descendants.size - 1 downTo 1) {
//            descendants[index].applyPaintTransform(descendants[index - 1], transform)
//        }
        drawEffect(canvas, transform)
    }

    /**
     * Override this method to draw the ripple.
     *
     * The transform argument gives the coordinate conversion from the coordinate
     * system of the canvas to the coordinate system of the target layout.
     */
    protected abstract fun drawEffect(canvas: Canvas, transform: Matrix4)

    /**
     * Called when the user input that triggered this ripple's appearance was confirmed or canceled.
     *
     * Typically causes the ripple to start disappearance animation.
     */
    abstract fun finish(canceled: Boolean)

    /** The ripple's color. */
    var color: Color = color
        set(value) {
            if (value == field)
                return
            field = value
            rippleSurface.markNeedsRedraw()
        }
}
