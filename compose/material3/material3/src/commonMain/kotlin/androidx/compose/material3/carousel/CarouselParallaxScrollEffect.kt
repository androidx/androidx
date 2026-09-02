/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.annotation.FloatRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp

/**
 * Add a parallax effect to an item by clipping and translating the item as it enters and exits a
 * scrollable viewport.
 *
 * Use [carouselParallaxScrollEffect] to give any scrollable container the look and feel of a
 * Material uncontained multi-aspect carousel. Unlike dedicated Carousel composables (i.e.
 * [HorizontalMultiBrowseCarousel], [HorizontalCenteredHeroCarousel]) which are based on Pager and
 * require all items to be the same size, this modifier can be used with
 * [androidx.compose.foundation.lazy.LazyRow], [androidx.compose.foundation.lazy.LazyColumn],
 * [androidx.compose.foundation.lazy.grid.LazyHorizontalGrid],
 * [androidx.compose.foundation.lazy.grid.LazyVerticalGrid], or any custom scrollable where items
 * are able to vary in size.
 *
 * @sample androidx.compose.material3.samples.MultiAspectCarouselRowSample
 * @param index the index of the item this modifier is applied to
 * @param state the object holding the scrollable container's effect state
 * @param shape the shape to clip the item and optional border to
 * @param border an optional border to draw around the clipped [shape]
 */
@ExperimentalMaterial3Api
@Composable
public fun Modifier.carouselParallaxScrollEffect(
    index: Int,
    state: CarouselParallaxScrollEffectState,
    shape: Shape,
    border: BorderStroke? = null,
): Modifier {
    // TODO(b/544671392): Replace composable border modifier with BorderLogic once public
    if (border != null) {
        return this.border(
            border,
            remember(index, state, shape) {
                MaskShape(index = index, state = state, shape = shape)
            },
        ) then MaskParallaxNodeElement(index = index, state = state, shape = shape)
    }
    return this then MaskParallaxNodeElement(index = index, state = state, shape = shape)
}

@OptIn(ExperimentalMaterial3Api::class)
private class MaskParallaxNode(
    var index: Int,
    var state: CarouselParallaxScrollEffectState,
    var shape: Shape,
) : LayoutModifierNode, DrawModifierNode, Modifier.Node() {

    val maskShape = MaskShape(index, state, shape)

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(
                0,
                0,
                layerBlock = {
                    clip = true
                    shape = maskShape
                },
            )
        }
    }

    override fun ContentDrawScope.draw() {
        val itemInfo = state.calculateItemInfo(index)
        if (state.orientation == Orientation.Horizontal) {
            translate(left = itemInfo.parallax) { this@draw.drawContent() }
        } else {
            translate(top = itemInfo.parallax) { this@draw.drawContent() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class MaskParallaxNodeElement(
    val index: Int,
    val state: CarouselParallaxScrollEffectState,
    val shape: Shape,
) : ModifierNodeElement<MaskParallaxNode>() {

    override fun create(): MaskParallaxNode = MaskParallaxNode(index, state, shape)

    override fun update(node: MaskParallaxNode) {
        node.index = index
        node.state = state
        node.shape = shape
        node.maskShape.index = index
        node.maskShape.state = state
        node.maskShape.shape = shape
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "maskParallaxNodeElement"
        properties["index"] = index
        properties["state"] = state
        properties["shape"] = shape
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MaskParallaxNodeElement) return false
        return index == other.index && state == other.state && shape == other.shape
    }

    override fun hashCode(): Int {
        var result = index.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class MaskShape(
    var index: Int,
    var state: CarouselParallaxScrollEffectState,
    var shape: Shape,
) : Shape {

    private val path = Path()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val itemInfo = state.calculateItemInfo(index)
        path.apply {
            reset()
            val shapeSize = size.toRect()
            val rect =
                if (itemInfo.size == 0f) {
                    shapeSize
                } else if (state.orientation == Orientation.Horizontal) {
                    Rect(
                        left = itemInfo.maskStart,
                        top = shapeSize.top,
                        right = itemInfo.maskEnd,
                        bottom = shapeSize.bottom,
                    )
                } else {
                    Rect(
                        left = shapeSize.left,
                        top = itemInfo.maskStart,
                        right = shapeSize.right,
                        bottom = itemInfo.maskEnd,
                    )
                }
            addOutline(shape.createOutline(rect.size, layoutDirection, density))
            translate(Offset(x = rect.left, y = rect.top))
            close()
        }

        return if (path.isEmpty) Outline.Rectangle(size.toRect()) else Outline.Generic(path)
    }
}

/** Visual effect values calculated for an item at a specific index in a scrollable container. */
@ExperimentalMaterial3Api
public class CarouselParallaxScrollEffectItemInfo(
    /** The offset in pixels from the start of the item's bounds by which it is masked. */
    public val maskStart: Float,

    /** The offset in pixels from the start of the item's bounds to the end of the visible mask. */
    public val maskEnd: Float,

    /** The distance in pixels to translate the item's content in the main scrolling axis. */
    public val parallax: Float,

    /** The progress of the mask effect from 0f (fully unmasked) to 1f (maximally masked). */
    @get:FloatRange(from = 0.0, to = 1.0)
    @param:FloatRange(from = 0.0, to = 1.0)
    public val maskProgress: Float,
) {
    /**
     * The current size of this item in the main scrolling axis taking into account any masking from
     * [maskStart] and [maskEnd].
     */
    public val size: Float
        get() = maxOf(0f, maskEnd - maskStart)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarouselParallaxScrollEffectItemInfo) return false

        if (maskStart != other.maskStart) return false
        if (maskEnd != other.maskEnd) return false
        if (parallax != other.parallax) return false
        if (maskProgress != other.maskProgress) return false

        return true
    }

    override fun hashCode(): Int {
        var result = maskStart.hashCode()
        result = 31 * result + maskEnd.hashCode()
        result = 31 * result + parallax.hashCode()
        result = 31 * result + maskProgress.hashCode()
        return result
    }

    override fun toString(): String {
        return "CarouselParallaxScrollEffectItemInfo(" +
            "maskStart=$maskStart, " +
            "maskEnd=$maskEnd, " +
            "parallax=$parallax, " +
            "maskProgress=$maskProgress, " +
            "size=$size)"
    }
}

/**
 * A state holder for scrollable items to track effect values that are applied by the
 * [carouselParallaxScrollEffect] modifier.
 *
 * Use one of the [CarouselParallaxScrollEffectState] functions to create an instance of this
 * interface using [LazyListState] or [LazyGridState], or implement the interface directly for a
 * custom layout.
 */
@ExperimentalMaterial3Api
public interface CarouselParallaxScrollEffectState {

    /**
     * Returns visual effect values ([CarouselParallaxScrollEffectItemInfo]) for the item at
     * [index].
     *
     * Note that this function should not be called during composition. Make sure to call
     * getItemInfo from a graphics, layout, or other Modifier.Node where changes to the underlying
     * info will not cause unwanted recompositions.
     */
    @FrequentlyChangingValue
    public fun calculateItemInfo(index: Int): CarouselParallaxScrollEffectItemInfo

    /** The scroll orientation of the container. */
    public val orientation: Orientation
}

/**
 * Create a [CarouselParallaxScrollEffectState] object for a
 * [androidx.compose.foundation.lazy.LazyRow] or [androidx.compose.foundation.lazy.LazyColumn].
 *
 * Remember a [CarouselParallaxScrollEffectState] for the LazyList and use
 * [carouselParallaxScrollEffect] on each item's container that should be clipped and translated to
 * create the effect.
 *
 * @sample androidx.compose.material3.samples.MultiAspectCarouselRowSample
 * @param state the [LazyListState] of the list
 */
@ExperimentalMaterial3Api
@RememberInComposition
public fun CarouselParallaxScrollEffectState(
    state: LazyListState
): CarouselParallaxScrollEffectState =
    object : CarouselParallaxScrollEffectState {
        private var cachedIndex: Int = -1
        private var cachedLayoutInfo: LazyListLayoutInfo? = null
        private var cachedItemInfo: CarouselParallaxScrollEffectItemInfo? = null

        override fun calculateItemInfo(index: Int): CarouselParallaxScrollEffectItemInfo {
            val layoutInfo = state.layoutInfo
            if (index == cachedIndex && layoutInfo === cachedLayoutInfo && cachedItemInfo != null) {
                return cachedItemInfo!!
            }

            val itemInfo = layoutInfo.visibleItemsInfo.fastFirstOrNull { it.index == index }
            val isItemVisible = itemInfo != null
            val itemMainAxisSize = itemInfo?.size?.toFloat() ?: 0f
            val itemCrossAxisSize =
                if (orientation == Orientation.Horizontal) {
                    layoutInfo.viewportSize.height.toFloat()
                } else {
                    layoutInfo.viewportSize.width.toFloat()
                }
            val itemOffset = itemInfo?.offset?.toFloat() ?: 0f
            val viewportStartOffset = layoutInfo.viewportStartOffset.toFloat()
            val viewportEndOffset = layoutInfo.viewportEndOffset.toFloat()

            val info =
                calculateItemInfo(
                    isItemVisible = isItemVisible,
                    itemMainAxisSize = itemMainAxisSize,
                    itemCrossAxisSize = itemCrossAxisSize,
                    itemOffset = itemOffset,
                    viewportStartOffset = viewportStartOffset,
                    viewportEndOffset = viewportEndOffset,
                )

            cachedIndex = index
            cachedLayoutInfo = layoutInfo
            cachedItemInfo = info
            return info
        }

        override val orientation: Orientation
            get() = state.layoutInfo.orientation
    }

/**
 * Create a [CarouselParallaxScrollEffectState] object for a
 * [androidx.compose.foundation.lazy.grid.LazyHorizontalGrid] or
 * [androidx.compose.foundation.lazy.grid.LazyVerticalGrid].
 *
 * Remember a [CarouselParallaxScrollEffectState] for the LazyGrid and use
 * [carouselParallaxScrollEffect] on each item's container that should be clipped and translated to
 * create the effect.
 *
 * @sample androidx.compose.material3.samples.MultiAspectCarouselRowSample
 * @param state the [LazyGridState] for the grid
 */
@ExperimentalMaterial3Api
@RememberInComposition
public fun CarouselParallaxScrollEffectState(
    state: LazyGridState
): CarouselParallaxScrollEffectState =
    object : CarouselParallaxScrollEffectState {
        private var cachedIndex: Int = -1
        private var cachedLayoutInfo: LazyGridLayoutInfo? = null
        private var cachedItemInfo: CarouselParallaxScrollEffectItemInfo? = null

        override fun calculateItemInfo(index: Int): CarouselParallaxScrollEffectItemInfo {
            val layoutInfo = state.layoutInfo
            if (index == cachedIndex && layoutInfo === cachedLayoutInfo && cachedItemInfo != null) {
                return cachedItemInfo!!
            }

            val itemInfo = layoutInfo.visibleItemsInfo.fastFirstOrNull { it.index == index }
            val isItemVisible = itemInfo != null
            val itemMainAxisSize =
                itemInfo
                    ?.size
                    ?.let { if (orientation == Orientation.Horizontal) it.width else it.height }
                    ?.toFloat() ?: 0f
            val itemCrossAxisSize =
                itemInfo
                    ?.size
                    ?.let { if (orientation == Orientation.Horizontal) it.height else it.width }
                    ?.toFloat() ?: 0f
            val itemOffset =
                itemInfo
                    ?.offset
                    ?.let { if (orientation == Orientation.Horizontal) it.x else it.y }
                    ?.toFloat() ?: 0f
            val viewportStartOffset = layoutInfo.viewportStartOffset.toFloat()
            val viewportEndOffset = layoutInfo.viewportEndOffset.toFloat()

            val info =
                calculateItemInfo(
                    isItemVisible = isItemVisible,
                    itemMainAxisSize = itemMainAxisSize,
                    itemCrossAxisSize = itemCrossAxisSize,
                    itemOffset = itemOffset,
                    viewportStartOffset = viewportStartOffset,
                    viewportEndOffset = viewportEndOffset,
                )

            cachedIndex = index
            cachedLayoutInfo = layoutInfo
            cachedItemInfo = info
            return info
        }

        override val orientation: Orientation
            get() = state.layoutInfo.orientation
    }

@OptIn(ExperimentalMaterial3Api::class)
private fun calculateItemInfo(
    isItemVisible: Boolean,
    itemMainAxisSize: Float,
    itemCrossAxisSize: Float,
    itemOffset: Float,
    viewportStartOffset: Float,
    viewportEndOffset: Float,
): CarouselParallaxScrollEffectItemInfo {
    if (!isItemVisible || itemMainAxisSize == 0f) {
        return EmptyCarouselParallaxScrollEffectItemInfo
    }

    val maskIntensity = getMaskIntensity(itemMainAxisSize, itemCrossAxisSize)
    val parallaxDistance = itemMainAxisSize * maskIntensity

    // When the item is entering/exiting the left/top side of the viewport
    if (itemOffset < viewportStartOffset) {
        val offscreenDistance = viewportStartOffset - itemOffset
        val maskProgress = (offscreenDistance / itemMainAxisSize).coerceIn(0f, 1f)
        return CarouselParallaxScrollEffectItemInfo(
            maskStart = lerp(0f, itemMainAxisSize * (1f - maskIntensity), maskProgress),
            maskEnd = itemMainAxisSize,
            parallax = lerp(0f, parallaxDistance, maskProgress),
            maskProgress = maskProgress,
        )
    }

    // When the item is entering/exiting the right/bottom side of the viewport
    if (itemOffset > viewportEndOffset - itemMainAxisSize) {
        val offscreenDistance = -(viewportEndOffset - itemOffset - itemMainAxisSize)
        val maskProgress = (offscreenDistance / itemMainAxisSize).coerceIn(0f, 1f)
        return CarouselParallaxScrollEffectItemInfo(
            maskStart = 0f,
            maskEnd = lerp(itemMainAxisSize, itemMainAxisSize * maskIntensity, maskProgress),
            parallax = -lerp(0f, parallaxDistance, maskProgress),
            maskProgress = maskProgress,
        )
    }

    return CarouselParallaxScrollEffectItemInfo(
        maskStart = 0f,
        maskEnd = itemMainAxisSize,
        parallax = 0f,
        maskProgress = 0f,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
private val EmptyCarouselParallaxScrollEffectItemInfo =
    CarouselParallaxScrollEffectItemInfo(
        maskStart = 0f,
        maskEnd = 0f,
        parallax = 0f,
        maskProgress = 0f,
    )

private fun getMaskIntensity(itemMainAxisSize: Float, itemCrossAxisSize: Float): Float {
    // Mask intensity is based on the item's aspect ratio
    val ar = itemMainAxisSize / itemCrossAxisSize
    return when {
        ar > 16 / 9f -> 1 / 2f
        ar < 16 / 9f && ar > 1f ->
            lerp(
                outputMin = 1 / 3f,
                outputMax = 1 / 2f,
                inputMin = 1f,
                inputMax = 16 / 9f,
                value = ar,
            )
        ar < 1f && ar > 9 / 16f ->
            lerp(
                outputMin = 1 / 4f,
                outputMax = 1 / 3f,
                inputMin = 9 / 16f,
                inputMax = 1f,
                value = ar,
            )
        else -> 1 / 4f
    }
}
