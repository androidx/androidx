/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core

import androidx.compose.ui.unit.Constraints
import androidx.ui.core.LayoutNode.LayoutState
import androidx.ui.core.LayoutNode.MeasureBlocks
import androidx.ui.core.LayoutNode.UsageByParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

@OptIn(ExperimentalLayoutNodeApi::class)
internal class OuterMeasurablePlaceable(
    private val layoutNode: LayoutNode,
    var outerWrapper: LayoutNodeWrapper
) : Measurable, Placeable() {

    private var measuredOnce = false
    val lastConstraints: Constraints? get() = if (measuredOnce) measurementConstraints else null
    var lastLayoutDirection: LayoutDirection? = null
        private set
    var lastPosition: IntOffset? = null
        private set
    private val lastProvidedAlignmentLines = mutableMapOf<AlignmentLine, Int>()

    /**
     * A local version of [Owner.measureIteration] to ensure that [MeasureBlocks.measure]
     * is not called multiple times within a measure pass.
     */
    var measureIteration = -1L
        private set

    override val parentData: Any? get() = outerWrapper.parentData

    /**
     * The function to be executed when the parent layout measures its children.
     */
    override fun measure(constraints: Constraints, layoutDirection: LayoutDirection): Placeable {
        // when we measure the root it is like the virtual parent is currently laying out
        val parentState = layoutNode.parent?.layoutState ?: LayoutState.LayingOut
        layoutNode.measuredByParent = when (parentState) {
            LayoutState.Measuring -> UsageByParent.InMeasureBlock
            LayoutState.LayingOut -> UsageByParent.InLayoutBlock
            else -> throw IllegalStateException(
                "Measurable could be only measured from the parent's measure or layout block." +
                        "Parents state is $parentState"
            )
        }
        remeasure(constraints, layoutDirection)
        return this
    }

    /**
     * Return true if the measured size has been changed
     */
    fun remeasure(constraints: Constraints, layoutDirection: LayoutDirection): Boolean {
        val owner = layoutNode.requireOwner()
        val iteration = owner.measureIteration
        val parent = layoutNode.parent
        @Suppress("Deprecation")
        layoutNode.canMultiMeasure = layoutNode.canMultiMeasure ||
                (parent != null && parent.canMultiMeasure)
        @Suppress("Deprecation")
        check(measureIteration != iteration || layoutNode.canMultiMeasure) {
            "measure() may not be called multiple times on the same Measurable"
        }
        measureIteration = owner.measureIteration
        if (layoutNode.layoutState == LayoutState.NeedsRemeasure ||
            measurementConstraints != constraints ||
            lastLayoutDirection != layoutDirection
        ) {
            measuredOnce = true
            layoutNode.layoutState = LayoutState.Measuring
            measurementConstraints = constraints
            lastLayoutDirection = layoutDirection
            lastProvidedAlignmentLines.clear()
            lastProvidedAlignmentLines.putAll(layoutNode.providedAlignmentLines)
            owner.observeMeasureModelReads(layoutNode) {
                outerWrapper.measure(constraints, layoutDirection)
            }
            layoutNode.layoutState = LayoutState.NeedsRelayout
            if (layoutNode.providedAlignmentLines != lastProvidedAlignmentLines) {
                layoutNode.onAlignmentsChanged()
            }
            val previousSize = measuredSize
            val newWidth = outerWrapper.width
            val newHeight = outerWrapper.height
            if (newWidth != previousSize.width ||
                newHeight != previousSize.height
            ) {
                measuredSize = IntSize(newWidth, newHeight)
                return true
            }
        }
        return false
    }

    override fun get(line: AlignmentLine): Int = outerWrapper[line]

    override fun place(position: IntOffset) {
        lastPosition = position
        with(InnerPlacementScope) {
            outerWrapper.placeAbsolute(position)
        }
    }

    /**
     * Calls [place] with the same position used during the last [place] call
     */
    fun replace() {
        place(checkNotNull(lastPosition))
    }

    override fun minIntrinsicWidth(height: Int, layoutDirection: LayoutDirection): Int =
        outerWrapper.minIntrinsicWidth(height, layoutDirection)

    override fun maxIntrinsicWidth(height: Int, layoutDirection: LayoutDirection): Int =
        outerWrapper.maxIntrinsicWidth(height, layoutDirection)

    override fun minIntrinsicHeight(width: Int, layoutDirection: LayoutDirection): Int =
        outerWrapper.minIntrinsicHeight(width, layoutDirection)

    override fun maxIntrinsicHeight(width: Int, layoutDirection: LayoutDirection): Int =
        outerWrapper.maxIntrinsicHeight(width, layoutDirection)
}
