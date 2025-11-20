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

package androidx.xr.compose.samples.animation

import androidx.annotation.Sampled
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.animation.AnimatedSpatialVisibility
import androidx.xr.compose.subspace.animation.SpatialTransitions
import androidx.xr.compose.unit.IntVolumeOffset

@Sampled
@Composable
fun SpatialFade() {
    Subspace {
        val visibleState = remember { MutableTransitionState(false) }
        visibleState.targetState = true

        AnimatedSpatialVisibility(
            visibleState = visibleState,
            enter = SpatialTransitions.fadeIn(initialAlpha = 0.5f),
            exit =
                SpatialTransitions.fadeOut(
                    animationSpec = spring(stiffness = Spring.StiffnessVeryLow)
                ),
        ) {
            SpatialPanel { Text("Spatial panel") }
        }
    }
}

@Sampled
@Composable
fun SpatialSlide() {
    Subspace {
        val visibleState = remember { MutableTransitionState(false) }
        visibleState.targetState = true

        AnimatedSpatialVisibility(
            visibleState = visibleState,
            // if sliding on multiple axes, an initial/target point must be given
            enter =
                SpatialTransitions.slideIn { size ->
                    IntVolumeOffset(
                        // negative x: from left to right
                        x = -size.width / 2,
                        // negative y: from bottom to top
                        y = -size.height / 2,
                        // negative z: from far to close
                        z = -size.depth / 2,
                    )
                },
            // defaults are provided when only sliding on one axis
            exit = SpatialTransitions.slideOutHorizontally(),
        ) {
            SpatialPanel { Text("Spatial panel") }
        }
    }
}
