/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress
import androidx.wear.compose.foundation.lazy.inverseLerp
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.TransformedContainerPainterScope

@Composable
fun rememberMorphingTransformationSpec(
    transformationSpec: TransformationSpec,
    minMorphingHeight: Int,
) =
    remember(transformationSpec, minMorphingHeight) {
        MorphingTransformationSpec(
            transformationSpec = transformationSpec,
            growthStartScreenFraction = 0.95f,
            growthEndScreenFraction = 0.8f,
            minMorphingHeight = minMorphingHeight,
        )
    }

data class MorphingTransformationSpec(
    val transformationSpec: TransformationSpec,
    /**
     * Configuration for the screen point where the height morphing starts (item is touching this
     * screen point with its bottom edge).
     */
    val growthStartScreenFraction: Float,

    /**
     * Configuration for the screen point where the height morphing ends and item is fully expanded
     * (item is touching this screen point with its bottom edge).
     */
    val growthEndScreenFraction: Float,

    /** The height to which the item should morph when it is fully collapsed. */
    val minMorphingHeight: Int,
) : TransformationSpec {
    init {
        // Morphing start point should be below the growth end.
        require(growthEndScreenFraction < growthStartScreenFraction) {
            "growthEndScreenFraction must be smaller than growthStartScreenFraction"
        }
    }

    override fun GraphicsLayerScope.applyContentTransformation(
        scrollProgress: TransformingLazyColumnItemScrollProgress
    ) {
        with(transformationSpec) { applyContentTransformation(scrollProgress) }
        val shape = this.shape
        this.shape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    shape.createOutline(
                        size.copy(height = morphedHeight(scrollProgress, size.height)),
                        layoutDirection,
                        density,
                    )
            }
    }

    override fun GraphicsLayerScope.applyContainerTransformation(
        scrollProgress: TransformingLazyColumnItemScrollProgress
    ) = with(transformationSpec) { applyContainerTransformation(scrollProgress) }

    override fun getTransformedHeight(
        measuredHeight: Int,
        scrollProgress: TransformingLazyColumnItemScrollProgress,
    ): Int =
        transformationSpec.getTransformedHeight(
            morphedHeight(scrollProgress, measuredHeight.toFloat(), minMorphingHeight.toFloat())
                .fastRoundToInt(),
            scrollProgress,
        )

    override fun TransformedContainerPainterScope.createTransformedContainerPainter(
        painter: Painter,
        shape: Shape,
        border: BorderStroke?,
    ): Painter =
        with(transformationSpec) {
            (object : TransformedContainerPainterScope by this@createTransformedContainerPainter {
                    override val DrawScope.itemHeight: Float
                        get() =
                            morphedHeight(
                                this@itemHeight.scrollProgress,
                                with(this@createTransformedContainerPainter) { itemHeight },
                            )
                })
                .createTransformedContainerPainter(painter, shape, border)
        }

    // Height of an item before it is scaled.
    fun morphedHeight(
        scrollProgress: TransformingLazyColumnItemScrollProgress,
        itemHeight: Float,
    ): Float =
        if (scrollProgress.isUnspecified) {
            itemHeight
        } else {
            morphedHeight(scrollProgress, itemHeight, minMorphingHeight.toFloat())
        }

    private fun morphedHeight(
        scrollProgress: TransformingLazyColumnItemScrollProgress,
        itemHeight: Float,
        minMorphingHeight: Float,
    ): Float {
        // Size of the item, relative to the screen
        val relativeItemHeight =
            scrollProgress.bottomOffsetFraction - scrollProgress.topOffsetFraction
        val screenSize = itemHeight / relativeItemHeight

        val growthStartTopOffsetFraction =
            growthStartScreenFraction - minMorphingHeight / screenSize
        val growthEndTopOffsetFraction = growthEndScreenFraction - relativeItemHeight
        // Fraction of how item has grown so far.
        val heightMorphProgress =
            // growthStartTopOffsetFraction > growthEndTopOffsetFraction since item has minimum size
            // at
            // the bottom of the screen.
            inverseLerp(
                    growthStartTopOffsetFraction,
                    growthEndTopOffsetFraction,
                    scrollProgress.topOffsetFraction,
                )
                .coerceIn(0f, 1f)
        return lerp(minMorphingHeight, itemHeight, heightMorphProgress)
    }
}
