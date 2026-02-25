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

package androidx.xr.arcore.playservices

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Anchor
import androidx.xr.arcore.runtime.Face
import androidx.xr.arcore.runtime.Mesh
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import com.google.ar.core.AugmentedFace
import java.lang.IllegalStateException

/**
 * Wraps an ARCore [AugmentedFace] with the [Face] interface.
 *
 * @property trackingState the [TrackingState] of the face
 * @property isValid whether the face is valid
 * @property blendShapeValues the blend shape values of the face
 * @property confidenceValues the confidence values of the face
 * @property centerPose the [Pose] of the center of the face
 * @property mesh the face [Mesh]
 * @property noseTipPose the [Pose] of the nose tip
 * @property foreheadLeftPose the [Pose] of the left forehead
 * @property foreheadRightPose the [Pose] of the right forehead
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ArCoreFace internal constructor(private val arCoreFace: AugmentedFace) : Face {
    @UnsupportedArCoreCompatApi public fun arCoreAugmentedFace(): AugmentedFace = arCoreFace

    override val trackingState: TrackingState
        get() = TrackingState.fromArCoreTrackingState(arCoreFace.trackingState)

    override val isValid: Boolean
        get() =
            mesh.triangleIndices != null &&
                mesh.vertices != null &&
                mesh.normals != null &&
                mesh.textureCoordinates != null

    override val blendShapeValues: FloatArray? = null

    override val confidenceValues: FloatArray? = null

    override val centerPose: Pose
        get() = arCoreFace.centerPose.toRuntimePose()

    override val mesh: Mesh
        get() =
            Mesh(
                arCoreFace.meshTriangleIndices,
                arCoreFace.meshVertices,
                arCoreFace.meshNormals,
                arCoreFace.meshTextureCoordinates,
            )

    override val noseTipPose: Pose
        get() = arCoreFace.getRegionPose(AugmentedFace.RegionType.NOSE_TIP).toRuntimePose()

    override val foreheadLeftPose: Pose
        get() = arCoreFace.getRegionPose(AugmentedFace.RegionType.FOREHEAD_LEFT).toRuntimePose()

    override val foreheadRightPose: Pose
        get() = arCoreFace.getRegionPose(AugmentedFace.RegionType.FOREHEAD_RIGHT).toRuntimePose()

    /**
     * ARCore AugmentedFace supports front-facing (selfie) camera only, and does not support
     * attaching anchors nor raycast hit testing. Calling [createAnchor] will always throw
     * [IllegalStateException].
     *
     * @param pose the [Pose] to create the anchor at
     * @return an [Anchor]
     * @throws [IllegalStateException]
     */
    override fun createAnchor(pose: Pose): Anchor =
        throw IllegalStateException("createAnchor is not supported by AugmentedFaces")
}
