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
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose

/**
 * Describes an object that's being tracked in the real world.
 *
 * @property category the [AugmentedObjectCategory] of this tracked object
 * @property centerPose the [Pose] determined to represent the center of this object
 * @property extents a set of extents to used to determine the size of the object
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface AugmentedObject : Trackable {
    public val category: AugmentedObjectCategory
    public val centerPose: Pose
    public val extents: FloatSize3d
}
