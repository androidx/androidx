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
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldHorizontalOrder
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldOverride
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldOverrideScope
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.xr.compose.material3.XrThreePaneScaffoldOverride.ThreePaneScaffold
import androidx.xr.compose.spatial.PlanarEmbeddedSubspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SpatialRowScope
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.offset

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
    PlanarEmbeddedSubspace {
        SpatialRow(
            modifier =
                modifier
                    // Offset by 1dp as a workaround to fix b/395685251, where elements in the
                    // XR-overrides ThreePaneScaffold are not clickable when composed from within
                    // the XR-overrides NavigationSuiteScaffold.
                    .offset(z = 1.dp)
                    .fillMaxSize(),
            horizontalArrangement =
                SpatialArrangement.spacedBy(scaffoldDirective.horizontalPartitionSpacerSize),
        ) {
            paneOrder.each { role ->
                when (role) {
                    ThreePaneScaffoldRole.Primary -> {
                        Panel(XrThreePaneScaffoldTokens.PRIMARY_PANE_WEIGHT, primaryPane)
                    }
                    ThreePaneScaffoldRole.Secondary -> {
                        Panel(XrThreePaneScaffoldTokens.SECONDARY_PANE_WEIGHT, secondaryPane)
                    }
                    ThreePaneScaffoldRole.Tertiary -> {
                        if (tertiaryPane != null) {
                            Panel(XrThreePaneScaffoldTokens.TERTIARY_PANE_WEIGHT, tertiaryPane)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
@SubspaceComposable
private fun SpatialRowScope.Panel(
    weight: Float,
    content: @Composable @SubspaceComposable () -> Unit,
) {
    SpatialBox(SubspaceModifier.weight(weight)) { content() }
}

/**
 * [ThreePaneScaffoldOverride] that uses the XR-specific [ThreePaneScaffold].
 *
 * Note that when using this override, any modifiers passed in to the 2D composable are ignored.
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
internal object XrThreePaneScaffoldTokens {
    const val PRIMARY_PANE_WEIGHT = 1f
    const val SECONDARY_PANE_WEIGHT = 0.5f
    const val TERTIARY_PANE_WEIGHT = 0.5f
}

@ExperimentalMaterial3AdaptiveApi
private inline fun ThreePaneScaffoldHorizontalOrder.each(action: (ThreePaneScaffoldRole) -> Unit) {
    action(get(0))
    action(get(1))
    action(get(2))
}
