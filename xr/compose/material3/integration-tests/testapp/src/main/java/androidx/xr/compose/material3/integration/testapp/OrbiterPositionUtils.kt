/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.material3.integration.testapp

import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.OrbiterAlignment
import androidx.xr.compose.spatial.OrbiterDefaults
import androidx.xr.compose.spatial.OrbiterEdgeOffsetType
import androidx.xr.compose.unit.DpVolumeOffset

private val NavigationSuiteOrbiterDefaultSpacing = 24.dp

internal fun OrbiterPosition.toHorizontalAlignment(): OrbiterAlignment =
    when (this) {
        OrbiterPosition.Outside ->
            OrbiterAlignment.BottomCenter(
                edgeOffsetType = OrbiterEdgeOffsetType.OuterEdge,
                offset =
                    DpVolumeOffset(
                        y = -NavigationSuiteOrbiterDefaultSpacing,
                        z = OrbiterDefaults.Elevation,
                    ),
            )
        OrbiterPosition.Overlapping ->
            OrbiterAlignment.BottomCenter(
                edgeOffsetType = OrbiterEdgeOffsetType.None,
                offset = DpVolumeOffset(y = 0.dp, z = OrbiterDefaults.Elevation),
            )
        OrbiterPosition.Inside ->
            OrbiterAlignment.BottomCenter(
                edgeOffsetType = OrbiterEdgeOffsetType.InnerEdge,
                offset =
                    DpVolumeOffset(
                        y = NavigationSuiteOrbiterDefaultSpacing,
                        z = OrbiterDefaults.Elevation,
                    ),
            )
    }

internal fun OrbiterPosition.toVerticalAlignment(): OrbiterAlignment =
    when (this) {
        OrbiterPosition.Outside ->
            OrbiterAlignment.CenterStart(
                edgeOffsetType = OrbiterEdgeOffsetType.OuterEdge,
                offset =
                    DpVolumeOffset(
                        x = -NavigationSuiteOrbiterDefaultSpacing,
                        z = OrbiterDefaults.Elevation,
                    ),
            )
        OrbiterPosition.Overlapping ->
            OrbiterAlignment.CenterStart(
                edgeOffsetType = OrbiterEdgeOffsetType.None,
                offset = DpVolumeOffset(x = 0.dp, z = OrbiterDefaults.Elevation),
            )
        OrbiterPosition.Inside ->
            OrbiterAlignment.CenterStart(
                edgeOffsetType = OrbiterEdgeOffsetType.InnerEdge,
                offset =
                    DpVolumeOffset(
                        x = NavigationSuiteOrbiterDefaultSpacing,
                        z = OrbiterDefaults.Elevation,
                    ),
            )
    }
