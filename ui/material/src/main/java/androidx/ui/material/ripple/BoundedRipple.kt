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
import androidx.ui.core.Px
import androidx.ui.core.PxBounds
import androidx.ui.material.borders.BorderRadius
import androidx.ui.material.borders.BoxShape
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer

/**
 * A rectangular area of a [RippleSurface] that responds to touch.
 *
 * For a variant of this widget that does not clip [RippleEffect]s, see [Ripple].
 *
 * The [BoundedRipple] must have a [RippleSurface] as an ancestor. The
 * [RippleSurface] is where the [RippleEffect]s are actually painted.
 *
 * See also:
 *  * [Ripple], a variant of [BoundedRipple] that doesn't force a rectangular
 *    shape on the ripple.
 */
@Composable
fun BoundedRipple(
    /**
     * The radius of the ripple.
     *
     * [RippleEffect]s grow up to this size. By default, this size is determined from
     * the size of the RECTANGLE provided by [boundsCallback], or the size of
     * the [BoundedRipple] itself.
     */
    finalRadius: Px? = null,
    /**
     * The clipping radius of the containing rect.
     *
     * If this is null, it is interpreted as [BorderRadius.Zero].
     */
    clippingBorderRadius: BorderRadius? = null,
    /**
     * Called when this part of the surface either becomes highlighted or stops
     * being highlighted.
     *
     * The value passed to the callback is true if this part of the surface has
     * become highlighted and false if this part of the surface has stopped
     * being highlighted.
     */
    onHighlightChanged: ((Boolean) -> Unit)? = null,
    /**
     * The bounds to use for the [RippleEffect]s.
     *
     * This function is intended to be provided for unusual cases.
     * For example, you can provide this for Table layouts to return
     * the bounds corresponding to the row that the item is in.
     *
     * The default value is null, which is equivalent to
     * returning the target layout argument's bounding box (though
     * slightly more efficient).
     */
    boundsCallback: ((LayoutCoordinates) -> PxBounds)? = null,
    @Children children: () -> Unit
) {
    <Ripple
        shape=BoxShape.RECTANGLE
        bounded=true
        onHighlightChanged
        finalRadius
        clippingBorderRadius
        boundsCallback
    >
        <children />
    </Ripple>
}
