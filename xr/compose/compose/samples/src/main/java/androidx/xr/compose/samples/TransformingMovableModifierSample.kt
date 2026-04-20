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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialMoveEvent
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.transformingMovable

/** A sample demonstrating a simple movable component. */
@Sampled
@SubspaceComposable
@Composable
public fun BasicTransformingMovableSample() {
    SpatialPanel(modifier = SubspaceModifier.transformingMovable()) {
        Text("The user can move me around!")
    }
}

/**
 * A sample demonstrating movement of sibling composables using the onMove callback from the
 * transformingMovable modifier
 */
@Sampled
@SubspaceComposable
@Composable
public fun TransformingMovableSiblingSample() {
    val density = LocalDensity.current
    var xOffset by remember { mutableStateOf(0.dp) }
    var yOffset by remember { mutableStateOf(0.dp) }
    var zOffset by remember { mutableStateOf(0.dp) }
    val customMovement: (SpatialMoveEvent) -> Unit = { moveEvent ->
        with(density) {
            xOffset = moveEvent.pose.translation.x.toDp()
            yOffset = moveEvent.pose.translation.y.toDp()
            zOffset = moveEvent.pose.translation.z.toDp()
        }
    }
    Subspace {
        SpatialPanel(modifier = SubspaceModifier.transformingMovable(onMove = customMovement)) {
            Text("The user can move me around")
        }
        SpatialPanel(modifier = SubspaceModifier.offset(x = xOffset, y = yOffset, z = zOffset)) {
            Text("Sibling Panel")
        }
    }
}
