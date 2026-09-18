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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.layout

import androidx.collection.mutableObjectIntMapOf
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.managers.CollapsiblePriority
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.modifiers.CollapsiblePriorityModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.RcPlayerChildren
import androidx.compose.remote.player.compose.embedded.horizontalPositioningReflection
import androidx.compose.remote.player.compose.embedded.rawDimensionDp
import androidx.compose.remote.player.compose.embedded.sortWithPriorities
import androidx.compose.remote.player.compose.embedded.verticalPositioningReflection
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed

@Composable
internal fun RcPlayerCollapsible(
    layout: LayoutComponent,
    modifier: Modifier,
    vertical: Boolean,
    spacedBy: Float,
) {
    val orientation = if (vertical) CollapsiblePriority.VERTICAL else CollapsiblePriority.HORIZONTAL
    val behavior = LocalCoreDocument.current.densityBehavior
    val density = LocalDensity.current.density
    val horizontalPositioning =
        when (layout) {
            is RowLayout -> layout.horizontalPositioningReflection
            is ColumnLayout -> layout.horizontalPositioningReflection
            else -> RowLayout.START
        }
    val verticalPositioning =
        when (layout) {
            is RowLayout -> layout.verticalPositioningReflection
            is ColumnLayout -> layout.verticalPositioningReflection
            else -> ColumnLayout.TOP
        }
    val horizontalArrangement =
        rowHorizontalArrangement(
            horizontalPositioning,
            spacedBy,
            behavior,
            density,
        )
    val verticalArrangement =
        columnVerticalArrangement(
            verticalPositioning,
            spacedBy,
            behavior,
            density,
        )
    val verticalAlignment = rowVerticalAlignment(verticalPositioning)
    val horizontalAlignment = columnHorizontalAlignment(horizontalPositioning)

    Layout(content = { RcPlayerChildren(layout) { Modifier } }, modifier = modifier) {
        measurables,
        constraints ->
        val spacingPx =
            if (spacedBy > 0f) rawDimensionDp(spacedBy, behavior, density).roundToPx() else 0

        // Relax both minWidth and minHeight so children measure at their preferred size on both
        // axes rather than inheriting the container's minimum cross-axis constraint.
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val n = measurables.size
        val children = layout.childrenComponents
        val placeables = arrayOfNulls<Placeable>(n)

        fun childWeight(child: Component): Float {
            val mods = (child as? LayoutComponent)?.componentModifiers?.getList() ?: return 0f
            return if (vertical) {
                (mods.fastFirstOrNull {
                        it is HeightModifierOperation &&
                            it.getType() == DimensionModifierOperation.Type.WEIGHT
                    } as? HeightModifierOperation)
                    ?.getValue() ?: 0f
            } else {
                (mods.fastFirstOrNull {
                        it is WidthModifierOperation &&
                            it.getType() == DimensionModifierOperation.Type.WEIGHT
                    } as? WidthModifierOperation)
                    ?.getValue() ?: 0f
            }
        }

        val weights = FloatArray(n) { i -> if (i < children.size) childWeight(children[i]) else 0f }
        for (i in 0 until n) {
            if (weights[i] <= 0f) {
                placeables[i] = measurables[i].measure(childConstraints)
            }
        }

        fun mainSize(p: Placeable?) = if (p == null) 0 else if (vertical) p.height else p.width
        fun crossSize(p: Placeable?) = if (p == null) 0 else if (vertical) p.width else p.height

        val available =
            if (vertical) {
                if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE
            } else {
                if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
            }

        val indexOfChild = mutableObjectIntMapOf<Component>()
        var hasPriorities = false
        children.fastForEachIndexed { index, child ->
            indexOfChild[child] = index
            if (
                (child as? LayoutComponent)?.selfOrModifier(
                    CollapsiblePriorityModifierOperation::class.java
                ) != null
            ) {
                hasPriorities = true
            }
        }

        // Only sort by CollapsiblePriority when at least one child defines a priority modifier;
        // otherwise collapse in document order, matching CollapsibleRowLayout.java.
        val visitOrder = if (hasPriorities) sortWithPriorities(children, orientation) else children
        val kept = BooleanArray(n)
        var usedUnweighted = 0
        var keptCount = 0
        var overflow = false
        visitOrder.fastForEach { child ->
            val index = indexOfChild.getOrDefault(child, -1)
            if (index == -1 || index >= n) return@fastForEach
            // Skip children whose explicit visibility modifier marked them GONE.
            val explicitVisOp =
                (child as? LayoutComponent)?.selfOrModifier(
                    ComponentVisibilityOperation::class.java
                )
            if (explicitVisOp != null && child.mVisibility == Component.Visibility.GONE) {
                return@fastForEach
            }
            val childSize = mainSize(placeables[index])
            val neededSpacing = if (keptCount > 0) spacingPx else 0
            if (overflow || usedUnweighted + neededSpacing + childSize > available) {
                overflow = true
                return@fastForEach
            }
            usedUnweighted += neededSpacing + childSize
            keptCount++
            kept[index] = true
        }

        var visibleCount = 0
        var totalWeight = 0f
        for (i in 0 until n) {
            if (kept[i]) {
                visibleCount++
                totalWeight += weights[i]
            }
        }

        val totalSpacing = if (visibleCount > 1) spacingPx * (visibleCount - 1) else 0
        if (totalWeight > 0f) {
            val remaining = (available - usedUnweighted).coerceAtLeast(0)
            for (i in 0 until n) {
                if (kept[i] && weights[i] > 0f) {
                    val share = ((remaining * (weights[i] / totalWeight)).toInt()).coerceAtLeast(0)
                    val weightedConstraints =
                        if (vertical) {
                            childConstraints.copy(minHeight = share, maxHeight = share)
                        } else {
                            childConstraints.copy(minWidth = share, maxWidth = share)
                        }
                    placeables[i] = measurables[i].measure(weightedConstraints)
                }
            }
        }

        // Synchronize child and container visibility state with remote-core so tree/state
        // inspection and parent containers observe collapsed vs visible components.
        for (i in 0 until minOf(n, children.size)) {
            val vis = if (kept[i]) Component.Visibility.VISIBLE else Component.Visibility.GONE
            children[i].mVisibility = vis
        }
        val selfVis =
            if (visibleCount == 0) Component.Visibility.GONE else Component.Visibility.VISIBLE
        layout.mVisibility = selfVis

        var mainExtent = 0
        var crossExtent = 0
        val keptIndices = IntArray(visibleCount)
        val mainSizes = IntArray(visibleCount)
        var k = 0
        for (i in 0 until n) {
            if (!kept[i]) continue
            val p = placeables[i]
            val m = mainSize(p)
            mainExtent += m
            crossExtent = maxOf(crossExtent, crossSize(p))
            keptIndices[k] = i
            mainSizes[k] = m
            k++
        }
        mainExtent = (mainExtent + totalSpacing).coerceAtMost(available)

        val width = constraints.constrainWidth(if (vertical) crossExtent else mainExtent)
        val height = constraints.constrainHeight(if (vertical) mainExtent else crossExtent)
        val mainPositions = IntArray(visibleCount)
        if (vertical) {
            with(verticalArrangement) { arrange(height, mainSizes, mainPositions) }
        } else {
            with(horizontalArrangement) {
                arrange(width, mainSizes, layoutDirection, mainPositions)
            }
        }

        layout(width, height) {
            var kIdx = 0
            var nextMainPos = if (visibleCount > 0) mainPositions[0] else 0
            for (i in 0 until n) {
                val p = placeables[i] ?: continue
                if (kept[i]) {
                    val mainPos = mainPositions[kIdx]
                    if (vertical) {
                        val x = horizontalAlignment.align(p.width, width, layoutDirection)
                        p.place(x, mainPos)
                        if (i < children.size) {
                            children[i].x = x.toFloat()
                            children[i].y = mainPos.toFloat()
                        }
                    } else {
                        val y = verticalAlignment.align(p.height, height)
                        p.place(mainPos, y)
                        if (i < children.size) {
                            children[i].x = mainPos.toFloat()
                            children[i].y = y.toFloat()
                        }
                    }
                    kIdx++
                    nextMainPos =
                        if (kIdx < visibleCount) {
                            mainPositions[kIdx]
                        } else {
                            mainPos + mainSize(p) + spacingPx
                        }
                } else if (i < children.size) {
                    // Collapsed children are not placed in Compose UI, but record the main-axis
                    // cursor and cross-axis aligned position on Component to match
                    // ColumnLayout.java.
                    if (vertical) {
                        val x = horizontalAlignment.align(p.width, width, layoutDirection)
                        children[i].x = x.toFloat()
                        children[i].y = nextMainPos.toFloat()
                    } else {
                        val y = verticalAlignment.align(p.height, height)
                        children[i].x = nextMainPos.toFloat()
                        children[i].y = y.toFloat()
                    }
                }
            }
        }
    }
}
