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

package androidx.glance.adaptive.appwidget.components.graph

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteColumnScope
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.glance.adaptive.appwidget.components.RemoteComposeFonts
import androidx.glance.adaptive.appwidget.components.RemoteComposePalette
import androidx.glance.adaptive.appwidget.components.RemoteVectorGlyph
import androidx.glance.adaptive.appwidget.components.SlotText
import androidx.glance.adaptive.core.slots.ChartLabel
import androidx.glance.adaptive.core.slots.Graph

/** Vertical gap between a bar or dot and its axis label. */
private const val BAR_LABEL_TOP_SPACE_DP = 4f

/** Edge length of each dot in a dotted graph. */
private const val DOT_SIZE_DP = 12f

/** Vertical gap between the bars row and the optional description row. */
private const val CHART_DESCRIPTION_TOP_SPACE_DP = 8f

/** Horizontal gap between an optional caption icon and its caption text. */
private const val CHART_CAPTION_ICON_GAP_DP = 4f

/**
 * Draws the graph for the hero slot — the RemoteCompose counterpart of [Graph.buildRemoteViews].
 *
 * Every column stretches across the full height of the bars row. Explicit [barSpacing] spacer boxes
 * separate the weighted columns so every bar has the exact same width while the outer bars stay
 * flush with the container's edges. A bar fills its fraction of the vertical space above its label
 * from the bottom up, while a dotted graph draws a circular dot at the bottom of its column
 * instead.
 *
 * @param barSpacing Horizontal gap between adjacent columns.
 */
@RemoteComposable
@Composable
internal fun Graph.Content(barSpacing: Dp) {
    RemoteColumn(
        modifier = RemoteModifier.fillMaxSize(),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
    ) {
        RemoteRow(
            modifier = RemoteModifier.weight(1f.rf).fillMaxWidth(),
            verticalAlignment = RemoteAlignment.Bottom,
        ) {
            for (index in values.indices) {
                if (index > 0) {
                    RemoteBox(modifier = RemoteModifier.width(barSpacing.asRdp()))
                }
                RemoteColumn(
                    modifier = RemoteModifier.weight(1f.rf).fillMaxHeight(),
                    verticalArrangement = RemoteArrangement.Bottom,
                    horizontalAlignment = RemoteAlignment.CenterHorizontally,
                ) {
                    GraphColumnIndicator(graph = this@Content, index = index)
                    val label = labelAt(index)
                    if (label != null) {
                        RemoteBox(modifier = RemoteModifier.height(BAR_LABEL_TOP_SPACE_DP.rdp))
                        SlotText(
                            text = label,
                            fontSize = GraphDefaults.LabelTextSize,
                            fontWeight = RemoteComposeFonts.Medium,
                            color = RemoteComposePalette.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (hasDescription) {
            RemoteBox(modifier = RemoteModifier.height(CHART_DESCRIPTION_TOP_SPACE_DP.rdp))
            RemoteRow(
                modifier = RemoteModifier.fillMaxWidth(),
                verticalAlignment = RemoteAlignment.CenterVertically,
            ) {
                startLabel?.let { ChartCaption(label = it) }
                RemoteBox(
                    modifier = RemoteModifier.weight(1f.rf),
                    contentAlignment = RemoteAlignment.CenterEnd,
                ) {
                    endLabel?.let { ChartCaption(label = it) }
                }
            }
        }
    }
}

/** Draws one description caption ([ChartLabel]) with its optional leading icon. */
@RemoteComposable
@Composable
private fun ChartCaption(label: ChartLabel) {
    RemoteRow(verticalAlignment = RemoteAlignment.CenterVertically) {
        label.icon?.let { iconRes ->
            RemoteVectorGlyph(
                icon = iconRes,
                size = GraphDefaults.CaptionIconSize,
                tint = RemoteComposePalette.primary,
            )
            RemoteBox(modifier = RemoteModifier.width(CHART_CAPTION_ICON_GAP_DP.rdp))
        }
        SlotText(
            text = label.text,
            fontSize = GraphDefaults.CaptionTextSize,
            fontWeight = RemoteComposeFonts.Medium,
            color = RemoteComposePalette.primary,
        )
    }
}

/** Draws the bar or dot for column [index] above any axis label. */
@RemoteComposable
@Composable
private fun RemoteColumnScope.GraphColumnIndicator(graph: Graph, index: Int) {
    if (graph.dotted) {
        val dotColor =
            if (graph.isDotFilledAt(index)) {
                RemoteComposePalette.tertiary
            } else {
                RemoteComposePalette.barMuted
            }
        RemoteBox(
            modifier =
                RemoteModifier.size(DOT_SIZE_DP.rdp).clip(RemoteCircleShape).background(dotColor)
        )
    } else {
        val barColor =
            if (graph.isEmphasizedAt(index)) {
                RemoteComposePalette.tertiary
            } else {
                RemoteComposePalette.barMuted
            }
        val fillFraction = graph.fractionAt(index).coerceIn(GraphDefaults.MinBarFraction, 1f)
        RemoteBox(
            modifier = RemoteModifier.weight(1f.rf).fillMaxWidth(),
            contentAlignment = RemoteAlignment.BottomCenter,
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxWidth()
                        .fillMaxHeight(fillFraction.rf)
                        .clip(RemoteCircleShape)
                        .background(barColor)
            )
        }
    }
}
