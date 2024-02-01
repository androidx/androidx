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

package androidx.compose.ui.node

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset

internal class TailModifierNode : Modifier.Node() {
    init {
        // aggregateChildKindSet defaults to all bits being set, and is expected to be set later.
        // We can deterministically set the tail's because the tail will never have children by
        // definition.
        aggregateChildKindSet = 0
    }
    // BackwardsCompatNode uses this to determine if it is in a "chain update" or not. If attach
    // has been run on the tail node, then we can assume that it is a chain update. Importantly,
    // this is different than using isAttached.
    var attachHasBeenRun = false
    override fun toString(): String {
        return "<tail>"
    }

    override fun onAttach() {
        attachHasBeenRun = true
    }

    override fun onDetach() {
        attachHasBeenRun = false
    }
}

internal class InnerNodeCoordinator(
    layoutNode: LayoutNode
) : NodeCoordinator(layoutNode) {
    @OptIn(ExperimentalComposeUiApi::class)
    override val tail = TailModifierNode()

    init {
        @OptIn(ExperimentalComposeUiApi::class)
        tail.updateCoordinator(this)
    }

    override var lookaheadDelegate: LookaheadDelegate? =
        if (layoutNode.lookaheadRoot != null) LookaheadDelegateImpl() else null

    private inner class LookaheadDelegateImpl : LookaheadDelegate(this@InnerNodeCoordinator) {

        // Lookahead measure
        override fun measure(constraints: Constraints): Placeable =
            performingMeasure(constraints) {
                // before rerunning the user's measure block reset previous measuredByParent for children
                layoutNode.forEachChild {
                    it.lookaheadPassDelegate!!.measuredByParent =
                        LayoutNode.UsageByParent.NotUsed
                }
                val measureResult = with(layoutNode.measurePolicy) {
                    measure(
                        layoutNode.childLookaheadMeasurables,
                        constraints
                    )
                }
                measureResult
            }

        override fun calculateAlignmentLine(alignmentLine: AlignmentLine): Int {
            return (alignmentLinesOwner
                .calculateAlignmentLines()[alignmentLine] ?: AlignmentLine.Unspecified).also {
                cachedAlignmentLinesMap[alignmentLine] = it
            }
        }

        override fun placeChildren() {
            layoutNode.lookaheadPassDelegate!!.onNodePlaced()
        }

        override fun minIntrinsicWidth(height: Int) =
            layoutNode.intrinsicsPolicy.minLookaheadIntrinsicWidth(height)

        override fun minIntrinsicHeight(width: Int) =
            layoutNode.intrinsicsPolicy.minLookaheadIntrinsicHeight(width)

        override fun maxIntrinsicWidth(height: Int) =
            layoutNode.intrinsicsPolicy.maxLookaheadIntrinsicWidth(height)

        override fun maxIntrinsicHeight(width: Int) =
            layoutNode.intrinsicsPolicy.maxLookaheadIntrinsicHeight(width)
    }

    override fun ensureLookaheadDelegateCreated() {
        if (lookaheadDelegate == null) {
            lookaheadDelegate = LookaheadDelegateImpl()
        }
    }

    override fun measure(constraints: Constraints): Placeable = performingMeasure(constraints) {
        // before rerunning the user's measure block reset previous measuredByParent for children
        layoutNode.forEachChild {
            it.measurePassDelegate.measuredByParent = LayoutNode.UsageByParent.NotUsed
        }

        measureResult = with(layoutNode.measurePolicy) {
            measure(layoutNode.childMeasurables, constraints)
        }
        onMeasured()
        this
    }

    override fun minIntrinsicWidth(height: Int) =
        layoutNode.intrinsicsPolicy.minIntrinsicWidth(height)

    override fun minIntrinsicHeight(width: Int) =
        layoutNode.intrinsicsPolicy.minIntrinsicHeight(width)

    override fun maxIntrinsicWidth(height: Int) =
        layoutNode.intrinsicsPolicy.maxIntrinsicWidth(height)

    override fun maxIntrinsicHeight(width: Int) =
        layoutNode.intrinsicsPolicy.maxIntrinsicHeight(width)

    override fun placeAt(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        super.placeAt(position, zIndex, layerBlock)

        // The coordinator only runs their placement block to obtain our position, which allows them
        // to calculate the offset of an alignment line we have already provided a position for.
        // No need to place our wrapped as well (we might have actually done this already in
        // get(line), to obtain the position of the alignment line the coordinator currently needs
        // our position in order ot know how to offset the value we provided).
        if (isShallowPlacing) return

        onPlaced()

        layoutNode.measurePassDelegate.onNodePlaced()
    }

    override fun calculateAlignmentLine(alignmentLine: AlignmentLine): Int {
        return lookaheadDelegate?.calculateAlignmentLine(alignmentLine)
            ?: alignmentLinesOwner
                .calculateAlignmentLines()[alignmentLine]
            ?: AlignmentLine.Unspecified
    }

    override fun performDraw(canvas: Canvas) {
        val owner = layoutNode.requireOwner()
        layoutNode.zSortedChildren.forEach { child ->
            if (child.isPlaced) {
                child.draw(canvas)
            }
        }
        if (owner.showLayoutBounds) {
            drawBorder(canvas, innerBoundsPaint)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun hitTestChild(
        hitTestSource: HitTestSource,
        pointerPosition: Offset,
        hitTestResult: HitTestResult,
        isTouchEvent: Boolean,
        isInLayer: Boolean
    ) {
        var inLayer = isInLayer
        var hitTestChildren = false

        if (hitTestSource.shouldHitTestChildren(layoutNode)) {
            if (withinLayerBounds(pointerPosition)) {
                hitTestChildren = true
            } else if (isTouchEvent &&
                distanceInMinimumTouchTarget(pointerPosition, minimumTouchTargetSize).isFinite()
            ) {
                inLayer = false
                hitTestChildren = true
            }
        }

        if (hitTestChildren) {
            hitTestResult.siblingHits {
                // Any because as soon as true is returned, we know we have found a hit path and we must
                // not add hit results on different paths so we should not even go looking.
                layoutNode.zSortedChildren.reversedAny { child ->
                    if (child.isPlaced) {
                        hitTestSource.childHitTest(
                            child,
                            pointerPosition,
                            hitTestResult,
                            isTouchEvent,
                            inLayer
                        )
                        val wasHit = hitTestResult.hasHit()
                        val continueHitTest: Boolean
                        if (!wasHit) {
                            continueHitTest = true
                        } else if (
                            child.outerCoordinator.shouldSharePointerInputWithSiblings()
                        ) {
                            hitTestResult.acceptHits()
                            continueHitTest = true
                        } else {
                            continueHitTest = false
                        }
                        !continueHitTest
                    } else {
                        false
                    }
                }
            }
        }
    }

    internal companion object {
        val innerBoundsPaint = Paint().also { paint ->
            paint.color = Color.Red
            paint.strokeWidth = 1f
            paint.style = PaintingStyle.Stroke
        }
    }
}
