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

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.remote.core.operations.layout.managers.CollapsibleRowLayout
import androidx.compose.remote.core.operations.layout.managers.FlowLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.LocalGraphContext
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.RcPlayerChildren
import androidx.compose.remote.player.compose.embedded.executeOperations
import androidx.compose.remote.player.compose.embedded.getDrawContentOperationsListReflection
import androidx.compose.remote.player.compose.embedded.horizontalPositioningReflection
import androidx.compose.remote.player.compose.embedded.lineReflection
import androidx.compose.remote.player.compose.embedded.rawDimensionDp
import androidx.compose.remote.player.compose.embedded.rowSpacedBy
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.remote.player.compose.embedded.verticalPositioningReflection
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth

@Composable
internal fun RcPlayerRow(layout: RowLayout, modifier: Modifier) {
    val remoteContext = LocalRemoteContext.current
    val graph = LocalGraphContext.current
    val behavior = LocalCoreDocument.current.densityBehavior
    val density = LocalDensity.current.density
    // Resolve spacedBy (may be a NaN-encoded variable/expression) before scaling.
    val spacedBy = rememberRemoteFloatAsState(rowSpacedBy(layout)).value
    val drawOpsList = layout.getDrawContentOperationsListReflection()
    val drawModifier =
        if (drawOpsList != null) {
            Modifier.drawWithContent {
                executeOperations(
                    operations = drawOpsList,
                    remoteContext = remoteContext,
                    onDrawContent = { drawContent() },
                    graph = graph,
                )
            }
        } else Modifier

    val combinedModifier = modifier.then(drawModifier)

    if (layout is CollapsibleRowLayout) {
        Layout(content = { RcPlayerChildren(layout) { Modifier } }, modifier = combinedModifier) {
            measurables,
            constraints ->
            val placeables = ArrayList<Placeable>()
            var accumulatedWidth = 0
            var maxHeight = 0

            val spacedByDp = rawDimensionDp(spacedBy, behavior, density)
            val spacingPx = spacedByDp.roundToPx()

            measurables.forEachIndexed { index, measurable ->
                val childSpacing = if (index > 0 && spacedBy > 0f) spacingPx else 0
                // Measure with original constraints (minWidth = 0) to get natural preferred size
                val childPlaceable = measurable.measure(constraints.copy(minWidth = 0))

                if (
                    index == 0 ||
                        accumulatedWidth + childSpacing + childPlaceable.width <=
                            constraints.maxWidth
                ) {
                    placeables.add(childPlaceable)
                    accumulatedWidth += childSpacing + childPlaceable.width
                    maxHeight = maxOf(maxHeight, childPlaceable.height)
                }
            }

            val width = constraints.constrainWidth(accumulatedWidth)
            val height = constraints.constrainHeight(maxHeight)

            layout(width, height) {
                var x = 0
                placeables.forEachIndexed { index, placeable ->
                    val childSpacing = if (index > 0 && spacedBy > 0f) spacingPx else 0
                    x += childSpacing
                    placeable.placeRelative(x, 0)
                    x += placeable.width
                }
            }
        }
    } else {
        Row(
            modifier = combinedModifier,
            horizontalArrangement =
                rowHorizontalArrangement(
                    layout.horizontalPositioningReflection,
                    spacedBy,
                    behavior,
                    density,
                ),
            verticalAlignment = rowVerticalAlignment(layout.verticalPositioningReflection),
        ) {
            RcPlayerChildren(layout) { child ->
                val mods =
                    (child as? androidx.compose.remote.core.operations.layout.LayoutComponent)
                        ?.componentModifiers
                        ?.getList()
                var childModifier: Modifier = Modifier

                val weightOp =
                    mods?.find {
                        it is
                            androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation &&
                            it.getType() ==
                                androidx.compose.remote.core.operations.layout.modifiers
                                    .DimensionModifierOperation
                                    .Type
                                    .WEIGHT
                    }
                        as?
                        androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
                if (weightOp != null) {
                    childModifier = childModifier.weight(weightOp.getValue())
                }

                val alignByOp =
                    mods?.find {
                        it is
                            androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation
                    }
                        as?
                        androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation
                if (alignByOp != null) {
                    val line = alignByOp.lineReflection
                    if (androidx.compose.remote.core.operations.Utils.isVariable(line)) {
                        val id = androidx.compose.remote.core.operations.Utils.idFromNan(line)
                        if (
                            id ==
                                androidx.compose.remote.core.operations.layout.modifiers
                                    .AlignByModifierOperation
                                    .ID_FIRST_BASELINE ||
                                id ==
                                    androidx.compose.remote.core.operations.layout.modifiers
                                        .AlignByModifierOperation
                                        .ID_LAST_BASELINE
                        ) {
                            childModifier = childModifier.alignByBaseline()
                        }
                    }
                }
                childModifier
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun RcPlayerFlowRow(layout: FlowLayout, modifier: Modifier) {
    val remoteContext = LocalRemoteContext.current
    val graph = LocalGraphContext.current
    val behavior = LocalCoreDocument.current.densityBehavior
    val density = LocalDensity.current.density
    val spacedBy = rememberRemoteFloatAsState(rowSpacedBy(layout)).value
    val drawOpsList = layout.getDrawContentOperationsListReflection()
    val drawModifier =
        if (drawOpsList != null) {
            Modifier.drawWithContent {
                executeOperations(
                    operations = drawOpsList,
                    remoteContext = remoteContext,
                    onDrawContent = { drawContent() },
                    graph = graph,
                )
            }
        } else Modifier

    val combinedModifier = modifier.then(drawModifier)

    FlowRow(
        modifier = combinedModifier,
        horizontalArrangement =
            rowHorizontalArrangement(
                layout.horizontalPositioningReflection,
                spacedBy,
                behavior,
                density,
            ),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Top,
        itemVerticalAlignment = rowVerticalAlignment(layout.verticalPositioningReflection),
        maxItemsInEachRow = layout.mMaxItemsInEachRow,
        maxLines = layout.mMaxLines,
    ) {
        RcPlayerChildren(layout) { child ->
            val mods =
                (child as? androidx.compose.remote.core.operations.layout.LayoutComponent)
                    ?.componentModifiers
                    ?.getList()
            var childModifier: Modifier = Modifier

            val weightOp =
                mods?.find {
                    it is
                        androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation &&
                        it.getType() ==
                            androidx.compose.remote.core.operations.layout.modifiers
                                .DimensionModifierOperation
                                .Type
                                .WEIGHT
                }
                    as?
                    androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
            if (weightOp != null) {
                childModifier = childModifier.weight(weightOp.getValue())
            }

            val alignByOp =
                mods?.find {
                    it is
                        androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation
                }
                    as?
                    androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation
            if (alignByOp != null) {
                val line = alignByOp.lineReflection
                if (androidx.compose.remote.core.operations.Utils.isVariable(line)) {
                    val id = androidx.compose.remote.core.operations.Utils.idFromNan(line)
                    if (
                        id ==
                            androidx.compose.remote.core.operations.layout.modifiers
                                .AlignByModifierOperation
                                .ID_FIRST_BASELINE ||
                            id ==
                                androidx.compose.remote.core.operations.layout.modifiers
                                    .AlignByModifierOperation
                                    .ID_LAST_BASELINE
                    ) {
                        childModifier = childModifier.alignByBaseline()
                    }
                }
            }
            childModifier
        }
    }
}
