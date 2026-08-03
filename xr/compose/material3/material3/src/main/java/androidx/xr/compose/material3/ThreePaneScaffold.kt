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

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.PlanarEmbeddedSubspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialBoxScope
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SpatialRowScope
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.offset

/**
 * A canonical three-pane horizontal scaffold. It is recommended to use [SpatialRowScope.Panel] with
 * the weights provided in [XrThreePaneScaffoldTokens] to create a scaffold proportioned according
 * to the Material Design spec.
 */
@ExperimentalMaterial3XrApi
@Composable
internal fun ThreePaneScaffold(
    modifier: SubspaceModifier,
    horizontalArrangement: SpatialArrangement.Horizontal,
    firstPane: @Composable @SubspaceComposable SpatialRowScope.() -> Unit,
    secondPane: @Composable @SubspaceComposable SpatialRowScope.() -> Unit,
    thirdPane: (@Composable @SubspaceComposable SpatialRowScope.() -> Unit)?,
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
            horizontalArrangement = horizontalArrangement,
        ) {
            firstPane()
            secondPane()
            thirdPane?.let { it() }
        }
    }
}

@Composable
@SubspaceComposable
internal fun SpatialRowScope.Panel(
    weight: Float,
    content: @Composable @SubspaceComposable SpatialBoxScope.() -> Unit,
) {
    SpatialBox(modifier = SubspaceModifier.weight(weight), content = content)
}

internal object XrThreePaneScaffoldTokens {
    const val PRIMARY_PANE_WEIGHT = 1f
    const val SECONDARY_PANE_WEIGHT = 0.5f
    const val TERTIARY_PANE_WEIGHT = 0.5f

    val DefaultArrangement = SpatialArrangement.spacedBy(24.dp)
}
