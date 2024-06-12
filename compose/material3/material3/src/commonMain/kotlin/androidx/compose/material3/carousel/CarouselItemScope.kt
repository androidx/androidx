/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.carousel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.platform.LocalDensity

/** Receiver scope for [Carousel] item content. */
@ExperimentalMaterial3Api
sealed interface CarouselItemScope {
    /**
     * Information regarding the carousel item, such as its minimum and maximum size.
     *
     * The item information is updated after every scroll. If you use it in a composable function,
     * it will be recomposed on every change causing potential performance issues. Avoid using it in
     * the composition.
     */
    val carouselItemDrawInfo: CarouselItemDrawInfo

    /**
     * Clips the composable to the given [shape], taking into account the item's size in the cross
     * axis and mask in the main axis.
     *
     * @param shape the shape to be applied to the composable
     */
    @Composable fun Modifier.maskClip(shape: Shape): Modifier

    /**
     * Draw a border on the composable using the given [shape], taking into account the item's size
     * in the cross axis and mask in the main axis.
     *
     * @param border the border to be drawn around the composable
     * @param shape the shape of the border
     */
    @Composable fun Modifier.maskBorder(border: BorderStroke, shape: Shape): Modifier

    /**
     * Converts and remembers [shape] into a [GenericShape] that uses the intersection of the
     * carousel item's mask Rect and Size as the final shape's bounds.
     *
     * This method is useful if using a [Shape] in a Modifier other than [maskClip] and [maskBorder]
     * where the shape should follow the changes in the item's mask size.
     *
     * @param shape The shape that will be converted and remembered and react to changes in the
     *   item's mask.
     */
    @Composable fun rememberMaskShape(shape: Shape): GenericShape
}

@ExperimentalMaterial3Api
internal class CarouselItemScopeImpl(private val itemInfo: CarouselItemDrawInfo) :
    CarouselItemScope {
    override val carouselItemDrawInfo: CarouselItemDrawInfo
        get() = itemInfo

    @Composable
    override fun Modifier.maskClip(shape: Shape): Modifier = clip(rememberMaskShape(shape = shape))

    @Composable
    override fun Modifier.maskBorder(border: BorderStroke, shape: Shape): Modifier =
        border(border, rememberMaskShape(shape = shape))

    @Composable
    override fun rememberMaskShape(shape: Shape): GenericShape {
        val density = LocalDensity.current
        return remember(carouselItemDrawInfo, density) {
            GenericShape { size, direction ->
                val rect = carouselItemDrawInfo.maskRect.intersect(size.toRect())
                addOutline(shape.createOutline(rect.size, direction, density))
                translate(Offset(rect.left, rect.top))
            }
        }
    }
}
