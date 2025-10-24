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

/** Describes a face. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface Face : Trackable {
    /** Flag indicating if the [Face] is valid */
    public val isValid: Boolean

    /** The values measuring the blend shapes of the face. Range: `[0.0f, 1.0f]` */
    public val blendShapeValues: FloatArray?

    /** The confidence values of the face tracker at different regions. Range: `[0.0f, 1.0f]` */
    public val confidenceValues: FloatArray?

    /** The [Pose] at the geometric center of the [mesh]. */
    public val centerPose: Pose?

    /** The [Mesh] data. */
    public val mesh: Mesh?

    /** The [Pose] located at the tip of the nose. */
    public val noseTipPose: Pose?

    /** The [Pose] located at the left side of the detected face's forehead. */
    public val foreheadLeftPose: Pose?

    /** The [Pose] located at the right side of the detected face's forehead. */
    public val foreheadRightPose: Pose?
}
