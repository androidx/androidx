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

package androidx.xr.compose.material3

import androidx.compose.runtime.Composable
import androidx.xr.compose.subspace.SpatialBoxScope
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier

/**
 * An XR-differentiated version of a three pane layout that follows the Material guidelines,
 * displaying the provided panes in a canonical
 * [list-detail layout](https://m3.material.io/foundations/layout/canonical-layouts/list-detail).
 *
 * @param listPane the list pane of the scaffold, which is supposed to hold a list of item summaries
 *   that can be selected from, for example, the inbox mail list of a mail app. Note that we suggest
 *   you to use `SpatialPanel` as the root layout of panes, which supports default pane behaviors
 *   like enter/exit transitions.
 * @param detailPane the detail pane of the scaffold, which is supposed to hold the detailed info of
 *   a selected item, for example, the mail content currently being viewed. Note that we suggest you
 *   to use `SpatialPanel` as the root layout of panes, which supports default pane behaviors like
 *   enter/exit transitions.
 * @param modifier [SubspaceModifier] of the surrounding spatial layout
 * @param horizontalArrangement The horizontal arrangement of the children.
 * @param extraPane the extra pane of the scaffold, which is supposed to hold any supplementary info
 *   besides the list and the detail panes, for example, a task list or a mini-calendar view of a
 *   mail app. Note that we suggest you to use `SpatialPanel` as the root layout of panes, which
 *   supports default pane behaviors like enter/exit transitions.
 */
@ExperimentalMaterial3XrApi
@Composable
public fun ListDetailPaneScaffold(
    listPane: @Composable @SubspaceComposable SpatialBoxScope.() -> Unit,
    detailPane: @Composable @SubspaceComposable SpatialBoxScope.() -> Unit,
    modifier: SubspaceModifier = SubspaceModifier,
    horizontalArrangement: SpatialArrangement.Horizontal =
        XrThreePaneScaffoldTokens.DefaultArrangement,
    extraPane: (@Composable @SubspaceComposable SpatialBoxScope.() -> Unit)? = null,
) {
    ThreePaneScaffold(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        firstPane = { Panel(XrThreePaneScaffoldTokens.SECONDARY_PANE_WEIGHT, listPane) },
        secondPane = { Panel(XrThreePaneScaffoldTokens.PRIMARY_PANE_WEIGHT, detailPane) },
        thirdPane =
            if (extraPane == null) {
                null
            } else {
                { Panel(XrThreePaneScaffoldTokens.TERTIARY_PANE_WEIGHT, extraPane) }
            },
    )
}
