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

import androidx.compose.foundation.layout.Column
import androidx.compose.remote.core.operations.layout.managers.CollapsibleColumnLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.RcPlayerChildren
import androidx.compose.remote.player.compose.embedded.columnSpacedBy
import androidx.compose.remote.player.compose.embedded.horizontalPositioningReflection
import androidx.compose.remote.player.compose.embedded.rawDimensionDp
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.remote.player.compose.embedded.verticalPositioningReflection
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth

@Composable
internal fun RcPlayerColumn(layout: ColumnLayout, modifier: Modifier) {
    val behavior = LocalCoreDocument.current.densityBehavior
    val density = LocalDensity.current.density
    // spacedBy may be a NaN-encoded variable/expression (dp recorded against the density variable),
    // so resolve it before the arrangement helper applies the density behavior.
    val spacedBy = rememberRemoteFloatAsState(columnSpacedBy(layout)).value

    if (layout is CollapsibleColumnLayout) {
        Layout(content = { RcPlayerChildren(layout) { Modifier } }, modifier = modifier) {
            measurables,
            constraints ->
            val placeables = ArrayList<Placeable>()
            var accumulatedHeight = 0
            var maxWidth = 0

            val spacedByDp = rawDimensionDp(spacedBy, behavior, density)
            val spacingPx = spacedByDp.roundToPx()

            measurables.forEachIndexed { index, measurable ->
                val childSpacing = if (index > 0 && spacedBy > 0f) spacingPx else 0
                // Measure with original constraints (minHeight = 0) to get natural preferred size
                val childPlaceable = measurable.measure(constraints.copy(minHeight = 0))

                if (
                    index == 0 ||
                        accumulatedHeight + childSpacing + childPlaceable.height <=
                            constraints.maxHeight
                ) {
                    placeables.add(childPlaceable)
                    accumulatedHeight += childSpacing + childPlaceable.height
                    maxWidth = maxOf(maxWidth, childPlaceable.width)
                }
            }

            val width = constraints.constrainWidth(maxWidth)
            val height = constraints.constrainHeight(accumulatedHeight)

            layout(width, height) {
                var y = 0
                placeables.forEachIndexed { index, placeable ->
                    val childSpacing = if (index > 0 && spacedBy > 0f) spacingPx else 0
                    y += childSpacing
                    placeable.placeRelative(0, y)
                    y += placeable.height
                }
            }
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement =
                columnVerticalArrangement(
                    layout.verticalPositioningReflection,
                    spacedBy,
                    behavior,
                    density,
                ),
            horizontalAlignment = columnHorizontalAlignment(layout.horizontalPositioningReflection),
        ) {
            RcPlayerChildren(layout) { child ->
                val layoutChild =
                    child as? androidx.compose.remote.core.operations.layout.LayoutComponent
                val heightModifier = layoutChild?.heightModifier
                if (heightModifier != null && heightModifier.hasWeight()) {
                    Modifier.weight(heightModifier.value)
                } else {
                    Modifier
                }
            }
        }
    }
}
