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

package androidx.xr.arcore

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose

/** Result of a call to [Earth.createPoseFromGeospatialPose]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public sealed class CreatePoseFromGeospatialPoseResult

/**
 * Result of a successful [Earth.createPoseFromGeospatialPose] call.
 *
 * @property pose the [Pose] that was created.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class CreatePoseFromGeospatialPoseSuccess(public val pose: Pose) :
    CreatePoseFromGeospatialPoseResult()

/**
 * Result of an unsuccessful [Earth.createPoseFromGeospatialPose] call. Required tracking is not
 * available.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class CreatePoseFromGeospatialPoseNotTracking() : CreatePoseFromGeospatialPoseResult()

/**
 * Result of an unsuccessful [Earth.createPoseFromGeospatialPose] call. The [Earth] encountered an
 * error, such as if Geospatial was not enabled.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class CreatePoseFromGeospatialPoseIllegalState() : CreatePoseFromGeospatialPoseResult()
