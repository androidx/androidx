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

@file:OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3XrApi::class)

package androidx.xr.compose.material3.integration.testapp.components

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.xr.compose.material3.ExperimentalMaterial3XrApi
import androidx.xr.compose.material3.SpatialListDetailPaneScaffold
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxSize

/**
 * A list-detail pane scaffold that uses [SpatialListDetailPaneScaffold] when spatial UI is enabled,
 * and [androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold] otherwise.
 *
 * When spatial UI is enabled, every pane is always shown, each in its own [SpatialPanel], and the
 * [navigator]'s directive and value are ignored. Otherwise, each pane is wrapped in an
 * [AnimatedPane] and shown according to the [navigator]'s state.
 */
@Composable
internal fun ListDetailPaneScaffold(
    navigator: ThreePaneScaffoldNavigator<*>,
    listPane: @Composable () -> Unit,
    detailPane: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    extraPane: (@Composable () -> Unit)? = null,
) {
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        SpatialListDetailPaneScaffold(
            listPane = { SpatialPanel(SubspaceModifier.fillMaxSize(), content = listPane) },
            detailPane = {
                SpatialPanel(SubspaceModifier.fillMaxSize(), content = detailPane)
            },
            extraPane =
                extraPane?.let { pane ->
                    { SpatialPanel(SubspaceModifier.fillMaxSize(), content = pane) }
                },
        )
    } else {
        androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            listPane = { AnimatedPane { listPane() } },
            detailPane = { AnimatedPane { detailPane() } },
            modifier = modifier,
            extraPane = extraPane?.let { pane -> { AnimatedPane { pane() } } },
        )
    }
}
