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

package androidx.xr.compose.material3

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveComponentOverrideApi
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.PaneScaffoldParentData
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldHorizontalOrder
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldOverride
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldOverrideScope
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialLayoutSpacer
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxHeight
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.width
import kotlin.math.roundToInt

/**
 * A pane scaffold composable that can display up to three panes in the order that
 * [ThreePaneScaffoldHorizontalOrder] specifies, and allocate margins and spacers according to
 * [PaneScaffoldDirective].
 *
 * [ThreePaneScaffold] is the base composable functions of adaptive programming. Developers can
 * freely pipeline the relevant adaptive signals and use them as input of the scaffold function to
 * render the final adaptive layout.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param scaffoldDirective The top-level directives about how the scaffold should arrange its
 *   panes.
 * @param paneOrder The horizontal order of the panes from start to end in the scaffold.
 * @param secondaryPane The content of the secondary pane that has a priority lower then the primary
 *   pane but higher than the tertiary pane.
 * @param tertiaryPane The content of the tertiary pane that has the lowest priority.
 * @param primaryPane The content of the primary pane that has the highest priority.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@ExperimentalMaterial3XrApi
@Composable
public fun ThreePaneScaffold(
    modifier: SubspaceModifier,
    scaffoldDirective: PaneScaffoldDirective,
    paneOrder: ThreePaneScaffoldHorizontalOrder,
    secondaryPane: @Composable () -> Unit,
    tertiaryPane: (@Composable () -> Unit)? = null,
    primaryPane: @Composable () -> Unit,
) {
    Subspace {
        SpatialRow(
            // Offset by 1dp as a workaround to fix b/395685251, where elements in the XR-overrides
            // ThreePaneScaffold are not clickable when composed from within the XR-overrides
            // NavigationSuiteScaffold.
            modifier = modifier.offset(z = 1.dp).height(XrThreePaneScaffoldTokens.PanelHeight)
        ) {
            var drawSpacer = false // Only draws spacers after the first pane is drawn
            paneOrder.each { role ->
                when (role) {
                    ThreePaneScaffoldRole.Primary -> {
                        Panel(
                            scaffoldDirective,
                            XrThreePaneScaffoldTokens.PrimaryPanePanelWidth,
                            drawSpacer,
                            primaryPane,
                        )
                        drawSpacer = true
                    }
                    ThreePaneScaffoldRole.Secondary -> {
                        Panel(
                            scaffoldDirective,
                            XrThreePaneScaffoldTokens.SecondaryPanePanelWidth,
                            drawSpacer,
                            secondaryPane,
                        )
                        drawSpacer = true
                    }
                    ThreePaneScaffoldRole.Tertiary ->
                        if (tertiaryPane != null) {
                            Panel(
                                scaffoldDirective,
                                XrThreePaneScaffoldTokens.TertiaryPanePanelWidth,
                                drawSpacer,
                                tertiaryPane,
                            )
                            drawSpacer = true
                        }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun Panel(
    scaffoldDirective: PaneScaffoldDirective,
    defaultPreferredWidth: Dp,
    drawSpacer: Boolean,
    content: @Composable () -> Unit,
) {
    if (drawSpacer) {
        SpatialLayoutSpacer(SubspaceModifier.width(scaffoldDirective.horizontalPartitionSpacerSize))
    }

    SpatialPanel(SubspaceModifier.width(defaultPreferredWidth).fillMaxHeight()) {
        Layout(content) { measurables, constraints ->
            val measurable = measurables.getOrNull(0)
            if (measurable == null) {
                return@Layout layout(
                    defaultPreferredWidth.toPx().roundToInt(),
                    constraints.maxHeight,
                ) {}
            }
            val parentData = measurable.parentData as? PaneScaffoldParentData
            val widthFloat = parentData?.preferredWidth ?: defaultPreferredWidth
            val width = widthFloat.toPx().roundToInt()
            val height = constraints.maxHeight
            return@Layout layout(width, height) {
                measurable.measure(Constraints.fixed(width, height)).place(0, 0)
            }
        }
    }
}

/**
 * [ThreePaneScaffoldOverride] that uses the XR-specific [ThreePaneScaffold].
 *
 * Note that when using this override, any madifiers passed in to the 2D composable are ignored.
 */
@ExperimentalMaterial3XrApi
@OptIn(
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3AdaptiveComponentOverrideApi::class,
)
internal object XrThreePaneScaffoldOverride : ThreePaneScaffoldOverride {
    @Composable
    override fun ThreePaneScaffoldOverrideScope.ThreePaneScaffold() {
        ThreePaneScaffold(
            modifier = SubspaceModifier,
            scaffoldDirective = scaffoldDirective.copy(maxHorizontalPartitions = 3),
            paneOrder = paneOrder,
            primaryPane = primaryPane,
            secondaryPane = secondaryPane,
            tertiaryPane = tertiaryPane,
        )
    }
}

// TODO(conradchen): Confirm the values with design
private object XrThreePaneScaffoldTokens {
    val PanelHeight = 1024.dp
    val PrimaryPanePanelWidth = 800.dp
    val SecondaryPanePanelWidth = 412.dp
    val TertiaryPanePanelWidth = 412.dp
}

@ExperimentalMaterial3AdaptiveApi
private inline fun ThreePaneScaffoldHorizontalOrder.each(action: (ThreePaneScaffoldRole) -> Unit) {
    action(get(0))
    action(get(1))
    action(get(2))
}
