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

package androidx.xr.arcore.openxr

import androidx.annotation.RestrictTo
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.math.Pose

/**
 * Represents the current state of a [RenderViewpoint].
 *
 * @property pose the pose of the view camera.
 * @property fieldOfView the field of view of the view camera.
 */
// TODO(b/439895601): Rename ViewCameraState to RenderViewpointState
@Suppress("DataClassDefinition")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal data class ViewCameraState(
    val pose: Pose = Pose(),
    val fieldOfView: FieldOfView = FieldOfView(0f, 0f, 0f, 0f),
)
