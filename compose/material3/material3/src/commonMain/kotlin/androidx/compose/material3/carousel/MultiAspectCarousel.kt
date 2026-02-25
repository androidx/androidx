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

package androidx.compose.material3.carousel

import androidx.collection.FloatFloatPair
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
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

/**
 * Creates a multi-apsect carousel scope that includes all related methods for creating a
 * multi-aspect carousel.
 *
 * Note: [MultiAspectCarouselScope] does not introduce a new [androidx.compose.ui.layout.Layout] to
 * the [content] passed in. All the [androidx.compose.ui.layout.Layout]s in the [content] will have
 * the same parent as they would without [MultiAspectCarouselScope].
 *
 * @sample androidx.compose.material3.samples.MultiAspectCarouselLazyRowSample
 */
@Composable
fun MultiAspectCarouselScope(content: @Composable MultiAspectCarouselScope.() -> Unit) {
    val scope = remember { MultiAspectCarouselScopeImpl() }
    scope.content()
}

/**
 * Create a [MultiAspectCarouselItemDrawInfo] object for an item at the given [index] in a
 * [androidx.compose.foundation.lazy.LazyRow] or [androidx.compose.foundation.lazy.LazyColumn].
 *
 * Remember a [MultiAspectCarouselItemDrawInfo] for each item in the LazyList and use
 * [MultiAspectCarouselScope.maskClip] on the item's outermost container to mask and parallax the
 * item on scroll.
 *
 * @sample androidx.compose.material3.samples.MultiAspectCarouselLazyRowSample
 * @param index the index of the LazyList item this draw info is being used for
 * @param state the [LazyListState] of the list this item belongs to
 */
@ExperimentalMaterial3Api
fun MultiAspectCarouselItemDrawInfo(
    index: Int,
    state: LazyListState,
): MultiAspectCarouselItemDrawInfo =
    MultiAspectCarouselItemDrawInfoImpl(
        index,
        object : MultiAspectCarouselContainerState {
            override val viewportEndOffset: Float
                get() = state.layoutInfo.viewportEndOffset.toFloat()

            override val viewportStartOffset: Float
                get() = state.layoutInfo.viewportStartOffset.toFloat()

            override val orientation: Orientation
                get() = state.layoutInfo.orientation
        },
        object : MultiAspectCarouselItemInfoState {
            private fun getInfo(): LazyListItemInfo? =
                state.layoutInfo.visibleItemsInfo.fastFirstOrNull { it.index == index }

            override val isVisible: Boolean
                get() = getInfo() != null

            override val crossAxisSize: Float
                get() =
                    if (state.layoutInfo.orientation == Orientation.Horizontal) {
                        state.layoutInfo.viewportSize.height.toFloat()
                    } else {
                        state.layoutInfo.viewportSize.width.toFloat()
                    }

            override val mainAxisSize: Float
                get() = getInfo()?.size?.toFloat() ?: 0f

            override val offset: Float
                get() = getInfo()?.offset?.toFloat() ?: 0f
        },
    )

/**
 * Create a [MultiAspectCarouselItemDrawInfo] object for an item at the given [index] in a
 * [androidx.compose.foundation.lazy.grid.LazyHorizontalGrid] or
 * [androidx.compose.foundation.lazy.grid.LazyVerticalGrid].
 *
 * Remember a [MultiAspectCarouselItemDrawInfo] for each item in the LazyList and use
 * [MultiAspectCarouselScope.maskClip] on the item's outermost container to mask and parallax the
 * item on scroll.
 *
 * @param index the index of the LazyGrid item this draw info is being used for
 * @param state the [LazyGridState] of the list this item belongs to
 */
@ExperimentalMaterial3Api
fun MultiAspectCarouselItemDrawInfo(
    index: Int,
    state: LazyGridState,
): MultiAspectCarouselItemDrawInfo =
    MultiAspectCarouselItemDrawInfoImpl(
        index,
        object : MultiAspectCarouselContainerState {
            override val viewportEndOffset: Float
                get() = state.layoutInfo.viewportEndOffset.toFloat()

            override val viewportStartOffset: Float
                get() = state.layoutInfo.viewportStartOffset.toFloat()

            override val orientation: Orientation
                get() = state.layoutInfo.orientation
        },
        object : MultiAspectCarouselItemInfoState {
            private fun getInfo(): LazyGridItemInfo? =
                state.layoutInfo.visibleItemsInfo.fastFirstOrNull { it.index == index }

            private val isHorizontal: Boolean
                get() = state.layoutInfo.orientation == Orientation.Horizontal

            override val isVisible: Boolean
                get() = getInfo() != null

            override val crossAxisSize: Float
                get() =
                    getInfo()?.size?.let { if (isHorizontal) it.height else it.width }?.toFloat()
                        ?: 0f

            override val mainAxisSize: Float
                get() =
                    getInfo()?.size?.let { if (isHorizontal) it.width else it.height }?.toFloat()
                        ?: 0f

            override val offset: Float
                get() = getInfo()?.offset?.let { if (isHorizontal) it.x else it.y }?.toFloat() ?: 0f
        },
    )

/**
 * A scope containing all methods used to create a multi-aspect carousel from a
 * [androidx.compose.foundation.lazy.LazyRow] or [androidx.compose.foundation.lazy.LazyColumn].
 *
 * For each item in a lazy list, remember a [MultiAspectCarouselItemDrawInfo] using and then modify
 * items with [maskClip] to create a parallax masking effect as items enter and exit the scrolling
 * container.
 *
 * @sample androidx.compose.material3.samples.MultiAspectCarouselLazyRowSample
 */
interface MultiAspectCarouselScope {

    /**
     * Clip and parallax a composable item in a LazyLayout to the given [shape] according to mask
     * and parallax values from [multiAspectItemDrawInfo].
     *
     * @sample androidx.compose.material3.samples.MultiAspectCarouselLazyRowSample
     * @param shape the shape to be applied to the composable
     * @param multiAspectItemDrawInfo The draw info whose details will be used for the shape's
     *   bounds in the main axis and the parallax effect
     */
    @ExperimentalMaterial3Api
    fun Modifier.maskClip(
        shape: Shape,
        multiAspectItemDrawInfo: MultiAspectCarouselItemDrawInfo,
    ): Modifier

    /**
     * Draw a border on a composable item in a LazyLayout using the given [shape] according to the
     * mask values from [multiAspectItemDrawInfo].
     *
     * Apply [maskBorder] before [maskClip] to avoid clipping the border.
     *
     * @param border the border to be drawn around the composable
     * @param shape the shape of the border
     * @param multiAspectItemDrawInfo the draw info whose details will be used for the shape's
     *   bounds in the main axis
     */
    @ExperimentalMaterial3Api
    @Composable
    fun Modifier.maskBorder(
        border: BorderStroke,
        shape: Shape,
        multiAspectItemDrawInfo: MultiAspectCarouselItemDrawInfo,
    ): Modifier
}

private class MultiAspectCarouselScopeImpl : MultiAspectCarouselScope {

    @ExperimentalMaterial3Api
    override fun Modifier.maskClip(
        shape: Shape,
        multiAspectItemDrawInfo: MultiAspectCarouselItemDrawInfo,
    ): Modifier = this then MaskParallaxNodeElement(shape, multiAspectItemDrawInfo)

    @ExperimentalMaterial3Api
    @Composable
    override fun Modifier.maskBorder(
        border: BorderStroke,
        shape: Shape,
        multiAspectItemDrawInfo: MultiAspectCarouselItemDrawInfo,
    ): Modifier =
        border(
            border,
            remember { MaskShape(baseShape = shape, drawInfo = multiAspectItemDrawInfo) },
        )
}

@OptIn(ExperimentalMaterial3Api::class)
private class MaskParallaxNode(baseShape: Shape, var drawInfo: MultiAspectCarouselItemDrawInfo) :
    LayoutModifierNode, DrawModifierNode, Modifier.Node() {

    val maskShape = MaskShape(baseShape, drawInfo)

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
        if (drawInfo.isHorizontal) {
            translate(left = drawInfo.parallax) { this@draw.drawContent() }
        } else {
            translate(top = drawInfo.parallax) { this@draw.drawContent() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class MaskParallaxNodeElement(
    val baseShape: Shape,
    val drawInfo: MultiAspectCarouselItemDrawInfo,
) : ModifierNodeElement<MaskParallaxNode>() {

    override fun create(): MaskParallaxNode = MaskParallaxNode(baseShape, drawInfo)

    override fun update(node: MaskParallaxNode) {
        node.maskShape.baseShape = baseShape
        node.maskShape.drawInfo = drawInfo
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "maskParallaxNodeElement"
        properties["baseShape"] = baseShape
        properties["drawInfo"] = drawInfo
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MaskParallaxNodeElement) return false
        return baseShape == other.baseShape && drawInfo == other.drawInfo
    }

    override fun hashCode(): Int {
        var result = baseShape.hashCode()
        result = 31 * result + drawInfo.hashCode()
        return result
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class MaskShape(var baseShape: Shape, var drawInfo: MultiAspectCarouselItemDrawInfo) :
    Shape {

    private val path = Path()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        path.apply {
            reset()
            val shapeSize = size.toRect()
            val rect =
                if (drawInfo.size == 0f) {
                    shapeSize
                } else if (drawInfo.isHorizontal) {
                    Rect(
                        left = drawInfo.maskStart,
                        top = shapeSize.top,
                        right = drawInfo.maskEnd,
                        bottom = shapeSize.bottom,
                    )
                } else {
                    Rect(
                        left = shapeSize.left,
                        top = drawInfo.maskStart,
                        right = shapeSize.right,
                        bottom = drawInfo.maskEnd,
                    )
                }
            addOutline(baseShape.createOutline(rect.size, layoutDirection, density))
            translate(Offset(x = rect.left, y = rect.top))
            close()
        }

        return if (path.isEmpty) Outline.Rectangle(size.toRect()) else Outline.Generic(path)
    }
}

private interface MultiAspectCarouselContainerState {
    val viewportEndOffset: Float
    val viewportStartOffset: Float
    val orientation: Orientation
}

private interface MultiAspectCarouselItemInfoState {
    val isVisible: Boolean
    val crossAxisSize: Float
    val mainAxisSize: Float
    val offset: Float
}

/**
 * Interface to hold information about a multi-aspect carousel item and its draw info that change as
 * the item scrolls.
 *
 * Example of MultiAspectCarouselItemDrawInfo usage:
 *
 * @sample androidx.compose.material3.samples.MultiAspectCarouselLazyRowSample
 */
@ExperimentalMaterial3Api
sealed interface MultiAspectCarouselItemDrawInfo {

    /** The index of this item in the list. */
    val index: Int

    /**
     * The current size of this item in the main scrolling axis taking into account any masking from
     * maskStart and maskEnd.
     */
    @get:FrequentlyChangingValue val size: Float

    /**
     * The smallest size this item will ever be masked to in the main scrolling axis. [size] will
     * never be less than [minSize].
     */
    val minSize: Float

    /**
     * The maximum size this item will be in the main scrolling axis. This is the fully unmasked
     * size of the item with no mask applied. [size] will never be greater than [maxSize].
     */
    val maxSize: Float

    /**
     * The offset in pixels from the start of this item's bounds by which the item should be masked.
     *
     * When this item exists the start or top of the viewport, maskStart will increase to make it
     * look like this item is being squeezed against the edge of the viewport.
     */
    @get:FrequentlyChangingValue val maskStart: Float

    /**
     * The offset in pixels from the end of this item's bounds by which the item should be masked.
     *
     * When this item exists the end or bottom of the viewport, maskEnd will increase to make it
     * look like this item is being squeezed against the edge of the viewport.
     */
    @get:FrequentlyChangingValue val maskEnd: Float

    /**
     * The distance in pixels to translate this item's content in the main scrolling axis.
     *
     * When an item is exiting the viewport, parallax will translate the content in the opposite
     * direction of scroll, making it look like the item is moving more slowly and being compressed
     * against the edge of the viewport.
     */
    @get:FrequentlyChangingValue val parallax: Float

    /** True if the main scrolling axis is horizontal. */
    val isHorizontal: Boolean
}

@OptIn(ExperimentalMaterial3Api::class)
private class MultiAspectCarouselItemDrawInfoImpl(
    override val index: Int,
    private val containerState: MultiAspectCarouselContainerState,
    private val itemState: MultiAspectCarouselItemInfoState,
) : MultiAspectCarouselItemDrawInfo {

    override val size: Float
        @FrequentlyChangingValue get() = maskEnd - maskStart

    override val minSize: Float
        get() = getMinSize(itemState)

    override val maxSize: Float
        get() = itemState.mainAxisSize

    override val maskStart: Float
        @FrequentlyChangingValue get() = getMask(containerState, itemState).first

    override val maskEnd: Float
        @FrequentlyChangingValue get() = getMask(containerState, itemState).second

    override val parallax: Float
        @FrequentlyChangingValue get() = getParallax(containerState, itemState)

    override val isHorizontal: Boolean
        get() = containerState.orientation == Orientation.Horizontal

    override fun toString(): String {
        return "MultiAspectCarouselItemDrawInfoImpl(" +
            "size=$size," +
            "minSize=$minSize," +
            "maxSize=$maxSize," +
            "maskStart=$maskStart," +
            "maskEnd=$maskEnd," +
            "parallax=$parallax," +
            "isHorizontal=$isHorizontal," +
            ")"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MultiAspectCarouselItemDrawInfoImpl) return false
        return index == other.index &&
            size == other.size &&
            minSize == other.minSize &&
            maxSize == other.maxSize &&
            maskStart == other.maskStart &&
            maskEnd == other.maskEnd &&
            parallax == other.parallax &&
            isHorizontal == other.isHorizontal
    }

    override fun hashCode(): Int {
        var result = index.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + minSize.hashCode()
        result = 31 * result + maxSize.hashCode()
        result = 31 * result + maskStart.hashCode()
        result = 31 * result + maskEnd.hashCode()
        result = 31 * result + parallax.hashCode()
        result = 31 * result + isHorizontal.hashCode()
        return result
    }
}

private fun getParallax(
    state: MultiAspectCarouselContainerState,
    itemState: MultiAspectCarouselItemInfoState,
): Float {
    if (!itemState.isVisible) return 0f

    val mainAxisSize = itemState.mainAxisSize
    val crossAxisSize = itemState.crossAxisSize
    // Offscreen threshold is the distance an item has to scroll outside of the viewport to become
    // fully masked and parallaxed. The threshold is based on a percentage of the item's size
    // which keeps the effect looking uniform even when item's differ greatly in their main
    // axis size.
    val offscreenThreshold = mainAxisSize
    val offset = itemState.offset

    // Mask intensity is based on the item's aspect ratio
    val maskIntensity = getMaskIntensity(mainAxisSize, crossAxisSize)
    val parallaxDistance = mainAxisSize * maskIntensity

    // When the item is entering/exiting the left side of the viewport
    if (offset < state.viewportStartOffset) {
        val offscreenDistance = state.viewportStartOffset - offset
        // Parallax
        return lerp(
            outputMin = 0f,
            outputMax = parallaxDistance,
            inputMin = 0f,
            inputMax = offscreenThreshold,
            value = offscreenDistance,
        )
    }

    // When the item is entering/exiting the right side of the viewport
    if (offset > state.viewportEndOffset - mainAxisSize) {
        val offscreenDistance = -(state.viewportEndOffset - offset - mainAxisSize)

        // Parallax
        return -lerp(
            outputMin = 0f,
            outputMax = parallaxDistance,
            inputMin = 0f,
            inputMax = offscreenThreshold,
            value = offscreenDistance,
        )
    }

    return 0f
}

private fun getMask(
    state: MultiAspectCarouselContainerState,
    itemState: MultiAspectCarouselItemInfoState,
): FloatFloatPair {
    if (!itemState.isVisible) return FloatFloatPair(0f, 0f)

    val mainAxisSize: Float = itemState.mainAxisSize
    val crossAxisSize: Float = itemState.crossAxisSize
    // Offscreen threshold is the distance an item has to scroll outside of the viewport to become
    // fully masked and parallaxed. The threshold is based on the item's size
    // which keeps the effect looking uniform even when item's differ greatly in their main
    // axis size.
    val offscreenThreshold = mainAxisSize
    val offset = itemState.offset

    // Mask intensity is based on the item's aspect ratio
    val maskIntensity = getMaskIntensity(mainAxisSize, crossAxisSize)

    // When the item is entering/exiting the left side of the viewport
    if (offset < state.viewportStartOffset) {
        val offscreenDistance = state.viewportStartOffset - offset
        // Mask
        val maskLeft =
            lerp(
                outputMin = 0f,
                outputMax = mainAxisSize * (1f - maskIntensity), // max mask
                inputMin = 0f,
                inputMax = offscreenThreshold,
                value = offscreenDistance,
            )
        return FloatFloatPair(maskLeft, mainAxisSize)
    }

    // When the item is entering/exiting the right side of the viewport
    if (offset > state.viewportEndOffset - mainAxisSize) {
        val offscreenDistance = -(state.viewportEndOffset - offset - mainAxisSize)

        // Mask
        val maskRight =
            lerp(
                outputMin = mainAxisSize,
                outputMax = mainAxisSize * maskIntensity,
                inputMin = 0f,
                inputMax = offscreenThreshold,
                value = offscreenDistance,
            )
        return FloatFloatPair(0f, maskRight)
    }

    return FloatFloatPair(0f, mainAxisSize)
}

private fun getMinSize(itemState: MultiAspectCarouselItemInfoState): Float {
    if (!itemState.isVisible) return 0f

    val maskIntensity = getMaskIntensity(itemState.mainAxisSize, itemState.crossAxisSize)
    return itemState.mainAxisSize * (1f - maskIntensity)
}

private fun getMaskIntensity(mainAxisSize: Float, crossAxisSize: Float): Float {
    // Mask intensity is based on the item's aspect ratio
    val ar = mainAxisSize / crossAxisSize
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
