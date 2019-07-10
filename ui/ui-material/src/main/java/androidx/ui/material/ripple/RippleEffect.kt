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

import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.PxPosition
import androidx.ui.core.px
import androidx.ui.graphics.Color
import androidx.ui.material.surface.Surface
import androidx.ui.painting.Canvas
import androidx.ui.vectormath64.Matrix4
import androidx.ui.vectormath64.Vector3

/**
 * A visual reaction on a [RippleSurface].
 *
 * To add an [RippleEffect] to a piece of [Surface], obtain the [RippleSurfaceOwner] via
 * [ambientRippleSurface] and call [RippleSurfaceOwner.addEffect].
 * @param coordinates The layout coordinates of the target layout.
 * @param surfaceCoordinates The surface layout coordinates.
 * @param color The color for this [RippleEffect].
 * @param requestRedraw Call when the ripple should be redrawn to display the next frame.
 */
abstract class RippleEffect(
    private val coordinates: LayoutCoordinates,
    private val surfaceCoordinates: LayoutCoordinates,
    color: Color,
    private val requestRedraw: (() -> Unit)
) {

    internal fun draw(canvas: Canvas) {
        val offset = surfaceCoordinates
            .childToLocal(coordinates, PxPosition(0.px, 0.px))
        val transform = Matrix4.translation(Vector3(
            offset.x.value,
            offset.y.value,
            0f
        ))
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
    open fun finish(canceled: Boolean) {}

    /**
     * Free up the resources associated with this ripple.
     */
    abstract fun dispose()

    /** The ripple's color. */
    var color: Color = color
        set(value) {
            if (value == field)
                return
            field = value
            requestRedraw()
        }
}
