/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.animation.demos.lookahead

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.DeferredTargetAnimation
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.CoroutineScope

@Composable
fun SceneHost(modifier: Modifier = Modifier, content: @Composable SceneScope.() -> Unit) {
    Box(modifier) {
        LookaheadScope {
            val sceneScope = remember { SceneScope(this) }
            sceneScope.content()
        }
    }
}

private const val debugSharedElement = true

class SceneScope internal constructor(lookaheadScope: LookaheadScope) :
    LookaheadScope by lookaheadScope {
    fun Modifier.sharedElement(): Modifier = this then SharedElementElement(this@SceneScope)
}

private data class SharedElementElement(val lookaheadScope: LookaheadScope) :
    ModifierNodeElement<SharedElementNode>() {
    override fun InspectorInfo.inspectableProperties() {
        name = "sharedElement"
    }

    override fun create() = SharedElementNode(lookaheadScope)

    override fun update(node: SharedElementNode) {
        node.lookaheadScope = lookaheadScope
    }
}

private class SharedElementNode(var lookaheadScope: LookaheadScope) :
    ApproachLayoutModifierNode, DrawModifierNode, Modifier.Node() {
    private val offsetAnimation: DeferredTargetAnimation<IntOffset, AnimationVector2D> =
        DeferredTargetAnimation(IntOffset.VectorConverter)
    private val sizeAnimation: DeferredTargetAnimation<IntSize, AnimationVector2D> =
        DeferredTargetAnimation(IntSize.VectorConverter)

    private var placementOffset: IntOffset by mutableStateOf(IntOffset.Zero)

    override fun ContentDrawScope.draw() {
        if (debugSharedElement) {
            drawRect(
                color = Color.Black,
                style = Stroke(2f),
                topLeft = (offsetAnimation.pendingTarget!! - placementOffset).toOffset(),
                size = sizeAnimation.pendingTarget!!.toSize(),
            )
        }
        drawContent()
    }

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean {
        sizeAnimation.updateTarget(lookaheadSize, coroutineScope)
        return !sizeAnimation.isIdle
    }

    override fun Placeable.PlacementScope.isPlacementApproachInProgress(
        lookaheadCoordinates: LayoutCoordinates
    ): Boolean {
        val target =
            with(lookaheadScope) {
                lookaheadScopeCoordinates.localLookaheadPositionOf(lookaheadCoordinates)
            }
        offsetAnimation.updateTarget(target.round(), coroutineScope, spring())
        return !offsetAnimation.isIdle
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val (width, height) =
            sizeAnimation.updateTarget(
                lookaheadSize,
                coroutineScope,
                spring(stiffness = Spring.StiffnessMediumLow),
            )
        val animatedConstraints = Constraints.fixed(width, height)
        val placeable = measurable.measure(animatedConstraints)
        return layout(placeable.width, placeable.height) {
            val (x, y) =
                offsetAnimation.updateTargetBasedOnCoordinates(
                    lookaheadScope,
                    this@layout,
                    coroutineScope,
                    spring(stiffness = Spring.StiffnessMediumLow),
                )
            coordinates?.let {
                placementOffset =
                    with(lookaheadScope) {
                        lookaheadScopeCoordinates.localPositionOf(it, Offset.Zero).round()
                    }
            }
            placeable.place(x, y)
        }
    }
}

fun Modifier.animateSizeAndSkipToFinalLayout(): Modifier =
    this then AnimateSizeAndSkipToFinalLayoutElement

private data object AnimateSizeAndSkipToFinalLayoutElement :
    ModifierNodeElement<AnimateSizeAndSkipToFinalLayoutNode>() {
    override fun InspectorInfo.inspectableProperties() {
        name = "animateSizeAndSkipToFinalLayout"
    }

    override fun create() = AnimateSizeAndSkipToFinalLayoutNode()

    override fun update(node: AnimateSizeAndSkipToFinalLayoutNode) {}
}

private class AnimateSizeAndSkipToFinalLayoutNode :
    ApproachLayoutModifierNode, DrawModifierNode, Modifier.Node() {
    private val sizeAnimation = DeferredTargetAnimation(IntSize.VectorConverter)
    private var targetSize: IntSize? by mutableStateOf(null)

    override fun ContentDrawScope.draw() {
        if (debugSharedElement) {
            drawRect(
                color = Color.Black,
                style = Stroke(2f),
                topLeft = Offset.Zero,
                size = targetSize!!.toSize(),
            )
        }
        drawContent()
    }

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean {
        sizeAnimation.updateTarget(lookaheadSize, coroutineScope)
        return !sizeAnimation.isIdle
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        targetSize = lookaheadSize
        val (width, height) = sizeAnimation.updateTarget(lookaheadSize, coroutineScope)
        val placeable =
            measurable.measure(Constraints.fixed(lookaheadSize.width, lookaheadSize.height))
        // Make sure the content is aligned to topStart
        val wrapperWidth = width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val wrapperHeight = height.coerceIn(constraints.minHeight, constraints.maxHeight)
        return layout(width, height) {
            placeable.place(-(wrapperWidth - width) / 2, -(wrapperHeight - height) / 2)
        }
    }
}

internal fun DeferredTargetAnimation<IntOffset, AnimationVector2D>.updateTargetBasedOnCoordinates(
    lookaheadScope: LookaheadScope,
    placementScope: Placeable.PlacementScope,
    coroutineScope: CoroutineScope,
    animationSpec: FiniteAnimationSpec<IntOffset>,
): IntOffset {
    with(lookaheadScope) {
        with(placementScope) {
            coordinates?.let { coordinates ->
                with(placementScope) {
                    val targetOffset =
                        lookaheadScopeCoordinates.localLookaheadPositionOf(coordinates)
                    val animOffset =
                        updateTarget(targetOffset.round(), coroutineScope, animationSpec)
                    val current =
                        lookaheadScopeCoordinates.localPositionOf(coordinates, Offset.Zero).round()
                    return (animOffset - current)
                }
            }

            return IntOffset.Zero
        }
    }
}
