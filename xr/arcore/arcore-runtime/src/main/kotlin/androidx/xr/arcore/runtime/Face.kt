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

package androidx.xr.arcore.runtime

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose

/**
 * Describes a face.
 *
 * @property isValid a flag indicating if the Face is valid
 * @property blendShapeValues the values measuring the blend shapes of the face
 * @property confidenceValues the confidence values of the face tracker at different regions
 * @property centerPose the [Pose] at the geometric center of the [mesh] if it exists
 * @property mesh a [Mesh] representation of the Face
 * @property noseTipPose the [Pose] located at the tip of the nose on the [mesh] if it exists
 * @property foreheadLeftPose the [Pose] located at the left side of the forehead on the [mesh] if
 *   it exists
 * @property foreheadRightPose the [Pose] located at the right side of the forehead on the [mesh] if
 *   it exists
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface Face : Trackable {
    public val isValid: Boolean
    public val blendShapeValues: FloatArray?
    public val confidenceValues: FloatArray?
    public val centerPose: Pose?
    public val mesh: Mesh?
    public val noseTipPose: Pose?
    public val foreheadLeftPose: Pose?
    public val foreheadRightPose: Pose?
}
