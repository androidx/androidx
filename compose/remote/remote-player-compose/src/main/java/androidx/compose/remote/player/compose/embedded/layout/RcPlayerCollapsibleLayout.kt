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
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.RcPlayerChildren
import androidx.compose.remote.player.compose.embedded.rawDimensionDp
import androidx.compose.remote.player.compose.embedded.sortWithPriorities
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap

/**
 * Renders a collapsible row/column: lay children out along the main axis and, when they don't all
 * fit in the available main-axis space, "collapse" children until the rest fit.
 *
 * The collapse *decision* reuses remote-core: children are ordered by
 * `CollapsiblePriority.sortWithPriorities` (the same ordering the View player's
 * `CollapsibleRowLayout.computeVisibleChildren` uses — highest priority first, no-priority children
 * defaulting to `Float.MAX_VALUE`), and, exactly like core's walk, children are kept in that order
 * until the first one that overflows the budget; everything after it collapses. Survivors are
 * placed in document order. Only the measurement source differs: core measures with its own layout
 * pass, here Compose owns measure/layout, so the core walk is replayed against the Compose-measured
 * sizes (measurables are assumed to align 1:1 with the component's children in document order).
 */
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

    Layout(content = { RcPlayerChildren(layout) { Modifier } }, modifier = modifier) {
        measurables,
        constraints ->
        // spacedBy may be a NaN-encoded variable/expression (dp recorded against the density
        // variable), resolved by the caller; apply the density behavior here like the plain
        // Row/Column arrangements do.
        val spacingPx =
            if (spacedBy > 0f) rawDimensionDp(spacedBy, behavior, density).roundToPx() else 0

        // Measure each child at its natural preferred size on the main axis.
        val childConstraints =
            if (vertical) constraints.copy(minHeight = 0) else constraints.copy(minWidth = 0)
        val placeables = measurables.fastMap { it.measure(childConstraints) }
        val n = placeables.size

        fun mainSize(p: Placeable) = if (vertical) p.height else p.width
        fun crossSize(p: Placeable) = if (vertical) p.width else p.height

        val available =
            if (vertical) {
                if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE
            } else {
                if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
            }

        // Core's collapse walk (CollapsibleRowLayout.computeVisibleChildren), replayed against
        // the Compose-measured sizes: visit children highest-priority-first (core's
        // sortWithPriorities), keep each that fits, and once one overflows collapse it and
        // everything after it in priority order. Like core, the walk does not include spacing in
        // the budget.
        val children = layout.childrenComponents
        val indexOfChild = mutableObjectIntMapOf<Component>()
        children.fastForEachIndexed { index, child -> indexOfChild[child] = index }
        val kept = BooleanArray(n)
        var used = 0
        var overflow = false
        sortWithPriorities(children, orientation).fastForEach { child ->
            val index = indexOfChild.getOrDefault(child, -1)
            if (index == -1 || index >= n) return@fastForEach
            val childSize = mainSize(placeables[index])
            if (overflow || used + childSize > available) {
                overflow = true
                return@fastForEach
            }
            used += childSize
            kept[index] = true
        }

        var mainExtent = 0
        var crossExtent = 0
        var visible = 0
        for (i in 0 until n) {
            if (!kept[i]) continue
            mainExtent += mainSize(placeables[i])
            crossExtent = maxOf(crossExtent, crossSize(placeables[i]))
            visible++
        }
        if (visible > 1) mainExtent += spacingPx * (visible - 1)
        mainExtent = mainExtent.coerceAtMost(available)

        val width = if (vertical) crossExtent else mainExtent
        val height = if (vertical) mainExtent else crossExtent

        layout(constraints.constrainWidth(width), constraints.constrainHeight(height)) {
            var pos = 0
            for (i in 0 until n) {
                if (!kept[i]) continue
                if (vertical) placeables[i].placeRelative(0, pos)
                else placeables[i].placeRelative(pos, 0)
                pos += mainSize(placeables[i]) + spacingPx
            }
        }
    }
}
