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
import androidx.compose.ui.unit.dp
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.runtime.math.Quaternion

/** A sample demonstrating a simple movable component. */
@Sampled
@SubspaceComposable
@Composable
public fun BasicMovableSample() {
    SpatialPanel(modifier = SubspaceModifier.movable()) { Text("The user can move me around!") }
}

/** A sample demonstrating a custom movable component. */
@Sampled
@SubspaceComposable
@Composable
public fun CustomMovableSample() {
    var offsetX by remember { mutableStateOf(0.dp) }
    var offsetY by remember { mutableStateOf(0.dp) }
    var offsetZ by remember { mutableStateOf(0.dp) }
    var rotation by remember { mutableStateOf(Quaternion.Identity) }

    SpatialPanel(
        modifier =
            SubspaceModifier.movable {
                    offsetX = it.pose.translation.x.meters.toDp()
                    offsetY = it.pose.translation.y.meters.toDp()
                    offsetZ = it.pose.translation.z.meters.toDp()
                    rotation = it.pose.rotation

                    true // return true to prevent default behavior
                }
                .offset(x = offsetX, y = offsetY, z = offsetZ)
                .rotate(rotation)
    ) {
        Text("The user can move me around!")
    }
}
