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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import androidx.xr.runtime.AugmentedObjectCategory as Category
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3

/** Describes an object that's being tracked in the real world. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface AugmentedObject : Trackable {
    /** The [Category] of this tracked object. */
    public val category: Category
    /**
     * The [Pose] determined to represent the center of this object.
     *
     * This value may or may not overlap with the object's center of gravity.
     */
    public val centerPose: Pose
    /**
     * A set of extents to used to determine the size of the object. These are assumed to originate
     * from the [centerPose].
     */
    public val extents: Vector3
}
