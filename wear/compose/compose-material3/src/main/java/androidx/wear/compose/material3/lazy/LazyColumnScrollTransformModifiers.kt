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

package androidx.wear.compose.material3.lazy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastRoundToInt
import androidx.wear.compose.foundation.lazy.LazyColumnItemScope
import androidx.wear.compose.foundation.lazy.LazyColumnItemScrollProgress
import org.jetbrains.annotations.TestOnly

/**
 * This modifier provides the height of the target composable to the [scrollTransform] during a
 * morph transition and represents minimum height of the item when morphed.
 *
 * Should be applied to a single child element or none at all (in which case, the morph effect is
 * disabled). When applied to multiple child elements, the last placed child's height we be used for
 * morphing.
 *
 * @sample androidx.wear.compose.material3.samples.LazyColumnTargetMorphingHeightSample
 * @param scope The LazyColumnItemScope provides access to the item's index and key.
 */
fun Modifier.targetMorphingHeight(
    @Suppress("UNUSED_PARAMETER") scope: LazyColumnItemScope,
): Modifier = this then TargetMorphingHeightProviderModifierElement()

/**
 * A modifier that enables Material3 Motion transformations for content within a LazyColumn item. It
 * also draws the background behind the content using Material3 Motion transformations. There is
 * also an overload that applies the same visual transformations to the background.
 *
 * This modifier calculates and applies transformations to the content based on the
 * [LazyColumnItemScrollProgress] of the item inside the LazyColumn. It adjusts the height,
 * position, applies scaling and morphing effects as the item scrolls.
 *
 * @sample androidx.wear.compose.material3.samples.LazyColumnScalingMorphingEffectSample
 * @param scope The LazyColumnItemScope provides access to the item's index and key.
 * @param backgroundColor Color of the background.
 * @param shape Shape of the background.
 */
@Composable
fun Modifier.scrollTransform(
    scope: LazyColumnItemScope,
    backgroundColor: Color,
    shape: Shape = RectangleShape
): Modifier =
    with(scope) {
        var minMorphingHeight by remember { mutableStateOf<Float?>(null) }
        val spec = remember { LazyColumnScrollTransformBehavior { minMorphingHeight } }
        val painter =
            remember(backgroundColor, shape) {
                ScalingMorphingBackgroundPainter(spec, shape, backgroundColor) { scrollProgress }
            }

        this@scrollTransform then
            TargetMorphingHeightConsumerModifierElement { minMorphingHeight = it?.toFloat() } then
            this@scrollTransform.paint(painter) then
            this@scrollTransform.transformedHeight { height, scrollProgress ->
                with(spec) { scrollProgress.placementHeight(height.toFloat()).fastRoundToInt() }
            } then
            this@scrollTransform.graphicsLayer { contentTransformation(spec) { scrollProgress } }
    }

/**
 * A modifier that enables Material3 Motion transformations for content within a LazyColumn item.
 *
 * This modifier calculates and applies transformations to the content and background based on the
 * [LazyColumnItemScrollProgress] of the item inside the LazyColumn. It adjusts the height,
 * position, applies scaling and morphing effects as the item scrolls.
 *
 * @sample androidx.wear.compose.material3.samples.LazyColumnScalingMorphingEffectSample
 * @param scope The LazyColumnItemScope provides access to the item's index and key.
 */
@Composable
fun Modifier.scrollTransform(
    scope: LazyColumnItemScope,
): Modifier =
    with(scope) {
        var minMorphingHeight by remember { mutableStateOf<Float?>(null) }
        val spec = remember { LazyColumnScrollTransformBehavior { minMorphingHeight } }

        this@scrollTransform then
            TargetMorphingHeightConsumerModifierElement { minMorphingHeight = it?.toFloat() } then
            this@scrollTransform.transformedHeight { height, scrollProgress ->
                with(spec) { scrollProgress.placementHeight(height.toFloat()).fastRoundToInt() }
            } then
            this@scrollTransform.graphicsLayer { contentTransformation(spec) { scrollProgress } }
    }

private fun GraphicsLayerScope.contentTransformation(
    spec: LazyColumnScrollTransformBehavior,
    scrollProgress: () -> LazyColumnItemScrollProgress?
) =
    with(spec) {
        scrollProgress()?.let {
            clip = true
            shape =
                object : Shape {
                    override fun createOutline(
                        size: Size,
                        layoutDirection: LayoutDirection,
                        density: Density
                    ): Outline =
                        Outline.Rounded(
                            RoundRect(
                                rect =
                                    Rect(
                                        left = 0f,
                                        top = 0f,
                                        right =
                                            size.width -
                                                2f * size.width * it.contentXOffsetFraction,
                                        bottom = it.morphedHeight(size.height)
                                    ),
                            )
                        )
                }
            translationX = size.width * it.contentXOffsetFraction * it.scale
            translationY = -1f * size.height * (1f - it.scale) / 2f
            alpha = it.contentAlpha.coerceAtMost(0.99f) // Alpha hack.
            scaleX = it.scale
            scaleY = it.scale
        }
    }

private class ScalingMorphingBackgroundPainter(
    private val spec: LazyColumnScrollTransformBehavior,
    private val shape: Shape,
    private val backgroundColor: Color,
    private val progress: DrawScope.() -> LazyColumnItemScrollProgress?
) : Painter() {
    override val intrinsicSize: Size
        get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        with(spec) {
            progress()?.let {
                val scale = it.scale
                val xOffset =
                    size.width * (1f - scale) / 2f +
                        it.backgroundXOffsetFraction * size.width * scale
                val width =
                    size.width * scale - 2f * it.backgroundXOffsetFraction * size.width * scale
                translate(xOffset, 0f) {
                    drawOutline(
                        outline =
                            shape.createOutline(
                                Size(width, it.placementHeight(size.height)),
                                layoutDirection,
                                this
                            ),
                        color = backgroundColor,
                        alpha = it.backgroundAlpha,
                    )
                }
            }
        }
    }
}

private const val TargetMorphingHeightTraversalKey = "TargetMorphingHeight"

private class TargetMorphingHeightProviderModifierNode :
    Modifier.Node(), TraversableNode, LayoutAwareModifierNode {
    override val traverseKey = TargetMorphingHeightTraversalKey

    private fun reportMinMorphingHeight(height: Int) {
        traverseAncestors(traverseKey) {
            if (it is TargetMorphingHeightConsumerModifierNode) {
                it.onMinMorphingHeightChanged(height)
                false
            } else {
                true
            }
        }
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        reportMinMorphingHeight(coordinates.size.height)
    }

    override fun onRemeasured(size: IntSize) {
        reportMinMorphingHeight(size.height)
    }
}

private class TargetMorphingHeightProviderModifierElement :
    ModifierNodeElement<TargetMorphingHeightProviderModifierNode>() {
    override fun create(): TargetMorphingHeightProviderModifierNode =
        TargetMorphingHeightProviderModifierNode()

    override fun update(node: TargetMorphingHeightProviderModifierNode) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "TargetMorphingHeightProviderModifierElement"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TargetMorphingHeightConsumerModifierElement) return false
        return true
    }

    override fun hashCode(): Int {
        return 42
    }
}

@TestOnly
internal fun Modifier.minMorphingHeightConsumer(
    onMinMorphingHeightChanged: (Int?) -> Unit
): Modifier = this then TargetMorphingHeightConsumerModifierElement(onMinMorphingHeightChanged)

private class TargetMorphingHeightConsumerModifierNode(
    var onMinMorphingHeightChanged: (Int?) -> Unit
) : Modifier.Node(), TraversableNode {
    override val traverseKey = TargetMorphingHeightTraversalKey
}

private class TargetMorphingHeightConsumerModifierElement(
    val onMinMorphingHeightChanged: (Int?) -> Unit
) : ModifierNodeElement<TargetMorphingHeightConsumerModifierNode>() {
    override fun create() = TargetMorphingHeightConsumerModifierNode(onMinMorphingHeightChanged)

    override fun update(node: TargetMorphingHeightConsumerModifierNode) {
        node.onMinMorphingHeightChanged = onMinMorphingHeightChanged
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "TargetMorphingHeightConsumerModifierElement"
        properties["onMinMorphingHeightChanged"] = onMinMorphingHeightChanged
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TargetMorphingHeightConsumerModifierElement) return false
        return onMinMorphingHeightChanged === other.onMinMorphingHeightChanged
    }

    override fun hashCode(): Int {
        return onMinMorphingHeightChanged.hashCode()
    }
}
