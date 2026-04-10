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

package androidx.xr.arcore.testing.internal

import androidx.xr.arcore.runtime.Face
import androidx.xr.arcore.runtime.Mesh
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import java.nio.FloatBuffer
import java.nio.ShortBuffer

internal class FakeRuntimeFace(
    override var trackingState: TrackingState = TrackingState.PAUSED,
    override var isValid: Boolean = true,
    override var blendShapeValues: FloatArray = FloatArray(0),
    override var confidenceValues: FloatArray = FloatArray(0),
) : Face {

    override var centerPose: Pose = Pose()

    override var mesh: Mesh =
        Mesh(
            triangleIndices = ShortBuffer.allocate(1),
            vertices = FloatBuffer.allocate(1),
            normals = FloatBuffer.allocate(1),
            textureCoordinates = FloatBuffer.allocate(1),
        )

    override var noseTipPose: Pose = Pose(Vector3(0f, 0f, 1f), Quaternion.Identity)

    override var foreheadLeftPose: Pose = Pose(Vector3(-1f, 1f, 0f), Quaternion.Identity)

    override var foreheadRightPose: Pose = Pose(Vector3(1f, 1f, 0f), Quaternion.Identity)

    init {
        mesh.triangleIndices!!.put(0)
        mesh.normals!!.put(0f)
        mesh.textureCoordinates!!.put(0f)
        mesh.vertices!!.put(0f)
    }
}
