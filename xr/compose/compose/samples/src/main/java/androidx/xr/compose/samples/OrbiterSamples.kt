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
package androidx.xr.compose.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterDefaults
import androidx.xr.compose.spatial.OrbiterPosition
import androidx.xr.compose.spatial.OrbiterPosition.EdgeAlignment
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.unit.DpVolumeOffset

/**
 * Sample demonstrating a bottom bar orbiter positioning using [OrbiterPosition].
 *
 * ```
 * +------------------------+
 * |                        |
 * |                        |
 * |      SpatialPanel      |
 * |                        |
 * |      +---------+       |
 * +------| Orbiter |-------+
 *        +---------+
 * ```
 */
@Sampled
@Composable
@SubspaceComposable
public fun OrbiterBottomBarSample() {
    SpatialPanel(SubspaceModifier.width(400.dp).height(300.dp)) {
        // Place a horizontal bar orbiter at the bottom-center edge of the panel.
        Orbiter(
            position =
                OrbiterPosition.BottomCenter(
                    offset = DpVolumeOffset(x = 0.dp, y = 0.dp, z = OrbiterDefaults.Elevation),
                    verticalEdgeAlignment = EdgeAlignment.Center,
                )
        ) {
            Box(
                modifier = Modifier.size(200.dp, 64.dp).background(Color.DarkGray),
                contentAlignment = Alignment.Center,
            ) {
                Text("Bottom Bar", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

/**
 * Sample demonstrating a top rail orbiter positioning using [OrbiterPosition].
 *
 * ```
 *     +---------+
 *     | Orbiter |
 *     +---------+--------------+
 *     |                        |
 *     |      SpatialPanel      |
 *     |                        |
 *     |                        |
 *     +------------------------+
 * ```
 */
@Sampled
@Composable
@SubspaceComposable
public fun OrbiterTopRailSample() {
    SpatialPanel(SubspaceModifier.width(400.dp).height(300.dp)) {
        // Place the orbiter at the top-start corner: horizontally aligned inside the left edge,
        // but vertically sitting completely above (outside) the top edge.
        Orbiter(
            position =
                OrbiterPosition.TopStart(
                    offset = DpVolumeOffset(x = 0.dp, y = 0.dp, z = OrbiterDefaults.Elevation),
                    horizontalEdgeAlignment = EdgeAlignment.Inside,
                    verticalEdgeAlignment = EdgeAlignment.Outside,
                )
        ) {
            Box(
                modifier = Modifier.size(200.dp, 64.dp).background(Color.DarkGray),
                contentAlignment = Alignment.Center,
            ) {
                Text("Top Rail", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

/**
 * Sample demonstrating a side rail orbiter positioning using [OrbiterPosition].
 *
 * ```
 * +---------+------------------------+
 * | Orbiter |                        |
 * +---------+                        |
 *           |      SpatialPanel      |
 *           |                        |
 *           |                        |
 *           +------------------------+
 * ```
 */
@Sampled
@Composable
@SubspaceComposable
public fun OrbiterSideRailSample() {
    SpatialPanel(SubspaceModifier.width(400.dp).height(300.dp)) {
        // Place the orbiter at the top-start corner: horizontally sitting completely to the side
        // (outside) of the left edge, but vertically aligned inside the top boundary.
        Orbiter(
            position =
                OrbiterPosition.TopStart(
                    offset = DpVolumeOffset(x = 0.dp, y = 0.dp, z = OrbiterDefaults.Elevation),
                    horizontalEdgeAlignment = EdgeAlignment.Outside,
                    verticalEdgeAlignment = EdgeAlignment.Inside,
                )
        ) {
            Box(
                modifier = Modifier.size(64.dp, 200.dp).background(Color.DarkGray),
                contentAlignment = Alignment.Center,
            ) {
                Text("Rail", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}
