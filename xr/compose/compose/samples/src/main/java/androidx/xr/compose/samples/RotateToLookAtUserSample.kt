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
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.gravityAligned
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.rotateToLookAtUser
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3

/**
 * A sample demonstrating how to combine [rotateToLookAtUser] and [gravityAligned] to achieve
 * billboard behavior where the content automatically rotates to face the user. [gravityAligned]
 * ensures the panel stays vertically upright and does not tilt forward or backward, even if the
 * user views it from a high or low angle.
 */
@Sampled
@Composable
public fun RotateToLookAtUserBillboardSample() {
    Subspace {
        SpatialPanel(modifier = SubspaceModifier.rotateToLookAtUser().gravityAligned()) {
            Text("I always face you and stay upright!")
        }
    }
}

/**
 * A sample showing how to use the 'upDirection' parameter. By providing a custom up vector, you can
 * change the reference frame for the content's orientation.
 */
@Sampled
@Composable
public fun RotateToLookAtUserWithUpVectorSample() {
    Subspace {
        SpatialPanel(
            modifier =
                SubspaceModifier.rotateToLookAtUser(
                    upDirection = Vector3(0f, 1f, 2f)
                ) // A slightly tilted "up" reference
        ) {
            Text("I have a custom 'Up' vector.")
        }
    }
}

/**
 * A sample showing how [rotateToLookAtUser] behaves within a parent spatial layout. In this
 * example, even if the [SpatialBox] is moved or rotated, the panel with [rotateToLookAtUser] will
 * independently calculate its local rotation to ensure it remains facing the user.
 */
@Sampled
@Composable
public fun RotateToLookAtUserUnderParentContainerSample() {
    val parentRotation = Quaternion.fromEulerAngles(pitch = 40f, yaw = 30f, roll = 20f)

    Subspace {
        SpatialBox(SubspaceModifier.rotate(parentRotation)) {
            // This panel will rotate to face the user regardless of where
            // the parent SpatialBox is placed in the ActivitySpace.
            SpatialPanel(modifier = SubspaceModifier.rotateToLookAtUser()) {
                Text("I'm inside a SpatialBox, but I still see you!")
            }
        }
    }
}
