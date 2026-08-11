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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.managers.CollapsibleRowLayout
import androidx.compose.remote.core.operations.layout.managers.FlowLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.LocalGraphContext
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.RcPlayerChildren
import androidx.compose.remote.player.compose.embedded.horizontalPositioningReflection
import androidx.compose.remote.player.compose.embedded.lineReflection
import androidx.compose.remote.player.compose.embedded.rowSpacedBy
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.remote.player.compose.embedded.verticalPositioningReflection
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.fastFirstOrNull

@Composable
internal fun RcPlayerRow(layout: RowLayout, modifier: Modifier) {
    val remoteContext = LocalRemoteContext.current
    val graph = LocalGraphContext.current
    val behavior = LocalCoreDocument.current.densityBehavior
    val density = LocalDensity.current.density
    // Resolve spacedBy (may be a NaN-encoded variable/expression) before scaling.
    val spacedBy = rememberRemoteFloatAsState(rowSpacedBy(layout)).value
    if (layout is CollapsibleRowLayout) {
        // Priority-aware collapsing: drop lowest-CollapsiblePriority children until the rest fit.
        RcPlayerCollapsible(layout, modifier, vertical = false, spacedBy = spacedBy)
    } else {
        Row(
            modifier = modifier,
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
                val mods = (child as? LayoutComponent)?.componentModifiers?.getList()
                var childModifier: Modifier = Modifier

                val weightOp =
                    mods?.fastFirstOrNull {
                        it is WidthModifierOperation &&
                            it.getType() == DimensionModifierOperation.Type.WEIGHT
                    } as? WidthModifierOperation
                if (weightOp != null) {
                    childModifier = childModifier.weight(weightOp.getValue())
                }

                val alignByOp =
                    mods?.fastFirstOrNull { it is AlignByModifierOperation }
                        as? AlignByModifierOperation
                if (alignByOp != null) {
                    val line = alignByOp.lineReflection
                    if (Utils.isVariable(line)) {
                        val id = Utils.idFromNan(line)
                        if (
                            id == AlignByModifierOperation.ID_FIRST_BASELINE ||
                                id == AlignByModifierOperation.ID_LAST_BASELINE
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RcPlayerFlowRow(layout: FlowLayout, modifier: Modifier) {
    val remoteContext = LocalRemoteContext.current
    val graph = LocalGraphContext.current
    val behavior = LocalCoreDocument.current.densityBehavior
    val density = LocalDensity.current.density
    val spacedBy = rememberRemoteFloatAsState(rowSpacedBy(layout)).value
    FlowRow(
        modifier = modifier,
        horizontalArrangement =
            rowHorizontalArrangement(
                layout.horizontalPositioningReflection,
                spacedBy,
                behavior,
                density,
            ),
        verticalArrangement = Arrangement.Top,
        itemVerticalAlignment = rowVerticalAlignment(layout.verticalPositioningReflection),
        maxItemsInEachRow = layout.mMaxItemsInEachRow,
        maxLines = layout.mMaxLines,
    ) {
        RcPlayerChildren(layout) { child ->
            val mods = (child as? LayoutComponent)?.componentModifiers?.getList()
            var childModifier: Modifier = Modifier

            val weightOp =
                mods?.fastFirstOrNull {
                    it is WidthModifierOperation &&
                        it.getType() == DimensionModifierOperation.Type.WEIGHT
                } as? WidthModifierOperation
            if (weightOp != null) {
                childModifier = childModifier.weight(weightOp.getValue())
            }

            val alignByOp =
                mods?.fastFirstOrNull { it is AlignByModifierOperation }
                    as? AlignByModifierOperation
            if (alignByOp != null) {
                val line = alignByOp.lineReflection
                if (Utils.isVariable(line)) {
                    val id = Utils.idFromNan(line)
                    if (
                        id == AlignByModifierOperation.ID_FIRST_BASELINE ||
                            id == AlignByModifierOperation.ID_LAST_BASELINE
                    ) {
                        childModifier = childModifier.alignByBaseline()
                    }
                }
            }
            childModifier
        }
    }
}
