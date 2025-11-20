/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.ar.core.Pose as ARCorePose

internal fun Pose.toARCorePose(): ARCorePose {
    return ARCorePose(
        floatArrayOf(translation.x, translation.y, translation.z),
        floatArrayOf(rotation.x, rotation.y, rotation.z, rotation.w),
    )
}

internal fun ARCorePose.toRuntimePose(): Pose {
    return Pose(
        translation = Vector3(tx(), ty(), tz()),
        rotation = Quaternion(qx(), qy(), qz(), qw()),
    )
}
