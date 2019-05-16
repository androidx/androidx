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
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Px
import androidx.ui.core.PxBounds
import androidx.ui.core.PxPosition
import androidx.ui.material.borders.BorderRadius
import androidx.ui.material.borders.BoxShape
import androidx.ui.graphics.Color

/**
 * An encapsulation of an [RippleEffect] constructor used by [BoundedRipple]
 * [Ripple] and [RippleTheme].
 *
 * Will be used as a theme parameter in [RippleTheme.factory]
 */
abstract class RippleEffectFactory {

    /**
     * The factory method.
     *
     * Subclasses should override this method to return a new instance of an [RippleEffect].
     */
    abstract fun create(
        rippleSurface: RippleSurfaceOwner,
        coordinates: LayoutCoordinates,
        touchPosition: PxPosition,
        color: Color,
        density: Density,
        // TODO("Andrey: Could we integrate shape and clippingBorderRadius into one concept API wise?
        // It is strange now that BoxShape can be a circle and there's a separate border
        // radius attribute")
        shape: BoxShape = BoxShape.Rectangle,
        finalRadius: Px? = null,
        containedInkWell: Boolean = false,
        boundsCallback: ((LayoutCoordinates) -> PxBounds)? = null,
        clippingBorderRadius: BorderRadius? = null,
        onRemoved: (() -> Unit)? = null
    ): RippleEffect
}
