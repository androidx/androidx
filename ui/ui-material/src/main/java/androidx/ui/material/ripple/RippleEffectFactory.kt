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

import androidx.ui.core.Density
import androidx.ui.core.Dp
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.PxPosition
import androidx.ui.graphics.Color

/**
 * An encapsulation of an [RippleEffect] constructor used by [Ripple] and [RippleTheme].
 *
 * Will be used as a theme parameter in [RippleTheme.factory]
 */
interface RippleEffectFactory {

    /**
     * The factory method.
     *
     * Subclasses should override this method to return a new instance of an [RippleEffect].
     *
     * @param coordinates The layout coordinates of the parent for this ripple.
     * @param surfaceCoordinates The surface layout coordinates.
     * @param touchPosition The position the animation will start from.
     * @param color The color for this [RippleEffect].
     * @param density The [Density] object to convert the dimensions.
     * @param radius Effects grow up to this size. By default the size is
     *  determined from the size of the layout itself.
     * @param bounded If true, then the ripple will be sized to fit the bounds of the target
     *  layout, then clipped to it when drawn. If false, then the ripple is clipped only
     *  to the edges of the surface.
     * @param requestRedraw Call when the ripple should be redrawn to display the next frame.
     * @param onAnimationFinished Call when the effect animation has been finished.
     */
    fun create(
        coordinates: LayoutCoordinates,
        surfaceCoordinates: LayoutCoordinates,
        touchPosition: PxPosition,
        color: Color,
        density: Density,
        radius: Dp?,
        bounded: Boolean,
        requestRedraw: (() -> Unit),
        onAnimationFinished: ((RippleEffect) -> Unit)
    ): RippleEffect
}
